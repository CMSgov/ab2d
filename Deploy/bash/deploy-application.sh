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
    || [ -z "${CMS_ECR_REPO_ENV_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${VPC_ID_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_API_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_WORKER_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_API_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_API_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM}" ] \
    || [ -z "${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${INTERNET_FACING_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set parameters
#

CMS_ENV="${CMS_ENV_PARAM}"

CMS_ECR_REPO_ENV="${CMS_ECR_REPO_ENV_PARAM}"

REGION="${REGION_PARAM}"

VPC_ID="${VPC_ID_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

EC2_INSTANCE_TYPE_API="${EC2_INSTANCE_TYPE_API_PARAM}"

EC2_INSTANCE_TYPE_WORKER="${EC2_INSTANCE_TYPE_WORKER_PARAM}"

EC2_DESIRED_INSTANCE_COUNT_API="${EC2_DESIRED_INSTANCE_COUNT_API_PARAM}"

EC2_MINIMUM_INSTANCE_COUNT_API="${EC2_DESIRED_INSTANCE_COUNT_API_PARAM}"

EC2_MAXIMUM_INSTANCE_COUNT_API="${EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM}"

EC2_DESIRED_INSTANCE_COUNT_WORKER="${EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM}"

EC2_MINIMUM_INSTANCE_COUNT_WORKER="${EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM}"

EC2_MAXIMUM_INSTANCE_COUNT_WORKER="${EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

# Set whether load balancer is internal based on "internet-facing" parameter

if [ "$INTERNET_FACING_PARAM" == "false" ]; then
  ALB_INTERNAL=true
elif [ "$INTERNET_FACING_PARAM" == "true" ]; then
  ALB_INTERNAL=false
else
  echo "ERROR: the '--internet-facing' parameter must be true or false"
  exit 1
fi

# Set whether CloudTamer API should be used

if [ "$CLOUD_TAMER" != "false"  && "$CLOUD_TAMER" != "true" ]; then
  echo "ERROR: the '--cloud-tamer' parameter must be true or false"
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
else
  echo "ERROR: 'CMS_ENV' environment is unknown."
  exit 1  
fi

#
# Define functions
#

get_temporary_aws_credentials ()
{
  # Set AWS account number

  AWS_ACCOUNT_NUMBER="$1"

  # Verify that CloudTamer user name and password environment variables are set

  if [ -z $CLOUDTAMER_USER_NAME ] || [ -z $CLOUDTAMER_PASSWORD ]; then
    echo ""
    echo "----------------------------"
    echo "Enter CloudTamer credentials"
    echo "----------------------------"
  fi

  if [ -z $CLOUDTAMER_USER_NAME ]; then
    echo ""
    echo "Enter your CloudTamer user name (EUA ID):"
    read CLOUDTAMER_USER_NAME
  fi

  if [ -z $CLOUDTAMER_PASSWORD ]; then
    echo ""
    echo "Enter your CloudTamer password:"
    read CLOUDTAMER_PASSWORD
  fi

  # Get bearer token

  echo ""
  echo "--------------------"
  echo "Getting bearer token"
  echo "--------------------"
  echo ""

  BEARER_TOKEN=$(curl --location --request POST 'https://cloudtamer.cms.gov/api/v2/token' \
    --header 'Accept: application/json' \
    --header 'Accept-Language: en-US,en;q=0.5' \
    --header 'Content-Type: application/json' \
    --data-raw "{\"username\":\"${CLOUDTAMER_USER_NAME}\",\"password\":\"${CLOUDTAMER_PASSWORD}\",\"idms\":{\"id\":2}}" \
    | jq --raw-output ".data.access.token")

  # Get json output for temporary AWS credentials

  echo ""
  echo "-----------------------------"
  echo "Getting temporary credentials"
  echo "-----------------------------"
  echo ""

  JSON_OUTPUT=$(curl --location --request POST 'https://cloudtamer.cms.gov/api/v3/temporary-credentials' \
    --header 'Accept: application/json' \
    --header 'Accept-Language: en-US,en;q=0.5' \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer ${BEARER_TOKEN}" \
    --header 'Content-Type: application/json' \
    --data-raw "{\"account_number\":\"${AWS_ACCOUNT_NUMBER}\",\"iam_role_name\":\"ab2d-spe-developer\"}" \
    | jq --raw-output ".data")

  # Set default AWS region

  export AWS_DEFAULT_REGION=us-east-1

  # Get temporary AWS credentials

  export AWS_ACCESS_KEY_ID=$(echo $JSON_OUTPUT | jq --raw-output ".access_key")
  export AWS_SECRET_ACCESS_KEY=$(echo $JSON_OUTPUT | jq --raw-output ".secret_access_key")

  # Get AWS session token (required for temporary credentials)

  export AWS_SESSION_TOKEN=$(echo $JSON_OUTPUT | jq --raw-output ".session_token")

  # Verify AWS credentials

  if [ -z "${AWS_ACCESS_KEY_ID}" ] \
      || [ -z "${AWS_SECRET_ACCESS_KEY}" ] \
      || [ -z "${AWS_SESSION_TOKEN}" ]; then
    echo "**********************************************************************"
    echo "ERROR: AWS credentials do not exist for the ${CMS_ENV} AWS account"
    echo "**********************************************************************"
    echo ""
    exit 1
  fi
}

#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  get_temporary_aws_credentials "${CMS_ENV_AWS_ACCOUNT_NUMBER}"
else
  export AWS_PROFILE="${CMS_ENV}"
fi

#
# Verify that VPC ID exists
#

VPC_EXISTS=$(aws --region "${REGION}" ec2 describe-vpcs \
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

KMS_KEY_ID=$(aws --region "${REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${KMS_KEY_ID}" ]; then
  KMS_KEY_STATE=$(aws --region "${REGION}" kms describe-key \
    --key-id alias/ab2d-kms \
    --query "KeyMetadata.KeyState" \
    --output text)
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

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

#
# Configure docker environment
#

# Delete orphaned volumes (if any)

docker volume ls -qf dangling=true | xargs -I name docker volume rm name

# Delete all containers (if any)

docker ps -aq | xargs -I name docker rm --force name

# Delete all images (if any)

docker images -q | xargs -I name docker rmi --force name

# Delete orphaned volumes again (if any)

docker volume ls -qf dangling=true | xargs -I name docker volume rm name

# Delete all images again (if any)

docker images -q | xargs -I name docker rmi --force name

#
# Initialize and validate terraform
#

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-automation" \
  -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Get and enable KMS key id for target environment
#

echo "Enabling KMS key..."
cd "${START_DIR}/.."
cd python3
./enable-kms-key.py $KMS_KEY_ID

#
# Get secrets
#

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

# Get bfd url secret

BFD_URL=$(./get-database-secret.py $CMS_ENV bfd_url $DATABASE_SECRET_DATETIME)

if [ -z "${BFD_URL}" ]; then
  echo "********************************"
  echo "ERROR: bfd url secret not found."
  echo "********************************"
  exit 1
fi

# Get bfd keystore location secret

BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV bfd_keystore_location $DATABASE_SECRET_DATETIME)

if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
  echo "**********************************************"
  echo "ERROR: bfd keystore location secret not found."
  echo "**********************************************"
  exit 1
fi

# Get bfd keystore password secret

BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV bfd_keystore_password $DATABASE_SECRET_DATETIME)

if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
  echo "**********************************************"
  echo "ERROR: bfd keystore password secret not found."
  echo "**********************************************"
  exit 1
fi

# Get hicn hash pepper secret

HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV hicn_hash_pepper $DATABASE_SECRET_DATETIME)

