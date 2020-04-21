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
<script src="assets/js/jquery.binarytransport.js" type="text/javascript"></script>

<style type="text/css">
    #export {
        display: none;
    }
    
    #export-status {
        display: none;
    }
    
    #progress-bar {
        display: none;
    }
    
    #reset-section {
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
    
    #alert-toast {
        z-index: 1000;
    }
    
    .ab2d-easy-section {
        margin-top: 45px;
    }
    
    #download-section {
        display: none;
    }
    
    #download-section-content {
        padding: 5px;
    }
    
    #download-files-text {
        margin-bottom: 5px;
    }
    
    #download-section-links {
        padding-inline-start: 5px;
        list-style: none;
        margin-top: 5px;
    }
    
    #download-section-links li {
        margin-bottom: 8px;
    }
    
    #download-section-links a {
        color: #4B78C4;
    }
    
    .progress {
        background-color: #DEDFEF;
    }
</style>

<script>
    // Sandbox URL, could change
    const baseUrl = 'https://sandbox.ab2d.cms.gov/';

    const fhirSegment = 'api/v1/fhir/';
    
    const fadeInTime = 1000;
    const fadeOutTime = 1000;
    
    const statusIntervalTimeout = 6000;
    let statusInterval = undefined;
    
    let contentLocationUrl = undefined;
    
    const successClass = 'alert-success';
    const failureClass = 'alert-danger';
    
    let token = '';
    
    let headerRight = undefined;
    
    const toastOptions = {
        delay: 4500
    };
    
    let downloadStartTime = undefined;    

    function showAlert(cssClass, message) {
        $('#toast-body').text(message).removeClass(successClass).removeClass(failureClass).addClass(cssClass);
        $('#alert-toast').toast(toastOptions);
        $('#alert-toast').toast('show');
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
                showAlert(successClass, 'Successfully authenticated');
                
                $("#export").fadeIn(fadeInTime);
                turnOnExportEventHandler();
            },
            error: function(data) {
                showAlert(failureClass, "Failed to authenticate. Please try again."); 
            }
        });
    }
    
    function startExport() {
        const contractNumber = $("#contractNumber").val();
        
        let url = '';
        if(contractNumber === undefined || contractNumber === null || contractNumber === '') {
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
                $('#progress-bar').fadeIn(fadeInTime);
                initiateStatusChecks();
                turnOffTokenEventHandler();
                turnOffExportEventHandler();
                turnOnCancelEventHandler();
                initiateDownloadTimer();

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
                    showDownloadLinks(xhr.responseJSON);
                } else if(xhr.status === 500) {
                    cancelStatusInterval();
                }
            },
            error: function() {
                showAlert(failureClass, "Failed to check status for bulk export.");
            }
        });
    }
    
    function cancelStatusInterval() {
        clearInterval(statusInterval);    
    }
    
    function initiateDownloadTimer() {
        downloadStartTime = new Date();
    }
    
    function updateProgressBar(value) {
        let text = value + '%';
    
        if(value > 0 && value < 100) {
            let timeElapsedMilliseconds = new Date().getTime() - downloadStartTime.getTime();
            let totalEstimatedMilliseconds = timeElapsedMilliseconds * (100 / value);
            let totalEstimatedMillisecondsLeft = totalEstimatedMilliseconds - timeElapsedMilliseconds;
            // Use Math.ceil, avoid showing a 0 seconds left scenario
            let totalSecondsLeft = Math.ceil(totalEstimatedMillisecondsLeft / 1000);
            if(totalSecondsLeft > 60) {
                let minutes = Math.floor(totalSecondsLeft / 60);
                let seconds = totalSecondsLeft - 60 * minutes;
                let totalTimeRemaining = minutes + " minutes, " + seconds + " seconds remaining";
                text += " (" + totalTimeRemaining + ")";
            } else {
                text += " (" + totalSecondsLeft + " seconds remaining)";
            }
        }
    
        $('#progress-bar .progress-bar').css('width', value + '%').attr('aria-valuenow', value)
            .text(text);
    }
    
    function cancelExport() {
        $.ajax({
            url: contentLocationUrl,
            headers: {
                'Authorization': 'Bearer ' + token
            },
            type: 'delete',
            success: function(data, status, xhr) {
                showAlert(successClass, "Cancelled bulk export");
                doReset();
            },
            error: function() {
                showAlert(failureClass, "Failed to cancel bulk export. Please try again");
            }
        });
    }
    
    function turnOnTokenEventHandler() {
        $("#generate-token-button").removeClass("disabled").addClass("enabled");
        $("#generate-token-button").on("click", function(event) {
            event.preventDefault();
            retrieveOktaToken();
        });
    }
    
    function turnOffTokenEventHandler() {
        $("#generate-token-button").removeClass("enabled").addClass("disabled");
        $("#generate-token-button").off("click");
    }
    
    function turnOnExportEventHandler() {
        $("#export-button").removeClass("disabled").addClass("enabled");
        $("#export-button").on("click", function(event) {
            event.preventDefault();
            startExport();
        });
    }
    
    function turnOffExportEventHandler() {
        $("#export-button").removeClass("enabled").addClass("disabled");
        $("#export-button").off("click");
    }
    
    function turnOnCancelEventHandler() {
        $("#cancel-button").removeClass("disabled").addClass("enabled");
        $("#cancel-button").on("click", function(event) {
            event.preventDefault();
            cancelExport();
        });
    }
    
    function turnOffCancelEventHandler() {
        $("#cancel-button").removeClass("enabled").addClass("disabled");
        $("#cancel-button").off("click");
    }
    
    function turnOnResetEventHandler() {
        $("#reset-button").on("click", function(event) {
            event.preventDefault();
            resetAll();  
        });  
    }
    
    function resetAll() {
        $('#reset-section').fadeOut(fadeOutTime);
        $('#download-section').fadeOut(fadeOutTime, function() {
            $('#download-section-links').html('');  
        });
        doReset();
    }
    
    function doReset() {
        cancelStatusInterval();
        resetProgressBar();
        $("#export").fadeOut(fadeOutTime, function() {
            $('#contractNumber').val('');  
        });
        turnOnTokenEventHandler();
        turnOffCancelEventHandler();
        turnOffExportEventHandler();
    }
    
    function resetProgressBar() {
        $("#progress-bar").fadeOut(fadeOutTime, function() {
            updateProgressBar(0);
        });
    }
    
    function downloadJSON(url, linkTitle) {
        $.ajax({
            url: url,
            headers: {
                'Authorization': 'Bearer ' + token
            },
            dataType: 'binary',
            type: 'get',
            processData: 'false',
            success: function(blob) {
                showAlert(successClass, "File download started");
                const windowUrl = window.URL || window.webkitURL;
                const downloadUrl = windowUrl.createObjectURL(blob);
                const anchor = $("<a></a>"); 
                anchor.css("display", "none"); 
                $("body").append(anchor);
                anchor.prop('href', downloadUrl);
                anchor.prop('download', linkTitle);
                anchor.get(0).click();
                windowUrl.revokeObjectURL(downloadUrl);
                anchor.remove();
            },
            error: function() {
                showAlert(failureClass, "Failed to download file");
            }
        });
    }
    
    function showDownloadLinks(responseJSON) {
        if(responseJSON.error.length > 0) {
            showAlert(failureClass, 'There was an error while processing the file');
        }
        for(let i = 0; i < responseJSON.output.length; i++) {
            const url = responseJSON.output[i].url;
            const linkTitle = getLinkTitle(url);
            $("#download-section-links").append("<li><a href='" + url + "'>" +
                linkTitle + "</a></li>");
        }
        
        $("#download-section-links li").each(function() {
            $(this).one('click', function(event) {
                event.preventDefault();
                const anchor = $(this).find('a');
                const url = anchor.attr('href');
                const linkTitle = getLinkTitle(url);
                downloadJSON(url, linkTitle);
            });
        });
        
        $("#download-section").fadeIn(fadeInTime, function() {
            $("#reset-section").fadeIn(fadeInTime);
            turnOnResetEventHandler();  
        });
    }
    
    function getLinkTitle(url) {
        return url.substring(url.indexOf(fhirSegment) + fhirSegment.length, url.length);
    }
    
    $(document).ready(function() {
        turnOnTokenEventHandler();
    });
