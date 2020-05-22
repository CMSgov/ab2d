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
if  [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${EC2_INSTANCE_TYPE_CONTROLLER_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${SSH_USERNAME_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${CLOUD_TAMER_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set parameters
#

CMS_ENV="${CMS_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

EC2_INSTANCE_TYPE_CONTROLLER="${EC2_INSTANCE_TYPE_CONTROLLER_PARAM}"

REGION="${REGION_PARAM}"

SSH_USERNAME="${SSH_USERNAME_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

# Set whether CloudTamer API should be used

if [ "${CLOUD_TAMER_PARAM}" != "false" ] && [ "${CLOUD_TAMER_PARAM}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER_PARAM parameter must be true or false"
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
elif [ "${CMS_ENV}" == "ab2d-east-prod" ]; then
  CMS_ENV_AWS_ACCOUNT_NUMBER=595094747606
else
  echo "ERROR: 'CMS_ENV' environment is unknown."
  exit 1  
fi

#
# Define functions
#

# Define get temporary AWS credentials via CloudTamer API function

get_temporary_aws_credentials_via_cloudtamer_api ()
{
  # Set parameters

  AWS_ACCOUNT_NUMBER="$1"
  CMS_ENV="$2"

  # Set default AWS region

  export AWS_DEFAULT_REGION="${REGION}"

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

  if [ "${BEARER_TOKEN}" == "null" ]; then
    echo "**********************************************************************************************"
    echo "ERROR: Retrieval of bearer token failed."
    echo ""
    echo "Do you need to update your "CLOUDTAMER_PASSWORD" environment variable?"
    echo ""
    echo "Have you been locked out due to the failed password attempts?"
    echo ""
    echo "If you have gotten locked out due to failed password attempts, do the following:"
    echo "1. Go to this site:"
    echo "   https://jiraent.cms.gov/servicedesk/customer/portal/13"
    echo "2. Select 'CMS Cloud Access Request'"
    echo "3. Configure page as follows:"
    echo "   - Summary: CloudTamer & CloudVPN account password reset for {your eua id}"
    echo "   - CMS Business Unit: OEDA"
    echo "   - Project Name: Project 058 BCDA"
    echo "   - Types of Access/Resets: Cisco AnyConnect and AWS Console Password Resets [not MFA]"
    echo "   - Approvers: Stephen Walter"
    echo "   - Description"
    echo "     I am locked out of VPN access due to failed password attempts."
    echo "     Can you reset my CloudTamer & CloudVPN account password?"
    echo "     EUA: {your eua id}"
    echo "     email: {your email}"
    echo "     cell phone: {your cell phone number}"
    echo "4. After you submit your ticket, call the following number and give them your ticket number."
    echo "   888-533-4777"
    echo "**********************************************************************************************"
    echo ""
    exit 1
  fi

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

# Define get temporary AWS credentials via AWS STS assume role

get_temporary_aws_credentials_via_aws_sts_assume_role ()
{
  # Set AWS account number

  AWS_ACCOUNT_NUMBER="$1"

  # Set session name

  SESSION_NAME="$2"

  # Set default AWS region

  export AWS_DEFAULT_REGION="${REGION}"

  # Get json output for temporary AWS credentials

  echo ""
  echo "-----------------------------"
  echo "Getting temporary credentials"
  echo "-----------------------------"
  echo ""

  JSON_OUTPUT=$(aws --region "${AWS_DEFAULT_REGION}" sts assume-role \
    --role-arn "arn:aws:iam::${AWS_ACCOUNT_NUMBER}:role/Ab2dMgmtRole" \
    --role-session-name "${SESSION_NAME}" \
    | jq --raw-output ".Credentials")

  # Get temporary AWS credentials

  export AWS_ACCESS_KEY_ID=$(echo $JSON_OUTPUT | jq --raw-output ".AccessKeyId")
  export AWS_SECRET_ACCESS_KEY=$(echo $JSON_OUTPUT | jq --raw-output ".SecretAccessKey")

  # Get AWS session token (required for temporary credentials)

  export AWS_SESSION_TOKEN=$(echo $JSON_OUTPUT | jq --raw-output ".SessionToken")

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
  get_temporary_aws_credentials_via_cloudtamer_api "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  get_temporary_aws_credentials_via_aws_sts_assume_role "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
fi

#
# Get VPC ID
#

VPC_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=${CMS_ENV}" \
  --query "Vpcs[*].VpcId" \
  --output text)

#
# Configure terraform
#

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-automation" \
  -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Deploy db
#

echo "Create or update database instance..."

# Change to the "python3" directory

cd "${START_DIR}"
cd python3

# Get database secrets

DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)

# Verify database credentials

if [ -z "${DATABASE_USER}" ] \
    || [ -z "${DATABASE_PASSWORD}" ] \
    || [ -z "${DATABASE_NAME}" ]; then
  echo "***********************************************************************"
  echo "ERROR: Database credentials do not exist for the ${CMS_ENV} AWS account"
  echo "***********************************************************************"
  echo ""
  exit 1
fi

# Get VPN privte ip address range

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)

# Change to the target directory

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

#
# Get network information
#

# Get first public subnet id

SUBNET_PUBLIC_1_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "ERROR: public subnet #1 not found..."
  exit 1
fi

# Get second public subnet id

SUBNET_PUBLIC_2_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then
  echo "ERROR: public subnet #2 not found..."
  exit 1
fi

# Get first private subnet id

SUBNET_PRIVATE_1_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then
  echo "ERROR: private subnet #1 not found..."
  exit 1
fi

# Get second private subnet id

SUBNET_PRIVATE_2_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=${CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then
  echo "ERROR: private subnet #2 not found..."
  exit 1
fi

#
# Create "auto.tfvars" file
#

# Get AMI ID

AMI_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

# Get deployer IP address

DEPLOYER_IP_ADDRESS=$(curl ipinfo.io/ip)

# Get DB endpoint

DB_ENDPOINT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

# Save VPC_ID

echo 'vpc_id = "'$VPC_ID'"' \
  > $CMS_ENV.auto.tfvars

# Save private_subnet_ids

if [ -z "${DB_ENDPOINT}" ]; then
  echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'"]' \
    >> $CMS_ENV.auto.tfvars
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
    >> $CMS_ENV.auto.tfvars
fi

# Set remaining vars

echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'"]' \
  >> $CMS_ENV.auto.tfvars

echo 'ec2_instance_type = "'$EC2_INSTANCE_TYPE_CONTROLLER'"' \
  >> $CMS_ENV.auto.tfvars

echo 'linux_user = "'$SSH_USERNAME'"' \
  >> $CMS_ENV.auto.tfvars

echo 'ami_id = "'$AMI_ID'"' \
  >> $CMS_ENV.auto.tfvars

echo 'deployer_ip_address = "'$DEPLOYER_IP_ADDRESS'"' \
  >> $CMS_ENV.auto.tfvars
echo 'vpn_private_ip_address_cidr_range = "'$VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE'"' \
  >> $CMS_ENV.auto.tfvars

# Create DB instance (if doesn't exist)

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "db_username=${DATABASE_USER}" \
  --var "db_password=${DATABASE_PASSWORD}" \
  --var "db_name=${DATABASE_NAME}" \
  --target module.db --auto-approve

DB_ENDPOINT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV
rm -f generated/.pgpass

# Generate ".pgpass" file

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV
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

cd "${START_DIR}"
cd terraform/environments/$CMS_ENV

echo "Create or update controller..."

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "db_username=${DATABASE_USER}" \
  --var "db_password=${DATABASE_PASSWORD}" \
  --var "db_name=${DATABASE_NAME}" \
  --var "deployer_ip_address=${DEPLOYER_IP_ADDRESS}" \
  --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
  --target module.controller \
  --auto-approve

#
# Deploy EFS
#

# Get files system id (if exists)

EFS_FS_ID=$(aws --region "${AWS_DEFAULT_REGION}" efs describe-file-systems \
  --query="FileSystems[?CreationToken=='${CMS_ENV}-efs'].FileSystemId" \
  --output=text)

# Create file system (if doesn't exist)

if [ -z "${EFS_FS_ID}" ]; then
  echo "Creating EFS..."
  terraform apply \
    --var "env=${CMS_ENV}" \
    --target module.efs \
    --auto-approve
fi

#
# Create database
#

cd "${START_DIR}"

# Get the private ip address of the controller

CONTROLLER_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
  --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
  --output text)

if [ "${CLOUD_TAMER}" == "true" ]; then

  # Determine if the database for the environment exists

  DB_NAME_IF_EXISTS=$(ssh -tt -i "~/.ssh/${CMS_ENV}.pem" \
    "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}" \
    "psql -t --host "${DB_ENDPOINT}" --username "${DATABASE_USER}" --dbname postgres --command='SELECT datname FROM pg_catalog.pg_database'" \
    | grep "${DATABASE_NAME}" \
    | sort \
    | head -n 1 \
    | xargs \
    | tr -d '\r')

  # Create the database for the environment if it doesn't exist

  if [ -n "${CONTROLLER_PRIVATE_IP}" ] && [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DATABASE_NAME}" ]; then
    echo "Creating database..."
    ssh -tt -i "~/.ssh/${CMS_ENV}.pem" \
      "${SSH_USERNAME}@${CONTROLLER_PRIVATE_IP}" \
      "createdb ${DATABASE_NAME} --host ${DB_ENDPOINT} --username ${DATABASE_USER}"
  fi

else # Running from Jenkins agent

  # Determine if the database for the environment exists

  DB_NAME_IF_EXISTS=$(psql -t \
    --host ab2d.cdi8i6wwresy.us-east-1.rds.amazonaws.com \
    --username cmsadmin \
    --dbname postgres \
    --command='SELECT datname FROM pg_catalog.pg_database' \
    | grep prod \
    | sort \
    | head -n 1 \
    | xargs \
    | tr -d '\r')

  # Create the database for the environment if it doesn't exist

  if [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DATABASE_NAME}" ]; then
    echo "Creating database..."
    createdb ${DATABASE_NAME} --host ${DB_ENDPOINT} --username ${DATABASE_USER}
  fi

fi

# Change to the "python3" directory

cd "${START_DIR}"
cd python3

# Create or get database host secret

DATABASE_HOST=$(./get-database-secret.py $CMS_ENV database_host $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_HOST}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_host/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_ENDPOINT}"
  DATABASE_HOST=$(./get-database-secret.py $CMS_ENV database_host $DATABASE_SECRET_DATETIME)
fi

# Create or get database port secret

DB_PORT=$(aws --region "${AWS_DEFAULT_REGION}" rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Port" \
  --output=text)

DATABASE_PORT=$(./get-database-secret.py $CMS_ENV database_port $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_PORT}" ]; then
  aws secretsmanager create-secret \
    --name "ab2d/${CMS_ENV}/module/db/database_port/${DATABASE_SECRET_DATETIME}" \
    --secret-string "${DB_PORT}"
  DATABASE_PORT=$(./get-database-secret.py $CMS_ENV database_port $DATABASE_SECRET_DATETIME)
fi
