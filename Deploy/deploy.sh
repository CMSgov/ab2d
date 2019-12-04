#!/bin/bash
set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

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
  --ssh-username=*)
  SSH_USERNAME="${i#*=}"
  shift # past argument=value
  ;;
  --database-secret-datetime=*)
  DATABASE_SECRET_DATETIME=$(echo ${i#*=})
  shift # past argument=value
  ;;
  --build-new-images)
  BUILD_NEW_IMAGES="true"
  shift # past argument=value
  ;;
  --use-existing-images)
  USE_EXISTING_IMAGES="true"
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
if [ -z "${ENVIRONMENT}" ] || [ -z "${SHARED_ENVIRONMENT}" ] || [ -z "${DATABASE_SECRET_DATETIME}" ] || [ -z "${SSH_USERNAME}" ]; then
  echo "Try running the script like so:"
  echo "./deploy.sh --environment=dev --shared-environment=shared --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --ssh-username=centos --build-new-images"
  echo "./deploy.sh --environment=dev --shared-environment=shared --database-secret-datetime={YYYY-MM-DD-HH-MM-SS} --ssh-username=centos --use-existing-images"
  exit 1
fi

#
# Verify that one and only one of the following parameters are included
# --build-new-images
# --use-existing-images
#

if [ -n "${BUILD_NEW_IMAGES}" ] && [ -n "${USE_EXISTING_IMAGES}" ]; then

  echo "ERROR: you can't include both '--build-new-images' and '--use-existing-images'"
  exit 1

elif [ -n "${BUILD_NEW_IMAGES}" ]; then

  echo "New images for API and Worker will be built during this process..."

elif [ -n "${USE_EXISTING_IMAGES}" ]; then

  echo "The latest existing images for API and Worker will be used during this process..."

else

  echo "ERROR: you must include one of the following parameters:"
  echo "--build-new-images"
  echo "--use-existing-images"  
  exit 1

fi

#
# Get existing AMI ID
#

echo "Get existing AMI_ID..."
AMI_ID=$(aws --region us-east-1 ec2 describe-images \
  --owners self \
  --filters "Name=tag:Name,Values=ab2d-ami" \
  --query "Images[*].[ImageId]" \
  --output text)

if [ -z "${AMI_ID}" ]; then

  echo "ERROR: the 'ab2d-ami' AMI must already exist!"
  exit 1

fi

#
# Set environment
#

export AWS_PROFILE="${CMS_ENV}"

#
# Get secrets
#

# Change to the "python3" directory

cd "${START_DIR}"
cd python3

# Get database secrets

DATABASE_USER=$(./get-database-secret.py $CMS_ENV database_user $DATABASE_SECRET_DATETIME)
DATABASE_PASSWORD=$(./get-database-secret.py $CMS_ENV database_password $DATABASE_SECRET_DATETIME)
DATABASE_NAME=$(./get-database-secret.py $CMS_ENV database_name $DATABASE_SECRET_DATETIME)

# If any databse secret produced an error, exit the script

if [ "${DATABASE_USER}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_PASSWORD}" == "ERROR: Cannot get database secret because KMS key is disabled!" ] \
  || [ "${DATABASE_NAME}" == "ERROR: Cannot get database secret because KMS key is disabled!" ]; then
    echo "ERROR: Cannot get database secrets because KMS key is disabled!"
    exit 1
fi

#
# Create database
#

# Connect to the controller

CONTROLLER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
  --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
  --output text)

# Get the DB_ENDPOINT of the shared database

DB_ENDPOINT=$(aws --region us-east-1 rds describe-db-instances \
  --query="DBInstances[?DBInstanceIdentifier=='ab2d'].Endpoint.Address" \
  --output=text)

# Determine if the database for the environment exists

DB_NAME_IF_EXISTS=$(ssh -tt -i "~/.ssh/ab2d-sbdemo-shared.pem" \
  "${SSH_USERNAME}@${CONTROLLER_PUBLIC_IP}" \
  "psql -t --host "${DB_ENDPOINT}" --username "${DATABASE_USER}" --dbname postgres --command='SELECT datname FROM pg_catalog.pg_database'" \
  | grep "${DATABASE_NAME}" \
  | sort \
  | head -n 1 \
  | xargs \
  | tr -d '\r')

# Create the database for the environment if it doesn't exist

if [ -n "${CONTROLLER_PUBLIC_IP}" ] && [ -n "${DB_ENDPOINT}" ] && [ "${DB_NAME_IF_EXISTS}" != "${DATABASE_NAME}" ]; then
  echo "Creating database..."
  ssh -tt -i "~/.ssh/ab2d-sbdemo-shared.pem" \
    "${SSH_USERNAME}@${CONTROLLER_PUBLIC_IP}" \
    "createdb ${DATABASE_NAME} --host ${DB_ENDPOINT} --username ${DATABASE_USER}"
