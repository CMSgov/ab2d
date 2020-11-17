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
if [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${CPM_BACKUP_DB_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DB_ALLOCATED_STORAGE_SIZE_PARAM}" ] \
    || [ -z "${DB_BACKUP_RETENTION_PERIOD_PARAM}" ] \
    || [ -z "${DB_BACKUP_WINDOW_PARAM}" ] \
    || [ -z "${DB_COPY_TAGS_TO_SNAPSHOT_PARAM}" ] \
    || [ -z "${DB_INSTANCE_CLASS_PARAM}" ] \
    || [ -z "${DB_IOPS_PARAM}" ] \
    || [ -z "${DB_MAINTENANCE_WINDOW_PARAM}" ] \
    || [ -z "${DB_MULTI_AZ_PARAM}" ] \
    || [ -z "${DB_SNAPSHOT_ID_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${JENKINS_AGENT_SEC_GROUP_ID_PARAM}" ] \
    || [ -z "${PARENT_ENV_PARAM}" ] \
    || [ -z "${POSTGRES_ENGINE_VERSION_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

CMS_ENV="${CMS_ENV_PARAM}"

CPM_BACKUP_DB="${CPM_BACKUP_DB_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

DB_ALLOCATED_STORAGE_SIZE="${DB_ALLOCATED_STORAGE_SIZE_PARAM}"

DB_BACKUP_RETENTION_PERIOD="${DB_BACKUP_RETENTION_PERIOD_PARAM}"

DB_BACKUP_WINDOW="${DB_BACKUP_WINDOW_PARAM}"

DB_COPY_TAGS_TO_SNAPSHOT="${DB_COPY_TAGS_TO_SNAPSHOT_PARAM}"

DB_INSTANCE_CLASS="${DB_INSTANCE_CLASS_PARAM}"

DB_IOPS="${DB_IOPS_PARAM}"

DB_MAINTENANCE_WINDOW="${DB_MAINTENANCE_WINDOW_PARAM}"

DB_MULTI_AZ="${DB_MULTI_AZ_PARAM}"

DB_SNAPSHOT_ID="${DB_SNAPSHOT_ID_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

JENKINS_AGENT_SEC_GROUP_ID="${JENKINS_AGENT_SEC_GROUP_ID_PARAM}"

MODULE="data"

PARENT_ENV="${PARENT_ENV_PARAM}"

POSTGRES_ENGINE_VERSION="${POSTGRES_ENGINE_VERSION_PARAM}"

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
  exit 1
else
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"
fi

# Set DB_SNAPSHOT_ID to blank, if "N/A"

if [ "${DB_SNAPSHOT_ID}" == "N/A" ]; then
  DB_SNAPSHOT_ID=""
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
# Set or verify secrets
#

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

# Create or get database password secret

AB2D_DB_PASSWORD=$(./get-database-secret.py "${CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" database_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_DB_PASSWORD=$(./get-database-secret.py "${CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")
fi

# Create or get database user secret

AB2D_DB_USER=$(./get-database-secret.py "${CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_USER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" database_user "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_DB_USER=$(./get-database-secret.py "${CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")
fi

# If any secret produced an error, exit the script

if [ "${AB2D_DB_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_DB_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
  echo "ERROR: Cannot get secrets because KMS key is disabled!"
  exit 1
fi

#
# Create or refresh data components for target environment
#

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/${MODULE}"

terraform apply \
  --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
  --var "cpm_backup_db=${CPM_BACKUP_DB}" \
  --var "db_allocated_storage_size=${DB_ALLOCATED_STORAGE_SIZE}" \
  --var "db_backup_retention_period=${DB_BACKUP_RETENTION_PERIOD}" \
  --var "db_backup_window=${DB_BACKUP_WINDOW}" \
  --var "db_copy_tags_to_snapshot=${DB_COPY_TAGS_TO_SNAPSHOT}" \
  --var "db_identifier=${CMS_ENV}" \
  --var "db_instance_class=${DB_INSTANCE_CLASS}" \
  --var "db_iops=${DB_IOPS}" \
  --var "db_maintenance_window=${DB_MAINTENANCE_WINDOW}" \
  --var "db_multi_az=${DB_MULTI_AZ}" \
  --var "db_parameter_group_name=${CMS_ENV}-rds-parameter-group" \
  --var "db_password=${AB2D_DB_PASSWORD}" \
  --var "db_snapshot_id=${DB_SNAPSHOT_ID}" \
  --var "db_subnet_group_name=${CMS_ENV}-rds-subnet-group" \
  --var "db_username=${AB2D_DB_USER}" \
  --var "env=${CMS_ENV}" \
  --var "jenkins_agent_sec_group_id=${JENKINS_AGENT_SEC_GROUP_ID}" \
  --var "parent_env=${PARENT_ENV}" \
  --var "postgres_engine_version=${POSTGRES_ENGINE_VERSION}" \
  --var "region=${AWS_DEFAULT_REGION}" \
  --auto-approve
