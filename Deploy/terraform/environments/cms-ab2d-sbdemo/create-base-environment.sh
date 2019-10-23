#!/bin/bash

#
# Create base AWS networking
#

# Change to working directory
cd "$(dirname "$0")"

# Set environment
export AWS_PROFILE="sbdemo"
export ENVIRONMENT="sbdemo"
export CMS_ENV="SBDEMO"

# Check if VPN already exists
VPC_EXISTS=$(aws --region us-east-1 ec2 describe-vpcs --output text \
  --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-VPC")

# Continue process if VPC does not exist
if [ -z "${VPC_EXISTS}" ]; then
  # Create VPC
  VPC_ID=$(aws ec2 create-vpc \
    --cidr-block 10.124.0.0/16 \
    --query 'Vpc.{VpcId:VpcId}' \
    --output text \
    --region us-east-1)
  echo "Creating VPC..."
else # Exit shell script if VPC already exists
  echo "Skipping network creation since VPC already exists."
  exit
fi

# Enable DNS hostname on VPC
aws ec2 modify-vpc-attribute \
  --vpc-id $VPC_ID \
  --enable-dns-hostnames

# Add name tag to VPC
aws --region us-east-1 ec2 create-tags \
  --resources $VPC_ID \
  --tags "Key=Name,Value=AB2D-SBDEMO-VPC"

# Display public subnet stage
echo "Creating public subnets..."

# Create first public subnet
SUBNET_PUBLIC_1_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.124.1.0/24 \
  --availability-zone us-east-1a \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text \
  --region us-east-1)

# Add name tag to first public subnet
aws ec2 create-tags \
  --resources $SUBNET_PUBLIC_1_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-PUBLIC-SUBNET-01" \
  --region us-east-1

# Create second public subnet
SUBNET_PUBLIC_2_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.124.2.0/24 \
  --availability-zone us-east-1b \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text \
  --region us-east-1)

# Add name tag to second public subnet
aws ec2 create-tags \
  --resources $SUBNET_PUBLIC_2_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-PUBLIC-SUBNET-02" \
  --region us-east-1

# Create third public subnet
SUBNET_PUBLIC_3_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.124.3.0/24 \
  --availability-zone us-east-1c \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text \
  --region us-east-1)

# Add name tag to third public subnet
aws ec2 create-tags \
  --resources $SUBNET_PUBLIC_3_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-PUBLIC-SUBNET-03" \
  --region us-east-1

# Display private subnet stage
echo "Creating private subnets..."

# Create first private subnet
SUBNET_PRIVATE_1_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.124.4.0/24 \
  --availability-zone us-east-1d \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text \
  --region us-east-1)

# Add name tag to first private subnet
aws ec2 create-tags \
  --resources $SUBNET_PRIVATE_1_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-PRIVATE-SUBNET-01" \
  --region us-east-1

# Create second private subnet
SUBNET_PRIVATE_2_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.124.5.0/24 \
  --availability-zone us-east-1e \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text \
  --region us-east-1)

# Add name tag to second private subnet
aws ec2 create-tags \
  --resources $SUBNET_PRIVATE_2_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-PRIVATE-SUBNET-02" \
  --region us-east-1

# Create third private subnet
SUBNET_PRIVATE_3_ID=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.124.6.0/24 \
  --availability-zone us-east-1f \
  --query 'Subnet.{SubnetId:SubnetId}' \
  --output text \
  --region us-east-1)

# Add name tag to third private subnet
aws ec2 create-tags \
  --resources $SUBNET_PRIVATE_3_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-PRIVATE-SUBNET-03" \
  --region us-east-1

# Display public subnet stage
echo "Creating internet gateway..."

# Create internet gateway
IGW_ID=$(aws ec2 create-internet-gateway \
  --query 'InternetGateway.{InternetGatewayId:InternetGatewayId}' \
  --output text \
  --region us-east-1)

# Add name tag to internet gateway
aws ec2 create-tags \
  --resources $IGW_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-IGW" \
  --region us-east-1

# Attach internet gateway to VPC
aws --region us-east-1 ec2 attach-internet-gateway \
  --internet-gateway-id $IGW_ID \
  --vpc-id $VPC_ID

# Create a custom route table for internet gateway
IGW_ROUTE_TABLE_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.{RouteTableId:RouteTableId}' \
  --output text \
  --region us-east-1)

