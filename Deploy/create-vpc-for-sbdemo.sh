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
  --region=*)
  REGION="${i#*=}"
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

VPC_ID=$(aws --region "${REGION}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="Vpcs[*].VpcId" \
  --output text)

if [ -z "${VPC_ID}" ]; then
  echo "Creating VPC..."
  VPC_ID=$(aws --region "${REGION}" ec2 create-vpc \
    --cidr-block "${VPC_CIDR_BLOCK_1}" \
    --query 'Vpc.{VpcId:VpcId}' \
    --output text )
else
    
  echo "INFO: The VPC already exists."
    
  # Verify first CIDR block is present in the VPC
  
  FIRST_CIDR_BLOCK_EXISTS=$(aws --region "${REGION}" ec2 describe-vpcs \
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

SECONDARY_CIDR_BLOCK_EXISTS=$(aws --region "${REGION}" ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="Vpcs[*].CidrBlockAssociationSet[?CidrBlock=='${VPC_CIDR_BLOCK_2}'].CidrBlock" \
  --output text)

if [ -z "${SECONDARY_CIDR_BLOCK_EXISTS}" ]; then
  echo "Associating secondary CIDR block..."
  aws --region "${REGION}" ec2 associate-vpc-cidr-block \
    --vpc-id $VPC_ID \
    --cidr-block "${VPC_CIDR_BLOCK_2}"
else
  echo "Second CIDR block in VPC is verified..."
fi

# Tag VPC

VPC_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-vpcs \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query "Vpcs[*].VpcId" \
  --output text)

if [ -z "${VPC_TAG_EXISTS}" ]; then
  echo "Setting tag for vpc..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $VPC_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}"
else
  echo "VPC tag verified..."
fi

# Tag default route table

DEFAULT_ROUTE_TABLE_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=association.main,Values=true" \
    "Name=vpc-id,Values=${VPC_ID}" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

DEFAULT_ROUTE_TABLE_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=route-table-id,Values=${DEFAULT_ROUTE_TABLE_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-default-rt" \
  --query "RouteTables[*].Associations[*].RouteTableId" \
  --output text)

if [ -z "${DEFAULT_ROUTE_TABLE_TAG_EXISTS}" ]; then
  echo "Setting tag for default route table..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $DEFAULT_ROUTE_TABLE_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-default-rt"
else
  echo "Default route table tag verified..."
fi

# Tag default network ACL

DEFAULT_NETWORK_ACL_ID=$(aws --region "${REGION}" ec2 describe-network-acls \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --query="NetworkAcls[?IsDefault].NetworkAclId" \
  --output text)

DEFAULT_NETWORK_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-network-acls \
  --filters "Name=network-acl-id,Values=${DEFAULT_NETWORK_ACL_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-default-nacl" \
  --query="NetworkAcls[*].NetworkAclId" \
  --output text)

if [ -z "${DEFAULT_NETWORK_TAG_EXISTS}" ]; then
  echo "Setting tag for default network ACL..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $DEFAULT_NETWORK_ACL_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-default-nacl"
else
  echo "Default network ACL tag verified..."
fi

# Tag default security group

DEFAULT_SECURITY_GROUP_ID=$(aws --region "${REGION}" ec2 describe-security-groups \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
  --query="SecurityGroups[?GroupName == 'default'].GroupId" \
  --output text)

DEFAULT_SECURITY_GROUP_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-security-groups \
  --filters "Name=group-id,Values=${DEFAULT_SECURITY_GROUP_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-default-sg" \
  --query="SecurityGroups[*].GroupId" \
  --output text)

if [ -z "${DEFAULT_SECURITY_GROUP_TAG_EXISTS}" ]; then
  echo "Setting tag for default security group..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $DEFAULT_SECURITY_GROUP_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-default-sg"
else
  echo "Default security group tag verified..."
fi

# Enable or verify DNS hostname on VPC

VPC_ENABLE_DNS_HOSTNAMES=$(aws --region "${REGION}" ec2 describe-vpc-attribute \
  --vpc-id $VPC_ID \
  --attribute enableDnsHostnames \
  --query "EnableDnsHostnames.Value" \
  --output text)