if [ -z "${HICN_HASH_PEPPER}" ]; then
  echo "*****************************************"
  echo "ERROR: hicn hash pepper secret not found."
  echo "*****************************************"
  exit 1
fi

# Get hicn hash iter secret

HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV hicn_hash_iter $DATABASE_SECRET_DATETIME)

if [ -z "${HICN_HASH_ITER}" ]; then
  echo "***************************************"
  echo "ERROR: hicn hash iter secret not found."
  echo "***************************************"
  exit 1
fi

# Get new relic app name secret

NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV new_relic_app_name $DATABASE_SECRET_DATETIME)

if [ -z "${NEW_RELIC_APP_NAME}" ]; then
  echo "*******************************************"
  echo "ERROR: new relic app name secret not found."
  echo "*******************************************"
  exit 1
fi

# Get new relic license key secret

NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV new_relic_license_key $DATABASE_SECRET_DATETIME)

if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
  echo "**********************************************"
  echo "ERROR: new relic license key secret not found."
  echo "**********************************************"
  exit 1
fi

# Get private ip address CIDR range for VPN

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)

# If any databse secret produced an error, exit the script

if [ "${DATABASE_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_URL}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_KEYSTORE_LOCATION}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_KEYSTORE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${HICN_HASH_PEPPER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${HICN_HASH_ITER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${NEW_RELIC_APP_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Get network attributes
#

# Get first public subnet id

SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "ERROR: public subnet #1 not found..."
  exit 1
fi

# Get second public subnet id

SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then
  echo "ERROR: public subnet #2 not found..."
  exit 1
fi

# Get first private subnet id

SUBNET_PRIVATE_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then
  echo "ERROR: private subnet #1 not found..."
  exit 1
fi

# Get second private subnet id

SUBNET_PRIVATE_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then
  echo "ERROR: private subnet #2 not found..."
  exit 1
fi

#
# Get AMI id
#

AMI_ID=$(aws --region "${REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${AMI_ID}" ]; then
  echo "ERROR: AMI id not found..."
  exit 1
fi

#
# Get deployer IP address
#

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

#
# Create ".auto.tfvars" file for the target environment
#

# Determine cpu and memory for new API ECS container definition

API_EC2_INSTANCE_CPU_COUNT=$(aws --region "${REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_API}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
  --output text)
let API_CPU="($API_EC2_INSTANCE_CPU_COUNT/2)*1024"

API_EC2_INSTANCE_MEMORY=$(aws --region "${REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_API}" --query "InstanceTypes[*].MemoryInfo.SizeInMiB" \
  --output text)
let API_MEMORY="$API_EC2_INSTANCE_MEMORY/2"

# Determine cpu and memory for new worker ECS container definition

WORKER_EC2_INSTANCE_CPU_COUNT=$(aws --region "${REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_WORKER}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
  --output text)
let WORKER_CPU="($WORKER_EC2_INSTANCE_CPU_COUNT/2)*1024"

WORKER_EC2_INSTANCE_MEMORY=$(aws --region "${REGION}" ec2 describe-instance-types \
  --instance-types "${EC2_INSTANCE_TYPE_WORKER}" --query "InstanceTypes[*].MemoryInfo.SizeInMiB" \
  --output text)
let WORKER_MEMORY="$WORKER_EC2_INSTANCE_MEMORY/2"

# Create ".auto.tfvars" file for the target environment

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

echo 'vpc_id = "'$VPC_ID'"' \
  > $CMS_ENV.auto.tfvars
echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars
echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_instance_type_api = "'$EC2_INSTANCE_TYPE_API'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_desired_instance_count_api = "'$EC2_DESIRED_INSTANCE_COUNT_API'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_minimum_instance_count_api = "'$EC2_MINIMUM_INSTANCE_COUNT_API'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_maximum_instance_count_api = "'$EC2_MAXIMUM_INSTANCE_COUNT_API'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ecs_container_definition_new_memory_api = "'$API_MEMORY'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ecs_task_definition_cpu_api = "'$API_CPU'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ecs_task_definition_memory_api = "'$API_MEMORY'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_instance_type_worker = "'$EC2_INSTANCE_TYPE_WORKER'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_desired_instance_count_worker = "'$EC2_DESIRED_INSTANCE_COUNT_WORKER'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_minimum_instance_count_worker = "'$EC2_MINIMUM_INSTANCE_COUNT_WORKER'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ec2_maximum_instance_count_worker = "'$EC2_MAXIMUM_INSTANCE_COUNT_WORKER'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ecs_container_definition_new_memory_worker = "'$WORKER_MEMORY'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ecs_task_definition_cpu_worker = "'$WORKER_CPU'"' \
  >> $CMS_ENV.auto.tfvars
echo 'ecs_task_definition_memory_worker = "'$WORKER_MEMORY'"' \
  >> $CMS_ENV.auto.tfvars
echo 'linux_user = "'$SSH_USERNAME'"' \
  >> $CMS_ENV.auto.tfvars
echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
  >> $CMS_ENV.auto.tfvars

######################################
# Deploy target environment components
######################################

#
# Deploy AWS application modules
#

# Set AWS management environment

if [ "${CLOUD_TAMER}" == "true" ]; then
  get_temporary_aws_credentials "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}"
else
  export AWS_PROFILE="${CMS_ECR_REPO_ENV}"
fi

MGMT_KMS_KEY_ID=$(aws --region "${REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

if [ -n "${MGMT_KMS_KEY_ID}" ]; then
  MGMT_KMS_KEY_STATE=$(aws --region "${REGION}" kms describe-key \
    --key-id alias/ab2d-kms \
    --query "KeyMetadata.KeyState" \
    --output text)
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
./enable-kms-key.py $MGMT_KMS_KEY_ID

# Set environment to the AWS account where the shared ECR repository is maintained

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ECR_REPO_ENV

# Build and push API and worker to ECR

echo "Build and push API and worker to ECR..."

# Log on to ECR
    
read -sra cmd < <(aws --region "${REGION}" ecr get-login --no-include-email)
pass="${cmd[5]}"
unset cmd[4] cmd[5]
"${cmd[@]}" --password-stdin <<< "$pass"

# Build API and worker (if creating a new image)

cd "${START_DIR}/.."
cd ..

mvn clean package -DskipTests
sleep 5

# Get image version based on a master commit number

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
COMMIT_NUMBER_OF_CURRENT_BRANCH=$(git rev-parse "${CURRENT_BRANCH}" | cut -c1-7)
COMMIT_NUMBER_OF_ORIGIN_MASTER=$(git rev-parse origin/master | cut -c1-7)

if [ "${COMMIT_NUMBER_OF_CURRENT_BRANCH}" == "${COMMIT_NUMBER_OF_ORIGIN_MASTER}" ]; then
  echo "NOTE: The current branch is the master branch."
  echo "Using commit number of origin/master branch as the image version."
  COMMIT_NUMBER="${COMMIT_NUMBER_OF_ORIGIN_MASTER}"
else
  echo "NOTE: Assuming this is a DevOps branch that has only DevOps changes."
  COMPARE_BRANCH_WITH_MASTER=$(git log \
    --decorate \
    --graph \
    --oneline \
    --cherry-mark \
    --boundary origin/master..."${BRANCH}")
  if [ -z "${COMPARE_BRANCH_WITH_MASTER}" ]; then
    echo "NOTE: DevOps branch is the same as origin/master."
    echo "Using commit number of origin/master branch as the image version."
    COMMIT_NUMBER="${COMMIT_NUMBER_OF_ORIGIN_MASTER}"
  else
    echo "NOTE: DevOps branch is different from origin/master."
    echo "Using commit number of latest merge from origin/master into the current branch as the image version."
    COMMIT_NUMBER=$(git log --merges | head -n 2 | tail -n 1 | cut -d" " -f 3 | cut -c1-7)
  fi
fi

IMAGE_VERSION="${CMS_ENV}-latest-${COMMIT_NUMBER}"

# Build API docker images

cd "${START_DIR}/.."
cd ../api
docker build \
  --no-cache \
  --tag "ab2d_api:${IMAGE_VERSION}" .
docker build \
  --no-cache \
  --tag "ab2d_api:${CMS_ENV}-latest" .

# Build worker docker image

cd "${START_DIR}/.."
cd ../worker
docker build \
  --no-cache \
  --tag "ab2d_worker:${IMAGE_VERSION}" .
docker build \
  --no-cache \
  --tag "ab2d_worker:${CMS_ENV}-latest" .

# Get or create api repo

API_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
  --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
  --output text)
if [ -z "${API_ECR_REPO_URI}" ]; then
  aws --region "${REGION}" ecr create-repository \
    --repository-name "ab2d_api"
  API_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_api'].repositoryUri" \
    --output text)
fi

# Get ecr repo aws account

ECR_REPO_AWS_ACCOUNT=$(aws --region "${REGION}" sts get-caller-identity \
  --query Account \
  --output text)

# Apply ecr repo policy to the "ab2d_api" repo

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ECR_REPO_ENV
aws --region "${REGION}" ecr set-repository-policy \
  --repository-name ab2d_api \
  --policy-text file://ab2d-ecr-policy.json

# Tag and push two copies (image version and latest version) of API docker image to ECR
# - image version keeps track of the master commit number (e.g. ab2d-dev-latest-3d0905b)
# - latest version is the same image but without the commit number (e.g. ab2d-dev-latest)
# - NOTE: the latest version is used by the ecs task definition so that it can remain static for zero downtime

docker tag "ab2d_api:${IMAGE_VERSION}" "${API_ECR_REPO_URI}:${IMAGE_VERSION}"
docker push "${API_ECR_REPO_URI}:${IMAGE_VERSION}"
docker tag "ab2d_api:${CMS_ENV}-latest" "${API_ECR_REPO_URI}:${CMS_ENV}-latest"
docker push "${API_ECR_REPO_URI}:${CMS_ENV}-latest"

# Get or create api repo

WORKER_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
  --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
  --output text)
