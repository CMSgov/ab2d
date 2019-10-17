#!/bin/bash
set -e #Exit on first error
set -x #Be verbose

#
# Configure
#

# Change to working directory
cd "$(dirname "$0")"

# Configure environment setting
export AWS_PROFILE="sbdemo"
export CMS_ENV="SBDEMO"


#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
  CMS_ENV=$(echo $ENVIRONMENT | tr '[:lower:]' '[:upper:]')
  shift # past argument=value
  ;;
  --keep-network=*)
  KEEP_NETWORK="true"
  shift # past argument=value
  ;;
  --keep-ami)
  KEEP_AMI="true"
  shift # past argument=value
  ;;
esac
done


#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ]; then
  echo "Try running the script like one of these options:"
  echo "./destroy-environment.sh --environment=sbdemo"
  echo "./destroy-environment.sh --environment=sbdemo --keep-ami"
  echo "./destroy-environment.sh --environment=sbdemo --keep-networking"
  echo "./destroy-environment.sh --environment=sbdemo --keep-ami --keep-networking"
  exit 1
fi


#
# Destroy the api and worker modules
#

# Change to the environment directory
ALB_ARN=$(aws --region us-east-1 elbv2 describe-load-balancers \
  --name=ab2d-sbdemo \
  --query 'LoadBalancers[*].[LoadBalancerArn]' \
  --output text)

# Turn off "delete protection" for the application load balancer
echo "Turn off 'delete protection' for the application load balancer..."
aws elbv2 modify-load-balancer-attributes \
  --load-balancer-arn $ALB_ARN \
  --attributes Key=deletion_protection.enabled,Value=false

# Destroy the environment of the "worker" module
echo "Destroying worker components..."
terraform destroy \
  --target module.worker --auto-approve

# Destroy the environment of the "api" module
echo "Destroying API components..."
terraform destroy \
  --target module.api --auto-approve


#
# Deregister the application AMI
#

# Get application AMI ID (if exists)

APPLICATION_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=AB2D-FRED-AMI" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${APPLICATION_AMI_ID}" ]; then
  if [ -n "${KEEP_AMI}" ]; then
    echo "No existing application AMI to preserve..."
  fi
else
  if [ -z "${KEEP_AMI}" ]; then
    echo "Deregistering application AMI..."
    aws ec2 deregister-image \
      --image-id $AMI_ID
  else
    echo "Preserving application AMI..."
  fi
fi


#
# Destroy non-compute modules
#

# Destroy the environment of the "db" module
echo "Destroying DB components..."
terraform destroy \
  --target module.db --auto-approve

# Destroy the environment of the "efs" module
echo "Destroying EFS components..."
terraform destroy \
  --target module.efs --auto-approve

# Destroy the environment of the "s3" module
echo "Destroying S3 components..."
terraform destroy \
  --target module.s3 --auto-approve

# Destroy the environment of the "kms" module
echo "Destroying KMS components..."
terraform destroy \
  --target module.kms --auto-approve
