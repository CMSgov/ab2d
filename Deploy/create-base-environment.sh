#!/bin/bash
set -e #Exit on first error
# set -x #Be verbose

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
  --seed-ami-product-code=*)
  SEED_AMI_PRODUCT_CODE="${i#*=}"
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
esac
done

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ] || [ -z "${SHARED_ENVIRONMENT}" ] || [ -z "${VPC_ID}" ] || [ -z "${SEED_AMI_PRODUCT_CODE}" ] || [ -z "${DATABASE_SECRET_DATETIME}" ]; then
  echo "Try running the script like one of these options:"
  echo "./create-base-environment.sh --environment=dev --shared-environment=sbdemo-shared --vpc-id={vpc id} --seed-ami-product-code={aw0evgkw8e5c1q413zgy5pjce|gold disk product code} --database-secret-datetime={YYYY-MM-DD-HH-MM-SS}"
  echo "./create-base-environment.sh --environment=dev --vpc-id={vpc id} --seed-ami-product-code={aw0evgkw8e5c1q413zgy5pjce|gold disk product code} --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --debug-level={TRACE|DEBUG|INFO|WARN|ERROR}"
  exit 1
fi

#
# Set environment
#

export AWS_PROFILE="${CMS_SHARED_ENV}"

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

export AWS_PROFILE="${CMS_SHARED_ENV}"

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

terraform init
terraform validate

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

export AWS_PROFILE="${CMS_ENV}"

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
  export AWS_PROFILE="${CMS_SHARED_ENV}"
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

#
# Create networking
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
# Create first public subnet
#

cd "${START_DIR}"
cd python3

SUBNET_PUBLIC_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-public-subnet-01" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then

  # Get next available CIDR block
    
  SUBNET_PUBLIC_1_CIDR_BLOCK=$(./get-new-cidr.py $CMS_ENV $VPC_ID)

  # Create first public subnet

  echo "Creating first public subnet..."

  SUBNET_PUBLIC_1_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $SUBNET_PUBLIC_1_CIDR_BLOCK \
    --availability-zone us-east-1a \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)

  # Add name tag to first public subnet

  aws ec2 create-tags \
    --resources $SUBNET_PUBLIC_1_ID \
    --tags "Key=Name,Value=ab2d-public-subnet-01" \
    --region us-east-1

fi

#
# Create second public subnet
#

cd "${START_DIR}"
cd python3

SUBNET_PUBLIC_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-public-subnet-02" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then

  # Get next available CIDR block
    
  SUBNET_PUBLIC_2_CIDR_BLOCK=$(./get-new-cidr.py $CMS_ENV $VPC_ID)

  # Create second public subnet

  echo "Creating second public subnet..."
  
  SUBNET_PUBLIC_2_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $SUBNET_PUBLIC_2_CIDR_BLOCK \
    --availability-zone us-east-1b \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)

  # Add name tag to first public subnet

  aws ec2 create-tags \
    --resources $SUBNET_PUBLIC_2_ID \
    --tags "Key=Name,Value=ab2d-public-subnet-02" \
    --region us-east-1

fi

#
# Create first private subnet for target environment
#

cd "${START_DIR}"
cd python3

SUBNET_PRIVATE_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-01" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then

  # Get next available CIDR block
    
  SUBNET_PRIVATE_1_CIDR_BLOCK=$(./get-new-cidr.py $CMS_ENV $VPC_ID)

  # Create first private subnet

  echo "Creating first private subnet..."

  SUBNET_PRIVATE_1_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $SUBNET_PRIVATE_1_CIDR_BLOCK \
    --availability-zone us-east-1a \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)

  # Add name tag to first private subnet

  aws ec2 create-tags \
    --resources $SUBNET_PRIVATE_1_ID \
    --tags "Key=Name,Value=ab2d-$CMS_ENV-private-subnet-01" \
    --region us-east-1

fi

#
# Create second private subnet for target environment
#

cd "${START_DIR}"
cd python3

SUBNET_PRIVATE_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-02" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then

  # Get next available CIDR block
    
  SUBNET_PRIVATE_2_CIDR_BLOCK=$(./get-new-cidr.py $CMS_ENV $VPC_ID)

  # Create second private subnet

  echo "Creating second private subnet..."
  
  SUBNET_PRIVATE_2_ID=$(aws ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block $SUBNET_PRIVATE_2_CIDR_BLOCK \
    --availability-zone us-east-1b \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)

  # Add name tag to first private subnet

  aws ec2 create-tags \
    --resources $SUBNET_PRIVATE_2_ID \
    --tags "Key=Name,Value=ab2d-$CMS_ENV-private-subnet-02" \
    --region us-east-1

