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
if [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

CMS_ENV="${CMS_ENV_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
  exit 1
else
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"
fi

#
# Set AWS account numbers
#

CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER=653916833532

if [ "${CMS_ENV}" == "ab2d-dev" ]; then
  CMS_ENV_AWS_ACCOUNT_NUMBER=349849222861
elif [ "${CMS_ENV}" == "ab2d-sbx-sandbox" ]; then
  CMS_ENV_AWS_ACCOUNT_NUMBER=777200079629
elif [ "${CMS_ENV}" == "ab2d-east-impl" ]; then
  CMS_ENV_AWS_ACCOUNT_NUMBER=330810004472
elif [ "${CMS_ENV}" == "ab2d-east-prod" ]; then
  CMS_ENV_AWS_ACCOUNT_NUMBER=595094747606
else
  echo "ERROR: 'CMS_ENV' environment is unknown."
  exit 1  
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
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
fi

#
# Get secrets
#

# Change to "python3" directory

cd "${START_DIR}/.."
cd python3

# Get database user secret

DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)

if [ -z "${DATABASE_USER}" ]; then
  echo "**************************************"
  echo "ERROR: database user secret not found."
  echo "**************************************"
  exit 1
fi

# Get database password secret

DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)

if [ -z "${DATABASE_PASSWORD}" ]; then
  echo "******************************************"
  echo "ERROR: database password secret not found."
  echo "******************************************"
  exit 1
fi

# Get database name secret

DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)

if [ -z "${DATABASE_NAME}" ]; then
  echo "***************************************"
  echo "ERROR: database name secret not found."
  echo "**************************************"
  exit 1
fi

# Get database host secret

DATABASE_HOST=$(./get-database-secret.py $CMS_ENV database_host $DATABASE_SECRET_DATETIME)

if [ -z "${DATABASE_HOST}" ]; then
  echo "***************************************************"
  echo "ERROR: database host secret not found."
  echo "***************************************************"
  exit 1
fi

# Get database port secret

DATABASE_PORT=$(./get-database-secret.py $CMS_ENV database_port $DATABASE_SECRET_DATETIME)

if [ -z "${DATABASE_PORT}" ]; then
  echo "***************************************************"
  echo "ERROR: database port secret not found."
  echo "***************************************************"
  exit 1
fi

#
# Backup database tables as csv files
#

# Create a database_backup directory for the target environment
  
rm -rf "/tmp/database_backup/${CMS_ENV}"
mkdir -p "/tmp/database_backup/${CMS_ENV}"

# Set PostgreSQL password

export PGPASSWORD="${DATABASE_PASSWORD}"

# Create a schema script

pg_dump \
  --dbname='impl' \
  --host='ab2d.c8ic6b9oakpi.us-east-1.rds.amazonaws.com' \
  --username='cmsadmin' \
  --schema='public' \
  --schema-only \
  --no-owner \
  --no-acl \
  --format=p \
  --encoding='UTF8' \
  --file='/tmp/ab2d-east-impl/01-public-schema.sql'

sed -i '/^CREATE SCHEMA.*$/d' "/tmp/database_backup/${CMS_ENV}/01-public-schema.sql"

# Change to the ruby directory

cd "${START_DIR}/.."
cd ruby

# Get data as CSV files

bundle install
bundle exec rake database_public_get_data
