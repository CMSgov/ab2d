#!/bin/bash

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Parse options
#

echo "Parse options..."
for i in "$@"
do
case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
  CMS_ENV=$(echo $ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
esac
done

#
# Set environment
#

export AWS_PROFILE="sbdemo-shared"

# Get VPC ID (if exists)

VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="Vpcs[*].VpcId" \
  --output text)

# Continue process if VPC does not exist

if [ -z "${VPC_ID}" ]; then

  echo "Creating VPC..."

  # Create VPC
  VPC_ID=$(aws ec2 create-vpc \
    --cidr-block 10.242.26.0/24 \
    --query 'Vpc.{VpcId:VpcId}' \
    --output text \
    --region us-east-1)

  # Add name tag to VPC

  aws --region us-east-1 ec2 create-tags \
    --resources $VPC_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}"

else
    
  echo "INFO: The VPC already exists."
  
fi

# Associate a secondary IPv4 CIDR block with the VPC

SECONDARY_CIDR_BLOCK_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="Vpcs[*].CidrBlockAssociationSet[?CidrBlock=='10.242.5.128/26'].CidrBlock" \
  --output text)

if [ -z "${SECONDARY_CIDR_BLOCK_EXISTS}" ]; then
  echo "Associating ..."
  aws ec2 associate-vpc-cidr-block \
    --vpc-id $VPC_ID \
    --cidr-block 10.242.5.128/26
fi

# Tag default route table

DEFAULT_ROUTE_TABLE_ID=$(aws ec2 describe-route-tables \
  --filters "Name=association.main,Values=true" \
    "Name=vpc-id,Values=${VPC_ID}" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

DEFAULT_ROUTE_TABLE_TAG_EXISTS=$(aws ec2 describe-route-tables \
  --filters "Name=route-table-id,Values=${DEFAULT_ROUTE_TABLE_ID}" \
    "Name=tag:Name,Values=ab2d-default-rt" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

if [ -z "${DEFAULT_ROUTE_TABLE_TAG_EXISTS}" ]; then
  aws ec2 create-tags \
    --resources $DEFAULT_ROUTE_TABLE_ID \
    --tags "Key=Name,Value=ab2d-default-rt" \
    --region us-east-1
fi

# Tag default network ACL

DEFAULT_NETWORK_ACL_ID=$(aws ec2 describe-network-acls \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --query="NetworkAcls[?IsDefault].NetworkAclId" \
  --output text)

DEFAULT_NETWORK_TAG_EXISTS=$(aws ec2 describe-network-acls \
  --filters "Name=network-acl-id,Values=${DEFAULT_NETWORK_ACL_ID}" \
    "Name=tag:Name,Values=ab2d-default-nacl" \
  --query="NetworkAcls[?IsDefault].NetworkAclId" \
  --output text)

if [ -z "${DEFAULT_NETWORK_TAG_EXISTS}" ]; then
  aws ec2 create-tags \
    --resources $DEFAULT_NETWORK_ACL_ID \
    --tags "Key=Name,Value=ab2d-default-nacl" \
    --region us-east-1
fi

# Tag default security group

DEFAULT_SECURITY_GROUP_ID=$(aws ec2 describe-security-groups \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --query="SecurityGroups[?GroupName == 'default'].GroupId" \
  --output text)

DEFAULT_SECURITY_GROUP_TAG_EXISTS=$(aws ec2 describe-security-groups \
  --filters "Name=group-id,Values=${DEFAULT_SECURITY_GROUP_ID}" \
    "Name=tag:Name,Values=ab2d-default-sg" \
  --query="SecurityGroups[?IsDefault].GroupId" \
  --output text)

if [ -z "${DEFAULT_SECURITY_GROUP_TAG_EXISTS}" ]; then
  aws ec2 create-tags \
    --resources $DEFAULT_SECURITY_GROUP_ID \
    --tags "Key=Name,Value=ab2d-default-sg" \
    --region us-east-1
fi

echo "The VPC ID is: $VPC_ID"
echo "Done."
