---
layout: tutorial_layout
title:  "Setup Mac"
date:   2019-11-02 09:21:12 -0500 
description: Setup Mac
landing-page: live
---
# Setup Mac

Setup Mac for API use

<i>Note that these instructions assume that you have HomeBrew installed. If you don't use HomeBrew, go 
[here](https://stedolan.github.io/jq/download/) for other ways to install jq</i>.
1. Install or update jq using HomeBrew

    ```brew install jq```
    
    <i>Note that if you already have the latest version of jq, you will see the following</i>
    
    ```Warning: jq {version} is already installed and up-to-date```
    
1. Verify the jq installation by checking the version of jq

    ```jq --version```

You are now ready to use the AB2D API. Jump to the [cUrl](tutorial-curl.html) tutorial.