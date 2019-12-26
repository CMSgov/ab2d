#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set default values
#

export DEBUG_LEVEL="WARN"

#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
  CMS_ENV=$(echo $ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --shared-environment=*)
  SHARED_ENVIRONMENT="${i#*=}"
  CMS_SHARED_ENV=$(echo $SHARED_ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --vpc-id=*)
  VPC_ID="${i#*=}"
  shift # past argument=value
  ;;
  --ssh-username=*)
  SSH_USERNAME="${i#*=}"
  shift # past argument=value
  ;;
  --owner=*)
  OWNER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2-instance-type=*)
  EC2_INSTANCE_TYPE="${i#*=}"
  shift # past argument=value
  ;;
  --database-secret-datetime=*)
  DATABASE_SECRET_DATETIME=$(echo ${i#*=})
  shift # past argument=value
  ;;  
  --debug-level=*)
  DEBUG_LEVEL=$(echo ${i#*=} | tr '[:lower:]' '[:upper:]')
  shift # past argument=value
  ;;
  --build-new-images)
  BUILD_NEW_IMAGES="true"
  shift # past argument=value
  ;;
  --use-existing-images)
  USE_EXISTING_IMAGES="true"
  shift # past argument=value
  ;;
  --auto-approve)
  AUTOAPPROVE="true"
  shift # past argument=value
  ;;
esac
done

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ] || [ -z "${SHARED_ENVIRONMENT}" ] || [ -z "${VPC_ID}" ] || [ -z "${OWNER}" ] || [ -z "${DATABASE_SECRET_DATETIME}" ]; then
  echo "Try running the script like one of these options:"
  echo "./create-base-environment.sh --environment=dev --shared-environment=shared --vpc-id={vpc id} --owner=842420567215 --database-secret-datetime={YYYY-MM-DD-HH-MM-SS}"
  echo "./create-base-environment.sh --environment=dev --vpc-id={vpc id} --owner=842420567215 --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --debug-level={TRACE|DEBUG|INFO|WARN|ERROR}"
  exit 1
fi

#
# Set environment
#

export AWS_PROFILE="ab2d-${CMS_SHARED_ENV}"

# Verify that VPC ID exists

VPC_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs \
  --query "Vpcs[?VpcId=='$VPC_ID'].VpcId" \
  --output text)

if [ -z "${VPC_EXISTS}" ]; then
  echo "*********************************************************"
  echo "ERROR: VPC ID does not exist for the target AWS profile."
  echo "*********************************************************"
  exit 1
fi

#
# Get KMS key id (if exists)
#

KMS_KEY_ID=$(aws kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${KMS_KEY_ID}" ]; then
  KMS_KEY_STATE=$(aws kms describe-key \
    --key-id alias/ab2d-kms \
    --query "KeyMetadata.KeyState" \
    --output text)
fi

#
# Configure terraform
#

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

#
# Initialize and validate terraform
#

# Initialize and validate terraform for the shared components

echo "**************************************************************"
echo "Initialize and validate terraform for the shared components..."
echo "**************************************************************"

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

terraform init
terraform validate

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

terraform init
terraform validate

#
# Deploy or enable KMS
#

# If KMS key exists, enable it; otherwise, create it

if [ -n "$KMS_KEY_ID" ]; then
  echo "Enabling KMS key..."
  cd "${START_DIR}"
  cd python3
  ./enable-kms-key.py $KMS_KEY_ID
else
  echo "Deploying KMS..."
  cd "${START_DIR}"
  cd terraform/environments/ab2d-$CMS_SHARED_ENV
  terraform apply \
    --target module.kms --auto-approve
fi

# Get KMS key id

KMS_KEY_ID=$(aws kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

#
# Create or get secrets
#

# Change to the "python3" directory

cd "${START_DIR}"
cd python3

# Create or get database user secret

DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_USER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_user $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
fi

# Create or get database password secret

DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
fi

# Create or get database name secret (if doesn't exist)

DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_NAME}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
fi

# If any databse secret produced an error, exit the script

if [ "${DATABASE_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get database secrets because KMS key is disabled!"
    exit 1
fi

#
# Configure networking and get network attributes
#

# Enable DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws ec2 describe-vpc-attribute \
  --vpc-id $VPC_ID \
  --attribute enableDnsHostnames \
  --query "EnableDnsHostnames.Value" \
  --output text)

if [ "${VPC_ENABLE_DNS_HOSTNAMES}" == "False" ]; then
  echo "Enabling DNS hostnames on VPC..."
  aws ec2 modify-vpc-attribute \
    --vpc-id $VPC_ID \
    --enable-dns-hostnames
fi

#
# Get first public subnet id
#

echo "Getting first public subnet id..."

SUBNET_PUBLIC_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then

    echo "ERROR: public subnet #1 not found..."
    exit 1

fi

#
# Get second public subnet id
#

echo "Getting second public subnet id..."

SUBNET_PUBLIC_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then

  echo "ERROR: public subnet #2 not found..."
  exit 1

fi

#
# Get first private subnet id
#

echo "Getting first private subnet id..."

SUBNET_PRIVATE_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then

  echo "ERROR: private subnet #1 not found..."
  exit 1
  
fi

#
# Get second private subnet id
#

echo "Getting second private subnet id..."

SUBNET_PRIVATE_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then

  echo "ERROR: private subnet #2 not found..."
  exit 1

fi

#
# Get internet gateway id
#

echo "Getting internet gateway id..."

IGW_ID=$(aws ec2 describe-internet-gateways \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query "InternetGateways[*].InternetGatewayId" \
  --output text)

if [ -z "${IGW_ID}" ]; then
    
  echo "ERROR: internet gateway not found..."
  exit 1

fi

#
# Get custom route table id for internet gateway
#

echo "Getting custom route table id for internet gateway..."

IGW_ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ID}" ]; then

  echo "ERROR: custom route table for internet gateway not found..."
  exit 1
  
