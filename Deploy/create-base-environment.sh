#!/bin/bash

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

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
  --seed-ami-product-code=*)
  SEED_AMI_PRODUCT_CODE="${i#*=}"
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
  --skip-network)
  SKIP_NETWORK="true"
  shift # past argument=value
  ;;
  --vpc-exists)
  VPC_EXISTS="true"
  shift # past argument=value
  ;;
esac
done

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ] || [ -z "${SEED_AMI_PRODUCT_CODE}" ] || [ -z "${DATABASE_SECRET_DATETIME}" ]; then
  echo "Try running the script like one of these options:"
  echo "./create-base-environment.sh --environment=dev --seed-ami-product-code={aw0evgkw8e5c1q413zgy5pjce|gold disk product code} --database-secret-datetime={YYYY-MM-DD-HH-MM-SS}"
  echo "./create-base-environment.sh --environment=dev --seed-ami-product-code={aw0evgkw8e5c1q413zgy5pjce|gold disk product code} --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --skip-network"
  echo "./create-base-environment.sh --environment=dev --seed-ami-product-code={aw0evgkw8e5c1q413zgy5pjce|gold disk product code} --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --debug-level={TRACE|DEBUG|INFO|WARN|ERROR}"
  exit 1
fi

#
# Set environment
#

export AWS_PROFILE="${CMS_ENV}"
export DEBUG_LEVEL="WARN"

# #
# # Deploy or enable KMS
# #

# # Get KMS key id (if exists)

# KMS_KEY_ID=$(aws kms list-aliases \
#   --query="Aliases[?AliasName=='alias/ab2d-$CMS_ENV-kms'].TargetKeyId" \
#   --output text)

# # If KMS key exists, enable it; otherwise, create it

# if [ -n "$KMS_KEY_ID" ]; then
#   echo "Enabling KMS key..."
#   cd "${START_DIR}"
#   cd python3
#   ./enable-kms-key.py $KMS_KEY_ID
# else
#   echo "Deploying KMS..."
#   cd "${START_DIR}"
#   cd terraform/environments/cms-ab2d-$CMS_ENV
#   terraform apply \
#     --target module.kms --auto-approve
# fi

# #
# # Create or get secrets
# #

# # Change to the "python3" directory

# cd "${START_DIR}"
# cd python3

# # Create or get database user secret

# DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
# if [ -z "${DATABASE_USER}" ]; then
#   ./create-database-secret.py $CMS_ENV database_user $KMS_KEY_ID $DATABASE_SECRET_DATETIME
#   DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
# fi

# # Create or get database password secret

# DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
# if [ -z "${DATABASE_PASSWORD}" ]; then
#   ./create-database-secret.py $CMS_ENV database_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
#   DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
# fi

# # Create or get database name secret (if doesn't exist)

# DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
# if [ -z "${DATABASE_NAME}" ]; then
#   ./create-database-secret.py $CMS_ENV database_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
#   DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
# fi

#
# Create networking
#

# Get VPC ID (if exists)

VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-vpc" \
  --query="Vpcs[*].VpcId" \
  --output text)

# Continue process if VPC does not exist

if [ -z "${SKIP_NETWORK}" ] && [ -z "${VPC_ID}" ]; then

  export FIRST_RUN=true

  # Create VPC
  VPC_ID=$(aws ec2 create-vpc \
    --cidr-block 10.124.0.0/16 \
    --query 'Vpc.{VpcId:VpcId}' \
    --output text \
    --region us-east-1)
  echo "Creating VPC..."

else
  if [ -z "${SKIP_NETWORK}" ] && [ -z "${VPC_EXISTS}" ]; then # Exit shell script if VPC already exists
    echo "Skipping network creation since VPC already exists."
    echo "Do you want to add the \"--skip-network\" parameter?"
    exit
  else
      echo "Skipping network creation since VPC already exists."
  fi
fi

# Enable DNS hostname on VPC

if [ -z "${SKIP_NETWORK}" ]; then
  aws ec2 describe-vpc-attribute \
    --vpc-id $VPC_ID \
    --attribute enableDnsHostnames
  
  aws ec2 modify-vpc-attribute \
    --vpc-id $VPC_ID \
    --enable-dns-hostnames
fi

# Add name tag to VPC

