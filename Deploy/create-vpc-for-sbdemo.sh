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
  --vpc-cidr-block-1=*)
  VPC_CIDR_BLOCK_1="${i#*=}"
  shift # past argument=value
  ;;
  --vpc-cidr-block-2=*)
  VPC_CIDR_BLOCK_2="${i#*=}"
  shift # past argument=value
  ;;
  --subnet-public-1-cidr-block=*)
  SUBNET_PUBLIC_1_CIDR_BLOCK="${i#*=}"
  shift # past argument=value
  ;;
  --subnet-public-2-cidr-block=*)
  SUBNET_PUBLIC_2_CIDR_BLOCK="${i#*=}"
  shift # past argument=value
  ;;
  --subnet-private-1-cidr-block=*)
  SUBNET_PRIVATE_1_CIDR_BLOCK="${i#*=}"
  shift # past argument=value
  ;;
  --subnet-private-2-cidr-block=*)
  SUBNET_PRIVATE_2_CIDR_BLOCK="${i#*=}"
  shift # past argument=value
  ;;
esac
done

# Set environment

export AWS_PROFILE="sbdemo-shared"

# Create or verify VPC

VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="Vpcs[*].VpcId" \
  --output text)

if [ -z "${VPC_ID}" ]; then
  echo "Creating VPC..."
  VPC_ID=$(aws --region us-east-1 ec2 create-vpc \
    --cidr-block "${VPC_CIDR_BLOCK_1}" \
    --query 'Vpc.{VpcId:VpcId}' \
    --output text \
    --region us-east-1)
else
    
  echo "INFO: The VPC already exists."
    
  # Verify first CIDR block is present in the VPC
  
  FIRST_CIDR_BLOCK_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
    --query="Vpcs[*].CidrBlockAssociationSet[?CidrBlock=='${VPC_CIDR_BLOCK_1}'].CidrBlock" \
    --output text)

  if [ -z "${FIRST_CIDR_BLOCK_EXISTS}" ]; then
    echo "ERROR: the first CIDR block is not present in the VPC"
    exit 1
  else
    echo "First CIDR block in VPC is verified..."
  fi
  
fi

# Associate a secondary IPv4 CIDR block with the VPC

SECONDARY_CIDR_BLOCK_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="Vpcs[*].CidrBlockAssociationSet[?CidrBlock=='${VPC_CIDR_BLOCK_2}'].CidrBlock" \
  --output text)

if [ -z "${SECONDARY_CIDR_BLOCK_EXISTS}" ]; then
  echo "Associating secondary CIDR block..."
  aws ec2 associate-vpc-cidr-block \
    --vpc-id $VPC_ID \
    --cidr-block "${VPC_CIDR_BLOCK_2}"
else
  echo "Second CIDR block in VPC is verified..."
fi

# Tag VPC

VPC_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query "Vpcs[*].VpcId" \
  --output text)

if [ -z "${VPC_TAG_EXISTS}" ]; then
  echo "Setting tag for vpc..."
  aws --region us-east-1 ec2 create-tags \
    --resources $VPC_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}"
else
  echo "VPC tag verified..."
fi

# Tag default route table

DEFAULT_ROUTE_TABLE_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filters "Name=association.main,Values=true" \
    "Name=vpc-id,Values=${VPC_ID}" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

DEFAULT_ROUTE_TABLE_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-route-tables \
  --filters "Name=route-table-id,Values=${DEFAULT_ROUTE_TABLE_ID}" \
    "Name=tag:Name,Values=ab2d-default-rt" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

if [ -z "${DEFAULT_ROUTE_TABLE_TAG_EXISTS}" ]; then
  echo "Setting tag for default route table..."
  aws ec2 create-tags \
    --resources $DEFAULT_ROUTE_TABLE_ID \
    --tags "Key=Name,Value=ab2d-default-rt" \
    --region us-east-1
else
  echo "Default route table tag verified..."
fi

# Tag default network ACL

DEFAULT_NETWORK_ACL_ID=$(aws --region us-east-1 ec2 describe-network-acls \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --query="NetworkAcls[?IsDefault].NetworkAclId" \
  --output text)

DEFAULT_NETWORK_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-network-acls \
  --filters "Name=network-acl-id,Values=${DEFAULT_NETWORK_ACL_ID}" \
    "Name=tag:Name,Values=ab2d-default-nacl" \
  --query="NetworkAcls[*].NetworkAclId" \
  --output text)

