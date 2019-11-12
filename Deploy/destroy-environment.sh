#!/bin/bash
set -e #Exit on first error
# set -x #Be verbose

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
  --shared-environment=*)
  SHARED_ENVIRONMENT="${i#*=}"
  CMS_SHARED_ENV=$(echo $SHARED_ENVIRONMENT | tr '[:upper:]' '[:lower:]')
  shift # past argument=value
  ;;
  --keep-network)
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
if [ -z "${ENVIRONMENT}" ] || [ -z "${SHARED_ENVIRONMENT}" ]; then
  echo "Try running the script like one of these options:"
  echo "./destroy-environment.sh --environment=dev --shared-environment=shared"
  echo "./destroy-environment.sh --environment=dev --shared-environment=shared --keep-ami"
  echo "./destroy-environment.sh --environment=dev --shared-environment=shared --keep-network"
  echo "./destroy-environment.sh --environment=dev --shared-environment=shared --keep-ami --keep-network"
  exit 1
fi

#
# Set environment
#

export AWS_PROFILE="${CMS_ENV}"

#
# Change to target environment directory
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

#
# Destroy the api and worker modules
#

# Change to the environment directory
LOAD_BALANCERS_EXIST=$(aws --region us-east-1 elbv2 describe-load-balancers \
  --query 'LoadBalancers[*].[LoadBalancerArn]' \
  --output text)
if [ -n "${LOAD_BALANCERS_EXIST}" ]; then
  ALB_ARN=$(aws --region us-east-1 elbv2 describe-load-balancers \
    --name=ab2d-$CMS_ENV \
    --query 'LoadBalancers[*].[LoadBalancerArn]' \
    --output text)
fi

# Turn off "delete protection" for the application load balancer
echo "Turn off 'delete protection' for the application load balancer..."
if [ -n "${ALB_ARN}" ]; then
  aws --region us-east-1 elbv2 modify-load-balancer-attributes \
    --load-balancer-arn $ALB_ARN \
    --attributes Key=deletion_protection.enabled,Value=false
fi

# Destroy the environment of the "worker" module
echo "Destroying worker components..."
terraform destroy \
  --target module.worker --auto-approve

# Destroy the environment of the "api" module
echo "Destroying API components..."
terraform destroy \
  --target module.api --auto-approve

#
# Destroy efs module
#

# Destroy the environment of the "efs" module
echo "Destroying EFS components..."
terraform destroy \
  --target module.efs --auto-approve

#
# Change to shared environment directory
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

#
# TEMPORARILY COMMENT OUT BEGIN
#

# #
# # Destroy controller module
# #

# echo "Destroying controller..."
# terraform destroy \
#   --target module.controller \
#   --auto-approve

# #
# # Destroy db module
# #

# echo "Destroying DB components..."
# terraform destroy \
#   --target module.db --auto-approve

# #
# # Destroy all S3 buckets except for the "ab2d-automation" bucket
# #

# # Destroy the environment of the "s3" module

# echo "Destroying S3 components..."

# terraform destroy \
#   --target module.s3 --auto-approve

# aws s3 rm s3://ab2d-cloudtrail/ab2d-$CMS_ENV \
#   --recursive

# #
# # Disable "kms" key
# #

# # Rerun db destroy again to ensure that it is in correct state
# # - this is a workaround that prevents the kms module from raising an eror sporadically
# echo "Rerun module.db destroy to ensure proper state..."
# terraform destroy \
#   --target module.db --auto-approve

# # Destroy the KMS module
# echo "Disabling KMS key..."
# KMS_KEY_ID=$(aws kms describe-key --key-id alias/ab2d-kms --query="KeyMetadata.KeyId")
# if [ -n "$KMS_KEY_ID" ]; then
#   cd "${START_DIR}"
#   cd python3
#   ./disable-kms-key.py $KMS_KEY_ID
#   cd "${START_DIR}"
#   cd terraform/environments/ab2d-$CMS_ENV
# fi

#
# TEMPORARILY COMMENT OUT END
#

#
# Destroy terraform state information
#

# *** TO DO ***: Need to ensure that this only occurs when there are no existing environments