fi

# Verify internet gateway route is present in the custom route table for internet gateway

echo "Verifying internet gateway route is present in the custom route table for internet gateway..."

IGW_ROUTE_TABLE_IGW_ROUTE_TARGET=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].Routes[?GatewayId=='$IGW_ID'].GatewayId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_IGW_ROUTE_TARGET}" ]; then

  echo "ERROR: internet gateway route is not present in the custom route table for internet gateway..."
  exit 1
  
fi

# Verify the association of the first public subnet with the custom route table for internet gateway

echo "Verifying the association of the first public subnet with the custom route table for internet gateway..."

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_1_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID}" ]; then

  echo "ERROR: the first public subnet is not associated with the custom route table for internet gateway..."
  exit 1
  
fi

# Verify the association of the second public subnet with the custom route table for internet gateway

echo "Verifying the association of the second public subnet with the custom route table for internet gateway..."

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_2_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID}" ]; then

  echo "ERROR: the second public subnet is not associated with the custom route table for internet gateway..."
  exit 1
  
fi

#
# Enable Auto-assign Public IP on public subnets
#

# Enable Auto-assign Public IP on the first public subnet

SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query "Subnets[?MapPublicIpOnLaunch].MapPublicIpOnLaunch" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH}" ]; then

  echo "Enabling Auto-assign Public IP on the first public subnet..."
    
  aws ec2 modify-subnet-attribute \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --map-public-ip-on-launch \
    --region us-east-1

fi

# Enable Auto-assign Public IP on the second public subnet

SUBNET_PUBLIC_2_MAP_PUBLIC_IP_ON_LAUNCH=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query "Subnets[?MapPublicIpOnLaunch].MapPublicIpOnLaunch" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_MAP_PUBLIC_IP_ON_LAUNCH}" ]; then

  echo "Enabling Auto-assign Public IP on the second public subnet..."
    
  aws ec2 modify-subnet-attribute \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --map-public-ip-on-launch \
    --region us-east-1
  
fi

#
# Verify first NAT Gateway
#

# Verify Elastic IP for first NAT Gateway

echo "Verifying Elastic IP for first NAT Gateway..."

EIP_ALLOC_1_ID=$(aws ec2 describe-addresses \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-nat-gateway-a" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_1_ID}" ]; then

  echo "ERROR: the elastic IP for the first NAT Gateway is not found..."
  exit 1

fi

# Verify first NAT Gateway

echo "Verifying first NAT Gateway..."

NAT_GW_1_ID=$(aws ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-${CMS_ENV}-a" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_1_ID}" ]; then

  echo "ERROR: the first NAT Gateway is not found..."
  exit 1

fi

# Verify the custom route table for the first NAT Gateway

echo "Verifying the custom route table for the first NAT Gateway..."

