#!/usr/bin/env bash

set -e # Turn on exit on error

# Set default AWS region and tag

#
######## update ECS service with new image ################
#
aws ecs update-service --cluster $DEPLOYMENT_ENV-microservice-cluster  --service ab2d-event-service --force-new-deployment
