#!/bin/bash

set -e # Turn on exit on error
set -x # Turn on verbosity

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if  [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${VPC_ID_PARAM}" ] \
    || [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${JENKINS_AGENT_SEC_GROUP_ID}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

CMS_ENV="${CMS_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

REGION="${REGION_PARAM}"

export AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

VPC_ID="${VPC_ID_PARAM}"

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
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
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
# Verify that VPC ID exists
#

VPC_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpcs \
  --query "Vpcs[?VpcId=='$VPC_ID'].VpcId" \
  --output text)

if [ -z "${VPC_EXISTS}" ]; then
  echo "*********************************************************"
  echo "ERROR: VPC ID does not exist for the target AWS profile."
  echo "*********************************************************"
  exit 1
fi

#
# Create or verify backend components
#

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

# Create or verify tfstate KMS key

export TFSTATE_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/${CMS_ENV}-tfstate-kms'].TargetKeyId" \
  --output text)

if [ -z "${TFSTATE_KMS_KEY_ID}" ]; then

  mkdir -p generated

  j2 tfstate_kms_key_policy.json.j2 -o generated/tfstate_kms_key_policy.json

  export TFSTATE_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms create-key \
    --description "${CMS_ENV}-tfstate-kms" \
    --policy file://generated/tfstate_kms_key_policy.json \
    | jq --raw-output ".KeyMetadata.KeyId")

  aws --region "${AWS_DEFAULT_REGION}" kms create-alias \
    --alias-name "alias/${CMS_ENV}-tfstate-kms" \
    --target-key-id "${TFSTATE_KMS_KEY_ID}"

fi

# Enable or verify tfstate KMS key rotation

KMS_KEY_ROTATION_ENABLED=$(aws kms get-key-rotation-status \
  --key-id "${TFSTATE_KMS_KEY_ID}" \
  --query "KeyRotationEnabled")

if [ "${KMS_KEY_ROTATION_ENABLED}" == "false" ]; then
  aws kms enable-key-rotation \
    --key-id "${TFSTATE_KMS_KEY_ID}"
fi

# Create or verify S3 server-access-logs bucket

export TEST_S3_SERVER_ACCESS_LOGS_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name == '${CMS_ENV}-server-access-logs'].Name" \
  --output text)

if [ -z "${TEST_S3_SERVER_ACCESS_LOGS_BUCKET}" ]; then
  # Create S3 server-access-logs bucket
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${CMS_ENV_PARAM}-server-access-logs"
  TEST_S3_SERVER_ACCESS_LOGS_BUCKET="${CMS_ENV}-server-access-logs"
else
  echo "NOTE: The ${TEST_S3_SERVER_ACCESS_LOGS_BUCKET} bucket exists."
fi

# Block public access on the S3 server-access-logs bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
  --bucket "${TEST_S3_SERVER_ACCESS_LOGS_BUCKET}" \
  2> /dev/null
BUCKET_POLICY_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${TEST_S3_SERVER_ACCESS_LOGS_BUCKET}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# Add or verify that the log-delivery group has WRITE and READ_ACP permissions on the server-access-logs bucket

LOG_DELIVERY_GROUP_PERMISSION_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-acl \
  --bucket "${TEST_S3_SERVER_ACCESS_LOGS_BUCKET}" \
  --query "Grants[?(Permission == 'WRITE' || Permission == 'READ_ACP')].Grantee.URI" \
  | jq .[] \
  | grep http://acs.amazonaws.com/groups/s3/LogDelivery \
  | wc -l \
  | tr -d ' ')

if [ "${LOG_DELIVERY_GROUP_PERMISSION_COUNT}" -lt 2 ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-acl \
    --bucket "${TEST_S3_SERVER_ACCESS_LOGS_BUCKET}" \
    --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
    --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery    
fi

# Create or verify S3 tfstate bucket

export TEST_S3_TFSTATE_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name == '${CMS_ENV}-tfstate'].Name" \
  --output text)

if [ -z "${TEST_S3_TFSTATE_BUCKET}" ]; then
  # Create S3 tfstate bucket
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${CMS_ENV_PARAM}-tfstate"
  export TEST_S3_TFSTATE_BUCKET="${CMS_ENV_PARAM}-tfstate"
else
  echo "NOTE: The ${TEST_S3_TFSTATE_BUCKET} bucket exists."
fi

# Block public access on the S3 tfstate bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
  --bucket "${TEST_S3_TFSTATE_BUCKET}" \
  2> /dev/null
BUCKET_POLICY_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${TEST_S3_TFSTATE_BUCKET}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# Add logging to the S3 tfstate bucket

TEST_S3_TFSTATE_BUCKET_LOGGING=$(aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-logging \
  --bucket "${TEST_S3_TFSTATE_BUCKET}")

