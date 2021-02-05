#!/bin/bash

# set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" || exit; pwd -P )"
cd "${START_DIR}" || exit

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${CMS_DEV_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_IMPL_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_PROD_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${CMS_SBX_ENV_AWS_ACCOUNT_NUMBER_PARAM}" ] \
    || [ -z "${DATABASE_SECRET_DATETIME_PARAM}" ] \
    || [ -z "${FEDERATED_LOGIN_ROLE_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

#
# Set variables
#

CMS_DEV_ENV_AWS_ACCOUNT_NUMBER="${CMS_DEV_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

CMS_IMPL_ENV_AWS_ACCOUNT_NUMBER="${CMS_IMPL_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER="${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

CMS_PROD_ENV_AWS_ACCOUNT_NUMBER="${CMS_PROD_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

CMS_SBX_ENV_AWS_ACCOUNT_NUMBER="${CMS_SBX_ENV_AWS_ACCOUNT_NUMBER_PARAM}"

DATABASE_SECRET_DATETIME="${DATABASE_SECRET_DATETIME_PARAM}"

DEBUG_LEVEL="TRACE"

FEDERATED_LOGIN_ROLE="${FEDERATED_LOGIN_ROLE_PARAM}"

#
# Set AWS account environment variables
#

# Set environment variables for development AWS account

CMS_DEV_ENV=ab2d-dev

# Set environment variables for sandbox AWS account

CMS_SBX_ENV=ab2d-sbx-sandbox

# Set environment variables for implementation AWS account

CMS_IMPL_ENV=ab2d-east-impl

# Set environment variables for production AWS account

CMS_PROD_ENV=ab2d-east-prod

# Set environment variables for management AWS account

CMS_MGMT_ENV=ab2d-mgmt-east-dev

#
# Configure terraform
#

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

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
  
  cd "${START_DIR}/.." || exit
  cd python3 || exit
  
  # Create or get database user secret
  
  DATABASE_USER=$(./get-database-secret.py "${CMS_ENV_SS}" database_user "${DATABASE_SECRET_DATETIME}")
  if [ -z "${DATABASE_USER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" database_user "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    DATABASE_USER=$(./get-database-secret.py "${CMS_ENV_SS}" database_user "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get database password secret
  
  DATABASE_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" database_password "${DATABASE_SECRET_DATETIME}")
  if [ -z "${DATABASE_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" database_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    DATABASE_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" database_password "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get database name secret
  
  DATABASE_NAME=$(./get-database-secret.py "${CMS_ENV_SS}" database_name "${DATABASE_SECRET_DATETIME}")
  if [ -z "${DATABASE_NAME}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" database_name "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    DATABASE_NAME=$(./get-database-secret.py "${CMS_ENV_SS}" database_name "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get database schema name secret

  DATABASE_SCHEMA_NAME=$(./get-database-secret.py "${CMS_ENV_SS}" database_schema_name "${DATABASE_SECRET_DATETIME}")
  if [ -z "${DATABASE_SCHEMA_NAME}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" database_schema_name "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    DATABASE_SCHEMA_NAME=$(./get-database-secret.py "${CMS_ENV_SS}" database_schema_name "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get bfd url secret
  
  BFD_URL=$(./get-database-secret.py "${CMS_ENV_SS}" bfd_url "${DATABASE_SECRET_DATETIME}")
  if [ -z "${BFD_URL}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" bfd_url "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    BFD_URL=$(./get-database-secret.py "${CMS_ENV_SS}" bfd_url "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get bfd keystore location secret
  
  BFD_KEYSTORE_LOCATION=$(./get-database-secret.py "${CMS_ENV_SS}" bfd_keystore_location "${DATABASE_SECRET_DATETIME}")
  if [ -z "${BFD_KEYSTORE_LOCATION}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" bfd_keystore_location "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    BFD_KEYSTORE_LOCATION=$(./get-database-secret.py "${CMS_ENV_SS}" bfd_keystore_location "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get bfd keystore password secret
  
  BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" bfd_keystore_password "${DATABASE_SECRET_DATETIME}")
  if [ -z "${BFD_KEYSTORE_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" bfd_keystore_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    BFD_KEYSTORE_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" bfd_keystore_password "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get hicn hash pepper secret
  
  HICN_HASH_PEPPER=$(./get-database-secret.py "${CMS_ENV_SS}" hicn_hash_pepper "${DATABASE_SECRET_DATETIME}")
  if [ -z "${HICN_HASH_PEPPER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" hicn_hash_pepper "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    HICN_HASH_PEPPER=$(./get-database-secret.py "${CMS_ENV_SS}" hicn_hash_pepper "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get hicn hash iter secret
  
  HICN_HASH_ITER=$(./get-database-secret.py "${CMS_ENV_SS}" hicn_hash_iter "${DATABASE_SECRET_DATETIME}")
  if [ -z "${HICN_HASH_ITER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" hicn_hash_iter "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    HICN_HASH_ITER=$(./get-database-secret.py "${CMS_ENV_SS}" hicn_hash_iter "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get new relic app name secret
  
  NEW_RELIC_APP_NAME=$(./get-database-secret.py "${CMS_ENV_SS}" new_relic_app_name "${DATABASE_SECRET_DATETIME}")
  if [ -z "${NEW_RELIC_APP_NAME}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" new_relic_app_name "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    NEW_RELIC_APP_NAME=$(./get-database-secret.py "${CMS_ENV_SS}" new_relic_app_name "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get new relic license key secret
  
  NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py "${CMS_ENV_SS}" new_relic_license_key "${DATABASE_SECRET_DATETIME}")
  if [ -z "${NEW_RELIC_LICENSE_KEY}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" new_relic_license_key "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    NEW_RELIC_LICENSE_KEY=$(./get-database-secret.py "${CMS_ENV_SS}" new_relic_license_key "${DATABASE_SECRET_DATETIME}")
  fi
  
  # Create or get private ip address CIDR range for VPN
  
  VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py "${CMS_ENV_SS}" vpn_private_ip_address_cidr_range "${DATABASE_SECRET_DATETIME}")
  if [ -z "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" vpn_private_ip_address_cidr_range "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE=$(./get-database-secret.py "${CMS_ENV_SS}" vpn_private_ip_address_cidr_range "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get AB2D keystore location

  AB2D_KEYSTORE_LOCATION=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_keystore_location "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_KEYSTORE_LOCATION}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_keystore_location "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_KEYSTORE_LOCATION=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_keystore_location "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get AB2D keystore password

  AB2D_KEYSTORE_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_keystore_password "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_KEYSTORE_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_keystore_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_KEYSTORE_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_keystore_password "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get OKTA JWT issuer

  AB2D_OKTA_JWT_ISSUER=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_okta_jwt_issuer "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_OKTA_JWT_ISSUER}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_okta_jwt_issuer "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_OKTA_JWT_ISSUER=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_okta_jwt_issuer "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get OKTA Client ID #1

  OKTA_CLIENT_ID=$(./get-database-secret.py "${CMS_ENV_SS}" okta_client_id "${DATABASE_SECRET_DATETIME}")
  if [ -z "${OKTA_CLIENT_ID}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" okta_client_id "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    OKTA_CLIENT_ID=$(./get-database-secret.py "${CMS_ENV_SS}" okta_client_id "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get OKTA Client ID #1 secret

  OKTA_CLIENT_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" okta_client_password "${DATABASE_SECRET_DATETIME}")
  if [ -z "${OKTA_CLIENT_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" okta_client_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    OKTA_CLIENT_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" okta_client_password "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get OKTA Client ID #2

  SECONDARY_USER_OKTA_CLIENT_ID=$(./get-database-secret.py "${CMS_ENV_SS}" secondary_user_okta_client_id "${DATABASE_SECRET_DATETIME}")
  if [ -z "${SECONDARY_USER_OKTA_CLIENT_ID}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" secondary_user_okta_client_id "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    SECONDARY_USER_OKTA_CLIENT_ID=$(./get-database-secret.py "${CMS_ENV_SS}" secondary_user_okta_client_id "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get OKTA Client ID #2 secret

  SECONDARY_USER_OKTA_CLIENT_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" secondary_user_okta_client_password "${DATABASE_SECRET_DATETIME}")
  if [ -z "${SECONDARY_USER_OKTA_CLIENT_PASSWORD}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" secondary_user_okta_client_password "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    SECONDARY_USER_OKTA_CLIENT_PASSWORD=$(./get-database-secret.py "${CMS_ENV_SS}" secondary_user_okta_client_password "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get HPMS URL

  AB2D_HPMS_URL=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_url "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_HPMS_URL}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_url "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_HPMS_URL=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_url "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get HPMS API PARAMS

  AB2D_HPMS_API_PARAMS=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_api_params "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_HPMS_API_PARAMS}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_api_params "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_HPMS_API_PARAMS=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_api_params "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get HPMS AUTH key id

  AB2D_HPMS_AUTH_KEY_ID=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_auth_key_id "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_HPMS_AUTH_KEY_ID}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_auth_key_id "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_HPMS_AUTH_KEY_ID=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_auth_key_id "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get HPMS AUTH key secret

  AB2D_HPMS_AUTH_KEY_SECRET=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_auth_key_secret "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_HPMS_AUTH_KEY_SECRET}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_auth_key_secret "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_HPMS_AUTH_KEY_SECRET=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_hpms_auth_key_secret "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get BFD insights S3 bucket secret

  AB2D_BFD_INSIGHTS_S3_BUCKET=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_bfd_insights_s3_bucket "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_BFD_INSIGHTS_S3_BUCKET}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_bfd_insights_s3_bucket "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_BFD_INSIGHTS_S3_BUCKET=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_bfd_insights_s3_bucket "${DATABASE_SECRET_DATETIME}")
  fi

  # Create or get BFD KMS ARN secret

  AB2D_BFD_KMS_ARN=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_bfd_kms_arn "${DATABASE_SECRET_DATETIME}")
  if [ -z "${AB2D_BFD_KMS_ARN}" ]; then
    echo "*********************************************************"
    ./create-database-secret.py "${CMS_ENV_SS}" ab2d_bfd_kms_arn "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
    echo "*********************************************************"
    AB2D_BFD_KMS_ARN=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_bfd_kms_arn "${DATABASE_SECRET_DATETIME}")
  fi

  if [ "${CMS_ENV_SS}" == "ab2d-sbx-sandbox" ] \
      || [ "${CMS_ENV_SS}" == "ab2d-east-prod" ]; then

    AB2D_SLACK_ALERT_WEBHOOKS=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_slack_alert_webhooks "${DATABASE_SECRET_DATETIME}")
    if [ -z "${AB2D_SLACK_ALERT_WEBHOOKS}" ]; then
      echo "*********************************************************"
      ./create-database-secret.py "${CMS_ENV_SS}" ab2d_slack_alert_webhooks "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
      echo "*********************************************************"
      AB2D_SLACK_ALERT_WEBHOOKS=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_slack_alert_webhooks "${DATABASE_SECRET_DATETIME}")
    fi

    AB2D_SLACK_TRACE_WEBHOOKS=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_slack_trace_webhooks "${DATABASE_SECRET_DATETIME}")
    if [ -z "${AB2D_SLACK_TRACE_WEBHOOKS}" ]; then
      echo "*********************************************************"
      ./create-database-secret.py "${CMS_ENV_SS}" ab2d_slack_trace_webhooks "${KMS_KEY_ID}" "${DATABASE_SECRET_DATETIME}"
      echo "*********************************************************"
      AB2D_SLACK_TRACE_WEBHOOKS=$(./get-database-secret.py "${CMS_ENV_SS}" ab2d_slack_trace_webhooks "${DATABASE_SECRET_DATETIME}")
    fi

  fi

  # If any secret produced an error, exit the script

  DATABASE_SECRET_ERROR_MESSAGE="ERROR: Cannot get database secret because KMS key is disabled!"

  if [ "${DATABASE_USER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${DATABASE_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${DATABASE_NAME}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${BFD_URL}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${BFD_KEYSTORE_LOCATION}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${BFD_KEYSTORE_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${HICN_HASH_PEPPER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${HICN_HASH_ITER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${NEW_RELIC_APP_NAME}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${NEW_RELIC_LICENSE_KEY}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_KEYSTORE_LOCATION}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_KEYSTORE_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_OKTA_JWT_ISSUER}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${OKTA_CLIENT_ID}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${OKTA_CLIENT_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${SECONDARY_USER_OKTA_CLIENT_ID}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${SECONDARY_USER_OKTA_CLIENT_PASSWORD}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_HPMS_URL}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_HPMS_API_PARAMS}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_HPMS_AUTH_KEY_ID}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_HPMS_AUTH_KEY_SECRET}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_BFD_INSIGHTS_S3_BUCKET}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_BFD_KMS_ARN}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_SLACK_ALERT_WEBHOOKS}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ] \
    || [ "${AB2D_SLACK_TRACE_WEBHOOKS}" == "${DATABASE_SECRET_ERROR_MESSAGE}" ]; then
      echo "ERROR: Cannot get secrets because KMS key is disabled!"
      exit 1
  fi
}

# Define configure greenfield environment function

configure_greenfield_environment ()
{

  # Set parameters
    
  AWS_ACCOUNT_NUMBER_GE="$1"
  CMS_ENV_GE="$2"              # Options: ab2d-dev|ab2d-sbx-sandbox|ab2d-east-impl|ab2d-east-prod|ab2d-mgmt-east-dev
  MODULE="$3"                  # Options: management_target|management_account

  # Get AWS credentials for target environment

  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER_GE}" "${CMS_ENV_GE}"

  # Initialize and validate terraform for the target environment

  echo "******************************************************************************"
  echo "Initialize and validate terraform for the ${CMS_ENV_GE} environment..."
  echo "******************************************************************************"

  cd "${START_DIR}/.." || exit
  cd terraform/environments/"${CMS_ENV_GE}" || exit

  rm -f ./*.tfvars

  terraform init \
    -backend-config="bucket=${CMS_ENV_GE}-automation" \
    -backend-config="key=${CMS_ENV_GE}/terraform/terraform.tfstate" \
    -backend-config="region=${AWS_DEFAULT_REGION}" \
    -backend-config="encrypt=true"

  terraform validate

  # Get the policies that are attached to the federated login role
  # - the federated login role is controlled by ITOPS automation
  # - any modifications to the federated login role are wiped out by ITOPS automation
  # - therefore, we are creating our own role that has the same policies but that also allows us
  #   to set trust relationships

  FEDERATED_LOGIN_ROLE_POLICIES=$(aws --region "${AWS_DEFAULT_REGION}" iam list-attached-role-policies \
    --role-name "${FEDERATED_LOGIN_ROLE}" \
    --query "AttachedPolicies[*].PolicyArn" \
    | tr -d '[:space:]\n')

  # Create or verify greenfield components

  terraform apply \
    --var "aws_account_number=${AWS_ACCOUNT_NUMBER_GE}" \
    --var "env=${CMS_ENV_GE}" \
    --var "federated_login_role_policies=${FEDERATED_LOGIN_ROLE_POLICIES}" \
    --var "mgmt_aws_account_number=${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" \
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
      --key-name "${CMS_ENV_GE}" \
      --query 'KeyMaterial' \
      --output text \
      > ~/.ssh/"${CMS_ENV_GE}.pem"

    # Set permissions on private key

    chmod 600 ~/.ssh/"${CMS_ENV_GE}.pem"

  fi

  if [ "${CMS_ENV_GE}" != "ab2d-mgmt-east-dev" ]; then
    set_secrets "${CMS_ENV_GE}"
  fi

  # Configure deployment Jenkins agent for static website updates

  if [ "${AWS_ACCOUNT_NUMBER_GE}" == "${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" ]; then

    # Get AWS credentials for management environment

    fn_get_temporary_aws_credentials_via_cloudtamer_api "${CMS_MGMT_ENV_AWS_ACCOUNT_NUMBER}" "${CMS_MGMT_ENV}"

    # Get the private IP address of deployment Jenkins agent

    DEPLOYMENT_JENKINS_AGENT_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
      --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
      --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
      --output text)

    # Upload or verify Akamai private SSH key private key on Jenkins agent

    PRIVATE_KEY_EXISTS=$(ssh -i ~/.ssh/${CMS_MGMT_ENV}.pem ec2-user@"${DEPLOYMENT_JENKINS_AGENT_PRIVATE_IP}" \
      sudo ls /var/lib/jenkins/.ssh/"${CMS_ENV_GE}.pem")

    # Copy the Akamai private SSH key to the Jenkins deployment agent (if doesn't exist)

    if [ -z "${PRIVATE_KEY_EXISTS}" ]; then

      # Ensure Akamai private SSH key exists on development machine

      if [ -f ~/.ssh/ab2d-akamai ]; then
        scp -i ~/.ssh/${CMS_MGMT_ENV}.pem ~/.ssh/ab2d-akamai ec2-user@"${DEPLOYMENT_JENKINS_AGENT_PRIVATE_IP}":~/.ssh \
          && ssh -i ~/.ssh/${CMS_MGMT_ENV}.pem ec2-user@"${DEPLOYMENT_JENKINS_AGENT_PRIVATE_IP}" \
            sudo cp /home/ec2-user/.ssh/ab2d-akamai /var/lib/jenkins/.ssh/ab2d-akamai \
          && ssh -i ~/.ssh/${CMS_MGMT_ENV}.pem ec2-user@"${DEPLOYMENT_JENKINS_AGENT_PRIVATE_IP}" \
            sudo chown jenkins:jenkins /var/lib/jenkins/.ssh/ab2d-akamai
      else
        echo "ERROR: ab2d-akamai private key does not exist on development machine."
        exit
      fi

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

# Configure target development AWS account

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
