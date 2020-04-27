---
layout: home
title:  "Claims Data to Part D Sponsors API Tutorial"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Prerequisites &amp; Caveats
  - Retrieve JSON Web Token
  - Request Data
ctas:

---
<style>
.ds-c-table td,
.ds-c-table th {
    padding: 0.3rem;
    font-size: small;
}
</style>

## Prerequisites &amp; Caveats
These instructions have been tested on the following machines:
- Mac - [Setup Information](setup-mac.html)
- RedHat Linux - [Setup Information](setup-linux.html)
- Windows 10 (using Ubuntu Windows Linux File System terminal) - [Setup Information](setup-windows.html)

If you don't have a Mac, Linux or Windows 10 machine setup with both cUrl and jq, jump to the setup information page
associated with your machine.

## Retrieve JSON Web Token
There are 5 users (clients) setup in the sandbox

<table class="ds-c-table">
    <thead>
        <tr>
            <th>Sponsor</th>
            <th>Contract</th>
            <th>Client ID</th>
            <th>Client Password</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>PDP-100</td>
            <td>Z0000</td>
            <td>0oa2t0lsrdZw5uWRx297</td>
            <td>HHduWG6LogIvDIQuWgp3Zlo9OYMValTtH5OBcuHw</td>
        </tr>
        <tr>
            <td>PDP-1000</td>
            <td>Z0001</td>
            <td>0lc65ErV8OmY297</td>
            <td>GO6eglkXUDtjVjto3L-3C0offzTMk2qlz9r</td>
        </tr>
        <tr>
            <td>PDP-2000</td>
            <td>Z0002</td>
            <td>0oa2t0lkicpxFGkGt297</td>
            <td>eDpanJTtw90vY2viYlX4o2rgVRIR4tDRH0mWr9vN</td>
        </tr>
        <tr>
            <td>PDP-5000</td>
            <td>Z0005</td>
            <td>0oa2t0l6c1tQbTikz297</td>
            <td>80zX-7GeiMiiA6zVghiqYZL82oLAWSxhgfBkfo0T</td>
        </tr>
        <tr>
            <td>PDP-10000</td>
            <td>Z0010</td>
            <td>0oa2t0lm9qoAtJHqC297</td>
            <td>ybR60JmtcpRt6SAeLmvbq6l-3YDRCZP-WN1At6t_</td>
        </tr>
    </tbody>
</table>
<br>
The basic authorization encoded user names and passwords for each of those 5 users are below for convenience
<table class="ds-c-table">
    <thead>
        <tr>
            <th>Sponsor</th>
            <th>Base64-encoded id:password</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>0oa2t0lsrdZw5uWRx297</td>
            <td>MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==</td>
        </tr>
        <tr>
            <td>0lc65ErV8OmY297</td>
            <td>MG9hMnQwbGM2NUVyVjhPbVkyOTc6MUJsM0hHTzZlZ2xrWFVEdGpWanRvM0wtM0Mwb2ZmelRNazJxbHo5cg==</td>
        </tr>
        <tr>
            <td>0oa2t0lkicpxFGkGt297</td>
            <td>MG9hMnQwbGtpY3B4RkdrR3QyOTc6ZURwYW5KVHR3OTB2WTJ2aVlsWDRvMnJnVlJJUjR0RFJIMG1Xcjl2Tg==</td>
        </tr>
        <tr>
            <td>0oa2t0l6c1tQbTikz297</td>
            <td>nQwbDZjMXRRYlRpa3oyOTc6ODB6WC03R2VpTWlpQTZ6VmdoaXFZWkw4Mm9MQVdTeGhnZkJrZm8wVA==</td>
        </tr>
        <tr>
            <td>0oa2t0lm9qoAtJHqC297</td>
            <td>nQwbG05cW9BdEpIcUMyOTc6eWJSNjBKbXRjcFJ0NlNBZUxtdmJxNmwtM1lEUkNaUC1XTjFBdDZ0Xw==</td>
        </tr>
    </tbody>
