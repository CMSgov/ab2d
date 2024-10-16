#!/bin/zsh

set -e


BASIC_AUTH=$(aws secretsmanager get-secret-value --secret-id ab2d/ab2d-dev/jenkins-verify-basic-auth --query SecretString --output text)

echo "Checking status"
max_attempts=90
while [ $STATE != 401 ]; do
    if [[ ${attempts} -eq ${max_attempts} ]];then
        echo "Max attempts reached timing out"
        exit 1
    fi
    STATE=$(curl -sLk -w "%{http_code}" "https://dev.ab2d.cms.gov" -o /dev/null)
    echo $STATE
    echo 'Unreachable, sleeping for 10 seconds . . .'
    attempts=$(($attempts+1))
    sleep 10
done

echo 'AB2D API is online . . .'

echo "Refreshing token"
# Get PDP-100 token
export TOKEN=$(curl -s --location --request POST 'https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header "Authorization: Basic '${BASIC_AUTH}'" \
--header 'Cookie: JSESSIONID=3E7BD665DE5673C73A82647BBD9E548A' \
| jq -r '.access_token')

# Start PDP-100 job
echo "Starting PDP-100 job"
curl -k -s --head --location --request GET 'https://dev.ab2d.cms.gov/api/v2/fhir/Patient/$export?_type=ExplanationOfBenefit&_since=2020-02-13T00:00:00.000-05:00&_outputFormat=application%2Ffhir%2Bndjson' \
--header "Prefer: respond-async" \
--header "Authorization: Bearer $TOKEN" \
| awk -v FS=": " "/^content-location/{print $2}" | sed 's/content-location: //' | tr -d '\r' | read JOB 

# Check on job status
echo "Checking job status..."
STATUS=""
while [ -z $STATUS ]; do
    sleep 5
    echo "$JOB"
    STATUS=$(curl -k -s --location --request GET "${JOB}" \
    --header "Authorization: Bearer $TOKEN")
done

echo "Complete"
