---
layout: home
title:  "Claims Data to Part D Sponsors API"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - What is the Sandbox?
  - Retrieving Synthetic Data
  - Production Data
ctas:

---
<style>
.ds-c-table td,
.ds-c-table th {
    padding: 0.3rem;
    font-size: small;
}
</style>

## What is the Sandbox?

The Sandbox is a test environment that enables you to interact with the AB2D API and retrieve synthetic Medicare parts 
A and B claims data.

The API follows the [FHIR Bulk Data Export](https://hl7.org/fhir/uv/bulkdata/export/index.html) pattern to perform data 
export. This requires an asynchronous call to create a job and provides status updates on that job until it 
is complete and the generated files can be downloaded.

## Retrieving Synthetic Data

We are providing three different examples on how users can interact with the AB2D API and retrieve synthetic claims data.

1. [Postman and Swagger User Guide](tutorial-postman.html)
2. [Curl User Guide](tutorial-curl.html)
3. [Advanced User Guide](advanced_user_guide.html)

## Production Data

Interested in Production Data? Has your organization attested and retrieved synthetic claims data from our Sandbox 
environment? If so, please email the job ID generated to: [PDP-Data@cms.hhs.gov](PDP-Data@cms.hhs.gov). The AB2D team 
will verify and follow-up with production credentials. 
