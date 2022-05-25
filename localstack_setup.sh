#!/bin/bash

function setup(){
  until grep -q '^Ready.' /tmp/localstack_infra.log >/dev/null 2>&1 ; do
      echo "Waiting for all LocalStack services to be ready"
      sleep 7
    done

    awslocal sns create-topic --name ab2d-job-tracking
}

setup &

$@