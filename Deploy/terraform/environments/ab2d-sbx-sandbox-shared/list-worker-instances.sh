#!/bin/bash
# Script to list worker instances in the environment

<<<<<<< HEAD
export CMS_ENV="ab2d-sbx-sandbox" #Examples: ab2d-dev, ab2d-sbx-sandbox, ab2d-east-impl, ab2d-east-prod
=======
<<<<<<< HEAD:Deploy/terraform/environments/ab2d-dev-shared/list-worker-instances.sh
export CMS_ENV="ab2d-dev" #Examples: ab2d-dev, ab2d-sbx-sandbox, ab2d-east-impl, ab2d-east-prod
=======
export CMS_ENV="ab2d-sbx-sandbox" #Examples: ab2d-dev, ab2d-sbx-sandbox, ab2d-east-impl, ab2d-east-prod
>>>>>>> origin/master:Deploy/terraform/environments/ab2d-sbx-sandbox-shared/list-worker-instances.sh
>>>>>>> origin/master

echo "*******************************************"
echo "Worker instances in $CMS_ENV environment"
echo "*******************************************"

aws --region us-east-1 ec2 describe-instances --output text \
  --filters "Name=tag:Name,Values=$CMS_ENV-worker" \
  --query "Reservations[*].Instances[*].[InstanceId,PrivateIpAddress]"
