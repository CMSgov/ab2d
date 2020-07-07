---
layout: tutorial_layout
title:  "Postman Tutorial"
date:   2019-11-02 09:21:12 -0500 
description: Postman Tutorial
landing-page: live
---

<style>
.ds-c-table td,
.ds-c-table th {
    padding: 0.3rem;
    font-size: small;
}
textarea {
    font-family: monospace, monospace;
    font-size: 1em;    
    background-color: #f3f6fa;
    border: solid 1px #dddddd;
    font: 1rem Consolas, "Liberation Mono", Menlo, Courier, monospace;
    font-size: .8rem;
    line-height: 1.25;
    color: #567482;
}
</style>

<script type="text/javascript">
    $(document).ready(function() {
        $('.highlight').css('overflow-y', 'auto').attr('tabindex', '0');
    });
</script>

This will provide step by step instructions on how to retrieve data the AB2D web service using Postman and the Swagger endpoint. 

## Postman

Postman is free and the download is available [here](https://www.postman.com/downloads/)

The Sandbox/Swagger page does not need to be downloaded and is available [here](https://sandbox.ab2d.cms.gov/swagger-ui.html)

## Authorization

First, we will prepare an "ab2d" Collection in Postman. 

1. Open Postman
 
1. Select the <b>New dropdown</b> from the top left of the page
 
1. Select <b>Collection</b>
 
1. Configure the "CREATE A NEW COLLECTION" page as follows
 
 <b>Name:</b> ab2d
 
1. Select <b>Create</b>
 
1. Close the "ab2d" panel that appears to the right of the leftmost panel
 
1. Hover over "ab2d" in the leftmost panel
 
1. Select ... beside "ab2d"
 
1. Select <b>Add Request</b>
 
1. Configure the "SAVE REQUEST" page as follows
 
  <b>Request name:</b> retreive-a-token
 
1. Select <b>Save to ab2d</b>
 
1. Expand the <b>ab2d</b> node
 
1. Select <b>GET retreive-a-token</b>
 
1. Change "GET" to "POST"
 
1. Configure the "POST retrieve-a-token" page as follows
 
  <b>Enter request URL:</b> https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token
 
1. Select the Params tab
 
1. Configure the "Params" tab as follows:

  <table class="ds-c-table">
  <thead>
      <tr>
          <th>Sponsor</th>
          <th>Contract</th> 
      </tr>
  </thead>
  <tbody>
      <tr>
          <td>grant_type</td>
          <td>client_credentials</td>
      </tr>
      <tr>
          <td>scope</td>
          <td>clientCreds</td>
      </tr>
  </tbody>
  </table>
 
1. There are 5 users (clients) setup in the sandbox

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
  
1. The basic authorization encoded user names and passwords for each of those 5 users are below for convenience
  
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
 
1. Choose the desired user to test and note its basic authorization
 
  Note that the first user has the least data and each successive user has a greater amount of data.
 
1. Select the <b>Headers</b> tab on the "POST retrieve-a-token" page
 
1. Configure the "Headers" tab as follows
 
  <table class="ds-c-table">
  <thead>
      <tr>
          <th>Key</th>
          <th>Value</th>
      </tr>
  </thead>
  <tbody>
      <tr>
          <td>Content-Type</td>
          <td>application/x-www-form-urlencoded</td>
      </tr>
      <tr>
          <td>Accept</td>
          <td>application/json</td>
      </tr>
      <tr>
          <td>Authorization</td>
          <td>Basic {Base64-encoded id:password}</td>
      </tr>
  </tbody>
  </table>

  Example:
 
  <table class="ds-c-table">
    <thead>
        <tr>
            <th>Key</th>
            <th>Value</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Content-Type</td>
            <td>application/x-www-form-urlencoded</td>
        </tr>
        <tr>
            <td>Accept</td>
            <td>application/json</td>
        </tr>
        <tr>
            <td>Authorization</td>
            <td>Basic MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==</td>
        </tr>
    </tbody> 
  </table>
 
1. Select Send
 
1. Verify that you get a response with an access token under the "Body" tab at the bottom of the page
 
  Format:
 
      {
          "token_type": "Bearer",
          "expires_in": 3600,
          "access_token": "{access token}",
          "scope": "clientCreds"
      }
 
  Example:
 
      {
          "token_type": "Bearer",
          "expires_in": 3600,
          "access_token": "eyJraWQiOiJRVkxaSkdWWHVPS20yZjdiTDNid1ludWEteWxMOWtZdUYtUmJTd0ZJV2ljIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULndOYnJWNXJlMGhEVjhMZHZISElQZGF1aHF5emFFWTdNVkNKdVBqbVE4Z1kiLCJpc3MiOiJodHRwczovL3Rlc3QuaWRwLmlkbS5jbXMuZ292L29hdXRoMi9hdXMycjd5M2dkYUZNS0JvbDI5NyIsImF1ZCI6IkFCMkQiLCJpYXQiOjE1ODMzMzM0MDYsImV4cCI6MTU4MzMzNzAwNiwiY2lkIjoiMG9hMnQwbHNyZFp3NXVXUngyOTciLCJzY3AiOlsiY2xpZW50Q3JlZHMiXSwic3ViIjoiMG9hMnQwbHNyZFp3NXVXUngyOTcifQ.frVhRMp7XtZL74ca2AqPDXpRXzmIMg5HlMoxbuDvOcvz4MSRXxmXMbGrQ0qfdk6fA49vQJ4KjoqNLTlP883DZP4tdxvZVApglrutipWH5vBRlU8kBRr-Ov0GNnIZPVgjooglGjd-9rCGJyH-9GNt2Hwrz2-Q5f51LgTDcR-zAS0REXw0D5KU8BfjhRUOWKNrwDZE8uSDV8UmnzIIZnrR74-ZxIdaDLX74M-WYq5UofZANp4E9C3tf-xTUF3LrQVrfygfxciHu5jc9jWuSQcH70FaQnPRUwTqlT3eYUDKJJeUExNeoC-WvStlbo-usjYKRu3O844ReDonPMSVm6UqFA",
          "scope": "clientCreds"
      }
 
1. Select <b>Save</b> near the top right of the page
 
1. Close the "POST retrieve-a-token" tab
 
1. Close Postman

### Retrieve a JSON Web Token (JWT)

1. Open Postman

1. Select the Collections tab in the leftmost panel

1. Expand the ab2d collection node

1. Select the POST retrieve-a-token

1. Select Send

1. Note the output

  Format:

      {
          "token_type": "Bearer",
          "expires_in": 3600,
          "access_token": "{access token}",
          "scope": "clientCreds"
      }

  Example:

      {
          "token_type": "Bearer",
          "expires_in": 3600,
          "access_token": "eyJraWQiOiJRVkxaSkdWWHVPS20yZjdiTDNid1ludWEteWxMOWtZdUYtUmJTd0ZJV2ljIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULndOYnJWNXJlMGhEVjhMZHZISElQZGF1aHF5emFFWTdNVkNKdVBqbVE4Z1kiLCJpc3MiOiJodHRwczovL3Rlc3QuaWRwLmlkbS5jbXMuZ292L29hdXRoMi9hdXMycjd5M2dkYUZNS0JvbDI5NyIsImF1ZCI6IkFCMkQiLCJpYXQiOjE1ODMzMzM0MDYsImV4cCI6MTU4MzMzNzAwNiwiY2lkIjoiMG9hMnQwbHNyZFp3NXVXUngyOTciLCJzY3AiOlsiY2xpZW50Q3JlZHMiXSwic3ViIjoiMG9hMnQwbHNyZFp3NXVXUngyOTcifQ.frVhRMp7XtZL74ca2AqPDXpRXzmIMg5HlMoxbuDvOcvz4MSRXxmXMbGrQ0qfdk6fA49vQJ4KjoqNLTlP883DZP4tdxvZVApglrutipWH5vBRlU8kBRr-Ov0GNnIZPVgjooglGjd-9rCGJyH-9GNt2Hwrz2-Q5f51LgTDcR-zAS0REXw0D5KU8BfjhRUOWKNrwDZE8uSDV8UmnzIIZnrR74-ZxIdaDLX74M-WYq5UofZANp4E9C3tf-xTUF3LrQVrfygfxciHu5jc9jWuSQcH70FaQnPRUwTqlT3eYUDKJJeUExNeoC-WvStlbo-usjYKRu3O844ReDonPMSVm6UqFA",
          "scope": "clientCreds"
      }

1. Copy and save the access token for use in the next sections

1. This bearer access token expires after 1 hour

## Bulk Export Job
These instructions will initiate a part A & B bulk claim export job

1. Create a new JWT bearer token before proceeding, if any of the following are true:
  - don't have a recent bearer token?
  - bearer token is older than 1 hour (expired)?
  - bearer token was created with a user that is different than the user that you want to use?

1. Open the Swagger web page for the API

  <i>Note that Chrome was used for our testing.</i>

  ```
  https://sandbox.ab2d.cms.gov/swagger-ui.html
  ```

1. Select Authorize on the right side of the page

1. Type the following in the <b>Value</b> text box using your latest access token

  <i>Note that the access token must be preceded by "Bearer" and there should be a space between "Bearer" and the access token.</i>

  Format:

  ```
  Bearer {access token}
  ```

1. Select <b>Authorize</b>

1. Select <b>Close</b>

1. If the export endpoints are not visible, select <b>Export</b> to view the export operations

1. Select <b>GET</b> beside "/api/v1/fhir/Patient/$export" for "Initiate Part A & B bulk claim export job" to expand the details of the operation

1. Select <b>Try it out</b>

1. Select <b>Execute</b>

1. Scroll down to the "Server response" section and note the "Code"

1. If the response code is 202, copy the job id from the "Response headers" section to your clipboard

  Format:

  ```
  content-location: http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/$status 
  ```

  Example:

  ```
  content-location: http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/afc222d1-a55b-403b-ad22-49f5aefec4b6/$status 
  ```

1. If the status endpoints are not visible, select <b>Status</b>

1. Select <b>GET</b> beside "/api/v1/fhir/Job/{jobUuid}/$status" for "Returns a status of an export job" to expand the details of the operation

1. Select <b>Try it out</b>

1. Paste the job id into the A job identifier text box

1. Select Execute

1. If the status is 202, it will tell you the percent complete.

  Example:

      x-progress: 7% complete

  Based on the progress, you can a wait a period of time and try the status check again by selecting <b>Execute</b> until you 
  see a status of 200

1. If the status is 200, save the job id and file(s) to download from the following line under the "Response body" text box

  Format:

      "url": "http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/file/{file to download}"

  Example:

      "url": "http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/afc222d1-a55b-403b-ad22-49f5aefec4b6/file/Z0000_0001.ndjson"

1. If the file download endpoint is not visible, select <b>Download</b>

1. Select <b>GET</b> beside "/api/v1/fhir/Job/{jobUuid}/file/{filename}" for "Downloads a file produced by an export job" to 
expand the details of the operation

1. Select <b>Try it out</b>

1. Type the file to download in the A file name text box

  Example:

      Z0000_0001.ndjson

1. Type the job id in the A job identifier text box

  Example:

      afc222d1-a55b-403b-ad22-49f5aefec4b6

1. Select <b>Execute</b>

1. Verify that you got a response code of 200. The results of the file appears within the "Response Body" text box.

  <i>Note at the top of the output it says "can't parse JSON.  Raw result:". This is normal because the specification 
  requires that the output be new line delimited JSON.</i>

1. Select <b>Download</b> within the "Response Body" text box at the bottom left of the text box. If the browser says that the 
page is unresponsive and asks you several times if you want to wait, do so.

1. Wait for the download to complete. The export file will appear in your downloads.

  Format:

      response_{unix time in milliseconds}.json

  Example:

      response_1583369581232.json

  If there was more than one file to download, do the following under the <b>GET</b> "/api/v1/fhir/Job/{jobUuid}/file/{filename}" endpoint

  a. Change the A file name text box to the next file name

  b. Select Execute

  c. Repeat the process for each file name that appeared in the output

1. If you are going to use Swagger again during this browser session, select Cancel under the three operations that you used in this section

      GET /api/v1/fhir/Job/{jobUuid}/$status
      GET /api/v1/fhir/Job/{jobUuid}/file/{filename}
      GET /api/v1/fhir/Patient/$export

## Bulk Export Contract Job
This section will show you how to initiate a Part A & B bulk claim export job for a given contract number.

1. Create a new JWT bearer token before proceeding, if any of the following are true:

  - don't have a recent bearer token?

  - bearer token is older than 1 hour (expired)?

  - bearer token was created with a user that is different than the user that you want to use?

1. Open the Swagger web page for the API

  <i>Note that Chrome was used for our testing.</i>

  [https://sandbox.ab2d.cms.gov/swagger-ui.html](https://sandbox.ab2d.cms.gov/swagger-ui.html)

1. Select <b>Authorize</b> on the right side of the page

1. Type the following in the <b>Value</b> text box using your latest access token

  Note that the access token must be preceded by "Bearer" and there should be a space between "Bearer" and the access token.

  Format:

      Bearer {access token}

1. Select <b>Authorize</b>

1. Select <b>Close</b>

1. If the export endpoints are not visible, select <b>Export</b> to view the export operations

1. Select <b>GET</b> beside "/api/v1/fhir/Group/{contractNumber}/$export" for "Initiate Part A & B bulk claim export job for a given contract number" to expand the details of the operation

1. Select <b>Try it out</b>

1. Type the desired contract number in the A contract number text box

  Example:

      Z0000

1. Select Execute

1. Scroll down to the "Server response" section and note the "Code"

1. If the response code is 202, copy the job id from the "Response headers" section to your clipboard

  Format:

      content-location: http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/$status 

  Example:

      content-location: http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/6c5df22b-61cc-4c3d-815f-4e56aa647699/$status 

1. If the status endpoints are not visible, select <b>Status</b>

1. Select <b>GET</b> beside "/api/v1/fhir/Job/{jobUuid}/$status" for "Returns a status of an export job" to expand the details of the operation

1. Select <b>Try it out</b>

1. Paste the job id into the A job identifier text box

1. Select <b>Execute</b>

1. If the status is 202, it will tell you the percent complete.

  Example:

      x-progress: 7% complete
      
  Based on the progress, you can a wait a period of time and try the status check again by selecting <b>Execute</b> until you see a status of 200

1. If the status is 200, note and save the job id and file(s) to download from the following line under the "Response body" text box

  Format:

      "url": "http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/{job id}/file/{file to download}"

  Example:

      "url": "http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/6c5df22b-61cc-4c3d-815f-4e56aa647699/file/Z0000_0001.ndjson"

1. If the download endpoint is not visible, select <b>Download</b>

1. Select <b>GET</b> beside "/api/v1/fhir/Job/{jobUuid}/file/{filename}" for "Downloads a file produced by an export job" to expand the details of the operation

1. Select <b>Try it out</b>

1. Type the file to download in the <b>A file name</b> text box

  Example:

      Z0000_0001.ndjson

1. Type the job id in the <b>A job identifier</b> text box

  Example:

      6c5df22b-61cc-4c3d-815f-4e56aa647699

1. Select <b>Execute</b>

1. Verify that you got a response code of 200. The results of the file appears within the "Response Body" text box.

  <i>Note at the top of the output it says "can't parse JSON.  Raw result:". This is normal because the specification requires that the output be new line delimited JSON.</i>

1. Select <b>Download</b> within the "Response Body" text box at the bottom left of the text box. If the browser says that the page is unresponsive and asks you several times if you want to wait, do so

1. Wait for the download to complete. The export file will appear in your downloads.

  Format:

      response_{unix time in milliseconds}.json

  Example:

      response_1583369581232.json

1. If there was more that one file to download, do the following under the <b>GET</b> “/api/v1/fhir/Job/{jobUuid}/file/{filename}” endpoint

  a. Change the A file name text box to the next file name

  b. Select <b>Execute</b>

  c. Repeat the process for each file name that appeared in the output

1. If you are going to use Swagger again during this browser session, select <b>Cancel</b> under the three operations that you used in this section

      GET /api/v1/fhir/Group/{contractNumber}/$export
      GET /api/v1/fhir/Job/{jobUuid}/$status
      GET /api/v1/fhir/Job/{jobUuid}/file/{filename}