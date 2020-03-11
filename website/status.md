---
layout: home
title:  "Maintenance Mode"
date:   2019-11-02 09:21:12 -0500 
description: Display the status of whether or not the AB2D bulk status API is in maintenance mode or not.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"

ctas:

---

<script type="text/javascript" src="https://code.jquery.com/jquery-3.4.1.min.js"></script>
<script>
    var $j = jQuery.noConflict();
    function pollServer() {
        $j("#status-content").html("<img src='assets/img/spinner.gif' />");
        $j.get({
            url: 'http://127.0.0.1:8080/status',
            success: function(data) {
                if(data.maintenanceMode === 'true') {
                    $j('#status-content').html("The system is operating normally");                     
                } else {
                    $j('#status-content').html("The system is currently in maintenance mode. Please check back later");
                }
            },
            failure: function() {
                $j('#status-content').html("Error occurred retrieving status from the system"); 
            },
            complete: function() {
                setTimeout(pollServer, 10000);
            }
        });
    }
   
    $j(document).ready(function() {
        pollServer();
    });
</script>

<div id="status-content"></div>