if [ -z "${SKIP_NETWORK}" ]; then
  aws --region us-east-1 ec2 create-tags \
    --resources $VPC_ID \
    --tags "Key=Name,Value=ab2d-vpc"
fi

# Tag default route table

DEFAULT_ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters "Name=association.main,Values=true" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

aws ec2 create-tags \
  --resources $DEFAULT_ROUTE_TABLE_ID \
  --tags "Key=Name,Value=ab2d-default-rt" \
  --region us-east-1

# Tag default network ACL

DEFAULT_NETWORK_ACL_ID=$(aws ec2 describe-network-acls \
  --query="NetworkAcls[?IsDefault].NetworkAclId" \
  --output text)

aws ec2 create-tags \
  --resources $DEFAULT_NETWORK_ACL_ID \
  --tags "Key=Name,Value=ab2d-default-nacl" \
  --region us-east-1

# Tag default security group

DEFAULT_SECURITY_GROUP_ID=$(aws ec2 describe-security-groups \
  --query="SecurityGroups[?GroupName == 'default'].GroupId" \
  --output text)

aws ec2 create-tags \
  --resources $DEFAULT_SECURITY_GROUP_ID \
  --tags "Key=Name,Value=ab2d-default-sg" \
  --region us-east-1

#
# Create first public subnet
#

cd "${START_DIR}"
cd python3

SUBNET_PUBLIC_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-public-subnet-01" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SKIP_NETWORK}" ] && [ -z "${SUBNET_PUBLIC_1_ID}" ]; then

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
    --tags "Key=Name,Value=ab2d-$CMS_ENV-public-subnet-01" \
    --region us-east-1

fi

#
# Create second public subnet
#

cd "${START_DIR}"
cd python3

SUBNET_PUBLIC_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-public-subnet-02" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SKIP_NETWORK}" ] && [ -z "${SUBNET_PUBLIC_2_ID}" ]; then

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
    --tags "Key=Name,Value=ab2d-$CMS_ENV-public-subnet-02" \
    --region us-east-1

fi

#
# Create first private subnet
#

cd "${START_DIR}"
cd python3

SUBNET_PRIVATE_1_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-01" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SKIP_NETWORK}" ] && [ -z "${SUBNET_PRIVATE_1_ID}" ]; then

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
# Create second private subnet
#

cd "${START_DIR}"
cd python3

SUBNET_PRIVATE_2_ID=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-02" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SKIP_NETWORK}" ] && [ -z "${SUBNET_PRIVATE_2_ID}" ]; then

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

if [ -z "${SKIP_NETWORK}" ] && [ -z "${IGW_ID}" ]; then
    
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

if [ -z "${SKIP_NETWORK}" ] && [ -z "${IGW_ATTACHED}" ]; then
  echo "Attach internet gateway to VPC..."
  aws --region us-east-1 ec2 attach-internet-gateway \
    --internet-gateway-id $IGW_ID \
    --vpc-id $VPC_ID
fi





# # Create a custom route table for internet gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   IGW_ROUTE_TABLE_ID=$(aws ec2 create-route-table \
#     --vpc-id $VPC_ID \
#     --query 'RouteTable.{RouteTableId:RouteTableId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag to route table for internet gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $IGW_ROUTE_TABLE_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-igw-rt" \
#     --region us-east-1
# fi

# # Add route for internet gateway to the custom route table for public subnets

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws --region us-east-1 ec2 create-route \
#     --destination-cidr-block 0.0.0.0/0 \
#     --gateway-id $IGW_ID \
#     --route-table-id $IGW_ROUTE_TABLE_ID
# fi

# # Associate the first public subnet with the custom route table for internet gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 associate-route-table  \
#     --subnet-id $SUBNET_PUBLIC_1_ID \
#     --route-table-id $IGW_ROUTE_TABLE_ID \
#     --region us-east-1
# fi

# # Associate the second public subnet with the custom route table for internet gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 associate-route-table  \
#     --subnet-id $SUBNET_PUBLIC_2_ID \
#     --route-table-id $IGW_ROUTE_TABLE_ID \
#     --region us-east-1
# fi

# # Enable Auto-assign Public IP on the first public subnet

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 modify-subnet-attribute \
#     --subnet-id $SUBNET_PUBLIC_1_ID \
#     --map-public-ip-on-launch \
#     --region us-east-1
# fi

