#!/bin/bash
# Script to list worker instances in the environment

# Note that the same values are used for both sbdemo and CMS AWS accounts
export CMS_ENV="sbx" #Examples: dev, sbx, impl, prod

echo "*******************************************"
echo "Worker instances in $CMS_ENV environment"
echo "*******************************************"

aws --region us-east-1 ec2 describe-instances --output text \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-worker" \
  --query "Reservations[*].Instances[*].[InstanceId,PrivateIpAddress]"
