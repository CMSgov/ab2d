#!/bin/bash

# set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if  [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set parameters
#

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

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

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

# Define set secrets function

set_secrets ()
{
  # Set parameters
    
  CMS_ENV_SS="$1"   # Options: ab2d-dev|ab2d-sbx-sandbox|ab2d-east-impl|ab2d-east-prod

  # Get target KMS key id

  KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
    --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
    --output text)

  # Change to the "python3" directory
  
  cd "${START_DIR}/.."
  cd python3
  
  # Create or get database user secret
  
  DATABASE_USER=$(./get-database-secret.py $CMS_ENV_GE database_user $DATABASE_SECRET_DATETIME)
  if [ -z "${DATABASE_USER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE database_user $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    DATABASE_USER=$(./get-database-secret.py $CMS_ENV_GE database_user $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get database password secret
  
  DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV_GE database_password $DATABASE_SECRET_DATETIME)
  if [ -z "${DATABASE_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE database_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV_GE database_password $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get database name secret
  
  DATABASE_NAME=$(./get-database-secret.py $CMS_ENV_GE database_name $DATABASE_SECRET_DATETIME)
  if [ -z "${DATABASE_NAME}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE database_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    DATABASE_NAME=$(./get-database-secret.py $CMS_ENV_GE database_name $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get bfd url secret
  
  BFD_URL=$(./get-database-secret.py $CMS_ENV_GE bfd_url $DATABASE_SECRET_DATETIME)
  if [ -z "${BFD_URL}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE bfd_url $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    BFD_URL=$(./get-database-secret.py $CMS_ENV_GE bfd_url $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get bfd keystore location secret
  
  BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV_GE bfd_keystore_location $DATABASE_SECRET_DATETIME)
  if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE bfd_keystore_location $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    BFD_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV_GE bfd_keystore_location $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get bfd keystore password secret
  
  BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV_GE bfd_keystore_password $DATABASE_SECRET_DATETIME)
  if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE bfd_keystore_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV_GE bfd_keystore_password $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get hicn hash pepper secret
  
  HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV_GE hicn_hash_pepper $DATABASE_SECRET_DATETIME)
  if [ -z "${HICN_HASH_PEPPER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE hicn_hash_pepper $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    HICN_HASH_PEPPER=$(./get-database-secret.py $CMS_ENV_GE hicn_hash_pepper $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get hicn hash iter secret
  
  HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV_GE hicn_hash_iter $DATABASE_SECRET_DATETIME)
  if [ -z "${HICN_HASH_ITER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE hicn_hash_iter $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    HICN_HASH_ITER=$(./get-database-secret.py $CMS_ENV_GE hicn_hash_iter $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get new relic app name secret
  
  NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV_GE new_relic_app_name $DATABASE_SECRET_DATETIME)
  if [ -z "${NEW_RELIC_APP_NAME}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE new_relic_app_name $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    NEW_RELIC_APP_NAME=$(./get-database-secret.py $CMS_ENV_GE new_relic_app_name $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get new relic license key secret
  
  NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV_GE new_relic_license_key $DATABASE_SECRET_DATETIME)
  if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE new_relic_license_key $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py $CMS_ENV_GE new_relic_license_key $DATABASE_SECRET_DATETIME)
  fi
  
  # Create or get private ip address CIDR range for VPN
  
  VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV_GE vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)
  if [ -z "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE vpn_private_ip_address_cidr_range $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py $CMS_ENV_GE vpn_private_ip_address_cidr_range $DATABASE_SECRET_DATETIME)
  fi

  # Create or get AB2D keystore location

  AB2D_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV_GE ab2d_keystore_location $DATABASE_SECRET_DATETIME)
  if [ -z "${AB2D_KEYSTORE_LOCATION}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE ab2d_keystore_location $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    AB2D_KEYSTORE_LOCATION=$(./get-database-secret.py $CMS_ENV_GE ab2d_keystore_location $DATABASE_SECRET_DATETIME)
  fi

  # Create or get AB2D keystore password

  AB2D_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV_GE ab2d_keystore_password $DATABASE_SECRET_DATETIME)
  if [ -z "${AB2D_KEYSTORE_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE ab2d_keystore_password $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    AB2D_KEYSTORE_PASSWORD=$(./get-database-secret.py $CMS_ENV_GE ab2d_keystore_password $DATABASE_SECRET_DATETIME)
  fi

  # Create or get OKTA JWT issuer

  AB2D_OKTA_JWT_ISSUER=$(./get-database-secret.py $CMS_ENV_GE ab2d_okta_jwt_issuer $DATABASE_SECRET_DATETIME)
  if [ -z "${AB2D_OKTA_JWT_ISSUER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py $CMS_ENV_GE ab2d_okta_jwt_issuer $KMS_KEY_ID $DATABASE_SECRET_DATETIME
    echo "*********************************************************"
    AB2D_OKTA_JWT_ISSUER=$(./get-database-secret.py $CMS_ENV_GE ab2d_okta_jwt_issuer $DATABASE_SECRET_DATETIME)
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
    || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_KEYSTORE_LOCATION}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_KEYSTORE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
    || [ "${AB2D_OKTA_JWT_ISSUER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
      echo "ERROR: Cannot get secrets because KMS key is disabled!"
      exit 1
  fi
}

# Define configure greenfield environment function

configure_greenfield_environment ()
{

  # Set parameters
    
  AWS_ACCOUNT_NUMBER_GE="$1"   # Options: 349849222861|777200079629|330810004472|595094747606|653916833532
  CMS_ENV_GE="$2"              # Options: ab2d-dev|ab2d-sbx-sandbox|ab2d-east-impl|ab2d-east-prod|ab2d-mgmt-east-dev
  MODULE="$3"                  # Options: management_target|management_account
    
  # Get AWS credentials for target environment
  
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER_GE}" "${CMS_ENV_GE}"
  
  # Initialize and validate terraform for the target environment

  echo "******************************************************************************"
  echo "Initialize and validate terraform for the ${CMS_ENV_GE} environment..."
  echo "******************************************************************************"
  
  cd "${START_DIR}/.."
  cd terraform/environments/$CMS_ENV_GE
  
  rm -f *.tfvars
  
  terraform init \
    -backend-config="bucket=${CMS_ENV_GE}-automation" \
    -backend-config="key=${CMS_ENV_GE}/terraform/terraform.tfstate" \
    -backend-config="region=${AWS_DEFAULT_REGION}" \
    -backend-config="encrypt=true"
  
  terraform validate

  # Get the policies that are attached to the "ab2d-spe-developer" role
  # - the "ab2d-spe-developer" role is controlled by GDIT automation
  # - any modifications to the "ab2d-spe-developer" role are wiped out by GDIT automation
  # - therefore, we are creating our own role that has the same policies but that also allows us
  #   to set trust relationships
  
  AB2D_SPE_DEVELOPER_POLICIES=$(aws --region "${AWS_DEFAULT_REGION}" iam list-attached-role-policies \
    --role-name ab2d-spe-developer \
    --query "AttachedPolicies[*].PolicyArn" \
    | tr -d '[:space:]\n')

  # Create or verify greenfield components
  
  terraform apply \
    --var "mgmt_aws_account_number=${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" \
    --var ab2d_spe_developer_policies=$AB2D_SPE_DEVELOPER_POLICIES \
    --target "module.${MODULE}" \
    --auto-approve

  # Create of verify key pair

  KEY_NAME=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-key-pairs \
    --filters "Name=key-name,Values=${CMS_ENV_GE}" \
    --query "KeyPairs[*].KeyName" \
    --output text)

  if [ -z "${KEY_NAME}" ]; then

    # Create private key

    aws --region "${AWS_DEFAULT_REGION}" ec2 create-key-pair \
      --key-name ${CMS_ENV_GE} \
      --query 'KeyMaterial' \
      --output text \
      > ~/.ssh/${CMS_ENV_GE}.pem

    # Set permissions on private key

    chmod 600 ~/.ssh/${CMS_ENV_GE}.pem

  fi

  if [ "${CMS_ENV_GE}" != "ab2d-mgmt-east-dev" ]; then
    set_secrets "${CMS_ENV_GE}"
  fi

  # Upload or verify private key on Jenkins agent (for non-management environments)

  if [ "${AWS_ACCOUNT_NUMBER_GE}" != "${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" ]; then
    
    # Get AWS credentials for management environment
      
    fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_MGMT_ENV}"

    # Get the private IP address of Jenkins agent instance
    
    JENKINS_AGENT_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
      --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
      --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
      --output text)

    # Upload or verify private key on Jenkins agent

    PRIVATE_KEY_EXISTS=$(ssh -i ~/.ssh/${CMS_MGMT_ENV}.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP \
      sudo ls /home/jenkins/.ssh/${CMS_ENV_GE}.pem)

    if [ -z "${PRIVATE_KEY_EXISTS}" ]; then
      scp -i ~/.ssh/${CMS_MGMT_ENV}.pem ~/.ssh/${CMS_ENV_GE}.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP:~/.ssh \
        && ssh -i ~/.ssh/${CMS_MGMT_ENV}.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP \
  	sudo cp /home/ec2-user/.ssh/${CMS_ENV_GE}.pem /home/jenkins/.ssh/${CMS_ENV_GE}.pem
    fi
    
  fi
  echo ""
  echo "**********************************************************"
  echo "${CMS_ENV_GE} greenfield environment configured"
  echo "**********************************************************"
  echo ""
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
