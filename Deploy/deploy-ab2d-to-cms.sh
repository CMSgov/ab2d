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
  --ecr-repo-environment=*)
  ECR_REPO_ENVIRONMENT="${i#*=}"
  CMS_ECR_REPO_ENV=$(echo $ECR_REPO_ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --region=*)
  REGION="${i#*=}"
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

export AWS_PROFILE="${CMS_ENV}"

# Verify that VPC ID exists

VPC_EXISTS=$(aws --region "${REGION}" ec2 describe-vpcs \
  --query "Vpcs[?VpcId=='$VPC_ID'].VpcId" \
  --output text)

if [ -z "${VPC_EXISTS}" ]; then
  echo "*********************************************************"
  echo "ERROR: VPC ID does not exist for the target AWS profile."
  echo "*********************************************************"
  exit 1
fi

#
# Get KMS key id for target environment (if exists)
#

export AWS_PROFILE="${CMS_ENV}"

KMS_KEY_ID=$(aws --region "${REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${KMS_KEY_ID}" ]; then
  KMS_KEY_STATE=$(aws --region "${REGION}" kms describe-key \
    --key-id alias/ab2d-kms \
    --query "KeyMetadata.KeyState" \
    --output text)
fi

#
# Get MGMT KMS key id for management environment (if exists)
#

export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

MGMT_KMS_KEY_ID=$(aws --region "${REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${MGMT_KMS_KEY_ID}" ]; then
  MGMT_KMS_KEY_STATE=$(aws --region "${REGION}" kms describe-key \
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

# Set AWS profile to the target environment

export AWS_PROFILE="${CMS_ENV}"

# Initialize and validate terraform for the shared components

echo "**************************************************************"
echo "Initialize and validate terraform for the shared components..."
echo "**************************************************************"

cd "${START_DIR}"
cd terraform/environments/$CMS_SHARED_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-automation" \
  -backend-config="key=${CMS_SHARED_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="encrypt=true"

terraform validate

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-automation" \
  -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="encrypt=true"

terraform validate

# Set AWS profile to the management environment

export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

# Initialize and validate terraform for the management environment

echo "*******************************************************************"
echo "Initialize and validate terraform for the management environment..."
echo "*******************************************************************"

cd "${START_DIR}"
cd "terraform/environments/$CMS_ECR_REPO_ENV-shared"

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ECR_REPO_ENV}-automation" \
  -backend-config="key=${CMS_ECR_REPO_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Deploy or enable KMS for target environment
#

# If target environment KMS key exists, enable it; otherwise, create it

export AWS_PROFILE="${CMS_ENV}"

if [ -n "$KMS_KEY_ID" ]; then
  echo "Enabling KMS key..."
  cd "${START_DIR}"
  cd python3
  ./enable-kms-key.py $KMS_KEY_ID
else
  echo "Deploying KMS..."
  cd "${START_DIR}"
  cd terraform/environments/$CMS_SHARED_ENV
  terraform apply \
    --target module.kms --auto-approve
fi

# Get target KMS key id

KMS_KEY_ID=$(aws --region "${REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

#
# Deploy or enable KMS for management environment
#

# If management environment KMS key exists, enable it; otherwise, create it

export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

if [ -n "$MGMT_KMS_KEY_ID" ]; then
  echo "Enabling KMS key..."
  cd "${START_DIR}"
  cd python3
  ./enable-kms-key.py $MGMT_KMS_KEY_ID
else
  echo "Deploying KMS..."
  cd "${START_DIR}"
  cd "terraform/environments/${CMS_ECR_REPO_ENV}-shared"
  terraform apply \
    --target module.kms --auto-approve
fi

# Get MGMT KMS key id

MGMT_KMS_KEY_ID=$(aws --region "${REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

#
# Create or get secrets
#

# Set AWS profile to the target environment

export AWS_PROFILE="${CMS_ENV}"

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

# Create or get database name secret

DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_NAME}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
fi

# Create or get bfd url secret

BFD_URL=$(./get-database-secret.py $CMS_ENV bfd_url $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_URL}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV bfd_url $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  BFD_URL=$(./get-database-secret.py $CMS_ENV bfd_url $DATABASE_SECRET_DATETIME)
fi

# Create or get bfd keystore location secret

BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV bfd_keystore_location $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV bfd_keystore_location $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV bfd_keystore_location $DATABASE_SECRET_DATETIME)
fi

# Create or get bfd keystore password secret

BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV bfd_keystore_password $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV bfd_keystore_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV bfd_keystore_password $DATABASE_SECRET_DATETIME)
fi

# Create or get hicn hash pepper secret

HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV hicn_hash_pepper $DATABASE_SECRET_DATETIME)
if [ -z "${HICN_HASH_PEPPER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV hicn_hash_pepper $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV hicn_hash_pepper $DATABASE_SECRET_DATETIME)
fi

# Create or get hicn hash iter secret

HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV hicn_hash_iter $DATABASE_SECRET_DATETIME)
if [ -z "${HICN_HASH_ITER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV hicn_hash_iter $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV hicn_hash_iter $DATABASE_SECRET_DATETIME)
fi

# Create or get new relic app name secret

NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV new_relic_app_name $DATABASE_SECRET_DATETIME)
if [ -z "${NEW_RELIC_APP_NAME}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV new_relic_app_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV new_relic_app_name $DATABASE_SECRET_DATETIME)
fi

# Create or get new relic license key secret

NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV new_relic_license_key $DATABASE_SECRET_DATETIME)
if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV new_relic_license_key $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV new_relic_license_key $DATABASE_SECRET_DATETIME)
fi

