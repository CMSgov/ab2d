#!/bin/bash

#
# Ensure that default AWS region environment variable is set
#

if [ -z $AWS_DEFAULT_REGION ] || [ -z $AWS_ACCOUNT_NUMBER ] || [ -z $FEDERATED_LOGIN_ROLE ]; then
  echo ""
  echo "------------------"
  echo "Enter AWS settings"
  echo "------------------"
fi

if [ -z $AWS_DEFAULT_REGION ]; then
  echo ""
  echo "Enter the default AWS region (e.g. us-east-1):"
  read AWS_DEFAULT_REGION
fi

if [ -z $AWS_ACCOUNT_NUMBER ]; then
  echo ""
  echo "Enter the target AWS Account number (no dashes):"
  read AWS_ACCOUNT_NUMBER
fi

if [ -z $FEDERATED_LOGIN_ROLE ]; then
  echo ""
  echo "Enter your AWS Federated Login role (the role that prefixes your EUA when logged into AWS Console):"
  read FEDERATED_LOGIN_ROLE
fi

#
# Ensure that CloudTamer user name and password environment variables are set
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

#
# Get temporary AWS credentials
#

# Get bearer token for CloudTamer

BEARER_TOKEN=$(curl --location --request POST 'https://cloudtamer.cms.gov/api/v3/token' \
  --header 'Accept: application/json' \
  --header 'Accept-Language: en-US,en;q=0.5' \
  --header 'Content-Type: application/json' \
  --data-raw "{\"username\":\"${CLOUDTAMER_USER_NAME}\",\"password\":\"${CLOUDTAMER_PASSWORD}\",\"idms\":2}" \
  | jq --raw-output ".data.access.token")

# Get json output for temporary AWS credentials

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
  --data-raw "{\"account_number\":\"${AWS_ACCOUNT_NUMBER}\",\"iam_role_name\":\"${FEDERATED_LOGIN_ROLE}\"}" \
  | jq --raw-output ".data")

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
  echo "ERROR: AWS credentials do not exist for the ${AWS_ACCOUNT_NUMBER} AWS account"
  echo "**********************************************************************"
  echo ""
  exit 1
fi

# Summary

echo ""
echo "*******************************************************************************"
echo "${AWS_ACCOUNT_NUMBER} environment variables have been set."
echo "-------------------------------------------------------------------------------"
echo "NOTE: These credentials expire in 1 hour."
echo "*******************************************************************************"
echo ""
