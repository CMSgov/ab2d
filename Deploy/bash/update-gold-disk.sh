#!/bin/bash

set -e # Turn on exit on error
set +x # <-- Do not change this value!
       # Logging is turned on in a later step based on CLOUD_TAMER_PARAM.
       # CLOUD_TAMER_PARAM = false (Jenkins assumed; verbose logging turned off)
       # CLOUD_TAMER_PARAM = true (Dev machine assumed; verbose logging turned on)
       # NOTE: Setting the CLOUD_TAMER_PARAM to a value that does not match the
       #       assumed host machine will cause the script to fail.

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_PACKER_PARAM}" ] \
    || [ -z "${IAM_INSTANCE_PROFILE_PARAM}" ] \
    || [ -z "${OWNER_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${TARGET_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${TARGET_CMS_ENV_PARAM}" ] \
    || [ -z "${VPC_ID_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set conditional variables
#

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
  exit 1
elif [ "${CLOUD_TAMER_PARAM}" == "false" ]; then

  # Turn off verbose logging for Jenkins jobs
  set +x
  echo "Don't print commands and their arguments as they are executed."
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

  # Import the "get temporary AWS credentials via AWS STS assume role" function
  source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

else # [ "${CLOUD_TAMER_PARAM}" == "true" ]

  # Turn on verbose logging for development machine testing
  set -x
  echo "Print commands and their arguments as they are executed."
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

  # Import the "get temporary AWS credentials via CloudTamer API" function
  source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

fi

# Set Parent AWS environment

if [ "${TARGET_CMS_ENV_PARAM}" == "ab2d-east-prod-test" ]; then
  PARENT_ENV="ab2d-east-prod"
else
  PARENT_ENV="${TARGET_CMS_ENV_PARAM}"
fi

#
# Set required variables
#

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

TARGET_AWS_ACCOUNT_NUMBER="${TARGET_AWS_ACCOUNT_NUMBER_PARAM}"

TARGET_CMS_ENV="${TARGET_CMS_ENV_PARAM}"

EC2_INSTANCE_TYPE_PACKER="${EC2_INSTANCE_TYPE_PACKER_PARAM}"

IAM_INSTANCE_PROFILE="${IAM_INSTANCE_PROFILE_PARAM}"

OWNER="${OWNER_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

VPC_ID="${VPC_ID_PARAM}"

#
# Set optional variables
#

SEED_AMI_PINNED="${SEED_AMI_PINNED_PARAM}"

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
  exit 1
else
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"
fi

#
# Define functions
#

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

# Import the "get temporary AWS credentials via AWS STS assume role" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${TARGET_AWS_ACCOUNT_NUMBER}" "${PARENT_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${TARGET_AWS_ACCOUNT_NUMBER}" "${PARENT_ENV}"
fi

#
# AMI Generation
#

# Get first public subnet id

echo "Getting first public subnet id..."

SUBNET_PUBLIC_1_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${PARENT_ENV}-public-a" \
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

AMI_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# If AMI ID exists, determine if it is in use

if [ -n "${AMI_ID}" ]; then
  AMI_ID_IN_USE=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
    --query "Reservations[*].Instances[?ImageId=='${AMI_ID}']".InstanceId \
    --output text)
fi

# If existing ab2d ami in use, rename it; if not deregister it

if [ -n "${AMI_ID_IN_USE}" ]; then
  echo "Rename existing ab2d ami..."

  BASE_GOLD_DISK=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
    --owners self --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-ami" \
    --query "Images[*].Tags[?Key=='gold_ami'].Value" \
    --output text)

  if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
    aws --region "${AWS_DEFAULT_REGION}" ec2 create-tags \
      --resources "${AMI_ID}" \
      --tags "Key=Name,Value=${TARGET_CMS_ENV}-ami-gold-disk-${BASE_GOLD_DISK}"
  else
    aws --region "${AWS_DEFAULT_REGION}" ec2 create-tags \
      --resources "${AMI_ID}" \
      --tags "Key=Name,Value=${TARGET_CMS_ENV}-ami-gold-disk-${BASE_GOLD_DISK}" \
      1> /dev/null \
      2> /dev/null
  fi

elif [ -n "${AMI_ID}" ]; then
  echo "Deregister existing ab2d ami..."
  if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
    aws --region "${AWS_DEFAULT_REGION}" ec2 deregister-image \
      --image-id "${AMI_ID}"
  else
    aws --region "${AWS_DEFAULT_REGION}" ec2 deregister-image \
      --image-id "${AMI_ID}" \
      1> /dev/null \
      2> /dev/null
  fi
else
  echo "Note that there is no existing ab2d ami."
fi

# Get the latest seed AMI

if [ -n "${SEED_AMI_PINNED}" ]; then
  SEED_AMI="${SEED_AMI_PINNED}"
else
  SEED_AMI=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
    --owners "${OWNER}" \
    --filters "Name=name,Values=rhel7-gi-*" \
    --query "Images[*].[ImageId,CreationDate]" \
    --output text \
    | sort -k2 -r \
    | head -n1 \
    | awk '{print $1}')
fi

if [ -z "${SEED_AMI}" ]; then
  echo "ERROR: seed AMI not found..."
  exit 1
else
  echo "SEED_AMI=${SEED_AMI}"
  SEED_AMI_NAME=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
    --owners "${OWNER}" \
    --filters "Name=image-id,Values=${SEED_AMI}" \
    --query "Images[*].Name" \
    --output text)
  echo "SEED_AMI_NAME=${SEED_AMI_NAME}"
