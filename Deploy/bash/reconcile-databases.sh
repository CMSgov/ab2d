#!/bin/bash -l
# Note: that "-l" was added to the first line to make bash a login shell
# - this causes .bash_profile and .bashrc to be sourced which is needed for ruby items

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
if [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${EXCLUDE_PROD_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM}" ] \
    || [ -z "${EXCLUDE_SBX_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM}" ] \
    || [ -z "${SOURCE_CMS_ENV_PARAM}" ] \
    || [ -z "${TARGET_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${TARGET_CMS_ENV_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

export DATABASE_HOST=""

export DATABASE_NAME=""

export DATABASE_PASSWORD=""

export DATABASE_PORT=""

export DATABASE_SCHEMA_NAME=""

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DATABASE_USER=""

EXCLUDE_PROD_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION="${EXCLUDE_PROD_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM}"

EXCLUDE_SBX_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION="${EXCLUDE_SBX_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION_PARAM}"

SOURCE_CMS_ENV="${SOURCE_CMS_ENV_PARAM}"

TARGET_CMS_ENV="${TARGET_CMS_ENV_PARAM}"

TARGET_AWS_ACCOUNT_NUMBER="${TARGET_AWS_ACCOUNT_NUMBER_PARAM}"

# Set whether CloudTamer API should be used

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER}" != "false" ]; then
  echo ""
  echo "*********************************************************"
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be false"
  echo ""
  echo "CloudTamer is not currently supported for this script"
  echo "*********************************************************"
  echo ""
  exit 1
else
  echo "NOTE: Cloudtamer is not being used."
fi

# Exit with error if the target is production

if [ "${TARGET_CMS_ENV}" == "${EXCLUDE_PROD_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION}" ]; then
  echo ""
  echo "*********************************************************"
  echo "ERROR: TARGET_CMS_ENV cannot be production"
  echo "*********************************************************"
  echo ""
  exit 1
fi

# Exit with error if the target is production

if [ "${TARGET_CMS_ENV}" == "${EXCLUDE_SBX_CMS_ENV_AS_TARGET_OF_DATABASE_MIGRATION}" ]; then
  echo ""
  echo "*********************************************************"
  echo "ERROR: TARGET_CMS_ENV cannot be sandbox"
  echo "*********************************************************"
  echo ""
  exit 1
fi

#
# Define functions
#

# Import the "get temporary AWS credentials via AWS STS assume role" function

# shellcheck disable=SC1090
source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

#
# Reconcile production validation with production
#

# Set AWS target environment

fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${TARGET_AWS_ACCOUNT_NUMBER}" "${TARGET_CMS_ENV}"

# Change to "python3" directory

cd "${START_DIR}/.."
cd python3

# Get database user secret

DATABASE_USER=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_USER}" ]; then
  echo "**************************************"
  echo "ERROR: database user secret not found."
  echo "**************************************"
  exit 1
fi

# Get database password secret

DATABASE_PASSWORD=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_PASSWORD}" ]; then
  echo "******************************************"
  echo "ERROR: database password secret not found."
  echo "******************************************"
  exit 1
fi

# Get database name secret

DATABASE_NAME=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_name "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_NAME}" ]; then
  echo "***************************************"
  echo "ERROR: database name secret not found."
  echo "**************************************"
  exit 1
fi

# Get database schema name secret

DATABASE_SCHEMA_NAME=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_schema_name "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_SCHEMA_NAME}" ]; then
  echo "***************************************"
  echo "ERROR: database name secret not found."
  echo "**************************************"
  exit 1
fi

# Get database host secret

DATABASE_HOST=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_host "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_HOST}" ]; then
  echo "***************************************************"
  echo "ERROR: database host secret not found."
  echo "***************************************************"
  exit 1
fi

# Get database port secret

DATABASE_PORT=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_port "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_PORT}" ]; then
  echo "***************************************************"
  echo "ERROR: database port secret not found."
  echo "***************************************************"
  exit 1
fi

# Set PostgreSQL password

export PGPASSWORD="${DATABASE_PASSWORD}"

#
# Reconcile databases
#

# Truncate target tables

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.sponsor CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.contract CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.user_account CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.role CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.user_role CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.coverage CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.event_bene_coverage_search_status_change CASCADE;"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="TRUNCATE ${DATABASE_SCHEMA_NAME}.bene_coverage_period CASCADE;"

# Restore data from CSVs to target tables

cd "${HOME}/database_backup/${SOURCE_CMS_ENV}/csv"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.sponsor FROM '${DATABASE_SCHEMA_NAME}.sponsor.csv' WITH (FORMAT CSV);"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.contract FROM '${DATABASE_SCHEMA_NAME}.contract.csv' WITH (FORMAT CSV);"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.user_account FROM '${DATABASE_SCHEMA_NAME}.user_account.csv' WITH (FORMAT CSV);"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.role FROM '${DATABASE_SCHEMA_NAME}.role.csv' WITH (FORMAT CSV);"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.user_role FROM '${DATABASE_SCHEMA_NAME}.user_role.csv' WITH (FORMAT CSV);"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.bene_coverage_period FROM '${DATABASE_SCHEMA_NAME}.bene_coverage_period.csv' WITH (FORMAT CSV);"

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --command="\\COPY ${DATABASE_SCHEMA_NAME}.event_bene_coverage_search_status_change FROM '${DATABASE_SCHEMA_NAME}.event_bene_coverage_search_status_change.csv' WITH (FORMAT CSV);"

for csv_file in "${DATABASE_SCHEMA_NAME}".coverage_part*.csv; do
  psql \
    --dbname="${DATABASE_NAME}" \
    --host="${DATABASE_HOST}" \
    --username="${DATABASE_USER}" \
    --command="\\COPY ${DATABASE_SCHEMA_NAME}.coverage FROM '${csv_file}' WITH (FORMAT CSV);"
done
