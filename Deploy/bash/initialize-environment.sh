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
# Set variables
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
# Get secrets
#

# Get target KMS key id

KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
  --output text)

# Change to the "python3" directory

cd "${START_DIR}/.."
cd python3

# Get AB2D_BFD_INSIGHTS_S3_BUCKET secret

AB2D_BFD_INSIGHTS_S3_BUCKET=$(./get-database-secret.py $CMS_ENV ab2d_bfd_insights_s3_bucket $DATABASE_SECRET_DATETIME)
if [ -z "${AB2D_BFD_INSIGHTS_S3_BUCKET}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# Get AB2D_BFD_KMS_ARN secret

AB2D_BFD_KMS_ARN=$(./get-database-secret.py $CMS_ENV ab2d_bfd_kms_arn $DATABASE_SECRET_DATETIME)
if [ -z "${AB2D_BFD_KMS_ARN}" ]; then
  echo "**********************************************************************************"
  echo "ERROR: The environment variable could not be retrieved."
  echo ""
  echo "Did you run the 'initialize-greenfield-environment.sh' script to set the variable?"
  echo "**********************************************************************************"
  exit 1
fi

# If any databse secret produced an error, exit the script

if [ "${AB2D_BFD_INSIGHTS_S3_BUCKET}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${AB2D_BFD_KMS_ARN}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Create or refresh IAM components for target environment
#

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "ab2d_bfd_insights_s3_bucket=${AB2D_BFD_INSIGHTS_S3_BUCKET}" \
  --var "ab2d_bfd_kms_arn=${AB2D_BFD_KMS_ARN}" \
  --target module.iam \
  --auto-approve

#
# Create or verify KMS components
#

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --target module.kms \
  --auto-approve

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
