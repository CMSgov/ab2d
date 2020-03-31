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
    
    .failure-status {
        border: 1px solid red;
        background-color: lightcoral;
    }
    .success-status {
        border: 1px solid green;
        background-color: lightgreen;
    }
</style>

<script>
    const baseUrl = 'http://localhost:8080/';
    const fhirSegment = 'api/v1/fhir/';
    let token = '';

    function retrieveOktaToken(event) {
        event.preventDefault();
    
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
                $("#okta-token-status-message").html("Successfully retrieved okta token").addClass("success-status").show();
                $("#export").show();
            },
            error: function(data) {
                $("#okta-token-status-message").html("Failed to retrieve okta token. Please try again.").addClass("failure-status").show();
            }
        });
    }
    
    function startExport(event) {
        event.preventDefault();
        
        const contractNumber = $("#contractNumber").val();
        
        let url = '';
        if(contractNumber === undefined || contractNumber === '') {
            url = baseUrl + fhirSegment + 'Patient/$export';
        } else {
            url = baseUrl + fhirSegment + '/Group/' + contractNumber + '/$export';        
        }
        
        $.ajax({
            url: url,
            headers: {
                'Authorization': 'Basic ' + token
            },
            type: 'get',
            success: function(data, status, xhr) {
                const contentLocation = xhr.getResponseHeader('Content-Location');
                $('#export-status-message').html("Bulk export successfully started.").addClass("success-status").show();
                $('#export-button').addClass("disabled"); //TODO remove event handler
                initiateStatusChecks(contentLocation);
            },
            error: function() {
                $('#export-status-message').html("Failed to start bulk export. Please try again").addClass("failure-status").show(); 
            }
        });
    }
    
    function initiateStatusChecks(url) {
        
    }
    
    function cancelExport() {
        $.ajax({
            url: url,
            headers: {
                'Authorization': 'Basic ' + token
            },
            type: 'get',
            success: function(data, status, xhr) {
        });
        
        /Job/{jobUuid}/$status
    }
</script>

<div id="ab2d-easy-section" style="padding: 5px;">
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
        <button class="btn btn-primary" type="submit" onclick="retrieveOktaToken(event);">Get Token</button>
    </form>
    
    <br />
    
    <div class="col-md-6 mb-3" id="okta-token-status-message"></div>
    
    <br />
    
    <form>
        <div class="form-row form-group" id="export">
            <div class="col-md-6 mb-3">
              <label for="clientSecret">Contract Number</label>
              <input type="text" class="form-control" id="contractNumber" placeholder="Contract Number (Optional)" required>
            </div>
            <button class="btn btn-primary" type="submit" id="export-button" onclick="startExport(event);">Start Export</button>
            
            <div id="export-status-message"></div>
        </div>
    </form>
    
    <br />
    
    <div id="export-in-progress">
        <form>
            <button class="btn btn-primary" type="submit" id="cancel-button" onclick="cancelExport(event);">Cancel Export</button>
            <div class="form-row form-group" id="status"></div>
        </form>
    </div>
        
    
</div>