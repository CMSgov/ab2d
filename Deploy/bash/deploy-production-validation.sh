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
if  [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${BFD_KEYSTORE_FILE_NAME_PARAM}" ] \
    || [ -z "${CLAIMS_SKIP_BILLABLE_PERIOD_CHECK_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CMS_ECR_REPO_ENV_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${CPM_BACKUP_DB_PARAM}" ] \
    || [ -z "${CPM_BACKUP_WORKER_PARAM}" ] \
    || [ -z "${DB_ALLOCATED_STORAGE_SIZE_PARAM}" ] \
    || [ -z "${DB_BACKUP_RETENTION_PERIOD_PARAM}" ] \
    || [ -z "${DB_BACKUP_WINDOW_PARAM}" ] \
    || [ -z "${DB_COPY_TAGS_TO_SNAPSHOT_PARAM}" ] \
    || [ -z "${DB_INSTANCE_CLASS_PARAM}" ] \
    || [ -z "${DB_IOPS_PARAM}" ] \
    || [ -z "${DB_MAINTENANCE_WINDOW_PARAM}" ] \
    || [ -z "${DB_MULTI_AZ_PARAM}" ] \
    || [ -z "${DB_NAME_PARAM}" ] \
    || [ -z "${DB_SNAPSHOT_ID_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_WORKER_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${ENV_PASCAL_CASE_PARAM}" ] \
    || [ -z "${IAM_INSTANCE_PROFILE_PARAM}" ] \
    || [ -z "${IAM_INSTANCE_ROLE_PARAM}" ] \
    || [ -z "${JENKINS_AGENT_SEC_GROUP_ID_PARAM}" ] \
    || [ -z "${OVERRIDE_TASK_DEFINITION_ARN_PARAM}" ] \
    || [ -z "${PARENT_ENV_PARAM}" ] \
    || [ -z "${PERCENT_CAPACITY_INCREASE_PARAM}" ] \
    || [ -z "${POSTGRES_ENGINE_VERSION_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${WORKER_DESIRED_INSTANCES_PARAM}" ] \
    || [ -z "${WORKER_MIN_INSTANCES_PARAM}" ] \
    || [ -z "${WORKER_MAX_INSTANCES_PARAM}" ]; then
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

# Deploy or update worker module

cd "${START_DIR}"
source ./deploy-worker-module.sh
