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
	    export SSH_PRIVATE_KEY=ab2d-dev.pem
	    break
            ;;
        "Sbx AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=777200079629
	    export CMS_ENV=ab2d-sbx-sandbox
	    export SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    break
            ;;
        "Impl AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=330810004472
	    export CMS_ENV=ab2d-east-impl
	    export SSH_PRIVATE_KEY=ab2d-east-impl.pem
	    break
            ;;
        "Prod AWS account")
	    export CMS_ENV_AWS_ACCOUNT_NUMBER=595094747606
	    export CMS_ENV=ab2d-east-prod
	    export SSH_PRIVATE_KEY=ab2d-east-prod.pem
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

#
# Configure terraform
#

# Create or verify S3 automation bucket

S3_AUTOMATION_BUCKET="${CMS_ENV}-automation"

S3_AUTOMATION_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${S3_AUTOMATION_BUCKET}'].Name" \
  --output text)

if [ -z "${S3_AUTOMATION_BUCKET_EXISTS}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket ${S3_AUTOMATION_BUCKET}
  
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${S3_AUTOMATION_BUCKET} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true  
fi

# Create or verify S3 environment bucket

S3_ENVIRONMENT_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${CMS_ENV}'].Name" \
  --output text)

if [ -z "${S3_ENVIRONMENT_BUCKET_EXISTS}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket ${CMS_ENV}
  
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${CMS_ENV} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true  
fi

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
TF_LOG=$DEBUG_LEVEL
TF_LOG_PATH=/var/log/terraform/tf.log
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
  -backend-config="region=${AWS_DEFAULT_REGION}" \
  -backend-config="encrypt=true"

terraform validate

#
# Create of verify key pair
#

KEY_NAME=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-key-pairs \
  --filters "Name=key-name,Values=${CMS_ENV}" \
  --query "KeyPairs[*].KeyName" \
  --output text)

if [ -z "${KEY_NAME}" ]; then
  aws --region "${AWS_DEFAULT_REGION}" ec2 create-key-pair \
    --key-name ${CMS_ENV} \
    --query 'KeyMaterial' \
    --output text \
    > ~/.ssh/${CMS_ENV}.pem
fi

#
# Create or refresh IAM components for target environment
#

terraform apply \
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "env=${CMS_ENV}" \
  --target module.iam \
  --auto-approve

#
# Create or verify KMS components
#

terraform apply \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "env=${CMS_ENV}" \
  --target module.kms \
  --auto-approve

#
# Create or get secrets
#

# Change to the "python3" directory

cd "${START_DIR}"
cd python3

# Create or get database user secret

DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_USER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_user $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
fi

# Create or get database password secret

DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
fi

# Create or get database name secret

DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
if [ -z "${DATABASE_NAME}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV database_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)
fi

# Create or get bfd url secret

BFD_URL=$(./get-database-secret.py $CMS_ENV bfd_url $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_URL}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV bfd_url $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  BFD_URL=$(./get-database-secret.py $CMS_ENV bfd_url $DATABASE_SECRET_DATETIME)
fi

# Create or get bfd keystore location secret

BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV bfd_keystore_location $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV bfd_keystore_location $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV bfd_keystore_location $DATABASE_SECRET_DATETIME)
fi

# Create or get bfd keystore password secret

BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV bfd_keystore_password $DATABASE_SECRET_DATETIME)
if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV bfd_keystore_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV bfd_keystore_password $DATABASE_SECRET_DATETIME)
fi

# Create or get hicn hash pepper secret

HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV hicn_hash_pepper $DATABASE_SECRET_DATETIME)
if [ -z "${HICN_HASH_PEPPER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV hicn_hash_pepper $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV hicn_hash_pepper $DATABASE_SECRET_DATETIME)
fi

# Create or get hicn hash iter secret

HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV hicn_hash_iter $DATABASE_SECRET_DATETIME)
if [ -z "${HICN_HASH_ITER}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV hicn_hash_iter $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV hicn_hash_iter $DATABASE_SECRET_DATETIME)
fi

# Create or get new relic app name secret

NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV new_relic_app_name $DATABASE_SECRET_DATETIME)
if [ -z "${NEW_RELIC_APP_NAME}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV new_relic_app_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV new_relic_app_name $DATABASE_SECRET_DATETIME)
fi

# Create or get new relic license key secret

NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV new_relic_license_key $DATABASE_SECRET_DATETIME)
if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV new_relic_license_key $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV new_relic_license_key $DATABASE_SECRET_DATETIME)
fi

# Create or get private ip address CIDR range for VPN

VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)
if [ -z "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" ]; then
  echo "*********************************************************"
  ./create-database-secret.py $CMS_ENV vpn_private_ip_address_cidr_range $KMS_KEY_ID $DATABASE_SECRET_DATETIME
  echo "*********************************************************"
  VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)
fi

# If any databse secret produced an error, exit the script

if [ "${DATABASE_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_URL}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_KEYSTORE_LOCATION}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${BFD_KEYSTORE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${HICN_HASH_PEPPER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${HICN_HASH_ITER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${NEW_RELIC_APP_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${NEW_RELIC_LICENSE_KEY}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get secrets because KMS key is disabled!"
    exit 1
fi

#
# Configure networking
#

# Enable DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws --region "${REGION}" ec2 describe-vpc-attribute \
  --vpc-id $VPC_ID \
  --attribute enableDnsHostnames \
  --query "EnableDnsHostnames.Value" \
  --output text)

if [ "${VPC_ENABLE_DNS_HOSTNAMES}" == "False" ]; then
  echo "Enabling DNS hostnames on VPC..."
  aws ec2 modify-vpc-attribute \
    --vpc-id $VPC_ID \
    --enable-dns-hostnames
fi

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
# Create of verify management account components
#

terraform apply \
  --var "mgmt_aws_account_number=${CMS_ECR_REPO_ENV_AWS_ACCOUNT_NUMBER}" \
  --var "aws_account_number=${CMS_ENV_AWS_ACCOUNT_NUMBER}" \
  --target module.management_account \
  --auto-approve
