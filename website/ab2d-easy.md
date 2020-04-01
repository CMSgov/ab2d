---
layout: home
title:  "AB2D Easy"
date:   2019-11-02 09:21:12 -0500 
description: Walk through the workflow of a bulk data export 
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - name: Home
    link: /
ctas:

---

<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css" integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS" crossorigin="anonymous">

<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js" integrity="sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js" integrity="sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k" crossorigin="anonymous"></script>

<style type="text/css">
    #okta-token-status-message {
        display: none;
        padding: 5px;
        color: black;
    }
    
    #export {
        display: none;
    }
    
    #export-status {
        display: none;
    }
    
    #export-in-progress {
        display: none;
    }
    
    #progress-bar {
        display: none;
    }
    
    .failure-status {
        border: 1px solid red;
        background-color: lightcoral;
    }
    .success-status {
        border: 1px solid green;
        background-color: lightgreen;
    }
    
    .myAlert-top {
        position: fixed;
        top: 5px; 
        left: 2%;
        width: 96%;
    }
    .alert {
        display: none;
    }
</style>

<script>
    const baseUrl = 'http://localhost:8080/';
    const fhirSegment = 'api/v1/fhir/';
    
    const fadeInTime = 1000;
    const fadeOutTime = 1000;
    
    const statusIntervalTimeout = 6000;
    let statusInterval = undefined;
    
    let contentLocationUrl = undefined;
    
    const successClass = 'alert-success';
    const failureClass = 'alert-danger';
    
    let token = '';
    
    function showAlert(cssClass, message) {
        $("#alert-dialog").removeClass(successClass).removeClass(failureClass);
        $("#alert-dialog").addClass(cssClass);
        $("#alert-text").text(message);
        
        $("#alert-dialog").fadeIn(fadeInTime);
        setTimeout(function() {
            $("#alert-dialog").fadeOut(fadeOutTime); 
        }, 3500);
    }

    function retrieveOktaToken() {
    
        const clientID = $('#clientID').val();
        const clientSecret = $('#clientSecret').val();
        const formData = {
            'clientID': clientID,
            'clientSecret': clientSecret
        };
        $.ajax({
            url: baseUrl + 'oktaproxy',
            data: formData,
            dataType: 'json',
            type: 'post',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            success: function (data) {
                token = data.accessToken;
                showAlert(successClass, 'Successfully retrieved okta token');
                
                $("#export").fadeIn(fadeInTime);
                turnOnExportEventHandler();
            },
            error: function(data) {
                $("#okta-token-status-message").html("Failed to retrieve okta token. Please try again.").addClass("failure-status").show();
            }
        });
    }
    
    function startExport() {
        const contractNumber = $("#contractNumber").val();
        
        let url = '';
        if(contractNumber === undefined || contractNumber === '') {
            url = baseUrl + fhirSegment + 'Patient/$export';
        } else {
            url = baseUrl + fhirSegment + 'Group/' + contractNumber + '/$export';        
        }
        
        $.ajax({
            url: url,
            headers: {
                'Authorization': 'Bearer ' + token
            },
            type: 'get',
            success: function(data, status, xhr) {
                contentLocationUrl = xhr.getResponseHeader('Content-Location');
                showAlert(successClass, "Bulk export successfully started.");
                $('#export-in-progress').fadeIn(fadeInTime);
                $('#progress-bar').fadeIn(fadeInTime);
                initiateStatusChecks();
                turnOffTokenEventHandler();
                turnOffExportEventHandler();
                turnOnCancelEventHandler();
            },
            error: function() {
                showAlert(failureClass, "Failed to start bulk export. Please try again"); 
            }
        });
    }
    
    function initiateStatusChecks() {
        statusInterval = setInterval(function() {
            doStatusCheck(contentLocationUrl);
        }, statusIntervalTimeout);
    }
    
    function doStatusCheck() {
        $.ajax({
            url: contentLocationUrl,
            headers: {
                'Authorization': 'Bearer ' + token
            },
            type: 'get',
            success: function(data, status, xhr) {
                if(xhr.status === 202) {
                    let xProgress = xhr.getResponseHeader('X-Progress');
                    let value = xProgress.substring(0, xProgress.indexOf('%'));
                    updateProgressBar(value);
                } else if(xhr.status === 200) {
                    cancelStatusInterval();
                    turnOffCancelEventHandler();
                    updateProgressBar(100);
                } else if(xhr.status === 500) {
                    cancelStatusInterval();
                }
            },
            error: function() {
                showAlert(failureClass, "Failed to start bulk export. Please try again");
            }
        });
    }
    
    function cancelStatusInterval() {
        clearInterval(statusInterval);    
    }
    
    function updateProgressBar(value) {
        $('#progress-bar .progress-bar').css('width', value + '%').attr('aria-valuenow', value)
            .text(value + '%');
    }
    
    function cancelExport(event) {
        $.ajax({
            url: contentLocationUrl,
            headers: {
                'Authorization': 'Bearer ' + token
            },
            type: 'get',
            success: function(data, status, xhr) {
            
            },
            error: function() {
                showAlert(failureClass, "Failed to start bulk export. Please try again");
            }
        });
    }
    
    function turnOnTokenEventHandler() {
        $("#generate-token-button").addClass("enabled");
        $("#generate-token-button").on("click", function(event) {
            event.preventDefault();
            retrieveOktaToken();
        });
    }
    
    function turnOffTokenEventHandler() {
        $("#generate-token-button").addClass("disabled");
        $("#generate-token-button").off("click");
    }
    
    function turnOnExportEventHandler() {
        console.log("turning on");
        $("#export-button").addClass("enabled");
        $("#export-button").on("click", function(event) {
            console.log("default");
            console.dir(event);
            event.preventDefault();
            startExport();
        });
    }
    
    function turnOffExportEventHandler() {
        $("#export-button").addClass("disabled");
        $("#export-button").off("click");
    }
    
    function turnOnCancelEventHandler() {
        $("#cancel-button").addClass("enabled");
        $("#cancel-button").on("click", function(event) {
            event.preventDefault();
            startExport();
        });
    }
    
    function turnOffCancelEventHandler() {
        $("#cancel-button").addClass("disabled");
        $("#cancel-button").off("click");
    }
    
    $(document).ready(function() {
        turnOnTokenEventHandler();
    });