ROUTE_TABLE_FOR_NGW_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_1_ID}" ]; then

  echo "ERROR: the custom route table for the first NAT Gateway is not found..."
  exit 1
  
fi

# Verify first NAT gateway route is present in the custom route table for the first NAT gateway

echo "Verifying first NAT gateway route is present in the custom route table for the first NAT gateway..."

NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_1_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET}" ]; then

  echo "ERROR: first NAT gateway route is not present in the custom route table for first NAT gateway..."
  exit 1
  
fi

# Verify that association of the first private subnet with the custom route table for the first NAT Gateway

echo "Verifying that association of the first private subnet with the custom route table for the first NAT Gateway..."

NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_1_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID}" ]; then

  echo "ERROR: the first private subnet is not associated with the custom route table for the first NAT Gateway..."
  exit 1

fi

#
# Verify second NAT Gateway
#

# Verify Elastic IP for second NAT Gateway

echo "Verifying Elastic IP for second NAT Gateway..."

EIP_ALLOC_2_ID=$(aws ec2 describe-addresses \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-nat-gateway-b" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_2_ID}" ]; then

  echo "ERROR: the elastic IP for the second NAT Gateway is not found..."
  exit 1

fi

# Verify second NAT Gateway

echo "Verifying second NAT Gateway..."

NAT_GW_2_ID=$(aws ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-${CMS_ENV}-b" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_2_ID}" ]; then

  echo "ERROR: the second NAT Gateway is not found..."
  exit 1

fi
 
# Verify the custom route table for the second NAT Gateway

echo "Verifying the custom route table for the second NAT Gateway..."

ROUTE_TABLE_FOR_NGW_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_2_ID}" ]; then

  echo "ERROR: the custom route table for the first NAT Gateway is not found..."
  exit 1
  
fi

# Verify second NAT gateway route is present in the custom route table for the second NAT gateway

echo "Verifying second NAT gateway route is present in the custom route table for the second NAT gateway..."

NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_2_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET}" ]; then

  echo "ERROR: second NAT gateway route is not present in the custom route table for second NAT gateway..."
  exit 1
  
fi

# Verify that association of the second private subnet with the custom route table for the second NAT Gateway

echo "Verifying that association of the second private subnet with the custom route table for the second NAT Gateway..."

NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_2_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID}" ]; then

  echo "ERROR: the second private subnet is not associated with the custom route table for the second NAT Gateway..."
  exit 1
  
fi

#
# AMI Generation for application nodes
#

# Set AMI_ID if it already exists for the deployment

echo "Set AMI_ID if it already exists for the deployment..."
AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# If no AMI is specified then create a new one

