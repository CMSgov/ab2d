#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Set variables
#

export START_DIR=""

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"

# Check vars are not empty before proceeding

echo "Check vars are not empty before proceeding..."
if [ -z "${API_URL_PREFIX_PARAM}" ] \
    ||[ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CONTRACT_NUMBER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${TARGET_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${TARGET_CMS_ENV_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

API_URL_PREFIX="${API_URL_PREFIX_PARAM}"

CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

export CONTRACT_NUMBER="${CONTRACT_NUMBER_PARAM}"

export DATABASE_HOST=""

export DATABASE_NAME=""

export DATABASE_PASSWORD=""

export DATABASE_PORT=""

export DATABASE_SCHEMA_NAME=""

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DATABASE_USER=""

TARGET_CMS_ENV="${TARGET_CMS_ENV_PARAM}"

TARGET_AWS_ACCOUNT_NUMBER="${TARGET_AWS_ACCOUNT_NUMBER_PARAM}"

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
# Create temporary schema in target environment
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

# Create get user account id command string

COMMAND_01="SELECT e.id FROM ${DATABASE_SCHEMA_NAME}.contract d "
COMMAND_02="INNER JOIN ${DATABASE_SCHEMA_NAME}.user_account e ON d.id = e.contract_id "
COMMAND_03="WHERE e.enabled = true AND d.contract_number = '${CONTRACT_NUMBER}';"

# Get user account id

USER_ACCOUNT_ID=$(
  psql \
    -t \
    --host="${DATABASE_HOST}" \
    --port="${DATABASE_PORT}" \
    --username="${DATABASE_USER}" \
    --dbname="${DATABASE_NAME}" \
    --command="${COMMAND_01}${COMMAND_02}${COMMAND_03}" \
    | head -n 1 \
    | xargs \
    | tr -d '\r')

# Create get contract id command string

COMMAND_01="SELECT a.contract_id FROM ${DATABASE_SCHEMA_NAME}.user_account a "
COMMAND_02="WHERE a.id = ${USER_ACCOUNT_ID};"

# Get contract id

CONTRACT_ID=$(
  psql \
    -t \
    --host="${DATABASE_HOST}" \
    --port="${DATABASE_PORT}" \
    --username="${DATABASE_USER}" \
    --dbname="${DATABASE_NAME}" \
    --command="${COMMAND_01}${COMMAND_02}" \
    | head -n 1 \
    | xargs \
    | tr -d '\r')

# Create insert job command string

JOB_ID=$(uuidgen)
COMMAND_01="INSERT INTO ${DATABASE_SCHEMA_NAME}.job("
COMMAND_02="id, job_uuid, user_account_id, created_at, expires_at, resource_types, status, status_message, "
COMMAND_03="request_url, progress, last_poll_time, completed_at, contract_id, output_format, since) "
COMMAND_04="VALUES ((select nextval('hibernate_sequence')), '${JOB_ID}', ${USER_ACCOUNT_ID}, (select now()), "
COMMAND_05="(select now() + INTERVAL '1 day'), 'ExplanationOfBenefit', 'SUBMITTED', '0%', "
COMMAND_06="'${API_URL_PREFIX}/v1/fhir/Patient/\$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit', "
COMMAND_07="0, null, null, ${CONTRACT_ID}, null, null);"

# Insert job record

psql \
  --dbname="${DATABASE_NAME}" \
  --host="${DATABASE_HOST}" \
  --port="${DATABASE_PORT}" \
  --username="${DATABASE_USER}" \
  --command="${COMMAND_01}${COMMAND_02}${COMMAND_03}${COMMAND_04}${COMMAND_05}${COMMAND_06}${COMMAND_07}"
