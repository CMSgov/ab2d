#!/bin/bash -l
# Note: that "-l" was added to the first line to make bash a login shell
# - this causes .bash_profile and .bashrc to be sourced which is needed for ruby items

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

# Change to "cms-ars-3.1-moderate-aws-foundations-cis-overlay" repo

cd "${WORKSPACE}/cms-ars-3.1-moderate-aws-foundations-cis-overlay"

# Configure path to Ruby gems and install them

mkdir -p gem_dependencies
bundle config set path "${PWD}/gem_dependencies"
bundle install

# Eliminate unused regions from generate_attributes.rb
# - note that only us-east-1 region is used for AB2D
# - note that approved regions are defined in the "CMSCloudApprovedRegions" policy for Greenfield
# - CMS approved regions: us-east-1, us-west-2, and us-gov-west-1
# - unapproved regions will cause "generate_attributes.rb" to fail

cd "${WORKSPACE}/cis-aws-foundations-baseline"

sed -i.bak "/.*'us-east-2'.*/d" generate_attributes.rb
sed -i.bak "/.*'us-west-1'.*/d" generate_attributes.rb
sed -i.bak "/.*'us-west-2'.*/d" generate_attributes.rb
sed -i.bak "s/'us-east-1',/'us-east-1'/g" generate_attributes.rb

# Change to the AB2D overlay to set up the environment dynamically

cd "${WORKSPACE}/ab2d/Deploy/inspec/ab2d-inspec-aws"

# Run the `generate_attributes.rb` to generate AWS environment specific elements to test

ruby "${WORKSPACE}/cis-aws-foundations-baseline/generate_attributes.rb" >> attributes_aws.yml

# Accept Inspec license

inspec vendor . --overwrite --chef-license accept-silent

# Run Inspec profile
# Don't fail if error code is 0 or 101
# 0 - no failures and no skipped tests
# 101 - skipped tests but no failures

inspec exec . \
  -t aws:// \
  --attrs=attributes_aws.yml \
  --reporter cli junit:aws-inspec-results.xml json:aws-inspec-results.json || if [ $? -eq 0 -o $? -eq 101 ]; then continue; else exit 1; fi