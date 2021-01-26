#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

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
if [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${BFD_KEYSTORE_FILE_NAME_PARAM}" ] \
    || [ -z "${CLAIMS_SKIP_BILLABLE_PERIOD_CHECK_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_ECR_REPO_ENV_PARAM}" ] \
    || [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${CPM_BACKUP_WORKER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_WORKER_PARAM}" ] \
    || [ -z "${IAM_INSTANCE_PROFILE_PARAM}" ] \
    || [ -z "${IAM_INSTANCE_ROLE_PARAM}" ] \
    || [ -z "${OVERRIDE_TASK_DEFINITION_ARN_PARAM}" ] \
    || [ -z "${PERCENT_CAPACITY_INCREASE_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${WORKER_DESIRED_INSTANCES_PARAM}" ] \
    || [ -z "${WORKER_MIN_INSTANCES_PARAM}" ] \
    || [ -z "${WORKER_MAX_INSTANCES_PARAM}" ] \
    ; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

BFD_KEYSTORE_FILE_NAME="${BFD_KEYSTORE_FILE_NAME_PARAM}"

CLAIMS_SKIP_BILLABLE_PERIOD_CHECK="${CLAIMS_SKIP_BILLABLE_PERIOD_CHECK_PARAM}"

CMS_ECR_REPO_ENV="${CMS_ECR_REPO_ENV_PARAM}"

CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER="${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

CMS_ENV="${CMS_ENV_PARAM}"

CPM_BACKUP_WORKER="${CPM_BACKUP_WORKER_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

EC2_INSTANCE_TYPE_WORKER="${EC2_INSTANCE_TYPE_WORKER_PARAM}"

IAM_INSTANCE_PROFILE="${IAM_INSTANCE_PROFILE_PARAM}"

IAM_INSTANCE_ROLE="${IAM_INSTANCE_ROLE_PARAM}"

OVERRIDE_TASK_DEFINITION_ARN="${OVERRIDE_TASK_DEFINITION_ARN_PARAM}"

MODULE="worker"

PERCENT_CAPACITY_INCREASE="${PERCENT_CAPACITY_INCREASE_PARAM}"

REGION="${REGION_PARAM}"

SSH_KEY_NAME="${CMS_ENV_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

WORKER_DESIRED_INSTANCES="${WORKER_DESIRED_INSTANCES_PARAM}"

WORKER_MIN_INSTANCES="${WORKER_MIN_INSTANCES_PARAM}"

WORKER_MAX_INSTANCES="${WORKER_MAX_INSTANCES_PARAM}"

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
  exit 1
else
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"
fi

# Set DB_SNAPSHOT_ID to blank, if "N/A"

if [ "${OVERRIDE_TASK_DEFINITION_ARN}" == "N/A" ]; then
  OVERRIDE_TASK_DEFINITION_ARN=""
fi

#
# Define functions
#

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

# Import the "get temporary AWS credentials via AWS STS assume role" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

# Get worker task count

worker_task_count() {
  aws --region "${AWS_DEFAULT_REGION}" ecs list-tasks --cluster "${CMS_ENV}-worker" \
    | grep -c "\:task\/" \
    | tr -d ' '
}

#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
fi

# If S3_TFSTATE_BUCKET is not set by a previous module, set it via the aws cli

if [ -z "${S3_TFSTATE_BUCKET}" ]; then
  S3_TFSTATE_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
    --query "Buckets[?Name == '${CMS_ENV}-tfstate'].Name" \
    --output text)
fi

# If TFSTATE_KMS_KEY_ID is not set by a previous module, set it via the aws cli

if [ -z "${TFSTATE_KMS_KEY_ID}" ]; then
  TFSTATE_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
    --query="Aliases[?AliasName=='alias/${CMS_ENV}-tfstate-kms'].TargetKeyId" \
    --output text)
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

# Create or get database schema name secret

AB2D_DB_SCHEMA=$(./get-database-secret.py "${CMS_ENV}" database_schema_name "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_DB_SCHEMA}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" database_schema_name "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  AB2D_DB_SCHEMA=$(./get-database-secret.py "${CMS_ENV}" database_schema_name "${DATABASE_SECRET_DATETIME}")
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

AB2D_SLACK_ALERT_WEBHOOKS=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_slack_alert_webhooks "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_SLACK_ALERT_WEBHOOKS}" ]; then
  echo "**************************************************"
  echo "ERROR: AB2D SLACK ALERT WEBHOOKS secret not found."
  echo "**************************************************"
  exit 1