echo "If no AMI is specified then create a new one..."
if [ -z "${AMI_ID}" ]; then
    
  # Get the latest seed AMI
  SEED_AMI=$(aws --region us-east-1 ec2 describe-images \
    --owners "${OWNER}" \
    --filters "Name=name,Values=EAST-RH 7*" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')
  
  # Get first public subnet
  SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-dev-public-a" \
    --query "Subnets[0].SubnetId" \
    --output text)
  
  # Create AMI for application nodes
  cd "${START_DIR}"
  cd packer/app
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
    --var region=us-east-1 \
    --var ec2_instance_type=$EC2_INSTANCE_TYPE \
    --var vpc_id=$VPC_ID \
    --var subnet_public_1_id=$SUBNET_PUBLIC_1_ID \
    --var my_ip_address=$IP \
    --var ssh_username=$SSH_USERNAME \
    --var git_commit_hash=$COMMIT \
    app.json  2>&1 | tee output.txt
  AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
  # Add name tag to AMI
  aws --region us-east-1 ec2 create-tags \
    --resources $AMI_ID \
    --tags "Key=Name,Value=ab2d-ami"
fi

#
# AMI Generation for Jenkins node
#

# Set JENKINS_AMI_ID if it already exists for the deployment

echo "Set JENKINS_AMI_ID if it already exists for the deployment..."
JENKINS_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-jenkins-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# If no AMI is specified then create a new one

echo "If no AMI is specified then create a new one..."
if [ -z "${JENKINS_AMI_ID}" ]; then

  # Get the latest seed AMI
  SEED_AMI=$(aws --region us-east-1 ec2 describe-images \
    --owners "${OWNER}" \
    --filters "Name=name,Values=EAST-RH 7*" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')

  # Get first public subnet
  SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-dev-public-a" \
    --query "Subnets[0].SubnetId" \
    --output text)

  # Create AMI for Jenkins
  cd "${START_DIR}"
  cd packer/jenkins
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
    --var region=us-east-1 \
    --var ec2_instance_type=$EC2_INSTANCE_TYPE \
    --var vpc_id=$VPC_ID \
    --var subnet_public_1_id=$SUBNET_PUBLIC_1_ID \
    --var my_ip_address=$IP \
    --var ssh_username=$SSH_USERNAME \
    --var git_commit_hash=$COMMIT \
    app.json  2>&1 | tee output.txt
  JENKINS_AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
  # Add name tag to AMI
  aws --region us-east-1 ec2 create-tags \
    --resources $JENKINS_AMI_ID \
    --tags "Key=Name,Value=ab2d-jenkins-ami"
fi

# Get deployer IP address

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

#
# Create "auto.tfvars" files
#

# Create "auto.tfvars" file for shared components

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

DB_ENDPOINT=$(aws --region us-east-1 rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

if [ -z "${DB_ENDPOINT}" ]; then
  echo 'vpc_id = "'$VPC_ID'"' \
    > $CMS_SHARED_ENV.auto.tfvars
  echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'linux_user = "'$SSH_USERNAME'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'ami_id = "'$AMI_ID'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
else
  PRIVATE_SUBNETS_OUTPUT=$(aws ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-*-private-*" \
    --query "Subnets[*].SubnetId" \
    --output text)

  IFS=$'\t' read -ra subnet_array <<< "$PRIVATE_SUBNETS_OUTPUT"
  unset IFS
  
  COUNT=1
  for i in "${subnet_array[@]}"
  do
    if [ $COUNT == 1 ]; then
      PRIVATE_SUBNETS=\"$i\"
    else
      PRIVATE_SUBNETS=$PRIVATE_SUBNETS,\"$i\"
    fi
    COUNT=$COUNT+1
  done

  echo 'vpc_id = "'$VPC_ID'"' \
    > $CMS_SHARED_ENV.auto.tfvars
  echo "private_subnet_ids = [${PRIVATE_SUBNETS}]" \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'linux_user = "'$SSH_USERNAME'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'ami_id = "'$AMI_ID'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
  echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
    >> $CMS_SHARED_ENV.auto.tfvars
fi

# Create ".auto.tfvars" file for the target environment

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

echo 'vpc_id = "'$VPC_ID'"' \
  > $CMS_ENV.auto.tfvars
echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars
echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE'"' \
  >> $CMS_ENV.auto.tfvars
echo 'linux_user = "'$SSH_USERNAME'"' \
  >> $CMS_ENV.auto.tfvars
echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
  >> $CMS_ENV.auto.tfvars

##########################
# Deploy shared components
##########################

#
# Configure shared environment
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

#
# Create "cms-ab2d-cloudtrail" bucket
#

echo "Creating "cms-ab2d-cloudtrail" bucket..."

aws --region us-east-1 s3api create-bucket \
  --bucket cms-ab2d-cloudtrail

# Block public access on bucket

aws --region us-east-1 s3api put-public-access-block \
  --bucket cms-ab2d-cloudtrail \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cms-ab2d-cloudtrail" bucket

aws --region us-east-1 s3api put-bucket-acl \
  --bucket cms-ab2d-cloudtrail \
  --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
  --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery

# Add bucket policy to the "cms-ab2d-cloudtrail" S3 bucket

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

aws --region us-east-1 s3api put-bucket-policy \
  --bucket cms-ab2d-cloudtrail \
  --policy file://ab2d-cloudtrail-bucket-policy.json

#
# Create dev S3 bucket
#

echo "Deploying dev S3 bucket..."

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

terraform apply \
  --target module.s3 \
  --auto-approve

#
# Deploy db
#

echo "Create or update database instance..."

# Create DB instance (if doesn't exist)

terraform apply \
  --var "db_username=${DATABASE_USER}" \
  --var "db_password=${DATABASE_PASSWORD}" \
  --var "db_name=${DATABASE_NAME}" \
  --target module.db --auto-approve

DB_ENDPOINT=$(aws --region us-east-1 rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV
rm -f generated/.pgpass

# Generate ".pgpass" file

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV
mkdir -p generated

# Add default database

echo "${DB_ENDPOINT}:5432:postgres:${DATABASE_USER}:${DATABASE_PASSWORD}" > generated/.pgpass

# *** TO DO *** BEGIN: add all environments to the .pgpass file

# Get the names of all deployed private subnets

# PRIVATE_SUBNETS=$(aws ec2 describe-subnets \
#   --filters "Name=tag:Name,Values=ab2d-*-private-subnet*" \
#   --query "Subnets[*].Tags[?Key == 'Name'].Value" \
#   --output text)

# Determine all deployed environments by extracting unique environment name from private subnet names

# IFS=$' ' UNIQUE_ENVS=($(uniq <<< $(sort <<<"${PRIVATE_SUBNETS[*]}" \
#   | sed 's/\(^ab2d-\)\(.*\)\(-private-subnet.*\)/\2/'))); \
#   unset IFS

# echo "${DB_ENDPOINT}:5432:postgres:${DATABASE_USER}:${DATABASE_PASSWORD}" > generated/.pgpass
# for i in "${UNIQUE_ENVS}"; do
#   cd "${START_DIR}"
#   cd python3
#   DATABASE_USER=$(./get-database-secret.py "${i}" database_user $DATABASE_SECRET_DATETIME)
#   DATABASE_PASSWORD=$(./get-database-secret.py "${i}" database_password $DATABASE_SECRET_DATETIME)
#   DATABASE_NAME=$(./get-database-secret.py "${i}" database_name $DATABASE_SECRET_DATETIME)
#   cd "${START_DIR}"
#   cd terraform/environments/ab2d-$CMS_SHARED_ENV
#   echo "${DB_ENDPOINT}:5432:${DATABASE_NAME}:${DATABASE_USER}:${DATABASE_PASSWORD}" >> generated/.pgpass
# done

# *** TO DO *** END: add all environments to the .pgpass file

#
# Deploy controller
#

echo "Create or update controller..."

terraform apply \
  --var "db_username=${DATABASE_USER}" \
  --var "db_password=${DATABASE_PASSWORD}" \
  --var "db_name=${DATABASE_NAME}" \
  --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
  --target module.controller \
  --auto-approve

######################################
# Deploy target environment components
######################################

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

#
# Deploy EFS
#

# Get files system id (if exists)

EFS_FS_ID=$(aws efs describe-file-systems --query="FileSystems[?CreationToken=='ab2d-${CMS_ENV}-efs'].FileSystemId" --output=text)

# Create file system (if doesn't exist)

if [ -z "${EFS_FS_ID}" ]; then
  echo "Creating EFS..."
  terraform apply \
    --target module.efs \
    --auto-approve
fi

#
# Create database
#

cd "${START_DIR}"

# Get the public ip address of the controller

CONTROLLER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
  --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
  --output text)

# Determine if the database for the environment exists

DB_NAME_IF_EXISTS=$(ssh -tt -i "~/.ssh/ab2d-${CMS_SHARED_ENV}.pem" \
  "${SSH_USERNAME}@${CONTROLLER_PUBLIC_IP}" \
  "psql -t --host "${DB_ENDPOINT}" --username "${DATABASE_USER}" --dbname postgres --command='SELECT datname FROM pg_catalog.pg_database'" \
  | grep "${DATABASE_NAME}" \
  | sort \
  | head -n 1 \
  | xargs \
  | tr -d '\r')

# Create the database for the environment if it doesn't exist

if [ -n "${CONTROLLER_PUBLIC_IP}" ] && [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DATABASE_NAME}" ]; then
  echo "Creating database..."
  ssh -tt -i "~/.ssh/ab2d-${CMS_SHARED_ENV}.pem" \
    "${SSH_USERNAME}@${CONTROLLER_PUBLIC_IP}" \
    "createdb ${DATABASE_NAME} --host ${DB_ENDPOINT} --username ${DATABASE_USER}"
fi

#
# Deploy AWS application modules
#

if [ -n "${BUILD_NEW_IMAGES}" ]; then

  # Build and push API and worker to ECR

  echo "Build and push API and worker to ECR..."
  
  # Log on to ECR
      
  read -sra cmd < <(aws ecr get-login --no-include-email)
  pass="${cmd[5]}"
  unset cmd[4] cmd[5]
  "${cmd[@]}" --password-stdin <<< "$pass"

  # Build API and worker (if creating a new image)

  cd "${START_DIR}"
  cd ..
  make docker-build
  sleep 5

  # Build API docker image

  cd "${START_DIR}"
  cd ../api
  docker build \
    --tag "ab2d_api:latest" .

  # Build worker docker image

  cd "${START_DIR}"
  cd ../worker
  docker build \
    --tag "ab2d_worker:latest" .

  # Tag and push API docker image to ECR

  API_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
    --output text)
  if [ -z "${API_ECR_REPO_URI}" ]; then
    aws --region us-east-1 ecr create-repository \
        --repository-name "ab2d_api"
    API_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
      --output text)
  fi
  docker tag "ab2d_api:latest" "${API_ECR_REPO_URI}:latest"
  docker push "${API_ECR_REPO_URI}:latest"

  # Tag and push worker docker image to ECR

  WORKER_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
    --output text)
  if [ -z "${WORKER_ECR_REPO_URI}" ]; then
    aws --region us-east-1 ecr create-repository \
      --repository-name "ab2d_worker"
    WORKER_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
      --output text)
  fi
  docker tag "ab2d_worker:latest" "${WORKER_ECR_REPO_URI}:latest"
  docker push "${WORKER_ECR_REPO_URI}:latest"
   
