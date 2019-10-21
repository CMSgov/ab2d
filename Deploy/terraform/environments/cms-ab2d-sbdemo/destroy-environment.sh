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
      --image-id $APPLICATION_AMI_ID
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

#
# Disassociate the subnet from the NAT Gateway route tables
#

# Disassociate the first subnet from the first NAT Gateway route table

NGW_RT_1_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-RT-1" \
  --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
  --output text)

aws --region us-east-1 ec2 disassociate-route-table \
  --association-id $NGW_RT_1_ASSOCIATION_ID

# Disassociate the second subnet from the second NAT Gateway route table

NGW_RT_2_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-RT-2" \
  --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
  --output text)

aws --region us-east-1 ec2 disassociate-route-table \
  --association-id $NGW_RT_2_ASSOCIATION_ID

# Disassociate the third subnet from the third NAT Gateway route table

NGW_RT_3_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-RT-3" \
  --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
  --output text)

aws --region us-east-1 ec2 disassociate-route-table \
  --association-id $NGW_RT_3_ASSOCIATION_ID

#
# Delete route tables
#

# Delete the first NAT Gateway route table for the first NAT gateway

NGW_RT_1_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-RT-1" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

aws --region us-east-1 ec2 delete-route-table \
  --route-table-id $NGW_RT_1_ID

# Delete the second NAT Gateway route table for the second NAT gateway

NGW_RT_2_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-RT-2" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

aws --region us-east-1 ec2 delete-route-table \
  --route-table-id $NGW_RT_2_ID

# Delete the third NAT Gateway route table for the third NAT gateway

NGW_RT_3_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-RT-3" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

aws --region us-east-1 ec2 delete-route-table \
  --route-table-id $NGW_RT_3_ID

#
# Disassociate the subnet from the Internet Gateway route table
#

# Disassociate the first subnet from the Internet Gateway route table

IGW_RT_ASSOCIATION_ID_1=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-IGW-RT" \
  --query 'RouteTables[*].Associations[0].[RouteTableAssociationId]' \
  --output text)

aws --region us-east-1 ec2 disassociate-route-table \
  --association-id $IGW_RT_ASSOCIATION_ID_1

# Disassociate the second subnet from the Internet Gateway route table

IGW_RT_ASSOCIATION_ID_2=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-IGW-RT" \
  --query 'RouteTables[*].Associations[1].[RouteTableAssociationId]' \
  --output text)

aws --region us-east-1 ec2 disassociate-route-table \
  --association-id $IGW_RT_ASSOCIATION_ID_2

# Disassociate the third subnet from the Internet Gateway route table

IGW_RT_ASSOCIATION_ID_3=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-IGW-RT" \
  --query 'RouteTables[*].Associations[2].[RouteTableAssociationId]' \
  --output text)

aws --region us-east-1 ec2 disassociate-route-table \
  --association-id $IGW_RT_ASSOCIATION_ID_3

#
# Delete the Internet Gateway route table
#

IGW_RT_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-IGW-RT" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

aws --region us-east-1 ec2 delete-route-table \
  --route-table-id $IGW_RT_ID

#
# Detach Internet Gateway from VPC
#

VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-VPC" \
  --query 'Vpcs[*].[VpcId]' \
  --output text)

IGW_ID=$(aws --region us-east-1 ec2 describe-internet-gateways \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-IGW" \
  --query 'InternetGateways[*].[InternetGatewayId]' \
  --output text)

aws --region us-east-1 ec2 detach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID

#
# Delete Internet Gateway
#

aws --region us-east-1 ec2 delete-internet-gateway \
  --internet-gateway-id $IGW_ID

#
# Delete subnets
#

# Delete the first private subnet

SUBNET_PRIVATE_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-PRIVATE-SUBNET-01" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

aws --region us-east-1 ec2 delete-subnet \
  --subnet-id $SUBNET_PRIVATE_1_ID

# Delete the second private subnet

SUBNET_PRIVATE_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-PRIVATE-SUBNET-02" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

aws --region us-east-1 ec2 delete-subnet \
  --subnet-id $SUBNET_PRIVATE_2_ID

# Delete the third private subnet

SUBNET_PRIVATE_3_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-PRIVATE-SUBNET-03" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

aws --region us-east-1 ec2 delete-subnet \
  --subnet-id $SUBNET_PRIVATE_3_ID

# Delete the first public subnet

SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-PUBLIC-SUBNET-01" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

aws --region us-east-1 ec2 delete-subnet \
  --subnet-id $SUBNET_PUBLIC_1_ID

# Delete the second public subnet

SUBNET_PUBLIC_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-PUBLIC-SUBNET-02" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

aws --region us-east-1 ec2 delete-subnet \
  --subnet-id $SUBNET_PUBLIC_2_ID

# Delete the third public subnet

SUBNET_PUBLIC_3_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-PUBLIC-SUBNET-03" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

aws --region us-east-1 ec2 delete-subnet \
  --subnet-id $SUBNET_PUBLIC_3_ID

#
# Delete VPC
#

aws --region us-east-1 ec2 delete-vpc \
  --vpc-id $VPC_ID
