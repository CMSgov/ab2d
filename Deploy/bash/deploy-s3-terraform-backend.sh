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
if  [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

CMS_ENV="${CMS_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

export AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

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

#
# Define function to create or verify dynamodb table for module
#

fn_create_or_verify_dynamodb_table_for_module ()
{
  # Set parameters

  IN_MODULE="$1"

  set +e # Turn off exit on error
  aws --region "${AWS_DEFAULT_REGION}" dynamodb describe-table \
    --table-name "${CMS_ENV}-${IN_MODULE}-tfstate-table" \
    1> /dev/null \
    2> /dev/null
  DYNAMODB_TABLE_STATUS=$?
  set -e # Turn on exit on error

  if [ "${DYNAMODB_TABLE_STATUS}" != "0" ]; then
    aws --region "${AWS_DEFAULT_REGION}" dynamodb create-table \
      --table-name "${CMS_ENV}-${IN_MODULE}-tfstate-table" \
      --attribute-definitions AttributeName=LockID,AttributeType=S \
      --key-schema AttributeName=LockID,KeyType=HASH \
      --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
      1> /dev/null
  fi
}

#
# Configure terraform
#

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

#
# Create or verify backend components
#

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/core"

# Create or verify tfstate KMS key

GET_TFSTATE_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/${CMS_ENV}-tfstate-kms'].TargetKeyId" \
  --output text)

if [ -z "${GET_TFSTATE_KMS_KEY_ID}" ]; then

  mkdir -p generated

  j2 tfstate_kms_key_policy.json.j2 -o generated/tfstate_kms_key_policy.json

  GET_TFSTATE_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms create-key \
    --description "${CMS_ENV}-tfstate-kms" \
    --policy file://generated/tfstate_kms_key_policy.json \
    | jq --raw-output ".KeyMetadata.KeyId")

  aws --region "${AWS_DEFAULT_REGION}" kms create-alias \
    --alias-name "alias/${CMS_ENV}-tfstate-kms" \
    --target-key-id "${TFSTATE_KMS_KEY_ID}"

fi

export TFSTATE_KMS_KEY_ID="${GET_TFSTATE_KMS_KEY_ID}"

# Enable or verify tfstate KMS key rotation

KMS_KEY_ROTATION_ENABLED=$(aws kms get-key-rotation-status \
  --key-id "${TFSTATE_KMS_KEY_ID}" \
  --query "KeyRotationEnabled")

if [ "${KMS_KEY_ROTATION_ENABLED}" == "false" ]; then
  aws kms enable-key-rotation \
    --key-id "${TFSTATE_KMS_KEY_ID}"
fi

# Create or verify S3 tfstate-server-access-logs bucket

GET_S3_SERVER_ACCESS_LOGS_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name == '${CMS_ENV}-tfstate-server-access-logs'].Name" \
  --output text)

if [ -z "${GET_S3_SERVER_ACCESS_LOGS_BUCKET}" ]; then
  # Create S3 tfstate-server-access-logs bucket
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${CMS_ENV}-tfstate-server-access-logs"
  GET_S3_SERVER_ACCESS_LOGS_BUCKET="${CMS_ENV}-tfstate-server-access-logs"
else
  echo "NOTE: The ${GET_S3_SERVER_ACCESS_LOGS_BUCKET} bucket exists."
fi

export S3_SERVER_ACCESS_LOGS_BUCKET="${GET_S3_SERVER_ACCESS_LOGS_BUCKET}"

# Block public access on the S3 tfstate-server-access-logs bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
  --bucket "${S3_SERVER_ACCESS_LOGS_BUCKET}" \
  2> /dev/null
BUCKET_POLICY_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${S3_SERVER_ACCESS_LOGS_BUCKET}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# Add or verify that the log-delivery group has WRITE and READ_ACP permissions on the tfstate-server-access-logs bucket