fi

AB2D_SLACK_TRACE_WEBHOOKS=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_slack_trace_webhooks "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_SLACK_TRACE_WEBHOOKS}" ]; then
  echo "**************************************************"
  echo "ERROR: AB2D SLACK TRACE WEBHOOKS secret not found."
  echo "**************************************************"
  exit 1
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

# Create or get private ip address CIDR range for VPN

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py "${CMS_ENV}" vpn_private_ip_address_cidr_range "${DATABASE_SECRET_DATETIME}")
if [ -z "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py "${CMS_ENV}" vpn_private_ip_address_cidr_range "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
  echo "*********************************************************"
  VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py "${CMS_ENV}" vpn_private_ip_address_cidr_range "${DATABASE_SECRET_DATETIME}")
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
    || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
  echo "ERROR: Cannot get secrets because KMS key is disabled!"
  exit 1
fi

#
# Get AMI ID and gold image name
#

AMI_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=${CMS_ENV}-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${AMI_ID}" ]; then
  echo "ERROR: AMI id not found..."
  exit 1
else

  # Get gold image name

  GOLD_IMAGE_NAME=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
    --owners self \
    --filters "Name=tag:Name,Values=${CMS_ENV}-ami" \
    --query "Images[*].Tags[?Key=='gold_disk_name'].Value" \
    --output text)

fi

#
# Build worker and push to ECR repo
#

# Set AWS management environment

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ECR_REPO_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ECR_REPO_ENV}"
fi

# Set environment to the AWS account where the shared ECR repository is maintained

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ECR_REPO_ENV}"

# Get image version

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
COMMIT_NUMBER=$(git rev-parse "${CURRENT_BRANCH}" | cut -c1-7)
IMAGE_VERSION="${CMS_ENV}-latest-${COMMIT_NUMBER}"

# Build and push API and worker to ECR

echo "Build and push API and worker to ECR..."

# Log on to ECR

aws --region "${AWS_DEFAULT_REGION}" ecr get-login-password \
  | docker login --username AWS --password-stdin "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}.dkr.ecr.us-east-1.amazonaws.com"

# Build API and worker (if creating a new image)

cd "${START_DIR}/.."
cd ..

mvn clean package -DskipTests
sleep 5

# Build worker docker image

cd "${START_DIR}/.."
cd ../worker
docker build \
  --no-cache \
  --tag "ab2d_worker:${CMS_ENV}-latest" .

# Get or create worker repo

WORKER_ECR_REPO_URI=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-repositories \
  --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
  --output text)
if [ -z "${WORKER_ECR_REPO_URI}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" ecr create-repository \
    --repository-name "ab2d_worker"
  WORKER_ECR_REPO_URI=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
    --output text)
fi

# Apply ecr repo policy to the "ab2d_worker" repo

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ECR_REPO_ENV}"
aws --region "${AWS_DEFAULT_REGION}" ecr set-repository-policy \
  --repository-name ab2d_worker \
  --policy-text file://ab2d-ecr-policy.json

# Tag and push latest version of worker docker image to ECR

docker tag "ab2d_worker:${CMS_ENV}-latest" "${WORKER_ECR_REPO_URI}:${CMS_ENV}-latest"
docker push "${WORKER_ECR_REPO_URI}:${CMS_ENV}-latest"

#
# Determine cpu and memory for new worker ECS container definition
#

WORKER_EC2_INSTANCE_CPU_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_WORKER}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
  --output text)

WORKER_CPU=$((WORKER_EC2_INSTANCE_CPU_COUNT*1024))

WORKER_EC2_INSTANCE_MEMORY=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_WORKER}" --query "InstanceTypes[*].MemoryInfo.SizeInMiB" \
  --output text)

WORKER_MEMORY=$((WORKER_EC2_INSTANCE_MEMORY*9/10))

#
# Create or refresh data components for target environment
#

# Set AWS target environment again

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
fi

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/${MODULE}"

# Get worker cluster ARN (if exists)

WORKER_CLUSTER_ARN=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-clusters \
  --query 'clusterArns' \
  --output text \
  | xargs \
  | tr -d '\r' \
  | tr " " "\n" \
  | grep "${CMS_ENV}-worker")

# Determine expected worker count when both old and new instances are present

if [ -n "${WORKER_CLUSTER_ARN}" ]; then
  OLD_WORKER_TASK_COUNT=$(worker_task_count)
else
  OLD_WORKER_TASK_COUNT=0
fi

