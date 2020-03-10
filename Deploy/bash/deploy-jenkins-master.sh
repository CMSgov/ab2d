#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}/.."

#
# Set default values
#

CMS_ENV="ab2d-mgmt-east-dev"
CMS_SHARED_ENV="ab2d-mgmt-east-dev-shared"
DEBUG_LEVEL="WARN"
EC2_INSTANCE_TYPE="m5.xlarge"
REGION="us-east-1"
SSH_USERNAME="ec2-user"
VPC_ID="vpc-0fccd950a4ce7469b"

#
# Set environment
#

# Set AWS profile

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

echo "**************************************************************"
echo "Initialize and validate terraform..."
echo "**************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_SHARED_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-automation" \
  -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="encrypt=true"

terraform validate

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

# Get first public subnet id

echo "Getting first public subnet id..."

SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then

    echo "ERROR: public subnet #1 not found..."
    exit 1

fi

# Get second public subnet id

echo "Getting second public subnet id..."

SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then

  echo "ERROR: public subnet #2 not found..."
  exit 1

fi

# Get first private subnet id

echo "Getting first private subnet id..."

SUBNET_PRIVATE_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then

  echo "ERROR: private subnet #1 not found..."
  exit 1
  
fi

# Get second private subnet id

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
# AMI Generation for Jenkins node
#

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

  cd "${START_DIR}/.."
  cd packer/jenkins
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build \
    --var seed_ami=$SEED_AMI \
    --var region="${REGION}" \
    --var ec2_instance_type="${EC2_INSTANCE_TYPE_PACKER}" \
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

#
# Create "auto.tfvars" file for shared components
#

# Change to the shared environment

cd "${START_DIR}/.."
cd terraform/environments/$CMS_SHARED_ENV

# Set variables in "auto.tfvars" file

echo 'vpc_id = "'$VPC_ID'"' \
  > $CMS_SHARED_ENV.auto.tfvars
echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
  >> $CMS_SHARED_ENV.auto.tfvars
echo 'public_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
  >> $CMS_SHARED_ENV.auto.tfvars
echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE'"' \
  >> $CMS_SHARED_ENV.auto.tfvars
echo 'linux_user = "'$SSH_USERNAME'"' \
  >> $CMS_SHARED_ENV.auto.tfvars
echo 'ami_id = "'$JENKINS_AMI_ID'"' \
  >> $CMS_SHARED_ENV.auto.tfvars

#
# Deploy jenkins master
#

cd "${START_DIR}/.."
cd terraform/environments/$CMS_SHARED_ENV

echo "Create or update jenkins master..."
terraform apply \
  --target module.jenkins_master \
  --auto-approve

#
# Push authorized_keys file to jenkins master
#

cd "${START_DIR}/.."
cd terraform/environments/$CMS_SHARED_ENV

echo "Push authorized_keys file to jenkins master..."
terraform taint \
  --allow-missing null_resource.authorized_keys_file
terraform apply \
  --target null_resource.authorized_keys_file \
  --auto-approve
