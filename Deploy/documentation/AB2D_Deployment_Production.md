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
   * [Initialize or verify base environment for production](#initialize-or-verify-base-environment-for-production)
   * [Encrypt and upload New Relic configuration file](#encrypt-and-upload-new-relic-configuration-file)
   * [Create, encrypt, and upload BFD AB2D keystore for Prod](#create-encrypt-and-upload-bfd-ab2d-keystore-for-prod)
   * [Create or update AMI with latest gold disk](#create-or-update-ami-with-latest-gold-disk)
   * [Create or update infrastructure](#create-or-update-infrastructure)
   * [Create or update application for production](#create-or-update-application-for-production)

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

### Initialize or verify base environment for sandbox

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Initialize or verify environment

   ```ShellShession
   $ ./bash/initialize-environment.sh \
     --database-secret-datetime=2020-01-02-09-15-01
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

### Create, encrypt, and upload BFD AB2D keystore for Prod

1. Note that there is a "bfd-users" slack channel for cmsgov with BFD engineers

1. Create a password in 1Password with the following desrciption

   ```
   AB2D_BFD_KEYSTORE_PASSWORD in Prod
   ```

1. Remove temporary directory (if exists)

   ```ShellSession
   $ rm -rf ~/Downloads/bfd-integration
   ```

1. Create a temporary directory

   ```ShellSession
   $ mkdir -p ~/Downloads/bfd-integration
   ```

1. Change to the temporary directory

   ```ShellSession
   $ cd ~/Downloads/bfd-integration
   ```

1. Create a self-signed SSL certificate for AB2D client to BFD sandbox

   ```ShellSession
   $ openssl req \
     -nodes -x509 \
     -days 1825 \
     -newkey rsa:4096 \
     -keyout client_data_server_ab2d_prod_certificate.key \
     -subj "/CN=ab2d-sbx-client" \
     -out client_data_server_ab2d_prod_certificate.pem
   ```

1. Note that the following two files were created

   - client_data_server_ab2d_prod_certificate.key (private key)

   - client_data_server_ab2d_prod_certificate.pem (self-signed crtificate)

1. Create a keystore that includes the self-signed SSL certificate for AB2D client to BFD sandbox

   ```ShellSession
   $ openssl pkcs12 -export \
     -in client_data_server_ab2d_prod_certificate.pem \
     -inkey client_data_server_ab2d_prod_certificate.key \
     -out ab2d_prod_keystore \
     -name client_data_server_ab2d_prod_certificate
   ```
 
1. Copy the "AB2D_BFD_KEYSTORE_PASSWORD in Prod" password from 1Password to the clipboard

1. Paste the password at the "Enter Export Password" prompt

1. Paste the password again at the "Verifying - Enter Export Password" prompt

1. Note that the following file has been created

   - ab2d_prod_keystore (keystore)

1. Export the public key from the self-signed SSL certificate for AB2D client to BFD sandbox

   ```ShellSession
   $ openssl x509 -pubkey -noout \
     -in client_data_server_ab2d_prod_certificate.pem \
     > client_data_server_ab2d_prod_certificate.pub
   ```

1. Note that the following file has been created

   - client_data_server_ab2d_prod_certificate.pub (public key)

1. Give the public key associated with the self-signed SSL certificate to a BFD engineer that you find on the "bfd-users" slack channel

   - client_data_server_ab2d_prod_certificate.pub

1. Send output from "prod.bfd.cms.gov" that includes only the certificate to a file

   ```ShellSession
   $ openssl s_client -connect prod.bfd.cms.gov:443 \
     2>/dev/null | openssl x509 -text \
     | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' \
     > prod.bfd.cms.gov.pem
   ```

1. Note that the following file has been created

   - prod.bfd.cms.gov.pem (certificate from the bfd sandbox server)

1. Import "prod.bfd.cms.gov" certificate into the keystore
   
   ```ShellSession
   $ keytool -import \
     -alias bfd-prod-selfsigned \
     -file prod.bfd.cms.gov.pem \
     -storetype PKCS12 \
     -keystore ab2d_prod_keystore
   ```

1. Copy the "AB2D_BFD_KEYSTORE_PASSWORD in Prod" password from 1Password to the clipboard

1. Enter the keystore password at the "Enter keystore password" prompt

1. Enter the following at the "Trust this certificate" prompt

   ```
   yes
   ```

1. Verify that both the bfd sandbox and client certificates are present
   
   ```ShellSession
   $ keytool -list -v -keystore ab2d_prod_keystore
   ```

1. Copy the "AB2D_BFD_KEYSTORE_PASSWORD in Prod" password from 1Password to the clipboard

1. Enter the keystore password at the "Enter keystore password" prompt

1. Verify that there are sections for the following two aliases in the keystore list output
   
   - Alias name: bfd-prod-sbx-selfsigned

   - Alias name: client_data_server_ab2d_prod_certificate

1. Save the keystore, private key, self-signed certificate, and public key in the "ab2d" vault of 1Password

   Label                                         |File
   ----------------------------------------------|-------------------------------------------
   AB2D Prod Keystore for BFD Prod               |ab2d_prod_keystore
   AB2D Prod Private Key for BFD Prod            |client_data_server_ab2d_prod_certificate.key
   AB2D Prod Self-signed Certificate for BFD Prod|client_data_server_ab2d_prod_certificate.pem
   AB2D Prod Public Key for BFD Prod             |client_data_server_ab2d_prod_certificate.pub

## Peer AB2D Dev, Sandbox, Impl environments with the BFD Sbx VPC and peer AB2D Prod with BFD Prod VPC

1. Note that peering is no longer needed for using the BFD Sbx (AKA prod-sbx.bfd.cms.gov)

1. *** TO DO *** Determine what will need to be done for BFD Prod

1. Note that CCS usually likes to handle the peering between project environments and BFD and then the ADO teams can authorize access

   > *** TO DO ***: Determine if this step needed for BFD Prod?

1. Request that CMS technical contact (e.g. Stephen) create a ticket like the following

   > https://jira.cms.gov/browse/CMSAWSOPS-53861

   > *** TO DO ***: Determine if this step needed for BFD Prod?

## Encrypt BFD keystore and put in S3

1. Get the keystore from 1Password and copy it to the "/tmp" directory

   ```
   /tmp/ab2d_prod_keystore
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set AWS environment variables using the CloudTamer API

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Enter the number of the desired AWS account where the desired logs reside

   ```
   5
   ```
   
1. Change to the ruby script directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/ruby
   ```

1. Ensure required gems are installed

   ```ShellSession
   $ bundle install
   ```
   
1. Encrypt keystore and put it in S3
   
   ```ShellSession
   $ bundle exec rake encrypt_and_put_file_into_s3['/tmp/ab2d_prod_keystore','ab2d-east-prod-automation']
   ```

1. Verify that you can get the encrypted keystore from S3 and decrypt it
   
   1. Remove existing keyfile from the "/tmp" directory (if exists)

      ```ShellSession
      $ rm -f /tmp/ab2d_prod_keystore
      ```

   1. Get keystore from S3 and decrypt it

      ```ShellSession
      $ bundle exec rake get_file_from_s3_and_decrypt['/tmp/ab2d_prod_keystore','ab2d-east-prod-automation']
      ```

   1. Verify that both the bfd sandbox and client certificates are present

      ```ShellSession
      $ keytool -list -v -keystore /tmp/ab2d_prod_keystore
      ```

   1. Copy the "AB2D_BFD_KEYSTORE_PASSWORD in Prod" password from 1Password to the clipboard

   1. Enter the keystore password at the "Enter keystore password" prompt

   1. Verify that there are sections for the following two aliases in the keystore list output

      - Alias name: bfd-prod-sbx-selfsigned

      - Alias name: client_data_server_ab2d_prod_certificate















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

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy infrastructure
   
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

### Initialize or verify base environment for production

1. Ensure that you are connected to CMS Cisco VPN

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```
   
1. Initialize or verify environment

   ```ShellShession
   $ ./bash/initialize-environment.sh \
     --database-secret-datetime=2020-01-02-09-15-01
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

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy infrastructure
   
   ```ShellSession
   $ ./deploy-infrastructure.sh \
     --environment=ab2d-east-prod \
     --ecr-repo-environment=ab2d-mgmt-east-dev \
     --region=us-east-1 \
     --vpc-id=vpc-0c9d55c3d85f46a65 \
     --ssh-username=ec2-user \
     --owner=743302140042 \
     --ec2_instance_type_api=m5.xlarge \
     --ec2_instance_type_worker=m5.4xlarge \
     --ec2_instance_type_other=m5.xlarge \
     --ec2_desired_instance_count_api=2 \
     --ec2_minimum_instance_count_api=2 \
     --ec2_maximum_instance_count_api=2 \
     --ec2_desired_instance_count_worker=2 \
     --ec2_minimum_instance_count_worker=2 \
     --ec2_maximum_instance_count_worker=2 \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --internet-facing=true \
     --auto-approve
   ```

### Create or update application for production

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-prod
   $ export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev
   $ export REGION_PARAM=us-east-1
   $ export VPC_ID_PARAM=vpc-0c9d55c3d85f46a65
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge
   $ export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.4xlarge
   $ export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=2
   $ export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=2
   $ export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=2
   $ export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=2
   $ export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=2
   $ export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=2
   $ export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export INTERNET_FACING_PARAM=true
   $ export CLOUD_TAMER_PARAM=true
   ``` 

1. Deploy application

   ```ShellSession
   $ ./bash/deploy-application.sh
   ```

### Submit an "Internet DNS Change Request Form" to product owner for the production application load balancer

> *** TO DO ***
