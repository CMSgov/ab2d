#!/bin/bash

#This script assumes a mostly graceful shutdown. If things go really bad you'll need to remove the containers using docker rm <container_id>

docker-compose -f ./docker-compose.yml run --rm terraform destroy --auto-approve

# terraform lock files hold state that assumes AWS (aka localstack) is accessible, even after destroy.
# Since destroy kills the localstack docker image that isn't true
# Deleting the lock files resets the state
rm -f ./terraform/terraform.tfstate ./terraform/terraform.tfstate.backup

docker-compose stop postgres localstack

./gradlew clean