# # Enable Auto-assign Public IP on the second public subnet

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 modify-subnet-attribute \
#     --subnet-id $SUBNET_PUBLIC_2_ID \
#     --map-public-ip-on-launch \
#     --region us-east-1
# fi

# # Display NAT gatways stage

# if [ -z "${SKIP_NETWORK}" ]; then
#   echo "Creating NAT gateways..."
# fi

# # Allocate Elastic IP Address for first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   EIP_ALLOC_1_ID=$(aws ec2 allocate-address \
#     --domain vpc \
#     --query '{AllocationId:AllocationId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag to Elastic IP Address for first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $EIP_ALLOC_1_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-ngw-eip-1" \
#     --region us-east-1
# fi

# # Create first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   NAT_GW_1_ID=$(aws ec2 create-nat-gateway \
#     --subnet-id $SUBNET_PUBLIC_1_ID \
#     --allocation-id $EIP_ALLOC_1_ID \
#     --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag to first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $NAT_GW_1_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-ngw-1" \
#     --region us-east-1
# fi

# if [ -z "${SKIP_NETWORK}" ]; then
#   # Wait for first NAT Gateway to become available
#   SECONDS_COUNT=0
#   LAST_SECONDS_COUNT=0
#   WAIT_SECONDS=5
#   STATE='PENDING'
#   echo "Waiting for first NAT gateway to become available"
#   until [[ $STATE == 'AVAILABLE' ]]; do
#     INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
#     echo $INTERVAL
#     if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
#       STATE=$(aws ec2 describe-nat-gateways \
#         --nat-gateway-ids $NAT_GW_1_ID \
#         --query 'NatGateways[*].{State:State}' \
#         --output text \
#         --region us-east-1)
#       STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
#       echo "Waiting for first NAT gateway to become available"
#       LAST_SECONDS_COUNT=$SECONDS_COUNT
#     fi
#     SECONDS_COUNT=$[SECONDS_COUNT+1]
#     sleep 1
#   done
# fi
  
# # Create a custom route table for the first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   ROUTE_TABLE_FOR_NGW_1_ID=$(aws ec2 create-route-table \
#     --vpc-id $VPC_ID \
#     --query 'RouteTable.{RouteTableId:RouteTableId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag for the custom route table for the first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $ROUTE_TABLE_FOR_NGW_1_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-ngw-rt-1" \
#     --region us-east-1
# fi

# # Create route to the first NAT Gateway for the custom route table

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-route \
#     --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
#     --destination-cidr-block 0.0.0.0/0 \
#     --gateway-id $NAT_GW_1_ID \
#     --region us-east-1
# fi

# # Associate the first private subnet with the custom route table for the first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 associate-route-table  \
#     --subnet-id $SUBNET_PRIVATE_1_ID \
#     --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
#     --region us-east-1
# fi

# # Allocate Elastic IP Address for second NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   EIP_ALLOC_2_ID=$(aws ec2 allocate-address \
#     --domain vpc \
#     --query '{AllocationId:AllocationId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag to Elastic IP Address for second NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $EIP_ALLOC_2_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-ngw-eip-2" \
#     --region us-east-1
# fi

# # Create second NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   NAT_GW_2_ID=$(aws ec2 create-nat-gateway \
#     --subnet-id $SUBNET_PUBLIC_2_ID \
#     --allocation-id $EIP_ALLOC_2_ID \
#     --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag to second NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $NAT_GW_2_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-ngw-2" \
#     --region us-east-1
# fi

# # Wait for second NAT Gateway to become available

# if [ -z "${SKIP_NETWORK}" ]; then
#   SECONDS_COUNT=0
#   LAST_SECONDS_COUNT=0
#   WAIT_SECONDS=5
#   STATE='PENDING'
#   echo "Waiting for second NAT gateway to become available"
#   until [[ $STATE == 'AVAILABLE' ]]; do
#     INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
#     echo $INTERVAL
#     if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
#       STATE=$(aws ec2 describe-nat-gateways \
#         --nat-gateway-ids $NAT_GW_2_ID \
#         --query 'NatGateways[*].{State:State}' \
#         --output text \
#         --region us-east-1)
#       STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
#       echo "Waiting for second NAT gateway to become available"
#       LAST_SECONDS_COUNT=$SECONDS_COUNT
#     fi
#     SECONDS_COUNT=$[SECONDS_COUNT+1]
#     sleep 1
#   done
# fi

