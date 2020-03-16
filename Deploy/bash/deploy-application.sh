#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set default values
#

export DEBUG_LEVEL="WARN"

#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
  CMS_ENV=$(echo $ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --shared-environment=*)
  SHARED_ENVIRONMENT="${i#*=}"
  CMS_SHARED_ENV=$(echo $SHARED_ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --ecr-repo-environment=*)
  ECR_REPO_ENVIRONMENT="${i#*=}"
  CMS_ECR_REPO_ENV=$(echo $ECR_REPO_ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --region=*)
  REGION="${i#*=}"
  shift # past argument=value
  ;;
  --vpc-id=*)
  VPC_ID="${i#*=}"
  shift # past argument=value
  ;;
  --ssh-username=*)
  SSH_USERNAME="${i#*=}"
  shift # past argument=value
  ;;
  --owner=*)
  OWNER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_instance_type_api=*)
  EC2_INSTANCE_TYPE_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_instance_type_worker=*)
  EC2_INSTANCE_TYPE_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_instance_type_other=*)
  EC2_INSTANCE_TYPE_OTHER="${i#*=}"
  EC2_INSTANCE_TYPE_CONTROLLER="${EC2_INSTANCE_TYPE_OTHER}"
  EC2_INSTANCE_TYPE_PACKER="${EC2_INSTANCE_TYPE_OTHER}"
  shift # past argument=value
  ;;
  --ec2_desired_instance_count_api=*)
  EC2_DESIRED_INSTANCE_COUNT_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_minimum_instance_count_api=*)
  EC2_MINIMUM_INSTANCE_COUNT_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_maximum_instance_count_api=*)
  EC2_MAXIMUM_INSTANCE_COUNT_API="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_desired_instance_count_worker=*)
  EC2_DESIRED_INSTANCE_COUNT_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_minimum_instance_count_worker=*)
  EC2_MINIMUM_INSTANCE_COUNT_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --ec2_maximum_instance_count_worker=*)
  EC2_MAXIMUM_INSTANCE_COUNT_WORKER="${i#*=}"
  shift # past argument=value
  ;;
  --database-secret-datetime=*)
  DATABASE_SECRET_DATETIME=$(echo ${i#*=})
  shift # past argument=value
  ;;  
  --debug-level=*)
  DEBUG_LEVEL=$(echo ${i#*=} | tr '[:lower:]' '[:upper:]')
  shift # past argument=value
  ;;
  --build-new-images)
  BUILD_NEW_IMAGES="true"
  shift # past argument=value
  ;;
  --use-existing-images)
  USE_EXISTING_IMAGES="true"
  shift # past argument=value
  ;;
  --auto-approve)
  AUTOAPPROVE="true"
  shift # past argument=value
  ;;
esac
done

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ] || [ -z "${SHARED_ENVIRONMENT}" ] || [ -z "${VPC_ID}" ] || [ -z "${OWNER}" ] || [ -z "${DATABASE_SECRET_DATETIME}" ]; then
  echo "Try running the script like one of these options:"
  echo "./create-base-environment.sh --environment=dev --shared-environment=shared --vpc-id={vpc id} --owner=842420567215 --database-secret-datetime={YYYY-MM-DD-HH-MM-SS}"
  echo "./create-base-environment.sh --environment=dev --vpc-id={vpc id} --owner=842420567215 --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --debug-level={TRACE|DEBUG|INFO|WARN|ERROR}"
  exit 1
fi

#
# Set environment
#

export AWS_PROFILE="${CMS_ENV}"

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

export AWS_PROFILE="${CMS_ENV}"

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
# Get MGMT KMS key id for management environment (if exists)
#

export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

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

# Set AWS profile to the target environment

export AWS_PROFILE="${CMS_ENV}"

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

export AWS_PROFILE="${CMS_ENV}"

echo "Enabling KMS key..."
cd "${START_DIR}/.."
cd python3
./enable-kms-key.py $KMS_KEY_ID

#
# Get and enable KMS key id for management environment
#

export AWS_PROFILE="${CMS_ECR_REPO_ENV}"

echo "Enabling KMS key..."
cd "${START_DIR}/.."
cd python3
./enable-kms-key.py $MGMT_KMS_KEY_ID

#
# Get secrets
#

# Set AWS profile to the target environment

export AWS_PROFILE="${CMS_ENV}"

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
  || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Get network attributes
#

# Set AWS profile to the target environment

export AWS_PROFILE="${CMS_ENV}"

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
