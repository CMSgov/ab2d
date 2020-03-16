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
  --ec2_instance_type_api=*)
  EC2_INSTANCE_TYPE_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_instance_type_worker=*)
  EC2_INSTANCE_TYPE_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_instance_type_other=*)
  EC2_INSTANCE_TYPE_OTHER="${i#*=}"
  EC2_INSTANCE_TYPE_CONTROLLER="${EC2_INSTANCE_TYPE_OTHER}"
  EC2_INSTANCE_TYPE_PACKER="${EC2_INSTANCE_TYPE_OTHER}"
  shift # past argument=value
  ;;
  --ec2_desired_instance_count_api=*)
  EC2_DESIRED_INSTANCE_COUNT_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_minimum_instance_count_api=*)
  EC2_MINIMUM_INSTANCE_COUNT_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_maximum_instance_count_api=*)
  EC2_MAXIMUM_INSTANCE_COUNT_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_desired_instance_count_worker=*)
  EC2_DESIRED_INSTANCE_COUNT_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_minimum_instance_count_worker=*)
  EC2_MINIMUM_INSTANCE_COUNT_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_maximum_instance_count_worker=*)
  EC2_MAXIMUM_INSTANCE_COUNT_WORKER="${i#*=}"
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

#
# Verify that VPC ID exists
#

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
