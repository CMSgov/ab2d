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
    || [ -z "${CLOUD_TAMER_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

CMS_ENV="${CMS_ENV_PARAM}"

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

# Accept inspec license

cd ../../../profiles/cms-ars-3.1-moderate-red-hat-enterprise-linux-7-stig-overlay
inspec vendor . --overwrite --chef-license accept-silent

# Get the private IP addresses of API nodes

PREVIOUS_EC2_INSTANCE_IP_ADDRESS=""
EC2_INSTANCE_IP_ADDRESS="START"
EC2_INSTANCE_INDEX=0
while [ "${PREVIOUS_EC2_INSTANCE_IP_ADDRESS}" != "${EC2_INSTANCE_IP_ADDRESS}" ]; do
  PREVIOUS_EC2_INSTANCE_IP_ADDRESS="${EC2_INSTANCE_IP_ADDRESS}"
  EC2_INSTANCE_INDEX=$(expr $EC2_INSTANCE_INDEX + 1)
  EC2_INSTANCE_IP_ADDRESS=$(aws --region us-east-1 ec2 describe-instances \
    --filters "Name=tag:Name,Values=${CMS_ENV}-*" \
    --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
    --output text \
    | sort \
    | head -${EC2_INSTANCE_INDEX} \
    | tail -1)
  if [ "${PREVIOUS_EC2_INSTANCE_IP_ADDRESS}" != "${EC2_INSTANCE_IP_ADDRESS}" ]; then

    # Install Gems
    mkdir gem_dependencies
    bundle install --path ./gem_dependencies/

    # Make sure the profile uses relative path when referencing dependencies
    sed -i 's/.*git:.*/  path: ..\\/inspec-profile-disa_stig-el7/g' inspec.yml
    sed -i 's/.*branch:.*//g' inspec.yml

    # Run Inspec profile
    # Don't fail if error code is 0 or 101
    # 0 - no failures and no skipped tests
    # 101 - skipped tests but no failures
    inspec exec . \
      --sudo \
      --attrs=attributes.yml \
      -i ~/.ssh/ab2d-east-prod.pem \
      -t ssh://ec2-user@$EC2_INSTANCE_IP_ADDRESS \
      --reporter cli junit:rhel-inspec-api-prod-results.xml json:rhel-inspec-api-prod-results.json || if [ $? -eq 0 -o $? -eq 101 ]; then continue; else exit 1; fi
  fi
done
