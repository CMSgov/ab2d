# AB2D Deployment (Greenfield)

## Table of Contents

1. [Create an Akamai Upload Account in Akamai NetStorage](#create-an-akamai-upload-account-in-akamai-netstorage)
1. [Initialize or verify greenfield environment](#initialize-or-verify-greenfield-environment)

## Create an Akamai Upload Account in Akamai NetStorage

1. Open Chrome

1. Enter the following in the address bar

   > https://control.akamai.com/apps/home-page

1. Select the three bar icon in the top left of the page

1. Select **NetStorage** under "CDN" in the leftmost panel

1. Select the following storage group

   ```
   AB2D
   ```

1. Scroll down to the "Upload Directories" section

1. Note the upload directories

   - 971498 (used for static website stage)

   - 1055691 (used for static website prod)

1. Create an "ab2d-akamai-stage" keypair

   ```ShellSession
   $ ssh-keygen -t rsa -b 2048 -P "" -q -f "$HOME/.ssh/ab2d-akamai"
   ```

1. Select the ![Akamai Upload Accounts](images/akamai-upload-accounts.png) icon from leftmost toolbar

1. Select **Add Upload Account**

1. Configure the "Upload Account" page as follows

   - **Id:** lhanekamp_ab2d

   - **Contact Email Address:** lonnie.hanekamp@semanticbits.com

   - **Add New Account radio button:** {selected}

   - **First Name:** Lonnie

   - **Last Name:** Hanekamp

   - **Email:** {secondary email}

   - **Phone:** {cell phone}

1. Select **Next** on the "Upload Account" page

1. Check the following

   - 971498 (AB2D)

   - 1055691 (AB2D)

1. Select **Yes** on the "Are you sure you want to add the selected Directory Access to the upload account?" dialog

1. Keep other defaults

1. Select **Next** on the "Upload Directory Association" page

1. Select **Add SSH Key**

1. Open a terminal

1. Copy the contents of "ab2d-akamai.pub" to the clipboard

   ```ShellSession
   $ cat ~/.ssh/ab2d-akamai.pub | pbcopy
   ```

1. Paste the public key into the **SSH Key** text box

1. Select **Save**

1. Select **Next** on the "Access Methods" page

1. Select **Next** on the "Advanced Settings" page

1. Note the "Upload Account Details"

   - **Id:** lhanekamp_ab2d

   - **Email:** lonnie.hanekamp@semanticbits.com

   - **Storage Group Id:** 1065622

   - **CP Codes:** 971498 1055691

   - **First Name:** Lonnie

   - **Last Name:** Hanekamp

   - **Email:** {seconary email}

   - **Phone:** {cell phone}

   - **FTP:** Disabled

   - **Rsync:** Disabled

   - **SSH:** Enabled

   - **Aspera Upload Acceleration:** Disabled

   - **HTTP API Access:** Disabled

   - **File Manager Access:** Disabled

   - **Upload Directory:** 971498 (AB2D)

   - **Upload Directory:** 971498 (AB2D)

   - **Directory limit:** {blank}

   - **Default directory:** {blank}

   - **Read Write Privileges:** Read/Write

1. Select **Create**

## Initialize or verify greenfield environment

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Initialize or verify environment

   ```ShellShession
   $ export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && ./Deploy/bash/initialize-greenfield-environment-v2.sh
   ```

1. If prompted, set secrets for the target environment
