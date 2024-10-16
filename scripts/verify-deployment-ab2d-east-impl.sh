#!/bin/bash
set -e

# Ensure jq is installed
if ! command -v jq &> /dev/null
then
    echo "jq could not be found. Please install jq and try again."
    exit 1
fi

BASIC_AUTH=$(aws secretsmanager get-secret-value --secret-id ab2d/ab2d-east-impl/jenkins-verify-api --query SecretString --output text)

echo "Checking status"
max_attempts=90
attempts=0
STATE=0

while [[ $STATE != 401 ]]; do
    if [[ ${attempts} -eq ${max_attempts} ]]; then
        echo "Max attempts reached, timing out"
        exit 1
    fi
    STATE=$(curl -sLk -w "%{http_code}" "https://impl.ab2d.cms.gov" -o /dev/null)
    echo $STATE
    if [[ $STATE != 401 ]]; then
        echo 'Unreachable, sleeping for 10 seconds . . .'
        attempts=$(($attempts + 1))
        sleep 10
    fi
done

echo 'AB2D API is online . . .'

echo "Refreshing token"
# Get PDP-100 token
export TOKEN=$(curl -s --location --request POST 'https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header "Authorization: Basic $BASIC_AUTH" \  # Removed single quotes here
--header 'Cookie: JSESSIONID=3E7BD665DE5673C73A82647BBD9E548A' \
| jq -r '.access_token')

if [[ -z "$TOKEN" ]]; then
    echo "Failed to retrieve access token. Exiting."
    exit 1
fi

# Start PDP-100 job
echo "Starting PDP-100 job"
JOB_RESPONSE=$(curl -k -s --head --location --request GET 'https://impl.ab2d.cms.gov/api/v2/fhir/Patient/$export?_type=ExplanationOfBenefit&_since=2020-02-13T00:00:00.000-05:00&_outputFormat=application%2Ffhir%2Bndjson' \
--header "Prefer: respond-async" \
--header "Authorization: Bearer $TOKEN")

echo "Full job response headers: $JOB_RESPONSE"

JOB=$(echo "$JOB_RESPONSE" | awk -v FS=": " "/^content-location/{print \$2}" | tr -d '\r')

if [[ -z "$JOB" ]]; then
    echo "Failed to retrieve the job URL. Here is the full response for troubleshooting:"
    echo "$JOB_RESPONSE"
    exit 1
fi

echo "Job URL: $JOB"

# Check on job status
echo "Checking job status..."
STATUS=""
max_status_attempts=30
status_attempts=0

while [[ -z "$STATUS" || "$STATUS" != *"200"* ]]; do
    if [[ $status_attempts -ge $max_status_attempts ]]; then
        echo "Max job status attempts reached. Exiting."
        exit 1
    fi
    sleep 10
    echo "$JOB"
    STATUS=$(curl -k -s --location --request GET "${JOB}" \
    --header "Authorization: Bearer $TOKEN")
    echo "Job Status Response: $STATUS"
    if [[ "$STATUS" == *"error"* || "$STATUS" == *"failed"* ]]; then
        echo "Job failed."
        exit 1
    fi
    status_attempts=$(($status_attempts + 1))
done

echo "Complete"