# If any databse secret produced an error, exit the script

if [ "${DATABASE_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_URL}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_KEYSTORE_LOCATION}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_KEYSTORE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${HICN_HASH_PEPPER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${HICN_HASH_ITER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${NEW_RELIC_APP_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Configure networking and get network attributes
#

# Enable DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws --region "${REGION}" ec2 describe-vpc-attribute \
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

SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
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

SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-b" \
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

SUBNET_PRIVATE_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
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

SUBNET_PRIVATE_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
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

IGW_ID=$(aws --region "${REGION}" ec2 describe-internet-gateways \
  --filters "Name=tag:Name,Values=${CMS_ENV}" \
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

IGW_ROUTE_TABLE_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ID}" ]; then

  echo "ERROR: custom route table for internet gateway not found..."
  exit 1
  
fi

# Verify internet gateway route is present in the custom route table for internet gateway

echo "Verifying internet gateway route is present in the custom route table for internet gateway..."

IGW_ROUTE_TABLE_IGW_ROUTE_TARGET=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public" \
  --query "RouteTables[*].Routes[?GatewayId=='$IGW_ID'].GatewayId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_IGW_ROUTE_TARGET}" ]; then

  echo "ERROR: internet gateway route is not present in the custom route table for internet gateway..."
  exit 1
  
fi

# Verify the association of the first public subnet with the custom route table for internet gateway

echo "Verifying the association of the first public subnet with the custom route table for internet gateway..."

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_1_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID}" ]; then

  echo "ERROR: the first public subnet is not associated with the custom route table for internet gateway..."
  exit 1
  
fi

# Verify the association of the second public subnet with the custom route table for internet gateway

echo "Verifying the association of the second public subnet with the custom route table for internet gateway..."

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public" \
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

SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
  --query "Subnets[?MapPublicIpOnLaunch].MapPublicIpOnLaunch" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH}" ]; then

  echo "Enabling Auto-assign Public IP on the first public subnet..."
    
  aws ec2 modify-subnet-attribute \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --map-public-ip-on-launch \
    --region "${REGION}"

fi

# Enable Auto-assign Public IP on the second public subnet

SUBNET_PUBLIC_2_MAP_PUBLIC_IP_ON_LAUNCH=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-b" \
  --query "Subnets[?MapPublicIpOnLaunch].MapPublicIpOnLaunch" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_MAP_PUBLIC_IP_ON_LAUNCH}" ]; then

  echo "Enabling Auto-assign Public IP on the second public subnet..."
    
  aws ec2 modify-subnet-attribute \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --map-public-ip-on-launch \
    --region "${REGION}"
  
fi

#
# Verify first NAT Gateway
#

# Verify Elastic IP for first NAT Gateway

echo "Verifying Elastic IP for first NAT Gateway..."

EIP_ALLOC_1_ID=$(aws --region "${REGION}" ec2 describe-addresses \
  --filters "Name=tag:Name,Values=${CMS_ENV}-nat-gateway-a" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_1_ID}" ]; then

  echo "ERROR: the elastic IP for the first NAT Gateway is not found..."
  exit 1

fi

# Verify first NAT Gateway

echo "Verifying first NAT Gateway..."

NAT_GW_1_ID=$(aws --region "${REGION}" ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=${CMS_ENV}-a" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_1_ID}" ]; then

  echo "ERROR: the first NAT Gateway is not found..."
  exit 1

fi

# Verify the custom route table for the first NAT Gateway

echo "Verifying the custom route table for the first NAT Gateway..."

ROUTE_TABLE_FOR_NGW_1_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_1_ID}" ]; then

  echo "ERROR: the custom route table for the first NAT Gateway is not found..."
  exit 1
  
fi

# Verify first NAT gateway route is present in the custom route table for the first NAT gateway

echo "Verifying first NAT gateway route is present in the custom route table for the first NAT gateway..."

NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_1_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET}" ]; then

  echo "ERROR: first NAT gateway route is not present in the custom route table for first NAT gateway..."
  exit 1
  
fi

# Verify that association of the first private subnet with the custom route table for the first NAT Gateway

echo "Verifying that association of the first private subnet with the custom route table for the first NAT Gateway..."

NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
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

EIP_ALLOC_2_ID=$(aws --region "${REGION}" ec2 describe-addresses \
  --filters "Name=tag:Name,Values=${CMS_ENV}-nat-gateway-b" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_2_ID}" ]; then

  echo "ERROR: the elastic IP for the second NAT Gateway is not found..."
  exit 1

fi

# Verify second NAT Gateway

echo "Verifying second NAT Gateway..."

