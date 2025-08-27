# Define get temporary AWS credentials via AWS STS assume role
echo "getting temporary AWS credentials via AWS STS assume role"

fn_get_temporary_aws_credentials_via_aws_sts_assume_role ()
{
  # Set parameters

  IN_AWS_ACCOUNT_NUMBER_ASSUME_ROLE="$1"
  IN_SESSION_NAME_ASSUME_ROLE="$2"

  # Unset existing credentials

  unset AWS_ACCESS_KEY_ID
  unset AWS_SECRET_ACCESS_KEY
  unset AWS_SESSION_TOKEN

  # Set default AWS region

  export AWS_DEFAULT_REGION="us-east-1"

  # Get json output for temporary AWS credentials

  echo ""
  echo "-----------------------------"
  echo "Getting temporary credentials"
  echo "-----------------------------"
  echo ""

  JSON_OUTPUT=$(aws --region "${AWS_DEFAULT_REGION}" sts assume-role \
    --role-arn "arn:aws:iam::${IN_AWS_ACCOUNT_NUMBER_ASSUME_ROLE}:role/delegatedadmin/developer/Ab2dMgmtV2Role" \
    --role-session-name "${IN_SESSION_NAME_ASSUME_ROLE}" \
    | jq --raw-output ".Credentials")

  # Get temporary AWS credentials

  export AWS_ACCESS_KEY_ID=$(echo $JSON_OUTPUT | jq --raw-output ".AccessKeyId")
  export AWS_SECRET_ACCESS_KEY=$(echo $JSON_OUTPUT | jq --raw-output ".SecretAccessKey")

  # Get AWS session token (required for temporary credentials)

  export AWS_SESSION_TOKEN=$(echo $JSON_OUTPUT | jq --raw-output ".SessionToken")

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

  # Summary

  echo ""
  echo "*******************************************************************************"
  echo "${IN_CMS_ENV} environment variables have been set."
  echo "-------------------------------------------------------------------------------"
  echo "NOTE: These credentials expire in 1 hour."
  echo "*******************************************************************************"
  echo ""
}
