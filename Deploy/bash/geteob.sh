#!/bin/bash
# Parameters:
#   1 - URL to run against
#   2 - Base64 encoded clientId:clientPassword
#   3 - The contract number (Optional)

# Set to the name of the service you're testing
SANDBOX=$1

# Set up the auth to use for Okta
AUTH=$2

# Get the Bearer token from Okta
BEARER_TOKEN=$(curl -X POST "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -H "Accept: application/json" \
    -H "Authorization: Basic ${AUTH}" | jq --raw-output ".access_token")

if [ "$3" != '' ]
then
    echo Getting contract $3
    JOB=$(curl "${SANDBOX}/Group/$3/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
        -sD - \
        -H "accept: application/json" \
        -H "Accept: application/fhir+json" \
        -H "Prefer: respond-async" \
        -H "Authorization: Bearer ${BEARER_TOKEN}" | grep "Content-Location" | sed 's/.*Job.//' | sed 's/..status//' | tr -d '[:space:]')
# Create a job
else
    JOB=$(curl "${SANDBOX}/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
        -sD - \
        -H "accept: application/json" \
        -H "Accept: application/fhir+json" \
        -H "Prefer: respond-async" \
        -H "Authorization: Bearer ${BEARER_TOKEN}" | grep "Content-Location" | sed 's/.*Job.//' | sed 's/..status//' | tr -d '[:space:]')
fi
echo $JOB created

# Get the status
URLS=$(curl "${SANDBOX}/Job/${JOB}/\$status" -sD - -H "accept: application/json" -H "Authorization: Bearer ${BEARER_TOKEN}" | grep ExplanationOfBenefit | jq --raw-output ".output[].url")
# If there are no results, wait 5 seconds and try again
while [ "$URLS" == '' ]
do
    URLS=$(curl "${SANDBOX}/Job/${JOB}/\$status" -sD - -H "accept: application/json" -H "Authorization: Bearer ${BEARER_TOKEN}" | grep ExplanationOfBenefit | jq --raw-output ".output[].url")
    echo "SLEEPING 5"
    sleep 5
done

# Print out the list of URLs
echo $URLS

# Iterate over the URLs
for URL in ${URLS}
do
    FILE_NAME=$(echo ${URL} | sed 's/.*.file.//')
    curl --header "accept: application/json" \
        --header "Accept: application/fhir+json" \
        --header "Authorization: Bearer ${BEARER_TOKEN}" \
        "$URL" > $FILE_NAME
    echo Created file ${FILE_NAME}
done

