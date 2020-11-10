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
if  [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${CMS_ECR_REPO_ENV_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${VPC_ID_PARAM}" ] \
    || [ -z "${JENKINS_AGENT_SEC_GROUP_ID}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_WORKER_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

# Deploy or update S3 automation bucket

cd "${START_DIR}"
source ./deploy-test-s3-automation-bucket-module.sh

# Deploy or update test Terraform initialization

# cd "${START_DIR}"
# source ./deploy-test-terraform-initialization.sh

# Deploy or update test IAM module

# cd "${START_DIR}"
# source ./deploy-test-iam-module.sh
