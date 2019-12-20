#!/bin/bash

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
  --environment-1=*)
  ENVIRONMENT_1="${i#*=}"
  CMS_ENV_1=$(echo $ENVIRONMENT_1 | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --region-1=*)
  REGION_1="${i#*=}"
  shift # past argument=value
  ;;
  --environment-2=*)
  ENVIRONMENT_2="${i#*=}"
  CMS_ENV_2=$(echo $ENVIRONMENT_2 | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --region-2=*)
  REGION_2="${i#*=}"
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
esac
done

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT_1}" ] \
  || [ -z "${REGION_1}" ] \
  || [ -z "${ENVIRONMENT_2}" ] \
  || [ -z "${REGION_2}" ] \
  || [ -z "${SSH_USERNAME}" ] \
  || [ -z "${SEED_AMI_PRODUCT_CODE}" ] \
  || [ -z "${EC2_INSTANCE_TYPE}" ]; then
    echo "Try running the script like the following:"
    echo "./bash/vpc-peering-test.sh \\"
    echo "  --environment-1=dev \\"
    echo "  --region-1=us-east-2 \\"
    echo "  --environment-2=sbx \\"
    echo "  --region-2=us-east-2 \\"
    echo "  --seed-ami-product-code=aw0evgkw8e5c1q413zgy5pjce \\"
    echo "  --ssh-username=centos \\"
    echo "  --ec2-instance-type=m5.xlarge"
    exit 1
fi

# Verify region 1 and region 2 are the same

if [ "${REGION_1}" != "${REGION_2}" ]; then
    echo "This script currently requires that region 1 and region 2 are the same."
    exit
fi

# Set environment

export AWS_PROFILE="sbdemo-shared"

#
# Configure terraform
#

# Create automation bucket

AUTOMATION_BUCKET_EXISTS=$(aws --region "${REGION_1}" s3api list-buckets \
  --query="Buckets[?Name=='ab2d-automation-${REGION_1}'].Name" \
  --output text)

if [ -z "${AUTOMATION_BUCKET_EXISTS}" ]; then

  echo "Creating automtion bucket..."
    
  # Note that a LocationConstraint is required for all buckets created outside of us-east-1
  aws --region "${REGION_1}" s3api create-bucket \
    --bucket "ab2d-automation-${REGION_1}" \
    --create-bucket-configuration LocationConstraint="${REGION_1}"

  aws --region "${REGION_1}" s3api put-public-access-block \
    --bucket "ab2d-automation-${REGION_1}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

fi

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}"
cd ../terraform/environments/vpc-peering-test

terraform init
terraform validate

#
# Get vpc ids for both environments
#

# Get VPC_ID of environment #1

VPC_ID_1=$(aws --region "${REGION_1}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_1}" \
  --query="Vpcs[*].VpcId" \
  --output text)

echo "The VPC ID for ${CMS_ENV_1} is: ${VPC_ID_1}"

# Get VPC_ID of environment #2

VPC_ID_2=$(aws --region "${REGION_2}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_2}" \
  --query="Vpcs[*].VpcId" \
  --output text)

echo "The VPC ID for ${CMS_ENV_2} is: ${VPC_ID_2}"

# Get deployer IP address

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

#
# Get public subnets for both environments
#

# Get first public subnet id for first environment

echo "Getting first public subnet id for first environment..."

SUBNET_PUBLIC_1_ID_ENV_1=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_1}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID_ENV_1}" ]; then
    echo "ERROR: public subnet #1 for first environment not found..."
    exit 1
fi

# Get second public subnet id for first environment

echo "Getting second public subnet id for first environment..."

SUBNET_PUBLIC_2_ID_ENV_1=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_1}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID_ENV_1}" ]; then
    echo "ERROR: public subnet #2 for first environment not found..."
    exit 1
fi

# Get first public subnet id for second environment

echo "Getting first public subnet id for second environment..."

SUBNET_PUBLIC_1_ID_ENV_2=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_2}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID_ENV_2}" ]; then
    echo "ERROR: public subnet #1 for second environment not found..."
    exit 1
fi

# Get second public subnet id for second environment

echo "Getting second public subnet id for second environment..."

SUBNET_PUBLIC_2_ID_ENV_2=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_2}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID_ENV_2}" ]; then
    echo "ERROR: public subnet #2 for second environment not found..."
    exit 1
fi

#
# Get private subnets for both environments
#

# Get first private subnet id for first environment

echo "Getting first private subnet id for first environment..."

SUBNET_PRIVATE_1_ID_ENV_1=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_1}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID_ENV_1}" ]; then
    echo "ERROR: private subnet #1 for first environment not found..."
    exit 1
fi

# Get second private subnet id for first environment

echo "Getting second private subnet id for first environment..."

SUBNET_PRIVATE_2_ID_ENV_1=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_1}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID_ENV_1}" ]; then
    echo "ERROR: private subnet #2 for first environment not found..."
    exit 1
