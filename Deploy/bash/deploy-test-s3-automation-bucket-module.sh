#!/bin/bash

set -e # Turn on exit on error
set -x # Turn on verbosity

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
    || [ -z "${AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${JENKINS_AGENT_SEC_GROUP_ID}" ] \
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

AWS_ACCOUNT_NUMBER="${AWS_ACCOUNT_NUMBER_PARAM}"

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
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
else
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${CMS_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
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

# #
# # Create or verify S3 automation bucket
# #

# # Create or verify S3 automation bucket

# PROD_TEST_S3_AUTOMATION_BUCKET=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
#   --query "Buckets[?Name == 'ab2d-east-prod-test-automation'].Name" \
#   --output text)

# if [ -z "${PROD_TEST_S3_AUTOMATION_BUCKET}" ]; then
#   # Create S3 automation bucket
#   aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
#     --bucket "${CMS_ENV_PARAM}-automation"
#   PROD_TEST_S3_AUTOMATION_BUCKET="${CMS_ENV_PARAM}-automation"
# else
#   echo "NOTE: The ${PROD_TEST_S3_AUTOMATION_BUCKET} bucket exists."
# fi

# # Block public access on the bucket

# set +e # Turn off exit on error
# aws --region "${AWS_DEFAULT_REGION}" s3api get-bucket-policy \
#   --bucket "${PROD_TEST_S3_AUTOMATION_BUCKET}" \
#   2> /dev/null
# BUCKET_POLICY_STATUS=$?
# set -e # Turn on exit on error

# if [ "${BUCKET_POLICY_STATUS}" != "0" ]; then
#   aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
#     --bucket "${PROD_TEST_S3_AUTOMATION_BUCKET}" \
#     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
# fi

#
# Create or verify S3 automation bucket
#

  # --var "ami_id=$AMI_ID" \
  # --var "current_task_definition_arn=$API_TASK_DEFINITION" \
  # --var "db_host=$DATABASE_HOST" \
  # --var "db_port=$DATABASE_PORT" \
  # --var "db_username=$DATABASE_USER" \
  # --var "db_password=$DATABASE_PASSWORD" \
  # --var "db_name=$DATABASE_NAME" \
  # --var "db_host_secret_arn=$DATABASE_HOST_SECRET_ARN" \
  # --var "db_port_secret_arn=$DATABASE_PORT_SECRET_ARN" \
  # --var "db_user_secret_arn=$DATABASE_USER_SECRET_ARN" \
  # --var "db_password_secret_arn=$DATABASE_PASSWORD_SECRET_ARN" \
  # --var "db_name_secret_arn=$DATABASE_NAME_SECRET_ARN" \
  # --var "deployer_ip_address=$DEPLOYER_IP_ADDRESS" \
  # --var "ecr_repo_aws_account=$ECR_REPO_AWS_ACCOUNT" \
  # --var "image_version=$IMAGE_VERSION" \
  # --var "new_relic_app_name=$NEW_RELIC_APP_NAME" \
  # --var "new_relic_license_key=$NEW_RELIC_LICENSE_KEY" \
  # --var "ecs_task_definition_host_port=$ALB_LISTENER_PORT" \
  # --var "host_port=$ALB_LISTENER_PORT" \
  # --var "alb_listener_protocol=$ALB_LISTENER_PROTOCOL" \
  # --var "alb_listener_certificate_arn=$ALB_LISTENER_CERTIFICATE_ARN" \
  # --var "alb_internal=$ALB_INTERNAL" \
  # --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  # --var "vpn_private_ip_address_cidr_range=${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" \
  # --var "ab2d_keystore_location=${AB2D_KEYSTORE_LOCATION}" \
  # --var "ab2d_keystore_password=${AB2D_KEYSTORE_PASSWORD}" \
  # --var "ab2d_okta_jwt_issuer=${AB2D_OKTA_JWT_ISSUER}" \
  # --var "ab2d_hpms_url=${AB2D_HPMS_URL}" \
  # --var "ab2d_hpms_api_params=${AB2D_HPMS_API_PARAMS}" \
  # --var "ab2d_hpms_auth_key_id=${AB2D_HPMS_AUTH_KEY_ID}" \
  # --var "ab2d_hpms_auth_key_secret=${AB2D_HPMS_AUTH_KEY_SECRET}" \
  # --var "stunnel_latest_version=${STUNNEL_LATEST_VERSION}" \
  # --var "gold_image_name=${GOLD_IMAGE_NAME}" \

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

terraform apply \
  --var "env=${CMS_ENV}" \
  --var "aws_account_number=${AWS_ACCOUNT_NUMBER}" \
  --target module.test_s3_automation_bucket \
  --auto-approve
