#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

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
    || [ -z "${PARENT_ENV_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
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

PARENT_ENV="${PARENT_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

MODULE="data"

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

# shellcheck source=./functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh
source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

# Import the "get temporary AWS credentials via AWS STS assume role" function

# shellcheck source=./functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh
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
# Create or refresh IAM components for target environment
#

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/${MODULE}"

terraform apply \
  --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
  --var "db_allocated_storage_size=${DB_ALLOCATED_STORAGE_SIZE}" \
  --var "db_backup_retention_period=${DB_BACKUP_RETENTION_PERIOD}" \
  --var "db_backup_window=${DB_BACKUP_WINDOW}" \
  --var "db_copy_tags_to_snapshot=${DB_COPY_TAGS_TO_SNAPSHOT}" \
  --var "db_identifier=${CMS_ENV}" \
  --var "db_instance_class=${DB_INSTANCE_CLASS}" \
  --var "db_iops=${DB_IOPS}" \
  --var "db_maintenance_window=${DB_MAINTENANCE_WINDOW}" \
  --var "db_multi_az=${DB_MULTI_AZ}" \
  --var "db_snapshot_id=${DB_SNAPSHOT_ID}" \
  --var "db_parameter_group_name=${CMS_ENV}-rds-parameter-group" \
  --var "db_subnet_group_name=${CMS_ENV}-rds-subnet-group" \
  --var "env=${CMS_ENV}" \
  --var "jenkins_agent_sec_group_id=${JENKINS_AGENT_SEC_GROUP_ID}" \
  --var "parent_env=${PARENT_ENV}" \
  --var "postgres_engine_version=${POSTGRES_ENGINE_VERSION}" \
  --var "region=${AWS_DEFAULT_REGION}" \
  --auto-approve