fi

# Get first private subnet id for second environment

echo "Getting first private subnet id for second environment..."

SUBNET_PRIVATE_1_ID_ENV_2=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_2}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID_ENV_2}" ]; then
    echo "ERROR: private subnet #1 for second environment not found..."
    exit 1
fi

# Get second private subnet id for second environment

echo "Getting second private subnet id for second environment..."

SUBNET_PRIVATE_2_ID_ENV_2=$(aws --region "${REGION_2}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV_2}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID_ENV_2}" ]; then
    echo "ERROR: private subnet #2 for second environment not found..."
    exit 1
fi

#
# Create VPC peering from VPC #1 to VPC #2
#

# Determine if a VPC peering connection exists ("pending-acceptance" or "active")

# Get VPC peering connection that is either "pending-acceptance" or "active" (if exists)

VPC_PEERING_CONNECTION_ID=$(aws --region "${REGION_1}" ec2 describe-vpc-peering-connections \
  --filters "Name=status-code,Values=pending-acceptance,active" \
	    "Name=requester-vpc-info.vpc-id,Values=${VPC_ID_1}" \
	    "Name=accepter-vpc-info.vpc-id,Values=${VPC_ID_2}" \
  --query "VpcPeeringConnections[*].VpcPeeringConnectionId" \
  --output text)

if [ -z "${VPC_PEERING_CONNECTION_ID}" ]; then
    
  # VPC #1 requests a peering connection to VPC #2

  aws --region "${REGION_1}" ec2 create-vpc-peering-connection \
    --vpc-id "${VPC_ID_1}" \
    --peer-vpc-id "${VPC_ID_2}"

  # Get VPC peering connection that is "pending-acceptance"

  VPC_PEERING_CONNECTION_ID=$(aws --region "${REGION_1}" ec2 describe-vpc-peering-connections \
    --filters "Name=status-code,Values=pending-acceptance" \
	      "Name=requester-vpc-info.vpc-id,Values=${VPC_ID_1}" \
	      "Name=accepter-vpc-info.vpc-id,Values=${VPC_ID_2}" \
    --query "VpcPeeringConnections[*].VpcPeeringConnectionId" \
    --output text)
  
fi

# Get status of VPC peering connection ("pending-acceptance" or "active")

VPC_PEERING_CONNECTION_STATUS=$(aws --region "${REGION_1}" ec2 describe-vpc-peering-connections \
  --filters "Name=status-code,Values=pending-acceptance,active" \
	    "Name=requester-vpc-info.vpc-id,Values=${VPC_ID_1}" \
	    "Name=accepter-vpc-info.vpc-id,Values=${VPC_ID_2}" \
  --query "VpcPeeringConnections[?VpcPeeringConnectionId=='${VPC_PEERING_CONNECTION_ID}'].Status.Code" \
  --output text)
VPC_PEERING_CONNECTION_STATUS=$(echo $VPC_PEERING_CONNECTION_STATUS | tr '[:lower:]' '[:upper:]')

# Make VPC peering connection active

if [ "${VPC_PEERING_CONNECTION_STATUS}" != "ACTIVE" ]; then

  # Get VPC peering connection id
    
  VPC_PEERING_CONNECTION_ID=$(aws --region "${REGION_1}" ec2 describe-vpc-peering-connections \
    --filters "Name=status-code,Values=pending-acceptance" \
              "Name=requester-vpc-info.vpc-id,Values=${VPC_ID_1}" \
              "Name=accepter-vpc-info.vpc-id,Values=${VPC_ID_2}" \
    --query "VpcPeeringConnections[*].VpcPeeringConnectionId" \
    --output text)

  # VPC #2 accepts the peering connection request from VPC #1

  aws --region "${REGION_2}" ec2 accept-vpc-peering-connection \
    --vpc-peering-connection-id "${VPC_PEERING_CONNECTION_ID}"

  # Wait for peering connection to become active
  
  SECONDS_COUNT=0
  LAST_SECONDS_COUNT=0
  WAIT_SECONDS=5
  echo "Waiting for VPC peering connection to become active..."
  until [[ "${VPC_PEERING_CONNECTION_STATUS}" == "ACTIVE" ]]; do
    INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
    echo $INTERVAL
    if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
      VPC_PEERING_CONNECTION_STATUS=$(aws --region "ca-central-1" ec2 describe-vpc-peering-connections \
        --query "VpcPeeringConnections[?VpcPeeringConnectionId=='${VPC_PEERING_CONNECTION_ID}'].Status.Code" \
        --output text)
      VPC_PEERING_CONNECTION_STATUS=$(echo $VPC_PEERING_CONNECTION_STATUS | tr '[:lower:]' '[:upper:]')
      echo "Waiting for VPC peering connection to become active..."
      LAST_SECONDS_COUNT=$SECONDS_COUNT
    fi
    SECONDS_COUNT=$[SECONDS_COUNT+1]
    sleep 1
  done

