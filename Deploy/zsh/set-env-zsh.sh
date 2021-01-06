#!/bin/zsh

#
# Ensure script is sourced
#

[[ $ZSH_EVAL_CONTEXT =~ :file$ ]] || { echo "ERROR: Run with source: source $0"; exit; }

#
# Get working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
echo $START_DIR

#
# Define functions
#

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/../bash/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

#
# Ask user of chooose an environment
#

echo ""
echo "--------------------------"
echo "Choose desired AWS account"
echo "--------------------------"
echo ""

echo "1) Dev AWS account"
echo "2) Sbx AWS account"
echo "3) Impl AWS account"
echo "4) Prod AWS account"
echo "5) Mgmt AWS account"
echo "6) Quit"

read "opt?Please enter your choice: "

echo $opt

if [[ "${opt}" == "1" ]]; then
  export AWS_ACCOUNT_NUMBER=349849222861
  export CMS_ENV=ab2d-dev
  SSH_PRIVATE_KEY=ab2d-dev.pem
elif [[ "${opt}" == "2" ]]; then
  export AWS_ACCOUNT_NUMBER=777200079629
  export CMS_ENV=ab2d-sbx-sandbox
  SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
elif [[ "${opt}" == "3" ]]; then
  export AWS_ACCOUNT_NUMBER=330810004472
  export CMS_ENV=ab2d-east-impl
  SSH_PRIVATE_KEY=ab2d-east-impl.pem
elif [[ "${opt}" == "4" ]]; then
  export AWS_ACCOUNT_NUMBER=595094747606
  export CMS_ENV=ab2d-east-prod
  SSH_PRIVATE_KEY=ab2d-east-prod.pem
elif [[ "${opt}" == "5" ]]; then
  export AWS_ACCOUNT_NUMBER=653916833532
  export CMS_ENV=ab2d-mgmt-east-dev
  SSH_PRIVATE_KEY=ab2d-mgmt-east-dev.pem
else
  echo ""
  return
fi

#
# Get Temporary AWS credentials via CloudTamer API
#

fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
