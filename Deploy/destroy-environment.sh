#!/bin/bash
set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set default values
#

export DEBUG_LEVEL="WARN"

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
  --debug-level=*)
  DEBUG_LEVEL=$(echo ${i#*=} | tr '[:lower:]' '[:upper:]')
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
# Configure terraform
#

# Reset logging

echo "Setting terraform debug level to $DEBUG_LEVEL..."
export TF_LOG=$DEBUG_LEVEL
export TF_LOG_PATH=/var/log/terraform/tf.log
rm -f /var/log/terraform/tf.log

#
# Change to target environment directory
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

# Define the environment count function (exluding the shared environment)

env_count(){
  PRIVATE_SUBNETS_PER_ENVIRONMENT=2

  PRIVATE_SUBNET_COUNT=$(aws ec2 describe-subnets \
    --filters "Name=tag:Name,Values=ab2d-*-private-subnet*" \
    --query "Subnets[*].Tags[?Key == 'Name'].Value" \
    --output json \
    | jq '. | length')

  ENV_COUNT=$((PRIVATE_SUBNET_COUNT/PRIVATE_SUBNETS_PER_ENVIRONMENT))

  echo $ENV_COUNT
}

# Determine target environment count at process start

ENVIRONMENT_COUNT=$(env_count)
echo "The target environment count at process start: $ENVIRONMENT_COUNT"

#
# Determine if common components should not be deleted
#

ENV_PRIVATE_SUBNET_COUNT=$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet*" \
  --query "Subnets[*].Tags[?Key == 'Name'].Value" \
  --output json \
  | jq '. | length')

if [ "$ENV_PRIVATE_SUBNET_COUNT" -ge "1" ] && [ "$ENVIRONMENT_COUNT" -eq "1" ]; then
  DELETE_COMMON_COMPONENTS="YES"
  DELETE_DATABASE="YES"
  DELETE_ENVIRONMENT_COMPONENTS="YES"
elif [ "$ENV_PRIVATE_SUBNET_COUNT" -eq "0" ] && [ "$ENVIRONMENT_COUNT" -ge "1" ]; then
  DELETE_COMMON_COMPONENTS="NO"
  DELETE_DATABASE="NO"
  DELETE_ENVIRONMENT_COMPONENTS="NO"
elif [ "$ENV_PRIVATE_SUBNET_COUNT" -eq "0" ] && [ "$ENVIRONMENT_COUNT" -eq "0" ]; then
  DELETE_COMMON_COMPONENTS="YES"
  DELETE_DATABASE="NO"
  DELETE_ENVIRONMENT_COMPONENTS="NO"
else
  DELETE_COMMON_COMPONENTS="NO"
  DELETE_DATABASE="NO"
  DELETE_ENVIRONMENT_COMPONENTS="YES"
fi

echo "DELETE_COMMON_COMPONENTS = $DELETE_COMMON_COMPONENTS"
echo "DELETE_DATABASE = $DELETE_DATABASE"
echo "DELETE_ENVIRONMENT_COMPONENTS = $DELETE_ENVIRONMENT_COMPONENTS"

#
# Destroy the api and worker modules
#

# Get load balancer ARN (if exists)

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  LOAD_BALANCERS_EXIST=$(aws --region us-east-1 elbv2 describe-load-balancers \
    --query "LoadBalancers[*].[LoadBalancerArn]" \
    --output text \
    | grep "ab2d-${CMS_ENV}" \
    | xargs \
    | tr -d '\r')
  if [ -n "${LOAD_BALANCERS_EXIST}" ]; then
    ALB_ARN=$(aws --region us-east-1 elbv2 describe-load-balancers \
      --name=ab2d-$CMS_ENV \
      --query 'LoadBalancers[*].[LoadBalancerArn]' \
      --output text)
  fi
fi

# Turn off "delete protection" for the application load balancer

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Turn off 'delete protection' for the application load balancer..."
  if [ -n "${ALB_ARN}" ]; then
    aws --region us-east-1 elbv2 modify-load-balancer-attributes \
      --load-balancer-arn $ALB_ARN \
      --attributes Key=deletion_protection.enabled,Value=false
  fi
fi

#
#  Detach AWS Shield standard from the application load balancer
#

# Note that no change is actually made since AWS shield standard remains applied to
# the application load balancer until the load balancer itself is removed. This section
# may be needed later if AWS Shield Advanced was applied instead.

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Destroying shield components..."
  terraform destroy \
    --target module.shield \
    --auto-approve
fi

# Destroy the environment of the "WAF" module

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Destroying WAF components..."
  terraform destroy \
    --target module.waf \
    --auto-approve
fi

# Destroy the environment of the "CloudWatch" module

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Destroying CloudWatch components..."
  terraform destroy \
    --target module.cloudwatch \
    --auto-approve
fi

# Destroy the environment of the "worker" module

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Destroying worker components..."
  terraform destroy \
    --target module.worker \
    --auto-approve
fi

# Destroy the environment of the "api" module

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Destroying API components..."
  terraform destroy \
    --target module.api \
    --auto-approve
fi

#
# Destroy efs module
#

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  # Destroy the environment of the "efs" module
  echo "Destroying EFS components..."
  terraform destroy \
    --target module.efs --auto-approve
fi

#
# Change to shared environment directory
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

#
# Destroy controller module
#

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Destroying controller..."
  terraform destroy \
    --target module.controller \
    --auto-approve
else
  echo "Skipping destroy of controller due to another existing environment..."
fi

#
# Destroy db module
#

if [ "$DELETE_DATABASE" == "YES" ]; then
  echo "Destroying DB components..."
  terraform destroy \
    --target module.db \
    --auto-approve
else
  echo "Skipping destroy of database due to another existing environment..."
fi

#
# Destroy all S3 buckets except for the "ab2d-automation" bucket
#

# Destroy the environment of the "s3" module

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Destroying S3 components..."
  terraform destroy \
    --target module.s3 --auto-approve
else
   echo "Skipping destroy of S3 due to another existing environment..."
fi

# Destroy environment specific cloudtrail S3 key

aws s3 rm s3://ab2d-cloudtrail/ab2d-$CMS_ENV \
  --recursive

#
# Disable "kms" key
#

# Rerun db destroy again to ensure that it is in correct state
# - this is a workaround that prevents the kms module from raising an eror sporadically

if [ "$DELETE_DATABASE" == "YES" ]; then
  echo "Rerun module.db destroy to ensure proper state..."
  terraform destroy \
    --target module.db --auto-approve
fi

# Destroy the KMS module
if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Disabling KMS key..."
  KMS_KEY_ID=$(aws kms describe-key --key-id alias/ab2d-kms --query="KeyMetadata.KeyId")
  if [ -n "$KMS_KEY_ID" ]; then
    cd "${START_DIR}"
    cd python3
    ./disable-kms-key.py $KMS_KEY_ID
    cd "${START_DIR}"
    cd terraform/environments/ab2d-$CMS_ENV
  fi
else
  echo "Skipping disabling KMS key due to another existing environment..."
fi

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

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  NAT_GW_1_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
    --filter "Name=tag:Name,Values=ab2d-ngw-1" "Name=state,Values=available" \
    --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
    --output text)
fi

if [ -n "${NAT_GW_1_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
    
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

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  NAT_GW_2_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
    --filter "Name=tag:Name,Values=ab2d-ngw-2" "Name=state,Values=available" \
    --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
    --output text)
fi

if [ -n "${NAT_GW_2_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then

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

NAT_GATEWAYS_STATE="NOT_EMPTY"
RETRIES_NGW=0

while [ -n "${NAT_GATEWAYS_STATE}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; do
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

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  NGW_EIP_1_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
    --filter "Name=tag:Name,Values=ab2d-ngw-eip-1" \
    --query 'Addresses[*].[AllocationId]' \
    --output text)
fi

if [ -n "${NGW_EIP_1_ALLOCATION_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Release first Elastic IP address..."
  aws --region us-east-1 ec2 release-address \
    --allocation-id $NGW_EIP_1_ALLOCATION_ID
fi

# Release second Elastic IP address

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  NGW_EIP_2_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
    --filter "Name=tag:Name,Values=ab2d-ngw-eip-2" \
    --query 'Addresses[*].[AllocationId]' \
    --output text)
fi

if [ -n "${NGW_EIP_2_ALLOCATION_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Release second Elastic IP address..."
  aws --region us-east-1 ec2 release-address \
    --allocation-id $NGW_EIP_2_ALLOCATION_ID
fi
  
#
# Disassociate subnets from the NAT Gateway route tables
#

# Disassociate subnets from the first NAT Gateway route table

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
    
  NGW_RT_1_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
    --filter "Name=tag:Name,Values=ab2d-ngw-rt-1" \
    --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
    --output text)
  
  IFS=$' ' read -ra NGW_RT_1_ASSOCIATION_ID <<< "$NGW_RT_1_ASSOCIATION_ID"
  
fi

while [ -n "${NGW_RT_1_ASSOCIATION_ID}" ] && [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; do
    
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

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then

  NGW_RT_2_ASSOCIATION_ID=$(aws --region us-east-1 ec2 describe-route-tables \
    --filter "Name=tag:Name,Values=ab2d-ngw-rt-2" \
    --query 'RouteTables[*].Associations[*].[RouteTableAssociationId]' \
    --output text)

  IFS=$' ' read -ra NGW_RT_2_ASSOCIATION_ID <<< "$NGW_RT_2_ASSOCIATION_ID"

fi

while [ -n "${NGW_RT_2_ASSOCIATION_ID}" ] && [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; do
    
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

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then

  NGW_RT_1_ID=$(aws --region us-east-1 ec2 describe-route-tables \
    --filter "Name=tag:Name,Values=ab2d-ngw-rt-1" \
    --query 'RouteTables[*].[RouteTableId]' \
    --output text)
  
fi

if [ -n "${NGW_RT_1_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Deleting the first NAT Gateway route table for the first NAT gateway..."
  aws --region us-east-1 ec2 delete-route-table \
    --route-table-id $NGW_RT_1_ID
fi
   
# Delete the second NAT Gateway route table for the second NAT gateway

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
    
  NGW_RT_2_ID=$(aws --region us-east-1 ec2 describe-route-tables \
    --filter "Name=tag:Name,Values=ab2d-ngw-rt-2" \
    --query 'RouteTables[*].[RouteTableId]' \
    --output text)

fi

if [ -n "${NGW_RT_2_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
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

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then

  IGW_RT_ID=$(aws --region us-east-1 ec2 describe-route-tables \
    --filter "Name=tag:Name,Values=ab2d-igw-rt" \
    --query 'RouteTables[*].[RouteTableId]' \
    --output text)

fi

if [ -n "${IGW_RT_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Deleting the Internet Gateway route table..."
  aws --region us-east-1 ec2 delete-route-table \
    --route-table-id $IGW_RT_ID
fi

#
# Detach Internet Gateway from VPC
#

# Get VPC ID and IGW ID

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
    
  VPC_ID=$(aws --region us-east-1 ec2 describe-vpcs \
    --filters "Name=tag:Name,Values=ab2d-vpc" \
    --query 'Vpcs[*].[VpcId]' \
    --output text)

  IGW_ID=$(aws --region us-east-1 ec2 describe-internet-gateways \
    --filter "Name=tag:Name,Values=ab2d-igw" \
    --query 'InternetGateways[*].[InternetGatewayId]' \
    --output text)

fi

# Detach Internet Gateway from VPC

if [ -n "${VPC_ID}" ] && [ -n "${IGW_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Detaching Internet Gateway from VPC..."
  aws --region us-east-1 ec2 detach-internet-gateway \
    --vpc-id $VPC_ID \
    --internet-gateway-id $IGW_ID
fi

#
# Delete Internet Gateway
#

if [ -n "${IGW_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Deleting Internet Gateway..."
  aws --region us-east-1 ec2 delete-internet-gateway \
    --internet-gateway-id $IGW_ID
fi

#
# Delete subnets
#

# Delete the first private subnet

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
    
  SUBNET_PRIVATE_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filter "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-01" \
    --query 'Subnets[*].[SubnetId]' \
    --output text)

fi

if [ -n "${SUBNET_PRIVATE_1_ID}" ] && [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Deleting the first private subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PRIVATE_1_ID
fi

# Delete the second private subnet

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
    
  SUBNET_PRIVATE_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filter "Name=tag:Name,Values=ab2d-$CMS_ENV-private-subnet-02" \
    --query 'Subnets[*].[SubnetId]' \
    --output text)

fi

if [ -n "${SUBNET_PRIVATE_2_ID}" ] && [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
  echo "Deleting the second private subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PRIVATE_2_ID
fi

# Delete the first public subnet

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
    
  SUBNET_PUBLIC_1_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filter "Name=tag:Name,Values=ab2d-public-subnet-01" \
    --query 'Subnets[*].[SubnetId]' \
    --output text)

fi

if [ -n "${SUBNET_PUBLIC_1_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Deleting the first public subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PUBLIC_1_ID
fi

# Delete the second public subnet

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
    
  SUBNET_PUBLIC_2_ID=$(aws --region us-east-1 ec2 describe-subnets \
    --filter "Name=tag:Name,Values=ab2d-public-subnet-02" \
    --query 'Subnets[*].[SubnetId]' \
    --output text)

fi

if [ -n "${SUBNET_PUBLIC_2_ID}" ] && [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  echo "Deleting the second public subnet..."
  aws --region us-east-1 ec2 delete-subnet \
    --subnet-id $SUBNET_PUBLIC_2_ID
fi

# Destroy shared environment cloudtrail S3 key

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ]; then
  aws s3 rm s3://ab2d-cloudtrail/ab2d-$CMS_SHARED_ENV \
    --recursive
fi

#
# Delete applicable the tfstate files and ".terraform" directories
#

# Delete applicable the tfstate files and ".terraform" directories for the target environment

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then

  echo "Delete applicable the tfstate files and ".terraform" directories..."

  cd "${START_DIR}"
  cd terraform/environments/ab2d-$CMS_ENV
  aws s3 rm s3://ab2d-automation/ab2d-$CMS_ENV \
    --recursive
  rm -rf .terraform
  
fi

# Delete applicable the tfstate files and ".terraform" directories for the shared environment
# (if no target environments exist)

ENVIRONMENT_COUNT=$(env_count)

if [ "$DELETE_COMMON_COMPONENTS" == "YES" ] && [ "$ENVIRONMENT_COUNT" -eq "0" ]; then
  
  cd "${START_DIR}"
  cd terraform/environments/ab2d-$CMS_SHARED_ENV
  aws s3 rm s3://ab2d-automation/ab2d-$CMS_SHARED_ENV \
    --recursive
  rm -rf .terraform    

fi

#
# Delete any orphaned components using AWS CLI
# - orphaned components may exist if their were automation issues with terraform
#

# Delete orphaned EFS (if exists)

if [ "$DELETE_ENVIRONMENT_COMPONENTS" == "YES" ]; then
    
  EFS_FS_ID=$(aws efs describe-file-systems \
    --query="FileSystems[?CreationToken=='ab2d-${CMS_ENV}-efs'].FileSystemId" \
    --output=text)

  if [ -n "${EFS_FS_ID}" ]; then
    aws efs delete-file-system \
      --file-system-id "${EFS_FS_ID}"
  fi

fi

# Determine target environment count at process end

ENVIRONMENT_COUNT=$(env_count)
echo "The target environment count at process end: $ENVIRONMENT_COUNT"

#
# Done
#

echo "Done"
