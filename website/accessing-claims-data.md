---
layout: subpage_layout
title:  "Accessing Claims Data"
date:   2019-11-02 09:21:12 -0500 
description: Accessing Claims Data
landing-page: live
active-nav: accessing-claims-data-nav
---

<script type="text/javascript">
    const stepMappings = {
        'step-1-icon': 'step-1-content',
        'step-2-icon': 'step-2-content',
        'step-3-icon': 'step-3-content',
        'step-4-icon': 'step-4-content'
    };
    $(document).ready(function() {
        $('.step-accessing-claims').on('click', function(event) {
            const stepId = $(this).attr('id');
            const idToShow = stepMappings[stepId];
            
            $('.step-accessing-claims').each(function() {
                $(this).removeClass('step-accessing-claims-active').addClass('step-accessing-claims-nonactive');
                $(this).find('.step-claims-dash').hide();
            });
            
            $(this).removeClass('step-accessing-claims-nonactive').addClass('step-accessing-claims-active');
            $(this).find('.step-claims-dash').show();
            
            $('html, body').animate({
                scrollTop: $('#' + idToShow).offset().top
            }, 2000);
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
            <div class="col" style="max-width: 250px;">
                <div style="position: fixed;">
                    <div id="step-1-icon" class="step-accessing-claims step-accessing-claims-active">Step 1 <span class="step-claims-dash"></span></div> 
                    <div id="step-2-icon" class="step-accessing-claims step-accessing-claims-nonactive">Step 2 <span class="step-claims-dash" style="display: none;"></span></div>
                    <div id="step-3-icon" class="step-accessing-claims step-accessing-claims-nonactive">Step 3 <span class="step-claims-dash" style="display: none;"></span></div>
                    <div id="step-4-icon" class="step-accessing-claims step-accessing-claims-nonactive">Step 4 <span class="step-claims-dash" style="display: none;"></span></div>
                </div>
            </div>
            <div class="col">
            <div id="step-1-content" class="step-content">
                <div class="step-header">Step 1</div>
                <div class="step-title">Attestation</div>
                <div class="line-copy"></div>
                <div class="header-title">Overview</div>
                <div class="row">
                    <div class="col">
                        <p>
                            In order to access Medicare Parts A and B claims data, a Part D Plan (PDP) sponsor must first complete the attestation process for each of
                            their participating Part D contracts. During this process, a Part D Plan (PDP) sponsor formally reviews and agrees to comply with the 
                            <a href="#">Claims
                            Data Usage Protocols</a>. These protocols regulate how their organization may or may not use the Medicare claims data provided by the AB2D
                            API, including limitations associated with the reuse and disclosure of the data.
                            What Type of Organizations Can Attest?
                            Stand-alone Medicare Part D Plan (PDP) sponsors (PACE and MAPD are not eligible).
                        </p>
                    </div>
                    <div class="col">
                        <img src="assets/img/attestation.svg" width="424" height="315" />
                    </div>
                </div>
                <div id="who-can-attest-accordion">
                  <div class="card">
                    <div class="card-header" id="headingOne">
                      <h5 class="mb-0">
                        <button class="btn btn-link" data-toggle="collapse" data-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
                          Who Can Attest?
                        </button>
                      </h5>
                    </div>
                
                    <div id="collapseOne" class="collapse show" aria-labelledby="headingOne" data-parent="#accordion">
                      <div class="card-body">
                        <ul>
                            <li>Attestation must be performed by a Medicare Part D Plan (PDP) Sponsor CEO, CFO, or COO.</li>
                            <li>An Attestor must hold an active CEO, CFO, or COO role within their organization.</li>
                            <li>Part D Plan (PDP) sponsors can have multiple executives attest to each of their participating contracts. This is considered best
                            practice and is strongly encouraged.</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div id="affect-claims-data-accordion">
                  <div class="card">
                    <div class="card-header" id="headingTwo">
                      <h5 class="mb-0">
                        <button class="btn btn-link" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="true" aria-controls="collapseTwo">
                          How Does Attestation Affect Claims Data?
                        </button>
                      </h5>
                    </div>
                
                    <div id="collapseTwo" class="collapse show" aria-labelledby="headingTwo" data-parent="#accordion">
                      <div class="card-body">
                        <ul>
                            <li>Attested Part D Plan (PDP) sponsors are able to retrieve claims data for active plan enrollees from the date of attestation onwards.
                            Claims data prior to the attestation date will not be provided.</li>
                            <li>Participating Part D Plan (PDP) sponsors must have an active Attestor at all times and will not receive data during periods where the
                            (PDP) sponsor does not have an active Attestor. Data access will be restored once another active CEO, CFO, or COO attests. We
                            highly recommend that Part D Plan (PDP) sponsors have multiple executives attest to contracts to reduce the risk of lapses in access
                            to data based on attestation status.</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div id="initially-attest-accordion">
                  <div class="card">
                    <div class="card-header" id="headingThree">
                      <h5 class="mb-0">
                        <button class="btn btn-link" data-toggle="collapse" data-target="#collapseThree" aria-expanded="true" aria-controls="collapseThree">
                          How to initially Attest
                        </button>
                      </h5>
                    </div>
                
                    <div id="collapseThree" class="collapse show" aria-labelledby="headingOne" data-parent="#accordion">
                      <div class="card-body">
                        <ol>
                            <li>Log into HPMS</li>
                            <li>Click on “Claims Data Attestation” (under Contract Management)</li>
                            <li>Select the checkbox(es) next to one, multiple, or all contracts within the 'Contracts Without Attestation' window</li>
                            <li>Click on the "Attest" button</li>
                            <li>Review the Claims Data Usage Protocols</li>
                            <li>Select the checkbox next to: "I hereby certify that I understand the attestation above"</li>
                            <li>Click "Confirm"</li>
                        </ol>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div id="affect-claims-data-accordion">
                  <div class="card">
                    <div class="card-header" id="headingFour">
                      <h5 class="mb-0">
                        <button class="btn btn-link" data-toggle="collapse" data-target="#collapseFour" aria-expanded="true" aria-controls="collapseFour">
                          How to Add Additional Attesters
                        </button>
                      </h5>
                    </div>
                
                    <div id="collapseFour" class="collapse show" aria-labelledby="headingFour" data-parent="#accordion">
                      <div class="card-body">
                        <ol>
                            <li>Log into HPMS</li>
                            <li>Click on "Claims Data Attestation" (under Contract Management)</li>
                            <li>Select the checkbox(es) next to one, multiple, or all contracts within the 'Attested Contract' window</li>
                            <li>Click on the "Re-Attest" button</li>
                            <li>Review the Claims Data Usage Protocols</li>
                            <li>Select the checkbox next to: "I hereby certify that I understand the attestation above"</li>
                            <li>Click "Confirm"</li>
                        </ol>
                      </div>
                    </div>
                  </div>
                </div>
            </div>
            
            <div id="step-2-content" class="step-content">
                <div class="step-header">Step 2</div>
                <div class="step-title">Appoint an "AB2D Operations Specialist"</div>
                <div class="line-copy"></div>
                <div class="header-title">Overview</div>
                
                <div class="row">
                    <div class="col">
                        After attesting, Part D Plan (PDP) Sponsors will need to assign an "AB2D Data Operations Specialist"
                        to act as their organization’s primary technical point of contact.
                        
                        <div class="header-title" style="margin-top: 30px;">The "AB2D Data Operations Specialist"</div>
                        
                        <div class="row">
                            <i class="col material-icons green-check">check</i>
                            <div class="col">Is a technical employee at the Part D Plan (PDP) sponsor that has the authority to access and view the data provided by the API.</div>
                        </div>
                        <br /><br />
                        
                        <div class="row">
                            <i class="col material-icons green-check">check</i>
                            <div class="col">Will be technically savvy enough to connect to the AB2D API and retrieve claims data from our Sandbox and Production environments.</div>
                        </div>
                        <br /><br />
                        
                        <div class="row">
                            <i class="col material-icons green-check">check</i>
                            <div class="col">Will need to provide static IP address(es) and or CIDR ranges for the network/system that is going to be accessing the AB2D API.</div> 
                        </div>
                        
                    </div>
                    <div class="col">
                        <img src="assets/img/data-specialist.svg" />
                    </div>
                </div>
            </div>
            
            <div id="step-3-content" class="step-content">
                <div class="step-header">Step 3</div>
                <div class="step-title">Retrieve Synthetic Claims Data</div>
                <div class="line-copy"></div>
                <div class="header-title">Overview</div>
                
                <div class="row">
                    <div class="col">
                        The Sandbox is a test environment that enables anyone to interact with the AB2D API and retrieve synthetic Medicare Parts A and B claims data.
                        A Part D Plan (PDP) sponsor “AB2D Data Operations Specialist” will need to verify they have retrieved synthetic claims data successfully in 
                        order to gain access to production data. 
                        
                        
                        <div class="header-title" style="margin-top: 30px;">Connecting to the Sandbox</div>
                        We are providing three different ways to retrieve synthetic claims data:
                        <ol>
                            <li><a href="sandbox/tutorial-postman.html">Postman and Swagger User Guide</a></li>
                            <li><a href="sandbox/tutorial-curl.html">Curl User Guide</a></li>
                            <li><a href="sandbox/advanced_user_guide.html">Advanced User Guide</a></li>
                        </ol>
                        
                        <div class="header-title" style="margin-top: 30px;">Verifying Synthetic Data Retrieval</div>
                        <ul>
                            <li>The "AB2D Data Operations Specialist" will need to record the Log ID from jobs that were
                             executed successfully; retrieving synthetic claims data in our Sandbox environment.</li>
                            <li>The "AB2D Data Operations Specialist" will provide the Log ID from one of these jobs to 
                             the AB2D Team.</li>
                        </ul>
                    </div>
                    <div class="col">
                        <img src="assets/img/programmer.svg" />
                    </div>
                </div>
            </div>
            
            <div id="step-4-content" class="step-content">
                <div class="step-header">Step 4</div>
                <div class="step-title">Accessing Claims Data in Production</div>
                <div class="line-copy"></div>
                <div class="header-title">Overview</div>
                
                <div class="row">
                    <div class="col">
                        Once a connection to the sandbox environment has been verified, the AB2D team will work directly with the Part D Plan (PDP) sponsors to 
                        deliver production credentials. The Part D Plan (PDP) sponsor will use the production credentials to retrieve actual Medicare Parts A and B 
                        Claims Data from our production environment.
                        
                        <div class="header-title" style="margin-top: 30px;">
                            Delivering Production Credentials
                        </div>
                        <ul>
                            <li>After the “AB2D Data Operations Specialist” has provided their IP addresses and verified synthetic data retrieval, the AB2D team 
                             will work directly with the Attestor(s) to provide their organization’s production credentials.</li>
                        </ul>
                    </div>
                    <div class="col">
                        <img src="assets/img/production.svg" />
                    </div>
                </div>
                <div class="header-title">
                    Retrieving Actual Claims Data
                </div>
                <div>
                    <ul>
                        <li>Using the production credentials, the Part D Plan (PDP) sponsors will connect to the AB2D API in our production environment to download
                         actual Medicare Parts A and B claims data.</li>
                        <li>he AB2D team will work closely with the organization and its "AB2D Data Operations Specialist" to answer any questions or troubleshoot
                         any issues they have connecting to the AB2D API.</li> 
                    </ul> 
                </div>
                            
            </div>
            
            </div>
        </div>
    </div>
</section>