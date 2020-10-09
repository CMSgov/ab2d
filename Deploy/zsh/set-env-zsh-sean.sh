#!/bin/zsh

# Check for dot source and parameters

[[ $ZSH_EVAL_CONTEXT =~ :file$ ]] || { echo "Run with dot source: . $0"; exit; }

[[ $# -eq 0 ]] && { echo "No arguments supplied. Use dev, sbc, impl, prod, or mgmt as parameter."; return; }

# Set account numbers

[[ $1 == 'dev' ]] && AWS_ACCT='349849222861'
[[ $1 == 'sbx' ]] && AWS_ACCT='777200079629'
[[ $1 == 'impl' ]] && AWS_ACCT='330810004472'
[[ $1 == 'prod' ]] && AWS_ACCT='595094747606'
[[ $1 == 'mgmt' ]] && AWS_ACCT='653916833532'

# # Get creds from cred.txt

# creds=()
# while IFS= read -r line; do
#       creds+=("$line")
# done < creds.txt
# USER=$(echo ${creds[1]})
# PASSWORD=$(echo ${creds[2]})

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

# Get Tokens

echo "Grabbing AWS CLI Tokens for $1 ... this might take a few seconds"
TOKEN=$(curl -s -q -X POST 'https://cloudtamer.cms.gov/api/v3/token' --data "{\"username\":\"${CLOUDTAMER_USER_NAME}\",\"password\":\"${CLOUDTAMER_PASSWORD}\",\"idms\":2}" | jq --raw-output ".data.access.token")
JSON=$(curl -s -q -X POST 'https://cloudtamer.cms.gov/api/v3/temporary-credentials' --header "Authorization: Bearer ${TOKEN}" --data "{\"account_number\":\"${AWS_ACCT}\",\"iam_role_name\":\"ab2d-spe-developer\"}" | jq --raw-output ".data")

# Export Tokens

export AWS_ACCESS_KEY_ID=$(echo $JSON | jq --raw-output ".access_key")
export AWS_SECRET_ACCESS_KEY=$(echo $JSON | jq --raw-output ".secret_access_key")
export AWS_SESSION_TOKEN=$(echo $JSON | jq --raw-output ".session_token")

# Get Identity

echo "Getting current identity..."
aws sts get-caller-identity
[[ $? != 0 ]] && echo -e "\nError getting caller identity. Check CloudTamer username/password."
