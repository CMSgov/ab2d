# AB2D Deployment Appendices

## Table of Contents

1. [Appendix A: Access the CMS AWS console](#appendix-a-access-the-cms-aws-console)

## Appendix A: Access the CMS AWS console

1. Log into Cisco AnyConnect client

   1. Select **Launchpad**
   
   1. Select **Cisco AnyConnect Secure Mobility Client**
   
   1. If a "Cisco AnyConnect Secure Mobility Client is not optimized for your Mac and needs to be updated" message appears, do the following:
      
      1. Select **OK**
   
      1. See the following for more information
   
         > https://support.apple.com/en-us/HT208436
   
   1. Enter the following in the **VPN** dropdown
   
      ```
      cloudvpn.cms.gov
      ```
   
   1. Select **Connect**
   
   1. Enter the following on the "Please enter your username and password" window
   
      - **USERNAME:** {your eua id}
   
      - **PASSWORD:** {password for CloudTamer and Cloud VPN}
   
      - **2nd Password:** {latest 6 number from the google authentication app on mobile phone}
   
   1. Select **OK**
   
   1. Select **Accept**
   
   1. If a "vpndownloader is not optimized for your Mac and needs to be updated" message appears, do the following:
      
      1. Select **OK**
   
      1. See the following for more information
   
         > https://support.apple.com/en-us/HT208436
   
   1. Verify that the VPN successfully connects

1. Log on to CloudTamer

   1. Open Chrome

   1. Enter the following in the address bar

      > https://cloudtamer.cms.gov

   1. Enter the following on the CloudTamer logon page

      - **Username:** {your eua id}

      - **Password:** {password for CloudTamer and Cloud VPN}

      - **Dropdown:** CMS Cloud Services

   1. Select **Log In**

1. Select **Projects** from the leftmost panel

1. Select **Filters**

1. Type the following in the **By Keyword** text box

   ab2d

1. Select **Apply Filters**

1. Select **Account Access** beside "Project AB2D"

1. Select the account
   
   ```
   aws-cms-oit-iusg-acct66 (349849222861)
   ```

1. Select the cloud access role

   
   ```
   West-AB2D
   ```

1. Select **Web Access**

1. Note that the AWS console for the AB2D project should now appear