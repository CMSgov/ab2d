#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set parameters
#


CMS_ENV="${CMS_ENV_PARAM}"

export DEBUG_LEVEL="${DEBUG_LEVEL_PARAM}"

REGION="${REGION_PARAM}"

#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${CMS_ENV_PARAM}" ] \
    || [ -z "${REGION_PARAM}" ] \
    || [ -z "${DEBUG_LEVEL_PARAM}" ] \
    || [ -z "${INTERNET_FACING_PARAM}" ]; then
  echo "ERROR: All parameters must be set."
  exit 1
fi

# Set whether load balancer is internal based on "internet-facing" parameter

if [ "$INTERNET_FACING_PARAM" == "false" ]; then
  ALB_INTERNAL=true
elif [ "$INTERNET_FACING_PARAM" == "true" ]; then
  ALB_INTERNAL=false
else
  echo "ERROR: the '--internet-facing' parameter must be true or false"
  exit 1
fi

#
# Initialize and validate terraform
#

# Set AWS profile to the target environment

export AWS_PROFILE="${CMS_ENV}"

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

# Get load balancer listener protocol, port, and certificate

if [ "$CMS_ENV" == "ab2d-sbx-sandbox" ]; then
  ALB_SECURITY_GROUP_IP_RANGE="0.0.0.0/0"
else
  ALB_SECURITY_GROUP_IP_RANGE="${VPN_PRIVATE_IP_ADDRESS_CIDR_RANGE}"
fi

#
# Update AWS WAF
#

terraform apply \
  --var "alb_internal=$ALB_INTERNAL" \
  --var "alb_security_group_ip_range=$ALB_SECURITY_GROUP_IP_RANGE" \
  --target module.waf \
  --auto-approve
