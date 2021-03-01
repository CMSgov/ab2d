#!/bin/bash

set -e # Turn on exit on error
set +x # <-- Do not change this value!
       # Logging is turned on in a later step based on CLOUD_TAMER_PARAM.
       # CLOUD_TAMER_PARAM = false (Jenkins assumed; verbose logging turned off)
       # CLOUD_TAMER_PARAM = true (Dev machine assumed; verbose logging turned on)
       # NOTE: Setting the CLOUD_TAMER_PARAM to a value that does not match the
       #       assumed host machine will cause the script to fail.

#
# Change to working directory (if not set by a parent script)
#

if [ -z "${START_DIR}" ]; then
  START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
  cd "${START_DIR}"
fi

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${ENV_PASCAL_CASE_PARAM}" ] \
    || [ -z "${PARENT_ENV_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

CMS_ENV="${CMS_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

ENV_PASCAL_CASE="${ENV_PASCAL_CASE_PARAM}"

MODULE="core"

PARENT_ENV="${PARENT_ENV_PARAM}"

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

#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
fi

# If S3_TFSTATE_BUCKET is not set by a previous module, set it via the aws cli

if [ -z "${S3_TFSTATE_BUCKET}" ]; then
  S3_TFSTATE_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
    --query "Buckets[?Name == '${CMS_ENV}-tfstate'].Name" \
    --output text)
fi

# If TFSTATE_KMS_KEY_ID is not set by a previous module, set it via the aws cli

if [ -z "${TFSTATE_KMS_KEY_ID}" ]; then
  TFSTATE_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
    --query="Aliases[?AliasName=='alias/${CMS_ENV}-tfstate-kms'].TargetKeyId" \
    --output text)
fi

#
# Configure terraform
#

# Reset logging

if [ "${CLOUD_TAMER}" == "true" ]; then

  # Enable terraform logging on development machine
  echo "Setting terraform debug level to $DEBUG_LEVEL..."
  export TF_LOG=$DEBUG_LEVEL
  export TF_LOG_PATH=/var/log/terraform/tf.log
  rm -f /var/log/terraform/tf.log

else

  # Disable terraform logging on Jenkins
  export TF_LOG=

fi

#
# Initialize and validate terraform
#

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/${MODULE}"

rm -f ./*.tfvars

terraform init \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="bucket=${S3_TFSTATE_BUCKET}" \
  -backend-config="key=${CMS_ENV}/terraform/${MODULE}/terraform.tfstate" \
  -backend-config="encrypt=true" \
  -backend-config="kms_key_id=${TFSTATE_KMS_KEY_ID}" \
  -backend-config="dynamodb_table=${CMS_ENV}-${MODULE}-tfstate-table"

terraform validate

#
# Create or refresh core components for target environment
#

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/${MODULE}"

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
    --var "env=${CMS_ENV}" \
    --var "env_pascal_case=${ENV_PASCAL_CASE}" \
    --var "parent_env=${PARENT_ENV}" \
    --var "region=${AWS_DEFAULT_REGION}" \
    --auto-approve
else
  terraform apply \
    --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
    --var "env=${CMS_ENV}" \
    --var "env_pascal_case=${ENV_PASCAL_CASE}" \
    --var "parent_env=${PARENT_ENV}" \
    --var "region=${AWS_DEFAULT_REGION}" \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

# Create of verify key pair

KEY_NAME=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-key-pairs \
  --filters "Name=key-name,Values=${CMS_ENV}" \
  --query "KeyPairs[*].KeyName" \
  --output text)

if [ -z "${KEY_NAME}" ]; then

  if [ "${CLOUD_TAMER}" == "true" ]; then

    # Create private key

    aws --region "${AWS_DEFAULT_REGION}" ec2 create-key-pair \
      --key-name "${CMS_ENV}" \
      --query 'KeyMaterial' \
      --output text \
      > "${HOME}/.ssh/${CMS_ENV}.pem"

    # Set permissions on private key

    chmod 600 "${HOME}/.ssh/${CMS_ENV}.pem"

  fi
fi