if [ -z "${DEFAULT_NETWORK_TAG_EXISTS}" ]; then
  echo "Setting tag for default network ACL..."
  aws ec2 create-tags \
    --resources $DEFAULT_NETWORK_ACL_ID \
    --tags "Key=Name,Value=ab2d-default-nacl" \
    --region us-east-1
else
  echo "Default network ACL tag verified..."
fi

# Tag default security group

DEFAULT_SECURITY_GROUP_ID=$(aws --region us-east-1 ec2 describe-security-groups \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --query="SecurityGroups[?GroupName == 'default'].GroupId" \
  --output text)

DEFAULT_SECURITY_GROUP_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-security-groups \
  --filters "Name=group-id,Values=${DEFAULT_SECURITY_GROUP_ID}" \
    "Name=tag:Name,Values=ab2d-default-sg" \
  --query="SecurityGroups[*].GroupId" \
  --output text)

if [ -z "${DEFAULT_SECURITY_GROUP_TAG_EXISTS}" ]; then
  echo "Setting tag for default security group..."
  aws ec2 create-tags \
    --resources $DEFAULT_SECURITY_GROUP_ID \
    --tags "Key=Name,Value=ab2d-default-sg" \
    --region us-east-1
else
  echo "Default security group tag verified..."
fi

# Enable or verify DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws --region us-east-1 ec2 describe-vpc-attribute \
  --vpc-id $VPC_ID \
  --attribute enableDnsHostnames \
  --query "EnableDnsHostnames.Value" \
  --output text)

if [ "${VPC_ENABLE_DNS_HOSTNAMES}" == "False" ]; then
  echo "Enabling DNS hostnames on VPC..."
  aws ec2 modify-vpc-attribute \
    --vpc-id $VPC_ID \
    --enable-dns-hostnames
else
  echo "DNS hostname verified as enabled..."
fi

# Create or verify first public subnet

SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "Creating first public subnet..."
  SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PUBLIC_1_CIDR_BLOCK}" \
    --availability-zone us-east-1a \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)
else
  echo "First public subnet verified..."
fi

# Tag first public subnet

SUBNET_PUBLIC_1_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_TAG_EXISTS}" ]; then
  echo "Setting tag for first public subnet..."
  aws ec2 create-tags \
    --resources $SUBNET_PUBLIC_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-public-a" \
    --region us-east-1
else
  echo "First public subnet tag verified..."
fi

# Create or verify second public subnet

SUBNET_PUBLIC_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then
  echo "Creating second public subnet..."
  SUBNET_PUBLIC_2_ID=$(aws --region us-east-1 ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PUBLIC_2_CIDR_BLOCK}" \
    --availability-zone us-east-1a \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)
else
  echo "Second public subnet verified..."
fi

# Create or verify second public subnet

SUBNET_PUBLIC_2_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_TAG_EXISTS}" ]; then
  echo "Setting tag for second public subnet..."
  aws ec2 create-tags \
    --resources $SUBNET_PUBLIC_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-public-b" \
    --region us-east-1
else
  echo "Second public subnet tag verified..."
fi

# Create or verify first private subnet

SUBNET_PRIVATE_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then
  echo "Creating first private subnet..."
  SUBNET_PRIVATE_1_ID=$(aws --region us-east-1 ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PRIVATE_1_CIDR_BLOCK}" \
    --availability-zone us-east-1a \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)
else
  echo "First private subnet verified..."
fi

# Tag first private subnet

SUBNET_PRIVATE_1_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_TAG_EXISTS}" ]; then
  echo "Setting tag for first private subnet..."
  aws ec2 create-tags \
    --resources $SUBNET_PRIVATE_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-private-a" \
    --region us-east-1
else
  echo "First private subnet tag verified..."
fi

# Create or verify second private subnet

SUBNET_PRIVATE_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then
  echo "Creating second private subnet..."
  SUBNET_PRIVATE_2_ID=$(aws --region us-east-1 ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PRIVATE_2_CIDR_BLOCK}" \
    --availability-zone us-east-1a \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region us-east-1)
else
  echo "Second private subnet verified..."
fi

# Tag second private subnet

SUBNET_PRIVATE_2_TAG_EXISTS=$(aws --region us-east-1 ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_TAG_EXISTS}" ]; then
  echo "Setting tag for second private subnet..."
  aws ec2 create-tags \
    --resources $SUBNET_PRIVATE_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-private-b" \
    --region us-east-1
else
  echo "Second private subnet tag verified..."
fi

# Done

echo "The VPC ID is: $VPC_ID"
echo "Done."
