#!/usr/bin/env bash

set -e # Turn on exit on error


# Export environmental variables
AWS_ACCOUNT_ID=$AWS_ACCOUNT_ID
ECR_REPO_ENV=$ECR_REPO_ENV
DEPLOYMENT_ENV=$DEPLOYMENT_ENV

# Set default AWS region and tag

  export AWS_DEFAULT_REGION="us-east-1"
  export IMAGE_TAG="event-service"
  
            
if [ "${CLOUD_TAMER}" != "false" ] && [ "${CLOUD_TAMER}" != "true" ]; then
  echo "ERROR: CLOUD_TAMER parameter must be true or false"
  exit 1
elif [ "${CLOUD_TAMER}" = "false" ]; then

  # Turn off verbose logging for Jenkins jobs
  set +x
  echo "Don't print commands and their arguments as they are executed."
  CLOUD_TAMER="${CLOUD_TAMER}"

  # Import the "get temporary AWS credentials via AWS STS assume role" function
  source "./scripts/fn_get_temporary_aws_credentials_via_aws_sts_assume_role.sh"
  fn_get_temporary_aws_credentials_via_aws_sts_assume_role "${AWS_ACCOUNT_ID}" "${ECR_REPO_ENV}"

else # [ "${CLOUD_TAMER}" == "true" ]

  # Turn on verbose logging for development machine testing
  set -x
  echo "Print commands and their arguments as they are executed."
  CLOUD_TAMER="${CLOUD_TAMER}"

  # Import the "get temporary AWS credentials via CloudTamer API" function
  source "./scripts/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"
  fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_ID}" "${ECR_REPO_ENV}"

fi

########### Logging into ECR ####################
echo Logging in to Amazon ECR...
aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
#export REPOSITORY_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$ECR_REPO_ENV

############# Building docker image ###############
echo Build started on `date`
echo Building the Docker image...          
docker build --no-cache -t $ECR_REPO_ENV:$IMAGE_TAG .
docker tag $ECR_REPO_ENV:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$ECR_REPO_ENV:$IMAGE_TAG


##### pushing docker image to ECR ################
echo Build completed on `date`
echo Pushing the Docker image...
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$ECR_REPO_ENV:$IMAGE_TAG

####### update ECS service with new image ################

aws ecs update-service --cluster $DEPLOYMENT_ENV-microservice-cluster  --service ab2d-event-service --force-new-deployment
