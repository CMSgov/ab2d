#!/bin/bash
# Script to list api instances in the environment

export CMS_ENV="dev" #Examples: dev, sbx, impl, prod, sbdemo

echo "*******************************************"
echo "API instances in $CMS_ENV environment"
echo "*******************************************"

aws --region us-east-1 ec2 describe-instances --output text \
  --filters "Name=tag:Name,Values=ab2d-$CMS_ENV-api" \
  --query "Reservations[*].Instances[*].[InstanceId,PrivateIpAddress]"
