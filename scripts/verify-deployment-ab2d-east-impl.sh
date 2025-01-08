#!/bin/bash
set -e
set -x  # Enable verbose mode for debugging

sleep 30

BASIC_AUTH=$(aws secretsmanager get-secret-value --secret-id ab2d/ab2d-east-impl/jenkins-verify-api --query SecretString --output text) || { echo "Failed to retrieve secret"; exit 3; }

echo "Checking status"
max_attempts=90
STATE=""
attempts=0

while [ "$STATE" != "401" ]; do
    if [[ $attempts -eq $max_attempts ]]; then
        echo "Max attempts reached timing out"
        exit 1
    fi
    STATE=$(curl -sLk -w "%{http_code}" "https://impl.ab2d.cms.gov" -o /dev/null) || { echo "Curl request failed"; exit 3; }
    echo "$STATE"
    if [ "$STATE" != "401" ]; then
        echo 'Unreachable, sleeping for 10 seconds . . .'
        attempts=$((attempts+1))
        sleep 10
    fi
done

echo 'AB2D API is online . . .'

echo "Refreshing token"
TOKEN=$(curl -s --location --request POST 'https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header "Authorization: Basic ${BASIC_AUTH}" \
--header 'Cookie: JSESSIONID=3E7BD665DE5673C73A82647BBD9E548A' \
| jq -r '.access_token') || { echo "Token refresh failed"; exit 3; }

echo "Starting PDP-100 job"
JOB=$(curl -k -s --head --location --request GET 'https://impl.ab2d.cms.gov/api/v2/fhir/Patient/$export?_type=ExplanationOfBenefit&_since=2020-02-13T00:00:00.000-05:00&_outputFormat=application%2Ffhir%2Bndjson' \
--header "Prefer: respond-async" \
--header "Authorization: Bearer $TOKEN" \
| awk -v FS=": " '/^content-location/{print $2}' | sed 's/content-location: //' | tr -d '\r') || { echo "Failed to start job"; exit 3; }

echo "Checking job status..."
STATUS=""
while [ -z "$STATUS" ]; do
    sleep 5
    echo "$JOB"
    STATUS=$(curl -k -s --location --request GET "${JOB}" \
    --header "Authorization: Bearer $TOKEN") || { echo "Failed to check job status"; exit 3; }
done

echo "Complete"
