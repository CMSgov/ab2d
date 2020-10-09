#!/bin/bash
# Parameters:
#   1 - URL to run against
#   2 - Base64 encoded clientId:clientPassword
#   3 - The contract number (Optional)

source fn_get_token.sh

# Refresh bearer token
BEARER_TOKEN=$(fn_get_token "$IDP_URL" "$AUTH")
if [ "$BEARER_TOKEN" == "null" ]
then
  printf "Failed to retrieve bearer token is base64 token accurate?\nIs %s available from this computer?\n", $IDP_URL
  exit 1
fi

if [ "$CONTRACT" != '' ]
then
    echo "Getting contract $CONTRACT"
    RESULT=$(curl "${API_URL}/Group/$CONTRACT/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
        -sD - \
        -H "accept: application/json" \
        -H "Accept: application/fhir+json" \
        -H "Prefer: respond-async" \
        -H "Authorization: Bearer ${BEARER_TOKEN}")
# Create a job
else
    RESULT=$(curl "${API_URL}/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
        -sD - \
        -H "accept: application/json" \
        -H "Accept: application/fhir+json" \
        -H "Prefer: respond-async" \
        -H "Authorization: Bearer ${BEARER_TOKEN}")
fi

echo "$RESULT"

JOB=$(echo "$RESULT" | grep "content-location" | sed 's/.*Job.//' | sed 's/..status//' | tr -d '[:space:]')

if [ "$JOB" == '' ]
then
  echo "Could not create job"
  exit 1
fi

echo "$JOB created"

echo "$JOB" > "$DIRECTORY/jobId.txt"

echo "Saved job id to $DIRECTORY/jobId.txt"