if [ "${VPC_ENABLE_DNS_HOSTNAMES}" == "False" ]; then
  echo "Enabling DNS hostnames on VPC..."
  aws --region "${REGION}" ec2 modify-vpc-attribute \
    --vpc-id $VPC_ID \
    --enable-dns-hostnames
else
  echo "DNS hostname verified as enabled..."
fi

# Create or verify first public subnet

SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "Creating first public subnet..."
  SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PUBLIC_1_CIDR_BLOCK}" \
    --availability-zone "${REGION}a" \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region "${REGION}")
else
  echo "First public subnet verified..."
fi

# Tag first public subnet

SUBNET_PUBLIC_1_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_TAG_EXISTS}" ]; then
  echo "Setting tag for first public subnet..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $SUBNET_PUBLIC_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-public-a"
else
  echo "First public subnet tag verified..."
fi

# Create or verify second public subnet

SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_ID}" ]; then
  echo "Creating second public subnet..."
  SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PUBLIC_2_CIDR_BLOCK}" \
    --availability-zone "${REGION}b" \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text)
else
  echo "Second public subnet verified..."
fi

# Create or verify second public subnet

SUBNET_PUBLIC_2_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_TAG_EXISTS}" ]; then
  echo "Setting tag for second public subnet..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $SUBNET_PUBLIC_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-public-b"
else
  echo "Second public subnet tag verified..."
fi

# Create or verify first private subnet

SUBNET_PRIVATE_1_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_ID}" ]; then
  echo "Creating first private subnet..."
  SUBNET_PRIVATE_1_ID=$(aws --region "${REGION}" ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PRIVATE_1_CIDR_BLOCK}" \
    --availability-zone "${REGION}a" \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region "${REGION}")
else
  echo "First private subnet verified..."
fi

# Tag first private subnet

SUBNET_PRIVATE_1_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_1_TAG_EXISTS}" ]; then
  echo "Setting tag for first private subnet..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $SUBNET_PRIVATE_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-private-a"
else
  echo "First private subnet tag verified..."
fi

# Create or verify second private subnet

SUBNET_PRIVATE_2_ID=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_ID}" ]; then
  echo "Creating second private subnet..."
  SUBNET_PRIVATE_2_ID=$(aws --region "${REGION}" ec2 create-subnet \
    --vpc-id $VPC_ID \
    --cidr-block "${SUBNET_PRIVATE_2_CIDR_BLOCK}" \
    --availability-zone "${REGION}b" \
    --query 'Subnet.{SubnetId:SubnetId}' \
    --output text \
    --region "${REGION}")
else
  echo "Second private subnet verified..."
fi

# Tag second private subnet

SUBNET_PRIVATE_2_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query="Subnets[*].SubnetId" \
  --output text)

if [ -z "${SUBNET_PRIVATE_2_TAG_EXISTS}" ]; then
  echo "Setting tag for second private subnet..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $SUBNET_PRIVATE_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-private-b"
else
  echo "Second private subnet tag verified..."
fi

# Create or verify internet gateway

IGW_ID=$(aws --region "${REGION}" ec2 describe-internet-gateways \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query "InternetGateways[*].InternetGatewayId" \
  --output text)

if [ -z "${IGW_ID}" ]; then
  echo "Creating internet gateway..."
  IGW_ID=$(aws --region "${REGION}" ec2 create-internet-gateway \
    --query 'InternetGateway.{InternetGatewayId:InternetGatewayId}' \
    --output text)
else
  echo "Internet gateway verified..."
fi

# Attach internet gateway to VPC

IGW_ATTACHED=$(aws --region "${REGION}" ec2 describe-internet-gateways \
  --filters "Name=attachment.vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query "InternetGateways[*].Attachments" \
  --output text)

if [ -z "${IGW_ATTACHED}" ]; then
  echo "Attaching internet gateway to VPC..."
  aws --region "${REGION}" ec2 attach-internet-gateway \
    --internet-gateway-id $IGW_ID \
    --vpc-id $VPC_ID
else
  echo "Internet gateway verified as attached to VPC..."
fi

# Tag internet gateway

IGW_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-internet-gateways \
  --filters "Name=attachment.vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}" \
  --query="InternetGateways[*].InternetGatewayId" \
  --output text)

