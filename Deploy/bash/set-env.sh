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

# Ask user of chooose an environment

echo ""
echo "--------------------------"
echo "Choose desired AWS account"
echo "--------------------------"
echo ""
PS3='Please enter your choice: '
options=("Mgmt AWS account" "Dev AWS account" "Sbx AWS account" "Impl AWS account" "Prod AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Mgmt AWS account")
	    export AWS_ACCOUNT_NUMBER=653916833532
	    export CMS_ENV=ab2d-mgmt-east-dev
	    SSH_PRIVATE_KEY=ab2d-mgmt-east-dev.pem
	    break
            ;;	
        "Dev AWS account")
	    export AWS_ACCOUNT_NUMBER=349849222861
	    export CMS_ENV=ab2d-dev
	    SSH_PRIVATE_KEY=ab2d-dev.pem
	    break
            ;;
        "Sbx AWS account")
	    export AWS_ACCOUNT_NUMBER=777200079629
	    export CMS_ENV=ab2d-sbx-sandbox
	    SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    break
            ;;
        "Impl AWS account")
	    export AWS_ACCOUNT_NUMBER=330810004472
	    export CMS_ENV=ab2d-east-impl
	    SSH_PRIVATE_KEY=ab2d-east-impl.pem
	    break
            ;;
        "Prod AWS account")
	    export AWS_ACCOUNT_NUMBER=595094747606
	    export CMS_ENV=ab2d-east-prod
	    SSH_PRIVATE_KEY=ab2d-east-prod.pem
	    break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

if [ $REPLY -eq 6 ]; then
  echo ""
  return
fi

#
# Acquire temporary credentials via CloudTamer API
#

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

echo ""

# Set default AWS region

export AWS_DEFAULT_REGION=us-east-1

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
  return
fi

# Summary

echo "*******************************************************************************"
echo "$opt environment variables have been set."
echo "-------------------------------------------------------------------------------"
echo "NOTE: These credentials expire in 1 hour."
echo "*******************************************************************************"
echo ""
