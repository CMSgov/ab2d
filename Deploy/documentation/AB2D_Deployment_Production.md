# AB2D Deployment Production

## Table of Contents

1. [Obtain and import ab2d.cms.gov certificate](#obtain-and-import-ab2dcmsgov-certificate)
   * [Download the AB2D domain certificates and get private key from CMS](#download-the-ab2d-domain-certificates-and-get-private-key-from-cms)
   * [Import the AB2D domain certificate into certificate manager](#import-the-ab2d-domain-certificate-into-certificate-manager)
1. [Obtain and import dev.ab2d.cms.gov common certificate](#obtain-and-import-devab2dcmsgov-common-certificate)
   * [Download the dev domain certificate and get private key from CMS](#download-the-dev-domain-certificate-and-get-private-key-from-cms)
   * [Import the dev domain certificate into certificate manager](#import-the-dev-domain-certificate-into-certificate-manager)
1. [Obtain and import sandbox.ab2d.cms.gov entrust certificate](#obtain-and-import-sandboxab2dcmsgov-entrust-certificate)
   * [Download the sandbox domain certificates and get private key from CMS](#download-the-sandbox-domain-certificates-and-get-private-key-from-cms)
   * [Import the sandbox domain certificate into certificate manager](#import-the-sandbox-domain-certificate-into-certificate-manager)
1. [Obtain and import impl.ab2d.cms.gov common certificate](#obtain-and-import-implab2dcmsgov-common-certificate)
1. [Obtain and import api.ab2d.cms.gov entrust certificate](#obtain-and-import-apiab2dcmsgov-entrust-certificate)
1. [Create initial AB2D static website](#create-initial-ab2d-static-website)
1. [Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer](#submit-an-internet-dns-change-request-form-to-product-owner-for-the-sandbox-application-load-balancer)
1. [Submit an "Internet DNS Change Request Form" to product owner for the production application load balancer](#Submit an "internet-dns-change-request-form-to-product-owner-for-the-production-application-load-balancer)
1. [Deploy to production](#deploy-to-production)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
   * [Encrypt and upload New Relic configuration file](#encrypt-and-upload-new-relic-configuration-file)
   * [Create, encrypt, and upload BFD AB2D keystore](#create-encrypt-and-upload-bfd-ab2d-keystore)
   * [Create or update AMI with latest gold disk](#create-or-update-ami-with-latest-gold-disk)
   * [Create or update infrastructure](#create-or-update-infrastructure)

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

## Obtain and import dev.ab2d.cms.gov common certificate](#obtain-and-import-devab2dcmsgov-common-certificate)

### Download the dev domain certificate and get private key from CMS

1. Note that CMS will request a common certificate for the following domain

   ```
   dev.ab2d.cms.gov
   ```

1. Ensure that you receive the following information from CMS

   - the Certificate Signing Request (CSR) file (dev_ab2d_cms_gov.csr)

   - the private key file (dev_ab2d_cms_gov.key)

   - reference number

   - authorization Code

   - an email that includes a link under "If you are retrieving a TLS Common Policy (“Web Server Certificate”)"

1. Select the link in the email

1. Configure the page as follows

   - **Reference Number:** {reference number}

   - **Authorization Code:** {authorization code}

   - **Options:** displayed as PEM enoding of certificate in raw DER

1. Select **Choose File**

1. Navigate to the certificate signing request

   ```
   dev_ab2d_cms_gov.csr
   ```

1. Select **Submit**

1. Copy the text of the certificate to the clipboard

1. Paste the text of the certificate to file called this

   ```
   dev_ab2d_cms_gov.crt
   ```

1. Select the **Display CA Certificate** tab

1. Copy the text of the certification authority certificate to the clipboard

1. Paste the text of the certificate to file called this

   ```
   dev_ab2d_cms_gov_certification_authority.crt
   ```

1. Save the following files to 1Password
   
   - dev_ab2d_cms_gov.crt (dev.ab2d.cms.gov certificate)

   - dev_ab2d_cms_gov.csr (dev.ab2d.cms.gov certificate signing request)

   - dev_ab2d_cms_gov.key (dev.ab2d.cms.gov private key)

   - dev_ab2d_cms_gov_certification_authority.crt (dev.ab2d.cms.gov certification authority certificate)

### Import the dev domain certificate into certificate manager

1. Download the following files from 1Password to the "~/Downloads" directory (if not already there)

   - dev_ab2d_cms_gov.crt (dev.ab2d.cms.gov certificate)

   - dev_ab2d_cms_gov.csr (dev.ab2d.cms.gov certificate signing request)

   - dev_ab2d_cms_gov.key (dev.ab2d.cms.gov private key)

   - dev_ab2d_cms_gov_certification_authority.crt (dev.ab2d.cms.gov certification authority certificate)

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. If the "Get Started" page is displayed, select **Get Started** under "Provision certificates"

1. Select **Import a certificate**

1. Open a terminal

1. Copy the contents of "ServerCertificate.crt" to the clipboard

   ```ShellSession
   $ cat ~/Downloads/dev_ab2d_cms_gov.crt | pbcopy
   ```

1. Return to the "Import a Certificate" page in Chrome

1. Paste the contents of the "ServerCertificate.crt" into the **Certificate body** text box

1. Copy the contents of the private key to the clipboard

   ```ShellSession
   $ cat ~/Downloads/dev_ab2d_cms_gov.key | pbcopy
   ```

1. Paste the contents of the the private key that was provided separately by CMS into the **Certificate private key** text box

1. Return to the terminal

1. Copy the contents of the certification authority certificate to the clipboard

   ```ShellSession
   $ cat ~/Downloads/dev_ab2d_cms_gov_certification_authority.crt | pbcopy
   ```

1. Paste the certification authority certificate into the **Certificate chain** text box

1. Select **Next** on the "Import certificate" page

1. Select **Review and import**

1. Note that the following information should be displayed

   *Format:*

   **Domains:** dev.ab2d.cms.gov

   **Expires in:** {number} Days

   **Public key info:** RSA-2048

   **Signature algorithm:** SHA256WITHRSA
   
1. Select **Import**

## Obtain and import sandbox.ab2d.cms.gov entrust certificate](#obtain-and-import-sandboxab2dcmsgov-entrust-certificate)

### Download the sandbox domain certificates and get private key from CMS

1. Note that CMS will request a domain certificate from Digicert for the following domain

   ```
   sandbox.ab2d.cms.gov
   ```

1. Wait for the email from Digicert

1. Wait for CMS to provide the following

   - private key used to make the domain certificate request

1. After receiving the private key save it under "~/Downloads"

   ```
   sandbox_ab2d_cms_gov.key
   ```
   
1. Note the following in the email

   - Key Info - RSA 2048-bit
   
   - Signature Algorithm - SHA-256 with RSA Encryption and SHA-1 root
   
   - Product Name - Digital ID Class 3 - Symantec Global Server OnSite
   
1. Select the "Download your certificate and the intermediate CAs here" link in the Digicert email

1. Scroll down to the bottom of the page

1. Select **Download**

1. Scroll down to the "X.509" section

1. Open a terminal

1. Delete existing "ServerCertificateSandbox.crt" file (if exists)

   ```ShellSession
   $ rm -f ~/Downloads/ServerCertificateSandbox.crt
   ```

1. Open a new "ServerCertificateSandbox.crt" file

   ```ShellSession
   $ vim ~/Downloads/ServerCertificateSandbox.crt
   ```

1. Return to the web page with the "X.509" text block

1. Select all text within the "X.509" text block

1. Copy the "X.509" text block to the clipboard

1. Paste the text into the "ServerCertificateSandbox.crt" file

1. Save and close the file

1. Return to "Download Certificate" page

1. Select the "Install the intermediate CA separately for PKCS #7" link

1. Select the **RSA SHA-2** tab

1. Select **Download** beside "Secure Site" under "Intermediate CA" under the the "SHA-2 Intermediate CAs (under SHA-1 Root)" section

1. Wait for the download to complete

1. Note the following file will appear under "Downloads"

   ```
   DigiCertSHA2SecureServerCA.pem
   ```

1. Save the following files to 1Password
   
   - ServerCertificateSandbox.crt (certificate)

   - sandbox_ab2d_cms_gov.key (private key)

   - DigiCertSHA2SecureServerCA.pem (intermediate certificate)

### Import the sandbox domain certificate into certificate manager

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. Select **Get Started** under "Provision certificates"

1. Select **Import a certificate**

1. Open a terminal

1. Copy the contents of "ServerCertificate.crt" to the clipboard

   ```ShellSession
   $ cat ~/Downloads/ServerCertificateSandbox.crt | pbcopy
   ```

1. Return to the "Import a Certificate" page in Chrome

1. Paste the contents of the "ServerCertificate.crt" into the **Certificate body** text box

1. Copy the contents of the private key to the clipboard

   ```ShellSession
   $ cat ~/Downloads/sandbox_ab2d_cms_gov.key | pbcopy
   ```
   
1. Paste the contents of the the private key that was provided separately by CMS into the **Certificate private key** text box

1. Return to the terminal

1. Copy Intermediate.crt the clipboard

   ```ShellSession
   $ cat DigiCertSHA2SecureServerCA.pem | pbcopy
   ```

1. Paste the intermediate certificate into the **Certificate chain** text box

1. Select **Next** on the "Import certificate" page

1. Select **Review and import**

1. Note that the following information should be displayed

   *Format:*

   **Domains:** ab2d.cms.gov

   **Expires in:** {number} Days

   **Public key info:** RSA-2048

   **Signature algorithm:** SHA256WITHRSA
   
1. Select **Import**

## Obtain and import impl.ab2d.cms.gov common certificate](#obtain-and-import-implab2dcmsgov-common-certificate)

> *** TO DO ***

## Obtain and import api.ab2d.cms.gov entrust certificate](#obtain-and-import-apiab2dcmsgov-entrust-certificate)

> *** TO DO ***

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

## Deploy to sandbox

### Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Initialize or verify environment

   ```ShellShession
   $ ./bash/initialize-environment.sh
   ```
   
1. Enter the number of the AWS account with the environment to initialize or verify

   ```
   2
   ```

### Encrypt and upload New Relic configuration file

1. Open a terminal

1. Copy the New Relic configuration file to the "/tmp" directory

   ```ShellSession
   $ cp yaml/newrelic-infra.yml /tmp
   ```

1. Open the New Relic configuration file

   ```SehllSession
   $ vim /tmp/newrelic-infra.yml
   ```
   
1. Open Chrome

1. Enter the following in the address bar

   > https://rpm.newrelic.com/accounts/2597286

1. Log on to New Relic GUI

1. Select the account dropdown in the upper right of the page

1. Select **Account settings**

1. Copy the "Licence key" to the clipboard

1. Return to the terminal and modify the "newrelic-infra.yml" the following line as follows

   ```
   license_key: {new relic license key}
   ```

1. Save and close the file

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Enter the number of the desired AWS account where the desired logs reside

   ```
   3
   ```

1. Get KMS key ID

   ```ShellSession
   $ KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
     --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
     --output text)
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Encrypt "newrelic-infra.yml" as "newrelic-infra.yml.encrypted"

   ```ShellSession
   $ aws kms --region "${AWS_DEFAULT_REGION}" encrypt \
     --key-id ${KMS_KEY_ID} \
     --plaintext fileb://newrelic-infra.yml \
     --query CiphertextBlob \
     --output text \
     | base64 --decode \
     > newrelic-infra.yml.encrypted
   ```

1. Copy "newrelic-infra.yml.encrypted" to S3

   ```ShellSession
   $ aws s3 --region "${AWS_DEFAULT_REGION}" cp \
     ./newrelic-infra.yml.encrypted \
     "s3://${CMS_ENV}-automation/encrypted-files/"
   ```

1. Get "newrelic-infra.yml.encrypted" from S3

   ```ShellSession
   $ aws s3 --region "${AWS_DEFAULT_REGION}" cp \
     "s3://${CMS_ENV}-automation/encrypted-files/newrelic-infra.yml.encrypted" \
     .
   ```

1. Test decryption of the new relic configuration file

   ```ShellSession
   $ aws kms --region "${AWS_DEFAULT_REGION}" decrypt \
     --ciphertext-blob fileb://newrelic-infra.yml.encrypted \
     --output text --query Plaintext \
     | base64 --decode \
     > /tmp/newrelic-infra.yml
   ```

1. Verify "newrelic-infra.yml" file contents

   ```ShellSession
   $ cat /tmp/newrelic-infra.yml
   ```

### Create, encrypt, and upload BFD AB2D keystore

> *** TO DO ***

### Create or update AMI with latest gold disk

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set gold disk test parameters

   *Example for "Prod" environment:*

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-sbx-sandbox
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge
   $ export OWNER_PARAM=743302140042
   $ export REGION_PARAM=us-east-1
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export VPC_ID_PARAM=vpc-08dbf3fa96684151c
   ```

1. Create or update AMI with latest gold disk

   ```ShellSession
   $ ./bash/update-gold-disk.sh
   ```

### Create or update infrastructure

1. Deploy infrastructure

   *Example for "Prod" environment:*
   
   ```ShellSession
   $ ./deploy-infrastructure.sh \
     --environment=ab2d-sbx-sandbox \
     --ecr-repo-environment=ab2d-mgmt-east-dev \
     --region=us-east-1 \
     --vpc-id=vpc-08dbf3fa96684151c \
     --ssh-username=ec2-user \
     --owner=743302140042 \
     --ec2_instance_type_api=m5.xlarge \
     --ec2_instance_type_worker=m5.xlarge \
     --ec2_instance_type_other=m5.xlarge \
     --ec2_desired_instance_count_api=1 \
     --ec2_minimum_instance_count_api=1 \
     --ec2_maximum_instance_count_api=1 \
     --ec2_desired_instance_count_worker=1 \
     --ec2_minimum_instance_count_worker=1 \
     --ec2_maximum_instance_count_worker=1 \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --internet-facing=false \
     --auto-approve
   ```

### Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer

1. Open Chrome

1. Enter the following in the address bar

   > https://confluence.cms.gov/pages/viewpage.action?pageId=138595233

1. If the Confluence logon page appears, log on to Confluence

1. Note that the "CNAME/DNS Change Requests" page should be displayed

1. Select the **DNS change request form** link under the "Process" section

1. Select the **Download** icon in the top right of the page

1. Wait for the download to complete

1. Open the downloaded form

   ```
   Internet DNS Change Request (2).pdf
   ```

1. Fill out the form as follows

   *Requestor Information:*

   - **Name:** {product owner first name} {product owner last name}

   - **Organization:** {product owner organization}

   - **Email:** {product owner email}

   - **Phone:** {product owner phone}

   *CMS Business Owner Information*

   - **Name:** {business owner first name} {business owner last name}

   - **Organization:** {business owner organization}

   - **Email:** {business owner email}

   - **Phone:** {business owner phone}

   - **Reason:** To support developers that will be integrating with the AB2D API

   *DNS Change Information*

   - **DNS Zone:** cms.gov

   - **Type of change:** CNAME

   - **Actual Change:** sandbox.ab2d.cms.gov CNAME ab2d-sbx-sandbox-{unique id}.us-east-1.elb.amazonaws.com

   - **Change Date & Time:** ASAP

   - **Purpose of the change:** Initial launch of sandbox

1. Submit the completed for to the product owner

1. Note that the product owner will complete the process

## Deploy to production

### Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Initialize or verify environment

   ```ShellShession
   $ ./bash/initialize-environment.sh
   ```
   
1. Enter the number of the AWS account with the environment to initialize or verify

   ```
   4
   ```

### Encrypt and upload New Relic configuration file

1. Open a terminal

1. Copy the New Relic configuration file to the "/tmp" directory

   ```ShellSession
   $ cp yaml/newrelic-infra.yml /tmp
   ```

1. Open the New Relic configuration file

   ```SehllSession
   $ vim /tmp/newrelic-infra.yml
   ```
   
1. Open Chrome

1. Enter the following in the address bar

   > https://rpm.newrelic.com/accounts/2597286

1. Log on to New Relic GUI

1. Select the account dropdown in the upper right of the page

1. Select **Account settings**

1. Copy the "Licence key" to the clipboard

1. Return to the terminal and modify the "newrelic-infra.yml" the following line as follows

   ```
   license_key: {new relic license key}
   ```

1. Save and close the file

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Enter the number of the desired AWS account where the desired logs reside

   *Example for "Prod" environment:*

   ```
   5
   ```

1. Get KMS key ID

   ```ShellSession
   $ KMS_KEY_ID=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
     --query="Aliases[?AliasName=='alias/ab2d-kms'].TargetKeyId" \
     --output text)
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Encrypt "newrelic-infra.yml" as "newrelic-infra.yml.encrypted"

   ```ShellSession
   $ aws kms --region "${AWS_DEFAULT_REGION}" encrypt \
     --key-id ${KMS_KEY_ID} \
     --plaintext fileb://newrelic-infra.yml \
     --query CiphertextBlob \
     --output text \
     | base64 --decode \
     > newrelic-infra.yml.encrypted
   ```

1. Copy "newrelic-infra.yml.encrypted" to S3

   ```ShellSession
   $ aws s3 --region "${AWS_DEFAULT_REGION}" cp \
     ./newrelic-infra.yml.encrypted \
     "s3://${CMS_ENV}-automation/encrypted-files/"
   ```

1. Get "newrelic-infra.yml.encrypted" from S3

   ```ShellSession
   $ aws s3 --region "${AWS_DEFAULT_REGION}" cp \
     "s3://${CMS_ENV}-automation/encrypted-files/newrelic-infra.yml.encrypted" \
     .
   ```

1. Test decryption of the new relic configuration file

   ```ShellSession
   $ aws kms --region "${AWS_DEFAULT_REGION}" decrypt \
     --ciphertext-blob fileb://newrelic-infra.yml.encrypted \
     --output text --query Plaintext \
     | base64 --decode \
     > /tmp/newrelic-infra.yml
   ```

1. Verify "newrelic-infra.yml" file contents

   ```ShellSession
   $ cat /tmp/newrelic-infra.yml
   ```

### Create, encrypt, and upload BFD AB2D keystore

> *** TO DO ***

### Create or update AMI with latest gold disk

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set gold disk test parameters

   *Example for "Prod" environment:*

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-prod
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge
   $ export OWNER_PARAM=743302140042
   $ export REGION_PARAM=us-east-1
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export VPC_ID_PARAM=vpc-0c9d55c3d85f46a65
   ```

1. Create or update AMI with latest gold disk

   ```ShellSession
   $ ./bash/update-gold-disk.sh
   ```

### Create or update infrastructure

1. Deploy infrastructure

   *Example for "Prod" environment:*
   
   ```ShellSession
   $ ./deploy-infrastructure.sh \
     --environment=ab2d-east-prod \
     --ecr-repo-environment=ab2d-mgmt-east-dev \
     --region=us-east-1 \
     --vpc-id=vpc-0c9d55c3d85f46a65 \
     --ssh-username=ec2-user \
     --owner=743302140042 \
     --ec2_instance_type_api=m5.xlarge \
     --ec2_instance_type_worker=m5.xlarge \
     --ec2_instance_type_other=m5.xlarge \
     --ec2_desired_instance_count_api=1 \
     --ec2_minimum_instance_count_api=1 \
     --ec2_maximum_instance_count_api=1 \
     --ec2_desired_instance_count_worker=1 \
     --ec2_minimum_instance_count_worker=1 \
     --ec2_maximum_instance_count_worker=1 \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --internet-facing=false \
     --auto-approve
   ```

### Submit an "Internet DNS Change Request Form" to product owner for the production application load balancer

> *** TO DO ***