if [ -z "${IGW_TAG_EXISTS}" ]; then
  echo "Setting tag for internet gateway..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $IGW_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}"
else
  echo "Internet gateway tag verified..."
fi

# Create a custom route table for internet gateway

IGW_ROUTE_TABLE_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ID}" ]; then
  echo "Creating a custom route table for internet gateway..."   
  IGW_ROUTE_TABLE_ID=$(aws --region "${REGION}" ec2 create-route-table \
    --vpc-id $VPC_ID \
    --query 'RouteTable.{RouteTableId:RouteTableId}' \
    --output text)
else
  echo "Custom route table for internet gateway verified..."
fi

# Tag custom route table for internet gateway

IGW_ROUTE_TABLE_TAG_EXISTS=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query="RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_TAG_EXISTS}" ]; then
  echo "Setting tag for custom route table for internet gateway..."
  aws --region "${REGION}" ec2 create-tags \
    --resources $IGW_ROUTE_TABLE_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-public"
else
  echo "Custom route table for internet gateway tag verified..."
fi

# Add route for internet gateway to the custom route table for public subnets

IGW_ROUTE_TABLE_IGW_ROUTE_TARGET=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].Routes[?GatewayId=='$IGW_ID'].GatewayId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_IGW_ROUTE_TARGET}" ]; then
  echo "Adding route for internet gateway to the custom route table for public subnets..."
  aws --region "${REGION}" ec2 create-route \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $IGW_ID \
    --route-table-id $IGW_ROUTE_TABLE_ID
else
  echo "Route for internet gateway verified in the custom route table for public subnets..."
fi

# Associate the first public subnet with the custom route table for internet gateway

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_1_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_1_ID}" ]; then
  echo "Associating the first public subnet with the custom route table for internet gateway..."
  aws --region "${REGION}" ec2 associate-route-table  \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --route-table-id $IGW_ROUTE_TABLE_ID
else
  echo "Association of the first public subnet with the custom route table for internet gateway verified..."
fi

# Associate the second public subnet with the custom route table for internet gateway

IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=vpc-id,Values=${VPC_ID}" \
    "Name=tag:Name,Values=ab2d-${CMS_ENV}-public" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PUBLIC_2_ID}'].SubnetId" \
  --output text)

if [ -z "${IGW_ROUTE_TABLE_ASSOCIATION_SUBNET_PUBLIC_2_ID}" ]; then
  echo "Associating the second public subnet with the custom route table for internet gateway..."
  aws --region "${REGION}" ec2 associate-route-table  \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --route-table-id $IGW_ROUTE_TABLE_ID
else
  echo "Association of the second public subnet with the custom route table for internet gateway verified..."
fi

# Enable Auto-assign Public IP on the first public subnet

SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-a" \
  --query "Subnets[?MapPublicIpOnLaunch].MapPublicIpOnLaunch" \
  --output text)

if [ -z "${SUBNET_PUBLIC_1_MAP_PUBLIC_IP_ON_LAUNCH}" ]; then
  echo "Enabling Auto-assign Public IP on the first public subnet..."
  aws --region "${REGION}" ec2 modify-subnet-attribute \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --map-public-ip-on-launch
else
  echo "Auto-assign Public IP verfied as enabled on the first public subnet..."
fi

# Enable Auto-assign Public IP on the second public subnet

