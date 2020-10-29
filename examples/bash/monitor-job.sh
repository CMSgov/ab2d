#!/usr/bin/env bash

source fn_get_token.sh

BEARER_TOKEN=$(fn_get_token "$IDP_URL" "$AUTH")
if [ "$BEARER_TOKEN" == "null" ]
then
    printf "Failed to retrieve bearer token is base64 token accurate?\nIs %s available from this computer?\n", $IDP_URL
    exit 1
fi

JOB=$(cat "$DIRECTORY/jobId.txt")

echo "Id of job being monitored $JOB"

# Get the status
RESPONSE=$(curl "${API_URL}/Job/${JOB}/\$status" -sD - -H "accept: application/json" -H "Authorization: Bearer ${BEARER_TOKEN}")
URLS=$(echo "$RESPONSE" | grep ExplanationOfBenefit)

# If there are no results, wait x seconds and try again
while [ "$URLS" == '' ]
do
    # If response is unauthorized refresh token and try again
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP/" | awk  '{print $2}')

    if [ "$HTTP_CODE" == 403 ]
    then
        echo "Token expired refreshing and then attempting to check status again"
        BEARER_TOKEN=$(fn_get_token "$IDP_URL" "$AUTH")
        sleep 30
    elif [ "$HTTP_CODE" != 202 ] && [ "$HTTP_CODE" != 200 ]
    then
        echo "Error making rest call $HTTP_CODE"
        echo "Exiting without saving results"
        exit 1
    else

        echo "$RESPONSE"
        URLS=$(echo "$RESPONSE" | grep ExplanationOfBenefit)

        # Sleep and increment counter
        if [ "$URLS" == '' ]
        then
            sleep 60
        fi

        COUNTER=$(( COUNTER +1 ))

        # Log every ten seconds
        if [ $((COUNTER % 10)) == 0 ]
        then
          echo "Running for $COUNTER minutes"
        fi

    fi

    RESPONSE=$(curl "${API_URL}/Job/${JOB}/\$status" -sD - -H "accept: application/json" -H "Authorization: Bearer ${BEARER_TOKEN}")
done


echo "Job finished with
$RESPONSE"

echo "$RESPONSE" > "$DIRECTORY/response.json"

echo "Saved response to $DIRECTORY/response.json"
