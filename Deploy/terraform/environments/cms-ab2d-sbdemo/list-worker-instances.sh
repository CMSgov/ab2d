#!/bin/bash
# Script to list worker instances in the environment

export CMS_ENV="SBDEMO" #Examples: DEV, SBX, IMPL, PROD, SBDEMO

echo "*******************************************"
echo "Worker instances in $CMS_ENV environment"
echo "*******************************************"

aws --region us-east-1 ec2 describe-instances --output text \
  --filters "Name=tag:Name,Values=*AB2D-WORKER-$CMS_ENV" \
  --query "Reservations[*].Instances[*].[InstanceId,PrivateIpAddress]"