fi

# Determine commit number

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
COMMIT_NUMBER_OF_CURRENT_BRANCH=$(git rev-parse "${CURRENT_BRANCH}" | cut -c1-7)
COMMIT_NUMBER_OF_ORIGIN_MASTER=$(git rev-parse origin/master | cut -c1-7)

if [ "${COMMIT_NUMBER_OF_CURRENT_BRANCH}" == "${COMMIT_NUMBER_OF_ORIGIN_MASTER}" ]; then
  echo "NOTE: The current branch is the master branch."
  echo "Using commit number of origin/master branch as the image version."
  COMMIT_NUMBER="${COMMIT_NUMBER_OF_ORIGIN_MASTER}"
else
  echo "NOTE: Assuming this is a DevOps branch that has only DevOps changes."
  COMPARE_BRANCH_WITH_MASTER=$(git log \
    --decorate \
    --graph \
    --oneline \
    --cherry-mark \
    --boundary origin/master..."${BRANCH}")
  if [ -z "${COMPARE_BRANCH_WITH_MASTER}" ]; then
    echo "NOTE: DevOps branch is the same as origin/master."
    echo "Using commit number of origin/master branch as the image version."
    COMMIT_NUMBER="${COMMIT_NUMBER_OF_ORIGIN_MASTER}"
  else
    echo "NOTE: DevOps branch is different from origin/master."
    echo "Using commit number of latest merge from origin/master into the current branch as the image version."
    COMMIT_NUMBER=$(git log --merges | head -n 2 | tail -n 1 | cut -d" " -f 3 | cut -c1-7)
  fi
fi

#
# Create AMI for controller, api, and worker nodes
#

# Set 75 minutes of polling at 1 minute intervals
# - this was necessary to prevent a timeout error

export AWS_MAX_ATTEMPTS=75
export AWS_POLL_DELAY_SECONDS=75

# Build the AMI

UNIX_EPOCH_TIME=$(date +%s)
cd "${START_DIR}/.."
cd packer/app
IP=$(curl ipinfo.io/ip)
packer build \
  --var seed_ami="${SEED_AMI}" \
  --var seed_ami_name="${SEED_AMI_NAME}" \
  --var environment="${TARGET_CMS_ENV}" \
  --var region="${AWS_DEFAULT_REGION}" \
  --var ec2_instance_type="${EC2_INSTANCE_TYPE_PACKER}" \
  --var iam_instance_profile="${IAM_INSTANCE_PROFILE}" \
  --var vpc_id="${VPC_ID}" \
  --var subnet_public_1_id="${SUBNET_PUBLIC_1_ID}" \
  --var my_ip_address="${IP}" \
  --var ssh_username="${SSH_USERNAME}" \
  --var git_commit_hash="${COMMIT_NUMBER}" \
  --var unix_epoch_time="${UNIX_EPOCH_TIME}" \
  app.json  2>&1 | tee output.txt

OUTPUT=$(< output.txt)

AMI_ID=$(echo "${OUTPUT}" \
  | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' \
  | tail -1 \
  | tr -d '\000-\011\013\014\016-\037' \
  | cut -d"[" -f 1)

# Add name tag to AMI

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  aws --region "${AWS_DEFAULT_REGION}" ec2 create-tags \
    --resources "${AMI_ID}" \
    --tags "Key=Name,Value=${TARGET_CMS_ENV}-ami"
else
  aws --region "${AWS_DEFAULT_REGION}" ec2 create-tags \
    --resources "${AMI_ID}" \
    --tags "Key=Name,Value=${TARGET_CMS_ENV}-ami" \
    1> /dev/null \
    2> /dev/null
fi
