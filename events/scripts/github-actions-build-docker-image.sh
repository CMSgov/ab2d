#!/usr/bin/env bash

set -e # Turn on exit on error

# Set default AWS region and tag

export AWS_DEFAULT_REGION="us-east-1"
export IMAGE_TAG="event-service"

            

########### Logging into ECR ####################
echo Logging in to Amazon ECR...
aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $ACCOUNT_NUMBER.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com

############# Building docker image ###############
echo Build started on `date`
echo Building the Docker image...          
ls build/libs/
docker build --no-cache -t $ECR_REPO_ENV:$IMAGE_TAG .
docker tag $ECR_REPO_ENV:$IMAGE_TAG $ACCOUNT_NUMBER.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$ECR_REPO_ENV:$IMAGE_TAG

##### pushing docker image to ECR ################
echo Build completed on `date`
echo Pushing the Docker image...
docker push $ACCOUNT_NUMBER.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$ECR_REPO_ENV:$IMAGE_TAG
