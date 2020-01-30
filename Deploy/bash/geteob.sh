#!/bin/bash

# Set to the name of the service you're testing
SANDBOX=http://sandbox.ab2d.cms.gov/api/v1/fhir

# Set up the auth to use for Okta
AUTH=MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==


# Get the Bearer token from Okta

BEARER_TOKEN=$(curl -X POST "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -H "Accept: application/json" \
        -H "Authorization: Basic ${AUTH}" | jq --raw-output ".access_token")

# Create a job

JOB=$(curl "${SANDBOX}/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
        -sD - \
        -H "accept: application/json" \
        -H "Accept: application/fhir+json" \
        -H "Prefer: respond-async" \
        -H "Authorization: Bearer ${BEARER_TOKEN}" | grep "Content-Location" | sed 's/.*Job.//' | sed 's/..status//' | tr -d '[:space:]')
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
        curl "$URL" \
                -H "accept: application/json" \
                -H "Accept: application/fhir+json" \
                -H "Authorization: Bearer ${BEARER_TOKEN}"
done
