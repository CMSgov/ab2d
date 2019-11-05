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

#
# Delete VPC
#

if [ -n "${VPC_ID}" ]; then
  echo "Deleting the VPC..."
  aws --region us-east-1 ec2 delete-vpc \
    --vpc-id $VPC_ID
fi
