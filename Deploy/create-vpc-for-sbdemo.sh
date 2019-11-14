#!/bin/bash

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set environment
#

export AWS_PROFILE="sbdemo-dev"

# Get VPC ID (if exists)

VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-vpc" \
  --query="Vpcs[*].VpcId" \
  --output text)

# Continue process if VPC does not exist

if [ -z "${VPC_ID}" ]; then

  echo "Creating VPC..."

  # Create VPC
  VPC_ID=$(aws ec2 create-vpc \
    --cidr-block 10.124.0.0/16 \
    --query 'Vpc.{VpcId:VpcId}' \
    --output text \
    --region us-east-1)

else
    
  echo "INFO: The VPC already exists."
  
fi

# Add name tag to VPC

if [ -z "${SKIP_NETWORK}" ]; then
  aws --region us-east-1 ec2 create-tags \
    --resources $VPC_ID \
    --tags "Key=Name,Value=ab2d-vpc"
fi

# Tag default route table

DEFAULT_ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters "Name=association.main,Values=true" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

aws ec2 create-tags \
  --resources $DEFAULT_ROUTE_TABLE_ID \
  --tags "Key=Name,Value=ab2d-default-rt" \
  --region us-east-1

# Tag default network ACL

DEFAULT_NETWORK_ACL_ID=$(aws ec2 describe-network-acls \
  --query="NetworkAcls[?IsDefault].NetworkAclId" \
  --output text)

aws ec2 create-tags \
  --resources $DEFAULT_NETWORK_ACL_ID \
  --tags "Key=Name,Value=ab2d-default-nacl" \
  --region us-east-1

# Tag default security group

DEFAULT_SECURITY_GROUP_ID=$(aws ec2 describe-security-groups \
  --query="SecurityGroups[?GroupName == 'default'].GroupId" \
  --output text)

aws ec2 create-tags \
  --resources $DEFAULT_SECURITY_GROUP_ID \
  --tags "Key=Name,Value=ab2d-default-sg" \
  --region us-east-1

echo "The VPC ID is: $VPC_ID"
echo "Done."
