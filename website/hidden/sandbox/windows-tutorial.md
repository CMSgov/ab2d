---
layout: home
title:  "Claims Data to Part D Sponsors API"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Overview
  - Setup Windows Linux Subsystem
  - Authentication and Authorization
  - User the API
ctas:

---

## Overview

1. Note the following: 

    a. The purpose of these instructions is to provide a way for Windows users to interact with the AB2D API using cURL and jq
    
    b. Even though Windows 10 builds 1706 and greater now include cURL by default, other tools like jq are not included
    
    c. One way to easily make use of tools like jq is to use a Unix-like shell (e.g. Windows Linux subsystem, Cygwin, MSYS2, etc.)
    
2. If you currently are not using a Unix-like shell or other method that provides the ability to install jq or are just not sure, jump to the following page: [Setup Windows Linux subsystem](#setup-windows-linux-subsystem)

3. If you already have your machine setup to use both cURL and jq, you can jump to the following page: [Use the API](#use-the-api)

## Setup Windows Linux Subsystem

1. Note that in this example, we will be using the Linux subsystem for Windows 10

2. Select the "Type here to search" text box near the bottom left on your Windows desktop

3. Type the following in the "Type here to search" text box

    <b>```windows features```</b>
    
4. Select <b>Turn Windows features on or off</b> from the leftmost panel

5. Scroll down to "Windows Subsystem for Linux"

6. Check <b>Windows Subsystem for Linux</b>

7. Select <b>OK</b> on the "Windows Features" window

8. Wait for the changes to complete

9. When prompted, select <b>Restart now</b>

10. Select the "Type here to search" text box again

11. Type the following in the "Type here to search" text box

    <b>```microsoft store```</b>
    
12. Select <b>Microsoft Store</b> from the leftmost panel

13. Select <b>Search</b> on the "Microsoft Store" page

14. Type the following in the "Search" text box

    <b>```linux```</b>

15. Select <b>Run Linux on Windows</b>

16. Select <b>Ubuntu</b>

17. Select <b>Get</b>

18. Select <b>No, thanks</b> on the "Use across your devices" dialog

19. Note that even though you see an "Install" button, it should start installing automatically after a few moments

20. Wait for the installation to complete

21. Select <b>Launch</b>

22. Note that an "Ubuntu" window appears and says "Installing, this may take a few minutes..."

23. Wait for the installation to complete

24. When prompted, enter your desired username at the "Enter new UNIX username" prompt

25. When prompted, enter your desired password at the "Enter new UNIX password" prompt

26. Note that you should see "Installation successful" within the output

27. Note that you will now have a prompt that looks something like this

    <b>```username@machinename:~$```</b>
    
28. Note the following:

    a. You will be entering commands at the dollar sign prompt
    
    b. The easiest way to do this is to use copy and paste
    
    c. After you have copied a command from this document to your internal clipboard, you can paste it simply by right clicking after the dollar sign prompt within the Ubuntu shell
    
    d. The copied text should automatically appear after the dollar sign when you right click (this includes multi-line commands)
    
29. Update the Ubuntu system by entering the following at the dollar sign prompt

    <b>```sudo apt-get update -y```</b>
    
30. Install jq by entering the following at the dollar sign prompt

    <b>```sudo apt-get install -y jq```</b>
    
31. When prompted, enter your UNIX password

32. Wait for the installation to complete

33. Verify that jq is installed by checking its version

    <b>```jq --version```</b>
    
34. Close the Ubuntu window

35. Close the Microsoft Store window

36. Note that you are now ready to use the AB2D API

37. Jump to the following section

## Use the API

Table of Contents
- Download the "geteob" shell script
- Get all patient data
- Get data for a specific contract number

### Download the "geteob" shell script
1. Note that these directions refer to the Ubuntu Windows Linux File System, but you can also use other Unix-like applications to do the same steps

1. Select the "Type here to search" text box near the bottom left on your Windows desktop

1. Type the following in the "Type here to search" text box

    <b>```ubuntu```</b>
    
1. Select <b>Ubuntu</b> from the leftmost panel

1. Note that an "Ubuntu" window appears

1. Note the following

    a. You will be entering commands at the dollar sign prompt
    
    b. The easiest way to do this is to use copy and paste
    
    c. After you have copied a command from this document to your internal clipboard, you can paste it simply by right clicking after the dollar sign prompt within the Ubuntu shell
    
    d. The copied text should automatically appear after the dollar sign when you right click (this includes multi-line commands)
    
1. Get the "geteob.sh" script from GitHub by entering the following at the dollar sign prompt

    <b>```curl -O https://raw.githubusercontent.com/CMSgov/ab2d/master/Deploy/bash/geteob.sh```</b>
    
1. Set permissions on the file by entering the following at the dollar sign prompt

    <b>```sudo chmod 755 geteob.sh```</b>
    
1. When prompted, enter your UNIX password

1. After you have completed this section, you have all you need to make use of the API in the sections below

### Get all patient data

1. Run the script to get the patient data

    ```
    ./geteob.sh \
      http://ab2d-sbx-sandbox-200688312.us-east-1.elb.amazonaws.com/api/v1/fhir \
      MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw==
    ```
      
2. Wait for the process to complete

3. Note that the following file has been created

    <b>S0000_0001.ndjson</b>
    
4. Open the file using notepad by entering the following at the dollar sign prompt

    <b>```notepad.exe ./S0000_0001.ndjson```</b>
    
### Get data for a specific contract number

1. Note that the next step will use contract number "S0000" for the sake of example

2. Run the script to get the patient data for a specific contract number by entering the following at the dollar sign prompt

    ```
    ./geteob.sh \
      http://ab2d-sbx-sandbox-200688312.us-east-1.elb.amazonaws.com/api/v1/fhir \
      MG9hMnQwbHNyZFp3NXVXUngyOTc6SEhkdVdHNkxvZ0l2RElRdVdncDNabG85T1lNVmFsVHRINU9CY3VIdw== \
      S0000
    ```

3. Wait for the process to complete

4. Note that the following file has been created

    <b>S0000_0001.ndjson</b>
    
5. Open the file using notepad by entering the following at the dollar sign prompt

    <b>```notepad.exe ./S0000_0001.ndjson```</b>
 