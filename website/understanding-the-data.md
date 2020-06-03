---
layout: subpage_layout
title:  "Understanding the Data"
date:   2019-11-02 09:21:12 -0500 
description: Understanding the Data
landing-page: live
active-nav: understanding-the-data-nav
---

<section class="bg-white page-section py-5">
    <svg class="shape-divider" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
         viewBox="0 0 1034.2 43.8" style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve">
	<path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z"/>
    </svg>
    <div class="container">
        <div class="section-header"><strong>Overview</strong></div>
        The AB2D API is a RESTful based web service providing Medicare Parts A and B claims data using the industry-
        standard HL7 Fast Healthcare Interoperability Resources (FHIR) resources, specifically the Bulk FHIR specification.
        The AB2D API only provides data records for active enrollees that have not opted out of data sharing and excludes
        all claims with substance abuse codes (as required by the Confidentiality of Alcohol and Drug Abuse Patient
        Records Regulations, 42 CFR Part 2).
        Sample AB2D Files
        The examples below were generated from synthetic claims data but are similar to the files that will be
        retrieved through the AB2D API for Part D Sponsors.
        <ol>
        <li>
        Parts A and Parts B Sample Export for a Given Contract Number (Will link to Sample Data File page-
        Use attachment in email to review page content. *Only minor changes were made from the content in the
        current website)
        </li>
        </ol>
        <br />
        <strong>Data Dictionary</strong>
        <br />
        AB2D API users can leverage the <a href="https://ab2d.cms.gov/data/data_dictionary.html" target="_blank">AB2D Data Dictionary</a> for more information on the data
         elements.
        <br /><br />
        <strong>Additional Resources</strong>
        <ul>
            <li><a href="http://json.org" target="_blank">Intro to JSON Format</a> and <a href="http://ndjson.org/" target="_blank">
             NDJSON</a></li>
            <li><a href="https://jsonlint.com/" target="_blank">JSON format viewer/validator (raw text/JSON format converter)</a></li>
        </ul>
        
    </div>
</section>    