fi

#
# Create internet gateway
#

IGW_ID=$(aws ec2 describe-internet-gateways \
  --filters "Name=tag:Name,Values=ab2d-igw" \
  --query "InternetGateways[*].InternetGatewayId" \
  --output text)

if [ -z "${IGW_ID}" ]; then
    
  # Create internet gateway

  echo "Creating internet gateway..."
  IGW_ID=$(aws ec2 create-internet-gateway \
    --query 'InternetGateway.{InternetGatewayId:InternetGatewayId}' \
    --output text \
    --region us-east-1)

  # Add name tag to internet gateway

  aws ec2 create-tags \
    --resources $IGW_ID \
    --tags "Key=Name,Value=ab2d-igw" \
    --region us-east-1

fi

#
# Attach internet gateway to VPC
#

IGW_ATTACHED=$(aws ec2 describe-internet-gateways \
  --filters "Name=tag:Name,Values=ab2d-igw" \
  --query "InternetGateways[*].Attachments" \
  --output text)

if [ -z "${IGW_ATTACHED}" ]; then
  echo "Attach internet gateway to VPC..."
  aws --region us-east-1 ec2 attach-internet-gateway \
    --internet-gateway-id $IGW_ID \
    --vpc-id $VPC_ID
fi

#
# Create a custom route table for internet gateway
#

IGW_ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-igw-rt" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ID}" ]; then

  echo "Creating a custom route table for internet gateway..."
    
  IGW_ROUTE_TABLE_ID=$(aws ec2 create-route-table \
    --vpc-id $VPC_ID \
    --query 'RouteTable.{RouteTableId:RouteTableId}' \
    --output text \
    --region us-east-1)
  
  aws ec2 create-tags \
    --resources $IGW_ROUTE_TABLE_ID \
    --tags "Key=Name,Value=ab2d-igw-rt" \
    --region us-east-1
  
fi

# Add route for internet gateway to the custom route table for public subnets

IGW_ROUTE_TABLE_IGW_ROUTE_TARGET=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-igw-rt" \
  --query "RouteTables[*].Routes[?GatewayId=='$IGW_ID'].GatewayId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_IGW_ROUTE_TARGET}" ]; then

  echo "Adding route for internet gateway to the custom route table for public subnets..."
    
  aws --region us-east-1 ec2 create-route \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $IGW_ID \
    --route-table-id $IGW_ROUTE_TABLE_ID
  
fi

# Associate the first public subnet with the custom route table for internet gateway

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-igw-rt" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_1_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID}" ]; then

  echo "Associating the first public subnet with the custom route table for internet gateway..."
    
  aws ec2 associate-route-table  \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --route-table-id $IGW_ROUTE_TABLE_ID \
    --region us-east-1
  
fi

# Associate the second public subnet with the custom route table for internet gateway

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-igw-rt" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_2_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID}" ]; then

  echo "Associating the second public subnet with the custom route table for internet gateway..."
    
  aws ec2 associate-route-table  \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --route-table-id $IGW_ROUTE_TABLE_ID \
    --region us-east-1
  
fi

#
# Enable Auto-assign Public IP on public subnets
#

# Enable Auto-assign Public IP on the first public subnet

SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-public-subnet-01" \
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
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-public-subnet-02" \
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
# Create first NAT Gateway
#

# Allocate Elastic IP Address for first NAT Gateway

EIP_ALLOC_1_ID=$(aws ec2 describe-addresses \
  --filters "Name=tag:Name,Values=ab2d-ngw-eip-1" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_1_ID}" ]; then

  # Allocate Elastic IP Address for first NAT Gateway
    
  echo "Allocating Elastic IP Address for first NAT Gateway..."

  EIP_ALLOC_1_ID=$(aws ec2 allocate-address \
    --domain vpc \
    --query '{AllocationId:AllocationId}' \
    --output text \
    --region us-east-1)

  # Add name tag to Elastic IP Address for first NAT Gateway
  
  aws ec2 create-tags \
    --resources $EIP_ALLOC_1_ID \
    --tags "Key=Name,Value=ab2d-ngw-eip-1" \
    --region us-east-1

fi

# Create first NAT Gateway

