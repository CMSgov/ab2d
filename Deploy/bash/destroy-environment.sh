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
  --keep-ami)
  KEEP_AMI="true"
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
if [ -z "${ENVIRONMENT}" ] || [ -z "${SHARED_ENVIRONMENT}" ]; then
  echo "Try running the script like one of these options:"
  echo "./destroy-environment.sh --environment=dev --shared-environment=shared"
  echo "./destroy-environment.sh --environment=dev --shared-environment=shared --keep-ami"
  exit 1
fi

#
# Set environment
#

export AWS_PROFILE="${CMS_ENV}"

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
cd ../terraform/environments/$CMS_SHARED_ENV

# LSH Temporary BEGIN
# terraform init \
#   -backend-config="bucket=ab2d-${CMS_ENV}-automation" \
#   -backend-config="key=${CMS_SHARED_ENV}/terraform/terraform.tfstate" \
#   -backend-config="region=us-east-1" \
#   -backend-config="encrypt=true"
terraform init \
  -backend-config="bucket=cms-ab2d-automation" \
  -backend-config="key=ab2d-shared/terraform/terraform.tfstate" \
  -backend-config="region=us-east-1" \
  -backend-config="encrypt=true"
# LSH Temporary END

terraform validate

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}"
cd ../terraform/environments/$CMS_ENV

# LSH Temporary BEGIN
# terraform init \
#   -backend-config="bucket=ab2d-${CMS_ENV}-automation" \
#   -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
#   -backend-config="region=us-east-1" \
#   -backend-config="encrypt=true"
terraform init \
  -backend-config="bucket=cms-ab2d-automation" \
  -backend-config="key=ab2d-dev/terraform/terraform.tfstate" \
  -backend-config="region=us-east-1" \
  -backend-config="encrypt=true"
# LSH Temporary END

terraform validate

#
# Change to target environment directory
#

cd "${START_DIR}"
cd ../terraform/environments/$CMS_ENV

#
# Destroy the api and worker modules
#

# Get load balancer ARN (if exists)

LOAD_BALANCERS_EXIST=$(aws --region us-east-1 elbv2 describe-load-balancers \
  --query "LoadBalancers[*].[LoadBalancerArn]" \
  --output text \
  | grep "${CMS_ENV}" \
  | xargs \
  | tr -d '\r')
if [ -n "${LOAD_BALANCERS_EXIST}" ]; then
  ALB_ARN=$(aws --region us-east-1 elbv2 describe-load-balancers \
    --name=$CMS_ENV \
    --query 'LoadBalancers[*].[LoadBalancerArn]' \
    --output text)
fi

# Turn off "delete protection" for the application load balancer

echo "Turn off 'delete protection' for the application load balancer..."
if [ -n "${ALB_ARN}" ]; then
  aws --region us-east-1 elbv2 modify-load-balancer-attributes \
    --load-balancer-arn $ALB_ARN \
    --attributes Key=deletion_protection.enabled,Value=false
fi

#
#  Detach AWS Shield standard from the application load balancer
#

# Note that no change is actually made since AWS shield standard remains applied to
# the application load balancer until the load balancer itself is removed. This section
# may be needed later if AWS Shield Advanced was applied instead.

echo "Destroying shield components..."
terraform destroy \
  --target module.shield \
  --auto-approve

# Destroy the environment of the "WAF" module

echo "Destroying WAF components..."
terraform destroy \
  --target module.waf \
  --auto-approve

# Destroy the environment of the "CloudWatch" module

echo "Destroying CloudWatch components..."
terraform destroy \
  --target module.cloudwatch \
  --auto-approve

# Destroy the environment of the "worker" module

echo "Destroying worker components..."
terraform destroy \
  --target module.worker \
  --auto-approve

# Destroy the environment of the "api" module

echo "Destroying API components..."
terraform destroy \
  --target module.api \
  --auto-approve

#
# Destroy efs module
#

# Destroy the environment of the "efs" module
echo "Destroying EFS components..."
terraform destroy \
  --target module.efs \
  --auto-approve