EXPECTED_WORKER_COUNT=$((OLD_WORKER_TASK_COUNT+WORKER_DESIRED_INSTANCES))

# Ensure old worker autoscaling group and containers are around to service requests

if [ -z "${WORKER_CLUSTER_ARN}" ]; then
  echo "Skipping removing autoscaling group and launch configuration, since there are no existing clusters"
else
  terraform state rm module.worker.aws_autoscaling_group.asg
  terraform state rm module.worker.aws_launch_configuration.launch_config
fi

if [ -z "${WORKER_CLUSTER_ARN}" ]; then
  echo "Skipping removing autoscaling group and launch configuration, since there are no existing clusters"
else
  OLD_WORKER_CONTAINER_INSTANCES=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-container-instances \
    --cluster "${CMS_ENV}-worker" \
    --output text)

  if [ -n "${OLD_WORKER_CONTAINER_INSTANCES}" ]; then
    OLD_WORKER_CONTAINER_INSTANCES=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-container-instances \
      --cluster "${CMS_ENV}-worker" \
      | grep container-instance)
  fi
fi

# Deploy worker module

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ENV}/${MODULE}"

terraform apply \
  --var "ami_id=${AMI_ID}" \
  --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
  --var "bfd_keystore_file_name=${BFD_KEYSTORE_FILE_NAME}" \
  --var "bfd_keystore_location=${AB2D_BFD_KEYSTORE_LOCATION}" \
  --var "bfd_keystore_password=${AB2D_BFD_KEYSTORE_PASSWORD}" \
  --var "bfd_url=${AB2D_BFD_URL}" \
  --var "claims_skip_billable_period_check=${CLAIMS_SKIP_BILLABLE_PERIOD_CHECK}" \
  --var "cpm_backup_worker=${CPM_BACKUP_WORKER}" \
  --var "db_host=${AB2D_DB_HOST}" \
  --var "db_name=${AB2D_DB_DATABASE}" \
  --var "db_password=${AB2D_DB_PASSWORD}" \
  --var "db_port=${AB2D_DB_PORT}" \
  --var "db_username=${AB2D_DB_USER}" \
  --var "ec2_instance_type_worker=${EC2_INSTANCE_TYPE_WORKER}" \
  --var "ecr_repo_aws_account=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "ecs_container_def_memory=${WORKER_MEMORY}" \
  --var "ecs_task_def_cpu=${WORKER_CPU}" \
  --var "ecs_task_def_memory=${WORKER_MEMORY}" \
  --var "env=${CMS_ENV}" \
  --var "gold_image_name=${GOLD_IMAGE_NAME}" \
  --var "hicn_hash_iter=${AB2D_HICN_HASH_ITER}" \
  --var "hicn_hash_pepper=${AB2D_HICN_HASH_PEPPER}" \
  --var "iam_instance_profile=${IAM_INSTANCE_PROFILE}" \
  --var "iam_instance_role=${IAM_INSTANCE_ROLE}" \
  --var "image_version=${IMAGE_VERSION}" \
  --var "new_relic_app_name=${NEW_RELIC_APP_NAME}" \
  --var "new_relic_license_key=${NEW_RELIC_LICENSE_KEY}" \
  --var "override_task_definition_arn=${OVERRIDE_TASK_DEFINITION_ARN}" \
  --var "percent_capacity_increase=${PERCENT_CAPACITY_INCREASE}" \
  --var "region=${REGION}" \
  --var "slack_alert_webhooks=${AB2D_SLACK_ALERT_WEBHOOKS}" \
  --var "slack_trace_webhooks=${AB2D_SLACK_TRACE_WEBHOOKS}" \
  --var "ssh_key_name=${SSH_KEY_NAME}" \
  --var "ssh_username=${SSH_USERNAME}" \
  --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
  --var "worker_desired_instances=${WORKER_DESIRED_INSTANCES}" \
  --var "worker_min_instances=${WORKER_MIN_INSTANCES}" \
  --var "worker_max_instances=${WORKER_MAX_INSTANCES}" \
  --auto-approve

# Ensure new worker autoscaling group is running containers

ACTUAL_WORKER_COUNT=0
RETRIES_WORKER=0