NAT_GW_1_ID=$(aws ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-ngw-1" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_1_ID}" ]; then

  # Create first NAT Gateway
    
  NAT_GW_1_ID=$(aws ec2 create-nat-gateway \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --allocation-id $EIP_ALLOC_1_ID \
    --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
    --output text \
    --region us-east-1)

  # Add name tag to first NAT Gateway

  aws ec2 create-tags \
    --resources $NAT_GW_1_ID \
    --tags "Key=Name,Value=ab2d-ngw-1" \
    --region us-east-1

  # Wait for first NAT Gateway to become available
  
  SECONDS_COUNT=0
  LAST_SECONDS_COUNT=0
  WAIT_SECONDS=5
  STATE='PENDING'
  echo "Waiting for first NAT gateway to become available"
  until [[ $STATE == 'AVAILABLE' ]]; do
    INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
    echo $INTERVAL
    if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
      STATE=$(aws ec2 describe-nat-gateways \
        --nat-gateway-ids $NAT_GW_1_ID \
        --query 'NatGateways[*].{State:State}' \
        --output text \
        --region us-east-1)
      STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
      echo "Waiting for first NAT gateway to become available"
      LAST_SECONDS_COUNT=$SECONDS_COUNT
    fi
    SECONDS_COUNT=$[SECONDS_COUNT+1]
    sleep 1
  done

fi

# Create a custom route table for the first NAT Gateway

ROUTE_TABLE_FOR_NGW_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-ngw-rt-1" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_1_ID}" ]; then

  echo "Creating a custom route table for the first NAT Gateway..."
    
  ROUTE_TABLE_FOR_NGW_1_ID=$(aws ec2 create-route-table \
    --vpc-id $VPC_ID \
    --query 'RouteTable.{RouteTableId:RouteTableId}' \
    --output text \
    --region us-east-1)

  aws ec2 create-tags \
    --resources $ROUTE_TABLE_FOR_NGW_1_ID \
    --tags "Key=Name,Value=ab2d-ngw-rt-1" \
    --region us-east-1
  
fi

# Create route to the first NAT Gateway for the custom route table

NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-ngw-rt-1" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_1_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET}" ]; then

  echo "Creating route to the first NAT Gateway for the custom route table..."
    
  aws ec2 create-route \
    --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $NAT_GW_1_ID \
    --region us-east-1
  
fi

# Associate the first private subnet with the custom route table for the first NAT Gateway

NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-ngw-rt-1" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_1_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID}" ]; then

  echo "Associate the first private subnet with the custom route table for the first NAT Gateway..."

  aws ec2 associate-route-table  \
    --subnet-id $SUBNET_PRIVATE_1_ID \
    --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
    --region us-east-1
  
fi

#
# Create second NAT Gateway
#

# Allocate Elastic IP Address for first NAT Gateway

EIP_ALLOC_2_ID=$(aws ec2 describe-addresses \
  --filters "Name=tag:Name,Values=ab2d-ngw-eip-2" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_2_ID}" ]; then

  # Allocate Elastic IP Address for first NAT Gateway
    
  echo "Allocating Elastic IP Address for first NAT Gateway..."

  EIP_ALLOC_2_ID=$(aws ec2 allocate-address \
    --domain vpc \
    --query '{AllocationId:AllocationId}' \
    --output text \
    --region us-east-1)

  # Add name tag to Elastic IP Address for first NAT Gateway
  
  aws ec2 create-tags \
    --resources $EIP_ALLOC_2_ID \
    --tags "Key=Name,Value=ab2d-ngw-eip-2" \
    --region us-east-1

fi

# Create second NAT Gateway

NAT_GW_2_ID=$(aws ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-ngw-2" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_2_ID}" ]; then

  # Create second NAT Gateway
    
  NAT_GW_2_ID=$(aws ec2 create-nat-gateway \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --allocation-id $EIP_ALLOC_2_ID \
    --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
    --output text \
    --region us-east-1)

  # Add name tag to second NAT Gateway

  aws ec2 create-tags \
    --resources $NAT_GW_2_ID \
    --tags "Key=Name,Value=ab2d-ngw-2" \
    --region us-east-1

  # Wait for second NAT Gateway to become available
  
  SECONDS_COUNT=0
  LAST_SECONDS_COUNT=0
  WAIT_SECONDS=5
  STATE='PENDING'
  echo "Waiting for second NAT gateway to become available"
  until [[ $STATE == 'AVAILABLE' ]]; do
    INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
    echo $INTERVAL
    if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
      STATE=$(aws ec2 describe-nat-gateways \
        --nat-gateway-ids $NAT_GW_2_ID \
        --query 'NatGateways[*].{State:State}' \
        --output text \
        --region us-east-1)
      STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
      echo "Waiting for second NAT gateway to become available"
      LAST_SECONDS_COUNT=$SECONDS_COUNT
    fi
    SECONDS_COUNT=$[SECONDS_COUNT+1]
    sleep 1
  done

