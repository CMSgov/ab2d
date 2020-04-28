#!/bin/bash

# set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set management AWS account
#

CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER=653916833532

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

#
# Retrieve user input
#

# Ask user of chooose an environment

echo ""
echo "--------------------------"
echo "Choose desired AWS account"
echo "--------------------------"
echo ""
PS3='Please enter your choice: '
options=("Dev AWS account" "Sbx AWS account" "Impl AWS account" "Prod AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Dev AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=349849222861
	    export CMS_ENV=ab2d-dev
	    SSH_PRIVATE_KEY=ab2d-dev.pem
	    break
            ;;
        "Sbx AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=777200079629
	    export CMS_ENV=ab2d-sbx-sandbox
	    SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    break
            ;;
        "Impl AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=330810004472
	    export CMS_ENV=ab2d-east-impl
	    SSH_PRIVATE_KEY=ab2d-east-impl.pem
	    break
            ;;
        "Prod AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=595094747606
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

if [ $REPLY -eq 5 ]; then
  echo ""
  exit 0
fi

#
# Set AWS target environment
#

get_temporary_aws_credentials "${CMS_ENV_AWS_ACCOUNT_NUMBER}"

# #
# # Verify that required CMS-created policy is present in the target environment
# #

# CMS_APPROVED_AWS_SERVICES_POLICY=$(aws --region "${REGION}" iam get-policy \
#   --policy-arn "arn:aws:iam::${CMS_ENV_AWS_ACCOUNT_NUMBER}:policy/CMSApprovedAWSServices" \
#   --query "Policy.Arn" \
#   --output text)

# echo $CMS_APPROVED_AWS_SERVICES_POLICY

#
# Configure terraform
#

# Create or verify S3 automation bucket

S3_AUTOMATION_BUCKET="${CMS_ENV}-automation"

S3_AUTOMATION_BUCKET_EXISTS=$(aws --region "${REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${S3_AUTOMATION_BUCKET}'].Name" \
  --output text)

if [ -z "${S3_AUTOMATION_BUCKET_EXISTS}" ]; then
  aws --region "${REGION}" s3api create-bucket \
    --bucket ${S3_AUTOMATION_BUCKET}
  
  aws --region "${REGION}" s3api put-public-access-block \
    --bucket ${S3_AUTOMATION_BUCKET} \
    --region us-east-1 \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true  
fi

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

# Initialize and validate terraform for the target environment

echo "***************************************************************"
echo "Initialize and validate terraform for the target environment..."
echo "***************************************************************"

cd "${START_DIR}/.."
cd terraform/environments/$CMS_ENV

rm -f *.tfvars

terraform init \
  -backend-config="bucket=${CMS_ENV}-automation" \
  -backend-config="key=${CMS_ENV}/terraform/terraform.tfstate" \
  -backend-config="region=${REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create or refresh Ab2dMgmtRole
#

terraform apply \
  --target module.iam \
  --auto-approve

exit 0





#
# Set AWS management environment
#

get_temporary_aws_credentials "${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}"





#
# Set AWS target environment
#

get_temporary_aws_credentials "${CMS_ENV_AWS_ACCOUNT_NUMBER}"

#
# Create of verify base AWS components
#

KEY_NAME=$(aws --region "${REGION}" ec2 describe-key-pairs \
  --filters "Name=key-name,Values=${CMS_ENV}" \
  --query "KeyPairs[*].KeyName" \
  --output text)

if [ -z "${KEY_NAME}" ]; then
  aws --region "${REGION}" ec2 create-key-pair \
    --key-name ${CMS_ENV} \
    --query 'KeyMaterial' \
    --output text \
    > ~/.ssh/${CMS_ENV}.pem
fi