# cd "${START_DIR}"
# cd terraform
# if [ -z "${NO_EXISTING_ENVIRONMENT}" ]; then
#   aws s3 rm s3://ab2d-automation \
#     --recursive
#   rm -rf .terraform
# fi

#
# Deregister the application AMI
#

# Get application AMI ID (if exists)
APPLICATION_AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${APPLICATION_AMI_ID}" ]; then
  if [ -n "${KEEP_AMI}" ]; then
    echo "No existing application AMI to preserve..."
  fi
else
  if [ -z "${KEEP_AMI}" ]; then
    echo "Deregistering application AMI..."
    aws --region us-east-1 ec2 deregister-image \
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
  --filters "Name=tag:Name,Values=ab2d-jenkins-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${JENKINS_AMI_ID}" ]; then
  if [ -n "${KEEP_AMI}" ]; then
    echo "No existing Jenkins AMI to preserve..."
  fi
else
  if [ -z "${KEEP_AMI}" ]; then
    echo "Deregistering Jenkins AMI..."
    aws --region us-east-1 ec2 deregister-image \
      --image-id $JENKINS_AMI_ID
  else
    echo "Preserving Jenkins AMI..."
  fi
fi

#
# Exit if keeping the networking
#

if [ -n "$KEEP_NETWORK" ]; then
    echo "Preserving networking..."
    echo "Done"
    exit
fi

#
# Delete the NAT gateways
#

# Delete first NAT gateway

NAT_GW_1_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-ngw-1" "Name=state,Values=available" \
  --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
  --output text)

if [ -n "${NAT_GW_1_ID}" ]; then
    
  echo "Deleting first NAT gateway..."

  # Delete the name tag to second NAT Gateway

  aws ec2 delete-tags \
    --resources $NAT_GW_1_ID \
    --tags "Key=Name" \
    --region us-east-1

  # Delete first NAT gateway
  
  aws --region us-east-1 ec2 delete-nat-gateway \
    --nat-gateway-id $NAT_GW_1_ID
  
fi

# Delete second NAT gateway

NAT_GW_2_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
  --filter "Name=tag:Name,Values=ab2d-ngw-2" "Name=state,Values=available" \
  --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
  --output text)

if [ -n "${NAT_GW_2_ID}" ]; then

  echo "Deleting second NAT gateway..."

  # Delete the name tag to second NAT Gateway

  aws ec2 delete-tags \
    --resources $NAT_GW_2_ID \
    --tags "Key=Name" \
    --region us-east-1

  # Delete second NAT gateway

  aws --region us-east-1 ec2 delete-nat-gateway \
    --nat-gateway-id $NAT_GW_2_ID
  
fi

# Wait for NAT gateways to all enter the deleted state

echo "Wait for NAT gateways to all enter the deleted state..."

NAT_GATEWAYS_STATE="NOT_EMPTY"
RETRIES_NGW=0

while [ -n "${NAT_GATEWAYS_STATE}" ]; do
  echo "Checking status of NAT Gateways as they are deleting..."
  if [ "$RETRIES_NGW" != "15" ]; then
    NAT_GATEWAYS_STATE=$(aws --region us-east-1 ec2 describe-nat-gateways \
      --filter "Name=state,Values=available,pending,deleting" \
	       "Name=nat-gateway-id,Values='$NAT_GW_1_ID','$NAT_GW_2_ID'" \
      --query 'NatGateways[*].{NatGatewayId:NatGatewayId,State:State}' \
      --output text)
    if [ -n "${NAT_GATEWAYS_STATE}" ]; then
      echo "NAT_GATEWAYS_STATE="$NAT_GATEWAYS_STATE
      echo "Retry in 60 seconds..."
      sleep 60
      RETRIES_NGW=$(expr $RETRIES_NGW + 1)
    else
      echo "NAT gateways successfully deleted..."
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
  --filter "Name=tag:Name,Values=ab2d-ngw-eip-1" \
  --query 'Addresses[*].[AllocationId]' \
  --output text)