SUBNET_PUBLIC_2_MAP_PUBLIC_IP_ON_LAUNCH=$(aws --region "${REGION}" ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-public-b" \
  --query "Subnets[?MapPublicIpOnLaunch].MapPublicIpOnLaunch" \
  --output text)

if [ -z "${SUBNET_PUBLIC_2_MAP_PUBLIC_IP_ON_LAUNCH}" ]; then
  echo "Enabling Auto-assign Public IP on the second public subnet..."  
  aws --region "${REGION}" ec2 modify-subnet-attribute \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --map-public-ip-on-launch 
else
  echo "Auto-assign Public IP verfied as enabled on the second public subnet..."
fi

# Allocate Elastic IP Address for first NAT Gateway

EIP_ALLOC_1_ID=$(aws --region "${REGION}" ec2 describe-addresses \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-nat-gateway-a" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_1_ID}" ]; then
    
  echo "Allocating Elastic IP Address for first NAT Gateway..."
    
  EIP_ALLOC_1_ID=$(aws --region "${REGION}" ec2 allocate-address \
    --domain vpc \
    --query '{AllocationId:AllocationId}' \
    --output text)

  aws --region "${REGION}" ec2 create-tags \
    --resources $EIP_ALLOC_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-nat-gateway-a"
else
  echo "Elastic IP Address for first NAT Gateway verified..."
fi

# Create first NAT Gateway

NAT_GW_1_ID=$(aws --region "${REGION}" ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-${CMS_ENV}-a" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_1_ID}" ]; then
    
  echo "Creating first NAT Gateway..."
    
  NAT_GW_1_ID=$(aws --region "${REGION}" ec2 create-nat-gateway \
    --subnet-id $SUBNET_PUBLIC_1_ID \
    --allocation-id $EIP_ALLOC_1_ID \
    --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
    --output text)

  # Add name tag to first NAT Gateway

  aws --region "${REGION}" ec2 create-tags \
    --resources $NAT_GW_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-a"

  # Wait for first NAT Gateway to become available
  
  SECONDS_COUNT=0
  LAST_SECONDS_COUNT=0
  WAIT_SECONDS=5
  STATE='PENDING'
  echo "Waiting for first NAT gateway to become available"
  until [[ $STATE == 'AVAILABLE' ]]; do
    INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
    echo $INTERVAL
    if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
      STATE=$(aws ec2 describe-nat-gateways \
        --nat-gateway-ids $NAT_GW_1_ID \
        --query 'NatGateways[*].{State:State}' \
        --output text \
        --region "${REGION}")
      STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
      echo "Waiting for first NAT gateway to become available"
      LAST_SECONDS_COUNT=$SECONDS_COUNT
    fi
    SECONDS_COUNT=$[SECONDS_COUNT+1]
    sleep 1
  done

fi

# Create a custom route table for the first NAT Gateway

ROUTE_TABLE_FOR_NGW_1_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
    "Name=vpc-id,Values=${VPC_ID}" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_1_ID}" ]; then

  echo "Creating a custom route table for the first NAT Gateway..."

  ROUTE_TABLE_FOR_NGW_1_ID=$(aws --region "${REGION}" ec2 create-route-table \
    --vpc-id $VPC_ID \
    --query 'RouteTable.{RouteTableId:RouteTableId}' \
    --output text)

  aws --region "${REGION}" ec2 create-tags \
    --resources $ROUTE_TABLE_FOR_NGW_1_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-private-a"
  
fi

# Create route to the first NAT Gateway for the custom route table

NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_1_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_NGW_1_ROUTE_TARGET}" ]; then
  echo "Creating route to the first NAT Gateway for the custom route table..."
  aws --region "${REGION}" ec2 create-route \
    --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $NAT_GW_1_ID
fi

# Associate the first private subnet with the custom route table for the first NAT Gateway

NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-a" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_1_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_1_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_1_ID}" ]; then
  echo "Associate the first private subnet with the custom route table for the first NAT Gateway..."
  aws --region "${REGION}" ec2 associate-route-table  \
    --subnet-id $SUBNET_PRIVATE_1_ID \
    --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID
fi

# Allocate Elastic IP Address for second NAT Gateway

