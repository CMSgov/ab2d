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

### Retrieve a token

Because we use the Bearer JWT throughout the demo, we need to generate one. CMS leverages
[Okta](www.okta.com) for authentication and it is used to generate a JWT for our fictitious user to retrieve sample data.

```
https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token
```

In Postman enter the URL for the Okta server and the request type POST:

Add the parameters grant_type=client_credentials, scope=clientCreds. This information will be added to the URL. Add header information:

<img src="/assets/img/sandbox/postmanheaders.png" width="800px" alt="Auth Header"/>

Next, enter Authorization information with the values of the Client Id and Client Password using Basic Auth:

<img src="/assets/img/sandbox/postmanauth.png" width="800px" alt="Auth Vals"/>

When you send the URL, this will create a header "Authorization" with value:

```
Basic MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==
```

Basic Authorization is just "Basic " + Base64 encoded(username:password). The fictitious user table contains the Basic token value for the client Id/password combination for convenience.

Click the "Send" button

This is what the response will look like:

<img src="/assets/img/sandbox/authtoken.png" width="800px" alt="Authentication Response"/>

This is what is passed into the API to retrieve data. It does expire after an hour so it may be helpful to keep the 
Postman window up. You can resend the request with the same parameters to get a new token.

### Secure the API
Now that you have a token, you can query the API/Web Service. Visit the 
[Swagger Page](http://sandbox.ab2d.cms.gov/swagger-ui.html). To view the endpoints we will be 
using, click on the "bulk-data-access-api" text:

<img src="/assets/img/sandbox/swaggerenpoints_not_secure.png" width="800px" alt="Insecure Endpoints"/>

Using the JWT you generated in the previous section, Authorize the endpoints by clicking the "Authorize" button: 

![Authorize Button](/assets/img/sandbox/authorizebutton.png)

The following dialog will appear:

![Secure Endpoints](/assets/img/sandbox/authorizationdialog.png)

Enter the value "Bearer xxx" where xxx is the token you received from Okta. In our example, the value would be:

```
Bearer eyJraWQiOiJRVkxaSkdWWHVPS20yZjdiTDNid1ludWEteWxMOWtZdUYtUmJTd0ZJV2ljIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULmtkNW5qUVBHaDk2MURjZ2NmWUdnanN2RURQMkhxc2MtMnF5V2JfempHeE0iLCJpc3MiOiJodHRwczovL3Rlc3QuaWRwLmlkbS5jbXMuZ292L29hdXRoMi9hdXMycjd5M2dkYUZNS0JvbDI5NyIsImF1ZCI6IkFCMkQiLCJpYXQiOjE1ODAzMDgxOTAsImV4cCI6MTU4MDMxMTc5MCwiY2lkIjoiMG9hMnQwbHNyZFp3NXVXUngyOTciLCJzY3AiOlsiY2xpZW50Q3JlZHMiXSwic3ViIjoiMG9hMnQwbHNyZFp3NXVXUngyOTcifQ.LVtCqJdYLKdUGJq5cU-31811wdnOnqzHVdlWYryjlw_adhdxG7IXVOXvQoimXFZZRlA4WRi3mQ05AJq_crojwqTud9jq59IsoPfWU4OfUSH-4rGZVaZzuXPRfqLQTuZ8y3FS_bgfqLDN_gzSR7Uv3gXnnErMUCIdcHTttTrSsrKUqmOzASmoXvKIghnQMxV0Aq550MB3zZSDTwMNRPam4RpuQ-3nyLGsv76ioQey4uWohXJBsokegvXfUnjOQHb8zZFDx20bGLWcGN7VPfXodHpQxkXSDZuorY-0MSe-gTLsMtMUB_ElG2XiOfXbm1Q9gCDTxpxjTY66PProFISkVw
```

Click the Authorize button, then close the dialog

You will see that the endpoints now have locks next to them. All requests will put the Bearer token in their header.

![Secure Endpoints](/assets/img/sandbox/lock.png)

## Request Data

Let's begin a request to download data. Click on the "GET /api/fhir/Patient/$export" endpoint. You will be given a 
description of the parameters and responses for the endpoint. In our situation, we want to receive a 202 HTTP response 
code indicating that the export request has started. Let's try it out. Click the button:

![Try it out](/assets/img/sandbox/tryitoutbutton.png)

The resulting page would look like this:

![Start Job](/assets/img/sandbox/startjob.png)

Enter the following values (they should by default be filled in):

| Field | Value |
|:------|:------|
| Accept | application/fhir+json |
| Prefer | respond-async |
| _outputFormat | application/fhir+ndjson |
| _type | ExplanationOfBenefit |


Press the Execute bar

The server will respond with a 202 indicating the job has started. The "content-location" header will contain the URL 
to determine the status of the job:

```
https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/9ad207d2-11ff-45b8-9c6c-2d59b86ea45c/$status
```

Let's try that next. Click on the "GET /api/v1/fhir/Job/{jobUuid}/$status endpoint, click "Try it Out" like you did 
above. It will request a job identifier. In this demo, it is 9ad207d2-11ff-45b8-9c6c-2d59b86ea45c. Enter that value 
and press Execute:

<img src="/assets/img/sandbox/status.png" width="800px;" alt="Job Status"/>


There are several possible responses. If the job is still in progress, it will return a 202 with "retry-after: x" in the response header. This gives you an indication when to retry the status request. The header "x-progress" will give you an indication of how far along it is in completing the job. If the job is complete, you will get a 200 HTTP response code as well as the location of the files to download.

## Download Data

In our situation, the location of the file is:
 
```
https://sandbox.ab2d.cms.gov/api/v1/fhir/Job/9ad207d2-11ff-45b8-9c6c-2d59b86ea45c/file/S0001_0001.ndjson
```

There could be one or more files listed. S0001 indicates the contract number, 0001 indicates the file for that
contract number. If the contract is big enough that breaking it into parts is warranted, you might see S0001_00002.ndjson, for example. 
Use the endpoint:

```
GET /api/v1/fhir/Job/{jobUuid}/file/{filename}
```

<img src="/assets/img/sandbox/downloadfile.png" alt="Download File" width="800px"/>

This should result in new line delimited JSON of Explanation of Benefit objects. The response data will look be hard 
to read, but you can download the data by clicking Download on the bottom right side of the response body text:

<img src="/assets/img/sandbox/downloadresponse.png" alt="Download Response" width="800px"/>

In this example, a formatted JSON of the first line could look like this:

<textarea cols="95" rows="50" readonly>
{ 
   "resourceType":"ExplanationOfBenefit",
   "id":"carrier-10384232074",
   "identifier":[ 
      { 
         "system":"https://bluebutton.cms.gov/resources/variables/clm_id",
         "value":"10384232074"
      },
      { 
         "system":"https://bluebutton.cms.gov/resources/identifier/claim-group",
         "value":"38491422049"
      }
   ],
   "type":{ 
      "coding":[ 
         { 
            "system":"https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd",
            "code":"71",
            "display":"Local carrier non-durable medical equipment, prosthetics, orthotics, and supplies (DMEPOS) claim"
         },
         { 
            "system":"https://bluebutton.cms.gov/resources/codesystem/eob-type",
            "code":"CARRIER"
         },
         { 
            "system":"http://hl7.org/fhir/ex-claimtype",
            "code":"professional",
            "display":"Professional"
         },
         { 
            "system":"https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
            "code":"O",
            "display":"Part B physician/supplier claim record (processed by local carriers; can include DMEPOS services)"
         }
      ]
   },
   "patient":{ 
      "reference":"Patient/19990000002901"
   },
   "diagnosis":[ 
      { 
         "sequence":1,
         "diagnosisCodeableConcept":{ 
            "coding":[ 
               { 
                  "system":"http://hl7.org/fhir/sid/icd-9-cm",
                  "code":"5672",
                  "display":"SUPPURAT PERITONITIS NEC"
               }
            ]
         },
         "type":[ 
            { 
               "coding":[ 
                  { 
                     "system":"https://bluebutton.cms.gov/resources/codesystem/diagnosis-type",
                     "code":"principal",
                     "display":"The single medical diagnosis that is most relevant to the patient's chief complaint or need for treatment."
                  }
               ]
            }
         ]
      },
      { 
         "sequence":2,
         "diagnosisCodeableConcept":{ 
            "coding":[ 
               { 
                  "system":"http://hl7.org/fhir/sid/icd-9-cm",
                  "code":"5570",
                  "display":"AC VASC INSUFF INTESTINE"
               }
            ]
         }
      },
      { 
         "sequence":3,
         "diagnosisCodeableConcept":{ 
            "coding":[ 
               { 
                  "system":"http://hl7.org/fhir/sid/icd-9-cm",
                  "code":"5699",
                  "display":"INTESTINAL DISORDER NOS"
               }
            ]
         }
      },
      { 
         "sequence":4,
         "diagnosisCodeableConcept":{ 
            "coding":[ 
               { 
                  "system":"http://hl7.org/fhir/sid/icd-9-cm",
                  "code":"5680",
                  "display":"PERITONEAL ADHESIONS"
               }
            ]
         }
      },
      { 
         "sequence":5,
         "diagnosisCodeableConcept":{ 
            "coding":[ 
               { 
                  "system":"http://hl7.org/fhir/sid/icd-9-cm",
                  "code":"9999999"
               }
            ]
         }
      }
   ],
   "precedence":0,
   "item":[ 
      { 
         "sequence":1,
         "service":{ 
            "coding":[ 
               { 
                  "system":"https://bluebutton.cms.gov/resources/codesystem/hcpcs",
                  "version":"0",
                  "code":"99283"
               }
            ]
         },
         "servicedPeriod":{ 
            "start":"2000-08-01",
            "end":"2000-08-01"
         },
         "locationCodeableConcept":{ 
            "extension":[ 
               { 
                  "url":"https://bluebutton.cms.gov/resources/variables/prvdr_state_cd",
                  "valueCoding":{ 
                     "system":"https://bluebutton.cms.gov/resources/variables/prvdr_state_cd",
                     "code":"99",
                     "display":"With 000 county code is American Samoa; otherwise unknown"
                  }
               },
               { 
                  "url":"https://bluebutton.cms.gov/resources/variables/prvdr_zip",
                  "valueCoding":{ 
                     "system":"https://bluebutton.cms.gov/resources/variables/prvdr_zip",
                     "code":"999999999"
                  }
               },
               { 
                  "url":"https://bluebutton.cms.gov/resources/variables/carr_line_prcng_lclty_cd",
                  "valueCoding":{ 
                     "system":"https://bluebutton.cms.gov/resources/variables/carr_line_prcng_lclty_cd",
                     "code":"99"
                  }
               }
            ],
            "coding":[ 
               { 
                  "system":"https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd",
                  "code":"99",
                  "display":"Other Place of Service. Other place of service not identified above."
               }
            ]
         },
         "quantity":{ 
            "value":1
         }
      }
   ]
}
</textarea >

The format is based on the FHIR standard for an Explanation of Benefit object. This represents the allowed subset of
data allowed for this project.

At any point before completion, the process can be cancelled by calling 

```
DELETE /api/v1/fhir/Job/{jobUuid}/$status endpoint.
```