NAT_GW_2_ID=$(aws --region "${REGION}" ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=${CMS_ENV}-b" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_2_ID}" ]; then

  echo "ERROR: the second NAT Gateway is not found..."
  exit 1

fi
 
# Verify the custom route table for the second NAT Gateway

echo "Verifying the custom route table for the second NAT Gateway..."

ROUTE_TABLE_FOR_NGW_2_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_2_ID}" ]; then

  echo "ERROR: the custom route table for the first NAT Gateway is not found..."
  exit 1
  
fi

# Verify second NAT gateway route is present in the custom route table for the second NAT gateway

echo "Verifying second NAT gateway route is present in the custom route table for the second NAT gateway..."

NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_2_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET}" ]; then

  echo "ERROR: second NAT gateway route is not present in the custom route table for second NAT gateway..."
  exit 1
  
fi

# Verify that association of the second private subnet with the custom route table for the second NAT Gateway

echo "Verifying that association of the second private subnet with the custom route table for the second NAT Gateway..."

NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_2_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID}" ]; then

  echo "ERROR: the second private subnet is not associated with the custom route table for the second NAT Gateway..."
  exit 1
  
fi

# Create or verify the VPN access security group

echo "Verifying VPN access security group..."

VPN_ACCESS_SECURITY_GROUP_ID=$(aws --region "${REGION}" ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=VPN access" \
  --query "SecurityGroups[*].GroupId" \
  --output text)

if [ -z "${VPN_ACCESS_SECURITY_GROUP_ID}" ]; then
  echo "Creating VPN access security group..."
  VPN_ACCESS_SECURITY_GROUP_ID=$(aws --region "${REGION}" ec2 create-security-group \
    --description "VPN access" \
    --group-name "VPN access" \
    | jq --raw-output ".GroupId")
fi

# Apply or verify tag for VPN access security group

VPN_ACCESS_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-security-groups \
  --filters "Name=tag:Name,Values=VPN access" \
  --query "SecurityGroups[*].GroupId" \
  --output text)

if [ -z "${VPN_ACCESS_TAG_EXISTS}" ]; then
  echo "Setting tag for vpc..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $VPN_ACCESS_SECURITY_GROUP_ID \
    --tags "Key=Name,Value=VPN access"
else
  echo "VPN access tag verified..."
fi

# Add or verify ingress rule for VPN

VPN_ACCESS_SECURITY_GROUP_INGRESS_RULE_EXISTS=$(aws --region "${REGION}" ec2 describe-security-groups \
  --filters "Name=group-id,Values=${VPN_ACCESS_SECURITY_GROUP_ID}" \
  --query "SecurityGroups[*].IpPermissions[*].IpRanges[?Description == 'ingress from VPN']".Description \
  --output text)

if [ -z "${VPN_ACCESS_SECURITY_GROUP_INGRESS_RULE_EXISTS}" ]; then
  echo "Adding ingress rule for VPN..."
  aws --region "${REGION}" ec2 authorize-security-group-ingress \
    --group-id "${VPN_ACCESS_SECURITY_GROUP_ID}" \
    --ip-permissions IpProtocol=-1,IpRanges='[{CidrIp=10.232.32.0/19,Description="ingress from VPN"}]'
else
  echo "Ingress rule for VPN verified..."
fi

#
# AMI Generation for application nodes
#

# Set AMI_ID if it already exists for the deployment