if [ -n "${NGW_EIP_1_ALLOCATION_ID}" ]; then
  echo "Release first Elastic IP address..."
  aws --region us-east-1 ec2 release-address \
    --allocation-id $NGW_EIP_1_ALLOCATION_ID
fi

# Release second Elastic IP address

NGW_EIP_2_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
  --filter "Name=tag:Name,Values=ab2d-ngw-eip-2" \
  --query 'Addresses[*].[AllocationId]' \
  --output text)

if [ -n "${NGW_EIP_2_ALLOCATION_ID}" ]; then
  echo "Release second Elastic IP address..."
  aws --region us-east-1 ec2 release-address \
    --allocation-id $NGW_EIP_2_ALLOCATION_ID
fi
  
#
# Disassociate subnets from the NAT Gateway route tables
#

# Disassociate subnets from the first NAT Gateway route table

NGW_RT_1_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-ngw-rt-1" \
  --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
  --output text)

IFS=$' ' read -ra NGW_RT_1_ASSOCIATION_ID <<< "$NGW_RT_1_ASSOCIATION_ID"

while [ -n "${NGW_RT_1_ASSOCIATION_ID}" ]; do
    
  if [ -n "${NGW_RT_1_ASSOCIATION_ID}" ]; then
    echo "Disassociating a subnet from the first NAT Gateway route table..."
    aws --region us-east-1 ec2 disassociate-route-table \
      --association-id $NGW_RT_1_ASSOCIATION_ID

    NGW_RT_1_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
      --filter "Name=tag:Name,Values=ab2d-ngw-rt-1" \
      --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
      --output text)

    IFS=$' ' read -ra NGW_RT_1_ASSOCIATION_ID <<< "$NGW_RT_1_ASSOCIATION_ID"

  fi

done

# Disassociate the second subnet from the second NAT Gateway route table

NGW_RT_2_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-ngw-rt-2" \
  --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
  --output text)

IFS=$' ' read -ra NGW_RT_2_ASSOCIATION_ID <<< "$NGW_RT_2_ASSOCIATION_ID"

while [ -n "${NGW_RT_2_ASSOCIATION_ID}" ]; do
    
  if [ -n "${NGW_RT_2_ASSOCIATION_ID}" ]; then
    echo "Disassociating a subnet from the second NAT Gateway route table..."
    aws --region us-east-1 ec2 disassociate-route-table \
      --association-id $NGW_RT_2_ASSOCIATION_ID
  fi

  NGW_RT_2_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
    --filter "Name=tag:Name,Values=ab2d-ngw-rt-2" \
    --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
    --output text)

  IFS=$' ' read -ra NGW_RT_2_ASSOCIATION_ID <<< "$NGW_RT_2_ASSOCIATION_ID"

done

#
# Delete route tables
#

# Delete the first NAT Gateway route table for the first NAT gateway

NGW_RT_1_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-ngw-rt-1" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

if [ -n "${NGW_RT_1_ID}" ]; then
  echo "Deleting the first NAT Gateway route table for the first NAT gateway..."
  aws --region us-east-1 ec2 delete-route-table \
    --route-table-id $NGW_RT_1_ID
fi
   
# Delete the second NAT Gateway route table for the second NAT gateway

NGW_RT_2_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-ngw-rt-2" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

if [ -n "${NGW_RT_2_ID}" ]; then
  echo "Deleting the second NAT Gateway route table for the second NAT gateway..."
  aws --region us-east-1 ec2 delete-route-table \
    --route-table-id $NGW_RT_2_ID
fi

#
# Disassociate the subnets in reverse order from the Internet Gateway route table
#

# Disassociate the second subnet from the Internet Gateway route table

IGW_RT_ASSOCIATION_ID_2=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-igw-rt" \
  --query 'RouteTables[*].Associations[1].[RouteTableAssociationId]' \
  --output text)

if [ -n "${IGW_RT_ASSOCIATION_ID_2}" ]; then
  echo "Disassociating the second subnet from the Internet Gateway route table..."
  aws --region us-east-1 ec2 disassociate-route-table \
    --association-id $IGW_RT_ASSOCIATION_ID_2
fi

# Disassociate the first subnet from the Internet Gateway route table