# Add name tag to route table for internet gateway
aws ec2 create-tags \
  --resources $IGW_ROUTE_TABLE_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-IGW-RT" \
  --region us-east-1

# Add route for internet gateway to the custom route table for public subnets
aws --region us-east-1 ec2 create-route \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID \
  --route-table-id $IGW_ROUTE_TABLE_ID

# Associate the first public subnet with the custom route table for internet gateway
aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PUBLIC_1_ID \
  --route-table-id $IGW_ROUTE_TABLE_ID \
  --region us-east-1

# Associate the second public subnet with the custom route table for internet gateway
aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PUBLIC_2_ID \
  --route-table-id $IGW_ROUTE_TABLE_ID \
  --region us-east-1

# Associate the third public subnet with the custom route table for internet gateway
aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PUBLIC_3_ID \
  --route-table-id $IGW_ROUTE_TABLE_ID \
  --region us-east-1

# Enable Auto-assign Public IP on the first public subnet
aws ec2 modify-subnet-attribute \
  --subnet-id $SUBNET_PUBLIC_1_ID \
  --map-public-ip-on-launch \
  --region us-east-1

# Enable Auto-assign Public IP on the second public subnet
aws ec2 modify-subnet-attribute \
  --subnet-id $SUBNET_PUBLIC_2_ID \
  --map-public-ip-on-launch \
  --region us-east-1

# Enable Auto-assign Public IP on the third public subnet
aws ec2 modify-subnet-attribute \
  --subnet-id $SUBNET_PUBLIC_3_ID \
  --map-public-ip-on-launch \
  --region us-east-1

# Display NAT gatways stage
echo "Creating NAT gateways..."

# Allocate Elastic IP Address for first NAT Gateway
EIP_ALLOC_1_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --query '{AllocationId:AllocationId}' \
  --output text \
  --region us-east-1)

# Add name tag to Elastic IP Address for first NAT Gateway
aws ec2 create-tags \
  --resources $EIP_ALLOC_1_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-EIP-1" \
  --region us-east-1

# Create first NAT Gateway
NAT_GW_1_ID=$(aws ec2 create-nat-gateway \
  --subnet-id $SUBNET_PUBLIC_1_ID \
  --allocation-id $EIP_ALLOC_1_ID \
  --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
  --output text \
  --region us-east-1)

# Add name tag to first NAT Gateway
aws ec2 create-tags \
  --resources $NAT_GW_1_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-1" \
  --region us-east-1

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
      --region us-east-1)
    STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
    echo "Waiting for first NAT gateway to become available"
    LAST_SECONDS_COUNT=$SECONDS_COUNT
  fi
  SECONDS_COUNT=$[SECONDS_COUNT+1]
  sleep 1
done

# Create a custom route table for the first NAT Gateway
ROUTE_TABLE_FOR_NGW_1_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.{RouteTableId:RouteTableId}' \
  --output text \
  --region us-east-1)

# Add name tag for the custom route table for the first NAT Gateway
aws ec2 create-tags \
  --resources $ROUTE_TABLE_FOR_NGW_1_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-RT-1" \
  --region us-east-1

# Create route to the first NAT Gateway for the custom route table
aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $NAT_GW_1_ID \
  --region us-east-1

# Associate the first private subnet with the custom route table for the first NAT Gateway
aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PRIVATE_1_ID \
  --route-table-id $ROUTE_TABLE_FOR_NGW_1_ID \
  --region us-east-1

# Allocate Elastic IP Address for second NAT Gateway
EIP_ALLOC_2_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --query '{AllocationId:AllocationId}' \
  --output text \
  --region us-east-1)

# Add name tag to Elastic IP Address for second NAT Gateway
aws ec2 create-tags \
  --resources $EIP_ALLOC_2_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-EIP-2" \
  --region us-east-1

# Create second NAT Gateway
NAT_GW_2_ID=$(aws ec2 create-nat-gateway \
  --subnet-id $SUBNET_PUBLIC_2_ID \
  --allocation-id $EIP_ALLOC_2_ID \
  --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
  --output text \
  --region us-east-1)

# Add name tag to second NAT Gateway
aws ec2 create-tags \
  --resources $NAT_GW_2_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-2" \
  --region us-east-1

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
    STATE=$(aws ec2 describe-nat-gateways \
      --nat-gateway-ids $NAT_GW_2_ID \
      --query 'NatGateways[*].{State:State}' \
      --output text \
      --region us-east-1)
    STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
    echo "Waiting for second NAT gateway to become available"
    LAST_SECONDS_COUNT=$SECONDS_COUNT
  fi
  SECONDS_COUNT=$[SECONDS_COUNT+1]
  sleep 1
