---
layout: tutorial_layout
title: "Postman Tutorial"
date: 2019-11-02 09:21:12 -0500
description: Postman and Swagger Tutorial
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

    #index {
        margin: auto;
    }

    #index a {
        color: #6c7b8d;
    }

    .sticky {
        position: fixed;
        top: 90px;
        left: 5%;
    }

    #scroll-to-top {
        position: fixed;
        bottom: 20px;
        right: 20px;
        z-index: 2;
        background-color: #323A45;
        padding: 16px;
        border-radius: 50%;
        padding: 13px 17px;
        color: white;
        cursor: pointer;
        display: none;
    }

    .show {
        display: block !important;
    }

    p {
        margin-top: 10px;
        margin-bottom: 5px;
    }

    h5,
    h4 {
        margin-top: 20px;
    }
</style>

<script>
    let scrollLock = false;
    
    const stepMappings = {
        'PostmanInstructions': 'PostmanInstructions-content',
        'SandboxandtheOnboardingProcess': 'SandboxandtheOnboardingProcess-content',
        'Whatdoesthisinstructionguidecontain': 'Whatdoesthisinstructionguidecontain-content',
        'step1': 'step1-content',
        'DownloadingPostman': 'DownloadingPostman-content',
        'Creatinganewcollection': 'Creatinganewcollection-content',
        'Savingarequest': 'Savingarequest-content',
        'Postingarequest': 'Postingarequest-content',
        'Step2': 'Step2-content',
        'Authorizeabearertoken': 'Authorizeabearertoken-content',
        'ExportaJobID': 'ExportaJobID-content',
        'Checkthestatusofyourjob': 'Checkthestatusofyourjob-content',
        'DownloadyourJobID': 'DownloadyourJobID-content',
        'Questions': 'Questions-content'
    };
    $(document).ready(function() {
        const offset = 100;
    
        $('.step-accessing-claims').on('click', function(event) {
            scrollLock = true;
            highlightNav($(this), true);
        });
        
        $(window).scroll(function() {
            if(scrollLock) {
                return;
            }
                
            if($(this).scrollTop() < $('#provider-content').offset().top - offset) {
                highlightNav($('#dates'), false);
            } else if($(this).scrollTop() >= $('#provider-content').offset().top - offset && $(this).scrollTop() < $('#diagnosis-content').offset().top - offset) {
                highlightNav($('#provider'), false);
            } else if($(this).scrollTop() >= $('#diagnosis-content').offset().top - offset && $(this).scrollTop() < $('#faculty-organization-content').offset().top - offset) {
                highlightNav($('#diagnosis'), false);
            } else if($(this).scrollTop() >= $('#faculty-organization-content').offset().top - offset && $(this).scrollTop() < $('#location-content').offset().top - offset) {
                highlightNav($('#faculty-organization'), false);
            } else if($(this).scrollTop() >= $('#location-content').offset().top - offset && $(this).scrollTop() < $('#procedure-content').offset().top - offset) {
                highlightNav($('#location'), false);
            } else if($(this).scrollTop() >= $('#procedure-content').offset().top - offset && $(this).scrollTop() < $('#services-content').offset().top - offset) {
                highlightNav($('#procedure'), false);
            } else if($(this).scrollTop() >= $('#services-content').offset().top - offset && $(this).scrollTop() < $('#claim-content').offset().top - offset) {
                highlightNav($('#services'), false);
            } else if($(this).scrollTop() >= $('#claim-content').offset().top - offset && $(this).scrollTop() < $('#patient-content').offset().top - offset) {
                highlightNav($('#claim'), false);
            } else if($(this).scrollTop() >= $('#patient-content').offset().top - offset && $(this).scrollTop() < $('#meta-content').offset().top - offset) {
                highlightNav($('#patient'), false);
            } else if($(this).scrollTop() >= $('#step-4-content').offset().top - offset) {
                highlightNav($('#meta'), false);
            }
        });
        
        function highlightNav(id, doScroll) {
            const stepId = id.attr('id');
            const idToShow = stepMappings[stepId];
            
            $('.step-accessing-claims').each(function() {
                $(this).removeClass('step-accessing-claims-active').addClass('step-accessing-claims-nonactive');
                $(this).find('.step-claims-dash').hide();
            });
            
            id.removeClass('step-accessing-claims-nonactive').addClass('step-accessing-claims-active');
            id.find('.step-claims-dash').show();
            
            if(doScroll) {
                $('html, body').animate({
                    scrollTop: $('#' + idToShow).offset().top - offset + 2
                }, 1000, function() {
                  scrollLock = false;
                });
            }
        }
        
        if (isIE()) {
            const elements = $('.step-claims-menu');
            Stickyfill.add(elements);
        }
    });
