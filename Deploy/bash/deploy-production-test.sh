#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

# Check vars are not empty before proceeding

echo "Check vars are not empty before proceeding..."
if  [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CMS_ECR_REPO_ENV_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${DB_ALLOCATED_STORAGE_SIZE}" ] \
    || [ -z "${DB_BACKUP_RETENTION_PERIOD}" ] \
    || [ -z "${DB_BACKUP_WINDOW}" ] \
    || [ -z "${DB_COPY_TAGS_TO_SNAPSHOT}" ] \
    || [ -z "${DB_INSTANCE_CLASS}" ] \
    || [ -z "${DB_IOPS}" ] \
    || [ -z "${DB_MAINTENANCE_WINDOW}" ] \
    || [ -z "${DB_MULTI_AZ}" ] \
    || [ -z "${DB_PASSWORD}" ] \
    || [ -z "${DB_SNAPSHOT_ID}" ] \
    || [ -z "${DB_USERNAME}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_WORKER_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${ENV_PASCAL_CASE_PARAM}" ] \
    || [ -z "${JENKINS_AGENT_SEC_GROUP_ID}" ] \
    || [ -z "${PARENT_ENV_PARAM}" ] \
    || [ -z "${POSTGRES_ENGINE_VERSION}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

# Deploy or update S3 terraform backend

cd "${START_DIR}"
source ./deploy-s3-terraform-backend.sh

# Deploy or update core module

cd "${START_DIR}"
source ./deploy-core-module.sh

# Deploy or update data module

cd "${START_DIR}"
source ./deploy-data-module.sh
