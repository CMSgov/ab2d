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
    || [ -z "${REGION_PARAM}" ] \
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

REGION="${REGION_PARAM}"

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

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

# Import the "get temporary AWS credentials via AWS STS assume role" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"

#
# Set AWS target environment
#

if [ "${CLOUD_TAMER}" == "true" ]; then
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
fi

#
# Configure terraform
#

# Create or verify S3 automation bucket

S3_AUTOMATION_BUCKET="${CMS_ENV}-automation"

S3_AUTOMATION_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${S3_AUTOMATION_BUCKET}'].Name" \
  --output text)

if [ -z "${S3_AUTOMATION_BUCKET_EXISTS}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket ${S3_AUTOMATION_BUCKET}
  
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${S3_AUTOMATION_BUCKET} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# Create or verify S3 environment bucket

S3_ENVIRONMENT_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${CMS_ENV}'].Name" \
  --output text)

if [ -z "${S3_ENVIRONMENT_BUCKET_EXISTS}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket ${CMS_ENV}
  
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${CMS_ENV} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true  
fi

#
# Create cloudtrail bucket
#

S3_CLOUDTRAIL_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${CMS_ENV}-cloudtrail'].Name" \
  --output text)

if [ -z "${S3_CLOUDTRAIL_BUCKET_EXISTS}" ]; then

  # Create cloudtrail bucket

  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${CMS_ENV}-cloudtrail"

  # Block public access

  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${CMS_ENV}-cloudtrail" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true

  # Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cloudtrail" bucket

  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-acl \
    --bucket "${CMS_ENV}-cloudtrail" \
    --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
    --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery

  # Add bucket policy to the "cloudtrail" S3 bucket

  cd "${START_DIR}/.."
  cd terraform/environments/$CMS_ENV

  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-policy \
    --bucket "${CMS_ENV}-cloudtrail" \
    --policy file://ab2d-cloudtrail-bucket-policy.json

fi

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
TF_LOG=$DEBUG_LEVEL
TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

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
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create or refresh IAM components for target environment
#

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --target module.iam \
  --auto-approve

#
# Create or verify KMS components
#

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --target module.kms \
  --auto-approve

#
# Create or get secrets
#

# Get target KMS key id

KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

# Get database user secret

DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_USER}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get database password secret

DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_PASSWORD}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get database name secret

DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_NAME}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get bfd url secret

BFD_URL=$(./get-database-secret.py $CMS_ENV bfd_url $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_URL}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get bfd keystore location secret

BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV bfd_keystore_location $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get bfd keystore password secret

BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV bfd_keystore_password $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get hicn hash pepper secret

HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV hicn_hash_pepper $DATABASE_SECRET_DATETIME)
if [ -z "${HICN_HASH_PEPPER}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get hicn hash iter secret

HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV hicn_hash_iter $DATABASE_SECRET_DATETIME)
if [ -z "${HICN_HASH_ITER}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get new relic app name secret

NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV new_relic_app_name $DATABASE_SECRET_DATETIME)
if [ -z "${NEW_RELIC_APP_NAME}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get new relic license key secret

NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV new_relic_license_key $DATABASE_SECRET_DATETIME)
if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get private ip address CIDR range for VPN

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)
if [ -z "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get AB2D keystore location

AB2D_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV ab2d_keystore_location $DATABASE_SECRET_DATETIME)
if [ -z "${AB2D_KEYSTORE_LOCATION}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get AB2D keystore password

AB2D_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV ab2d_keystore_password $DATABASE_SECRET_DATETIME)
if [ -z "${AB2D_KEYSTORE_PASSWORD}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

AB2D_OKTA_JWT_ISSUER=$(./get-database-secret.py $CMS_ENV ab2d_okta_jwt_issuer $DATABASE_SECRET_DATETIME)
if [ -z "${AB2D_OKTA_JWT_ISSUER}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
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
  || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${AB2D_KEYSTORE_LOCATION}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${AB2D_KEYSTORE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${AB2D_OKTA_JWT_ISSUER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Configure networking
#

# Get VPC ID

VPC_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=${CMS_ENV}" \
  --query "Vpcs[*].VpcId" \
  --output text)

# Enable DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpc-attribute \
  --vpc-id $VPC_ID \
  --attribute enableDnsHostnames \
  --query "EnableDnsHostnames.Value" \
  --output text)

if [ "${VPC_ENABLE_DNS_HOSTNAMES}" == "False" ]; then
  echo "Enabling DNS hostnames on VPC..."
  aws ec2 modify-vpc-attribute \
    --vpc-id $VPC_ID \
    --enable-dns-hostnames
fi
