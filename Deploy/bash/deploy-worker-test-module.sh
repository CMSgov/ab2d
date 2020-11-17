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
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${PARENT_ENV_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

CMS_ENV="${CMS_ENV_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

MODULE="worker_test"

PARENT_ENV="${PARENT_ENV_PARAM}"

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
# Set or verify secrets
#

# Get database host and port

DB_ENDPOINT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='${CMS_ENV}'].Endpoint.Address" \
  --output=text)

DB_PORT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='${CMS_ENV}'].Endpoint.Port" \
  --output=text)

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

# Create or get bfd keystore location secret

AB2D_BFD_KEYSTORE_LOCATION=$(./get-database-secret.py "${CMS_ENV}" bfd_keystore_location "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_BFD_KEYSTORE_LOCATION}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" bfd_keystore_location "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_BFD_KEYSTORE_LOCATION=$(./get-database-secret.py "${CMS_ENV}" bfd_keystore_location "${DATABASE_SECRET_DATETIME}")
fi

# Create or get bfd keystore password secret

AB2D_BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py "${CMS_ENV}" bfd_keystore_password "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_BFD_KEYSTORE_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" bfd_keystore_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py "${CMS_ENV}" bfd_keystore_password "${DATABASE_SECRET_DATETIME}")
fi

# Create or get bfd url secret

AB2D_BFD_URL=$(./get-database-secret.py "${CMS_ENV}" bfd_url "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_BFD_URL}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" bfd_url "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_BFD_URL=$(./get-database-secret.py "${CMS_ENV}" bfd_url "${DATABASE_SECRET_DATETIME}")
fi

# Create or get database name secret

AB2D_DB_DATABASE=$(./get-database-secret.py "${CMS_ENV}" database_name "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_DATABASE}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" database_name "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_DB_DATABASE=$(./get-database-secret.py "${CMS_ENV}" database_name "${DATABASE_SECRET_DATETIME}")
fi

# Create or get database host secret

AB2D_DB_HOST=$(./get-database-secret.py "${CMS_ENV}" database_host "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_HOST}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_ENDPOINT}"
  AB2D_DB_HOST=$(./get-database-secret.py "${CMS_ENV}" database_host "${DATABASE_SECRET_DATETIME}")
fi

# Create or get database password secret

AB2D_DB_PASSWORD=$(./get-database-secret.py "${CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" database_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_DB_PASSWORD=$(./get-database-secret.py "${CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")
fi

# Create or get database port secret

AB2D_DB_PORT=$(./get-database-secret.py "${CMS_ENV}" database_port "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_PORT}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_PORT}"
  AB2D_DB_PORT=$(./get-database-secret.py "${CMS_ENV}" database_port "${DATABASE_SECRET_DATETIME}")
fi

# Create or get database user secret

AB2D_DB_USER=$(./get-database-secret.py "${CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_USER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" database_user "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_DB_USER=$(./get-database-secret.py "${CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")
fi

# Create or get hicn hash iter secret

AB2D_HICN_HASH_ITER=$(./get-database-secret.py "${CMS_ENV}" hicn_hash_iter "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_HICN_HASH_ITER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" hicn_hash_iter "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_HICN_HASH_ITER=$(./get-database-secret.py "${CMS_ENV}" hicn_hash_iter "${DATABASE_SECRET_DATETIME}")
fi

# Create or get hicn hash pepper secret

AB2D_HICN_HASH_PEPPER=$(./get-database-secret.py "${CMS_ENV}" hicn_hash_pepper "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_HICN_HASH_PEPPER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" hicn_hash_pepper "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_HICN_HASH_PEPPER=$(./get-database-secret.py "${CMS_ENV}" hicn_hash_pepper "${DATABASE_SECRET_DATETIME}")
fi

# Create or get new relic app name secret

NEW_RELIC_APP_NAME=$(./get-database-secret.py "${CMS_ENV}" new_relic_app_name "${DATABASE_SECRET_DATETIME}")
if [ -z "${NEW_RELIC_APP_NAME}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" new_relic_app_name "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  NEW_RELIC_APP_NAME=$(./get-database-secret.py "${CMS_ENV}" new_relic_app_name "${DATABASE_SECRET_DATETIME}")
fi

# Create or get new relic license key secret

NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py "${CMS_ENV}" new_relic_license_key "${DATABASE_SECRET_DATETIME}")
if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" new_relic_license_key "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py "${CMS_ENV}" new_relic_license_key "${DATABASE_SECRET_DATETIME}")
fi

# If any secret produced an error, exit the script

if [ "${AB2D_BFD_KEYSTORE_LOCATION}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_BFD_KEYSTORE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_BFD_URL}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_DB_DATABASE}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_DB_HOST}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_DB_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_DB_PORT}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_DB_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_HICN_HASH_ITER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_HICN_HASH_PEPPER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${NEW_RELIC_APP_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
  echo "ERROR: Cannot get secrets because KMS key is disabled!"
  exit 1
fi