fi

#
# Build and push API and worker to ECR
#

echo "Build and push API and worker to ECR..."

cd "${START_DIR}"

# Log on to ECR

if [ -n "${BUILD_NEW_IMAGES}" ]; then

  read -sra cmd < <(aws ecr get-login --no-include-email)
  pass="${cmd[5]}"
  unset cmd[4] cmd[5]
  "${cmd[@]}" --password-stdin <<< "$pass"

fi

# Build API and worker (if creating a new image)

if [ -n "${BUILD_NEW_IMAGES}" ]; then
	 
  cd "${START_DIR}"
  cd ..
  make docker-build
  sleep 5

fi

# Create API and worker Dockerfiles for ECS

if [ -n "${BUILD_NEW_IMAGES}" ]; then
    
  cd "${START_DIR}"
  rm -rf generated
  mkdir -p generated/api
  mkdir -p generated/worker
  cp ../docker-compose.yml generated
  cp ./yaml/config.yml generated
  cp ../api/Dockerfile generated/api/Dockerfile.original
  cp -r ../api/target generated/api
  cp ../worker/Dockerfile generated/worker/Dockerfile.original
  cp -r ../worker/target generated/worker
  sleep 5
  cd python3
  ./create-dockerfilles-for-ecs.py

fi

# Build API docker image

if [ -n "${BUILD_NEW_IMAGES}" ]; then

  cd "${START_DIR}"
  cd generated/api
  docker build \
    --build-arg ab2d_db_host_arg="${DB_ENDPOINT}" \
    --build-arg ab2d_db_port_arg=5432 \
    --build-arg ab2d_db_database_arg="${DATABASE_NAME}" \
    --build-arg ab2d_db_user_arg="${DATABASE_USER}" \
    --build-arg ab2d_db_password_arg="${DATABASE_PASSWORD}" \
    --tag "ab2d_${CMS_ENV}_api:latest" .

fi

# Build worker docker image

if [ -n "${BUILD_NEW_IMAGES}" ]; then
    
  cd "${START_DIR}"
  cd generated/worker
  docker build \
    --build-arg ab2d_db_host_arg="${DB_ENDPOINT}" \
    --build-arg ab2d_db_port_arg=5432 \
    --build-arg ab2d_db_database_arg="${DATABASE_NAME}" \
    --build-arg ab2d_db_user_arg="${DATABASE_USER}" \
    --build-arg ab2d_db_password_arg="${DATABASE_PASSWORD}" \
    --tag "ab2d_${CMS_ENV}_worker:latest" .

fi

# Tag and push API docker image to ECR

if [ -n "${BUILD_NEW_IMAGES}" ]; then

  API_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_${CMS_ENV}_api'].repositoryUri" \
    --output text)
  if [ -z "${API_ECR_REPO_URI}" ]; then
    aws --region us-east-1 ecr create-repository \
        --repository-name "ab2d_${CMS_ENV}_api"
    API_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_${CMS_ENV}_api'].repositoryUri" \
      --output text)
  fi
  docker tag "ab2d_${CMS_ENV}_api:latest" "${API_ECR_REPO_URI}:latest"
  docker push "${API_ECR_REPO_URI}:latest"

fi

# Tag and push worker docker image to ECR

if [ -n "${BUILD_NEW_IMAGES}" ]; then
    
  WORKER_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
    --query "repositories[?repositoryName == 'ab2d_${CMS_ENV}_worker'].repositoryUri" \
    --output text)
  if [ -z "${WORKER_ECR_REPO_URI}" ]; then
    aws --region us-east-1 ecr create-repository \
      --repository-name "ab2d_${CMS_ENV}_worker"
    WORKER_ECR_REPO_URI=$(aws --region us-east-1 ecr describe-repositories \
      --query "repositories[?repositoryName == 'ab2d_${CMS_ENV}_worker'].repositoryUri" \
      --output text)
  fi
  docker tag "ab2d_${CMS_ENV}_worker:latest" "${WORKER_ECR_REPO_URI}:latest"
  docker push "${WORKER_ECR_REPO_URI}:latest"

fi

#
# Switch context to terraform environment
#

echo "Switch context to terraform environment..."

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

#
# Get current known good ECS task definitions
#

echo "Get current known good ECS task definitions..."
CLUSTER_ARNS=$(aws --region us-east-1 ecs list-clusters \
  --query 'clusterArns' \
  --output text \
  | grep "/ab2d-${CMS_ENV}-api" \
  | xargs \
  | tr -d '\r')
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping getting current ECS task definitions, since there are no existing clusters"
else
  echo "TEST"
  API_TASK_DEFINITION=$(aws --region us-east-1 ecs describe-services \
    --services ab2d-api \
    --cluster "ab2d-${CMS_ENV}-api" \
    | grep "taskDefinition" \
    | head -1)
  API_TASK_DEFINITION=$(echo $API_TASK_DEFINITION | awk -F'": "' '{print $2}' | tr -d '"' | tr -d ',')
