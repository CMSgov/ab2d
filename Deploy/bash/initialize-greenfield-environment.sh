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

CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER=653916833532
CMS_ECR_REPO_ENV=ab2d-mgmt-east-dev

#
# Define functions
#

get_temporary_aws_credentials ()
{
  # Set AWS account number

  AWS_ACCOUNT_NUMBER="$1"

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

########################################################################################
# Configure Greenfield Development AWS account
########################################################################################

#
# Set AWS development environment
#

get_temporary_aws_credentials "${CMS_DEV_ENV_AWS_ACCOUNT_NUMBER}"

# Initialize and validate terraform for the development environment

echo "********************************************************************"
echo "Initialize and validate terraform for the development environment..."
echo "********************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_DEV_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_DEV_ENV}-automation" \
  -backend-config="key=${CMS_DEV_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create of verify greenfield components for development account
#

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
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
  --target module.management_target \
  --auto-approve

########################################################################################
# Configure Greenfield Sandbox AWS account
########################################################################################

#
# Set AWS sandbox environment
#

get_temporary_aws_credentials "${CMS_SBX_ENV_AWS_ACCOUNT_NUMBER}"

# Initialize and validate terraform for the sandbox environment

echo "****************************************************************"
echo "Initialize and validate terraform for the sandbox environment..."
echo "****************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_SBX_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_SBX_ENV}-automation" \
  -backend-config="key=${CMS_SBX_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create of verify greenfield components for management account
#

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
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
  --target module.management_target \
  --auto-approve

########################################################################################
# Configure Greenfield Implementation AWS account
########################################################################################

#
# Set AWS implementation environment
#

get_temporary_aws_credentials "${CMS_IMPL_ENV_AWS_ACCOUNT_NUMBER}"

# Initialize and validate terraform for the implementation environment

echo "***********************************************************************"
echo "Initialize and validate terraform for the implementation environment..."
echo "***********************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_IMPL_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_IMPL_ENV}-automation" \
  -backend-config="key=${CMS_IMPL_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create of verify greenfield components for management account
#

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
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
  --target module.management_target \
  --auto-approve

########################################################################################
# Configure Greenfield Production AWS account
########################################################################################

#
# Set AWS production environment
#

get_temporary_aws_credentials "${CMS_PROD_ENV_AWS_ACCOUNT_NUMBER}"

# Initialize and validate terraform for the production environment

echo "*******************************************************************"
echo "Initialize and validate terraform for the production environment..."
echo "*******************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_PROD_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_PROD_ENV}-automation" \
  -backend-config="key=${CMS_PROD_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create of verify greenfield components for management account
#

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
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
  --target module.management_target \
  --auto-approve

########################################################################################
# Configure Greenfield Management AWS account
########################################################################################

#
# Set AWS management environment
#

get_temporary_aws_credentials "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}"

# Initialize and validate terraform for the management environment

echo "*******************************************************************"
echo "Initialize and validate terraform for the management environment..."
echo "*******************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ECR_REPO_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ECR_REPO_ENV}-automation" \
  -backend-config="key=${CMS_ECR_REPO_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create of verify greenfield components for management account
#

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
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
  --target module.management_account \
  --auto-approve
