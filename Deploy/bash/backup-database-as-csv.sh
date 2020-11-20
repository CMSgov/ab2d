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
    || [ -z "${SOURCE_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${SOURCE_CMS_ENV_PARAM}" ]; then
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

SOURCE_AWS_ACCOUNT_NUMBER="${SOURCE_AWS_ACCOUNT_NUMBER_PARAM}"

export SOURCE_CMS_ENV="${SOURCE_CMS_ENV_PARAM}"

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

#
# Define functions
#

# Import the "get temporary AWS credentials via AWS STS assume role" function

# shellcheck disable=SC1090
source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

#
# Set AWS target environment
#

fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${SOURCE_AWS_ACCOUNT_NUMBER}" "${SOURCE_CMS_ENV}"

#
# Get secrets
#

# Change to "python3" directory

cd "${START_DIR}/.."
cd python3

# Get database host secret

DATABASE_HOST=$(./get-database-secret.py "${SOURCE_CMS_ENV}" database_host "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_HOST}" ]; then
  echo "***************************************************"
  echo "ERROR: database host secret not found."
  echo "***************************************************"
  exit 1
fi

# Get database name secret

DATABASE_NAME=$(./get-database-secret.py "${SOURCE_CMS_ENV}" database_name "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_NAME}" ]; then
  echo "***************************************"
  echo "ERROR: database name secret not found."
  echo "**************************************"
  exit 1
fi

# Get database password secret

DATABASE_PASSWORD=$(./get-database-secret.py "${SOURCE_CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_PASSWORD}" ]; then
  echo "******************************************"
  echo "ERROR: database password secret not found."
  echo "******************************************"
  exit 1
fi

# Get database port secret

DATABASE_PORT=$(./get-database-secret.py "${SOURCE_CMS_ENV}" database_port "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_PORT}" ]; then
  echo "***************************************************"
  echo "ERROR: database port secret not found."
  echo "***************************************************"
  exit 1
fi

# Get database schema name secret

DATABASE_SCHEMA_NAME=$(./get-database-secret.py "${SOURCE_CMS_ENV}" database_schema_name "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_SCHEMA_NAME}" ]; then
  echo "***************************************"
  echo "ERROR: database name secret not found."
  echo "**************************************"
  exit 1
fi

# Get database user secret

DATABASE_USER=$(./get-database-secret.py "${SOURCE_CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")

if [ -z "${DATABASE_USER}" ]; then
  echo "**************************************"
  echo "ERROR: database user secret not found."
  echo "**************************************"
  exit 1
fi

#
# Backup database tables as csv files
#

# Create a database_backup directory for the target environment
  
rm -rf "${HOME}/database_backup/${SOURCE_CMS_ENV}"
mkdir -p "${HOME}/database_backup/${SOURCE_CMS_ENV}/csv"

# Set PostgreSQL password

export PGPASSWORD="${DATABASE_PASSWORD}"

# Create a schema script

pg_dump \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --username="${DATABASE_USER}" \
  --schema="${DATABASE_SCHEMA_NAME}" \
  --schema-only \
  --no-owner \
  --no-acl \
  --format=p \
  --encoding='UTF8' \
  --file="${HOME}/database_backup/${SOURCE_CMS_ENV}/01-schema.sql"

sed -i '/^CREATE SCHEMA.*$/d' "${HOME}/database_backup/${SOURCE_CMS_ENV}/01-schema.sql"

# Change to the ruby directory

cd "${START_DIR}/.."
cd ruby

#
# Get data as CSV files
#

# Add the pgsql-10 binary directory to the path (required to install the pg gem)

export PATH=$PATH:/usr/pgsql-10/bin

# Install required gems

bundle install

# Get the schema script and its data as csv files

bundle exec rake database_get_data

# Compress the database backup

cd "${HOME}/database_backup"
rm -f "${SOURCE_CMS_ENV}.tar.gz"
tar -czvf "${SOURCE_CMS_ENV}.tar.gz" "/var/lib/jenkins/database_backup/${SOURCE_CMS_ENV}"
rm -f "${SOURCE_CMS_ENV}.tar.gz"