fi

#
# Get ECS task counts before making any changes
#

echo "Get ECS task counts before making any changes..."

# Define api_task_count
api_task_count() { aws --region us-east-1 ecs list-tasks --cluster "ab2d-${CMS_ENV}-api" | grep "\:task\/"|wc -l|tr -d ' '; }

# Get old api task count (if exists)
if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_TASK_COUNT, since there are no existing clusters"
else
  OLD_API_TASK_COUNT=$(api_task_count)
fi

# set expected api task count

# LSH BEGIN 2019-11-20
# if [ -z "${CLUSTER_ARNS}" ]; then
#   EXPECTED_API_COUNT="2"
# else
#   EXPECTED_API_COUNT="$((OLD_API_TASK_COUNT*2))"
# fi
EXPECTED_API_COUNT="2"
# LSH END 2019-11-20

#
# Ensure Old Autoscaling Groups and containers are around to service requests
#

echo "Ensure Old Autoscaling Groups and containers are around to service requests..."

if [ -z "${CLUSTER_ARNS}" ]; then
  echo "Skipping setting OLD_API_ASG, since there are no existing clusters"
else
  OLD_API_ASG=$(terraform show|grep :autoScalingGroup:|awk -F" = " '{print $2}' | grep ab2d-$CMS_ENV)
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
  OLD_API_CONTAINER_INSTANCES=$(aws --region us-east-1 ecs list-container-instances --cluster "ab2d-${CMS_ENV}-api" | grep container-instance)
fi

#
# Deploy API and Worker
#

echo "Deploy API and Worker..."
if [ -z "${AUTOAPPROVE}" ]; then
    
  # Confirm with the caller prior to applying changes.

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --var "db_username=${DATABASE_USER}" \
    --var "db_password=${DATABASE_PASSWORD}" \
    --var "db_name=${DATABASE_NAME}" \
    --target module.api
  
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target module.worker

else
    
  # Apply the changes without prompting

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --var "db_username=${DATABASE_USER}" \
    --var "db_password=${DATABASE_PASSWORD}" \
    --var "db_name=${DATABASE_NAME}" \
    --target module.api \
    --auto-approve

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target module.worker \
    --auto-approve

fi

#
# Apply schedule autoscaling if applicable
#

echo "Apply schedule autoscaling if applicable..."
if [ -f ./autoscaling-schedule.tf ]; then
    
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target=aws_autoscaling_schedule.morning \
    --auto-approve

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target=aws_autoscaling_schedule.night \
    --auto-approve

fi

#
# Push authorized_keys file to deployment_controller
#

export AWS_PROFILE="${CMS_SHARED_ENV}"

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_SHARED_ENV

echo "Push authorized_keys file to deployment_controller..."
terraform taint \
  --allow-missing null_resource.authorized_keys_file
if [ -z "${AUTOAPPROVE}" ]; then
  # Confirm with the caller prior to applying changes.
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target null_resource.authorized_keys_file
else
  # Apply the changes without prompting
  terraform apply \
    --var "ami_id=$AMI_ID" \
    --var "current_task_definition_arn=$API_TASK_DEFINITION" \
    --target null_resource.authorized_keys_file \
    --auto-approve
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

#
# Deploy CloudWatch
#

cd "${START_DIR}"
cd terraform/environments/ab2d-$CMS_ENV

echo "Deploy CloudWatch..."
if [ -z "${AUTOAPPROVE}" ]; then

  terraform apply \
    --var "ami_id=$AMI_ID" \
    --target module.cloudwatch

else
    
  # Apply the changes without prompting

  terraform apply \
    --target module.cloudwatch \
    --var "ami_id=$AMI_ID" \
    --auto-approve

fi

#
# Deploy AWS WAF
#

if [ -z "${AUTOAPPROVE}" ]; then
    
  terraform apply \
    --target module.waf
  
else

  # Apply the changes without prompting
    
  terraform apply \
    --target module.waf \
    --auto-approve
  
fi

#
# Apply AWS Shield standard to the application load balancer
#

# Note that no change is actually made since AWS shield standard is automatically applied to
# the application load balancer. This section may be needed later if AWS Shield Advanced is
# applied instead.

if [ -z "${AUTOAPPROVE}" ]; then
    
  terraform apply \
    --target module.shield
  
else

  # Apply the changes without prompting
    
  terraform apply \
    --target module.shield \
    --auto-approve
  
fi
