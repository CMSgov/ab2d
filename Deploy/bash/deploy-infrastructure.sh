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
if  [ -z "${CLOUD_TAMER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_CONTROLLER_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${TARGET_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${TARGET_CMS_ENV_PARAM}" ]; then
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

#
# Set remaining variables
#

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

EC2_INSTANCE_TYPE_CONTROLLER="${EC2_INSTANCE_TYPE_CONTROLLER_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

TARGET_AWS_ACCOUNT_NUMBER="${TARGET_AWS_ACCOUNT_NUMBER_PARAM}"

TARGET_CMS_ENV="${TARGET_CMS_ENV_PARAM}"

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
# Get VPC ID
#

VPC_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}" \
  --query "Vpcs[*].VpcId" \
  --output text)

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
# Deploy db
#

echo "Create or update database instance..."

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

# Get database secrets

DATABASE_USER=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_user "${DATABASE_SECRET_DATETIME}")
DATABASE_PASSWORD=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_password "${DATABASE_SECRET_DATETIME}")
DATABASE_NAME=$(./get-database-secret.py "${TARGET_CMS_ENV}" database_name "${DATABASE_SECRET_DATETIME}")

# Verify database credentials

if [ -z "${DATABASE_USER}" ] \
    || [ -z "${DATABASE_PASSWORD}" ] \
    || [ -z "${DATABASE_NAME}" ]; then
  echo "***********************************************************************"
  echo "ERROR: Database credentials do not exist for the ${TARGET_CMS_ENV} AWS account"
  echo "***********************************************************************"
  echo ""
  exit 1
fi

# Get VPN private ip address range

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py "${TARGET_CMS_ENV}" vpn_private_ip_address_cidr_range "${DATABASE_SECRET_DATETIME}")

# Change to the target directory

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

#
# Get network information
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
# Create "auto.tfvars" file
#

# Get AMI ID and gold image name

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

# Get deployer IP address

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

# Get DB endpoint

DB_ENDPOINT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

# Save VPC_ID

echo 'vpc_id = "'"${VPC_ID}"'"' \
  > "${TARGET_CMS_ENV}.auto.tfvars"

# Save private_subnet_ids

if [ -z "${DB_ENDPOINT}" ]; then
  echo 'private_subnet_ids = ["'"${SUBNET_PRIVATE_1_ID}"'","'"${SUBNET_PRIVATE_2_ID}"'"]' \
    >> "${TARGET_CMS_ENV}.auto.tfvars"
else
  PRIVATE_SUBNETS_OUTPUT=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
    --filters "Name=tag:Name,Values=*-private-*" \
    --query "Subnets[*].SubnetId" \
    --output text)

  IFS=$'\t' read -ra subnet_array <<< "$PRIVATE_SUBNETS_OUTPUT"
  unset IFS

  COUNT=1
  for i in "${subnet_array[@]}"
  do
    if [ $COUNT == 1 ]; then
      PRIVATE_SUBNETS=\"$i\"
    else
      PRIVATE_SUBNETS=$PRIVATE_SUBNETS,\"$i\"
    fi
    COUNT=$COUNT+1
  done

  echo "private_subnet_ids = [${PRIVATE_SUBNETS}]" \
    >> "${TARGET_CMS_ENV}.auto.tfvars"
fi

# Set remaining vars

{
  echo 'deployment_controller_subnet_ids = ["'"${SUBNET_PUBLIC_1_ID}"'","'"${SUBNET_PUBLIC_2_ID}"'"]'
  echo 'ec2_instance_type = "'"${EC2_INSTANCE_TYPE_CONTROLLER}"'"'
  echo 'linux_user = "'"${SSH_USERNAME}"'"'
  echo 'ami_id = "'"${AMI_ID}"'"'
  echo 'deployer_ip_address = "'"${DEPLOYER_IP_ADDRESS}"'"'
  echo 'vpn_private_ip_address_cidr_range = "'"${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}"'"'
} >> "${TARGET_CMS_ENV}.auto.tfvars"

# Create DB instance (if doesn't exist)

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "db_username=${DATABASE_USER}" \
    --var "db_password=${DATABASE_PASSWORD}" \
    --var "db_name=${DATABASE_NAME}" \
    --target module.db \
    --auto-approve
else
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "db_username=${DATABASE_USER}" \
    --var "db_password=${DATABASE_PASSWORD}" \
    --var "db_name=${DATABASE_NAME}" \
    --target module.db \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

DB_ENDPOINT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"
rm -f generated/.pgpass

# Generate ".pgpass" file

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"
mkdir -p generated

# Add default database

echo "${DB_ENDPOINT}:5432:postgres:${DATABASE_USER}:${DATABASE_PASSWORD}" > generated/.pgpass

# Set the ".pgpass" file

cd generated
chmod 600 .pgpass
export PGPASSFILE="${PWD}/.pgpass"

#
# Deploy controller
#

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

echo "Create or update controller..."

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "db_username=${DATABASE_USER}" \
    --var "db_password=${DATABASE_PASSWORD}" \
    --var "db_name=${DATABASE_NAME}" \
    --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
    --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
    --var "gold_image_name=${GOLD_IMAGE_NAME}" \
    --target module.controller \
    --auto-approve
