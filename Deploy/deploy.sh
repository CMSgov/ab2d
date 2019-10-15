#!/bin/bash
set -e #Exit on first error
set -x #Be verbose

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
  --ami=*)
  AMI_ID="${i#*=}"
  shift # past argument=value
  ;;
  --auto-approve)
  AUTOAPPROVE="true"
  shift # past argument=value
  ;;
esac
done


#
# Check vars are not empty before proceeding
#

echo "Check vars are not empty before proceeding..."
if [ -z "${ENVIRONMENT}" ]; then
  echo "Try running the script like so:"
  echo "./deploy.sh --environment=prototype"
  exit 1
fi

#
# Set AMI_ID if it already exists for the deployment
#

echo "Set AMI_ID if it already exists for the deployment..."
AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-AMI" \
  --query "Images[*].[ImageId]" \
  --output text)


#
# If no AMI is specified then create a new one
#

echo "If no AMI is specified then create a new one..."
if [ -z "${AMI_ID}" ]; then
  cd packer/app/
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build --var my_ip_address=$IP --var git_commit_hash=$COMMIT app.json  2>&1 | tee output.txt
  AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  cd ../../
  # Add name tag to AMI
  aws --region us-east-1 ec2 create-tags \
    --resources $AMI_ID \
    --tags "Key=Name,Value=AB2D-$CMS_ENV-AMI"
fi


#
# Get current known good ECS task definitions
#

echo "Get current known good ECS task definitions..."
CLUSTER_ARNS=$(aws --region us-east-1 ecs list-clusters --query 'clusterArns' --output text)
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping getting current ECS task definitions, since there are no existing clusters"
else  
  API_TASK_DEFINITION=$(aws --region us-east-1 ecs describe-services --services ab2d-api --cluster ab2d-$ENVIRONMENT | grep "taskDefinition" | head -1)
  API_TASK_DEFINITION=$(echo $API_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')
fi


#
# Get ECS task counts before making any changes
#

echo "Get ECS task counts before making any changes..."

# Define api_task_count
api_task_count() { aws --region us-east-1 ecs list-tasks --cluster ab2d-$ENVIRONMENT|grep "\:task\/"|wc -l|tr -d ' '; }

# Get old api task count (if exists)
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_TASK_COUNT, since there are no existing clusters"
else
  OLD_API_TASK_COUNT=$(api_task_count)
fi

# Get expected api task count
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting EXPECTED_API_COUNT, since there are no existing clusters"
  EXPECTED_API_COUNT="2"
else
  EXPECTED_API_COUNT="$OLD_API_TASK_COUNT*2"
fi


#
# Switch context to terraform environment
#

echo "Switch context to terraform environment..."
cd terraform/environments/cms-ab2d-$ENVIRONMENT


#
# Ensure Old Autoscaling Groups and containers are around to service requests
#

echo "Ensure Old Autoscaling Groups and containers are around to service requests..."

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_ASG, since there are no existing clusters"
else
  OLD_API_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}'|grep ab2d-$ENVIRONMENT)
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  terraform state rm module.app.aws_autoscaling_group.asg
  terraform state rm module.app.aws_launch_configuration.launch_config
fi

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping removing autosclaing group and launch configuration, since there are no existing clusters"
else
  OLD_API_CONTAINER_INSTANCES=$(aws --region us-east-1 ecs list-container-instances --cluster ab2d-$ENVIRONMENT|grep container-instance)
fi


#
# Deploy new AMI out to AWS
#

echo "Deploy new AMI out to AWS..."
if [ -z "${AUTOAPPROVE}" ]; then
  # Confirm with the caller prior to applying changes.
  terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.api
  terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.worker
else
  # Apply the changes without prompting
  terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.api --auto-approve
  terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.worker --auto-approve
fi


#
# Apply schedule autoscaling if applicable
#

echo "Apply schedule autoscaling if applicable..."
if [ -f ./autoscaling-schedule.tf ]; then
  terraform apply --auto-approve -var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=aws_autoscaling_schedule.morning
  terraform apply --auto-approve -var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=aws_autoscaling_schedule.night
fi


#
# Push authorized_keys file to deployment_controller
#

echo "Push authorized_keys file to deployment_controller..."
terraform taint -allow-missing null_resource.authorized_keys_file
if [ -z "${AUTOAPPROVE}" ]; then
  # Confirm with the caller prior to applying changes.
  terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=null_resource.authorized_keys_file
else
  # Apply the changes without prompting
  terraform apply --auto-approve --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=null_resource.authorized_keys_file
fi


#
# Ensure new autoscaling group is running containers
#

echo "Ensure new autoscaling group is running containers..."

ACTUAL_API_COUNT=0
RETRIES_API=0

while [ "$ACTUAL_API_COUNT" -lt "$EXPECTED_API_COUNT" ]; do
  ACTUAL_API_COUNT=$(api_task_count)
  echo "Running API Tasks: $ACTUAL_API_COUNT, Expected: $EXPECTED_API_COUNT"
  if [ "$RETRIES_API" != "15" ]; then
    echo "Retry in 60 seconds..."
    sleep 60
    RETRIES_API=$(expr $RETRIES_API + 1)
  else
    echo "Max retries reached. Exiting..."
    exit 1
  fi
done
