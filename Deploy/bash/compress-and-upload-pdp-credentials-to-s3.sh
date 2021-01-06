#!/bin/bash

set -e # Turn on exit on error
# set -x # Turn on verbosity

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
    || [ -z "${CREDENTIALS_FILE_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

export AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

CMS_ENV="${CMS_ENV_PARAM}"

export CREDENTIALS_FILE="${CREDENTIALS_FILE_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

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

# shellcheck disable=SC1090
source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

# Import the "get temporary AWS credentials via AWS STS assume role" function

# shellcheck disable=SC1090
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
# Create public S3 bucket for customer
#

FILE_NAME_WITHOUT_EXT=$(basename "${CREDENTIALS_FILE}" .txt)
DIR_NAME=$(dirname "${CREDENTIALS_FILE}")
UUID=$(uuidgen)

CUSTOMER_BUCKET_NAME=$(echo "ab2d-pdp-temp-${FILE_NAME_WITHOUT_EXT}-${UUID}" \
  | tr '[:upper:]' '[:lower:]' | cut -c 1-63)

CUSTOMER_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name == '${CUSTOMER_BUCKET_NAME}'].Name" \
  --output text)

if [ -z "${CUSTOMER_BUCKET_EXISTS}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${CUSTOMER_BUCKET_NAME}"
else
  echo "NOTE: The ${CUSTOMER_BUCKET_NAME} bucket already exists."
fi

#
# Compress file
#

rm -f "${DIR_NAME}/${FILE_NAME_WITHOUT_EXT}.zip"
7z a -tzip -mem=AES256 -p "${DIR_NAME}/${FILE_NAME_WITHOUT_EXT}.zip" "${CREDENTIALS_FILE}"

#
# Upload compressed file with public access
#

aws --region "${AWS_DEFAULT_REGION}" s3api put-object \
  --bucket "${CUSTOMER_BUCKET_NAME}" \
  --body "${DIR_NAME}/${FILE_NAME_WITHOUT_EXT}.zip" \
  --key "${FILE_NAME_WITHOUT_EXT}.zip" \
  --acl public-read

#
# Output customer credentials link
#

echo ""
echo "-----------------------------------------------------------------------------"
echo "Customer Credentials S3 Link"
echo "-----------------------------------------------------------------------------"
echo "https://${CUSTOMER_BUCKET_NAME}.s3.amazonaws.com/${FILE_NAME_WITHOUT_EXT}.zip"
echo ""
