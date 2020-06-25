---
layout: home
title:  "System Status"
date:   2019-11-02 09:21:12 -0500 
description: This page gives an overview of the status of our systems.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - name: Home
    link: /
ctas:

---

<script>
    let baseUrl = 'https://sandbox.ab2d.cms.gov/';

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
    });
</script>

<div id="status-section" style="padding: 5px;" role="main">
    <h3>System Status</h3>
    <div style="border-top: 1px solid silver; border-bottom: 1px solid silver; padding: 4px;">
        <span id="status-content" style="padding-left: 10px;"></span>
    </div>
</div>