if [ -z "${WORKER_ECR_REPO_URI}" ]; then
  aws --region "${REGION}" ecr create-repository \
    --repository-name "ab2d_worker"
  WORKER_ECR_REPO_URI=$(aws --region "${REGION}" ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_worker'].repositoryUri" \
    --output text)
fi

# Apply ecr repo policy to the "ab2d_worker" repo

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ECR_REPO_ENV
aws --region "${REGION}" ecr set-repository-policy \
  --repository-name ab2d_worker \
  --policy-text file://ab2d-ecr-policy.json

# Tag and push two copies (image version and latest version) of worker docker image to ECR
# - image version keeps track of the master commit number (e.g. ab2d-dev-latest-3d0905b)
# - latest version is the same image but without the commit number (e.g. ab2d-dev-latest)
# - NOTE: the latest version is used by the ecs task definition so that it can remain static for zero downtime

docker tag "ab2d_worker:${IMAGE_VERSION}" "${WORKER_ECR_REPO_URI}:${IMAGE_VERSION}"
docker push "${WORKER_ECR_REPO_URI}:${IMAGE_VERSION}"
docker tag "ab2d_worker:${CMS_ENV}-latest" "${WORKER_ECR_REPO_URI}:${CMS_ENV}-latest"
docker push "${WORKER_ECR_REPO_URI}:${CMS_ENV}-latest"

# Get list of untagged images in the api repository

IMAGES_TO_DELETE=$(aws --region "${REGION}" ecr list-images \
  --repository-name ab2d_api \
  --filter "tagStatus=UNTAGGED" \
  --query 'imageIds[*]' \
  --output json)

# Delete untagged images in the api repository

aws --region "${REGION}" ecr batch-delete-image \
  --repository-name ab2d_api \
  --image-ids "$IMAGES_TO_DELETE" \
  || true

# Get old 'latest-commit' api tag

API_OLD_LATEST_COMMIT_TAG=$(aws --region "${REGION}" ecr describe-images \
  --repository-name ab2d_api \
  --query "imageDetails[*].{imageTag:imageTags[0],imagePushedAt:imagePushedAt}" \
  --output json \
  | jq 'sort_by(.imagePushedAt) | reverse' \
  | jq ".[] | select(.imageTag | startswith(\"${CMS_ENV}-latest\"))" \
  | jq --slurp '.' \
  | jq 'del(.[0,1])' \
  | jq '.[] | select(.imageTag)' \
  | jq '.imageTag' \
  | tr -d '"' \
  | head -1)

if [ -n "${API_OLD_LATEST_COMMIT_TAG}" ]; then
    
  # Rename 'latest-commit' api tag

  RENAME_API_OLD_LATEST_COMMIT_TAG=$(echo "${API_OLD_LATEST_COMMIT_TAG/-latest-/-}")

  # Get manifest of tag to rename
  
  MANIFEST=$(aws --region "${REGION}" ecr batch-get-image \
    --repository-name ab2d_api \
    --image-ids imageTag="${API_OLD_LATEST_COMMIT_TAG}" \
    --query 'images[].imageManifest' \
    --output text)

  # Add renamed api tag

  if [ -n "${MANIFEST}" ]; then
    aws --region "${REGION}" ecr put-image \
      --repository-name ab2d_api \
      --image-tag "${RENAME_API_OLD_LATEST_COMMIT_TAG}" \
      --image-manifest "${MANIFEST}"
  fi
  
  # Remove old api tag

  aws --region "${REGION}" ecr batch-delete-image \
    --repository-name ab2d_api \
    --image-ids imageTag="${API_OLD_LATEST_COMMIT_TAG}"

fi

# Get list of untagged images in the worker repository
  
IMAGES_TO_DELETE=$(aws --region "${REGION}" ecr list-images \
  --repository-name ab2d_worker \
  --filter "tagStatus=UNTAGGED" \
  --query 'imageIds[*]' \
  --output json)

# Delete untagged images in the worker repository

aws --region "${REGION}" ecr batch-delete-image \
  --repository-name ab2d_worker \
  --image-ids "$IMAGES_TO_DELETE" \
  || true

# Get old 'latest-commit' worker tag

WORKER_OLD_LATEST_COMMIT_TAG=$(aws --region "${REGION}" ecr describe-images \
  --repository-name ab2d_worker \
  --query "imageDetails[*].{imageTag:imageTags[0],imagePushedAt:imagePushedAt}" \
  --output json \
  | jq 'sort_by(.imagePushedAt) | reverse' \
  | jq ".[] | select(.imageTag | startswith(\"${CMS_ENV}-latest\"))" \
  | jq --slurp '.' \
  | jq 'del(.[0,1])' \
  | jq '.[] | select(.imageTag)' \
  | jq '.imageTag' \
  | tr -d '"' \
  | head -1)

if [ -n "${WORKER_OLD_LATEST_COMMIT_TAG}" ]; then
    
  # Rename 'latest-commit' worker tag

  RENAME_WORKER_OLD_LATEST_COMMIT_TAG=$(echo "${WORKER_OLD_LATEST_COMMIT_TAG/-latest-/-}")

  # Get manifest of tag to rename
  
  MANIFEST=$(aws --region "${REGION}" ecr batch-get-image \
    --repository-name ab2d_worker \
    --image-ids imageTag="${WORKER_OLD_LATEST_COMMIT_TAG}" \
    --query 'images[].imageManifest' \
    --output text)

  # Add renamed worker tag

  if [ -n "${MANIFEST}" ]; then
    aws --region "${REGION}" ecr put-image \
      --repository-name ab2d_worker \
      --image-tag "${RENAME_WORKER_OLD_LATEST_COMMIT_TAG}" \
      --image-manifest "${MANIFEST}"
  fi
  
  # Remove old worker tag

  aws --region "${REGION}" ecr batch-delete-image \
    --repository-name ab2d_worker \
    --image-ids imageTag="${WORKER_OLD_LATEST_COMMIT_TAG}"

fi
  
# Verify that the image versions of API and Worker are the same

IMAGE_VERSION_API=$(aws --region "${REGION}" ecr describe-images \
  --repository-name ab2d_api \
  --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]')

IMAGE_VERSION_WORKER=$(aws --region "${REGION}" ecr describe-images \
  --repository-name ab2d_worker \
  --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]')

if [ "${IMAGE_VERSION_API}" != "${IMAGE_VERSION_WORKER}" ]; then
  echo "ERROR: The ECR image versions for ab2d_api and ab2d_worker must be the same!"
  exit 0
else
  echo "The ECR image versions for ab2d_api and ab2d_worker were verified to be the same..."
fi

echo "Using master branch commit number '${COMMIT_NUMBER}' for ab2d_api and ab2d_worker..."
    
#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  get_temporary_aws_credentials "${CMS_ENV_AWS_ACCOUNT_NUMBER}"
else
  export AWS_PROFILE="${CMS_ENV}"
fi

# Reset to the target environment

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

#
# Get current known good ECS task definitions
#

CLUSTER_ARNS=$(aws --region "${REGION}" ecs list-clusters \
  --query 'clusterArns' \
  --output text \
  | grep "/${CMS_ENV}-api" \
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

api_task_count() { aws --region "${REGION}" ecs list-tasks --cluster "${CMS_ENV}-api" | grep "\:task\/"|wc -l|tr -d ' '; }
worker_task_count() { aws --region "${REGION}" ecs list-tasks --cluster "${CMS_ENV}-worker" | grep "\:task\/"|wc -l|tr -d ' '; }

# Get old api task count (if exists)

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_TASK_COUNT, since there are no existing clusters"
else
  OLD_API_TASK_COUNT=$(api_task_count)
  OLD_WORKER_TASK_COUNT=$(worker_task_count)
fi

# Set expected api and worker task counts

if [ -z "${CLUSTER_ARNS}" ]; then
  EXPECTED_API_COUNT="${EC2_DESIRED_INSTANCE_COUNT_API}"
  EXPECTED_WORKER_COUNT="${EC2_DESIRED_INSTANCE_COUNT_WORKER}"
else
  let EXPECTED_API_COUNT="$OLD_API_TASK_COUNT+$EC2_DESIRED_INSTANCE_COUNT_API"
  let EXPECTED_WORKER_COUNT="$OLD_WORKER_TASK_COUNT+$EC2_DESIRED_INSTANCE_COUNT_WORKER"
fi

#
# Ensure Old Autoscaling Groups and containers are around to service requests
#

echo "Ensure Old Autoscaling Groups and containers are around to service requests..."

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_ASG, since there are no existing clusters"
else
  OLD_API_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}' | grep $CMS_ENV-api | tr -d '"')
  OLD_WORKER_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}' | grep $CMS_ENV-worker | tr -d '"')
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  terraform state rm module.api.aws_autoscaling_group.asg
  terraform state rm module.api.aws_launch_configuration.launch_config
  terraform state rm module.worker.aws_autoscaling_group.asg
  terraform state rm module.worker.aws_launch_configuration.launch_config
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  OLD_API_CONTAINER_INSTANCES=$(aws --region "${REGION}" ecs list-container-instances \
    --cluster "${CMS_ENV}-api" \
    | grep container-instance)
  OLD_WORKER_CONTAINER_INSTANCES=$(aws --region "${REGION}" ecs list-container-instances \
    --cluster "${CMS_ENV}-worker" \
    | grep container-instance)
