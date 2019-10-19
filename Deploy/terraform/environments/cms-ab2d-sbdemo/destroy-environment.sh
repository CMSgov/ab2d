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
# Destroy db, efs, s3, and kms modules
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

#
# Deregister the application AMI
#

# Get application AMI ID (if exists)
APPLICATION_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-AMI" \
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
# Deregister the Jenkins AMI
#

# Get application AMI ID (if exists)
JENKINS_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-JENKINS-AMI" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${JENKINS_AMI_ID}" ]; then
  if [ -n "${KEEP_AMI}" ]; then
    echo "No existing Jenkins AMI to preserve..."
  fi
else
  if [ -z "${KEEP_AMI}" ]; then
    echo "Deregistering Jenkins AMI..."
    aws ec2 deregister-image \
      --image-id $JENKINS_AMI_ID
  else
    echo "Preserving Jenkins AMI..."
  fi
fi

#
# Delete the NAT gateways
#

# Delete first NAT gateway

NAT_GW_1_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-1" "Name=state,Values=available" \
  --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
  --output text)

aws --region us-east-1 ec2 delete-nat-gateway \
  --nat-gateway-id $NAT_GW_1_ID

# Delete second NAT gateway

NAT_GW_2_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-2" "Name=state,Values=available" \
  --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
  --output text)

aws --region us-east-1 ec2 delete-nat-gateway \
  --nat-gateway-id $NAT_GW_2_ID

# Delete third NAT gateway

NAT_GW_3_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-3" "Name=state,Values=available" \
  --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
  --output text)

aws --region us-east-1 ec2 delete-nat-gateway \
  --nat-gateway-id $NAT_GW_3_ID

# Wait for NAT gateways to all enter the deleted state

echo "Wait for NAT gateways to all enter the deleted state..."

NAT_GATEWAYS_STATE="NOT_EMPTY"
RETRIES_NGW=0

while [ -n "${NAT_GATEWAYS_STATE}" ]; do
  echo "Checking status of NAT Gateways as they are deleting..."
  if [ "$RETRIES_NGW" != "15" ]; then
    NAT_GATEWAYS_STATE=$(aws --region us-east-1 ec2 describe-nat-gateways \
      --filter "Name=state,Values=available,pending,deleting" \
      --query 'NatGateways[*].{NatGatewayId:NatGatewayId,State:State}' \
      --output text)
    if [ -n "${NAT_GATEWAYS_STATE}" ]; then
      echo "NAT_GATEWAYS_STATE="$NAT_GATEWAYS_STATE
      echo "Retry in 60 seconds..."
      sleep 60
      RETRIES_NGW=$(expr $RETRIES_NGW + 1)
    else
      echo "NAT gateways successfully deleted..."
      exit 0
    fi
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done

#
# Release Elastic IP addresses
#

# Release first Elastic IP address

NGW_EIP_1_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-EIP-1" \
  --query 'Addresses[*].[AllocationId]' \
  --output text)

aws --region us-east-1 ec2 release-address \
  --allocation-id $NGW_EIP_1_ALLOCATION_ID

# Release second Elastic IP address

NGW_EIP_2_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-EIP-2" \
  --query 'Addresses[*].[AllocationId]' \
  --output text)

aws --region us-east-1 ec2 release-address \
  --allocation-id $NGW_EIP_2_ALLOCATION_ID

# Release third Elastic IP address

NGW_EIP_3_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-EIP-3" \
  --query 'Addresses[*].[AllocationId]' \
  --output text)

aws --region us-east-1 ec2 release-address \
  --allocation-id $NGW_EIP_3_ALLOCATION_ID
