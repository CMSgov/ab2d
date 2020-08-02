#!/bin/bash
# Script to list api instances in the environment

export CMS_ENV="ab2d-east-impl" #Examples: ab2d-dev, ab2d-sbx-sandbox, ab2d-east-impl, ab2d-east-prod

echo "*******************************************"
echo "API instances in $CMS_ENV environment"
echo "*******************************************"

aws --region us-east-1 ec2 describe-instances --output text \
  --filters "Name=tag:Name,Values=$CMS_ENV-api" \
  --query "Reservations[*].Instances[*].[InstanceId,PrivateIpAddress]"