</script>

<div id="ab2d-easy-section" style="padding: 5px;">

    <div class="myAlert-top alert" id="alert-dialog">
        <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
        <span id="alert-text"></span>
    </div>

    <h3>AB2D Easy</h3>
    
    <br />
    
    <div class="intro-text">
        This page will help guide you through the workflow to do a bulk data export. Fill out the forms below to
        download sample data.
    </div>
    
    <br />
    
    <form>
        <div class="form-row form-group">
            <div class="col-md-6 mb-3">
              <label for="clientID">Client ID</label>
              <input type="text" class="form-control" id="clientID" placeholder="Client ID" required>
            </div>
            <div class="col-md-6 mb-3">
              <label for="clientSecret">Client Secret</label>
              <input type="text" class="form-control" id="clientSecret" placeholder="Client Secret" required>
            </div>
        </div>
        <button class="btn btn-primary" id="generate-token-button">Get Token</button>
    </form>
    
    <br />
    
    <form>
        <div class="form-row form-group" id="export">
            <div class="col-md-6 mb-3">
              <label for="clientSecret">Contract Number</label>
              <input type="text" class="form-control" id="contractNumber" placeholder="Contract Number (Optional)">
            </div>
            <button class="btn btn-primary" type="submit" id="export-button">Start Export</button>
        </div>
    </form>
    
    <br />
    
    <div id="export-in-progress">
        <form>
            <button class="btn btn-primary" type="submit" id="cancel-button">Cancel Export</button>
            <div class="form-row form-group" id="status"></div>
        </form>
    </div>
        
    <br />    
        
    <div id="progress-bar">    
        <div class="progress">
            <div class="progress-bar" role="progressbar" style="width: 0%;" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">0%</div>
        </div>
    </div>        
    
    <!-- Download Data -->
</div>