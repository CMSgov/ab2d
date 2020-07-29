#!/bin/bash

set -e #Exit on first error
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

verify_production_deployment ()
{
  echo ""
  read -r -p "$1 [y/N] " response
  case "$response" in
    [yY][eE][sS]|[yY])
      return 0
      ;;
    *)
      return 1
      ;;
  esac
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
options=("Dev AWS account" "Prod AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Dev AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=349849222861
	    export CMS_ENV=ab2d-dev
	    export SSH_PRIVATE_KEY=ab2d-dev.pem
	    export S3_BUCKET=cms-ab2d-website
	    break
            ;;
        "Prod AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=595094747606
	    export CMS_ENV=ab2d-east-prod
	    export SSH_PRIVATE_KEY=ab2d-east-prod.pem
	    export S3_BUCKET=ab2d-east-prod-website
	    break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

if [ $REPLY -eq 3 ]; then
  echo ""
  exit 0
fi

#
# If it is a production deployment, verify that it has been approved
#

if [ "$opt" == "Prod AWS account" ]; then
  verify_production_deployment "Has this version of the website been approved for deployment to production?"
  FIRST_RESPONSE=$?
  if [ $FIRST_RESPONSE -eq 0 ]; then
    verify_production_deployment "Are you sure?"
    SECOND_RESPONSE=$?
    if [ $SECOND_RESPONSE -ne 0 ]; then
      exit 0
    fi
  else
    exit 0
  fi
fi

#
# Set AWS target environment
#

get_temporary_aws_credentials "${CMS_ENV_AWS_ACCOUNT_NUMBER}"

#
# Create static website
#

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
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

# Create or verify website bucket

terraform apply \
  --var "env=${CMS_ENV}" \
  --target module.static_website \
  --auto-approve

# Change to the "website" directory

cd "${START_DIR}"/../../website

# Configure head for Tealium/Google Analytics

if [ "$opt" == "Prod AWS account" ]; then
  sed -i "" 's%cms-ab2d[\/]prod%cms-ab2d/dev%g' _includes/head.html
else
  sed -i "" 's%cms-ab2d[\/]prod%cms-ab2d/dev%g' _includes/head.html
fi

# Build the website

rm -rf _site
bundle install
bundle exec jekyll build

# Create or verify website backup bucket

S3_WEBSITE_BACKUP_BUCKET="${S3_BUCKET}-backup"

S3_WEBSITE_BACKUP_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${S3_WEBSITE_BACKUP_BUCKET}'].Name" \
  --output text)

if [ -z "${S3_WEBSITE_BACKUP_BUCKET_EXISTS}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket ${S3_WEBSITE_BACKUP_BUCKET}
  
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${S3_WEBSITE_BACKUP_BUCKET} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

# Create a backup of existing website

S3_BACKUP_DATETIME=$(date '+%Y-%m-%d-%H%M%S')
aws --region "${AWS_DEFAULT_REGION}" s3 sync \
  s3://${S3_BUCKET} \
  s3://${S3_WEBSITE_BACKUP_BUCKET}/${S3_BACKUP_DATETIME}

# Delete existing website files

aws --region us-east-1 s3 rm s3://${S3_BUCKET} \
  --recursive

# Invalidate the CloudFront distribution in order to clear caches on CloudFront edge nodes

CLOUDFRONT_DISTRIBUTION_ID=$(aws --region us-east-1 cloudfront list-distributions \
  --query "DistributionList.Items[*].Origins.Items[?Id=='S3-${S3_BUCKET}']" \
  --query "DistributionList.Items[*].Id" \
  --output text)

CLOUDFRONT_INVALIDATION_ID=$(aws --region us-east-1 cloudfront create-invalidation \
  --distribution-id "${CLOUDFRONT_DISTRIBUTION_ID}" \
  --paths "/*" \
  | jq '.Invalidation.Id' \
  | tr -d '"')

# Wait for CloudFront distribution invalidation to complete

RETRIES_INVALIDATION=0

CLOUDFRONT_INVALIDATION_STATUS=$(aws --region us-east-1 cloudfront list-invalidations \
  --distribution-id "${CLOUDFRONT_DISTRIBUTION_ID}" \
  --query "InvalidationList.Items[?Id=='${CLOUDFRONT_INVALIDATION_ID}'].Status" \
  --output text)

echo "CloudFront invalidation status is: ${CLOUDFRONT_INVALIDATION_STATUS}"

while [ "${CLOUDFRONT_INVALIDATION_STATUS}" != "Completed" ]; do
  if [ "$RETRIES_INVALIDATION" != "15" ]; then
    echo "Recheck CloudFront invalidiation status in 60 seconds..."
    sleep 60
    RETRIES_INVALIDATION=$(expr $RETRIES_INVALIDATION + 1)
    CLOUDFRONT_INVALIDATION_STATUS=$(aws --region us-east-1 cloudfront list-invalidations \
      --distribution-id "${CLOUDFRONT_DISTRIBUTION_ID}" \
      --query "InvalidationList.Items[?Id=='${CLOUDFRONT_INVALIDATION_ID}'].Status" \
      --output text)
    echo "CloudFront invalidation status is: ${CLOUDFRONT_INVALIDATION_STATUS}"
  else
    echo "ERROR: Max retries reached. Exiting checking of CloudFront invalidation status..."
    exit 1
  fi
done

# Upload latest website

aws --region "${AWS_DEFAULT_REGION}" s3 cp \
  --recursive _site/ s3://${S3_BUCKET}/

# Revert head for Tealium/Google Analytics to dev setting

sed -i "" 's%cms-ab2d[\/]prod%cms-ab2d/dev%g' _includes/head.html