fi

#
# Deploy API and Worker
#

echo "Deploy API and Worker..."

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

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

# Get database secret manager ARNs

DATABASE_HOST_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PORT_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_USER_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_user/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_PASSWORD_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_password/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

DATABASE_NAME_SECRET_ARN=$(aws --region "${REGION}" secretsmanager describe-secret \
  --secret-id "ab2d/${CMS_ENV}/module/db/database_name/${DATABASE_SECRET_DATETIME}" \
  --query "ARN" \
  --output text)

# Change to the target terraform environment

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

# Get the BFD keystore file name

BFD_KEYSTORE_FILE_NAME=$(echo $BFD_KEYSTORE_LOCATION | cut -d"/" -f 6)

# Get load balancer listener protocol, port, and certificate

if [ "$CMS_ENV" == "ab2d-sbx-sandbox" ]; then
  ALB_LISTENER_PORT=443
  ALB_LISTENER_PROTOCOL="HTTPS"
  ALB_LISTENER_CERTIFICATE_ARN=$(aws --region "${REGION}" acm list-certificates \
    --query "CertificateSummaryList[?DomainName=='sandbox.ab2d.cms.gov'].CertificateArn" \
    --output text)
  ALB_SECURITY_GROUP_IP_RANGE="0.0.0.0/0"
