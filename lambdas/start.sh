#!/bin/bash

export TF_LOG="TRACE"

if [ "$1" = "clean" ]; then
  ./gradlew clean
fi

docker-compose up db localstack -d &
command="while docker container inspect -f '{{.State.Running}}' postgres localstack | sed -e '1h;2,\$H;\$!d;g' -e '/^true\ntrue/p;q' >> /dev/null ; do echo \"checking if docker containers are up\" >> test.txt && sleep  5; done"
perl -e 'alarm shift; exec $command' "30";

./gradlew clean buildZip

if [ ! -f ./terraform/terraform.tfstate ]; then
  docker-compose -f ./docker-compose.yml run --rm terraform init -upgrade
fi

docker-compose -f ./docker-compose.yml run --rm terraform apply --auto-approve
