#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set parameters
#

CMS_ENV="${CMS_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

EC2_INSTANCE_TYPE_PACKER="${EC2_INSTANCE_TYPE_PACKER_PARAM}"

OWNER="${OWNER_PARAM}"

REGION="${REGION_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

VPC_ID="${VPC_ID_PARAM}"

#
# AMI Generation
#

# Get first public subnet id

echo "Getting first public subnet id..."

SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "ERROR: public subnet #1 not found..."
  exit 1
else
  echo "SUBNET_PUBLIC_1_ID=${SUBNET_PUBLIC_1_ID}"
fi

# Get AMI_ID if it already exists for the deployment

echo "Get AMI_ID if it already exists for the deployment..."

AMI_ID=$(aws --region "${REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# Deregister existing ab2d ami

if [ -n "${AMI_ID}" ]; then
  echo "Deregister existing ab2d ami..."  
  aws --region "${REGION}" ec2 deregister-image \
    --image-id "${AMI_ID}"
fi

# Get the latest seed AMI

SEED_AMI=$(aws --region "${REGION}" ec2 describe-images \
  --owners "${OWNER}" \
  --filters "Name=name,Values=rhel7-gi-*" \
  --query "Images[*].[ImageId,CreationDate]" \
  --output text \
  | sort -k2 -r \
  | head -n1 \
  | awk '{print $1}')

if [ -z "${SEED_AMI}" ]; then
  echo "ERROR: seed AMI not found..."
  exit 1
else
  echo "SEED_AMI=${SEED_AMI}"
fi

# Create AMI for controller, api, and worker nodes

cd "${START_DIR}/.."
cd packer/app
IP=$(curl ipinfo.io/ip)
COMMIT=$(git rev-parse HEAD)
packer build \
  --var seed_ami="${SEED_AMI}" \
  --var environment="${CMS_ENV}" \
  --var region="${REGION}" \
  --var ec2_instance_type="${EC2_INSTANCE_TYPE_PACKER}" \
  --var vpc_id="${VPC_ID}" \
  --var subnet_public_1_id="${SUBNET_PUBLIC_1_ID}" \
  --var my_ip_address="${IP}" \
  --var ssh_username="${SSH_USERNAME}" \
  --var git_commit_hash="${COMMIT}" \
  app.json  2>&1 | tee output.txt
AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)

# Add name tag to AMI

aws --region "${REGION}" ec2 create-tags \
  --resources "${AMI_ID}" \
  --tags "Key=Name,Value=ab2d-ami"
