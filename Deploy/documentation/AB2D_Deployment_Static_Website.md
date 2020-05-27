# AB2D Deployment Static Website

## Table of Contents

1. [Obtain and import ab2d.cms.gov certificate](#obtain-and-import-ab2dcmsgov-certificate)
   * [Download the AB2D domain certificates and get private key from CMS](#download-the-ab2d-domain-certificates-and-get-private-key-from-cms)
   * [Import the AB2D domain certificate into certificate manager](#import-the-ab2d-domain-certificate-into-certificate-manager)
1. [Create initial AB2D static website](#create-initial-ab2d-static-website)

## Obtain and import ab2d.cms.gov certificate

### Download the AB2D domain certificates and get private key from CMS

1. Note that CMS will request a domain certificate from Entrust for the following domain

   ```
   ab2d.cms.gov
   ```

1. Wait for CMS to provide the following

   - private key used to make the domain certificate request

   - forwarded email from Entrust to download the certificate

1. Select the link under "Use the following URL to pick up and install your certificate" in the forwarded Entrust email

1. Select "Other" from the "...server type" dropdown

1. Select **Next** on the "Select Server Type" page

1. Select **Download Certificates**

1. Wait for the download to complete

1. Note the following file has been downloaded

   ```
   entrust.zip
   ```

1. Open a terminal

1. Change to the downloads directory

   ```ShellSession
   $ cd ~/Downloads
   ```

1. Remove existing files

   ```ShellSession
   $ rm -f Intermediate.crt
   $ rm -f Root.crt
   $ rm -f ServerCertificate.crt
   $ rm -f ab2d_cms_gov.key
   ```
   
1. Unzip "entrust.zip"

   ```ShellSession
   $ 7z x entrust.zip
   ```

1. Save the private key that you got from CMS to the "~/Downloads" directory

   ```
   ab2d_cms_gov.key
   ```

1. Save the "entrust.zip" file in 1Password

   **1Password label:** ab2d.cms.gov certificate chain (entrust.zip)

1. Save the "ab2d_cms_gov.key" in 1Password

   **1Password label:** ab2d.cms.gov private key (ab2d_cms_gov.key)

### Import the AB2D domain certificate into certificate manager

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. Select **Get Started** under "Provision certificates"

1. Select **Import a certificate**

1. Open a terminal

1. Copy the contents of "ServerCertificate.crt" to the clipboard

   ```ShellSession
   $ cat ~/Downloads/ServerCertificate.crt | pbcopy
   ```

1. Return to the "Import a Certificate" page in Chrome

1. Paste the contents of the "ServerCertificate.crt" into the **Certificate body** text box

1. Copy the contents of the private key to the clipboard

   ```ShellSession
   $ cat ~/Downloads/ab2d_cms_gov.key | pbcopy
   ```
   
1. Paste the contents of the the private key that was provided separately by CMS into the **Certificate private key** text box

1. Return to the terminal

1. Copy the certificate key chain (Intermediate.crt + Root.crt) to the clipboard

   ```ShellSession
   $ echo -ne "$(cat ~/Downloads/Intermediate.crt)\n$(cat ~/Downloads/Root.crt)" | pbcopy
   ```

1. Paste the combined intermediate and root certificates into the **Certificate chain** text box

1. Select **Next** on the "Import certificate" page

1. Select **Review and import**

1. Note that the following information should be displayed

   *Format:*

   **Domains:** ab2d.cms.gov

   **Expires in:** {number} Days

   **Public key info:** RSA-2048

   **Signature algorithm:** SHA256WITHRSA
   
1. Select **Import**

## Create initial AB2D static website

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create static website

   ```ShellSession
   $ ./bash/create-or-update-website.sh
   ```

1. Choose desired AWS account

   *Example for "Prod" environment:*

   ```
   2
   ```

1. If a "Has this version of the website been approved for deployment to production" question appears, enter the following on the keyboard

   ```
   y
   ```

1. If an "Are you sure" question appears, enter the following on the keyboard

   ```
   y
   ```
