---
layout: home
title:  "AB2D API Tutorial - Setup Windows"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Setup Windows 10
ctas:

---
# Setup Windows 10
In this example, we will be using the Linux subsystem for Windows 10.

1. Select the "Type here to search" text box near the bottom left on your Windows desktop
2. Type the following in the "Type here to search" text box

    ```windows features```

3. Select <b>Turn Windows</b> features on or off from the leftmost panel
4. Scroll down to "Windows Subsystem for Linux"
5. Check <b>Windows Subsystem for Linux</b>
6. Select OK on the "Windows Features" window
7. Wait for the changes to complete
8. When prompted, select <b>Restart now</b>
9. Select the "Type here to search" text box again
10. Type the following in the "Type here to search" text box

    ```microsoft store```

11. Select <b>Microsoft Store</b> from the leftmost panel
12. Select <b>Search</b> on the "Microsoft Store" page
13. Type the following in the "Search" text box

    ```linux```

14. Select <b>Run Linux on Windows</b>
15. Select <b>Ubuntu</b>
16. Select <b>Get</b>
17. Select <b>No, thanks</b> on the "Use across your devices" dialog
18. Even though you see an "Install" button, it should start installing automatically after a few moments
19. Wait for the installation to complete
20. Select <b>Launch</b>
21. An "Ubuntu" window appears and says "Installing, this may take a few minutes..."
22. Wait for the installation to complete
23. When prompted, enter your desired username at the "Enter new UNIX username" prompt
24. When prompted, enter your desired password at the "Enter new UNIX password" prompt
25. You should see "Installation successful" within the output
26. You will now have a prompt that looks something like this

    ```username@machinename:~$```

    <i>You will be entering commands at the dollar sign prompt. The easiest way to do this is to use copy and paste.
    After you have copied a command from this document to your internal clipboard, you can paste it simply by right 
    clicking after the dollar sign prompt within the Ubuntu shell. The copied text should automatically appear after 
    the dollar sign when you right click (this includes multi-line commands).</i>
    
27. Update the Ubuntu system by entering the following at the dollar sign prompt

    ```sudo apt-get update -y```

28. Install jq by entering the following at the dollar sign prompt

    ```sudo apt-get install -y jq```
    
29. When prompted, enter your UNIX password
30. Wait for the installation to complete
31. Verify that jq is installed by checking its version

    ```jq --version```
    
32. Close the Ubuntu window
33. Close the Microsoft Store window

### Open the Ubuntu Windows Linux File System terminal
These directions refer to the Ubuntu Windows Linux File System, but you can also use other Unix-like applications to do the same steps.
When you are instructed to "Open a terminal" in the Use cUrl instructions, you will do the following steps:

<i>NOTE: Make sure that you are comfortable with these steps before proceeding to the [cUrl](tutorial-curl.html) tutorial</i>

1. Select the "Type here to search" text box near the bottom left on your Windows desktop
2. Type the following in the "Type here to search" text box

    ```ubuntu```
    
3. Select <b>Ubuntu</b> from the leftmost panel and an "Ubuntu" window will appears

    Note the following:
    - You will be entering commands at the dollar sign prompt
    - The easiest way to do this is to use copy and paste
    - After you have copied a command from this document to your internal clipboard, you can paste it simply by right 
    clicking after the dollar sign prompt within the Ubuntu shell
    - The copied text should automatically appear after the dollar sign when you right click (this includes multi-line commands)
    
You are now ready to use the AB2D API. Jump to the [cUrl](tutorial-curl.html) tutorial.