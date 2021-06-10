---
layout: subpage_layout
title: "Support"
date: 2019-11-02 09:21:12 -0500
description: Support
landing-page: live
active-nav: support-nav
---

<script type="text/javascript">
  $(document).ready(function () {
    $('.card-header').on('click', function (event) {
      $(this).parent().find('.card-expand').toggleClass('icon-flipped');
    });
  });
</script>


<section class="bg-white page-section py-5 pb-10" role="main">
  <svg preserveAspectRatio="xMidYMin slice" class="shape-divider" version="1.1" xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 1034.2 43.8"
    style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve" alt="divider">
    <path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z" />
  </svg>
  <div class="container">
    <div class="row">
      <div class="col-lg-5">
        <div class="header-title">We Are Listening</div>
        If you have a question, suggestion, or feedback please post it in the AB2D Google Group!

        <div class="google-group-wrapper mb-3 mt-4">
          <a class="join-our-google-group" href="https://groups.google.com/u/1/g/cms-ab2d-api" target="_blank">
            JOIN OUR GOOGLE GROUP
            <i class="material-icons pl-1 external-icon">open_in_new</i>
          </a>
        </div>

      </div>
      <div class="col-lg-7">
        <img src="assets/img/experts.svg" alt="experts" />
      </div>
    </div>

    <div class="header-title mb-3 mt-5">
      Frequently Asked Questions
    </div>

    <div id="accordion-support" class="accordion-white-bg">
      <div class="card">
        <div class="card-header" id="headingOne" data-toggle="collapse" data-target="#collapseOne" aria-expanded="false"
          aria-controls="collapseOne">
          <div class="mb-0 card-header-h5">
            Why is CMS making claims data available to PDP sponsors?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>

        <div id="collapseOne" class="collapse" aria-labelledby="headingOne" data-parent="#accordion">
          <div class="card-body">
            In February 2018, the <a href="https://www.congress.gov/bill/115th-congress/house-bill/1892/text"
              target="_blank">Bipartisan Budget Act of 2018 (BBA) </a> was signed into law and included a provision
            requiring the development of a process to share Medicare fee-for-services claims data with PDP sponsors.
            Section 50354 of the BBA specifically provides that the Secretary of Health and Human Services shall
            establish a process under which PDP sponsors may request, beginning in plan year 2020, that the
            Secretary provide on a periodic basis and in an electronic format standardized extracts of Medicare claims
            data about its plan enrollees. Such extracts would contain a subset of Medicare Parts A and B claims data
            as determined by the Secretary and would be as current as practicable.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingTwo" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="false"
          aria-controls="collapseTwo">
          <div class="mb-0 card-header-h5">
            What is the Final Rule?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseTwo" class="collapse" aria-labelledby="headingTwo" data-parent="#accordion">
          <div class="card-body">
            In response to the <a href="https://www.congress.gov/bill/115th-congress/house-bill/1892/text"
              target="_blank">Bipartisan Budget Act of 2018 (BBA) </a>, CMS published a
            <a href="https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare#page-15745"
              target="_blank">
              Final Rule </a> to implement section
            50354 of the BBA, which outlines the manner in which CMS proposes to implement this requirement.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingThree" data-toggle="collapse" data-target="#collapseThree"
          aria-expanded="false" aria-controls="collapseThree">
          <div class="mb-0 card-header-h5">
            Who is eligible to request Medicare Claims data under this process?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseThree" class="collapse" aria-labelledby="headingThree" data-parent="#accordion">
          <div class="card-body">
            Stand-alone Medicare Part D Plan (PDP) sponsors.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingFour" data-toggle="collapse" data-target="#collapseFour"
          aria-expanded="false" aria-controls="collapseFour">
          <div class="mb-0 card-header-h5">
            How do standalone Medicare Part D Plan (PDP) sponsors access the Medicare Parts A and B claims data as
            mandated by the BBA and Final Rule?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseFour" class="collapse" aria-labelledby="headingTwo" data-parent="#accordion">
          <div class="card-body">
            PDP sponsors will be able to access the Medicare Parts A and B claims data by leveraging the AB2D
            Application Programming Interface (API).
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingFive" data-toggle="collapse" data-target="#collapseFive"
          aria-expanded="false" aria-controls="collapseFive">
          <div class="mb-0 card-header-h5">
            I'm a Standalone Medicare Part D Plan (PDP) sponsor. How does my organization receive Medicare Parts
            A and B claims information using the AB2D API?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseFive" class="collapse" aria-labelledby="headingFive" data-parent="#accordion">
          <div class="card-body">
            A standalone Medicare Part D Plan (PDP) sponsor must first complete the attestation process for each of
            their participating Part D contracts. After attesting, the AB2D team will work directly with the “AB2D Data
            Operations Specialist” and the Attestor to ensure that the (PDP) is able to retrieve Medicare Parts A and B
            claims data. Learn more about <a href="accessing-claims-data.html">accessing AB2D claims data</a>.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingSix" data-toggle="collapse" data-target="#collapseSix" aria-expanded="false"
          aria-controls="collapseSix">
          <div class="mb-0 card-header-h5">
            What is Claims Data Attestation?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseSix" class="collapse" aria-labelledby="headingSix">
          <div class="card-body">
            Attestation is a process to ensure PDP sponsors are aware of how the Medicare claims data provided by
            AB2D may and may not be used, including limitations associated with reuse and redisclosure of data.
            Attestation performed through the <a
              href="https://www.cms.gov/Research-Statistics-Data-and-Systems/Computer-Data-and-Systems/HPMS/Overview"
              target="_blank">Health Plan Management System (HPMS) </a> Claims Data Attestation
            module affirms adherence to these permitted uses and limitations of this claims data as listed in
            § 423.153 of the <a
              href="https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare#page-15745"
              target="_blank">Final Rule </a>. To attest, visit
            <a href="https://hpms.cms.gov/app/ng/home/" target="_blank">HPMS </a>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingSeven" data-toggle="collapse" data-target="#collapseSeven"
          aria-expanded="false" aria-controls="collapseSeven">
          <div class="mb-0 card-header-h5">
            Who within my organization can Attest?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseSeven" class="collapse" aria-labelledby="headingSeven">
          <div class="card-body">
            Attestation must be performed by a Medicare Part D Plan (PDP) Sponsor CEO, CFO, or COO. The Attestor
            must hold an active CEO, CFO, or COO role within their organization. Part D Plan (PDP) sponsors are
            allowed to have multiple executives attest to each participating contract.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingEight" data-toggle="collapse" data-target="#collapseEight"
          aria-expanded="false" aria-controls="collapseEight">
          <div class="mb-0 card-header-h5">
            What happens if an Attestor leaves my company?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseEight" class="collapse" aria-labelledby="headingEight">
          <div class="card-body">
            Participating Part D Plan (PDP) sponsors must have an active Attestor at all times and will not receive data
            during periods where the (PDP) sponsor does not have an active Attestor.
            If your organization has a single Attestor and they leave without a replacement, then your organization
            will lose access to new data until another active CEO, CFO, or COO attests. Re-attestation will restore
            access to new claims data and provide historical claims data that was not accessible during the lapse in
            active attestation.
            Having multiple Attestors reduces the risk of data gaps due to workplace attrition and is strongly
            encouraged.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingNine" data-toggle="collapse" data-target="#collapseNine"
          aria-expanded="false" aria-controls="collapseNine">
          <div class="mb-0 card-header-h5">
            What is the permitted use of the data?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseNine" class="collapse" aria-labelledby="headingNine">
          <div class="card-body">
            Section § 423.153(c) of the <a
              href="https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare#page-15745"
              target="_blank">Final Rule </a> specifies that PDP sponsors receiving Medicare claims data for their
            corresponding PDP plan enrollees may use the data for:
            (i) Optimizing therapeutic outcomes through improved medication use;
            (ii) improving care coordination so as to prevent adverse healthcare outcomes;
            (iii) for any other purpose described in the first or second paragraph of “health care operations” under 45
            CFR 164.501, or that qualify as “fraud and abuse detection or compliance activities” under 45 CFR
            164.506(c)(4).
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingTen" data-toggle="collapse" data-target="#collapseTen" aria-expanded="false"
          aria-controls="collapseTen">
          <div class="mb-0 card-header-h5">
            What use of the data is not permitted?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseTen" class="collapse" aria-labelledby="headingTen">
          <div class="card-body">
            Section § 423.153(c) of the <a
              href="https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare#page-15745"
              target="_blank">Final Rule </a>
            specifies that PDP sponsors receiving Medicare Parts A and B claims
            data for their PDP plan enrollees may not use the data for the following purposes:
            (i) To inform coverage determinations under Part D;
            (ii) To conduct retroactive reviews of medically accepted indications determinations;
            (iii) To facilitate enrollment changes to a different prescription drug plan or an MA-PD plan offered by the
            same parent organization; or
            (iv) To inform marketing of benefits.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingEleven" data-toggle="collapse" data-target="#collapseEleven"
          aria-expanded="false" aria-controls="collapseEleven">
          <div class="mb-0 card-header-h5">
            When can PDP sponsors begin requesting data?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseEleven" class="collapse" aria-labelledby="headingEleven">
          <div class="card-body">
            The Health Plan Management System (HPMS) Claims Data Attestation module can be used by PDP
            Sponsors to submit a request for Medicare claims data (by contract) beginning January 1, 2020.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingTwelve" data-toggle="collapse" data-target="#collapseTwelve"
          aria-expanded="false" aria-controls="collapseTwelve">
          <div class="mb-0 card-header-h5">
            What is the format of the data extract?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseTwelve" class="collapse" aria-labelledby="headingTwelve">
          <div class="card-body">
            The AB2D API leverages the <a href="http://build.fhir.org/ig/HL7/VhDir/bulk-data.html" target="_blank">Bulk
              FHIR Specification </a>
            which uses the file format: NDJSON, <a href="http://ndjson.org/" target="_blank">New Line Delimited JSON
            </a>. An NDJSON file provides a single record on each line, which makes it
            easy for various tools to look at and process one record at a time before moving on to the next one.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingThirteen" data-toggle="collapse" data-target="#collapseThirteen"
          aria-expanded="false" aria-controls="collapseThirteen">
          <div class="mb-0 card-header-h5">
            What are the data elements that will be accessible through the API?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseThirteen" class="collapse" aria-labelledby="headingThirteen">
          <div class="card-body">
            Medicare Parts A and B claims data elements (fields) in the standardized extract as specified in the rule:
            <ul>
              <li>An enrollee identifier</li>
              <li>Diagnosis and procedure codes (for example, ICD-10 diagnosis and Healthcare Common</li>
            </ul>
            Procedure Coding System (HCPCS) codes)
            <ul>
              <li>Dates of service</li>
              <li>Place of service</li>
              <li>Provider numbers (for example, NPI)</li>
              <li>Claim processing and linking identifiers/codes (for example, claim ID, and claim type code)</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingFifteen" data-toggle="collapse" data-target="#collapseFifteen"
          aria-expanded="false" aria-controls="collapseFifteen">
          <div class="mb-0 card-header-h5">
            Can a PDP sponsor request historical data?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseFifteen" class="collapse" aria-labelledby="headingFifteen">
          <div class="card-body">
            Section 1860D-4(c)(6)(D) of the Act provides that the Secretary shall make standardized extracts available
            to PDP sponsors with data that is the most current as practicable. While we understand that historical
            data may assist PDP sponsors, we must adhere to the statutory language. As this program matures, PDP
            sponsors will amass historical data.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingSixteen" data-toggle="collapse" data-target="#collapseSixteen"
          aria-expanded="false" aria-controls="collapseSixteen">
          <div class="mb-0 card-header-h5">
            How can we get more data elements in addition to what’s listed in the Final Rule?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseSixteen" class="collapse" aria-labelledby="headingSixteen">
          <div class="card-body">
            CMS will continue to evaluate the data elements provided to PDP sponsors to determine if data elements
            should be added or removed based on the information needed to carry out the permitted uses of the
            data. Any proposed changes would be established through rulemaking.
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header" id="headingSeventeen" data-toggle="collapse" data-target="#collapseSeventeen"
          aria-expanded="false" aria-controls="collapseSeventeen">
          <div class="mb-0 card-header-h5">
            What are the data sources and how often is the data updated?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseSeventeen" class="collapse" aria-labelledby="headingSeventeen">
          <div class="card-body">
            The AB2D API will leverage the Beneficiary FHIR Data (BFD) server, which receives data from the Chronic
            Condition Warehouse (CCW). The majority of the BFD data is refreshed weekly with a few data elements
            being loaded monthly.
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header" id="headingEighteen" data-toggle="collapse" data-target="#collapseEighteen"
          aria-expanded="false" aria-controls="collapseSEighteen">
          <div class="mb-0 card-header-h5">
            What can I find in your production documentation, and where can I find it?
          </div>
          <i class="material-icons card-expand">expand_more</i>
        </div>
        <div id="collapseEighteen" class="collapse" aria-labelledby="headingEighteen">
          <div class="card-body">
            <p>
              The production documentation can be <a href="https://github.com/CMSgov/ab2d-pdp-documentation"
                rel="noopener noreferrer" target="_blank">found here</a>. This documentation can be helpful in answering
              the following
              questions:
            </p>

            <p>
              <strong>Accessing the production environment:</strong>
            <ul>
              <li>How do I prepare to start pulling in data from production?</li>
              <li>How do I verify that my setup can successfully connect to the API?</li>
            </ul>
            </p>
            <p>
              <strong>Support:</strong>
            <ul>
              <li>Where can I find information on how to process EOBs?</li>
              <li>Where can I find information on the production errors I’m seeing?</li>
            </ul>
            </p>
            <p>
              <strong>Understanding the Data:</strong>
            <ul>
              <li>Where can I find information on the non-standard fields produced by AB2D?</li>
              <li>Where can I find information on handling updated or canceled claims?</li>
              <li>Where can I find information on uniquely identifying a claim?</li>
              <li>Where can I find information on how patients are identified in the claims data?</li>
            </ul>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>