while [ "$ACTUAL_WORKER_COUNT" -lt "$EXPECTED_WORKER_COUNT" ]; do
  ACTUAL_WORKER_COUNT=$(worker_task_count)
  echo "Running WORKER Tasks: $ACTUAL_WORKER_COUNT, Expected: $EXPECTED_WORKER_COUNT"
  if [ "$RETRIES_WORKER" != "30" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    RETRIES_WORKER=$((RETRIES_WORKER + 1))
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

# Drain old container instances

if [ -z "${WORKER_CLUSTER_ARN}" ]; then
  echo "Skipping draining old container instances, since there are no existing clusters"
else
  if [ -n "${OLD_WORKER_CONTAINER_INSTANCES}" ]; then
    OLD_WORKER_INSTANCE_LIST=$(echo "${OLD_WORKER_CONTAINER_INSTANCES}" \
      | tr -d ' ' \
      | tr "\n" " " \
      | tr -d "," \
      | tr '""' ' ' \
      | tr -d '"')

    # shellcheck disable=SC2086
    # $OLD_WORKER_INSTANCE_LIST format is required here
    aws --region "${AWS_DEFAULT_REGION}" ecs update-container-instances-state \
      --cluster "${CMS_ENV}-worker" \
      --status DRAINING \
      --container-instances $OLD_WORKER_INSTANCE_LIST \
      1> /dev/null

    echo "Allowing all instances to drain for 60 seconds before proceeding..."
    sleep 60
  fi
fi

# Remove any duplicative worker autoscaling groups

WORKER_ASG_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
  --query "AutoScalingGroups[*].Tags[?Value == '${CMS_ENV}-worker'].ResourceId" \
  --output text \
  | sort \
  | wc -l \
  | tr -d ' ')

if [ "${WORKER_ASG_COUNT}" -gt 1 ]; then
  DUPLICATIVE_WORKER_ASG_COUNT=$((WORKER_ASG_COUNT - 1))
  for ((i=1;i<=DUPLICATIVE_WORKER_ASG_COUNT;i++)); do
    DUPLICATIVE_WORKER_ASG=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
      --query "AutoScalingGroups[*].Tags[?Value == '${CMS_ENV}-worker'].ResourceId" \
      --output text \
      | sort \
      | head -n "${i}" \
      | tail -n 1)
    aws --region "${AWS_DEFAULT_REGION}" autoscaling delete-auto-scaling-group \
      --auto-scaling-group-name "${DUPLICATIVE_WORKER_ASG}" \
      --force-delete || true
  done
fi

# Wait for old Autoscaling groups to terminate

sleep 60
RETRIES_ASG=0
ASG_NOT_IN_SERVICE=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
  --query "AutoScalingGroups[*].Instances[?LifecycleState != 'InService'].LifecycleState" \
  --output text)
while [ -n "${ASG_NOT_IN_SERVICE}" ]; do
  echo "Waiting for old autoscaling groups to terminate..."
  if [ "$RETRIES_ASG" != "30" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    ASG_NOT_IN_SERVICE=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
      --query "AutoScalingGroups[*].Instances[?LifecycleState != 'InService'].LifecycleState" \
      --output text)
    RETRIES_ASG=$((RETRIES_ASG + 1))
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

sleep 60

# Remove old launch configurations

if [ -z "${WORKER_CLUSTER_ARN}" ]; then
  echo "Skipping removing old launch configurations, since there are no existing clusters"
else

  LAUNCH_CONFIGURATION_EXPECTED_COUNT=1

  # TO DO: migrate from "*-test-*" naming to "*-validation-*" naming
  # Count only "*-test-*" and "*-validation-*" launch configurations
  LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-launch-configurations \
    --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
    --output text \
    | sort -k2 \
    | grep -c "\-test\-\|\-validation\-")

  while [ "$LAUNCH_CONFIGURATION_ACTUAL_COUNT" -gt "$LAUNCH_CONFIGURATION_EXPECTED_COUNT" ]; do

    # TO DO: migrate from "*-test-*" naming to "*-validation-*" naming
    # Note that only "*-test-*" and "*-validation-*" launch configurations will be included
    OLD_LAUNCH_CONFIGURATION=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      --output text \
      | sort -k2 \
      | grep "\-test\-\|\-validation\-" \
      | head -n1 \
      | awk '{print $1}')

    aws --region "${AWS_DEFAULT_REGION}" autoscaling delete-launch-configuration \
      --launch-configuration-name "${OLD_LAUNCH_CONFIGURATION}"
    sleep 5

    # TO DO: migrate from "*-test-*" naming to "*-validation-*" naming
    # Note that only "*-test-*" and "*-validation-*" launch configurations will be counted
    LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      --output text \
      | sort -k2 \
      | grep -c "\-test\-\|\-validation\-")

  done

fi