else
  echo "VPC peering connection verified..."
fi

# Tag VPC peering connection

VPC_PEERING_CONNECTION_ID=$(aws --region "${REGION_2}" ec2 describe-vpc-peering-connections \
  --filters "Name=status-code,Values=active" \
  --query "VpcPeeringConnections[*].VpcPeeringConnectionId" \
  --output text)

VPC_PEERING_CONNECTION_TAG_EXISTS=$(aws --region "${REGION_2}" ec2 describe-vpc-peering-connections \
  --filters "Name=vpc-peering-connection-id,Values=${VPC_PEERING_CONNECTION_ID}" \
	    "Name=tag:Name,Values=ab2d-${CMS_ENV_1}-${CMS_ENV_2}-pcx" \
  --query "VpcPeeringConnections[*].VpcPeeringConnectionId" \
  --output text)

if [ -z "${VPC_PEERING_CONNECTION_TAG_EXISTS}" ]; then
  echo "Setting tag for vpc peering connection..."
  aws --region "${REGION_2}" ec2 create-tags \
    --resources "${VPC_PEERING_CONNECTION_ID}" \
    --tags "Key=Name,Value=ab2d-${CMS_ENV_1}-${CMS_ENV_2}-pcx"
else
  echo "VPC peering connection tag verified..."
fi

#
# AMI Generation for EC2 instances
#

# Change to the environment directory

cd "${START_DIR}"
cd ../terraform/environments/vpc-peering-test

# Set AMI_ID if it already exists for the deployment

echo "Set AMI_ID if it already exists for the deployment..."
AMI_ID=$(aws --region "${REGION_2}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# If no AMI is specified then create a new one

echo "If no AMI is specified then create a new one..."
if [ -z "${AMI_ID}" ]; then
    
  # Get the latest seed AMI
  SEED_AMI=$(aws --region "${REGION_2}" ec2 describe-images \
    --owners aws-marketplace \
    --filters "Name=product-code,Values=${SEED_AMI_PRODUCT_CODE}" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')
    
  # Create AMI for application nodes
  cd "${START_DIR}"
  cd ../packer/app
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
    --var region="${REGION_1}" \
    --var ec2_instance_type=$EC2_INSTANCE_TYPE \
    --var vpc_id=$VPC_ID \
    --var subnet_public_1_id=$SUBNET_PUBLIC_1_ID_ENV_1 \
    --var my_ip_address=$DEPLOYER_IP_ADDRESS \
    --var ssh_username=$SSH_USERNAME \
    --var git_commit_hash=$COMMIT \
    app.json  2>&1 | tee output.txt
  AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  
  # Add name tag to AMI
  aws --region "${REGION_2}" ec2 create-tags \
    --resources $AMI_ID \
    --tags "Key=Name,Value=ab2d-ami"
fi

#
# Create ".auto.tfvars" file for the target environment
#

cd "${START_DIR}"
cd ../terraform/environments/vpc-peering-test

echo 'vpc_id_1 = "'$VPC_ID_1'"' \
  > vpc-peering-test.auto.tfvars
echo 'vpc_id_2 = "'$VPC_ID_2'"' \
  >> vpc-peering-test.auto.tfvars

# echo 'private_subnet_ids_env_1 = ["'$SUBNET_PRIVATE_1_ID_ENV_1'","'$SUBNET_PRIVATE_2_ID_ENV_1'"]' \
#   >> vpc-peering-test.auto.tfvars

echo 'public_subnet_ids_env_1 = ["'$SUBNET_PUBLIC_1_ID_ENV_1'","'$SUBNET_PUBLIC_2_ID_ENV_1'"]' \
  >> vpc-peering-test.auto.tfvars

echo 'private_subnet_ids_env_2 = ["'$SUBNET_PRIVATE_1_ID_ENV_2'","'$SUBNET_PRIVATE_2_ID_ENV_2'"]' \
  >> vpc-peering-test.auto.tfvars

# echo 'public_subnet_ids_env_2 = ["'$SUBNET_PUBLIC_1_ID_ENV_2'","'$SUBNET_PUBLIC_2_ID_ENV_2'"]' \
#   >> vpc-peering-test.auto.tfvars

echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE'"' \
  >> vpc-peering-test.auto.tfvars
echo 'linux_user = "'$SSH_USERNAME'"' \
  >> vpc-peering-test.auto.tfvars
echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
  >> vpc-peering-test.auto.tfvars
echo 'ami_id = "'$AMI_ID'"' \
  >> vpc-peering-test.auto.tfvars

#
# Deploy test controller
#

# Change to the environment directory

cd "${START_DIR}"
cd ../terraform/environments/vpc-peering-test

# Create or update test controller

echo "Creating or updating test controller..."
terraform apply \
  --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
  --target module.test_controller \
  --auto-approve

# Create or update test node

echo "Creating or updating test node..."
terraform apply \
  --target module.test_node \
  --auto-approve
