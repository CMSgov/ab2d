#!/bin/bash
set -e
set -x  # Enable verbose mode for debugging

# Input parameters
SECRET_ID="${1}"
BASE_URL="${2}"
EXPORT_URL="${3}"

# Validate inputs
if [ -z "$SECRET_ID" ] || [ -z "$BASE_URL" ] || [ -z "$EXPORT_URL" ]; then
    echo "Usage: $0 <SECRET_ID> <BASE_URL> <EXPORT_URL>"
    exit 1
fi

# Wait for dependencies to initialize
sleep 30

# Fetch the secret for basic authentication
BASIC_AUTH=$(aws secretsmanager get-secret-value --secret-id "$SECRET_ID" --query SecretString --output text) || {
    echo "Failed to retrieve secret"; exit 3;
}

# Check API status
echo "Checking API status..."
max_attempts=90
STATE=""
attempts=0

while [ "$STATE" != "401" ]; do
    if [[ $attempts -eq $max_attempts ]]; then
        echo "Max attempts reached, timing out."
        exit 1
    fi

    STATE=$(curl -sLk -w "%{http_code}" "$BASE_URL" -o /dev/null) || {
        echo "Failed to reach API. Curl request failed."; exit 3;
    }
    echo "Current state: $STATE"

    if [ "$STATE" != "401" ]; then
        echo "API not ready. Retrying in 10 seconds..."
        attempts=$((attempts + 1))
        sleep 10
    fi
done

echo "AB2D API is online."

# Refresh token
echo "Refreshing token..."
TOKEN=$(curl -s --location --request POST 'https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header "Authorization: Basic $BASIC_AUTH" \
--header 'Cookie: JSESSIONID=3E7BD665DE5673C73A82647BBD9E548A' \
| jq -r '.access_token') || {
    echo "Token refresh failed."; exit 3;
}

if [ -z "$TOKEN" ]; then
    echo "Token is empty. Exiting."
    exit 3
fi

echo "Starting PDP-100 job..."
RESPONSE_HEADERS=$(curl -k -s --head --location --request GET "$EXPORT_URL" \
    --header "Prefer: respond-async" \
    --header "Authorization: Bearer $TOKEN")

echo "Response headers:"
echo "$RESPONSE_HEADERS"

JOB=$(echo "$RESPONSE_HEADERS" | awk -v FS=": " '/^content-location/{print $2}' | tr -d '\r' | sed 's/content-location: //')

if [ -z "$JOB" ]; then
    echo "Failed to retrieve job location. Exiting."
    echo "Full response headers:"
    echo "$RESPONSE_HEADERS"
    exit 3
fi

echo "Job started at: $JOB"

# Check job status
echo "Checking job status..."
STATUS=""
attempts=0
max_attempts=60

while [ -z "$STATUS" ]; do
    if [ $attempts -eq $max_attempts ]; then
        echo "Max attempts reached while checking job status. Exiting."
        exit 3
    fi

    echo "Fetching status from $JOB"
    STATUS=$(curl -k -s --location --request GET "$JOB" \
    --header "Authorization: Bearer $TOKEN" \
    | jq -r '.status') || {
        echo "Failed to check job status."; exit 3;
    }

    if [ -z "$STATUS" ]; then
        echo "Job status not available. Retrying in 5 seconds..."
        attempts=$((attempts + 1))
        sleep 5
    fi
done

echo "Job status: $STATUS"

# Verify job completion
if [ "$STATUS" != "COMPLETED" ]; then
    echo "Job did not complete successfully. Exiting."
    exit 5
fi

echo "Job completed successfully."
