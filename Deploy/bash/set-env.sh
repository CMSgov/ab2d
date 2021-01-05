#!/bin/bash

# set -e #Exit on first error
# set -x #Be verbose

#
# Get working directory
#

START_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

#
# Capture command invoked by user
#

INVOKED_COMMAND="$(printf %q "$BASH_SOURCE")$((($#)) && printf ' %q' "$@")"

#
# Ensure script is sourced
#

if [ "$0" != "-bash" ]; then
  echo ""
  echo "*********************************************"
  echo "ERROR: This script must be run using 'source'"
  echo "---------------------------------------------"
  echo "EXPECTED FORMAT:"
  echo "source ${INVOKED_COMMAND}"
  echo "*********************************************"
  echo ""
  exit 1
fi

#
# Define functions
#

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

#
# Ask user of chooose an environment
#

echo ""
echo "--------------------------"
echo "Choose desired AWS account"
echo "--------------------------"
echo ""
PS3='Please enter your choice: '
options=("Dev AWS account" "Sbx AWS account" "Impl AWS account" "Prod AWS account" "Prod Validation AWS account" "Mgmt AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Dev AWS account")
	    export AWS_ACCOUNT_NUMBER=349849222861
	    export CMS_ENV=ab2d-dev
	    SSH_PRIVATE_KEY=ab2d-dev.pem
	    CONTROLLER_PRIVATE_KEY=ab2d-dev.pem
	    break
            ;;
        "Sbx AWS account")
	    export AWS_ACCOUNT_NUMBER=777200079629
	    export CMS_ENV=ab2d-sbx-sandbox
	    SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    CONTROLLER_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    break
            ;;
        "Impl AWS account")
	    export AWS_ACCOUNT_NUMBER=330810004472
	    export CMS_ENV=ab2d-east-impl
	    SSH_PRIVATE_KEY=ab2d-east-impl.pem
	    CONTROLLER_PRIVATE_KEY=ab2d-east-impl.pem
	    break
            ;;
        "Prod AWS account")
	    export AWS_ACCOUNT_NUMBER=595094747606
	    export CMS_ENV=ab2d-east-prod
	    SSH_PRIVATE_KEY=ab2d-east-prod.pem
	    CONTROLLER_PRIVATE_KEY=ab2d-east-prod.pem
	    break
            ;;
        "Prod Validation AWS account")
	    export AWS_ACCOUNT_NUMBER=595094747606
	    export CMS_ENV=ab2d-east-prod-test
	    SSH_PRIVATE_KEY=ab2d-east-prod-test.pem
	    CONTROLLER_PRIVATE_KEY=ab2d-east-prod.pem
	    break
            ;;
        "Mgmt AWS account")
	    export AWS_ACCOUNT_NUMBER=653916833532
	    export CMS_ENV=ab2d-mgmt-east-dev
	    SSH_PRIVATE_KEY=ab2d-mgmt-east-dev.pem
	    break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

if [ $REPLY -eq 7 ]; then
  echo ""
  return
fi

#
# Get Temporary AWS credentials via CloudTamer API
#

fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"