done

# Create a custom route table for the second NAT Gateway
ROUTE_TABLE_FOR_NGW_2_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.{RouteTableId:RouteTableId}' \
  --output text \
  --region us-east-1)

# Add name tag for the custom route table for the first NAT Gateway
aws ec2 create-tags \
  --resources $ROUTE_TABLE_FOR_NGW_2_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-RT-2" \
  --region us-east-1

# Create route to the second NAT Gateway for the custom route table
aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $NAT_GW_2_ID \
  --region us-east-1

# Associate the second private subnet with the custom route table for the second NAT Gateway
aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PRIVATE_2_ID \
  --route-table-id $ROUTE_TABLE_FOR_NGW_2_ID \
  --region us-east-1

# Allocate Elastic IP Address for third NAT Gateway
EIP_ALLOC_3_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --query '{AllocationId:AllocationId}' \
  --output text \
  --region us-east-1)

# Add name tag to Elastic IP Address for third NAT Gateway
aws ec2 create-tags \
  --resources $EIP_ALLOC_3_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-EIP-3" \
  --region us-east-1

# Create third NAT Gateway
NAT_GW_3_ID=$(aws ec2 create-nat-gateway \
  --subnet-id $SUBNET_PUBLIC_3_ID \
  --allocation-id $EIP_ALLOC_3_ID \
  --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
  --output text \
  --region us-east-1)

# Add name tag to third NAT Gateway
aws ec2 create-tags \
  --resources $NAT_GW_3_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-3" \
  --region us-east-1

# Wait for third NAT Gateway to become available
SECONDS_COUNT=0
LAST_SECONDS_COUNT=0
WAIT_SECONDS=5
STATE='PENDING'
echo "Waiting for third NAT gateway to become available"
until [[ $STATE == 'AVAILABLE' ]]; do
  INTERVAL=$[SECONDS_COUNT-LAST_SECONDS_COUNT]
  echo $INTERVAL
  if [[ $INTERVAL -ge $WAIT_SECONDS ]]; then
    STATE=$(aws ec2 describe-nat-gateways \
      --nat-gateway-ids $NAT_GW_3_ID \
      --query 'NatGateways[*].{State:State}' \
      --output text \
      --region us-east-1)
    STATE=$(echo $STATE | tr '[:lower:]' '[:upper:]')
    echo "Waiting for third NAT gateway to become available"
    LAST_SECONDS_COUNT=$SECONDS_COUNT
  fi
  SECONDS_COUNT=$[SECONDS_COUNT+1]
  sleep 1
done

# Create a custom route table for the third NAT Gateway
ROUTE_TABLE_FOR_NGW_3_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.{RouteTableId:RouteTableId}' \
  --output text \
  --region us-east-1)

# Add name tag for the custom route table for the first NAT Gateway
aws ec2 create-tags \
  --resources $ROUTE_TABLE_FOR_NGW_3_ID \
  --tags "Key=Name,Value=AB2D-$CMS_ENV-NGW-RT-3" \
  --region us-east-1

# Create route to the third NAT Gateway for the custom route table
aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_FOR_NGW_3_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $NAT_GW_3_ID \
  --region us-east-1

# Associate the third private subnet with the custom route table for the third NAT Gateway
aws ec2 associate-route-table  \
  --subnet-id $SUBNET_PRIVATE_3_ID \
  --route-table-id $ROUTE_TABLE_FOR_NGW_3_ID \
  --region us-east-1

# Recreate the ".auto.tfvars" file for the environment
echo 'vpc_id = "'$VPC_ID'"' \
  > $ENVIRONMENT.auto.tfvars
echo 'private_subnet_ids = ["'$SUBNET_PRIVATE_1_ID'","'$SUBNET_PRIVATE_2_ID'","'$SUBNET_PRIVATE_3_ID'"]' \
  >> $ENVIRONMENT.auto.tfvars
echo 'deployment_controller_subnet_ids = ["'$SUBNET_PUBLIC_1_ID'","'$SUBNET_PUBLIC_2_ID'","'$SUBNET_PUBLIC_3_ID'"]' \
  >> $ENVIRONMENT.auto.tfvars
