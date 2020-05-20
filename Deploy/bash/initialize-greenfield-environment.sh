#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set AWS account environment variables
#

# Set environment variables for development AWS account

CMS_DEV_ENV_AWS_ACCOUNT_NUMBER=349849222861
CMS_DEV_ENV=ab2d-dev

# Set environment variables for sandbox AWS account

CMS_SBX_ENV_AWS_ACCOUNT_NUMBER=777200079629
CMS_SBX_ENV=ab2d-sbx-sandbox

# Set environment variables for implementation AWS account

CMS_IMPL_ENV_AWS_ACCOUNT_NUMBER=330810004472
CMS_IMPL_ENV=ab2d-east-impl

# Set environment variables for production AWS account

CMS_PROD_ENV_AWS_ACCOUNT_NUMBER=595094747606
CMS_PROD_ENV=ab2d-east-prod

# Set environment variables for management AWS account

CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER=653916833532
CMS_MGMT_ENV=ab2d-mgmt-east-dev

#
# Define functions
#

# Define get temporary AWS credentials via CloudTamer API function

get_temporary_aws_credentials_via_cloudtamer_api ()
{
  # Set parameters

  AWS_ACCOUNT_NUMBER="$1"
  CMS_ENV="$2"

  # Verify that CloudTamer user name and password environment variables are set

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

  # Get bearer token

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

  if [ "${BEARER_TOKEN}" == "null" ]; then
    echo "**********************************************************************************************"
    echo "ERROR: Retrieval of bearer token failed."
    echo ""
    echo "Do you need to update your "CLOUDTAMER_PASSWORD" environment variable?"
    echo ""
    echo "Have you been locked out due to the failed password attempts?"
    echo ""
    echo "If you have gotten locked out due to failed password attempts, do the following:"
    echo "1. Go to this site:"
    echo "   https://jiraent.cms.gov/servicedesk/customer/portal/13"
    echo "2. Select 'CMS Cloud Access Request'"
    echo "3. Configure page as follows:"
    echo "   - Summary: CloudTamer & CloudVPN account password reset for {your eua id}"
    echo "   - CMS Business Unit: OEDA"
    echo "   - Project Name: Project 058 BCDA"
    echo "   - Types of Access/Resets: Cisco AnyConnect and AWS Console Password Resets [not MFA]"
    echo "   - Approvers: Stephen Walter"
    echo "   - Description"
    echo "     I am locked out of VPN access due to failed password attempts."
    echo "     Can you reset my CloudTamer & CloudVPN account password?"
    echo "     EUA: {your eua id}"
    echo "     email: {your email}"
    echo "     cell phone: {your cell phone number}"
    echo "4. After you submit your ticket, call the following number and give them your ticket number."
    echo "   888-533-4777"
    echo "**********************************************************************************************"
    echo ""
    exit 1
  fi

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
    --data-raw "{\"account_number\":\"${AWS_ACCOUNT_NUMBER}\",\"iam_role_name\":\"ab2d-spe-developer\"}" \
    | jq --raw-output ".data")

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
    exit 1
  fi
}

# Define configure greenfield environment function

configure_greenfield_environment ()
{

  # Set parameters
    
  AWS_ACCOUNT_NUMBER="$1"   # Options: 349849222861|777200079629|330810004472|595094747606|653916833532
  CMS_ENV="$2"              # Options: ab2d-dev|ab2d-sbx-sandbox|ab2d-east-impl|ab2d-east-prod|ab2d-mgmt-east-dev
  MODULE="$3"               # Options: management_target|management_account
    
  # Get AWS credentials for target environment
  
  get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"

  # Initialize and validate terraform for the target environment

  echo "******************************************************************************"
  echo "Initialize and validate terraform for the ${CMS_ENV} environment..."
  echo "******************************************************************************"
  
  cd "${START_DIR}/.."
  cd terraform/environments/$CMS_ENV
  
  rm -f *.tfvars
  
  terraform init \
    -backend-config="bucket=${CMS_ENV}-automation" \
    -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
    -backend-config="region=${AWS_DEFAULT_REGION}" \
    -backend-config="encrypt=true"
  
  terraform validate

  # Get the policies that are attached to the "ab2d-spe-developer" role
  # - the "ab2d-spe-developer" role is controlled by GDIT automation
  # - any modifications to the "ab2d-spe-developer" role are wiped out by GDIT automation
  # - therefore, we are creating our own role that has the same policies but that also allows us
  #   to set trust relationships
  
  AB2D_SPE_DEVELOPER_POLICIES=$(aws --region us-east-1 iam list-attached-role-policies \
    --role-name ab2d-spe-developer \
    --query "AttachedPolicies[*].PolicyArn" \
    | tr -d '[:space:]\n')

  # Create or verify greenfield components
  
  terraform apply \
    --var "mgmt_aws_account_number=${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" \
    --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
    --target "module.${MODULE}" \
    --auto-approve
}

#
# Configure Greenfield AWS accounts
#

# Configure target develoment AWS account

configure_greenfield_environment \
  "${CMS_DEV_ENV_AWS_ACCOUNT_NUMBER}" \
  "${CMS_DEV_ENV}" \
  "management_target"

# Configure target sandbox AWS account

configure_greenfield_environment \
  "${CMS_SBX_ENV_AWS_ACCOUNT_NUMBER}" \
  "${CMS_SBX_ENV}" \
  "management_target"

# Configure target implementation AWS account

configure_greenfield_environment \
  "${CMS_IMPL_ENV_AWS_ACCOUNT_NUMBER}" \
  "${CMS_IMPL_ENV}" \
  "management_target"

# Configure target production AWS account

configure_greenfield_environment \
  "${CMS_PROD_ENV_AWS_ACCOUNT_NUMBER}" \
  "${CMS_PROD_ENV}" \
  "management_target"

# Configure management AWS account that manages the other accounts

configure_greenfield_environment \
  "${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" \
  "${CMS_MGMT_ENV}" \
  "management_account"