</script>

<div id="ab2d-easy-section" style="padding: 5px;">

    <h3 id="ab2d-easy-header">AB2D Easy</h3>
        
    <div class="toast" id="alert-toast" role="alert" aria-live="assertive" aria-atomic="true" style="position: absolute;">
        <div class="toast-header">
            <small>Notification</small>
            <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" style="position: absolute; right: 5px;">
            <span aria-hidden="true">&times;</span>
            </button>
        </div>
        <div class="toast-body" id="toast-body"></div>
    </div>   
       
    <br />
    
    <div class="intro-text">
        This page will help guide you through the workflow to do a bulk data export. Fill out the forms below to
        download sample data.
    </div>
    
    <div class="form-group ab2d-easy-section">
        <form>
            <div class="form-row">
                <div class="col-md-6 mb-3">
                  <label for="clientID">Client ID</label>
                  <input type="text" class="form-control" id="clientID" placeholder="Client ID" required>
                </div>
                <div class="col-md-6 mb-3">
                  <label for="clientSecret">Client Secret</label>
                  <input type="password" class="form-control" id="clientSecret" placeholder="Client Secret" required>
                </div>
            </div>
            <div class="form-row">
                <div class="col-md-6 mb-3">
                    <button class="btn btn-primary" id="generate-token-button">Authenticate</button>
                </div>
            </div>
        </form>    
    </div>
    
    <div class="form-group ab2d-easy-section" id="export">
        <form>
            <div class="form-row">
                <div class="col-md-6 mb-3">
                    <label for="clientSecret">Contract Number</label>
                    <input type="text" class="form-control" id="contractNumber" placeholder="Contract Number (Optional)">               
                </div>
            </div>
            <div class="form-row">
                <div class="col-md-6 mb-3">
                    <button class="btn btn-primary" type="submit" id="export-button">Start Export</button>
                    <button class="btn btn-danger disabled" type="submit" id="cancel-button">Cancel Export</button>
                </div>
            </div>
        </form>
    </div>
        
    <div id="progress-bar" class="ab2d-easy-section">    
        <div class="progress" style="height: 32px;">
            <div class="progress-bar progress-bar-striped" role="progressbar" style="width: 0%; padding: 0px 5px 0px 5px;" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">0%</div>
        </div>
    </div>
    
    <div id="download-section" class="ab2d-easy-section">
        <div id="download-files-text">Download Files</div>
        <div id="download-section-content">
            <ul id="download-section-links">
            </ul>
        </div>
    </div>
    
    <div id="reset-section" class="ab2d-easy-section">
         <button class="btn btn-primary" type="submit" id="reset-button">Reset</button>
    </div>
</div>