else # use existing images

  echo "Using existing images..."

fi

#
# Switch context to terraform environment
#

echo "Switch context to terraform environment..."

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

#
# Get current known good ECS task definitions
#

echo "Get current known good ECS task definitions..."
CLUSTER_ARNS=$(aws --region us-east-1 ecs list-clusters \
  --query 'clusterArns' \
  --output text \
  | grep "/ab2d-${CMS_ENV}-api" \
  | xargs \
  | tr -d '\r')
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping getting current ECS task definitions, since there are no existing clusters"
else
  echo "TEST"
  API_TASK_DEFINITION=$(aws --region us-east-1 ecs describe-services \
    --services "ab2d-${CMS_ENV}-api" \
    --cluster "ab2d-${CMS_ENV}-api" \
    | grep "taskDefinition" \
    | head -1)
  API_TASK_DEFINITION=$(echo $API_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')
fi

#
# Get ECS task counts before making any changes
#

echo "Get ECS task counts before making any changes..."

# Define api_task_count
api_task_count() { aws --region us-east-1 ecs list-tasks --cluster "ab2d-${CMS_ENV}-api" | grep "\:task\/"|wc -l|tr -d ' '; }

# Get old api task count (if exists)
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_TASK_COUNT, since there are no existing clusters"
else
  OLD_API_TASK_COUNT=$(api_task_count)
fi

# set expected api task count

# LSH BEGIN 2019-11-20
# if [ -z "${CLUSTER_ARNS}" ]; then
#   EXPECTED_API_COUNT="2"
# else
#   EXPECTED_API_COUNT="$((OLD_API_TASK_COUNT*2))"
# fi
EXPECTED_API_COUNT="2"
# LSH END 2019-11-20

#
# Ensure Old Autoscaling Groups and containers are around to service requests
#

echo "Ensure Old Autoscaling Groups and containers are around to service requests..."

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_ASG, since there are no existing clusters"
else
  OLD_API_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}' | grep ab2d-$CMS_ENV)
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  terraform state rm module.app.aws_autoscaling_group.asg
  terraform state rm module.app.aws_launch_configuration.launch_config
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  OLD_API_CONTAINER_INSTANCES=$(aws --region us-east-1 ecs list-container-instances \
    --cluster "ab2d-${CMS_ENV}-api" \
    | grep container-instance)