elif [ "$CMS_ENV" == "ab2d-dev" ]; then
  ALB_LISTENER_PORT=443
  ALB_LISTENER_PROTOCOL="HTTPS"
  ALB_LISTENER_CERTIFICATE_ARN=$(aws --region "${REGION}" acm list-certificates \
    --query "CertificateSummaryList[?DomainName=='dev.ab2d.cms.gov'].CertificateArn" \
    --output text)
  ALB_SECURITY_GROUP_IP_RANGE="${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}"
else
  echo "*** TO DO ***: need to configure IMPL and Prod"
fi

# Run automation for API and worker

terraform apply \
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
  --target module.api \
  --auto-approve

terraform apply \
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
  --target module.worker \
  --auto-approve

#
# Deploy CloudWatch
#

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

echo "Deploy CloudWatch..."

terraform apply \
  --target module.cloudwatch \
  --var "ami_id=$AMI_ID" \
  --var "ecs_task_definition_host_port=$ALB_LISTENER_PORT" \
  --var "host_port=$ALB_LISTENER_PORT" \
  --var "alb_listener_protocol=$ALB_LISTENER_PROTOCOL" \
  --var "alb_listener_certificate_arn=$ALB_LISTENER_CERTIFICATE_ARN" \
  --var "alb_internal=$ALB_INTERNAL" \
  --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  --auto-approve