</table>

1. Choose the desired user to test. The first user has the least data and each successive user has a greater amount of data.

2. Open a new terminal

3. Set the authorization for the desired user in the format:

    ```AUTH={basic authorization}```
    
    Example for the user 0oa2t0lsrdZw5uWRx297:
                         
    ```AUTH=MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==```
4. Retrieve the JWT bearer token by entering the following at the terminal prompt:

```
BEARER_TOKEN=$(curl -X POST "https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -H "Accept: application/json" \
    -H "Authorization: Basic ${AUTH}" \
| jq --raw-output ".access_token")
```

You will need this "BEARER_TOKEN" environment variable in order to do any of the following sections. All commands should
be in this same terminal window since the value of the environment variable is only set in that terminal.


The bearer token will expire in 1 hour.

## Request Data
In this section, you will initiate a Part A & B bulk explanation of benefit export job.

<i style="font-size: small">Note to Windows users - When you are instructed to "open a terminal", the instructions are assuming that you are 
opening ubuntu or some other terminal that supports curl and jq. See [Windows 10 Setup information](setup-windows.html).</i>

1. Open a terminal

2. Create a new JWT bearer token before proceeding if any of the following are true:
    - bearer token environment variables does not exist
    - bearer token is older than 1 hour (expired)
    - bearer token was created with a user different than the user that you want to use
3. Ensure that you have a "BEARER_TOKEN" environment variable defined before proceeding:

    ```
    echo $BEARER_TOKEN
    ```