if [ -z "${TEST_S3_TFSTATE_BUCKET_LOGGING}" ]; then

  j2 tfstate_bucket_logging.json.j2 -o generated/tfstate_bucket_logging.json

  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-logging \
  --bucket "${TEST_S3_TFSTATE_BUCKET}" \
  --bucket-logging-status file://generated/tfstate_bucket_logging.json
fi

# Add or verify versioning on the S3 tfstate bucket

TEST_S3_TFSTATE_BUCKET_VERSIONING=$(aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-versioning \
  --bucket "${TEST_S3_TFSTATE_BUCKET}")

if [ -z "${TEST_S3_TFSTATE_BUCKET_VERSIONING}" ]; then
  aws s3api put-bucket-versioning --bucket "${TEST_S3_TFSTATE_BUCKET}" \
    --versioning-configuration Status=Enabled
fi

# Add or verify bucket policy on the S3 tfstate bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
  --bucket "${TEST_S3_TFSTATE_BUCKET}" \
  2> /dev/null
BUCKET_POLICY_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then
    
  j2 tfstate_bucket_policy.json.j2 -o generated/tfstate_bucket_policy.json
    
  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-policy \
      --bucket "${TEST_S3_TFSTATE_BUCKET}" \
      --policy file://generated/tfstate_bucket_policy.json
fi

# Add or verify server side encryption on the S3 tfstate bucket

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-encryption \
  --bucket "${TEST_S3_TFSTATE_BUCKET}" \
  2> /dev/null
BUCKET_ENCRYPTION_STATUS=$?
set -e # Turn on exit on error

if [ "${BUCKET_ENCRYPTION_STATUS}" != "0" ]; then

  j2 tfstate_bucket_server_side_encryption.json.j2 -o generated/tfstate_bucket_server_side_encryption.json
    
  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-encryption \
    --bucket "${TEST_S3_TFSTATE_BUCKET}" \
    --server-side-encryption-configuration file://generated/tfstate_bucket_server_side_encryption.json
fi

# Create or verify dynamodb table

# Table.TableName

set +e # Turn off exit on error
aws --region "${AWS_DEFAULT_REGION}" dynamodb describe-table \
  --table-name "${CMS_ENV}-tfstate-table" \
  1> /dev/null \
  2> /dev/null
DYNAMODB_TABLE_STATUS=$?
set -e # Turn on exit on error

if [ "${DYNAMODB_TABLE_STATUS}" != "0" ]; then
  aws --region "${AWS_DEFAULT_REGION}" dynamodb create-table \
    --table-name "${CMS_ENV}-tfstate-table" \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    1> /dev/null
fi

exit

#
# Initialize and validate terraform
#

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-tfstate" \
  -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate


#
# Create or verify S3 automation bucket
#

  # --var "ami_id=$AMI_ID" \
  # --var "current_task_definition_arn=$API_TASK_DEFINITION" \
  # --var "db_host=$DATABASE_HOST" \
  # --var "db_port=$DATABASE_PORT" \
  # --var "db_username=$DATABASE_USER" \
  # --var "db_password=$DATABASE_PASSWORD" \
  # --var "db_name=$DATABASE_NAME" \
  # --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
  # --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
  # --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
  # --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
  # --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
  # --var "deployer_ip_address=$DEPLOYER_IP_ADDRESS" \
  # --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
  # --var "image_version=$IMAGE_VERSION" \
  # --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
  # --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
  # --var "ecs_task_definition_host_port=$ALB_LISTENER_PORT" \
  # --var "host_port=$ALB_LISTENER_PORT" \
  # --var "alb_listener_protocol=$ALB_LISTENER_PROTOCOL" \
  # --var "alb_listener_certificate_arn=$ALB_LISTENER_CERTIFICATE_ARN" \
  # --var "alb_internal=$ALB_INTERNAL" \
  # --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  # --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
  # --var "ab2d_keystore_location=${AB2D_KEYSTORE_LOCATION}" \
  # --var "ab2d_keystore_password=${AB2D_KEYSTORE_PASSWORD}" \
  # --var "ab2d_okta_jwt_issuer=${AB2D_OKTA_JWT_ISSUER}" \
  # --var "ab2d_hpms_url=${AB2D_HPMS_URL}" \
  # --var "ab2d_hpms_api_params=${AB2D_HPMS_API_PARAMS}" \
  # --var "ab2d_hpms_auth_key_id=${AB2D_HPMS_AUTH_KEY_ID}" \
  # --var "ab2d_hpms_auth_key_secret=${AB2D_HPMS_AUTH_KEY_SECRET}" \
  # --var "stunnel_latest_version=${STUNNEL_LATEST_VERSION}" \
  # --var "gold_image_name=${GOLD_IMAGE_NAME}" \

# cd "${START_DIR}/.."
# cd terraform/environments/$CMS_ENV

# terraform apply \
#   --var "env=${CMS_ENV}" \
#   --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
#   --target module.test_s3_automation_bucket \
#   --auto-approve