#
# Deploy AWS WAF
#

terraform apply \
  --var "alb_internal=$ALB_INTERNAL" \
  --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  --target module.waf \
  --auto-approve

#
# Apply AWS Shield standard to the application load balancer
#

# Note that no change is actually made since AWS shield standard is automatically applied to
# the application load balancer. This section may be needed later if AWS Shield Advanced is
# applied instead.

terraform apply \
  --target module.shield \
  --auto-approve

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
    RETRIES_API=$(expr $RETRIES_API + 1)
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
    RETRIES_WORKER=$(expr $RETRIES_WORKER + 1)
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

# Drain old container instances

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping draining old container instances, since there are no existing clusters"
else
  OLD_API_INSTANCE_LIST=$(echo $OLD_API_CONTAINER_INSTANCES | tr -d ' ' | tr "\n" " " | tr -d "," | tr '""' ' ' | tr -d '"')
  OLD_WORKER_INSTANCE_LIST=$(echo $OLD_WORKER_CONTAINER_INSTANCES | tr -d ' ' | tr "\n" " " | tr -d "," | tr '""' ' ' | tr -d '"')
  aws --region "${REGION}" ecs update-container-instances-state \
    --cluster "${CMS_ENV}-api" \
    --status DRAINING \
    --container-instances $OLD_API_INSTANCE_LIST
  aws --region "${REGION}" ecs update-container-instances-state \
    --cluster "${CMS_ENV}-worker" \
    --status DRAINING \
    --container-instances $OLD_WORKER_INSTANCE_LIST
  echo "Allowing all instances to drain for 60 seconds before proceeding..."
  sleep 60