fi

#
# Deploy API and Worker
#

echo "Deploy API and Worker..."

# Change to the "python3" directory

cd "${START_DIR}"
cd python3

# Create or get database host secret

DATABASE_HOST=$(./get-database-secret.py $CMS_ENV database_host $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_HOST}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_ENDPOINT}"
fi

# Create or get database port secret

DB_PORT=$(aws --region us-east-1 rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Port" \
  --output=text)

DATABASE_PORT=$(./get-database-secret.py $CMS_ENV database_port $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_PORT}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_PORT}"
fi

# Get database secret manager ARNs

DATABASE_HOST_SECRET_ARN=$(aws secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PORT_SECRET_ARN=$(aws secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_USER_SECRET_ARN=$(aws secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_user/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PASSWORD_SECRET_ARN=$(aws secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_password/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_NAME_SECRET_ARN=$(aws secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_name/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

# Change to the target terraform environment

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

if [ -z "${AUTOAPPROVE}" ]; then
    
  # Confirm with the caller prior to applying changes.

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --var "db_host=$DATABASE_HOST" \
    --var "db_port=$DATABASE_PORT" \
    --var "db_username=$DATABASE_USER" \
    --var "db_password=$DATABASE_PASSWORD" \
    --var "db_name=$DATABASE_NAME" \
    --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
    --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
    --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
    --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
    --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
    --var "deployer_ip_address=$DEPLOYER_IP_ADDRESS" \
    --target module.api
  
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --var "db_host=$DATABASE_HOST" \
    --var "db_port=$DATABASE_PORT" \
    --var "db_username=$DATABASE_USER" \
    --var "db_password=$DATABASE_PASSWORD" \
    --var "db_name=$DATABASE_NAME" \
    --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
    --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
    --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
    --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
    --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
    --target module.worker

else
    
  # Apply the changes without prompting

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --var "db_host=$DATABASE_HOST" \
    --var "db_port=$DATABASE_PORT" \
    --var "db_username=$DATABASE_USER" \
    --var "db_password=$DATABASE_PASSWORD" \
    --var "db_name=$DATABASE_NAME" \
    --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
    --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
    --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
    --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
    --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
    --var "deployer_ip_address=$DEPLOYER_IP_ADDRESS" \
    --target module.api \
    --auto-approve

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --var "db_host=$DATABASE_HOST" \
    --var "db_port=$DATABASE_PORT" \
    --var "db_username=$DATABASE_USER" \
    --var "db_password=$DATABASE_PASSWORD" \
    --var "db_name=$DATABASE_NAME" \
    --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
    --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
    --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
    --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
    --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
    --target module.worker \
    --auto-approve

fi

#
# Apply schedule autoscaling if applicable
#

echo "Apply schedule autoscaling if applicable..."
if [ -f ./autoscaling-schedule.tf ]; then
    
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target=aws_autoscaling_schedule.morning \
    --auto-approve

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target=aws_autoscaling_schedule.night \
    --auto-approve

fi

#
# Push authorized_keys file to deployment_controller
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

echo "Push authorized_keys file to deployment_controller..."
terraform taint \
  --allow-missing null_resource.authorized_keys_file
if [ -z "${AUTOAPPROVE}" ]; then
  # Confirm with the caller prior to applying changes.
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target null_resource.authorized_keys_file
else
  # Apply the changes without prompting
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target null_resource.authorized_keys_file \
    --auto-approve
fi

#
# Ensure new autoscaling group is running containers
#

echo "Ensure new autoscaling group is running containers..."

ACTUAL_API_COUNT=0
RETRIES_API=0

while [ "$ACTUAL_API_COUNT" -lt "$EXPECTED_API_COUNT" ]; do
  ACTUAL_API_COUNT=$(api_task_count)
  echo "Running API Tasks: $ACTUAL_API_COUNT, Expected: $EXPECTED_API_COUNT"
  if [ "$RETRIES_API" != "15" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    RETRIES_API=$(expr $RETRIES_API + 1)
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

#
# Deploy CloudWatch
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

echo "Deploy CloudWatch..."
if [ -z "${AUTOAPPROVE}" ]; then

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --target module.cloudwatch

else
    
  # Apply the changes without prompting

  terraform apply \
    --target module.cloudwatch \
    --var "ami_id=$AMI_ID" \
    --auto-approve

fi

#
# Deploy AWS WAF
#

if [ -z "${AUTOAPPROVE}" ]; then
    
  terraform apply \
    --target module.waf
  
else

  # Apply the changes without prompting
    
  terraform apply \
    --target module.waf \
    --auto-approve
  
fi

#
# Apply AWS Shield standard to the application load balancer
#

# Note that no change is actually made since AWS shield standard is automatically applied to
# the application load balancer. This section may be needed later if AWS Shield Advanced is
# applied instead.

if [ -z "${AUTOAPPROVE}" ]; then
    
  terraform apply \
    --target module.shield
  
else

  # Apply the changes without prompting
    
  terraform apply \
    --target module.shield \
    --auto-approve
  
fi
