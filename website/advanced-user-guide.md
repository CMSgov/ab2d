---
layout: tutorial_layout
title:  "Advanced User Guide"
date:   2019-11-02 09:21:12 -0500 
description: Advanced User Guide
landing-page: live
---


<style>
.ds-c-table td,
.ds-c-table th {
    padding: 0.3rem;
    font-size: small;
}
</style>
## Introduction

This is an advanced user's guide on how to use the AB2D API to bulk search select Part A and Part B claims data by 
Medicare Part D plan (PDP) sponsors. This service provides asynchronous access to the data. The sponsor will request data, a 
job will be created to retrieve this data, a job number returned, the job will be processed and once it is complete, 
the data will be available to download. The status of the job can be requested at any time to determine whether it is complete.
The Sandbox is available to everyone but to get access to the production site, the PDP sponsor must first attest and
then verify that they can successfully download sample claim data from the Sandbox. [HAPI FHIR](https://hapifhir.io/) 
was used to implement the [HL7 FHIR standard](https://www.hl7.org/fhir/overview.html) and the API follows the
[FHIR Bulk Data Export](https://hl7.org/fhir/uv/bulkdata/export/index.html) pattern to perform data export. Errors come
back in the [Resource OperationOutcome](errors come back in the https://www.hl7.org/fhir/operationoutcome.html) format.

### Sandbox
The Sandbox/Swagger page is available [here](https://sandbox.ab2d.cms.gov/swagger-ui/index.html).

## Authentication and Authorization
The API uses the JSON Web Tokens (JWT) to authorize use of the endpoints. The token should be sent using the 
"Authorization" header field with the value specified as "Bearer xxxxx" where xxxxx is the value of the JWT. 

There are 5 users set up in the Sandbox. They are useful to view different types and sizes of data. For example, 
sponsor PDP-100 will have 100 patients and PDP-1000 will have 1000 patients. Each has one contract associated with it.

<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-100</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>Z0000</td>
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
<br/>

<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-1000</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>Z0001</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0oa2t0lc65ErV8OmY297</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>1Bl3HGO6eglkXUDtjVjto3L-3C0offzTMk2qlz9r</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>MG9hMnQwbGM2NUVyVjhPbVkyOTc6MUJsM0hHTzZlZ2xrWFVEdGpWanRvM0wtM0Mwb2ZmelRNazJxbHo5cg==</td>
        </tr>
    </tbody>
</table>
<br/>
<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-2000</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>Z0002</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0oa2t0lkicpxFGkGt297	</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>eDpanJTtw90vY2viYlX4o2rgVRIR4tDRH0mWr9vN</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>MG9hMnQwbGtpY3B4RkdrR3QyOTc6ZURwYW5KVHR3OTB2WTJ2aVlsWDRvMnJnVlJJUjR0RFJIMG1Xcjl2Tg==</td>
        </tr>
    </tbody>
</table>
<br/>
<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-5000</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>Z0005</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0oa2t0l6c1tQbTikz297</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>80zX-7GeiMiiA6zVghiqYZL82oLAWSxhgfBkfo0T</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>MG9hMnQwbDZjMXRRYlRpa3oyOTc6ODB6WC03R2VpTWlpQTZ6VmdoaXFZWkw4Mm9MQVdTeGhnZkJrZm8wVA==</td>
        </tr>
    </tbody>
</table>
<br/>
<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-10000</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>Z0010</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0oa2t0lm9qoAtJHqC297</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>ybR60JmtcpRt6SAeLmvbq6l-3YDRCZP-WN1At6t_</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>MG9hMnQwbG05cW9BdEpIcUMyOTc6eWJSNjBKbXRjcFJ0NlNBZUxtdmJxNmwtM1lEUkNaUC1XTjFBdDZ0Xw==</td>
        </tr>
    </tbody>
</table>

CMS leverages [Okta](http://www.okta.com) for authentication to generate a JWT. Use the  "Client Id" and "Client 
Password" you've chosen from above. The Okta request should look like this:

```
POST https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token
Headers:
        Content-Type: application/x-www-form-urlencoded
        Accept: application/json
Parameters:
        grant_type: client_credentials
        scope: clientCreds
Authorization: Basic Auth
```

Basic Auth uses a Base64-encoded value for ClientId:ClientPassword. The table above, as a convenience, provides that 
encoded information for each Client Id, Client Password combination. The response will contain an access_token, which 
will expire in 1 hour from the time it was obtained. The access_token will be used in the authorization header in 
the AB2D API to authenticate using the syntax

```
Bearer <access_token>
```

## API Endpoints
The following are the list of endpoints the API supports:

### Request data
A job will be created and a job identifier returned. You can request either by contract number:

```
GET /api/v1/fhir/Group/{contractNumber}/$export
```

or all Part D patients registered with the sponsor:

```
GET /api/v1/fhir/Patient/$export
```

#### Parameters

The _since parameter can be used to limit data to only data that has been updated since the specified parameter.
The format is the [ISO 8601 DateTime standard](https://www.w3.org/TR/NOTE-datetime) e.g. YYYY-MM-DDThh:mm:ssTZD

```
GET /api/v1/fhir/Patient/$export?_since=2020-03-16T00:00:00-05:00
```

Dates prior to 2020-02-13 are not supported and will result in a failure response.

### Status
Once a job has been created, the user can/should request the status of the submitted job. 

```
GET /api/v1/fhir/Job/{jobUuid}/$status
```

The job will either be in progress or completed. The application will limit the frequency in which a job status may 
be queried. The value of "retry-after" passed back in the response header should indicate a minimum amount of time 
between status checks. Once the job is complete, this request will respond with the list of files containing the bulk 
data or any error messages.

### Download
Once the search job has been completed, the contents of the of the created file(s) can be downloaded by using:

```
GET /api/v1/fhir/Job/{jobUuid}/file/{filename}
```

The file(s) are specified as the output of the status request. Each file will only be available for 72 hours after the 
job has completed. Files are also unavailable after they have been successfully downloaded. 

### Cancellation
A job may be cancelled at any point during its processing:

```
DELETE /api/v1/fhir/Job/{jobUuid}/$status
```

### Other
Retrieve the capabilities of the server (required by the standard)

```
GET /api/v1/fhir/metadata
```

### Warning

As required by the FHIR Bulk Export specification, the files are provided in NDJSON format, which is essentially 
plain-text. Depending on the number of patients in a Part D contract, these files can become rather large. 
The API does support "Content-Encoding: gzip" when serving the files so that the downloads could complete faster and 
use less bandwidth. Thus, it is strongly encouraged that the API client you build also supports compressed media 
types and properly advertises so by specifying  "Accept-Encoding: gzip, deflate" header.

## Tutorials

The purpose of these instructions is to provide a way for users to interact with the AB2D API. There are two main ways 
to do that:

### Postman and Swagger. 
Swagger is included with the API and the free Postman application can be downloaded [here](https://www.postman.com/).
If you want to use Postman and Swagger jump to the following page - [Use the API with Postman and Swagger](tutorial-postman.md)

### Using cURL and jq

If you currently don't have both curl and jq installed, jump to the setup page associated with your machine

- [Setup Linux](setup-linux.html)
- [Setup Mac](setup-mac.html)
- [Setup Windows 10](setup-windows.html)

If you already have your machine setup to use both curl and jq, you can jump to the following page - 
[Use the API with Curl](tutorial-curl.html)

## Troubleshooting

While testing, common issues may come up. Here are some suggestions on how to address them:

### 401 HTTP Response - Forbidden
Your token is either incorrect or has expired

### 403 HTTP response - Unauthorized

- Your token has expired
- You have specified a contract that is not yours
- You are not a user authorized to use the service

### 404 HTTP Response - Page not found
- The page doesn't exist. Check the URL to make sure it exists. Put it in a browser and see what the error is. You will 
not have passed credentials or necessary parameters so it will give you another error but it shouldn't give you a 404.
- If you are using cURL at the command line, you may have to escape characters. For example, $ is used in $export and 
$status but $ means a variable value in the bash command line.

### Unable to Download Bulk Data File
- Your file name or job name are not correct. You can call the $status command again and verify that you have the file 
name & job name correct.
- You can only download the file once. If you have already done that, it no longer exists on our system
- The time between when the job completed and you requested the file was greater than 72 hours. Files are 
automatically deleted (or expired) after 72 hours.
- There was an error on our server. If this continues to happen, contact technical support

### Other
If none of these hints work, you can always start a new job and retrieve the information again.