# # Create a custom route table for the second NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   ROUTE_TABLE_FOR_NGW_2_ID=$(aws ec2 create-route-table \
#     --vpc-id $VPC_ID \
#     --query 'RouteTable.{RouteTableId:RouteTableId}' \
#     --output text \
#     --region us-east-1)
# fi

# # Add name tag for the custom route table for the first NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-tags \
#     --resources $ROUTE_TABLE_FOR_NGW_2_ID \
#     --tags "Key=Name,Value=ab2d-$CMS_ENV-ngw-rt-2" \
#     --region us-east-1
# fi

# # Create route to the second NAT Gateway for the custom route table

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 create-route \
#     --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
#     --destination-cidr-block 0.0.0.0/0 \
#     --gateway-id $NAT_GW_2_ID \
#     --region us-east-1
# fi

# # Associate the second private subnet with the custom route table for the second NAT Gateway

# if [ -z "${SKIP_NETWORK}" ]; then
#   aws ec2 associate-route-table  \
#     --subnet-id $SUBNET_PRIVATE_2_ID \
#     --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
#     --region us-east-1
# fi

# # Create ".auto.tfvars" file for network settings

# cd "${START_DIR}"
# cd terraform/environments/cms-ab2d-$CMS_ENV

# if [ -z "${SKIP_NETWORK}" ]; then
#   echo 'vpc_id = "'$VPC_ID'"' \
#       > $ENVIRONMENT.auto.tfvars
#   echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
#     >> $ENVIRONMENT.auto.tfvars
#   echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
#     >> $ENVIRONMENT.auto.tfvars
# fi

# #
# # Configure terraform
# #

# # Reset logging
# echo "Setting terraform debug level to $DEBUG_LEVEL..."
# export TF_LOG=$DEBUG_LEVEL
# export TF_LOG_PATH=/var/log/terraform/tf.log
# rm -f /var/log/terraform/tf.log

# # Destroy tfstate environment in S3, if first run

# if [ -n "${FIRST_RUN}" ]; then
#   aws s3 rm s3://cms-ab2d-automation/cms-ab2d-${ENVIRONMENT} \
#     --recursive
# fi

# cd "${START_DIR}"

# # Initialize and validate terraform

# terraform init
# terraform validate

# #
# # Deploy S3
# #

# echo "Deploying S3..."

# aws s3api create-bucket --bucket cms-ab2d-cloudtrail --region us-east-1

# # Block public access on bucket

# aws s3api put-public-access-block \
#   --bucket cms-ab2d-cloudtrail \
#   --region us-east-1 \
#   --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

# # Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cms-ab2d-cloudtrail" bucket

# aws s3api put-bucket-acl \
#   --bucket cms-ab2d-cloudtrail \
#   --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
#   --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery

# # Add bucket policy to the "cms-ab2d-cloudtrail" S3 bucket

# cd "${START_DIR}"
# cd ../../../aws/s3-bucket-policies

# aws s3api put-bucket-policy \
#   --bucket cms-ab2d-cloudtrail \
#   --policy file://cms-ab2d-cloudtrail-bucket-policy.json

# cd "${START_DIR}"

# terraform apply \
#   --target module.s3 --auto-approve

# #
# # Deploy EFS
# #

# # Get files system id (if exists)
# EFS_FS_ID=$(aws efs describe-file-systems --query="FileSystems[?CreationToken=='cms-ab2d'].FileSystemId" --output=text)

# # Create file system (if doesn't exist)
# if [ -z "${EFS_FS_ID}" ]; then
#   echo "Creating EFS..."
#   terraform apply \
#     --target module.efs --auto-approve
# fi

# #
# # Deploy db
# #

# DB_ENDPOINT=$(aws rds describe-db-instances --query="DBInstances[?DBInstanceIdentifier=='cms-ab2d-${ENVIRONMENT}'].Endpoint.Address" --output=text)
# if [ -z "${DB_ENDPOINT}" ]; then
#   echo "Creating database..."
#   terraform apply \
#     --var "db_username=${DATABASE_USER}" \
#     --var "db_password=${DATABASE_PASSWORD}" \
#     --var "db_name=${DATABASE_NAME}" \
#     --target module.db --auto-approve
# fi