#
# Change to shared environment directory
#

cd "${START_DIR}"
cd ../terraform/environments/$CMS_SHARED_ENV

#
# Destroy controller module
#

echo "Destroying controller..."
terraform destroy \
  --target module.controller \
  --auto-approve

#
# Destroy db module
#

echo "Destroying DB components..."
terraform destroy \
  --target module.db \
  --auto-approve

#
# Destroy all S3 buckets except for the "automation" bucket
#

# Destroy the environment of the "s3" module

echo "Destroying S3 components..."
terraform destroy \
  --target module.s3 --auto-approve

# Destroy environment specific cloudtrail S3 key

aws s3 rm s3://$CMS_ENV-cloudtrail/$CMS_ENV \
  --recursive

#
# Disable "kms" key
#

# Rerun db destroy again to ensure that it is in correct state
# - this is a workaround that prevents the kms module from raising an eror sporadically

echo "Rerun module.db destroy to ensure proper state..."
terraform destroy \
  --target module.db --auto-approve

# Destroy the KMS module
echo "Disabling KMS key..."
KMS_KEY_ID=$(aws kms describe-key --key-id alias/ab2d-kms --query="KeyMetadata.KeyId")
if [ -n "$KMS_KEY_ID" ]; then
  cd "${START_DIR}"
  cd ../python3
  ./disable-kms-key.py $KMS_KEY_ID
  cd "${START_DIR}"
  cd ../terraform/environments/$CMS_ENV
fi

#
# Deregister the application AMI
#

# Get application AMI ID (if exists)
APPLICATION_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${APPLICATION_AMI_ID}" ]; then
  if [ -n "${KEEP_AMI}" ]; then
    echo "No existing application AMI to preserve..."
  fi
else
  if [ -z "${KEEP_AMI}" ]; then
    echo "Deregistering application AMI..."
    aws --region us-east-1 ec2 deregister-image \
      --image-id $APPLICATION_AMI_ID
  else
    echo "Preserving application AMI..."
  fi
fi

#
# Deregister the Jenkins AMI
#

# Get application AMI ID (if exists)
JENKINS_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-jenkins-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${JENKINS_AMI_ID}" ]; then
  if [ -n "${KEEP_AMI}" ]; then
    echo "No existing Jenkins AMI to preserve..."
  fi
else
  if [ -z "${KEEP_AMI}" ]; then
    echo "Deregistering Jenkins AMI..."
    aws --region us-east-1 ec2 deregister-image \
      --image-id $JENKINS_AMI_ID
  else
    echo "Preserving Jenkins AMI..."
  fi
fi

# Destroy shared environment cloudtrail S3 key

aws s3 rm "s3://${CMS_ENV}-cloudtrail/${CMS_ENV}" \
  --recursive

#
# Delete applicable the tfstate files and ".terraform" directories
#

# Delete applicable the tfstate files and ".terraform" directories for the target environment

echo "Delete applicable the tfstate files and ".terraform" directories..."

cd "${START_DIR}"
cd ../terraform/environments/$CMS_ENV
aws s3 rm "s3://${CMS_ENV}-automation/${CMS_ENV}" \
  --recursive
rm -rf .terraform

# Delete applicable the tfstate files and ".terraform" directories for the shared environment
# (if no target environments exist)
  
cd "${START_DIR}"
cd ../terraform/environments/$CMS_SHARED_ENV
aws s3 rm "s3://${CMS_ENV}-automation/${CMS_SHARED_ENV}" \
  --recursive
rm -rf .terraform    

#
# Delete any orphaned components using AWS CLI
# - orphaned components may exist if their were automation issues with terraform
#

# Delete orphaned EFS (if exists)
    
EFS_FS_ID=$(aws efs describe-file-systems \
  --query="FileSystems[?CreationToken=='${CMS_ENV}-efs'].FileSystemId" \
  --output=text)

if [ -n "${EFS_FS_ID}" ]; then
  aws efs delete-file-system \
    --file-system-id "${EFS_FS_ID}"
fi

#
# Done
#

echo "Done"