fi
 
# Create a custom route table for the second NAT Gateway

ROUTE_TABLE_FOR_NGW_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-ngw-rt-2" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_2_ID}" ]; then

  echo "Creating a custom route table for the second NAT Gateway..."
    
  ROUTE_TABLE_FOR_NGW_2_ID=$(aws ec2 create-route-table \
    --vpc-id $VPC_ID \
    --query 'RouteTable.{RouteTableId:RouteTableId}' \
    --output text \
    --region us-east-1)

  aws ec2 create-tags \
    --resources $ROUTE_TABLE_FOR_NGW_2_ID \
    --tags "Key=Name,Value=ab2d-ngw-rt-2" \
    --region us-east-1
  
fi

# Create route to the second NAT Gateway for the custom route table

NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-ngw-rt-2" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_2_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET}" ]; then

  echo "Creating route to the second NAT Gateway for the custom route table..."
    
  aws ec2 create-route \
    --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $NAT_GW_2_ID \
    --region us-east-1
  
fi

# Associate the second private subnet with the custom route table for the second NAT Gateway

NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-ngw-rt-2" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_2_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID}" ]; then

  echo "Associate the second private subnet with the custom route table for the second NAT Gateway..."

  aws ec2 associate-route-table  \
    --subnet-id $SUBNET_PRIVATE_2_ID \
    --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
    --region us-east-1
  
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
    --owners aws-marketplace \
    --filters "Name=product-code,Values=${SEED_AMI_PRODUCT_CODE}" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')
  
  # Get first public subnet
  SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-public-subnet-01" \
    --query "Subnets[0].SubnetId" \
    --output text)
  
  # Create AMI for application nodes
  cd "${START_DIR}"
  cd packer/app
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
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

export AWS_PROFILE="${CMS_ENV}"

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
    --owners aws-marketplace \
    --filters "Name=product-code,Values=${SEED_AMI_PRODUCT_CODE}" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')

  # Get first public subnet
  SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-public-subnet-01" \
    --query "Subnets[0].SubnetId" \
    --output text)

  # Create AMI for Jenkins
  cd "${START_DIR}"
  cd packer/jenkins
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
	 --var seed_ami=$SEED_AMI \
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

#
# Create "auto.tfvars" files
#

# Create "auto.tfvars" file for shared components

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

DB_ENDPOINT=$(aws --region us-east-1 rds describe-db-instances --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" --output=text)

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
else
  PRIVATE_SUBNETS_OUTPUT=$(aws ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-*-private-subnet-*" \
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

##########################
# Deploy shared components
##########################

#
# Configure shared environment
#

export AWS_PROFILE="${CMS_SHARED_ENV}"

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

#
# Deploy S3
#

echo "Deploying S3..."

# Create "ab2d-cloudtrail" bucket

aws s3api create-bucket --bucket ab2d-cloudtrail --region us-east-1

# Block public access on bucket

aws s3api put-public-access-block \
  --bucket ab2d-cloudtrail \
  --region us-east-1 \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "ab2d-cloudtrail" bucket

aws s3api put-bucket-acl \
  --bucket ab2d-cloudtrail \
  --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
  --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery

# Add bucket policy to the "ab2d-cloudtrail" S3 bucket

cd "${START_DIR}"
cd aws/s3-bucket-policies

aws s3api put-bucket-policy \
  --bucket ab2d-cloudtrail \
  --policy file://ab2d-cloudtrail-bucket-policy.json

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
DB_ENDPOINT=$(aws --region us-east-1 rds describe-db-instances --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" --output=text)
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
  --target module.controller \
  --auto-approve

######################################
# Deploy target environment components
######################################

export AWS_PROFILE="${CMS_ENV}"

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
    --target module.efs --auto-approve
fi

#
# Deploy AWS application modules
#

cd "${START_DIR}"
./deploy.sh \
  --environment="${CMS_ENV}" \
  --shared-environment="${CMS_SHARED_ENV}" \
  --ami="${AMI_ID}" \
  --ssh-username="${SSH_USERNAME}" \
  --database-secret-datetime="${DATABASE_SECRET_DATETIME}" \
  --auto-approve