LOG_DELIVERY_GROUP_PERMISSION_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-acl \
  --bucket "${S3_SERVER_ACCESS_LOGS_BUCKET}" \
  --query "Grants[?(Permission == 'WRITE' || Permission == 'READ_ACP')].Grantee.URI" \
  | jq .[] \
  | grep -c http://acs.amazonaws.com/groups/s3/LogDelivery \
  | tr -d ' ')

if [ "${LOG_DELIVERY_GROUP_PERMISSION_COUNT}" -lt 2 ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-acl \
    --bucket "${S3_SERVER_ACCESS_LOGS_BUCKET}" \
    --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
    --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
    1> /dev/null
fi

# Create or verify S3 tfstate bucket

GET_S3_TFSTATE_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name == '${CMS_ENV}-tfstate'].Name" \
  --output text)

if [ -z "${GET_S3_TFSTATE_BUCKET}" ]; then
  # Create S3 tfstate bucket
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${CMS_ENV}-tfstate"
  GET_S3_TFSTATE_BUCKET="${CMS_ENV}-tfstate"
else
  echo "NOTE: The ${GET_S3_TFSTATE_BUCKET} bucket exists."
fi

export S3_TFSTATE_BUCKET="${GET_S3_TFSTATE_BUCKET}"

# Block public access on the S3 tfstate bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
  --bucket "${S3_TFSTATE_BUCKET}" \
  2> /dev/null
BUCKET_POLICY_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${S3_TFSTATE_BUCKET}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
    1> /dev/null
fi

# Add logging to the S3 tfstate bucket

S3_TFSTATE_BUCKET_LOGGING=$(aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-logging \
  --bucket "${S3_TFSTATE_BUCKET}")

if [ -z "${S3_TFSTATE_BUCKET_LOGGING}" ]; then

  mkdir -p generated

  j2 tfstate_bucket_logging.json.j2 -o generated/tfstate_bucket_logging.json

  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-logging \
    --bucket "${S3_TFSTATE_BUCKET}" \
    --bucket-logging-status file://generated/tfstate_bucket_logging.json \
    1> /dev/null
fi

# Add or verify versioning on the S3 tfstate bucket

S3_TFSTATE_BUCKET_VERSIONING=$(aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-versioning \
  --bucket "${S3_TFSTATE_BUCKET}")

if [ -z "${S3_TFSTATE_BUCKET_VERSIONING}" ]; then
  aws s3api put-bucket-versioning --bucket "${S3_TFSTATE_BUCKET}" \
    --versioning-configuration Status=Enabled \
    1> /dev/null
fi

# Add or verify bucket policy on the S3 tfstate bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
  --bucket "${S3_TFSTATE_BUCKET}" \
  2> /dev/null
BUCKET_POLICY_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then

  mkdir -p generated

  j2 tfstate_bucket_policy.json.j2 -o generated/tfstate_bucket_policy.json
    
  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-policy \
      --bucket "${S3_TFSTATE_BUCKET}" \
      --policy file://generated/tfstate_bucket_policy.json \
      1> /dev/null
fi

# Add or verify server side encryption on the S3 tfstate bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-encryption \
  --bucket "${S3_TFSTATE_BUCKET}" \
  2> /dev/null
BUCKET_ENCRYPTION_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_ENCRYPTION_STATUS}" != "0" ]; then

  mkdir -p generated

  j2 tfstate_bucket_server_side_encryption.json.j2 -o generated/tfstate_bucket_server_side_encryption.json
    
  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-encryption \
    --bucket "${S3_TFSTATE_BUCKET}" \
    --server-side-encryption-configuration file://generated/tfstate_bucket_server_side_encryption.json \
    1> /dev/null
fi

# Create or verify dynamodb table for core module

fn_create_or_verify_dynamodb_table_for_module "core"

# Create or verify dynamodb table for data module

fn_create_or_verify_dynamodb_table_for_module "data"

# Create or verify dynamodb table for data module

fn_create_or_verify_dynamodb_table_for_module "worker"
