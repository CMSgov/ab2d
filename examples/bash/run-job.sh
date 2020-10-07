#!/usr/bin/env bash

source bootstrap.sh "$@"

if [ "$BEARER_TOKEN" == "null" ]
then
    printf "Failed to retrieve bearer token is base64 token accurate?\nIs %s available from this computer?\n", $IDP_URL
    exit 1
fi

echo "Using okta url: $IDP_URL"
echo "Connecting to AB2D API at: $API_URL"
echo "Saving data to: $DIRECTORY"
echo "Optional contract parameter: $CONTRACT"

echo "Starting job"

./start-job.sh

echo "Monitoring job"

./monitor-job.sh

echo "Download results for job"

./download-results.sh