EIP_ALLOC_2_ID=$(aws --region "${REGION}" ec2 describe-addresses \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-nat-gateway-b" \
  --query "Addresses[*].AllocationId" \
  --output text)

if [ -z "${EIP_ALLOC_2_ID}" ]; then
    
  echo "Allocating Elastic IP Address for second NAT Gateway..."
    
  EIP_ALLOC_2_ID=$(aws --region "${REGION}" ec2 allocate-address \
    --domain vpc \
    --query '{AllocationId:AllocationId}' \
    --output text)

  aws --region "${REGION}" ec2 create-tags \
    --resources $EIP_ALLOC_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-nat-gateway-b"
else
  echo "Elastic IP Address for second NAT Gateway verified..."
fi

# Create second NAT Gateway

NAT_GW_2_ID=$(aws --region "${REGION}" ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-${CMS_ENV}-b" \
  --query "NatGateways[*].NatGatewayId" \
  --output text)

if [ -z "${NAT_GW_2_ID}" ]; then
    
  echo "Creating second NAT Gateway..."
    
  NAT_GW_2_ID=$(aws --region "${REGION}" ec2 create-nat-gateway \
    --subnet-id $SUBNET_PUBLIC_2_ID \
    --allocation-id $EIP_ALLOC_2_ID \
    --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
    --output text)

  # Add name tag to second NAT Gateway

  aws --region "${REGION}" ec2 create-tags \
    --resources $NAT_GW_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-b"

  # Wait for second NAT Gateway to become available
  
  SECONDS_COUNT=0
  LAST_SECONDS_COUNT=0
  WAIT_SECONDS=5
  STATE='PENDING'
  echo "Waiting for second NAT gateway to become available"
  until [[ $STATE == 'AVAILABLE' ]]; do
    INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
    echo $INTERVAL
    if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
      STATE=$(aws --region "${REGION}" ec2 describe-nat-gateways \
        --nat-gateway-ids $NAT_GW_2_ID \
        --query 'NatGateways[*].{State:State}' \
        --output text)
      STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
      echo "Waiting for second NAT gateway to become available"
      LAST_SECONDS_COUNT=$SECONDS_COUNT
    fi
    SECONDS_COUNT=$[SECONDS_COUNT+1]
    sleep 1
  done

fi

# Create a custom route table for the second NAT Gateway

ROUTE_TABLE_FOR_NGW_2_ID=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
    "Name=vpc-id,Values=${VPC_ID}" \
  --query "RouteTables[*].RouteTableId" \
  --output text)

if [ -z "${ROUTE_TABLE_FOR_NGW_2_ID}" ]; then

  echo "Creating a custom route table for the second NAT Gateway..."

  ROUTE_TABLE_FOR_NGW_2_ID=$(aws --region "${REGION}" ec2 create-route-table \
    --vpc-id $VPC_ID \
    --query 'RouteTable.{RouteTableId:RouteTableId}' \
    --output text)

  aws --region "${REGION}" ec2 create-tags \
    --resources $ROUTE_TABLE_FOR_NGW_2_ID \
    --tags "Key=Name,Value=ab2d-${CMS_ENV}-private-b"
  
fi

# Create route to the second NAT Gateway for the custom route table

NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET=$(aws --region "${REGION}" ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "RouteTables[*].Routes[?NatGatewayId=='$NAT_GW_2_ID'].NatGatewayId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_NGW_2_ROUTE_TARGET}" ]; then
  echo "Creating route to the second NAT Gateway for the custom route table..."
  aws --region "${REGION}" ec2 create-route \
    --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
    --destination-cidr-block 0.0.0.0/0 \
    --gateway-id $NAT_GW_2_ID
fi

# Associate the second private subnet with the custom route table for the second NAT Gateway

NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID=$(aws ec2 describe-route-tables \
  --filters "Name=tag:Name,Values=ab2d-${CMS_ENV}-private-b" \
  --query "RouteTables[*].Associations[?SubnetId=='${SUBNET_PRIVATE_2_ID}'].SubnetId" \
  --output text)

if [ -z "${NGW_2_ROUTE_TABLE_ASSOCIATION_SUBNET_PRIVATE_2_ID}" ]; then
  echo "Associate the second private subnet with the custom route table for the second NAT Gateway..."
  aws ec2 --region "${REGION}" associate-route-table  \
    --subnet-id $SUBNET_PRIVATE_2_ID \
    --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID
fi

# Done

echo "The VPC ID is: $VPC_ID"
echo "Done."