else
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "db_username=${DATABASE_USER}" \
    --var "db_password=${DATABASE_PASSWORD}" \
    --var "db_name=${DATABASE_NAME}" \
    --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
    --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
    --var "gold_image_name=${GOLD_IMAGE_NAME}" \
    --target module.controller \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

#
# Deploy EFS
#

# Get files system id (if exists)

echo "Create or update EFS..."

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --target module.efs \
    --auto-approve
else
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --target module.efs \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

#
# Create or verify database
#

# Get the private ip address of the controller

CONTROLLER_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
  --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
  --output text)

if [ "${CLOUD_TAMER}" == "true" ]; then

  cd "${START_DIR}/.."
  cd "terraform/environments/${CMS_ENV}/${MODULE}"

  # Get database host and port

  DB_ENDPOINT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
    --query="DBInstances[?DBInstanceIdentifier=='${CMS_ENV}'].Endpoint.Address" \
    --output=text)

  DB_PORT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
    --query="DBInstances[?DBInstanceIdentifier=='${CMS_ENV}'].Endpoint.Port" \
    --output=text)

  rm -f generated/.pgpass

  # Generate ".pgpass" file

  mkdir -p generated

  # Add default database

  echo "${DB_ENDPOINT}:${DB_PORT}:postgres:${AB2D_DB_USER}:${AB2D_DB_PASSWORD}" > generated/.pgpass

  # Set the ".pgpass" file

  chmod 600 generated/.pgpass

  # Upload .pgpass file to controller

  scp -i "${HOME}/.ssh/${PARENT_ENV}.pem" "generated/.pgpass" "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}":~

  # Determine if the database for the environment exists

  DB_NAME_IF_EXISTS=$(ssh -tt -i "${HOME}/.ssh/${PARENT_ENV}.pem" \
    "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}" \
    "psql -t --host ${DB_ENDPOINT} --username ${AB2D_DB_USER} --dbname postgres --command='SELECT datname FROM pg_catalog.pg_database'" \
    | grep "${DB_NAME}" \
    | sort \
    | head -n 1 \
    | xargs \
    | tr -d '\r')

  # Create the database for the environment if it doesn't exist

  if [ -n "${CONTROLLER_PRIVATE_IP}" ] && [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DB_NAME}" ]; then
    echo "Creating database..."
    ssh -tt -i "${HOME}/.ssh/${PARENT_ENV}.pem" \
      "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}" \
      "createdb ${DB_NAME} --host ${DB_ENDPOINT} --username ${AB2D_DB_USER}"
  fi

else # Running from Jenkins agent

  # Set PostgreSQL password

  export PGPASSWORD="${DATABASE_PASSWORD}"

  # Determine if the database for the environment exists

  DB_NAME_IF_EXISTS=$(psql -t \
    --host "${DB_ENDPOINT}" \
    --username "${DATABASE_USER}" \
    --dbname postgres \
    --command='SELECT datname FROM pg_catalog.pg_database' \
    | grep "${DATABASE_NAME}" \
    | sort \
    | head -n 1 \
    | xargs \
    | tr -d '\r')

  # Create the database for the environment if it doesn't exist

  if [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DATABASE_NAME}" ]; then
    echo "Creating database..."
    createdb "${DATABASE_NAME}" --host "${DB_ENDPOINT}" --username "${DATABASE_USER}"
  fi

fi

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

# Get AB2D_BFD_INSIGHTS_S3_BUCKET secret

AB2D_BFD_INSIGHTS_S3_BUCKET=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_bfd_insights_s3_bucket "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_BFD_INSIGHTS_S3_BUCKET}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get AB2D_BFD_KMS_ARN secret

AB2D_BFD_KMS_ARN=$(./get-database-secret.py "${TARGET_CMS_ENV}" ab2d_bfd_kms_arn "${DATABASE_SECRET_DATETIME}")
if [ -z "${AB2D_BFD_KMS_ARN}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# If any database secret produced an error, exit the script

if [ "${AB2D_BFD_INSIGHTS_S3_BUCKET}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${AB2D_BFD_KMS_ARN}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

# Create Kinesis Firehose for lower environments only

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

if [ "${TARGET_CMS_ENV}" != "ab2d-east-prod" ]; then
  if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
    terraform apply \
      --var "env=${TARGET_CMS_ENV}" \
      --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
      --var "kinesis_firehose_bucket=${AB2D_BFD_INSIGHTS_S3_BUCKET}" \
      --var "kinesis_firehose_kms_key_arn=${AB2D_BFD_KMS_ARN}" \
      --target module.kinesis_firehose \
      --auto-approve
  else
    terraform apply \
      --var "env=${TARGET_CMS_ENV}" \
      --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
      --var "kinesis_firehose_bucket=${AB2D_BFD_INSIGHTS_S3_BUCKET}" \
      --var "kinesis_firehose_kms_key_arn=${AB2D_BFD_KMS_ARN}" \
      --target module.kinesis_firehose \
      --auto-approve \
      1> /dev/null \
      2> /dev/null
  fi
fi
