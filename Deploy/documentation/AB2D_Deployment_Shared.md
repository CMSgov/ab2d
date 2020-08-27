# AB2D Deployment Shared

## Table of Contents

1. [Configure VictorOps](#configure-victorops)
   * [Request access to VictorOps](#request-access-to-victorops)
   * [Bookmark important VictorOps URLs](#bookmark-important-victorops-urls)
   * [Add users to the VictorOps account](#add-users-to-the-victorops-account)
   * [Add users to the AB2D team](#add-users-to-the-ab2d-team)
   * [Set up paging policy for a user](#set-up-paging-policy-for-a-user)
   * [Install the VictorOps mobile app](#install-the-victorops-mobile-app)
   * [Create on-call rotations and add relevant team members](#create-on-call-rotations-and-add-relevant-team-members)
   * [Create an escalation policy](#create-an-escalation-policy)
   * [Set the default routing policy](#set-the-default-routing-policy)
   * [Configure New Relic integration](#configure-new-relic-integration)

## Configure VictorOps

### Request access to VictorOps

1. Request access to the following VictorOps

   > https://portal.victorops.com/client/cms-ccxp

1. If you have never been added to any VictorOps account before, click the link in the email that you receive to create a username and password for VictorOps

### Bookmark important VictorOps URLs

1. Open Chrome

1. Add the following bookmarks for VictorOps

   *VictorOps - AB2D on CCXP:*

   > https://portal.victorops.com/client/cms-ccxp

   *VictorOps - Blog:*

   > https://victorops.com/blog/

   *VictorOps - Knowledge Base:*

   > https://help.victorops.com

   *VictorOps - Support:*

   > https://victorops.com/contact/

### Add users to the VictorOps account

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on to VictorOps (if not already logged in)

1. Select the **Users** tab

1. Select **Invite User**

1. Enter a comma-separated list of the emails of users to add in the **Enter and email address** text box

1. Select **Add User**

### Add users to the AB2D team

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on to VictorOps (if not already logged in)

1. If you are setting up VictorOps, verify that you are a "Global Admin" by doing the following

   1. Select the **Users** tab

   1. Scroll to your user

   1. Verify that your role is "Global Admin"

1. Select the **Teams** tab

1. Select **AB2D**

1. Select **Invite User**

1. Add users

   1. Type a user in the "Enter usernames" text box

   1. Select the desired user from the search results

   1. Repeat adding all desired users

1. Select **Add User**

1. Select **Close** on the "Success" window

### Set up paging policy for a user

1. Note the following is an example of a three step paging policy

   - **Step 1:**

     ```
     Immediately...
     - Send an SMS to {mobile phone number}
     - Execute the next step if I have not responded within 5 minutes
     ```

   - **Step 2:**

     ```
     Then...
     - Send an SMS to {mobile phone number}
     - Execute the next step if I have not responded within 5 minutes
     ```

   - **Step 3:**

     ```
     Finally...
     - Every 5 minutes until we have reached you
     - Make a phone call to {mobile phone number}
     ```

1. If a user wants to change his "Primary Paging Policy", he does the following

   1. Log on to VictorOps

   1. Select the username in the top right of the page

   1. Select **Profile**

   1. Configure desired primary paging policy

   1. Select **Save**

1. If a global admin wants to set the "Primary Paging Policy" for someone else, he does the following

   1. Log on to VictorOps

   1. Select the **User** tab

   1. Configure desired primary paging policy

   1. Select **Save**

### Install the VictorOps mobile app

> *** TO DO ***: Determine if we want to use this.

### Create on-call rotations and add relevant team members

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Select the **Teams** tab

1. Select the following team

   ```
   AB2D
   ```

1. Select the **Rotations** tab

1. Add an "On Call" rotation

   1. Select **Add Rotation**

   1. Type the following in the **Rotation name** text box

      ```
      On Call
      ```

   1. Select the following under "Add a shift to this rotation"

      ```
      24/7
      ```

   1. Configure the "On Call" rotation as follows

      - **Shift name:** Standard Rotation

      - **Time Zone:** America/New_York

      - **Handoff happens every:** 7

      - **days at:** 11:00 am

      - **The next handoff happens:** {tomorrow's date}

   1. Select **Save Rotation**

   1. Select a user to add to the rotation from the "Select a user to add" dropdown

   1. Repeat the last step to add additional users

      *Example:*

      - Adam (shift schedule 1)

      - Barry (shift schedule 2)

      - Carl (shift schedule 3)

1. Add an "On Escalation" rotation

   1. Select **Add Rotation**
   
   1. Type the following in the **Rotation name** text box
   
      ```
      On Escalation
      ```
   
   1. Select the following under "Add a shift to this rotation"
   
      ```
      24/7
      ```
   
   1. Configure the "On Escalation" rotation as follows

      - **Shift name:** Standard Shift

      - **Time Zone:** America/New_York

      - **Handoff happens every:** 7

      - **days at:** 11:00 am

      - **The next handoff happens:** {tomorrow's date}

   1. Select **Save Rotation**

   1. Select a user to add to the rotation from the "Select a user to add" dropdown

   1. Repeat the last step to add additional users

      *If using the same user, make sure the users are ordered so that the same user is not in the same shift schedule of the two rotations.*

      - Barry (shift schedule 1)

      - Carl (shift schedule 2)

      - Adam (shift schedule 3)

### Create an escalation policy

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Select the **Teams** tab

1. Select the following team

   ```
   AB2D
   ```

1. Select the **Escalation Policies** tab

1. Select **Add Escalation Policy**

1. Type the following in the **Policy Name** text box

   ```
   Standard
   ```

1. Check **Ignore Custom Paging Policies**

1. Configure "Step 1" as follows

   - **First dropdown:** Immediately

   - **Second dropdown:** Notify the on-duty user(s) in rotation

   - **Third dropdown:** On Call

1. Select **Add Step**

1. Configure "Step 2" as follows

   - **First dropdown:** If still unacked after 30 more minutes,

   - **Second dropdown:** Notify the on-duty user(s) in rotation

   - **Third dropdown:** On Escalation

1. Select **Add Step**

1. Configure "Step 3" as follows

   - **First dropdown:** If still unacked after 60 more minutes,

   - **Second dropdown:** Notify user

   - **Third dropdown:** {devops engineer}

1. Select **Save**

### Set the default routing policy

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Select the **Settings** tab

1. Note that CCXP owns the "Default Routing Policy", so we will not be making any changes to this

### Configure New Relic integration

1. Note that New Relic APM, Infrastructure, and Synthetics alerts can be forwarded to the VictorOps alerting service

1. Log on to VictorOps

1. Select the **Integrations** tab

1. Type the following in the **Search** text box

   ```
   new relic
   ```

1. If New Relic does not display "enabled", do the following

   1. Select **New Relic**

   1. Select **Enable Integration**

1. Select the **Integrations** tab

1. Type the following in the **Search** text box again

   ```
   new relic
   ```

1. Verify that the following is displayed

   ```
   New Relic
   APM
   enabled
   ```

1. Select **New Relic**

1. Copy and save the following information for configuration of all AWS account environments

   *New Relic Alerts:*

   ```
   {victor ops api key for new relic}
   ```