echo "Set AMI_ID if it already exists for the deployment..."
AMI_ID=$(aws --region "${REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# If no AMI is specified then create a new one

echo "If no AMI is specified then create a new one..."
if [ -z "${AMI_ID}" ]; then
    
  # Get the latest seed AMI
  SEED_AMI=$(aws --region "${REGION}" ec2 describe-images \
    --owners "${OWNER}" \
    --filters "Name=name,Values=EAST-RH 7*" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')
    
  # Create AMI for application nodes
  cd "${START_DIR}"
  cd packer/app
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
    --var environment="${CMS_ENV}" \
    --var region="${REGION}" \
    --var ec2_instance_type=$EC2_INSTANCE_TYPE \
    --var vpc_id=$VPC_ID \
    --var subnet_public_1_id=$SUBNET_PUBLIC_1_ID \
    --var my_ip_address=$IP \
    --var ssh_username=$SSH_USERNAME \
    --var git_commit_hash=$COMMIT \
    app.json  2>&1 | tee output.txt
  AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
  # Add name tag to AMI
  aws --region "${REGION}" ec2 create-tags \
    --resources $AMI_ID \
    --tags "Key=Name,Value=ab2d-ami"
fi

#
# AMI Generation for Jenkins node
#

# Set profile to the management AWS account

export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

# Set JENKINS_AMI_ID if it already exists for the deployment

echo "Set JENKINS_AMI_ID if it already exists for the deployment..."
JENKINS_AMI_ID=$(aws --region "${REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-jenkins-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# If no AMI is specified then create a new one

echo "If no AMI is specified then create a new one..."
if [ -z "${JENKINS_AMI_ID}" ]; then

  # Get the latest seed AMI

  SEED_AMI=$(aws --region "${REGION}" ec2 describe-images \
    --owners "${OWNER}" \
    --filters "Name=name,Values=EAST-RH 7*" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')

  # Get VPC ID for the management AWS account

  echo "Getting VPC ID for the management AWS account..."

  MGMT_VPC_ID=$(aws --region "${REGION}" ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=${CMS_ECR_REPO_ENV}" \
    --query "Vpcs[*].[VpcId]" \
    --output text)

  if [ -z "${MGMT_VPC_ID}" ]; then
    echo "*************************************************************"
    echo "ERROR: MGMT VPC ID does not exist for the target AWS profile."
    echo "*************************************************************"
    exit 1
  fi

  # Get first subnet id for the management AWS account

  echo "Getting first public subnet id for the management AWS account..."

  MGMT_SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
    --filters "Name=tag:Name,Values=${CMS_ECR_REPO_ENV}-public-a" \
    --query "Subnets[*].SubnetId" \
    --output text)

  if [ -z "${MGMT_SUBNET_PUBLIC_1_ID}" ]; then

    echo "ERROR: public subnet #1 for the management AWS account not found..."
    exit 1

  fi

  # Create AMI for Jenkins

  cd "${START_DIR}"
  cd packer/jenkins
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
    --var region="${REGION}" \
    --var ec2_instance_type=$EC2_INSTANCE_TYPE \
    --var vpc_id=$MGMT_VPC_ID \
    --var subnet_public_1_id=$MGMT_SUBNET_PUBLIC_1_ID \
    --var my_ip_address=$IP \
    --var ssh_username=$SSH_USERNAME \
    --var git_commit_hash=$COMMIT \
    app.json  2>&1 | tee output.txt
  JENKINS_AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
  # Add name tag to AMI

  aws --region "${REGION}" ec2 create-tags \
    --resources $JENKINS_AMI_ID \
    --tags "Key=Name,Value=ab2d-jenkins-ami"

fi

# Get deployer IP address

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

#
# Create "auto.tfvars" files
#

# Set profile to the target AWS account

export AWS_PROFILE="${CMS_ENV}"

# Create "auto.tfvars" file for shared components

cd "${START_DIR}"
cd terraform/environments/$CMS_SHARED_ENV

DB_ENDPOINT=$(aws --region "${REGION}" rds describe-db-instances \
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
  PRIVATE_SUBNETS_OUTPUT=$(aws --region "${REGION}" ec2 describe-subnets \
    --filters "Name=tag:Name,Values=*-private-*" \
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
cd terraform/environments/$CMS_ENV

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
cd terraform/environments/$CMS_SHARED_ENV

#
# Create cloudtrail bucket
#

echo "Creating "${CMS_ENV}-cloudtrail" bucket..."

aws --region "${REGION}" s3api create-bucket \
  --bucket "${CMS_ENV}-cloudtrail"

# Block public access on bucket

aws --region "${REGION}" s3api put-public-access-block \
  --bucket "${CMS_ENV}-cloudtrail" \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cloudtrail" bucket

aws --region "${REGION}" s3api put-bucket-acl \
  --bucket "${CMS_ENV}-cloudtrail" \
  --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
  --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery

# Add bucket policy to the "cloudtrail" S3 bucket

cd "${START_DIR}"
cd terraform/environments/$CMS_SHARED_ENV

aws --region "${REGION}" s3api put-bucket-policy \
  --bucket "${CMS_ENV}-cloudtrail" \
  --policy file://ab2d-cloudtrail-bucket-policy.json

#
# Create dev S3 bucket
#

echo "Deploying dev S3 bucket..."

cd "${START_DIR}"
cd terraform/environments/$CMS_SHARED_ENV

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

DB_ENDPOINT=$(aws --region "${REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

cd "${START_DIR}"
cd terraform/environments/$CMS_SHARED_ENV
rm -f generated/.pgpass

# Generate ".pgpass" file

cd "${START_DIR}"
cd terraform/environments/$CMS_SHARED_ENV
mkdir -p generated

# Add default database

echo "${DB_ENDPOINT}:5432:postgres:${DATABASE_USER}:${DATABASE_PASSWORD}" > generated/.pgpass

# *** TO DO *** BEGIN: add all environments to the .pgpass file

# Get the names of all deployed private subnets

# PRIVATE_SUBNETS=$(aws --region "${REGION}" ec2 describe-subnets \
#   --filters "Name=tag:Name,Values=*-private-subnet*" \
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
#   cd terraform/environments/$CMS_SHARED_ENV
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
cd terraform/environments/$CMS_ENV

#
# Deploy EFS
#

# Get files system id (if exists)

EFS_FS_ID=$(aws --region "${REGION}" efs describe-file-systems \
  --query="FileSystems[?CreationToken=='${CMS_ENV}-efs'].FileSystemId" \
  --output=text)

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

CONTROLLER_PRIVATE_IP=$(aws --region "${REGION}" ec2 describe-instances \
  --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
  --output text)

# Determine if the database for the environment exists

DB_NAME_IF_EXISTS=$(ssh -tt -i "~/.ssh/${CMS_ENV}.pem" \
  "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}" \
  "psql -t --host "${DB_ENDPOINT}" --username "${DATABASE_USER}" --dbname postgres --command='SELECT datname FROM pg_catalog.pg_database'" \
  | grep "${DATABASE_NAME}" \
  | sort \
  | head -n 1 \
  | xargs \
  | tr -d '\r')

# Create the database for the environment if it doesn't exist

if [ -n "${CONTROLLER_PRIVATE_IP}" ] && [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DATABASE_NAME}" ]; then
  echo "Creating database..."
  ssh -tt -i "~/.ssh/${CMS_ENV}.pem" \
    "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}" \
    "createdb ${DATABASE_NAME} --host ${DB_ENDPOINT} --username ${DATABASE_USER}"
fi

#
# Deploy AWS application modules
#

# Set environment to the AWS account where the shared ECR repository is maintained
    
export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

cd "${START_DIR}"
cd terraform/environments/$CMS_ECR_REPO_ENV

# Build new images or use existing images

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

  # Note that I can't build with "make docker-build" because test containers
  # try to run inside the docker container. Using "mvn clean package" instead.
  export OKTA_CLIENT_ID=0oa2t0lsrdZw5uWRx297
  export OKTA_CLIENT_PASSWORD=HHduWG6LogIvDIQuWgp3Zlo9OYMValTtH5OBcuHw
  mvn clean package -DskipTests
  sleep 5

  # Get branch name

  BRANCH=$(git rev-parse --abbrev-ref HEAD)

  # Create an image version using the seven character git commit id
  
  if [ "${BRANCH}" == "master" ]; then
    echo "Using commit number of master branch as the image version."
    COMMIT_NUMBER=$(git rev-parse HEAD | cut -c1-7)
    IMAGE_VERSION="${CMS_ENV}-latest-${COMMIT_NUMBER}"
  else # assume it is a devops branch and get the latest merge from master into the branch
    echo "NOTE: Assuming this is a DevOps branch that has only DevOps changes."
    echo "Using commit number of latest merge from branch into the current branch as the image version."
    COMMIT_NUMBER=$(git log --merges | head -n 2 | tail -n 1 | cut -d" " -f 3)
    IMAGE_VERSION="${CMS_ENV}-latest-${COMMIT_NUMBER}"
  fi

  # Build API docker images

  cd "${START_DIR}"
  cd ../api
  docker build \
    --no-cache \
    --tag "ab2d_api:${IMAGE_VERSION}" .
  docker build \
    --no-cache \
    --tag "ab2d_api:${CMS_ENV}-latest" .

  # Build worker docker image

  cd "${START_DIR}"
  cd ../worker
  docker build \
    --no-cache \
    --tag "ab2d_worker:${IMAGE_VERSION}" .
  docker build \
    --no-cache \
    --tag "ab2d_worker:${CMS_ENV}-latest" .

  # Get or create api repo
  
  API_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
    --output text)
  if [ -z "${API_ECR_REPO_URI}" ]; then
    aws --region "${REGION}" ecr create-repository \
      --repository-name "ab2d_api"
    API_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
      --output text)
  fi

  # Get ecr repo aws account

  ECR_REPO_AWS_ACCOUNT=$(aws --region "${REGION}" sts get-caller-identity \
    --query Account \
    --output text)
  
  # Apply ecr repo policy to the "ab2d_api" repo

  cd "${START_DIR}"
  cd terraform/environments/$CMS_ECR_REPO_ENV
  aws --region "${REGION}" ecr set-repository-policy \
    --repository-name ab2d_api \
    --policy-text file://ab2d-ecr-policy.json

  # Tag and push two copies (image version and latest version) of API docker image to ECR
  # - image version keeps track of the master commit number (e.g. ab2d-dev-latest-3d0905b)
  # - latest version is the same image but without the commit number (e.g. ab2d-dev-latest)
  # - NOTE: the latest version is used by the ecs task definition so that it can remain static for zero downtime

  docker tag "ab2d_api:${IMAGE_VERSION}" "${API_ECR_REPO_URI}:${IMAGE_VERSION}"
  docker push "${API_ECR_REPO_URI}:${IMAGE_VERSION}"
  docker tag "ab2d_api:${CMS_ENV}-latest" "${API_ECR_REPO_URI}:${CMS_ENV}-latest"
  docker push "${API_ECR_REPO_URI}:${CMS_ENV}-latest"

  # Get or create api repo

  WORKER_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
    --output text)
  if [ -z "${WORKER_ECR_REPO_URI}" ]; then
    aws --region "${REGION}" ecr create-repository \
      --repository-name "ab2d_worker"
    WORKER_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
      --output text)
  fi

  # Apply ecr repo policy to the "ab2d_worker" repo

  cd "${START_DIR}"
  cd terraform/environments/$CMS_ECR_REPO_ENV
  aws --region "${REGION}" ecr set-repository-policy \
    --repository-name ab2d_worker \
    --policy-text file://ab2d-ecr-policy.json

  # Tag and push two copies (image version and latest version) of worker docker image to ECR
  # - image version keeps track of the master commit number (e.g. ab2d-dev-latest-3d0905b)
  # - latest version is the same image but without the commit number (e.g. ab2d-dev-latest)
  # - NOTE: the latest version is used by the ecs task definition so that it can remain static for zero downtime

  docker tag "ab2d_worker:${IMAGE_VERSION}" "${WORKER_ECR_REPO_URI}:${IMAGE_VERSION}"
  docker push "${WORKER_ECR_REPO_URI}:${IMAGE_VERSION}"
  docker tag "ab2d_worker:${CMS_ENV}-latest" "${WORKER_ECR_REPO_URI}:${CMS_ENV}-latest"
  docker push "${WORKER_ECR_REPO_URI}:${CMS_ENV}-latest"

  # Get list of untagged images in the api repository

  IMAGES_TO_DELETE=$(aws --region "${REGION}" ecr list-images \
    --repository-name ab2d_api \
    --filter "tagStatus=UNTAGGED" \
    --query 'imageIds[*]' \
    --output json)

  # Delete untagged images in the api repository
  
  aws --region "${REGION}" ecr batch-delete-image \
    --repository-name ab2d_api \
    --image-ids "$IMAGES_TO_DELETE" \
    || true

  # Get old 'latest-commit' api tag
  
  API_OLD_LATEST_COMMIT_TAG=$(aws --region "${REGION}" ecr describe-images \
    --repository-name ab2d_api \
    --query "imageDetails[*].{imageTag:imageTags[0],imagePushedAt:imagePushedAt}" \
    --output json \
    | jq 'sort_by(.imagePushedAt) | reverse' \
    | jq ".[] | select(.imageTag | startswith(\"${CMS_ENV}-latest\"))" \
    | jq --slurp '.' \
    | jq 'del(.[0,1])' \
    | jq '.[] | select(.imageTag)' \
    | jq '.imageTag' \
    | tr -d '"' \
    | head -1)

  if [ -n "${API_OLD_LATEST_COMMIT_TAG}" ]; then
      
    # Rename 'latest-commit' api tag

    RENAME_API_OLD_LATEST_COMMIT_TAG=$(echo "${API_OLD_LATEST_COMMIT_TAG/-latest-/-}")

    # Get manifest of tag to rename
    
    MANIFEST=$(aws --region "${REGION}" ecr batch-get-image \
      --repository-name ab2d_api \
      --image-ids imageTag="${API_OLD_LATEST_COMMIT_TAG}" \
      --query 'images[].imageManifest' \
      --output text)

    # Add renamed api tag
    
    aws --region "${REGION}" ecr put-image \
      --repository-name ab2d_api \
      --image-tag "${RENAME_API_OLD_LATEST_COMMIT_TAG}" \
      --image-manifest "${MANIFEST}"

    # Remove old api tag
  
    aws --region "${REGION}" ecr batch-delete-image \
      --repository-name ab2d_api \
      --image-ids imageTag="${API_OLD_LATEST_COMMIT_TAG}"

  fi
  
  # Get list of untagged images in the worker repository
    
  IMAGES_TO_DELETE=$(aws --region "${REGION}" ecr list-images \
    --repository-name ab2d_worker \
    --filter "tagStatus=UNTAGGED" \
    --query 'imageIds[*]' \
    --output json)

  # Delete untagged images in the worker repository
  
  aws --region "${REGION}" ecr batch-delete-image \
    --repository-name ab2d_worker \
    --image-ids "$IMAGES_TO_DELETE" \
    || true

  # Get old 'latest-commit' worker tag
  
  WORKER_OLD_LATEST_COMMIT_TAG=$(aws --region "${REGION}" ecr describe-images \
    --repository-name ab2d_worker \
    --query "imageDetails[*].{imageTag:imageTags[0],imagePushedAt:imagePushedAt}" \
    --output json \
    | jq 'sort_by(.imagePushedAt) | reverse' \
    | jq ".[] | select(.imageTag | startswith(\"${CMS_ENV}-latest\"))" \
    | jq --slurp '.' \
    | jq 'del(.[0,1])' \
    | jq '.[] | select(.imageTag)' \
    | jq '.imageTag' \
    | tr -d '"' \
    | head -1)

  if [ -n "${WORKER_OLD_LATEST_COMMIT_TAG}" ]; then
      
    # Rename 'latest-commit' worker tag

    RENAME_WORKER_OLD_LATEST_COMMIT_TAG=$(echo "${WORKER_OLD_LATEST_COMMIT_TAG/-latest-/-}")

    # Get manifest of tag to rename
    
    MANIFEST=$(aws --region "${REGION}" ecr batch-get-image \
      --repository-name ab2d_worker \
      --image-ids imageTag="${WORKER_OLD_LATEST_COMMIT_TAG}" \
      --query 'images[].imageManifest' \
      --output text)

    # Add renamed tag

    aws --region "${REGION}" ecr put-image \
      --repository-name ab2d_worker \
      --image-tag "${RENAME_WORKER_OLD_LATEST_COMMIT_TAG}" \
      --image-manifest "${MANIFEST}"

    # Remove old tag
  
    aws --region "${REGION}" ecr batch-delete-image \
      --repository-name ab2d_worker \
      --image-ids imageTag="${WORKER_OLD_LATEST_COMMIT_TAG}"

  fi
  
else # use existing images

  echo "Using existing images..."

  # Get current image version
  
  IMAGE_VERSION=$(aws --region "${REGION}" ecr describe-images \
    --repository-name ab2d_api \
    --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]')

  # Remove double quotes
  
  IMAGE_VERSION=$(echo $IMAGE_VERSION | tr -d '"')
  
fi

# Verify that the image versions of API and Worker are the same

IMAGE_VERSION_API=$(aws --region "${REGION}" ecr describe-images \
  --repository-name ab2d_api \
  --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]')

IMAGE_VERSION_WORKER=$(aws --region "${REGION}" ecr describe-images \
  --repository-name ab2d_worker \
  --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]')

if [ "${IMAGE_VERSION_API}" != "${IMAGE_VERSION_WORKER}" ]; then
  echo "ERROR: The ECR image versions for ab2d_api and ab2d_worker must be the same!"
  exit 0
else
  echo "The ECR image versions for ab2d_api and ab2d_worker were verified to be the same..."
fi

echo "Using master branch commit number '${COMMIT_NUMBER}' for ab2d_api and ab2d_worker..."

# Reset to the target environment
    
export AWS_PROFILE="${CMS_ENV}"

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

#
# Switch context to terraform environment
#

echo "Switch context to terraform environment..."

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

#
# Get current known good ECS task definitions
#

CLUSTER_ARNS=$(aws --region "${REGION}" ecs list-clusters \
  --query 'clusterArns' \
  --output text \
  | grep "/${CMS_ENV}-api" \
  | xargs \
  | tr -d '\r')

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping getting current ECS task definitions, since there are no existing clusters"
else
  if [ -n "${BUILD_NEW_IMAGES}" ]; then
    echo "Using existing ECS task definitions..."
  else
    echo "Using newly created ECS task definitions..."

    echo "Get current known good ECS task definitions..."

    API_TASK_DEFINITION=$(aws --region "${REGION}" ecs describe-services \
      --services "${CMS_ENV}-api" \
      --cluster "${CMS_ENV}-api" \
      | grep "taskDefinition" \
      | head -1)
    API_TASK_DEFINITION=$(echo $API_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')
    
    WORKER_TASK_DEFINITION=$(aws --region "${REGION}" ecs describe-services \
      --services "${CMS_ENV}-worker" \
      --cluster "${CMS_ENV}-worker" \
      | grep "taskDefinition" \
      | head -1)
    WORKER_TASK_DEFINITION=$(echo $WORKER_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')

  fi
fi

#
# Get ECS task counts before making any changes
#

echo "Get ECS task counts before making any changes..."

# Define task count functions

api_task_count() { aws --region "${REGION}" ecs list-tasks --cluster "${CMS_ENV}-api" | grep "\:task\/"|wc -l|tr -d ' '; }
worker_task_count() { aws --region "${REGION}" ecs list-tasks --cluster "${CMS_ENV}-worker" | grep "\:task\/"|wc -l|tr -d ' '; }

# Get old api task count (if exists)

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_TASK_COUNT, since there are no existing clusters"
else
  OLD_API_TASK_COUNT=$(api_task_count)
  OLD_WORKER_TASK_COUNT=$(worker_task_count)
fi

# set expected api task count

if [ -z "${CLUSTER_ARNS}" ]; then
  EXPECTED_API_COUNT="2"
  EXPECTED_WORKER_COUNT="2"
else
  let EXPECTED_API_COUNT="$OLD_API_TASK_COUNT*2"
  let EXPECTED_WORKER_COUNT="$OLD_WORKER_TASK_COUNT*2"
fi

#
# Ensure Old Autoscaling Groups and containers are around to service requests
#

echo "Ensure Old Autoscaling Groups and containers are around to service requests..."

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_ASG, since there are no existing clusters"
else
  OLD_API_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}' | grep $CMS_ENV-api | tr -d '"')
  OLD_WORKER_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}' | grep $CMS_ENV-worker | tr -d '"')
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  terraform state rm module.api.aws_autoscaling_group.asg
  terraform state rm module.api.aws_launch_configuration.launch_config
  terraform state rm module.worker.aws_autoscaling_group.asg
  terraform state rm module.worker.aws_launch_configuration.launch_config
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  OLD_API_CONTAINER_INSTANCES=$(aws --region "${REGION}" ecs list-container-instances \
    --cluster "${CMS_ENV}-api" \
    | grep container-instance)
  OLD_WORKER_CONTAINER_INSTANCES=$(aws --region "${REGION}" ecs list-container-instances \
    --cluster "${CMS_ENV}-worker" \
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
  DATABASE_HOST=$(./get-database-secret.py $CMS_ENV database_host $DATABASE_SECRET_DATETIME)
fi

# Create or get database port secret

DB_PORT=$(aws --region "${REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Port" \
  --output=text)

DATABASE_PORT=$(./get-database-secret.py $CMS_ENV database_port $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_PORT}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_PORT}"
  DATABASE_PORT=$(./get-database-secret.py $CMS_ENV database_port $DATABASE_SECRET_DATETIME)
fi

# Get database secret manager ARNs

DATABASE_HOST_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PORT_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_USER_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_user/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PASSWORD_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_password/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_NAME_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_name/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

# Change to the target terraform environment

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

# Get the BFD keystore file name

BFD_KEYSTORE_FILE_NAME=$(echo $BFD_KEYSTORE_LOCATION | cut -d"/" -f 6)

# Run automation for API and worker based on auto approve parameter

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
    --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
    --var "image_version=$IMAGE_VERSION" \
    --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
    --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
    --target module.api
  
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$WORKER_TASK_DEFINITION" \
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
    --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
    --var "image_version=$IMAGE_VERSION" \
    --var "bfd_url=$BFD_URL" \
    --var "bfd_keystore_location=$BFD_KEYSTORE_LOCATION" \
    --var "bfd_keystore_password=$BFD_KEYSTORE_PASSWORD" \
    --var "hicn_hash_pepper=$HICN_HASH_PEPPER" \
    --var "hicn_hash_iter=$HICN_HASH_ITER" \
    --var "bfd_keystore_file_name=$BFD_KEYSTORE_FILE_NAME" \
    --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
    --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
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
    --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
    --var "image_version=$IMAGE_VERSION" \
    --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
    --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
    --target module.api \
    --auto-approve

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$WORKER_TASK_DEFINITION" \
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
    --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
    --var "image_version=$IMAGE_VERSION" \
    --var "bfd_url=$BFD_URL" \
    --var "bfd_keystore_location=$BFD_KEYSTORE_LOCATION" \
    --var "bfd_keystore_password=$BFD_KEYSTORE_PASSWORD" \
    --var "hicn_hash_pepper=$HICN_HASH_PEPPER" \
    --var "hicn_hash_iter=$HICN_HASH_ITER" \
    --var "bfd_keystore_file_name=$BFD_KEYSTORE_FILE_NAME" \
    --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
    --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
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
cd terraform/environments/$CMS_SHARED_ENV

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

ACTUAL_WORKER_COUNT=0
RETRIES_WORKER=0

while [ "$ACTUAL_WORKER_COUNT" -lt "$EXPECTED_WORKER_COUNT" ]; do
  ACTUAL_WORKER_COUNT=$(worker_task_count)
  echo "Running WORKER Tasks: $ACTUAL_WORKER_COUNT, Expected: $EXPECTED_WORKER_COUNT"
  if [ "$RETRIES_WORKER" != "15" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    RETRIES_WORKER=$(expr $RETRIES_WORKER + 1)
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

# Drain old container instances

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping draining old container instances, since there are no existing clusters"
else
  OLD_API_INSTANCE_LIST=$(echo $OLD_API_CONTAINER_INSTANCES | tr -d ' ' | tr "\n" " " | tr -d "," | tr '""' ' ' | tr -d '"')
  OLD_WORKER_INSTANCE_LIST=$(echo $OLD_WORKER_CONTAINER_INSTANCES | tr -d ' ' | tr "\n" " " | tr -d "," | tr '""' ' ' | tr -d '"')
  aws --region "${REGION}" ecs update-container-instances-state \
    --cluster "${CMS_ENV}-api" \
    --status DRAINING \
    --container-instances $OLD_API_INSTANCE_LIST
  aws --region "${REGION}" ecs update-container-instances-state \
    --cluster "${CMS_ENV}-worker" \
    --status DRAINING \
    --container-instances $OLD_WORKER_INSTANCE_LIST
  echo "Allowing all instances to drain for 60 seconds before proceeding..."
  sleep 60
fi

# Remove old Autoscaling groups

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing old autoscaling groups, since there are no existing clusters"
else
  OLD_API_ASG=$(echo $OLD_API_ASG | awk -F"/" '{print $2}')
  OLD_WORKER_ASG=$(echo $OLD_WORKER_ASG | awk -F"/" '{print $2}')
  aws --region "${REGION}" autoscaling delete-auto-scaling-group \
    --auto-scaling-group-name $OLD_API_ASG \
    --force-delete || true
  aws --region "${REGION}" autoscaling delete-auto-scaling-group \
    --auto-scaling-group-name $OLD_WORKER_ASG \
    --force-delete || true
fi

# Remove old launch configurations

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing old launch configurations, since there are no existing clusters"
else
    
  LAUNCH_CONFIGURATION_EXPECTED_COUNT=2
  LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${REGION}" autoscaling describe-launch-configurations \
    --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
    | jq '. | length')
  
  while [ "$LAUNCH_CONFIGURATION_ACTUAL_COUNT" -gt "$LAUNCH_CONFIGURATION_EXPECTED_COUNT" ]; do
  
    OLD_LAUNCH_CONFIGURATION=$(aws --region "${REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      --output text \
      | sort -k2 \
      | head -n1 \
      | awk '{print $1}')
  
    aws --region "${REGION}" autoscaling delete-launch-configuration \
      --launch-configuration-name "${OLD_LAUNCH_CONFIGURATION}"
  
    sleep 5
    
    LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      | jq '. | length')
  
  done
  
fi

#
# Deploy CloudWatch
#

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

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
