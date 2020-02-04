---
layout: home
title:  "Claims Data to Part D Sponsors API Tutorial"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Authorization
  - Request Data
  - Download Data
ctas:

---
<style>
.ds-c-table td,
.ds-c-table th {
    padding: 0.3rem;
    font-size: small;
}
</style>
This will provide step by step instructions on how to retrieve data the AB2D web service. See [API](intro) 
for the full list of fictitious users. 

## Authorization
We will use the following user for our tutorials because it returns the least amount of data.
<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-100</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>S0000</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0oa2t0lsrdZw5uWRx297</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>HHduWG6LogIvDIQuWgp3Zlo9OYMValTtH5OBcuHw</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==</td>
        </tr>
    </tbody>
</table>

This will work in a bash shell for either OS X or Unix based environments. JQ (a JSON parser) may need to be 
installed but not entirely necessary (it helps us retrieve the correct field for the Bearer JWT in the Okta 
response).

### Retrieve a token
Because we use the Bearer JWT throughout the demo, first generate a value for it and save it to a variable. CMS leverages
[Okta](www.okta.com) for authentication and it is used to generate a JWT for our fictitious user to retrieve sample data.

```
BEARER_TOKEN=$(curl -X POST "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -H "Accept: application/json" \
        -H "Authorization: Basic MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==" | jq --raw-output ".access_token")
```

If you don't have jq installed, just manually set the value to BEARER_TOKEN to the value specified in access_token.

If you perform execute at the command line:

```
echo $BEARER_TOKEN
```

It should look like the token in the Postman example. You can now use ${BEARER_TOKEN} in the cURL commands.

## Request Data
Attempt to create an export job (notice that the $ in $export is escaped):

```
curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Patient/\$export?_outputFormat=application%2Ffhir%2Bndjson&_type=ExplanationOfBenefit" \
    -sD - \
    -H "accept: application/json" \
    -H "Accept: application/fhir+json" \
    -H "Prefer: respond-async" \
    -H "Authorization: Bearer ${BEARER_TOKEN}" 
```

You should get a response that looks like this:

```
HTTP/1.1 202 
Content-Location: https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/9ad207d2-11ff-45b8-9c6c-2d59b86ea45c/$status
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
Date: Wed, 29 Jan 2020 15:28:06 GMT
```
You will notice that the Content-Location header contains the URL to determine the status of the job. In this case, our Job Id is 9ad207d2-11ff-45b8-9c6c-2d59b86ea45c. Because we will need the Job Id for the next few commands, save it in the shell for convenience:

```
JOB=9ad207d2-11ff-45b8-9c6c-2d59b86ea45c
echo ${JOB}
```
should result in 9ad207d2-11ff-45b8-9c6c-2d59b86ea45c.

Next, query the status of the job:

```
curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/\$status" -sD - -H "accept: application/json" -H "Authorization: Bearer ${BEARER_TOKEN}"
```
If the job is still processing, it will return a 202 and a percent complete:

```
HTTP/1.1 202 
X-Progress: 20% complete
Retry-After: 5
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
Date: Wed, 29 Jan 2020 19:14:56 GMT
```

If the job is complete, it will return a 200: It will also display the location of the file(s) created for the batch job.

```
HTTP/1.1 200 
Expires: Thu, 30 Jan 2020 15:28:27 GMT
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
X-Frame-Options: DENY
vary: accept-encoding
Content-Type: application/json
Transfer-Encoding: chunked
Date: Wed, 29 Jan 2020 16:04:47 GMT
{
    "transactionTime":"Jan 29, 2020, 10:28:27 AM",
    "request":"https://sandbox.ab2d.cms.gov/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit",
    "requiresAccessToken":true,
    "output":[
        {
            "type":"ExplanationOfBenefit",
            "url":"https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/5298026c-e503-4d93-9974-c7732f56a0f8/file/S0000_0001.ndjson"
        }
    ],
    "error":[]
}
```

The JSON (formated here for readability) will contain the location of the files in the .output[].url values. 
S0001 indicates the contract number, 0001 indicates the file for that contract number. If the contract is big enough 
that breaking it into parts is warranted, you might see S0001_00002.ndjson, for example.

## Download Data

To download the content of the file (for each file that is specified):

```
curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/file/S0000.ndjson" \
    -H "accept: application/json" \
    -H "Accept: application/fhir+json" \
    -H "Authorization: Bearer ${BEARER_TOKEN}"
```

The contents of the file will be returned in a new line delimited JSON file.