IGW_RT_ASSOCIATION_ID_1=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-igw-rt" \
  --query 'RouteTables[*].Associations[0].[RouteTableAssociationId]' \
  --output text)

if [ -n "${IGW_RT_ASSOCIATION_ID_1}" ]; then
  echo "Disassociating the first subnet from the Internet Gateway route table..."
  aws --region us-east-1 ec2 disassociate-route-table \
    --association-id $IGW_RT_ASSOCIATION_ID_1
fi

#
# Delete the Internet Gateway route table
#

IGW_RT_ID=$(aws --region us-east-1 ec2 describe-route-tables \
  --filter "Name=tag:Name,Values=ab2d-igw-rt" \
  --query 'RouteTables[*].[RouteTableId]' \
  --output text)

if [ -n "${IGW_RT_ID}" ]; then
  echo "Deleting the Internet Gateway route table..."
  aws --region us-east-1 ec2 delete-route-table \
    --route-table-id $IGW_RT_ID
fi

# Determine if internet gateway is being used by a different environment

IGW_IN_USE_IN_OTHER_ENV=$(aws --region us-east-1 ec2 describe-route-tables \
  --query "RouteTables[*].Routes[?GatewayId == '$IGW_ID'].GatewayId" \
  --output text)

if [ -n "${VPC_ID}" ] && [ -n "${IGW_ID}" ] && [ -z "$IGW_IN_USE_IN_OTHER_ENV" ]; then
  echo "Detaching Internet Gateway from VPC..."
  aws --region us-east-1 ec2 detach-internet-gateway \
    --vpc-id $VPC_ID \
    --internet-gateway-id $IGW_ID
fi

#
# Detach Internet Gateway from VPC
#

if [ -n "${IGW_ID}" ] && [ -z "$IGW_IN_USE_IN_OTHER_ENV" ]; then
    
  VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=ab2d-vpc" \
    --query 'Vpcs[*].[VpcId]' \
    --output text)

  IGW_ID=$(aws --region us-east-1 ec2 describe-internet-gateways \
    --filter "Name=tag:Name,Values=ab2d-igw" \
    --query 'InternetGateways[*].[InternetGatewayId]' \
    --output text)

fi

#
# Delete Internet Gateway
#

if [ -n "${IGW_ID}" ] && [ -z "$IGW_IN_USE_IN_OTHER_ENV" ]; then
  echo "Deleting Internet Gateway..."
  aws --region us-east-1 ec2 delete-internet-gateway \
    --internet-gateway-id $IGW_ID
fi

#
# Delete subnets
#

# Delete the first private subnet

SUBNET_PRIVATE_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-01" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

if [ -n "${SUBNET_PRIVATE_1_ID}" ]; then
  echo "Deleting the first private subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PRIVATE_1_ID
fi

# Delete the second private subnet

SUBNET_PRIVATE_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-02" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

if [ -n "${SUBNET_PRIVATE_2_ID}" ]; then
  echo "Deleting the second private subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PRIVATE_2_ID
fi

# Delete the first public subnet

SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=ab2d-public-subnet-01" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

if [ -n "${SUBNET_PUBLIC_1_ID}" ]; then
  echo "Deleting the first public subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PUBLIC_1_ID
fi

# Delete the second public subnet

SUBNET_PUBLIC_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
  --filter "Name=tag:Name,Values=ab2d-public-subnet-02" \
  --query 'Subnets[*].[SubnetId]' \
  --output text)

if [ -n "${SUBNET_PUBLIC_2_ID}" ]; then
  echo "Deleting the second public subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PUBLIC_2_ID
fi

#
# Done
#

echo "Done"

#
# Delete any orphaned components using AWS CLI
# - orphaned components may exist if their were automation issues with terraform
#

# Delete orphaned EFS (if exists)

EFS_FS_ID=$(aws efs describe-file-systems \
  --query="FileSystems[?CreationToken=='ab2d-${CMS_ENV}-efs'].FileSystemId" \
  --output=text)

if [ -n "${EFS_FS_ID}" ]; then
  aws efs delete-file-system \
    --file-system-id "${EFS_FS_ID}"
fi
