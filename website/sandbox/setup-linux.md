---
layout: home
title:  "AB2D API Tutorial - Setup Linux"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Setup Linux
ctas:

---
# Setup Linux

These instructions were written for CentOS/RedHat Linux.

1. Install jq

    ```sudo yum install -y jq```

2. Verify the jq installation by checking the version of jq

    ```jq --version```

You are now ready to use the AB2D API. Jump to the [cUrl](tutorial-curl.html) tutorial.
