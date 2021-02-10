#!/bin/bash

set -e # Turn on exit on error
set +x # <-- Do not change this value!
       # Logging is turned on in a later step based on CLOUD_TAMER_PARAM.
       # CLOUD_TAMER_PARAM = false (Jenkins assumed; verbose logging turned off)
       # CLOUD_TAMER_PARAM = true (Dev machine assumed; verbose logging turned on)
       # NOTE: Setting the CLOUD_TAMER_PARAM to a value that does not match the
       #       assumed host machine will cause the script to fail.

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
    || [ -z "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_ECR_REPO_ENV_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_API_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_API_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_WORKER_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_API_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${INTERNET_FACING_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${TARGET_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${TARGET_CMS_ENV_PARAM}" ] \
    || [ -z "${VPC_ID_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set conditional variables
#

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
  exit 1
elif [ "${CLOUD_TAMER_PARAM}" == "false" ]; then

  # Turn off verbose logging for Jenkins jobs
  set +x
  echo "Don't print commands and their arguments as they are executed."
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

  # Import the "get temporary AWS credentials via AWS STS assume role" function
  source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

else # [ "${CLOUD_TAMER_PARAM}" == "true" ]

  # Turn on verbose logging for development machine testing
  set -x
  echo "Print commands and their arguments as they are executed."
  CLOUD_TAMER="${CLOUD_TAMER_PARAM}"

  # Import the "get temporary AWS credentials via CloudTamer API" function
  source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

fi

# Set whether load balancer is internal based on "internet-facing" parameter

if [ "$INTERNET_FACING_PARAM" == "false" ]; then
  ALB_INTERNAL=true
elif [ "$INTERNET_FACING_PARAM" == "true" ]; then
  ALB_INTERNAL=false
else
  echo "ERROR: the '--internet-facing' parameter must be true or false"
  exit 1
fi

#
# Set remaining variables
#

CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER="${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

CMS_ECR_REPO_ENV="${CMS_ECR_REPO_ENV_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

EC2_DESIRED_INSTANCE_COUNT_API="${EC2_DESIRED_INSTANCE_COUNT_API_PARAM}"

EC2_DESIRED_INSTANCE_COUNT_WORKER="${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}"

EC2_INSTANCE_TYPE_API="${EC2_INSTANCE_TYPE_API_PARAM}"

EC2_INSTANCE_TYPE_WORKER="${EC2_INSTANCE_TYPE_WORKER_PARAM}"

EC2_MAXIMUM_INSTANCE_COUNT_API="${EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM}"

EC2_MAXIMUM_INSTANCE_COUNT_WORKER="${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}"

EC2_MINIMUM_INSTANCE_COUNT_API="${EC2_MINIMUM_INSTANCE_COUNT_API_PARAM}"

EC2_MINIMUM_INSTANCE_COUNT_WORKER="${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

TARGET_AWS_ACCOUNT_NUMBER="${TARGET_AWS_ACCOUNT_NUMBER_PARAM}"

TARGET_CMS_ENV="${TARGET_CMS_ENV_PARAM}"

VPC_ID="${VPC_ID_PARAM}"

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
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${TARGET_AWS_ACCOUNT_NUMBER}" "${TARGET_CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${TARGET_AWS_ACCOUNT_NUMBER}" "${TARGET_CMS_ENV}"
fi

#
# Verify that VPC ID exists
#

VPC_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpcs \
  --query "Vpcs[?VpcId=='$VPC_ID'].VpcId" \
  --output text)

if [ -z "${VPC_EXISTS}" ]; then
  echo "*********************************************************"
  echo "ERROR: VPC ID does not exist for the target AWS profile."
  echo "*********************************************************"
  exit 1
fi

#
# Get KMS key id for target environment (if exists)
#

KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${KMS_KEY_ID}" ]; then
  KMS_KEY_STATE=$(aws --region "${AWS_DEFAULT_REGION}" kms describe-key \
    --key-id alias/ab2d-kms \
    --query "KeyMetadata.KeyState" \
    --output text)
  if [ "${KMS_KEY_STATE}" != "Enabled" ]; then
    echo "*****************************************************"
    echo "ERROR: kms key for target environment is not enabled."
    echo "*****************************************************"
    exit 1
  fi
else
  echo "***************************************************"
  echo "ERROR: kms key id for target environment not found."
  echo "***************************************************"
  exit 1
fi

#
# Configure terraform
#

# Reset logging

if [ "${CLOUD_TAMER}" == "true" ]; then

  # Enable terraform logging on development machine
  echo "Setting terraform debug level to $DEBUG_LEVEL..."
  export TF_LOG=$DEBUG_LEVEL
  export TF_LOG_PATH=/var/log/terraform/tf.log
  rm -f /var/log/terraform/tf.log

else

  # Disable terraform logging on Jenkins
  export TF_LOG=

fi

#
# Configure docker environment
#

# Delete all API images related to target environment (if any) - first pass

docker images | grep _api | grep "${TARGET_CMS_ENV}-latest" | awk '{print $3}' | xargs -I name docker rmi --force name

# Delete all API images related to target environment (if any) - second pass

docker images | grep _api | grep "${TARGET_CMS_ENV}-latest" | awk '{print $3}' | xargs -I name docker rmi --force name

# Delete all worker images related to target environment (if any) - first pass

docker images | grep _worker | grep "${TARGET_CMS_ENV}-latest" | awk '{print $3}' | xargs -I name docker rmi --force name

# Delete all worker images related to target environment (if any) - second pass

docker images | grep _worker | grep "${TARGET_CMS_ENV}-latest" | awk '{print $3}' | xargs -I name docker rmi --force name

#
# Initialize and validate terraform
#

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

rm -f ./*.tfvars

terraform init \
  -backend-config="bucket=${TARGET_CMS_ENV}-automation" \
  -backend-config="key=${TARGET_CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Get and enable KMS key id for target environment
#

echo "Enabling KMS key..."
cd "${START_DIR}/.."
cd python3
./enable-kms-key.py "${KMS_KEY_ID}"

#
# Get secrets
#

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

# Get bfd url secret

BFD_URL=$(./get-database-secret.py "${TARGET_CMS_ENV}" bfd_url "${DATABASE_SECRET_DATETIME}")

if [ -z "${BFD_URL}" ]; then
  echo "********************************"
  echo "ERROR: bfd url secret not found."
  echo "********************************"
  exit 1
fi

# Get bfd keystore location secret

BFD_KEYSTORE_LOCATION=$(./get-database-secret.py "${TARGET_CMS_ENV}" bfd_keystore_location "${DATABASE_SECRET_DATETIME}")

if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
  echo "**********************************************"
  echo "ERROR: bfd keystore location secret not found."
  echo "**********************************************"
  exit 1
fi

# Get bfd keystore password secret

BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py "${TARGET_CMS_ENV}" bfd_keystore_password "${DATABASE_SECRET_DATETIME}")

if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
  echo "**********************************************"
  echo "ERROR: bfd keystore password secret not found."
  echo "**********************************************"
  exit 1
fi

# Get hicn hash pepper secret

HICN_HASH_PEPPER=$(./get-database-secret.py "${TARGET_CMS_ENV}" hicn_hash_pepper "${DATABASE_SECRET_DATETIME}")

if [ -z "${HICN_HASH_PEPPER}" ]; then
  echo "*****************************************"
  echo "ERROR: hicn hash pepper secret not found."
  echo "*****************************************"
  exit 1
fi

# Get hicn hash iter secret

HICN_HASH_ITER=$(./get-database-secret.py "${TARGET_CMS_ENV}" hicn_hash_iter "${DATABASE_SECRET_DATETIME}")

if [ -z "${HICN_HASH_ITER}" ]; then
  echo "***************************************"
  echo "ERROR: hicn hash iter secret not found."
  echo "***************************************"
  exit 1
fi

# Get new relic app name secret

NEW_RELIC_APP_NAME=$(./get-database-secret.py "${TARGET_CMS_ENV}" new_relic_app_name "${DATABASE_SECRET_DATETIME}")

if [ -z "${NEW_RELIC_APP_NAME}" ]; then
  echo "*******************************************"
  echo "ERROR: new relic app name secret not found."
  echo "*******************************************"
  exit 1
fi

# Get new relic license key secret

NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py "${TARGET_CMS_ENV}" new_relic_license_key "${DATABASE_SECRET_DATETIME}")

if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
  echo "**********************************************"
  echo "ERROR: new relic license key secret not found."
  echo "**********************************************"
  exit 1
fi

# Get private ip address CIDR range for VPN

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py "${TARGET_CMS_ENV}" vpn_private_ip_address_cidr_range "${DATABASE_SECRET_DATETIME}")

if [ -z "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" ]; then
  echo "**********************************************************"
  echo "ERROR: VPN private IP address CIDR range secret not found."
  echo "**********************************************************"
  exit 1
fi

# Get AB2D keystore location

AB2D_KEYSTORE_LOCATION=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_keystore_location "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_KEYSTORE_LOCATION}" ]; then
  echo "***********************************************"
  echo "ERROR: AB2D keystore location secret not found."
  echo "***********************************************"
  exit 1
fi

# Get AB2D keystore password

AB2D_KEYSTORE_PASSWORD=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_keystore_password "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_KEYSTORE_PASSWORD}" ]; then
  echo "***********************************************"
  echo "ERROR: AB2D keystore password secret not found."
  echo "***********************************************"
  exit 1
fi

AB2D_OKTA_JWT_ISSUER=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_okta_jwt_issuer "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_OKTA_JWT_ISSUER}" ]; then
  echo "*********************************************"
  echo "ERROR: AB2D OKTA JWT issuer secret not found."
  echo "*********************************************"
  exit 1
fi

# Get HPMS URL

AB2D_HPMS_URL=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_hpms_url "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_HPMS_URL}" ]; then
  echo "**************************************"
  echo "ERROR: AB2D HPMS URL secret not found."
  echo "**************************************"
  exit 1
fi

# Get HPMS API PARAMS

AB2D_HPMS_API_PARAMS=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_hpms_api_params "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_HPMS_API_PARAMS}" ]; then
  echo "*******************************************"
  echo "ERROR: AB2D HPMS API PARAMS secret not found."
  echo "*******************************************"
  exit 1
fi

# Get HPMS AUTH key id

AB2D_HPMS_AUTH_KEY_ID=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_hpms_auth_key_id "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_HPMS_AUTH_KEY_ID}" ]; then
  echo "**********************************************"
  echo "ERROR: AB2D HPMS AUTH KEY ID secret not found."
  echo "**********************************************"
  exit 1
fi

# Get HPMS AUTH key secret

AB2D_HPMS_AUTH_KEY_SECRET=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_hpms_auth_key_secret "${DATABASE_SECRET_DATETIME}")

if [ -z "${AB2D_HPMS_AUTH_KEY_SECRET}" ]; then
  echo "**************************************************"
  echo "ERROR: AB2D HPMS AUTH KEY SECRET secret not found."
  echo "**************************************************"
  exit 1
fi

if [ "${CMS_ENV_SS}" == "ab2d-sbx-sandbox" ] \
    || [ "${CMS_ENV_SS}" == "ab2d-east-prod" ]; then

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

else
  AB2D_SLACK_ALERT_WEBHOOKS=""
  AB2D_SLACK_TRACE_WEBHOOKS=""
fi

# If any database secret produced an error, exit the script
DATABASE_SECRET_ERROR_MESSAGE="ERROR: Cannot get database secret because KMS key is disabled!"

if [ "${DATABASE_USER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${DATABASE_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${DATABASE_NAME}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${BFD_URL}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${BFD_KEYSTORE_LOCATION}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${BFD_KEYSTORE_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${HICN_HASH_PEPPER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${HICN_HASH_ITER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${NEW_RELIC_APP_NAME}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${NEW_RELIC_LICENSE_KEY}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_KEYSTORE_LOCATION}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_KEYSTORE_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_OKTA_JWT_ISSUER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_HPMS_URL}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_HPMS_API_PARAMS}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_HPMS_AUTH_KEY_ID}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_HPMS_AUTH_KEY_SECRET}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_SLACK_ALERT_WEBHOOKS}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
  || [ "${AB2D_SLACK_TRACE_WEBHOOKS}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Get network attributes
#

# Get first public subnet id

SUBNET_PUBLIC_1_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "ERROR: public subnet #1 not found..."
  exit 1
fi

# Get second public subnet id

SUBNET_PUBLIC_2_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then
  echo "ERROR: public subnet #2 not found..."
  exit 1
fi

# Get first private subnet id

SUBNET_PRIVATE_1_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then
  echo "ERROR: private subnet #1 not found..."
  exit 1
fi

# Get second private subnet id

SUBNET_PRIVATE_2_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then
  echo "ERROR: private subnet #2 not found..."
  exit 1
fi

#
# Get AMI ID and gold image name
#

AMI_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${AMI_ID}" ]; then
  echo "ERROR: AMI id not found..."
  exit 1
else

  # Get gold image name

  GOLD_IMAGE_NAME=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
    --owners self \
    --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}-ami" \
    --query "Images[*].Tags[?Key=='gold_disk_name'].Value" \
    --output text)

fi

#
# Get deployer IP address
#

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

#
# Create ".auto.tfvars" file for the target environment
#

# Change to the target environment

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

# Determine cpu and memory for new API ECS container definition

API_EC2_INSTANCE_CPU_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_API}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
  --output text)

API_CPU=$((API_EC2_INSTANCE_CPU_COUNT*1024))

API_EC2_INSTANCE_MEMORY=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_API}" --query "InstanceTypes[*].MemoryInfo.SizeInMiB" \
  --output text)

API_MEMORY=$((API_EC2_INSTANCE_MEMORY*9/10))

# Determine cpu and memory for new worker ECS container definition

WORKER_EC2_INSTANCE_CPU_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_WORKER}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
  --output text)

WORKER_CPU=$((WORKER_EC2_INSTANCE_CPU_COUNT*1024))

WORKER_EC2_INSTANCE_MEMORY=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_WORKER}" --query "InstanceTypes[*].MemoryInfo.SizeInMiB" \
  --output text)

WORKER_MEMORY=$((WORKER_EC2_INSTANCE_MEMORY*9/10))

# Create ".auto.tfvars" file for the target environment

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

{
  echo 'vpc_id = "'"${VPC_ID}"'"'
  echo 'private_subnet_ids = ["'"${SUBNET_PRIVATE_1_ID}"'","'"${SUBNET_PRIVATE_2_ID}"'"]'
  echo 'deployment_controller_subnet_ids = ["'"${SUBNET_PUBLIC_1_ID}"'","'"${SUBNET_PUBLIC_2_ID}"'"]'
  echo 'ec2_instance_type_api = "'"${EC2_INSTANCE_TYPE_API}"'"'
  echo 'ecs_container_definition_new_memory_api = "'"${API_MEMORY}"'"'
  echo 'ecs_task_definition_cpu_api = "'"${API_CPU}"'"'
  echo 'ecs_task_definition_memory_api = "'"${API_MEMORY}"'"'
  echo 'ec2_instance_type_worker = "'"${EC2_INSTANCE_TYPE_WORKER}"'"'
  echo 'ecs_container_definition_new_memory_worker = "'"${WORKER_MEMORY}"'"'
  echo 'ecs_task_definition_cpu_worker = "'"${WORKER_CPU}"'"'
  echo 'ecs_task_definition_memory_worker = "'"${WORKER_MEMORY}"'"'
  echo 'linux_user = "'"${SSH_USERNAME}"'"'
  echo 'deployer_ip_address = "'"${DEPLOYER_IP_ADDRESS}"'"'
} > "${TARGET_CMS_ENV}.auto.tfvars"

######################################
# Deploy target environment components
######################################

#
# Deploy AWS application modules
#

# Set AWS management environment

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" "${TARGET_CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ECR_REPO_ENV}"
fi

MGMT_KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${MGMT_KMS_KEY_ID}" ]; then
  MGMT_KMS_KEY_STATE=$(aws --region "${AWS_DEFAULT_REGION}" kms describe-key \
    --key-id alias/ab2d-kms \
    --query "KeyMetadata.KeyState" \
    --output text)
  if [ "${MGMT_KMS_KEY_STATE}" != "Enabled" ]; then
    echo "*********************************************************"
    echo "ERROR: kms key for management environment is not enabled."
    echo "*********************************************************"
    exit 1
  fi
else
  echo "*******************************************************"
  echo "ERROR: kms key id for management environment not found."
  echo "*******************************************************"
  exit 1
fi

# Get and enable KMS key id for management environment

echo "Enabling KMS key..."
cd "${START_DIR}/.."
cd python3
./enable-kms-key.py "${MGMT_KMS_KEY_ID}"

# Set environment to the AWS account where the shared ECR repository is maintained

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ECR_REPO_ENV}"

# Build and push API and worker to ECR

echo "Build and push API and worker to ECR..."

# Log on to ECR

aws --region "${AWS_DEFAULT_REGION}" ecr get-login-password \
  | docker login --username AWS --password-stdin "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}.dkr.ecr.us-east-1.amazonaws.com" \
  1> /dev/null \
  2> /dev/null

# Build API and worker (if creating a new image)

cd "${START_DIR}/.."
cd ..

mvn clean package -DskipTests
sleep 5

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
COMMIT_NUMBER=$(git rev-parse "${CURRENT_BRANCH}" | cut -c1-7)
IMAGE_VERSION="${TARGET_CMS_ENV}-latest-${COMMIT_NUMBER}"

# Build API docker images

cd "${START_DIR}/.."
cd ../api
docker build \
  --no-cache \
  --tag "ab2d_api:${IMAGE_VERSION}" .
docker build \
  --no-cache \
  --tag "ab2d_api:${TARGET_CMS_ENV}-latest" .

# Build worker docker image

cd "${START_DIR}/.."
cd ../worker
docker build \
  --no-cache \
  --tag "ab2d_worker:${IMAGE_VERSION}" .
docker build \
  --no-cache \
  --tag "ab2d_worker:${TARGET_CMS_ENV}-latest" .

# Get or create api repo

API_ECR_REPO_URI=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-repositories \
  --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
  --output text)
if [ -z "${API_ECR_REPO_URI}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" ecr create-repository \
    --repository-name "ab2d_api" \
    1> /dev/null \
    2> /dev/null
  API_ECR_REPO_URI=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
    --output text)
fi

# Get ecr repo aws account

ECR_REPO_AWS_ACCOUNT=$(aws --region "${AWS_DEFAULT_REGION}" sts get-caller-identity \
  --query Account \
  --output text)

# Apply ecr repo policy to the "ab2d_api" repo

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ECR_REPO_ENV}"
aws --region "${AWS_DEFAULT_REGION}" ecr set-repository-policy \
  --repository-name ab2d_api \
  --policy-text file://ab2d-ecr-policy.json \
  1> /dev/null \
  2> /dev/null

# Tag and push two copies (image version and latest version) of API docker image to ECR
# - image version keeps track of the master commit number (e.g. ab2d-dev-latest-3d0905b)
# - latest version is the same image but without the commit number (e.g. ab2d-dev-latest)
# - NOTE: the latest version is used by the ecs task definition so that it can remain static for zero downtime

docker tag "ab2d_api:${IMAGE_VERSION}" "${API_ECR_REPO_URI}:${IMAGE_VERSION}"
docker push "${API_ECR_REPO_URI}:${IMAGE_VERSION}"
docker tag "ab2d_api:${TARGET_CMS_ENV}-latest" "${API_ECR_REPO_URI}:${TARGET_CMS_ENV}-latest"
docker push "${API_ECR_REPO_URI}:${TARGET_CMS_ENV}-latest"

# Get or create worker repo

WORKER_ECR_REPO_URI=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-repositories \
  --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
  --output text)
if [ -z "${WORKER_ECR_REPO_URI}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" ecr create-repository \
    --repository-name "ab2d_worker" \
    1> /dev/null \
    2> /dev/null
  WORKER_ECR_REPO_URI=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
    --output text)
fi

# Apply ecr repo policy to the "ab2d_worker" repo

cd "${START_DIR}/.."
cd "terraform/environments/${CMS_ECR_REPO_ENV}"
aws --region "${AWS_DEFAULT_REGION}" ecr set-repository-policy \
  --repository-name ab2d_worker \
  --policy-text file://ab2d-ecr-policy.json \
  1> /dev/null \
  2> /dev/null

# Tag and push two copies (image version and latest version) of worker docker image to ECR
# - image version keeps track of the master commit number (e.g. ab2d-dev-latest-3d0905b)
# - latest version is the same image but without the commit number (e.g. ab2d-dev-latest)
# - NOTE: the latest version is used by the ecs task definition so that it can remain static for zero downtime

docker tag "ab2d_worker:${IMAGE_VERSION}" "${WORKER_ECR_REPO_URI}:${IMAGE_VERSION}"
docker push "${WORKER_ECR_REPO_URI}:${IMAGE_VERSION}"
docker tag "ab2d_worker:${TARGET_CMS_ENV}-latest" "${WORKER_ECR_REPO_URI}:${TARGET_CMS_ENV}-latest"
docker push "${WORKER_ECR_REPO_URI}:${TARGET_CMS_ENV}-latest"

# Get list of untagged images in the api repository

IMAGES_TO_DELETE=$(aws --region "${AWS_DEFAULT_REGION}" ecr list-images \
  --repository-name ab2d_api \
  --filter "tagStatus=UNTAGGED" \
  --query 'imageIds[*]' \
  --output json)

# Delete untagged images in the api repository

aws --region "${AWS_DEFAULT_REGION}" ecr batch-delete-image \
  --repository-name ab2d_api \
  --image-ids "$IMAGES_TO_DELETE" \
  || true \
  1> /dev/null \
  2> /dev/null

# Get old 'latest-commit' api tag

API_OLD_LATEST_COMMIT_TAG=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-images \
  --repository-name ab2d_api \
  --query "imageDetails[*].{imageTag:imageTags[0],imagePushedAt:imagePushedAt}" \
  --output json \
  | jq 'sort_by(.imagePushedAt) | reverse' \
  | jq ".[] | select(.imageTag | startswith(\"${TARGET_CMS_ENV}-latest\"))" \
  | jq --slurp '.' \
  | jq 'del(.[0,1])' \
  | jq '.[] | select(.imageTag)' \
  | jq '.imageTag' \
  | tr -d '"' \
  | head -1)

if [ -n "${API_OLD_LATEST_COMMIT_TAG}" ]; then

  # Rename 'latest-commit' api tag

  RENAME_API_OLD_LATEST_COMMIT_TAG=${API_OLD_LATEST_COMMIT_TAG/'-latest-'/'-'}

  # Get manifest of tag to rename

  MANIFEST=$(aws --region "${AWS_DEFAULT_REGION}" ecr batch-get-image \
    --repository-name ab2d_api \
    --image-ids imageTag="${API_OLD_LATEST_COMMIT_TAG}" \
    --query 'images[].imageManifest' \
    --output text)

  # Add renamed api tag

  if [ -n "${MANIFEST}" ]; then
    aws --region "${AWS_DEFAULT_REGION}" ecr put-image \
      --repository-name ab2d_api \
      --image-tag "${RENAME_API_OLD_LATEST_COMMIT_TAG}" \
      --image-manifest "${MANIFEST}" \
      1> /dev/null \
      2> /dev/null
  fi

  # Remove old api tag

  aws --region "${AWS_DEFAULT_REGION}" ecr batch-delete-image \
    --repository-name ab2d_api \
    --image-ids imageTag="${API_OLD_LATEST_COMMIT_TAG}" \
    1> /dev/null \
    2> /dev/null

fi

# Get list of untagged images in the worker repository

IMAGES_TO_DELETE=$(aws --region "${AWS_DEFAULT_REGION}" ecr list-images \
  --repository-name ab2d_worker \
  --filter "tagStatus=UNTAGGED" \
  --query 'imageIds[*]' \
  --output json)

# Delete untagged images in the worker repository

aws --region "${AWS_DEFAULT_REGION}" ecr batch-delete-image \
  --repository-name ab2d_worker \
  --image-ids "$IMAGES_TO_DELETE" \
  || true \
  1> /dev/null \
  2> /dev/null

# Get old 'latest-commit' worker tag

WORKER_OLD_LATEST_COMMIT_TAG=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-images \
  --repository-name ab2d_worker \
  --query "imageDetails[*].{imageTag:imageTags[0],imagePushedAt:imagePushedAt}" \
  --output json \
  | jq 'sort_by(.imagePushedAt) | reverse' \
  | jq ".[] | select(.imageTag | startswith(\"${TARGET_CMS_ENV}-latest\"))" \
  | jq --slurp '.' \
  | jq 'del(.[0,1])' \
  | jq '.[] | select(.imageTag)' \
  | jq '.imageTag' \
  | tr -d '"' \
  | head -1)

if [ -n "${WORKER_OLD_LATEST_COMMIT_TAG}" ]; then

  # Rename 'latest-commit' worker tag

  RENAME_WORKER_OLD_LATEST_COMMIT_TAG=${WORKER_OLD_LATEST_COMMIT_TAG/'-latest-'/'-'}

  # Get manifest of tag to rename

  MANIFEST=$(aws --region "${AWS_DEFAULT_REGION}" ecr batch-get-image \
    --repository-name ab2d_worker \
    --image-ids imageTag="${WORKER_OLD_LATEST_COMMIT_TAG}" \
    --query 'images[].imageManifest' \
    --output text)

  # Add renamed worker tag

  if [ -n "${MANIFEST}" ]; then
    aws --region "${AWS_DEFAULT_REGION}" ecr put-image \
      --repository-name ab2d_worker \
      --image-tag "${RENAME_WORKER_OLD_LATEST_COMMIT_TAG}" \
      --image-manifest "${MANIFEST}" \
      1> /dev/null \
      2> /dev/null
  fi

  # Remove old worker tag

  aws --region "${AWS_DEFAULT_REGION}" ecr batch-delete-image \
    --repository-name ab2d_worker \
    --image-ids imageTag="${WORKER_OLD_LATEST_COMMIT_TAG}" \
    1> /dev/null \
    2> /dev/null

fi

# Verify that the image versions of API and Worker are the same

IMAGE_VERSION_API=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-images \
  --repository-name ab2d_api \
  --query "sort_by(imageDetails,& imagePushedAt)[*].imageTags[? @ == '${TARGET_CMS_ENV}-latest']" \
  --output text)

IMAGE_VERSION_WORKER=$(aws --region "${AWS_DEFAULT_REGION}" ecr describe-images \
  --repository-name ab2d_worker \
  --query "sort_by(imageDetails,& imagePushedAt)[*].imageTags[? @ == '${TARGET_CMS_ENV}-latest']" \
  --output text)

if [ "${IMAGE_VERSION_API}" != "${IMAGE_VERSION_WORKER}" ]; then
  echo "ERROR: The ECR image versions for ab2d_api and ab2d_worker must be the same!"
  exit 1
else
  echo "The ECR image versions for ab2d_api and ab2d_worker were verified to be the same..."
fi

echo "Using master branch commit number '${COMMIT_NUMBER}' for ab2d_api and ab2d_worker..."

#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${TARGET_AWS_ACCOUNT_NUMBER}" "${TARGET_CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${TARGET_AWS_ACCOUNT_NUMBER}" "${TARGET_CMS_ENV}"
fi

# Reset to the target environment

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

#
# Get current known good ECS task definitions
#

CLUSTER_ARNS=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-clusters \
  --query 'clusterArns' \
  --output text \
  | grep "/${TARGET_CMS_ENV}-api" \
  | xargs \
  | tr -d '\r')

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping getting current ECS task definitions, since there are no existing clusters"
else
  echo "Using existing ECS task definitions..."
fi

#
# Get ECS task counts before making any changes
#

echo "Get ECS task counts before making any changes..."

# Define task count functions

api_task_count() { aws --region "${AWS_DEFAULT_REGION}" ecs list-tasks --cluster "${TARGET_CMS_ENV}-api" | grep -c "\:task\/" | tr -d ' '; }
worker_task_count() { aws --region "${AWS_DEFAULT_REGION}" ecs list-tasks --cluster "${TARGET_CMS_ENV}-worker" | grep -c "\:task\/" | tr -d ' '; }

# Get old api task count (if exists)

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_TASK_COUNT, since there are no existing clusters"
else
  OLD_API_TASK_COUNT=$(api_task_count)
  OLD_WORKER_TASK_COUNT=$(worker_task_count)
fi

# Set expected api and worker task counts

if [ -z "${CLUSTER_ARNS}" ]; then
  EXPECTED_API_COUNT=$((EC2_DESIRED_INSTANCE_COUNT_API))
  EXPECTED_WORKER_COUNT=$((EC2_DESIRED_INSTANCE_COUNT_WORKER))
else
  EXPECTED_API_COUNT=$((OLD_API_TASK_COUNT+EC2_DESIRED_INSTANCE_COUNT_API))
  EXPECTED_WORKER_COUNT=$((OLD_WORKER_TASK_COUNT+EC2_DESIRED_INSTANCE_COUNT_WORKER))
fi

#
# Ensure Old Autoscaling Groups and containers are around to service requests
#

echo "Ensure Old Autoscaling Groups and containers are around to service requests..."

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autoscaling group and launch configuration, since there are no existing clusters"
else
  terraform state rm module.api.aws_autoscaling_group.asg
  terraform state rm module.api.aws_launch_configuration.launch_config
  terraform state rm module.worker.aws_autoscaling_group.asg
  terraform state rm module.worker.aws_launch_configuration.launch_config
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autoscaling group and launch configuration, since there are no existing clusters"
else

  OLD_API_CONTAINER_INSTANCES=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-container-instances \
    --cluster "${TARGET_CMS_ENV}-api" \
    --output text)

  if [ -n "${OLD_API_CONTAINER_INSTANCES}" ]; then
    OLD_API_CONTAINER_INSTANCES=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-container-instances \
      --cluster "${TARGET_CMS_ENV}-api" \
      | grep container-instance)
  fi

  OLD_WORKER_CONTAINER_INSTANCES=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-container-instances \
    --cluster "${TARGET_CMS_ENV}-worker" \
    --output text)

  if [ -n "${OLD_WORKER_CONTAINER_INSTANCES}" ]; then
    OLD_WORKER_CONTAINER_INSTANCES=$(aws --region "${AWS_DEFAULT_REGION}" ecs list-container-instances \
      --cluster "${TARGET_CMS_ENV}-worker" \
      | grep container-instance)
  fi

fi

#
# Deploy API and Worker
#

echo "Deploy API and Worker..."

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

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

# Get database secret manager ARNs

DATABASE_HOST_SECRET_ARN=$(aws --region "${AWS_DEFAULT_REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${TARGET_CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PORT_SECRET_ARN=$(aws --region "${AWS_DEFAULT_REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${TARGET_CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_USER_SECRET_ARN=$(aws --region "${AWS_DEFAULT_REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${TARGET_CMS_ENV}/module/db/database_user/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PASSWORD_SECRET_ARN=$(aws --region "${AWS_DEFAULT_REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${TARGET_CMS_ENV}/module/db/database_password/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_NAME_SECRET_ARN=$(aws --region "${AWS_DEFAULT_REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${TARGET_CMS_ENV}/module/db/database_name/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

# Change to the target terraform environment

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

# Get the BFD keystore file name

BFD_KEYSTORE_FILE_NAME=$(echo "${BFD_KEYSTORE_LOCATION}" | cut -d"/" -f 6)

# Get load balancer listener protocol, port, and certificate

if [ "$TARGET_CMS_ENV" == "ab2d-sbx-sandbox" ]; then
  ALB_LISTENER_PORT=443
  ALB_LISTENER_PROTOCOL="HTTPS"
  ALB_LISTENER_CERTIFICATE_ARN=$(aws --region "${AWS_DEFAULT_REGION}" acm list-certificates \
    --query "CertificateSummaryList[?DomainName=='sandbox.ab2d.cms.gov'].CertificateArn" \
    --output text)
  ALB_SECURITY_GROUP_IP_RANGE="0.0.0.0/0"
elif [ "$TARGET_CMS_ENV" == "ab2d-dev" ]; then
  ALB_LISTENER_PORT=443
  ALB_LISTENER_PROTOCOL="HTTPS"
  ALB_LISTENER_CERTIFICATE_ARN=$(aws --region "${AWS_DEFAULT_REGION}" acm list-certificates \
    --query "CertificateSummaryList[?DomainName=='dev.ab2d.cms.gov'].CertificateArn" \
    --output text)
  ALB_SECURITY_GROUP_IP_RANGE="${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}"
elif [ "$TARGET_CMS_ENV" == "ab2d-east-prod" ]; then
  ALB_LISTENER_PORT=443
  ALB_LISTENER_PROTOCOL="HTTPS"
  ALB_LISTENER_CERTIFICATE_ARN=$(aws --region "${AWS_DEFAULT_REGION}" acm list-certificates \
    --query "CertificateSummaryList[?DomainName=='api.ab2d.cms.gov'].CertificateArn" \
    --output text)
  ALB_SECURITY_GROUP_IP_RANGE="${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}"
elif [ "$TARGET_CMS_ENV" == "ab2d-east-impl" ]; then
  ALB_LISTENER_PORT=443
  ALB_LISTENER_PROTOCOL="HTTPS"
  ALB_LISTENER_CERTIFICATE_ARN=$(aws --region "${AWS_DEFAULT_REGION}" acm list-certificates \
    --query "CertificateSummaryList[?DomainName=='impl.ab2d.cms.gov'].CertificateArn" \
    --output text)
  ALB_SECURITY_GROUP_IP_RANGE="${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}"
else
  echo "ERROR: '${TARGET_CMS_ENV}' is unknown."
  exit 1
fi

# Run automation for API and worker

terraform apply \
  --var "env=${TARGET_CMS_ENV}" \
  --var	"aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
  --var "ami_id=$AMI_ID" \
  --var "current_task_definition_arn=$API_TASK_DEFINITION" \
  --var "db_host=$DATABASE_HOST" \
  --var "db_port=$DATABASE_PORT" \
  --var "db_username=$DATABASE_USER" \
  --var "db_password=$DATABASE_PASSWORD" \
  --var "db_name=$DATABASE_NAME" \
  --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
  --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
  --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
  --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
  --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
  --var "deployer_ip_address=$DEPLOYER_IP_ADDRESS" \
  --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
  --var "image_version=$IMAGE_VERSION" \
  --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
  --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
  --var "ecs_task_definition_host_port=$ALB_LISTENER_PORT" \
  --var "host_port=$ALB_LISTENER_PORT" \
  --var "alb_listener_protocol=$ALB_LISTENER_PROTOCOL" \
  --var "alb_listener_certificate_arn=$ALB_LISTENER_CERTIFICATE_ARN" \
  --var "alb_internal=$ALB_INTERNAL" \
  --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
  --var "ab2d_keystore_location=${AB2D_KEYSTORE_LOCATION}" \
  --var "ab2d_keystore_password=${AB2D_KEYSTORE_PASSWORD}" \
  --var "ab2d_okta_jwt_issuer=${AB2D_OKTA_JWT_ISSUER}" \
  --var "ab2d_hpms_url=${AB2D_HPMS_URL}" \
  --var "ab2d_hpms_api_params=${AB2D_HPMS_API_PARAMS}" \
  --var "ab2d_hpms_auth_key_id=${AB2D_HPMS_AUTH_KEY_ID}" \
  --var "ab2d_hpms_auth_key_secret=${AB2D_HPMS_AUTH_KEY_SECRET}" \
  --var "ab2d_slack_alert_webhooks=${AB2D_SLACK_ALERT_WEBHOOKS}" \
  --var "ab2d_slack_trace_webhooks=${AB2D_SLACK_TRACE_WEBHOOKS}" \
  --var "gold_image_name=${GOLD_IMAGE_NAME}" \
  --var "ec2_desired_instance_count_api=${EC2_DESIRED_INSTANCE_COUNT_API}" \
  --var "ec2_minimum_instance_count_api=${EC2_MINIMUM_INSTANCE_COUNT_API}" \
  --var "ec2_maximum_instance_count_api=${EC2_MAXIMUM_INSTANCE_COUNT_API}" \
  --target module.api \
  --auto-approve \
  1> /dev/null \
  2> /dev/null

terraform apply \
  --var "env=${TARGET_CMS_ENV}" \
  --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
  --var "ami_id=$AMI_ID" \
  --var "current_task_definition_arn=$WORKER_TASK_DEFINITION" \
  --var "db_host=$DATABASE_HOST" \
  --var "db_port=$DATABASE_PORT" \
  --var "db_username=$DATABASE_USER" \
  --var "db_password=$DATABASE_PASSWORD" \
  --var "db_name=$DATABASE_NAME" \
  --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
  --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
  --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
  --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
  --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
  --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
  --var "image_version=$IMAGE_VERSION" \
  --var "bfd_url=$BFD_URL" \
  --var "bfd_keystore_location=$BFD_KEYSTORE_LOCATION" \
  --var "bfd_keystore_password=$BFD_KEYSTORE_PASSWORD" \
  --var "hicn_hash_pepper=$HICN_HASH_PEPPER" \
  --var "hicn_hash_iter=$HICN_HASH_ITER" \
  --var "bfd_keystore_file_name=$BFD_KEYSTORE_FILE_NAME" \
  --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
  --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
  --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
  --var "ab2d_slack_alert_webhooks=${AB2D_SLACK_ALERT_WEBHOOKS}" \
  --var "ab2d_slack_trace_webhooks=${AB2D_SLACK_TRACE_WEBHOOKS}" \
  --var "gold_image_name=${GOLD_IMAGE_NAME}" \
  --var "ec2_desired_instance_count_worker=${EC2_DESIRED_INSTANCE_COUNT_WORKER}" \
  --var "ec2_minimum_instance_count_worker=${EC2_MINIMUM_INSTANCE_COUNT_WORKER}" \
  --var "ec2_maximum_instance_count_worker=${EC2_MAXIMUM_INSTANCE_COUNT_WORKER}" \
  --target module.worker \
  --auto-approve \
  1> /dev/null \
  2> /dev/null

#
# Get the correct target group arn for CloudWatch
#

# shellcheck disable=SC2016
TARGET_GROUP_ARN_SUFFIX=$(aws --region "${AWS_DEFAULT_REGION}" elbv2 describe-target-groups \
  --query 'TargetGroups[?length(LoadBalancerArns)> `0`].TargetGroupArn' \
  --output text \
  | awk 'BEGIN { FS = ":" } ; {print $6}')

#
# Deploy CloudWatch
#

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

echo "Deploy CloudWatch..."

terraform apply \
  --target module.cloudwatch \
  --var "env=${TARGET_CMS_ENV}" \
  --var "ami_id=$AMI_ID" \
  --var "ecs_task_definition_host_port=$ALB_LISTENER_PORT" \
  --var "host_port=$ALB_LISTENER_PORT" \
  --var "alb_listener_protocol=$ALB_LISTENER_PROTOCOL" \
  --var "alb_listener_certificate_arn=$ALB_LISTENER_CERTIFICATE_ARN" \
  --var "alb_internal=$ALB_INTERNAL" \
  --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  --var "gold_image_name=${GOLD_IMAGE_NAME}" \
  --var "target_group_arn_suffix=${TARGET_GROUP_ARN_SUFFIX}" \
  --auto-approve \
  1> /dev/null \
  2> /dev/null

#
# Deploy AWS WAF
#

terraform apply \
  --var "env=${TARGET_CMS_ENV}" \
  --var "alb_internal=$ALB_INTERNAL" \
  --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  --target module.waf \
  --auto-approve \
  1> /dev/null \
  2> /dev/null

#
# Apply AWS Shield standard to the application load balancer
#

# Note that no change is actually made since AWS shield standard is automatically applied to
# the application load balancer. This section may be needed later if AWS Shield Advanced is
# applied instead.

terraform apply \
  --var "env=${TARGET_CMS_ENV}" \
  --target module.shield \
  --auto-approve \
  1> /dev/null \
  2> /dev/null

#
# Ensure new autoscaling group is running containers
#

echo "Ensure new autoscaling group is running containers..."

ACTUAL_API_COUNT=0
RETRIES_API=0

while [ "$ACTUAL_API_COUNT" -lt "$EXPECTED_API_COUNT" ]; do
  ACTUAL_API_COUNT=$(api_task_count)
  echo "Running API Tasks: $ACTUAL_API_COUNT, Expected: $EXPECTED_API_COUNT"
  if [ "$RETRIES_API" != "15" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    RETRIES_API=$((RETRIES_API + 1))
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

ACTUAL_WORKER_COUNT=0
RETRIES_WORKER=0

while [ "$ACTUAL_WORKER_COUNT" -lt "$EXPECTED_WORKER_COUNT" ]; do
  ACTUAL_WORKER_COUNT=$(worker_task_count)
  echo "Running WORKER Tasks: $ACTUAL_WORKER_COUNT, Expected: $EXPECTED_WORKER_COUNT"
  if [ "$RETRIES_WORKER" != "15" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    RETRIES_WORKER=$((RETRIES_WORKER + 1))
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

# Drain old container instances

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping draining old container instances, since there are no existing clusters"
else
  if [ -n "${OLD_API_CONTAINER_INSTANCES}" ]; then
    OLD_API_INSTANCE_LIST=$(echo "${OLD_API_CONTAINER_INSTANCES}" \
      | tr -d ' ' \
      | tr "\n" " " \
      | tr -d "," \
      | tr '""' ' ' \
      | tr -d '"')

    # shellcheck disable=SC2086
    # $OLD_API_INSTANCE_LIST format is required here
    aws --region "${AWS_DEFAULT_REGION}" ecs update-container-instances-state \
      --cluster "${TARGET_CMS_ENV}-api" \
      --status DRAINING \
      --container-instances $OLD_API_INSTANCE_LIST \
      1> /dev/null \
      2> /dev/null

  fi
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
      --cluster "${TARGET_CMS_ENV}-worker" \
      --status DRAINING \
      --container-instances $OLD_WORKER_INSTANCE_LIST \
      1> /dev/null \
      2> /dev/null

    echo "Allowing all instances to drain for 60 seconds before proceeding..."
    sleep 60
  fi
fi

# Remove any duplicative API autoscaling groups

API_ASG_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
  --query "AutoScalingGroups[*].Tags[?Value == '${TARGET_CMS_ENV}-api'].ResourceId" \
  --output text \
  | sort \
  | wc -l \
  | tr -d ' ')

if [ "${API_ASG_COUNT}" -gt 1 ]; then
  DUPLICATIVE_API_ASG_COUNT=$((API_ASG_COUNT - 1))
  for ((i=1;i<=DUPLICATIVE_API_ASG_COUNT;i++)); do
    DUPLICATIVE_API_ASG=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
      --query "AutoScalingGroups[*].Tags[?Value == '${TARGET_CMS_ENV}-api'].ResourceId" \
      --output text \
      | sort \
      | head -n "${i}" \
      | tail -n 1)
    aws --region "${AWS_DEFAULT_REGION}" autoscaling delete-auto-scaling-group \
      --auto-scaling-group-name "${DUPLICATIVE_API_ASG}" \
      --force-delete || true \
      1> /dev/null \
      2> /dev/null
  done
fi

# Remove any duplicative worker autoscaling groups

WORKER_ASG_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
  --query "AutoScalingGroups[*].Tags[?Value == '${TARGET_CMS_ENV}-worker'].ResourceId" \
  --output text \
  | sort \
  | wc -l \
  | tr -d ' ')

if [ "${WORKER_ASG_COUNT}" -gt 1 ]; then
  DUPLICATIVE_WORKER_ASG_COUNT=$((WORKER_ASG_COUNT - 1))
  for ((i=1;i<=DUPLICATIVE_WORKER_ASG_COUNT;i++)); do
    DUPLICATIVE_WORKER_ASG=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-auto-scaling-groups \
      --query "AutoScalingGroups[*].Tags[?Value == '${TARGET_CMS_ENV}-worker'].ResourceId" \
      --output text \
      | sort \
      | head -n "${i}" \
      | tail -n 1)
    aws --region "${AWS_DEFAULT_REGION}" autoscaling delete-auto-scaling-group \
      --auto-scaling-group-name "${DUPLICATIVE_WORKER_ASG}" \
      --force-delete || true \
      1> /dev/null \
      2> /dev/null
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
  if [ "$RETRIES_ASG" != "15" ]; then
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

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing old launch configurations, since there are no existing clusters"
else

  LAUNCH_CONFIGURATION_EXPECTED_COUNT=2
  LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-launch-configurations \
    --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
    --output text \
    | sort -k2 \
    | grep -v "\-test\-" \
    | grep -cv "\-validation\-")

  while [ "$LAUNCH_CONFIGURATION_ACTUAL_COUNT" -gt "$LAUNCH_CONFIGURATION_EXPECTED_COUNT" ]; do

    # Note that "*-test-*" and "*-validation-*" launch configurations will be excluded
    OLD_LAUNCH_CONFIGURATION=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      --output text \
      | sort -k2 \
      | grep -v "\-test\-" \
      | grep -v "\-validation\-" \
      | head -n1 \
      | awk '{print $1}')

    aws --region "${AWS_DEFAULT_REGION}" autoscaling delete-launch-configuration \
      --launch-configuration-name "${OLD_LAUNCH_CONFIGURATION}" \
      1> /dev/null \
      2> /dev/null
    sleep 5

    # Note that "*-test-*" and "*-validation-*" launch configurations will be excluded
    LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      --output text \
      | sort -k2 \
      | grep -v "\-test\-" \
      | grep -cv "\-validation\-")

  done

fi

# Remove old target groups

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing old launch configurations, since there are no existing clusters"
else

  # Get count of old target groups

  OLD_TARGET_GROUP_COUNT=$(aws --region "${AWS_DEFAULT_REGION}" elbv2 describe-target-groups \
    --query "TargetGroups[?LoadBalancerArns==\`[]\`].TargetGroupName" \
    | jq '. | length')

  if [ "${OLD_TARGET_GROUP_COUNT}" -gt 0 ]; then
    for ((i=1;i<=OLD_TARGET_GROUP_COUNT;i++)); do
      OLD_TARGET_GROUP_ARN=$(aws --region "${AWS_DEFAULT_REGION}" elbv2 describe-target-groups \
        --query "TargetGroups[?LoadBalancerArns==\`[]\`].TargetGroupArn" \
        --output text \
        | sort -k2 \
        | head -n1 \
        | awk '{print $1}')

      aws --region "${AWS_DEFAULT_REGION}" elbv2 delete-target-group \
        --target-group-arn "${OLD_TARGET_GROUP_ARN}" \
        1> /dev/null \
        2> /dev/null

    done
  fi
fi
