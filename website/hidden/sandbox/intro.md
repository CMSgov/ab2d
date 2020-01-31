---
layout: home
title:  "Claims Data to Part D Sponsors API"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Introduction
  - API Use  
  - Tutorials
  - Troubleshooting
ctas:

---
<style>
.ds-c-table td,
.ds-c-table th {
    padding: 0.3rem;
    font-size: small;
}
</style>
## Introduction

This site is a developer's guide on how to use the AB2D API to bulk search select Part A and Part B claims data by Medicare Part D plan (PDP) sponsors. The AB2D API is a RESTful based web service that allows PDP sponsors to retrieve Part A and Part B claim data. This service provides asynchronous access to the data. The sponsor will request data, a job will be created to retrieve this data, a job number returned, the job will be processed and once it is complete, the data will be available to download. The status of the job can be requested at any time to determine whether it is complete.

Data will be limited to a subset of explanation of benefit data records by the following constraints:

- Only claim data belonging to the PDP sponsor's active enrollees list
- Only data from active enrollees who did not opt out of data sharing by calling 1-(800)-Medicare (1-800-633-4227)
- Only Part A and Part B data. Part D data is excluded
- Claims with disease codes related to Substance Abuse and Mental Health are excluded
- Only data specified by the Secretary of Health and Human Services within the explanation of benefit object (not all data in the explanation of benefit object is included in the returned object)

### Data Use and Limitations
This data may be used for:

- Optimizing therapeutic outcomes through improved medication use
- Improving care coordination so as to prevent adverse healthcare outcomes, such as preventable emergency department visits and hospital readmissions
- For any other purposes determined appropriate by the Secretary

The sponsors may not use the data:

- To inform coverage determination under Part D
- To conduct retroactive reviews of medically accepted conditions
- To facilitate enrollment changes to a different or a MA-PD plan offered b the same parent organization
- To inform marketing of benefits
- For any other purpose the Secretary determines is necessary to include in order to protect the identity of individuals entitled to or enrolled in Medicare, and to protect the security of personal health information.

### Access
To get access to the sandbox environment go to https://sandbox.ab2d.cms.gov/swagger-ui.html