fi

# Remove old Autoscaling groups

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing old autoscaling groups, since there are no existing clusters"
else
  OLD_API_ASG=$(echo $OLD_API_ASG | awk -F"/" '{print $2}')
  OLD_WORKER_ASG=$(echo $OLD_WORKER_ASG | awk -F"/" '{print $2}')
  aws --region "${REGION}" autoscaling delete-auto-scaling-group \
    --auto-scaling-group-name $OLD_API_ASG \
    --force-delete || true
  aws --region "${REGION}" autoscaling delete-auto-scaling-group \
    --auto-scaling-group-name $OLD_WORKER_ASG \
    --force-delete || true
  sleep 60
fi

# Wait for old Autoscaling groups to terminate

RETRIES_ASG=0
ASG_NOT_IN_SERVICE=$(aws --region "${REGION}" autoscaling describe-auto-scaling-groups \
  --query "AutoScalingGroups[*].Instances[?LifecycleState != 'InService'].LifecycleState" \
  --output text)
while [ -n "${ASG_NOT_IN_SERVICE}" ]; do
  echo "Waiting for old autoscaling groups to terminate..."
  if [ "$RETRIES_ASG" != "15" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    ASG_NOT_IN_SERVICE=$(aws --region "${REGION}" autoscaling describe-auto-scaling-groups \
      --query "AutoScalingGroups[*].Instances[?LifecycleState != 'InService'].LifecycleState" \
      --output text)
    RETRIES_ASG=$(expr $RETRIES_ASG + 1)
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
  LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${REGION}" autoscaling describe-launch-configurations \
    --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
    | jq '. | length')
  
  while [ "$LAUNCH_CONFIGURATION_ACTUAL_COUNT" -gt "$LAUNCH_CONFIGURATION_EXPECTED_COUNT" ]; do
  
    OLD_LAUNCH_CONFIGURATION=$(aws --region "${REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      --output text \
      | sort -k2 \
      | head -n1 \
      | awk '{print $1}')
  
    aws --region "${REGION}" autoscaling delete-launch-configuration \
      --launch-configuration-name "${OLD_LAUNCH_CONFIGURATION}"
  
    sleep 5
    
    LAUNCH_CONFIGURATION_ACTUAL_COUNT=$(aws --region "${REGION}" autoscaling describe-launch-configurations \
      --query "LaunchConfigurations[*].[LaunchConfigurationName,CreatedTime]" \
      | jq '. | length')
  
  done
  
fi
