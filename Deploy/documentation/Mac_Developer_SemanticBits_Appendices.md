# Mac Developer SemanticBits Appendices

## Table of Contents

1. [Appendix A: PostgreSQL 11](#appendix-a-postgresql-11)
   * [Install PostgreSQL 11](#install-postgresql-11)
   * [Uninstall PostgreSQL 11](#uninstall-postgresql-11)
1. [Appendix B: CMS VPN access](#appendix-b-cms-vpn-access)
   * [Create a Jira ticket for CMS VPN ticket access](#create-a-jira-ticket-for-cms-vpn-ticket-access)
   * [Install and configure Google Authenticator](#install-and-configure-google-authenticator)
   * [Install and configure Cisco AnyConnect VPN](#install-and-configure-cisco-anyconnect-vpn)
   * [Log into Cisco AnyConnect client](#log-into-cisco-anyconnect-client)
   * [Disconnect from the Cisco AnyConnect client](#disconnect-from-the-cisco-anyconnect-client)
1. [Appendix C: Install 1Password API](#appendix-c-install-1password-api)
1. [Appendix D: Troubleshoot VPN access](#appendix-d-troubleshoot-vpn-access)
1. [Appendix E: Configure show file extensions in Finder](#appendix-e-configure-show-file-extensions-in-finder)

## Appendix A: PostgreSQL 11

### Install PostgreSQL 11

1. Install PostgreSQL 11

   ```ShellSession
   $ brew install postgresql@11
   ```

1. Note the following caveats

   ```
   ==> Caveats
   To migrate existing data from a previous major version of PostgreSQL run:
     brew postgresql-upgrade-database

   To have launchd start postgresql now and restart at login:
     brew services start postgresql
   Or, if you don't want/need a background service you can just run:
     pg_ctl -D /usr/local/var/postgres start
   ```

1. Start PostgreSQL service

   ```ShellSession
   $ brew services start postgresql
   ```

1. Ensure that PostgreSQL is running on port 5432

   ```ShellSession
   $ netstat -an | grep 5432
   ```
   
### Uninstall PostgreSQL 11

1. Stop PostgreSQL service

   ```ShellSession
   $ brew services stop postgresql
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ brew uninstall postgresql@11
   ```

1. Ensure that PostgreSQL is no longer running on port 5432

   ```ShellSession
   $ netstat -an | grep 5432
   ```

## Appendix B: CMS VPN access

### Create a Jira ticket for CMS VPN ticket access

1. Open Chrome

1. Enter the following in the address bar

   > https://jiraent.cms.gov/servicedesk/customer/portal/13

1. Select **CMS Cloud Access Request**

1. Fill out the form as follows

   *Format:*
   
   **Summary:** AWS VPN access for {your eua id}

   **Project Name:** Project 058 BCDA

   **Account Alias:** None

   **Types of Access/Resets:** Cisco AnyConnect Access

   **Approvers:** Andrew Harnish

   **Description:**

   ```
   I'm an engineer working on the AB2D project at CMS. Can I have AWS VPN access?

   User ID: {your eua id}

   Cellphone: {your cellphone number}

   Full name: {your first and last name}

   Email: {your semanticbits email}

   Thanks,

   {your first name}
   ```

   **Severity:** Minimal

   **Urgency:** Medium

   **Reported Source:** Self Service

   **Requested Due Date:** {3 business days from today's date}

1. Select **Create**

1. Verify that you receive an email regarding the issue

### Install and configure Google Authenticator

1. Before you begin this section, verify that you received a "Google authenticator URL for Cisco AnyConnect VPN" email from "CMSCloudOperations@cms.hhs.gov"

1. Note the secret key that was provided in the email (you will need this in a later step)

1. Install the Google Authenticator application on your mobile phone

1. Open Google Authenticator on your mobile phone

1. Select **BEGIN SETUP**

1. Select **Manaul entry**

1. Configure the "Manual entry" page as follows

   - **Account:** cloudvpn.cms.gov

   - **Key:** {secret key from email}

1. Select the checkmark icon

1. Note that the following is now displayed

   ```
   XXX XXX (where the X's are numbers that change every 30 seconds)
   cloudvpn.cms.gov
   ```

### Install and configure Cisco AnyConnect VPN

1. Before you begin this section, verify that you received a text that looks similar to this

   *Format:*
   
   ```
   Your new password for Cloud AD used to access CloudTamer and Cloud VPN:

   {password}
   ```

1. Note the password that you received in the text (you will need it in a later step)

1. Open Chrome

1. Enter the following in the address bar

   > https://cloudvpn.cms.gov

1. Complete the Login as follows

   - **USERNAME:** {your eua id}

   - **PASSWORD:** {password that you received in text}

   - **2nd Password:** {latest 6 number from the google authentication app on mobile phone}

1. Select **Continue** on the "WARNING" page

1. Select the **AnyConnect VPN** link

1. Wait for the download to complete

1. Open a new terminal

1. Enter the following at the prompt

   ```ShellSession
   $ open ~/Downloads/anyconnect-macos-*.dmg
   ```

1. Double click on the following in the "AnyConnect VPN..." window

   ```
   anyconnect-macos-{version}.pkg
   ```

1. Select **Continue** on the "Introduction" window

1. Select **Continue** on the "License" window

1. Select **Agree**

1. Select **Install**

1. Enter your password or PIN for your Mac (depending on how your logged into your Mac)

1. Select **Install Password**

1. If a "manifesttool is not optimized for your Mac and needs to be updated" message appears, do the following, select **OK**

1. Select **Close** on the "Summary" page

1. Select **Move to Trash**

### Log into Cisco AnyConnect client

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

   - **PASSWORD:** {password that you received in text}

   - **2nd Password:** {latest 6 number from the google authentication app on mobile phone}

1. Select **OK**

1. Select **Accept**

1. If a "vpndownloader is not optimized for your Mac and needs to be updated" message appears, do the following:

   1. Select **OK**

   1. See the following for more information

      > https://support.apple.com/en-us/HT208436

1. Verify that the VPN successfully connects

### Disconnect from the Cisco AnyConnect client

1. Select the following in the system bar

   ![Cisco AnyConnect](images/cisco-anyconnect.png)

1. Select **Disconnect**

1. Select the **Cisco AnyConnect Secure Mobility Client** menu

1. Select **Quit Cisco AnyConnect**

## Appendix C: Install 1Password API

1. Install the 1Password API

   ```ShellSession
   $ brew cask install 1password-cli
   ```

1. Wait for the installation to complete

   *Note that this takes a while.*

1. Verify the 1Password API by checking its version

   ```ShellSession
   $ op --version
   ```

## Appendix D: Troubleshoot VPN access

1. If you get a "The VPN client driver encountered an error. Please restart your computer or device, then try again." message, try the following:

   1. Reboot your machine

   1. Try reconnecting to the Cisco VPN client again

1. If you continute to get a "The VPN client driver encountered an error. Please restart your computer or device, then try again." message, try the following:

   1. Open Chrome

   1. Enter the following in the address bar

      > https://confluence.cms.gov/pages/viewpage.action?spaceKey=AWSOC&title=Cisco+AnyConnect+Client


   1. Download and install the latest VPN client

   1. Reboot your machine

   1. Try reconnecting to the Cisco VPN client again

1. If you continute to get a "The VPN client driver encountered an error. Please restart your computer or device, then try again." message, try the following:

   1. Open a terminal

   1. Enter the following at the prompt

      ```ShellSession
      $ sudo launchctl unload /Library/LaunchDaemons/com.vpn.semanticbits.plist
      ```

   1. Try reconnecting to the Cisco VPN client again

## Appendix E: Configure show file extensions in Finder

1. Open Finder

1. Select the **Finder** menu

1. Select **Preferences**

1. Select the **Advanced** tab

1. Check **Show all filename extensions**

1. CLose the "Finder Preferences" window