</script>

<section class="page-section py-5" role="main" id="Top">
    <a href="#Top" id="scroll-to-top">
        <i class="fas fa-chevron-up"></i>
    </a>
    <div class="container-fluid bg-light-grey">
        <div class="row">
            <div class="col-lg-2 step-claims-menu-col" style="max-width: 250px;">
                <div class="step-claims-menu">
                    <div id="PostmanInstructions" class="step-accessing-claims step-accessing-claims-active">
                        Postman Instructions <span class="step-claims-dash"></span>
                    </div> 
                    <div id="SandboxandtheOnboardingProcess" class="step-accessing-claims step-accessing-claims-nonactive">
                        Sandbox and the Onboarding Process <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Whatdoesthisinstructionguidecontain" class="step-accessing-claims step-accessing-claims-nonactive">
                        What does this instruction guide contain? <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="step1" class="step-accessing-claims step-accessing-claims-nonactive">
                        Step 1: Postman Instructions <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="DownloadingPostman" class="step-accessing-claims step-accessing-claims-nonactive">
                        Downloading Postman <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Creatinganewcollection" class="step-accessing-claims step-accessing-claims-nonactive">
                        Creating a new collection <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Savingarequest" class="step-accessing-claims step-accessing-claims-nonactive">
                        Saving a request <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Postingarequest" class="step-accessing-claims step-accessing-claims-nonactive">
                        Posting a request <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Step2" class="step-accessing-claims step-accessing-claims-nonactive">
                        Step 2: Swagger Instructions <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Authorizeabearertoken" class="step-accessing-claims step-accessing-claims-nonactive">
                        Authorize a bearer token <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="ExportaJobID" class="step-accessing-claims step-accessing-claims-nonactive">
                        Export a Job ID <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Checkthestatusofyourjob" class="step-accessing-claims step-accessing-claims-nonactive">
                        Check the status of your job <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="DownloadyourJobID" class="step-accessing-claims step-accessing-claims-nonactive">
                        Download your Job ID <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                    <div id="Questions" class="step-accessing-claims step-accessing-claims-nonactive">
                        Questions? <span class="step-claims-dash" style="display: none;"></span>
                    </div>
                </div>
            </div>
            <div class="col-lg-10">
                <h1 id="PostmanInstructions-content">Postman Instructions</h1>

                <h5 id="SandboxandtheOnboardingProcess-content">Sandbox and the Onboarding Process</h5>
                <p>
                    After attesting and electing an AB2D Data Operations Specialist (ADOS), a PDP organization must
                    demonstrate their ability to use the API to access Production by successfully retrieving synthetic
                    claims data from the test (Sandbox) environment. In order to verify this requirement, PDP
                    organizations must provide the AB2D team with the Job ID from a successful run in the test (Sandbox)
                    environment.
                </p>

                <h5 id="Whatdoesthisinstructionguidecontain-content">What does this instruction guide contain?</h5>

                <p>These instructions will guide you through the process of obtaining a JSON web token (JWT), also
                    referred to as a bearer token, using Postman. Postman offers a Graphical User Interface (GUI) and
                    provides an easy entrance point for users. Once complete, this token will be used to pull synthetic
                    claims data by accessing test (Sandbox) API endpoints using another application called Swagger. The
                    instructions below: Postman (Step 1) + Swagger (Step 2) are needed to access the Sandbox
                    environment. </p>

                <h4 id="Step1-content">Step 1: Postman Instructions</h4>
                <p>The Postman directions below are broken up into the following sections:</p>
                <ul>
                    <li>Downloading Postman</li>
                    <li>Creating a new collection</li>
                    <li>Saving a request</li>
                    <li>Posting a request</li>
                </ul>

                <h5 id="DownloadingPostman-content">Downloading Postman</h5>

                <p>
                    Go to the Postman site <a target="_blank" href="https://www.postman.com/downloads/">here</a> to
                    download and install the app
                    version of Postman. Because only the app version of Postman is allowed in Production, we promote the
                    use of this version in Sandbox as well. The web version is available to you in Sandbox, but it will
                    not
                    be in Production. The directions below follow along with the app version of Postman.
                </p>

                <img src="./assets/img/sandbox/postman-1.png" alt="download postman">

                <p>You will then be directed to an account sign-in page. Note, you are able to directly access the app
                    and
                    skip sign-in by clicking the link at the bottom of the page as shown below.
                </p>

                <img src="./assets/img/sandbox/postman-2.png" alt="postman signin">

                <h5 id="Creatinganewcollection-content">Creating a new collection</h5>

                <p>Click on the orange <strong>+ New</strong> button in the top left corner of the app.</p>

                <img src="./assets/img/sandbox/postman-3.png" alt="postman create new button">

                <p>Choose <strong>Create New</strong> to create a new Collection:</p>

                <img src="./assets/img/sandbox/postman-4.png" alt="postman create new collection">

                <p>
                    Configure as follows: <br />
                    Name: <strong>ab2d</strong> <br />
                    Choose: <strong>Create.</strong> <br />
                </p>

                <img src="./assets/img/sandbox/postman-5.png" alt="postman create new
                collection details">

                <p>In the left hand panel, click on the three dot’s next the ab2d node you just created and choose
                    <strong>Add
                        Request:</strong>
                </p>

                <img src="./assets/img/sandbox/postman-6.png" alt="postman new request">

                <h5 id="Savingarequest-content">Saving a request</h5>
                <p>Configure the “SAVE REQUEST” page as follows:</p>
                <ul>
                    <li>Request name: <strong>retrieve-a-token</strong></li>
                    <li>Select: <strong>Save to ab2d</strong> at bottom right corner.</li>
                </ul>

                <img src="./assets/img/sandbox/postman-7.png" alt="postman save new request">

                <h5 id="Postingarequest-content">Posting a request</h5>

                <p>Click on <strong>GET, retrieve-a-token</strong> under the ab2d node and immediately, a new tab will
                    appear to the
                    right.</p>

                <img src="./assets/img/sandbox/postman-8.png" alt="postman post a request">

                <p>Alter the <strong>GET</strong> request to a <strong>POST</strong> request:</p>

                <img src="./assets/img/sandbox/postman-9.png" alt="postman change get to post">

                <p>
                    In the bar next to <strong>POST</strong> enter the following URL: <br />
                    https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token
                </p>

                <img src="./assets/img/sandbox/postman-10.png" alt="postman enter url">

                <p>Configure the Params tab as follows:</p>

                <table class="ds-c-table">
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
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
                <br />

                <img src="./assets/img/sandbox/postman-11.png" alt="postman enter params">

                <p>
                    Configure the Headers tab as follows: <br>
                    Choose one of the sample Base64-encoded credentials from a sample PDP Sponsor. This will be placed
                    under
                    the <strong>Value</strong> column by <strong>Authorization</strong>.
                </p>

                <table class="ds-c-table">
                    <thead>
                        <tr>
                            <th>PDP Sponsor</th>
                            <th>Base64-encoded id: password</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>0oa2t0lsrdZw5uWRx297</td>
                            <td>MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==
                            </td>
                        </tr>
                        <tr>
                            <td>0lc65ErV8OmY297</td>
                            <td>MG9hMnQwbGM2NUVyVjhPbVkyOTc6MUJsM0hHTzZlZ2xrWFVEdGpWanRvM0wtM0Mwb2ZmelRNazJxbHo5cg==
                            </td>
                        </tr>
                        <tr>
                            <td>0oa2t0lkicpxFGkGt297</td>
                            <td>MG9hMnQwbGtpY3B4RkdrR3QyOTc6ZURwYW5KVHR3OTB2WTJ2aVlsWDRvMnJnVlJJUjR0RFJIMG1Xcjl2Tg==
                            </td>
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

                <br />

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
                <br />

                <img src="./assets/img/sandbox/postman-12.png" alt="postman headers filled">
                <p>
                    Select <strong>Send.</strong> <br>
                    In the body below you should see a token type, expires in statement, an access token, and scope
                    statement as shown below:
                </p>

                <img src="./assets/img/sandbox/postman-13.png" alt="postman response body">

                <p>
                    You will use this bearer token, specified by the <strong>access_token</strong> value (in the next
                    hour), to access
                    Sandbox endpoints in Swagger, which we explain how to use below.
                </p>

                <h4 id="Step2-content">Step 2: Swagger Instructions</h4>

                <p>The Swagger directions below are broken into the following sections:</p>
                <ul>
                    <li>Authorize your bearer token</li>
                    <li>Export a Job ID</li>
                    <li>Check the status of a job</li>
                    <li>Download your Job ID</li>
                </ul>

                <h5 id="Authorizeabearertoken-content">Authorize a bearer token</h5>
                <p>First - you must access the AB2D Swagger site by going <a target="_blank"
                        href="https://sandbox.ab2d.cms.gov/swagger-ui/index.html">here</a> Click
                    “authorize” in the top right corner.</p>
                <img src="./assets/img/sandbox/swagger-1.png" alt="swagger authorize">

                <p>
                    Use the bearer token (retrieved in the last 24 hours by you, and no other user) to authorize entry
                    into
                    the Sandbox endpoints. You will place this in the box under Value, adding the word <strong>Bearer
                        before the
                        token.</strong>
                </p>

                <img src="./assets/img/sandbox/swagger-2.png" alt="swagger authorize api key">

                <p>Be sure to leave a space between the word <strong>Bearer</strong> and the actual bearer token. Also
                    remove any quotes
                    from the token itself. Click <strong>Authorize</strong>.</p>

                <img src="./assets/img/sandbox/swagger-3.png" alt="swagger enter api key">

                <p>You will see the following message:</p>
                <img src="./assets/img/sandbox/swagger-4.png" alt="swagger authorize response">

                <p>Click Close to <strong>close</strong> the window.</p>

                <h5 id="ExportaJobID-content">Export a Job ID</h5>
                <p>
                    Open up the <strong>Export</strong> menu to view all possible endpoints:
                </p>

                <img src="./assets/img/sandbox/swagger-5.png" alt="swagger export menu">

                <p>Choose <strong> /api/v1/fhir/Patient/$export</strong> to initiate a Part A & B bulk claim export job.
                    Then choose to
                    <strong>Try it out</strong> in the right hand corner.
                </p>

                <img src="./assets/img/sandbox/swagger-6.png" alt="swagger export parameters">

                <p>Under <strong>Prefer</strong> add <strong> respond-async</strong> and then click the big blue bar to
                    <strong> Execute.</strong>
                </p>

                <img src="./assets/img/sandbox/swagger-7.png" alt="swagger execute">

                <p>In the responses, look at the first code provided under <strong>Server response</strong>. Below that
                    are all the
                    other possible responses. The correct response should be a <strong>202</strong>, which means
                    <strong>Accepted</strong>. This means
                    the job has been created.
                </p>
                <img src="./assets/img/sandbox/swagger-8.png" alt="swagger response">

                <p>
                    From the information provided in the response, copy the Job ID from within the status request.
                    Format:
                    <br>
                    <em>content-location: http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/<strong>{job
                            id}</strong>/$status</em>
                </p>

                <p>
                    Example: <br>
                    <em>content-location:</em> <br>
                    <em>http://sandbox.ab2d.cms.gov/api/v1/fhir/Job/afc222d1-a55b-403b-ad22-49f5aefec4b6/$status</em>
                </p>

                <h5 id="Checkthestatusofyourjob-conent">Check the status of your job</h5>

                <p>While these are test jobs and most will run immediately, it is good practice to understand the steps
                    associated with running a job, including checking its status.</p>

                <p>Click on the <strong>Status</strong> menu to view the status endpoints:</p>

                <img src="./assets/img/sandbox/swagger-9.png" alt="swagger job status">

                <p>Copy the Job ID from the Export step. Click on the <strong>GET
                        /api/v1/fhir/{jobUuid}/$status</strong> endpoint,
                    click <strong>Try it out</strong> and paste the Job ID into the box provided.</p>

                <img src="./assets/img/sandbox/swagger-10.png " alt="swagger endpoint status">

                <p>Click on the big blue bar labeled <strong>Execute</strong>.</p>
                <img src="./assets/img/sandbox/swagger-11.png" alt="swagger try it out">

                <p>
                    In the responses, view the first value. This is the server response. There are two possible values,
                    202
                    or 200. If the response is 202, this means that the job is still in progress. It will give you an
                    indication of the job progress from 1 to 100%.
                </p>
                <img src="./assets/img/sandbox/swagger-12.png" alt="swagger response">

                <p>You will need to re-click on the Execute blue bar periodically until the status returns a 200. This
                    means the job is done and the response will contain a list of files. These files can then be
                    downloaded
                    and contain the claim records for our sample job.</p>

                <h5 id="DownloadyourJobID-content">Download your Job ID</h5>

                <p>Click on the <strong>Download</strong> menu in swagger. Select the <strong>GET
                        /api/v1/fhir/Job/{jobUuid}/file/{filename}</strong>
                    endpoint to download a file. Click <strong>Try it out</strong>. Enter the Job ID of the job you
                    created and the file
                    name, then press the <strong>Execute</strong> big blue bar.</p>

                <img src="./assets/img/sandbox/swagger-13.png" alt="swagger download">

                <p>
                    It might take a while for the file to be downloaded depending on how big the job is. The browser may
                    even stop responding, but it will eventually respond. The <strong>Server response</strong> value should be a
                    <strong>200</strong>
                    and the <strong>Response body</strong> will contain the claims data. To download the data into a file, click on
                    the
                    Download button in the lower right corner. This will be saved as an ndjson (new line delimited JSON)
                    file in your downloads. This data format will be identical to the production data. Only the Job ID
                    from
                    this file is needed - please send the Job ID to the AB2D team per the instructions emailed to your
                    organizations assigned ADOS.
                </p>

                <img src="./assets/img/sandbox/swagger-14.png" alt="swagger response body">
                <img src="./assets/img/sandbox/swagger-15.png" alt="swagger json file">

                <h1 id="Questions-content">Questions?</h1> 
                <p>
                    Having issues or concerns - please get in touch. <br>
                    <a href="mailto:ab2d@semanticbits.com">ab2d@semanticbits.com</a> - direct email <br>
                    <a href="https://groups.google.com/u/1/g/cms-ab2d-api">AB2D Google Group</a> - join the conversation
                </p>
            </div>
        </div>
    </div>
</section>