# #
# # AMI Generation for application nodes
# #

# # Set AMI_ID if it already exists for the deployment

# echo "Set AMI_ID if it already exists for the deployment..."
# AMI_ID=$(aws --region us-east-1 ec2 describe-images \
#   --owners self \
#   --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-AMI" \
#   --query "Images[*].[ImageId]" \
#   --output text)

# # If no AMI is specified then create a new one

# echo "If no AMI is specified then create a new one..."
# if [ -z "${AMI_ID}" ]; then
    
#   # Get the latest seed AMI
#   SEED_AMI=$(aws --region us-east-1 ec2 describe-images \
#     --owners aws-marketplace \
#     --filters "Name=product-code,Values=${SEED_AMI_PRODUCT_CODE}" \
#     --query "Images[*].[ImageId,CreationDate]" \
#     --output text \
#     | sort -k2 -r \
#     | head -n1 \
#     | awk '{print $1}')
  
#   # Get first public subnet
#   SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
#     --filters "Name=tag:Name,Values=AB2D-${CMS_ENV}-PUBLIC-SUBNET-01" \
#     --query "Subnets[0].SubnetId" \
#     --output text)
  
#   # Create AMI for application nodes
#   cd "${START_DIR}"
#   cd ../../../packer/app
#   IP=$(curl ipinfo.io/ip)
#   COMMIT=$(git rev-parse HEAD)
#   packer build --var seed_ami=$SEED_AMI --var vpc_id=$VPC_ID --var subnet_public_1_id=$SUBNET_PUBLIC_1_ID --var my_ip_address=$IP --var git_commit_hash=$COMMIT app.json  2>&1 | tee output.txt
#   AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
#   # Add name tag to AMI
#   aws --region us-east-1 ec2 create-tags \
#     --resources $AMI_ID \
#     --tags "Key=Name,Value=AB2D-$CMS_ENV-AMI"
# fi

# #
# # AMI Generation for Jenkins node
# #

# # Set JENKINS_AMI_ID if it already exists for the deployment

# echo "Set JENKINS_AMI_ID if it already exists for the deployment..."
# JENKINS_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
#   --owners self \
#   --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-JENKINS-AMI" \
#   --query "Images[*].[ImageId]" \
#   --output text)

# # If no AMI is specified then create a new one

# echo "If no AMI is specified then create a new one..."
# if [ -z "${JENKINS_AMI_ID}" ]; then

#   # Get the latest seed AMI
#   SEED_AMI=$(aws --region us-east-1 ec2 describe-images \
#     --owners aws-marketplace \
#     --filters "Name=product-code,Values=${SEED_AMI_PRODUCT_CODE}" \
#     --query "Images[*].[ImageId,CreationDate]" \
#     --output text \
#     | sort -k2 -r \
#     | head -n1 \
#     | awk '{print $1}')

#   # Get first public subnet
#   SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
#     --filters "Name=tag:Name,Values=AB2D-${CMS_ENV}-PUBLIC-SUBNET-01" \
#     --query "Subnets[0].SubnetId" \
#     --output text)

#   # Create AMI for Jenkins
#   cd "${START_DIR}"
#   cd ../../../packer/jenkins
#   IP=$(curl ipinfo.io/ip)
#   COMMIT=$(git rev-parse HEAD)
#   packer build --var seed_ami=$SEED_AMI --var vpc_id=$VPC_ID --var subnet_public_1_id=$SUBNET_PUBLIC_1_ID --var my_ip_address=$IP --var git_commit_hash=$COMMIT app.json  2>&1 | tee output.txt
#   JENKINS_AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
#   # Add name tag to AMI
#   aws --region us-east-1 ec2 create-tags \
#     --resources $JENKINS_AMI_ID \
#     --tags "Key=Name,Value=AB2D-$CMS_ENV-JENKINS-AMI"
# fi

# #
# # Deploy AWS application modules
# #

# cd "${START_DIR}"
# cd ../../..
# ./deploy.sh --environment="${ENVIRONMENT}" --ami="${AMI_ID}" --auto-approve