### Legislation
[Bipartisan Budget Act of 2018](https://www.congress.gov/bill/115th-congress/house-bill/1892/text) - The actual law responsible for creating the need of the AB2D API (Section 50323)

[Final Rule](https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare) 
- More detailed instruction on the implementation of the law

### Notes
[HAPI FHIR](https://hapifhir.io/) was used to implement the [HL7 FHIR standard](https://www.hl7.org/fhir/overview.html)

[FHIR Bulk Data Export](https://hl7.org/fhir/uv/bulkdata/export/index.html) - Details on the API pattern to perform bulk data export

## API Use

The AB2D API is a RESTful based web service that allows PDP sponsors to retrieve Part A and Part B claim data. The service provides asynchronous access to bulk data. The first thing you need to do is get a token to use with the API.

### Authentication and Authorization
The API uses the JSON Web Tokens (JWT) to authorize use of the endpoints. The token should be sent using the 
"Authorization" header field with the value specified as "Bearer xxxxx" where xxxxx is the value of the JWT. The 
Swagger page allows the requester to test the API by clicking on the "Authorize" button and then specifying a 
"Bearer xxxxx" value. Once you authorize the Swagger page, all endpoints will add that Authorization header to their 
request and the lock next to the end point will go from unlocked to locked. To verify that this is working, inspection 
of the generated curl statement should include the token in the header.

There are 6 users set up in the Sandbox. They are useful to view different types and sizes of data. For example, 
sponsor PDP-100 will have 100 patients and PDP-30000 will have 30000 patients. Each has one contract associated with it.

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
<br/>

<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-1000</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>S0001</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0lc65ErV8OmY297	</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>GO6eglkXUDtjVjto3L-3C0offzTMk2qlz9r</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>nQwbGM2NUVyVjhPbVkyOTc6MUJsM0hHTzZlZ2xrWFVEdGpWanRvM0wtM0Mwb2ZmelRNazJxbHo5cg==</td>
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
            <td>S0002</td>
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
            <td>S0005</td>
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
            <td>S0010</td>
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

<br/>
<table class="ds-c-table">
    <tbody>
        <tr>
            <td><b>Sponsor</b></td>
            <td>PDP-30000</td>
        </tr>
        <tr>
            <td><b>Contract</b></td>
            <td>S0030</td>
        </tr>
        <tr>
            <td><b>Client ID</b></td>
            <td>0oa2t0lrjyVeVAZjt297</td>
        </tr>
        <tr>
            <td><b>Client Password</b></td>
            <td>kpJkYR2k7CfojrzuZAha0CVVN9PtFKBW4M2ADRKx</td>
        </tr>
        <tr>
            <td><b>Base64-encoded id:password</b></td>
            <td>MG9hMnQwbHJqeVZlVkFaanQyOTc6a3BKa1lSMms3Q2ZvanJ6dVpBaGEwQ1ZWTjlQdEZLQlc0TTJBRFJLeA==</td>
        </tr>
    </tbody>
</table>

CMS leverages [Okta](http://www.okta.com) for authentication to generate a JWT. Use the  "Client Id" and "Client Password" you've chosen 
from above. The Okta request should look like this:

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
You can either use a tool such as Postman that allows you to specify the username & password, 
or create the header Authorization with value "Basic xxxx" where xxxx is a base64 encoded value of ClientId:ClientPassword
The response will contain an access_token, which will expire in 1 hour from the time it was obtained. The access_token 
will be used in the authorization header in the AB2D service to authenticate the API, using the syntax

```
Bearer <access_token>
```

API Endpoints
The following are the list of endpoints the API supports:

### Request data
A job will be created and a job identifier returned. You can request either by contract number:

```GET /api/v1/fhir/Group/{contractNumber}/$export```

or all Part D patients registered with the sponsor:

```GET /api/v1/fhir/Patient/$export```

### Status
Once a job has been created, the user can/should request the status of the submitted job. 

```GET /api/v1/fhir/Job/{jobUuid}/$status```

The job will either be in progress or completed. The application will limit the frequency in which a job status may 
be queried. The value of "retry-after" passed back in the response header should indicate a minimum amount of time 
between status checks. Once the job is complete, this request will respond with the list of files containing the bulk 
data or any error messages.

### Download
Once the search job has been completed, the contents of the of the created file(s) can be downloaded by using:

```GET /api/v1/fhir/Job/{jobUuid}/file/{filename}```

The file(s) are specified as the output of the status request. The files will only be available for 24 hours after the 
job starts. Files are also unavailable after they have been successfully downloaded. The contents of the file will 
be in ndjson (New Line Delimited JSON) of explanation of benefit objects.

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

As required by the FHIR Bulk Export specification, the files are provided in NDJSON format, which is essentially plain-text. Depending on the number of patients in a Part D contract, these files can become rather large. The API does support "Content-Encoding: gzip" when serving the files so that the downloads could complete faster and use less bandwidth. Thus, it is strongly encouraged that the API client you build also supports compressed media types and properly advertises so by specifying  "Accept-Encoding: gzip, deflate" header.

Future versions of this system will also support generating export output files in zip format.

## Tutorials

There are two tutorials for the Sandbox:

- A [tutorial](tutorial-postman) that leverages Postman and the Swagger docs. This is the easier tutorial and
walks through the interaction of the Postman UI and Swagger UI.

- A [tutorial](tutorial-curl) that uses cURL. This tutorial expects more experience with the BASH command line.

## Troubleshooting

Sometimes things do not go as planned and you get stuck on an error and don't know where to go next. Here are some common errors and how to fix them:

#### 401 HTTP Response - Forbidden
Your token is either incorrect or has expired

#### 403 HTTP response - Unauthorized
There can be several reasons for this

- Your token has expired
- You have specified a contract that is not yours
- 404 HTTP Response - Page not found
- Check the URL to make sure it exists. Put it in a browser and see what the error is. You will not have passed credentials so it will give you another error but it should be able to see it.
- If you are using cURL at the command line, you may have to escape characters such as & and $ since these often have meaning in the command line. For example, $ is used in $export and $status and parameters for the URL contain & to separate parameters.

#### Unable to Download Bulk Data File
This could be for several reasons:

- Call the $status command and verify that you have the file name & job name correct
- You may only download the file once. If you have already done that, it no longer exists on our system
- The time between when you started the job and you requested the file was greater than 24  hours. Files are automatically deleted (or expired) after 24 hours.
- There was an error on our server. If this continues to happen, contact technical support

You can always start a new job and retrieve the information again.