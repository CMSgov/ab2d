---
layout: subpage_layout
title:  "Understanding AB2D Data"
date:   2019-11-02 09:21:12 -0500 
description: Understanding AB2D Data
landing-page: live
active-nav: understanding-the-data-nav
---

<section class="bg-white page-section py-5" role="main">
    <svg class="shape-divider" preserveAspectRatio="xMidYMin slice" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
         viewBox="0 0 1034.2 43.8" style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve" alt="divider">
	<path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z"/>
    </svg>
    <div class="container">
        <div class="row" style="margin-bottom: 70px;">
            <div class="col-lg-6">
                <div class="step-header">Overview</div>
                <p>
                The AB2D API is a RESTful based web service providing Medicare Parts A and B claims data using the industry-
                standard <a href="https://www.hl7.org/fhir/overview.html" target="_blank">HL7 Fast Healthcare Interoperability Resources (FHIR) </a> resources, specifically the 
                <a href="https://hl7.org/fhir/uv/bulkdata/export/index.html" target="_blank">Bulk FHIR specification </a>. The AB2D API only provides data records for active enrollees and excludes
                all claims with substance abuse codes (as required by the Confidentiality of Alcohol and Drug Abuse Patient
                Records Regulations, 42 CFR Part 2).
                </p>
            </div>
            <div class="col-lg-6">
                <img class="mt-3" src="assets/img/data-analysis.svg" alt="data-analysis" />
            </div>
        </div>
    </div>
</section>         

<section class="bg-light-blue page-section pt-20 pb-10" role="region" aria-label="Developer Resources">        
    <svg class="shape-divider flip" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
             viewBox="0 0 1034.2 43.8" style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve" alt="divider">
        <path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z"/>
    </svg>
    <div class="container">    
        <div class="understanding-the-data-section">
            <div class="row">
                <div class="col-lg-2 text-center">
                    <img class="mb-3" src="assets/img/paper.svg" alt="paper" />
                </div>
                <div class="col-lg-10">
                    <div class="header-title">Sample AB2D Files</div>
                    The examples below were generated from synthetic claims data but are similar to the files that will be
                    retrieved through the AB2D API for Part D Sponsors.
                    <br /><br />
                    <ul class="gray-bullets">
                    <li>
                    <a href="assets/downloads/sample-data.ndjson">Parts A and Parts B Sample Export</a>
                    </li>
                    </ul>
                </div>
            </div>
        </div>
        
        <div class="understanding-the-data-section">
            <div class="row">
                <div class="col-lg-2 text-center">
                    <img class="mb-3" src="assets/img/book.svg" alt="book" />
                </div>
                <div class="col-lg-10">
                    <div class="header-title">Data Dictionary</div>
                    AB2D API users can leverage the AB2D Data Dictionary
                     for more information on the data elements.
                    <br /><br />
                    <ul class="gray-bullets">
                        <li>
                            <a href="data_dictionary.html">
                                View Data Dictionary
                            </a>
                        </li>    
                    </ul>
                </div>
            </div>
        </div>
                
        <div class="understanding-the-data-section">
            <div class="row">
                <div class="col-lg-2 text-center">
                    <img class="mb-3" src="assets/img/creativity.svg" alt="creativity" />
                </div>
                <div class="col-lg-10">
                    <div class="header-title">Additional Resources</div>
                    Here are some helpful resources you can reference while using this site:
                    <br /><br />
                    <ul class="gray-bullets">
                        <li>
                            <a href="http://json.org/" target="_blank">Intro to JSON Format </a>
                        </li>
                        <li>
                            <a href="http://ndjson.org/" target="_blank">Newline Delimited JSON (ndjson) </a>
                        </li>
                        <li>
                            <a href="https://jsonlint.com/" target="_blank">JSON format viewer/validator </a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>    
    </div>
</section>    