4. Create an export job

    <i>Note that the $ in $export is escaped</i>

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
    HTTP/1.1 {response code}
    Content-Location: https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/$status
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    Cache-Control: no-cache, no-store, max-age=0, must-revalidate
    Pragma: no-cache
    Expires: 0
    X-Frame-Options: DENY
    Content-Length: 0
    Date: Wed, 29 Jan 2020 15:28:06 GMT
    ```

    Example:

    ```
    HTTP/2 202 
    date: Tue, 03 Mar 2020 21:08:36 GMT
    content-length: 0
    content-location: http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/c40e30e6-0913-4803-9b05-bb0563f0cac6/$status
    x-content-type-options: nosniff
    x-xss-protection: 1; mode=block
    cache-control: no-cache, no-store, max-age=0, must-revalidate
    pragma: no-cache
    expires: 0
    x-frame-options: DENY
    ```

    Note the response and job id from the output
   
    Example:

    ```
    {response-code} = 202 
    {job id} = c40e30e6-0913-4803-9b05-bb0563f0cac6
    ``` 
    
5. Below are the possible response codes:

    <table class="ds-c-table">
    <thead>
        <tr>
            <th>Response Code</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>202</td>
            <td>Export request has started</td>
        </tr>
        <tr>
            <td>400</td>
            <td>There was a problem with the request. The body will contain a FHIR OperationOutcome resource in JSON format. https://www.hl7.org/fhir/operationoutcome.html Please refer to the body of the response for details.</td>
        </tr>
        <tr>
            <td>401</td>
            <td>Unauthorized. Missing authentication token. </td>
        </tr>
        <tr>
            <td>403</td>
            <td>Forbidden. Access not permitted.</td>
        </tr>
        <tr>
            <td>500</td>
            <td>An error occurred. The body will contain a FHIR OperationOutcome resource in JSON format. https://www.hl7.org/fhir/operationoutcome.html Please refer to the body of the response for details.</td>
        </tr>
    </tbody>
    </table>

6. If the response code is 202, set an environment variable for the job by entering the following at the terminal prompt

    Format:

    ```
    JOB={job id}
    ```

    Example:

    ```
    JOB=c40e30e6-0913-4803-9b05-bb0563f0cac6
    ```
    
7. Check the status of the job by entering the following at the terminal prompt

    Notice that the $ in $status is escaped.

    ```
    curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/\$status" \
        -sD - \
        -H "accept: application/json" \
        -H "Authorization: Bearer ${BEARER_TOKEN}"
    ```
   
8. Note the output

    Format:

    ```
    HTTP/2 {response code}
    date: Tue, 03 Mar 2020 22:48:36 GMT
    content-type: application/json
    expires: Wed, 4 Mar 2020 22:47:24 GMT
    x-content-type-options: nosniff
    x-xss-protection: 1; mode=block
    x-frame-options: DENY
    vary: accept-encoding

    {"transactionTime":"Mar 3, 2020, 10:47:24 PM","request":"http://sandbox.ab2d.cms.gov/api/v1/fhir/Patient/$export?_outputFormat=application%252Ffhir%252Bndjson&_type=ExplanationOfBenefit","requiresAccessToken":true,"output":[{"type":"ExplanationOfBenefit","url":"http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/file/{file to download}"}],"error":[]}
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
                "url":"https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/5298026c-e503-4d93-9974-c7732f56a0f8/file/Z0000_0001.ndjson"
            }
        ],
        "error":[]
    }
    ```

    The JSON (formated here for readability) will contain the location of the files in the .output[].url values. Z0001 indicates the contract number, 0001 indicates the file for that contract number. If the contract is big enough that breaking it into parts is warranted, you might see Z0001_00002.ndjson, for example.

    9. Note the response code and file to download from the output

    Example:

    ```
    {response code} = 200
    {file to download} = Z0000_0001.ndjson
    ```

    The following are the possible response codes

    <table class="ds-c-table">
    <thead>
        <tr>
            <th>Response Code</th>
            <th>Description</th>
        </tr>
    </thead>
        <tr>
            <td>200	The job is completed.
            <td>202	The job is still in progress.
        </tr>
        <tr>
            <td>400</td>
            <td>There was a problem with the request. The body will contain a FHIR OperationOutcome resource in 
            JSON format. https://www.hl7.org/fhir/operationoutcome.html Please refer to the body of the response for details.</td>
        </tr>
        <tr>
            <td>401</td>
            <td>Unauthorized. Missing authentication token.</td>
        </tr>
        <tr>
            <td>403</td>
            <td>Forbidden. Access not permitted.</td>
        </tr>
        <tr>
            <td>404</td>
            <td>Job not found. The body will contain a FHIR OperationOutcome resource in JSON format. 
            https://www.hl7.org/fhir/operationoutcome.html Please refer to the body of the response for details.</td>
        </tr>
        <tr>
            <td>500</td>
            <td>An error occurred. The body will contain a FHIR OperationOutcome resource in JSON format. 
            https://www.hl7.org/fhir/operationoutcome.html Please refer to the body of the response for details.</td>
        </tr>
    </table>

10. If the status is 202, do the following

    a. Note the following in the output, for example:

        x-progress: 7% complete
   
    b. Based on the progress, you can a wait a period of time and try the status check again until you see a status of 200

11. If the status is 200, download the files by doing the following:

    a. Set an environment variable to the first file to download

    Format:

    ```
    FILE={file to download}
    ```

    Example:

    ```
    FILE=Z0000_0001.ndjson
    ```

    b. Get the Part A & B bulk claim export data by entering the following at the terminal prompt

    ```
    curl "https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/${JOB}/file/${FILE}" \
        -H "accept: application/json" \
        -H "Accept: application/fhir+json" \
        -H "Authorization: Bearer ${BEARER_TOKEN}" \
    > ${FILE}
    ```

    c. Wait for the process to complete

    d. The following file has been created in your current directory

    Format:

        {file to download}

    Example:

        Z0000_0001.ndjson

    e. Verify that there is data in the file by entering the following at the terminal prompt

    Format:

        cat {file to download}    

    Example:

        cat Z0000_0001.ndjson

    f. Repeat this process to download additional files (if any)

    g. After the file has been downloaded for a given job, it can't be downloaded again using the same job. 
    If you want to download the data again, a new export job would need to be initiated