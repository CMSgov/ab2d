#!/bin/bash
# Script to list worker instances in the environment

export CMS_ENV="ab2d-sbx-sandbox" #Examples: ab2d-dev, ab2d-sbx-sandbox, ab2d-east-impl, ab2d-east-prod

echo "*******************************************"
echo "Worker instances in $CMS_ENV environment"
echo "*******************************************"

aws --region us-east-1 ec2 describe-instances --output text \
  --filters "Name=tag:Name,Values=$CMS_ENV-worker" \
  --query "Reservations[*].Instances[*].[InstanceId,PrivateIpAddress]"
