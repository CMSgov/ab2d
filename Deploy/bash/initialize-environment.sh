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
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
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

  # Reset logging

  echo "Setting terraform debug level to $DEBUG_LEVEL..."
  export TF_LOG=$DEBUG_LEVEL
  export TF_LOG_PATH=/var/log/terraform/tf.log
  rm -f /var/log/terraform/tf.log

  # Import the "get temporary AWS credentials via CloudTamer API" function

  source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

fi

#
# Set remaining variables
#

CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER="${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

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
# Configure terraform
#

# Create or verify S3 automation bucket

S3_AUTOMATION_BUCKET="${TARGET_CMS_ENV}-automation"

S3_AUTOMATION_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${S3_AUTOMATION_BUCKET}'].Name" \
  --output text)

if [ -z "${S3_AUTOMATION_BUCKET_EXISTS}" ]; then

  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${S3_AUTOMATION_BUCKET}" \
    1> /dev/null \
    2> /dev/null

  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${S3_AUTOMATION_BUCKET}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
    1> /dev/null \
    2> /dev/null

fi

# Create or verify S3 environment bucket

S3_ENVIRONMENT_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${TARGET_CMS_ENV}'].Name" \
  --output text)

if [ -z "${S3_ENVIRONMENT_BUCKET_EXISTS}" ]; then

  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${TARGET_CMS_ENV}" \
    1> /dev/null \
    2> /dev/null


  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${TARGET_CMS_ENV}" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
    1> /dev/null \
    2> /dev/null

fi

#
# Create cloudtrail bucket
#

S3_CLOUDTRAIL_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${TARGET_CMS_ENV}-cloudtrail'].Name" \
  --output text)

if [ -z "${S3_CLOUDTRAIL_BUCKET_EXISTS}" ]; then

  # Create cloudtrail bucket

  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket "${TARGET_CMS_ENV}-cloudtrail" \
    1> /dev/null \
    2> /dev/null

  # Block public access

  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket "${TARGET_CMS_ENV}-cloudtrail" \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true \
    1> /dev/null \
    2> /dev/null

  # Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cloudtrail" bucket

  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-acl \
    --bucket "${TARGET_CMS_ENV}-cloudtrail" \
    --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
    --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
    1> /dev/null \
    2> /dev/null

  # Add bucket policy to the "cloudtrail" S3 bucket

  cd "${START_DIR}/.."
  cd "terraform/environments/${TARGET_CMS_ENV}"

  aws --region "${AWS_DEFAULT_REGION}" s3api put-bucket-policy \
    --bucket "${TARGET_CMS_ENV}-cloudtrail" \
    --policy file://ab2d-cloudtrail-bucket-policy.json \
    1> /dev/null \
    2> /dev/null

fi

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
# Create or refresh IAM components for target environment
#

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
    --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
    --target module.iam \
    --auto-approve
else
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
    --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
    --target module.iam \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

#
# Create or verify KMS components
#

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
    --target module.kms \
    --auto-approve
else
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
    --target module.kms \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

#
# Get secrets
#

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

#
# Create or refresh BFD Insights IAM components for target environment
#

cd "${START_DIR}/.."
cd "terraform/environments/${TARGET_CMS_ENV}"

if [ "${CLOUD_TAMER_PARAM}" == "true" ]; then
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
    --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
    --var "ab2d_bfd_insights_s3_bucket=${AB2D_BFD_INSIGHTS_S3_BUCKET}" \
    --var "ab2d_bfd_kms_arn=${AB2D_BFD_KMS_ARN}" \
    --target module.iam_bfd_insights \
    --auto-approve
else
  terraform apply \
    --var "env=${TARGET_CMS_ENV}" \
    --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
    --var "aws_account_number=${TARGET_AWS_ACCOUNT_NUMBER}" \
    --var "ab2d_bfd_insights_s3_bucket=${AB2D_BFD_INSIGHTS_S3_BUCKET}" \
    --var "ab2d_bfd_kms_arn=${AB2D_BFD_KMS_ARN}" \
    --target module.iam_bfd_insights \
    --auto-approve \
    1> /dev/null \
    2> /dev/null
fi

#
# Configure networking
#

# Get VPC ID

VPC_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=${TARGET_CMS_ENV}" \
  --query "Vpcs[*].VpcId" \
  --output text)

# Enable DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-vpc-attribute \
  --vpc-id "${VPC_ID}" \
  --attribute enableDnsHostnames \
  --query "EnableDnsHostnames.Value" \
  --output text)

if [ "${VPC_ENABLE_DNS_HOSTNAMES}" == "False" ]; then
  echo "Enabling DNS hostnames on VPC..."
  aws ec2 modify-vpc-attribute \
    --vpc-id "${VPC_ID}" \
    --enable-dns-hostnames \
    1> /dev/null \
    2> /dev/null
fi
