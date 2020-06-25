---
layout: subpage_layout
title:  "Overview"
date:   2019-11-02 09:21:12 -0500 
description: Overview
landing-page: live
active-nav: overview-nav
---

<section class="bg-white page-section py-5">
    <svg preserveAspectRatio="xMidYMin slice" class="shape-divider" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
         viewBox="0 0 1034.2 43.8" style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve">
	<path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z"/>
  </svg>
    <div class="container">
        <div class="row">
            <div class="col-lg-12 text-center">
                <h2 class="section-heading">The Medicare program provides health insurance coverage to more than <strong>60 million people</strong>.</h2>
                <div class="divider-small-border center my-4"></div>
            </div>
        </div>
        <div class="row align-items-center">
            <div class="col-lg-6"> <img src="assets/img/pharmacist.svg" alt="Pharmaceuticals"/> </div>
            <div class="col-lg-6 px-5">
                <p>The Centers for Medicare and Medicaid Services (CMS) contracts with private insurance companies, known as Medicare Part D Plan (PDP) sponsors, to provide prescription drug coverage for enrollees.</p>
                <p>
                    The <a href="https://www.congress.gov/bill/115th-congress/house-bill/1892/text" target="_blank">Bipartisan Budget Act of 2018 (BBA)</a> requires the development of a process to share Medicare claims data with
                    PDP sponsors. <a target="_blank" href="https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare">The Final Rule</a> provides additional detail on the purposes and limitations on the use of data, the data request process, and data extract content.
                </p>
                <p>
                    In response to the BBA, CMS has developed an Application Programming Interface (API) known as the AB2D API. The AB2D API securely provides 
                    stand-alone PDP sponsors with Medicare Parts A and B claims data for their active enrollees. PDP sponsors will be able to utilize this claims data 
                    to promote the appropriate use of medications and to improve health outcomes for their beneficiaries. The bulk claims data provided by the AB2D API 
                    is formatted in <a href="https://hl7.org/fhir/uv/bulkdata/export/index.html" target="_blank">Fast Healthcare Interoperability Resources (FHIR)</a> a standard for exchanging healthcare information electronically.
                </p>
            </div>
        </div>
    </div>
</section>
<section class="bg-light-blue page-section pt-20 pb-10">

  <svg preserveAspectRatio="xMidYMin slice" alt="divider" class="shape-divider flip" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
         viewBox="0 0 1034.2 43.8" style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve">
	<path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z"/>
  </svg>
    <div class="container">

        <div class="row">
            <div class="col-lg-12 text-center">
                <h3 class="section-heading text-center">How is the AB2D API different from other CMS APIs?</h3>
                <div class="divider-small-border center mt-4 mb-5"></div>

            </div>
        </div>

        <div class="row align-items-center">
            <div class="col-lg-6 px-5">
                <ol>
                    <li>The AB2D API provides FHIR-formatted bulk claims data to stand-alone PDP sponsors for their enrollees. Beneficiaries can opt out of data sharing <a href="https://www.medicare.gov/privacy-policy" target="_blank">here</a>.</li>
                    <li><a href="https://bluebutton.cms.gov/" target="_blank">Blue Button 2.0</a> provides FHIR-formatted data for one individual Medicare beneficiary at a time, to registered applications with beneficiary authorization.</li>
                    <li><a href="https://bcda.cms.gov/" target="_blank">BCDA</a> provides FHIR-formatted bulk data files to an ACO for all of the beneficiaries eligible to a given Shared Savings Program ACO. BCDA does not require individual beneficiary authorization but does allow a process for patients to opt out of data sharing.</li>
                    <li><a href="https://dpc.cms.gov/" target="_blank">Data at the Point of Care</a> pilot provides FHIR-formatted bulk data files to fee-for-service providers for their active patients as needed for treatment purposes under HIPAA. Data at the Point of Care does not require individual beneficiary authorization but does allow a process for patients to opt out of data sharing.</li>
                </ol>
            </div>
            <div class="col-lg-6"> <img src="assets/img/programmer.svg" alt="Accessing Claims Data"/> </div>

        </div>
    </div>
</section>