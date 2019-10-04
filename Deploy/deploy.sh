#!/bin/bash
set -e #Exit on first error
set -x #Be verbose


# Parse options
for i in "$@"
do
case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
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


# Check vars are not empty before proceeding
if [ -z "${ENVIRONMENT}" ]; then
  echo "Try running the script like so:"
  echo "./deploy.sh --environment=prototype"
  exit 1
fi


# If no AMI is specified then create a new one
if [ -z "${AMI_ID}" ]; then
  cd packer/app/
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build --var my_ip_address=$IP --var git_commit_hash=$COMMIT app.json  2>&1 | tee output.txt
  AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  cd ../../
fi


#
# *** TO DO ***: Uncomment and test each section iteratively
#


# # Get current known good ECS task definitions
# API_TASK_DEFINITION=$(aws --region us-east-1 ecs describe-services --services ab2d-api  --cluster ab2d-$ENVIRONMENT | grep "taskDefinition" | head -1)
# API_TASK_DEFINITION=$(echo $API_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')
# TAXONOMY_TASK_DEFINITION=$(aws --region us-east-1 ecs describe-services --services ab2d-taxonomy  --cluster ab2d-taxonomy-$ENVIRONMENT | grep "taskDefinition" | head -1)
# TAXONOMY_TASK_DEFINITION=$(echo $TAXONOMY_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')


# # Get ECS task counts before making any changes
# api_task_count() { aws --region us-east-1 ecs list-tasks --cluster ab2d-$ENVIRONMENT|grep "\:task\/"|wc -l|tr -d ' '; }
# taxonomy_task_count() { aws --region us-east-1 ecs list-tasks --cluster ab2d-taxonomy-$ENVIRONMENT|grep "\:task\/"|wc -l|tr -d ' '; }
# OLD_API_TASK_COUNT=$(api_task_count)
# OLD_TAXONOMY_TASK_COUNT=$(taxonomy_task_count)
# let EXPECTED_API_COUNT="$OLD_API_TASK_COUNT*2"
# let EXPECTED_TAXONOMY_COUNT="$OLD_TAXONOMY_TASK_COUNT*2"


# # Switch context to terraform environment
# cd terraform/environments/cms-ab2d-$ENVIRONMENT


# # Ensure Old Autoscaling Groups and containers are around to service requests
# OLD_API_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}'|grep ab2d-$ENVIRONMENT)
# OLD_TAXONOMY_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}'|grep ab2d-taxonomy-$ENVIRONMENT)
# terraform state rm module.app.aws_autoscaling_group.asg
# terraform state rm module.app.aws_launch_configuration.launch_config
# terraform state rm module.taxonomy.aws_autoscaling_group.asg
# terraform state rm module.taxonomy.aws_launch_configuration.launch_config
# OLD_API_CONTAINER_INSTANCES=$(aws --region us-east-1 ecs list-container-instances --cluster ab2d-$ENVIRONMENT|grep container-instance)
# OLD_TAXONOMY_CONTAINER_INSTANCES=$(aws --region us-east-1 ecs list-container-instances --cluster ab2d-taxonomy-$ENVIRONMENT|grep container-instance)


# #Make sure new deployment controller gets data migration scripts
# terraform state rm module.data_migration


# # Deploy new AMI out to AWS
# if [ -z "${AUTOAPPROVE}" ]; then
#   # Confirm with the caller prior to applying changes.
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.app
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.cloudwatch
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.data_migration
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$TAXONOMY_TASK_DEFINITION" --target module.taxonomy
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$TAXONOMY_TASK_DEFINITION" --target module.cloudwatch_taxonomy
# else
#   # Apply the changes without prompting
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.app --auto-approve
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.cloudwatch --auto-approve
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.data_migration --auto-approve
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$TAXONOMY_TASK_DEFINITION" --target module.taxonomy --auto-approve
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$TAXONOMY_TASK_DEFINITION" --target module.cloudwatch_taxonomy --auto-approve
# fi


# # Apply schedule autoscaling if applicable
# if [ -f ./autoscaling-schedule.tf ]; then
#     terraform apply --auto-approve -var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=aws_autoscaling_schedule.morning
#     terraform apply --auto-approve -var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=aws_autoscaling_schedule.night
# fi


# # Push authorized_keys file to deployment_controller
# terraform taint -allow-missing null_resource.authorized_keys_file
# if [ -z "${AUTOAPPROVE}" ]; then
#   # Confirm with the caller prior to applying changes.
#   terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=null_resource.authorized_keys_file
# else
#   # Apply the changes without prompting
#   terraform apply --auto-approve --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" -target=null_resource.authorized_keys_file
# fi


# # Ensure new autoscaling group is running containers
# ACTUAL_API_COUNT=0
# RETRIES_API=0

# while [ "$ACTUAL_API_COUNT" -lt "$EXPECTED_API_COUNT" ]; do
#   ACTUAL_API_COUNT=$(api_task_count)
#   echo "Running API Tasks: $ACTUAL_API_COUNT, Expected: $EXPECTED_API_COUNT"
#   if [ "$RETRIES_API" != "15" ]; then
#     echo "Retry in 60 seconds..."
#     sleep 60
#     RETRIES_API=$(expr $RETRIES_API + 1)
#   else
#     echo "Max retries reached. Exiting..."
#     exit 1
#   fi
# done

# ACTUAL_TAXONOMY_COUNT=0
# RETRIES_TAXONOMY=0

# while [ "$ACTUAL_TAXONOMY_COUNT" -lt "$EXPECTED_TAXONOMY_COUNT" ]; do
#   ACTUAL_TAXONOMY_COUNT=$(taxonomy_task_count)
#   echo "Running Taxonomy Tasks: $ACTUAL_TAXONOMY_COUNT, Expected: $EXPECTED_TAXONOMY_COUNT"
#   if [ "$RETRIES_TAXONOMY" != "15" ]; then
#     echo "Retry in 60 seconds..."
#     sleep 60
#     RETRIES_TAXONOMY=$(expr $RETRIES_TAXONOMY + 1)
#   else
#     echo "Max retries reached. Exiting..."
#     exit 1
#   fi
# done


# # Drain old container instances
# OLD_API_INSTANCE_LIST=$(echo $OLD_API_CONTAINER_INSTANCES|tr -d ' '|tr "\n" " "|tr -d ","|tr '""' ' ' |tr -d '"')
# OLD_TAXONOMY_INSTANCE_LIST=$(echo $OLD_TAXONOMY_CONTAINER_INSTANCES|tr -d ' '|tr "\n" " "|tr -d ","|tr '""' ' ' |tr -d '"')
# aws --region us-east-1 ecs update-container-instances-state --cluster ab2d-$ENVIRONMENT --status DRAINING --container-instances $OLD_API_INSTANCE_LIST
# aws --region us-east-1 ecs update-container-instances-state --cluster ab2d-taxonomy-$ENVIRONMENT --status DRAINING --container-instances $OLD_TAXONOMY_INSTANCE_LIST
# echo "Allowing all instances to drain for 60 seconds before proceeding..."
# sleep 60


# # Remove old Autoscaling group
# OLD_API_ASG=$(echo $OLD_API_ASG|awk -F"/" '{print $2}')
# OLD_TAXONOMY_ASG=$(echo $OLD_TAXONOMY_ASG|awk -F"/" '{print $2}')
# aws --region us-east-1 autoscaling delete-auto-scaling-group --auto-scaling-group-name $OLD_API_ASG --force-delete || true
# aws --region us-east-1 autoscaling delete-auto-scaling-group --auto-scaling-group-name $OLD_TAXONOMY_ASG --force-delete || true
