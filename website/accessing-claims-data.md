---
layout: subpage_layout
title:  "Accessing Claims Data"
date:   2019-11-02 09:21:12 -0500 
description: Accessing Claims Data
landing-page: live
active-nav: accessing-claims-data
---

<script type="text/javascript">
    const stepMappings = {
        'step-1-icon': 'step-1-content',
        'step-2-icon': 'step-2-content',
        'step-3-icon': 'step-3-content',
        'step-4-icon': 'step-4-content'
    };
    $(document).ready(function() {
        $('.step-section').on('click', function(event) {
            $('.step-content').each(function() {
                $(this).fadeOut();  
            });
            const className = $(this).attr('id');
            const idToShow = stepMappings[className];
            $('#' + idToShow).fadeIn();
        });
    });
</script>

<section class="bg-white page-section py-5">
    <svg class="shape-divider" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
         viewBox="0 0 1034.2 43.8" style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve">
	<path fill="#ffffff" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z"/>
  </svg>
    <div class="container">
        <div class="row">
            <div class="col-lg-12 text-center">
                <h5 class="section-heading">How To Access</h5>
                <h2 class="section-heading">Medicare Parts A & B Claims Data</h2>
                <div class="divider-small-border center my-4"></div>
            </div>
        </div>
        <div class="row">
            <div class="col text-center">
                <span id="step-1-icon" class="step-section">Step 1</span>
                <span id="step-2-icon" class="step-section">Step 2</span>
                <span id="step-3-icon" class="step-section">Step 3</span>
                <span id="step-4-icon" class="step-section">Step 4</span>
            </div>
        </div>
        <div class="row" class="claims-data-content">
            <div id="step-1-content" class="step-content">
                <strong style="text-align: center;">Overview</strong>
                <p>
                    In order to access Medicare Parts A and B claims data, a Part D Plan (PDP) sponsor must first complete the attestation process for each of
                    their participating Part D contracts. During this process, a Part D Plan (PDP) sponsor formally reviews and agrees to comply with the Claims
                    Data Usage Protocols. These protocols regulate how their organization may or may not use the Medicare claims data provided by the AB2D
                    API, including limitations associated with the reuse and disclosure of the data.
                    What Type of Organizations Can Attest?
                    Stand-alone Medicare Part D Plan (PDP) sponsors (PACE and MAPD are not eligible).
                </p>
                
                <p>
                    Who Can Attest?
                    <ul>
                        <li>Attestation must be performed by a Medicare Part D Plan (PDP) Sponsor CEO, CFO, or COO.</li>
                        <li>An Attestor must hold an active CEO, CFO, or COO role within their organization.</li>
                        <li>Part D Plan (PDP) sponsors can have multiple executives attest to each of their participating contracts. This is considered best
                        practice and is strongly encouraged.</li>
                    </ul>
                </p>
                
                <p>
                    How Does Attestation Affect Claims Data?
                    <ul>
                        <li>Attested Part D Plan (PDP) sponsors are able to retrieve claims data for active plan enrollees from the date of attestation onwards.
                        Claims data prior to the attestation date will not be provided.</li>
                        <li>Participating Part D Plan (PDP) sponsors must have an active Attestor at all times and will not receive data during periods where the
                        (PDP) sponsor does not have an active Attestor. Data access will be restored once another active CEO, CFO, or COO attests. We
                        highly recommend that Part D Plan (PDP) sponsors have multiple executives attest to contracts to reduce the risk of lapses in access
                        to data based on attestation status.</li>
                    </ul>
                </p>
                
                <p>
                    How to initial Attest
                    <ol>
                        <li>Log into HPMS</li>
                        <li>Click on “Claims Data Attestation” (under Contract Management)</li>
                        <li>Select the checkbox(es) next to one, multiple, or all contracts within the 'Contracts Without Attestation' window</li>
                        <li>Click on the "Attest" button</li>
                        <li>Review the Claims Data Usage Protocols</li>
                        <li>Select the checkbox next to: "I hereby certify that I understand the attestation above"</li>
                        <li>>Click "Confirm"</li>
                    </ol>
                </p>
                
                <p>
                    How to add additional Attestors
                    <ol>
                        <li>Log into HPMS</li>
                        <li>Click on “Claims Data Attestation” (under Contract Management)</li>
                        <li>Select the checkbox(es) next to one, multiple, or all contracts within the 'Attested Contract' window</li>
                        <li>>Click on the “Re-Attest” button</li>
                        <li>Review the Claims Data Usage Protocols</li>
                        <li>Select the checkbox next to: "I hereby certify that I understand the attestation above"</li>
                        <li>Click "Confirm"</li>
                    </ol>
                </p>
            </div>
            
            <div id="step-2-content" class="step-content" style="visibility: hidden;">
                <strong style="text-align: center;">Overview</strong>
                <p>
                After attesting, Part D Plan (PDP) Sponsors will need to assign an “AB2D Data Operations Specialist” to act as their organization’s primary
                technical point of contact.
                The "AB2D Data Operations Specialist":
                <ul>
                <li>Is a technical employee at the Part D Plan (PDP) sponsor that has the authority to access and view the data provided by the API.</li>
                <li>Will be technically savvy enough to connect to the AB2D API and retrieve claims data from our Sandbox and Production
                environments.</li>
                <li>Will need to provide static IP address(es) and or CIDR ranges for the network/system that is going to be accessing the AB2D API.</li>
                </ul>
                </p>
            </div>
            
            <div id="step-3-content" class="step-content" style="visibility: hidden;">
                        
            </div>
            
            <div id="step-4-content" class="step-content" style="visibility: hidden;">
                                    
            </div>
        </div>
    </div>
</section>