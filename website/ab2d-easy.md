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

<script src="https://ok1static.oktacdn.com/assets/js/sdk/okta-auth-js/2.0.1/okta-auth-js.min.js" type="text/javascript"></script>

<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js" integrity="sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js" integrity="sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k" crossorigin="anonymous"></script>

<style type="text/css">
    #okta-token-status-message {
        display: 'hidden'
    }
</style>

<script>
    function retrieveOktaToken(event) {
        event.preventDefault();
    
        const clientId = $('#clientID').val();
        const clientSecret = $('#clientSecret').val();
        const authorization = btoa(clientId + ':' + clientSecret);
        console.log("Auth: " + authorization); 
        $.ajax({
            url: 'https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token?grant_type=client_credentials&scope=clientCreds',
            dataType: 'json',
            type: 'post',
            contentType: 'application/json',
            headers: {
                'Authorization': 'Basic ' + authorization,
                'Content-Type': 'application/x-www-form-urlencoded',
                'Origin': 'ab2d-easy'
            },
            success: function (data) {
                console.log(data);
            },
            error: function(data) {
                console.log(data);
            }
        });
    }
    
    /*let baseUrl = 'https://sandbox.ab2d.cms.gov/';
    
    
    function pollServer() {
        $.get(baseUrl + 'status', function(data) {
            if(data.maintenanceMode === 'false') {
                $('#status-content').html("<img style=\"vertical-align: middle;\" src='assets/img/status-up.png' /> <span>The system is operating normally</span>");                     
            } else {
                $('#status-content').html("<img style=\"vertical-align: middle;\" src='assets/img/status-down.png' /> <span>The system is currently in maintenance mode. Please check back later.</span>");
            }
        })
        .fail(function() {
            $('#status-content').html("<img style=\"vertical-align: middle;\" src='assets/img/status-down.png' /> <span>The system is currently unreachable. Please check back later.</span>"); 
        })
        .always(function() {
            setTimeout(pollServer, 10000);
        });
    }
   
    $(document).ready(function() {
        pollServer();
    });*/
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
    
    <div id="okta-token-status-message"></div>
</div>