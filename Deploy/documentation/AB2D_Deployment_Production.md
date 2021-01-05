# AB2D Deployment Production

## Table of Contents

1. [Obtain and import api.ab2d.cms.gov entrust certificate](#obtain-and-import-apiab2dcmsgov-entrust-certificate)
lication-load-balancer)
   * [Download the production domain certificates and get private key from CMS](#download-the-production-domain-certificates-and-get-private-key-from-cms)
   * [Import the production domain certificate into certificate manager](#import-the-production-domain-certificate-into-certificate-manager)
1. [Peer AB2D Prod with BFD Prod VPC](#peer-ab2d-prod-with-bfd-prod-vpc)
1. [Create a keystore for API nodes](#create-a-keystore-for-api-nodes)
1. [Complete Okta production process](#complete-okta-production-process)
   * [Register for Okta production](#register-for-okta-production)
   * [Save the production Okta Client ID and Client Secret for the AB2D administrator account](#save-the-production-okta-client-id-and-client-secret-for-the-ab2d-administrator-account)
1. [Deploy to production](#deploy-to-production)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
   * [Encrypt and upload New Relic configuration file](#encrypt-and-upload-new-relic-configuration-file)
   * [Create AB2D keystore for Production](#create-ab2d-keystore-for-production)
   * [Encrypt AB2D keystore for Production and upload to S3](#encrypt-ab2d-keystore-for-production-and-upload-to-s3)
   * [Create or update AMI with latest gold disk](#create-or-update-ami-with-latest-gold-disk)
   * [Create or update infrastructure](#create-or-update-infrastructure)
   * [Create or update application](#create-or-update-application)
1. [Upload HPMS Reports](#upload-hpms-reports)
   * [Review HPMS Report requirements](#review-hpms-report-requirements)
   * [Get 2020 Parent Organization and Legal Entity to Contract Report](#get-2020-parent-organization-and-legal-entity-to-contract-report)
   * [Upload 2020 Parent Organization and Legal Entity to Contract Report data](#upload-2020-parent-organization-and-legal-entity-to-contract-report-data)
   * [Get 2020 Attestation Report](#get-2020-attestation-report)
   * [Upload 2020 Attestation Report data](#upload-2020-attestation-report-data)
1. [Upload static website to an Akamai Upload Directory within Akamai NetStorage](#upload-static-website-to-an-akamai-upload-directory-within-akamai-netstorage)
1. [Submit an "Internet DNS Change Request Form" to product owner for the production application load balancer](#submit-an-internet-dns-change-request-form-to-product-owner-for-the-production-application-load-balancer)
1. [Configure CloudWatch Log groups](#configure-cloudwatch-log-groups)
   * [Configure CloudTrail CloudWatch Log group](#configure-cloudtrail-cloudwatch-log-group)
   * [Configure VPC flow log CloudWatch Log group](#configure-vpc-flow-log-cloudwatch-log-group)
   * [Configure CloudWatch Log groups for RDS](#configure-cloudwatch-log-groups-for-rds)
   * [Submit a ticket to subscribe log groups to Splunk](#submit-a-ticket-to-subscribe-log-groups-to-splunk)
1. [Configure New Relic](#configure-new-relic)
   * [Create New Relic alerts](#create-new-relic-alerts)
     * [Create a CPU Percent above threshold alert](#create-a-cpu-percent-above-threshold-alert)
     * [Create a Memory Used percentage above threshold alert](#create-a-memory-used-percentage-above-threshold-alert)
     * [Create a Disk Used percentage above threshold alert](#create-a-disk-used-percentage-above-threshold-alert)
1. [Configure SNS Topic for CloudWatch alarms](#configure-sns-topic-for-cloudwatch-alarms)
1. [Configure VictorOps for production](#configure-victorops-for-production)
   * [Create a route key and verify escalation policy](#create-a-route-key-and-verify-escalation-policy)
   * [Forward New Relic alerts to the VictorOps alerting service](#forward-new-relic-alerts-to-the-victorops-alerting-service)
   * [Forward AWS CloudWatch alarms to the VictorOps alerting service](#forward-aws-cloudwatch-alarms-to-the-victorops-alerting-service)
   * [Create a dedicated SemanticBits Slack channel for AB2D incidents](#create-a-dedicated-semanticbits-slack-channel-for-ab2d-incidents)
   * [Configure VictorOps alerting service to forward alerts to a dedicated Slack channel using Slack integration](#configure-victorops-alerting-service-to-forward-alerts-to-a-dedicated-slack-channel-using-slack-integration)
1. [Configure Cloud Protection Manager](#configure-cloud-protection-manager)
   * [Ensure that all instances have CPM backup tags](#ensure-that-all-instances-have-cpm-backup-tags)
   * [Complete CPM questionnaire](#complete-cpm-questionnaire)
   * [Create a CPM ticket in CMS Jira Enterprise](#create-a-cpm-ticket-in-cms-jira-enterprise)

## Obtain and import api.ab2d.cms.gov entrust certificate](#obtain-and-import-apiab2dcmsgov-entrust-certificate)

### Download the production domain certificates and get private key from CMS

1. Note that CMS will request a domain certificate from Entrust for the following domain

   ```
   api.ab2d.cms.gov
   ```

1. Wait for the email from Entrust

1. Wait for CMS to provide the following

   - private key used to make the domain certificate request

   - forwarded email from Entrust to download the certificate

1. After receiving the private key save it under "~/Downloads"

   ```
   api_ab2d_cms_gov.key
   ```

1. Note the following in the email regarding the requested certificate

   - cn=api.ab2d.cms.gov

   - o=US Dept of Health and Human Services

   - l=Rockville

   - st=Maryland

   - c=US 

1. Select the link under "Use the following URL to pick up and install your certificate" in the email

1. Select "Other" from the "...server type" dropdown

1. Select **Next** on the "Select Server Type" page

1. Select **Download Certificates**

1. Wait for the download to complete

1. Change to the downloads directory

   ```ShellSession
   $ cd ~/Downloads
   ```

1. Note the following file has been downloaded

   ```
   entrust.zip
   ```

1. Create a "production" directory

   ```ShellSession
   $ mkdir -p production
   ```

1. Move the zip file to the "production" directory

   ```ShellSession
   $ mv entrust.zip production
   ```

1. Move the private key to the "production" directory

   ```ShellSession
   $ mv api_ab2d_cms_gov.key production
   ```

1. Save the "entrust.zip" file and private key in 1Password as follows

   Label                                       |File
   --------------------------------------------|--------------------
   AB2D Prod - Domain Certificate - Private Key|api_ab2d_cms_gov.key
   AB2D Prod - Domain Certificate - Zip        |entrust.zip

1. Change to the "production" directory

   ```ShellSession
   $ cd production
   ```

1. Unzip "entrust.zip"

   ```ShellSession
   $ 7z x entrust.zip
   ```

1. Note the that following files have been added to the "production" directory

   - Intermediate.crt

   - ServerCertificate.crt

   - Root.crt

### Import the production domain certificate into certificate manager

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. If the "Get Started" page is displayed, select **Get Started** under "Provision certificates"

1. Select **Import a certificate**

1. Open a terminal

1. Copy the contents of "ServerCertificate.crt" to the clipboard

   ```ShellSession
   $ cat ~/Downloads/production/ServerCertificate.crt | pbcopy
   ```

1. Return to the "Import a Certificate" page in Chrome

1. Paste the contents of the "ServerCertificate.crt" into the **Certificate body** text box

1. Return to the terminal

1. Copy the contents of the private key to the clipboard

   ```ShellSession
   $ cat ~/Downloads/production/api_ab2d_cms_gov.key | pbcopy
   ```
   
1. Paste the contents of the the private key that was provided separately by CMS into the **Certificate private key** text box

1. Return to the terminal

1. Copy the certificate key chain (Intermediate.crt + Root.crt) to the clipboard

   ```ShellSession
   $ echo -ne "$(cat ~/Downloads/production/Intermediate.crt)\n$(cat ~/Downloads/production/Root.crt)" | pbcopy
   ```

1. Paste the combined intermediate and root certificates into the **Certificate chain** text box

1. Select **Next** on the "Import certificate" page

1. Select **Review and import**

1. Note that the following information should be displayed

   *Format:*

   **Domains:** api.ab2d.cms.gov

   **Expires in:** {number} Days

   **Public key info:** RSA-2048

   **Signature algorithm:** SHA256WITHRSA
   
1. Select **Import**

## Peer AB2D Prod with BFD Prod VPC

1. *** TO DO ***: Need to update this section

1. Note that CCS usually likes to handle the peering between project environments and BFD and then the ADO teams can authorize access

   > *** TO DO ***: Determine if this step needed for BFD Prod?

1. Request that CMS technical contact (e.g. Stephen) create a ticket like the following

   > https://jira.cms.gov/browse/CMSAWSOPS-53861

   > *** TO DO ***: Determine if this step needed for BFD Prod?

## Create a keystore for API nodes

1. Create a "Password" entry in 1Password as follows

   *Format:*

   - **label:** AB2D Prod - API - Keystore Password

   - **vault:** ab2d

   - **password:** {generate a unique password}

   - **notes:** AB2D_KEYSTORE_PASSWORD

1. Create an "ab2d-api" directory under "Downloads"

   ```ShellSession
   $ mkdir -p ~/Downloads/ab2d-api
   ```

1. Change to the "ab2d-api" directory

   ```ShellSession
   $ cd ~/Downloads/ab2d-api
   ```

1. Create a self-signed SSL certificate for API nodes

   ```ShellSession
   $ openssl req \
     -nodes -x509 \
     -days 3650 \
     -newkey rsa:2048 \
     -keyout ab2d_api_prod.key \
     -subj "/CN=ab2d-api" \
     -config /usr/local/etc/openssl@1.1/openssl.cnf \
     -extensions v3_req \
     -out ab2d_api_prod.pem
   ```

1. Note that the following two files were created

   - ab2d_api_prod.key

   - ab2d_api_prod.pem

1. Create a keystore that includes the self-signed SSL certificate with an "ab2d" alias

   ```ShellSession
   $ openssl pkcs12 -export \
     -in ab2d_api_prod.pem \
     -inkey ab2d_api_prod.key \
     -out ab2d_api_prod.p12 \
     -name ab2d
   ```

1. Copy the "AB2D Prod - API - Keystore Password" password from 1Password to the clipboard

1. Paste the password at the "Enter Export Password" prompt and press **enter** on the keyboard

1. Paste the password again at the "Verifying - Enter Export Password" prompt and press **enter** on the keyboard

1. Verify the contents of the keystore

   ```ShellSession
   $ keytool -list -v -keystore ab2d_api_prod.p12
   ```

1. Save the keystore, private key, and self-signed certificate in the "ab2d" vault of 1Password

   Label                                    |File
   -----------------------------------------|-------------------------------------------
   AB2D Prod - API - Keystore               |ab2d_api_prod.p12
   AB2D Prod - API - Private Key            |ab2d_api_prod.key
   AB2D Prod - API - Self-signed Certificate|ab2d_api_prod.pem

## Complete Okta production process

### Register for Okta production

1. Open Chrome

1. Enter the following in the address bar

   > https://test.reg.idm.cms.gov/registration.html

1. Complete the registration process

1. Provide the scrum master with your Okta username

   *For example:*

   ```
   fred.smith@semanticbits.com
   ```

> *** TO DO ***: Ask John if he recalls the process after this step.

### Save the production Okta Client ID and Client Secret for the AB2D administrator account

1. Open Chrome

1. Enter the following in the address bar

   > https://idm.cms.gov/app/UserHome

1. Complete the log on

1. Select **Admin** from the top right of the page

1. Select the **Applications** tab

1. Select **AB2D - Admin**

1. Note the following:

   - the client ID of "AB2D Admin" is the public identifier for the client that is required for all OAuth flows

   - the client ID of "AB2D Admin" will be the same for every AB2D environment

1. Save the **Client ID** and **Client secret** under the "Client Credentials" section to 1Password as follows

   *Format:*

   1Password Type|Label                                               |Value
   --------------|----------------------------------------------------|-------------------------------
   Secure Note   |AB2D Prod : OKTA Prod : AB2D - Admin : Client ID    |{okta ab2d admin client id}
   Password      |AB2D Prod : OKTA Prod : AB2D - Admin : Client Secret|{okta ab2d admin client secret}

## Deploy to production

### Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN

1. Initialize or verify environment

   ```ShellShession
   $ cd ~/code/ab2d/Deploy \
     && export CMS_ENV_PARAM=ab2d-east-prod \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export REGION_PARAM=us-east-1 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export CLOUD_TAMER_PARAM=true \
     && ./bash/initialize-environment.sh
   ```

### Encrypt and upload New Relic configuration file

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

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
   4 (Prod AWS account)
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

### Create AB2D keystore for Production

1. Note that there is a "bfd-users" slack channel for cmsgov with BFD engineers

1. Create a password in 1Password with the following desrciption

   ```
   AB2D Prod - BFD Prod - Keystore Password
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

1. Create a self-signed SSL certificate for AB2D client to BFD Prod

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

1. Create a keystore that includes the self-signed SSL certificate for AB2D client to BFD prod

   ```ShellSession
   $ openssl pkcs12 -export \
     -in client_data_server_ab2d_prod_certificate.pem \
     -inkey client_data_server_ab2d_prod_certificate.key \
     -out ab2d_prod_keystore \
     -name client_data_server_ab2d_prod_certificate
   ```
 
1. Copy the "AB2D Prod - BFD Prod - Keystore Password" password from 1Password to the clipboard

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

   1. Ensure that you are connected to CMS Cisco VPN

   1. Set AWS environment variables using the CloudTamer API

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Enter the number of the desired AWS account where the desired logs reside

      ```
      4 (Prod AWS Account)
      ```

   1. Get certificate from "prod.bfd.cms.gov"

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-east-prod.pem \
        ec2-user@$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=${CMS_ENV}-api" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
        | head -1) \
        openssl s_client -connect prod.bfd.cms.gov:443 \
        2>/dev/null | openssl x509 -text \
        | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' \
        > prod.bfd.cms.gov.pem
      ```

1. Note that the following file has been created

   - prod.bfd.cms.gov.pem (certificate from the bfd prod server)

1. Verify that there is a certificate inside of "prod.bfd.cms.gov.pem"

   ```ShellSession
   $ cat prod.bfd.cms.gov.pem
   ```

1. Import "prod.bfd.cms.gov" certificate into the keystore
   
   ```ShellSession
   $ keytool -import \
     -alias bfd-prod-selfsigned \
     -file prod.bfd.cms.gov.pem \
     -storetype PKCS12 \
     -keystore ab2d_prod_keystore
   ```

1. Copy the "AB2D Prod - BFD Prod - Keystore Password" password from 1Password to the clipboard

1. Enter the keystore password at the "Enter keystore password" prompt

1. Enter the following at the "Trust this certificate" prompt

   ```
   yes
   ```

1. Verify that both the bfd prod and client certificates are present
   
   ```ShellSession
   $ keytool -list -v -keystore ab2d_prod_keystore
   ```

1. Copy the "AB2D Prod - BFD Prod - Keystore Password" password from 1Password to the clipboard

1. Enter the keystore password at the "Enter keystore password" prompt

1. Verify that there are sections for the following two aliases in the keystore list output
   
   - Alias name: bfd-prod-selfsigned

   - Alias name: client_data_server_ab2d_prod_certificate

1. Save the keystore, private key, self-signed certificate, and public key in the "ab2d" vault of 1Password

   Label                                         |File
   ----------------------------------------------|-------------------------------------------
   AB2D Prod - BFD Prod - Keystore               |ab2d_prod_keystore
   AB2D Prod - BFD Prod - Private Key            |client_data_server_ab2d_prod_certificate.key
   AB2D Prod - BFD Prod - Self-signed Certificate|client_data_server_ab2d_prod_certificate.pem
   AB2D Prod - BFD Prod - Public Key             |client_data_server_ab2d_prod_certificate.pub

### Encrypt AB2D keystore for Production and upload to S3

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
   4 (Prod AWS account)
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

1. Change to your "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set gold disk parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-prod \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge \
     && export OWNER_PARAM=743302140042 \
     && export REGION_PARAM=us-east-1 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export VPC_ID_PARAM=vpc-0c9d55c3d85f46a65 \
     && export CLOUD_TAMER_PARAM=true
   ```

1. Create or update AMI with latest gold disk

   ```ShellSession
   $ ./Deploy/bash/update-gold-disk.sh
   ```

### Create or update infrastructure

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-prod \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export EC2_INSTANCE_TYPE_CONTROLLER_PARAM=m5.xlarge \
     && export REGION_PARAM=us-east-1 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export CLOUD_TAMER_PARAM=true
   ```

1. Deploy infrastructure
   
   ```ShellSession
   $ ./deploy-infrastructure.sh
   ```

### Create or update application

1. Change to your "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-prod \
     && export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev \
     && export REGION_PARAM=us-east-1 \
     && export VPC_ID_PARAM=vpc-0c9d55c3d85f46a65 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge \
     && export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.4xlarge \
     && export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=2 \
     && export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=2 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=4 \
     && export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=2 \
     && export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=2 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=4 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export INTERNET_FACING_PARAM=true \
     && export CLOUD_TAMER_PARAM=true
   ``` 

1. Deploy application

   ```ShellSession
   $ ./Deploy/bash/deploy-application.sh
   ```

## Upload HPMS Reports

### Review HPMS Report requirements

1. Note that before the system can begin handling PDP requests, the database needs to be seeded with certain data from HPMS

1. Note that the data comes from two HPMS reports that need to be uploaded in the following order:

   - 2020 Parent Organization and Legal Entity to Contract Report

   - 2020 Attestation Report

### Get 2020 Parent Organization and Legal Entity to Contract Report

1. Open Chrome

1. Enter the following in the address bar

   > https://hpms.cms.gov/app/home.aspx

1. Log on to HPMS

1. Select **OK** on the "hpms.cms.gov says" dialog

1. Select **Contract Management**

1. Select **Contact Reports**

1. Select the following from the **Select a Contract Year** list

   ```
   2020
   ```

1. Select **Next** on the "Select a Contract Year" page

1. Select the following from the **Select a Report** list

   ```
   Parent Organization and Legal Entity to Contract Report
   ```

1. Select **Next** on the "Contract Year 2020" page

1. Select the **All Parent Organizations** radio button

1. Select the **All Legal Entities** radio button

1. Select **Next** on the "Contract Management Reports 2020" page

1. Scroll down to the bottom of the page

1. Select **Download to Excel**

1. Wait for the download to complete

1. Open the downloaded file

   *Example:*

   ```
   parent_org_and_legal_entity_20200601_143055.xls
   ```

1. Select **Yes** on the "Alert" dialog to open the file in Excel

1. Save the file in "xlsx" format

   1. Select **File**

   1. Select **Save as**

   1. Select the following from the **File Format** dropdown

      ```
      Excel Workbook (.xlsx)
      ```

   1. Select **Save**

1. Close Excel

1. Open a terminal tab

1. Change to the "Downloads" directory

   ```ShellSession
   $ cd ~/Downloads
   ```

1. Delete the "xls" file

   ```ShellSession
   $ rm -f parent_org_and_legal_entity_*.xls
   ```

1. Verify the name of the "xlsx" file

   ```ShellSession
   $ ls parent_org_and_legal_entity_*.xlsx
   ```

1. Note the name of the "xlsx" file

   *Example:*

   ```
   parent_org_and_legal_entity_20200601_143055.xlsx
   ```

1. Return to the "HPMS" website

1. Select **HPMS** in the top left of the page

1. Select **Log Out**

### Upload 2020 Parent Organization and Legal Entity to Contract Report data

1. Open a terminal tab

1. Note that even though we are not using Postman for this process, we will move the file to the Postman working directory (in case we want to use Postman for debugging purposes)

1. Remove any existing contract report data files from Postman working directory

   ```ShellSession
   $ rm -f ~/Postman/files/parent_org_and_legal_entity_*.xlsx
   ```

1. Copy the file to the Postman working directory

   ```ShellSession
   $ mv ~/Downloads/parent_org_and_legal_entity_*.xlsx ~/Postman/files
   ```

1. Set "AB2D Prod : OKTA Prod : AB2D - Admin : Client ID" from 1Password

   *Format:*

   ```ShellSession
   $ OKTA_AB2D_ADMIN_CLIENT_ID={okta ab2d admin client id}
   ```

1. Set "AB2D Prod : OKTA Prod : AB2D - Admin : Client Secret" from 1Password

   *Format:*

   ```ShellSession
   $ OKTA_AB2D_ADMIN_CLIENT_SECRET={okta ab2d admin client secret}
   ```

1. Set authorization by converting "client_id:client secret" to Base64

   ```ShellSession
   $ AUTH=$(echo -n "${OKTA_AB2D_ADMIN_CLIENT_ID}:${OKTA_AB2D_ADMIN_CLIENT_SECRET}" | base64)
   ```

1. Set "AB2D Prod - OKTA Prod - URL" from 1Password

   ```ShellSession
   $ AB2D_OKTA_JWT_ISSUER={ab2d okta jwt issuer}
   ```

1. Get bearer token

   ```ShellSession
   $ BEARER_TOKEN=$(curl -X POST "${AB2D_OKTA_JWT_ISSUER}/v1/token?grant_type=client_credentials&scope=clientCreds" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -H "Accept: application/json" \
     -H "Authorization: Basic ${AUTH}" \
     | jq --raw-output ".access_token")
   ```

1. Verify bearer token

   ```ShellSession
   $ echo $BEARER_TOKEN
   ```

1. Change to the Postman working directory

   ```ShellSession
   $ cd ~/Postman/files
   ```

1. Set the file name

   ```ShellSession
   $ FILE=$(ls parent_org_and_legal_entity_*.xlsx | tr -d '\r')
   ```

1. Upload 2020 Parent Organization and Legal Entity to Contract Report data
   
   ```ShellSession
   $ curl \
     -sD - \
     --location \
     --request POST 'https://api.ab2d.cms.gov/api/v1/admin/uploadOrgStructureReport' \
     --header "Authorization: Bearer ${BEARER_TOKEN}" \
     --form "file=@${FILE}"
   ```

1. Verify that you get a "202" response

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Connect to a worker node for production

   ```ShellSesssion
   $ ./bash/connect-to-node.sh
   ```

1. Get database secrets

   ```ShellSession
   $ export DATABASE_HOST=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_HOST"' | tr -d '\r') \
     && export DATABASE_NAME=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_DATABASE"' | tr -d '\r') \
     && export DATABASE_USER=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_USER"' | tr -d '\r') \
     && export PGPASSWORD=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_PASSWORD"' | tr -d '\r')
   ```

1. Connect to the database

    ```ShellSession
    $ psql --host "${DATABASE_HOST}" --username "${DATABASE_USER}" --dbname "${DATABASE_NAME}"
    ```

1. Query the database

   ```ShellSession
   select p.org_name as "Parent Org Name",
     s.org_name as "Org Name",
     s.hpms_id as "Org HPMS ID",
     c.contract_number as "Contract Number"
   from sponsor s
     left join sponsor p on s.parent_id = p.id
     inner join contract c on s.id = c.sponsor_id
   where c.contract_number not like 'Z%';
   ```

1. Spot check the results to see if the expected records have been inserted

### Get 2020 Attestation Report

1. Open Chrome

1. Enter the following in the address bar

   > https://hpms.cms.gov/app/home.aspx

1. Log on to HPMS

1. Select **OK** on the "hpms.cms.gov says" dialog

1. Select **Contract Management**

1. Select **Claims Data Attestation**

1. Expand the **Reports** node in the leftmost panel

1. Select the **Claims Data Attestation Report** node in the leftmost panel

1. Select the following from the **Select Contracts** dropdown

   ```
   Select All
   ```

1. Click into a whitespace are of the form to close the dropdown

1. Note that the dropdown should now display the following

   ```
   All Selected
   ```

1. Select **Create Report**

1. Scroll down to the bottom of the page

1. Select **Download to Excel**

1. Wait for the download to complete

1. Open the downloaded file

   *Example:*

   ```
   Attestation_Report10122020_1609.xlsx
   ```

1. Close Excel

1. Open a terminal tab

1. Change to the "Downloads" directory

   ```ShellSession
   $ cd ~/Downloads
   ```

1. Verify the name of the "xlsx" file

   ```ShellSession
   $ ls Attestation_Report*.xlsx
   ```

1. Note the name of the "xlsx" file

   *Example:*

   ```
   Attestation_Report1591039077734.xlsx
   ```

1. Return to the "HPMS" website

1. Select **HPMS** in the top left of the page

1. Select **Log Out**

### Upload 2020 Attestation Report data

1. Open a terminal tab

1. Note that even though we are not using Postman for this process, we will move the file to the Postman working directory (in case we want to use Postman for debugging purposes)

1. Remove any existing contract report data files from Postman working directory

   ```ShellSession
   $ rm -f ~/Postman/files/Attestation_Report*.xlsx
   ```

1. Copy the file to the Postman working directory

   ```ShellSession
   $ mv ~/Downloads/Attestation_Report*.xlsx ~/Postman/files
   ```

1. Set "AB2D Prod : OKTA Prod : AB2D - Admin : Client ID" from 1Password

   *Format:*

   ```ShellSession
   $ OKTA_AB2D_ADMIN_CLIENT_ID={okta ab2d admin client id}
   ```

1. Set "AB2D Prod : OKTA Prod : AB2D - Admin : Client Secret" from 1Password

   *Format:*

   ```ShellSession
   $ OKTA_AB2D_ADMIN_CLIENT_SECRET={okta ab2d admin client secret}
   ```

1. Set authorization by converting "client_id:client secret" to Base64

   ```ShellSession
   $ AUTH=$(echo -n "${OKTA_AB2D_ADMIN_CLIENT_ID}:${OKTA_AB2D_ADMIN_CLIENT_SECRET}" | base64)
   ```

1. Set "AB2D Prod - OKTA Prod - URL" from 1Password

   ```ShellSession
   $ AB2D_OKTA_JWT_ISSUER={ab2d okta jwt issuer}
   ```

1. Get bearer token

   ```ShellSession
   $ BEARER_TOKEN=$(curl -X POST "${AB2D_OKTA_JWT_ISSUER}/v1/token?grant_type=client_credentials&scope=clientCreds" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -H "Accept: application/json" \
     -H "Authorization: Basic ${AUTH}" \
     | jq --raw-output ".access_token")
   ```

1. Verify bearer token

   ```ShellSession
   $ echo $BEARER_TOKEN
   ```

1. Change to the Postman working directory

   ```ShellSession
   $ cd ~/Postman/files
   ```

1. Set the file name

   ```ShellSession
   $ FILE=$(ls Attestation_Report*.xlsx | tr -d '\r')
   ```

1. Upload the {current year} Attestation Report data
   
   ```ShellSession
   $ curl \
     -sD - \
     --location \
     --request POST 'https://api.ab2d.cms.gov/api/v1/admin/uploadAttestationReport' \
     --header "Authorization: Bearer ${BEARER_TOKEN}" \
     --form "file=@${FILE}"
   ```

1. Verify that you get a "202" response

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Connect to a worker node for production

   ```ShellSesssion
   $ ./bash/connect-to-node.sh
   ```

1. Get database secrets

   ```ShellSession
   $ export DATABASE_HOST=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_HOST"' | tr -d '\r') \
     && export DATABASE_NAME=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_DATABASE"' | tr -d '\r') \
     && export DATABASE_USER=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_USER"' | tr -d '\r') \
     && export PGPASSWORD=$(docker exec -it $(docker ps -aqf "name=ecs-worker-*" --filter "status=running") bash -c 'echo "$AB2D_DB_PASSWORD"' | tr -d '\r')
   ```

1. Connect to the database

    ```ShellSession
    $ psql --host "${DATABASE_HOST}" --username "${DATABASE_USER}" --dbname "${DATABASE_NAME}"
    ```

1. Query the database

   ```ShellSession
   select p.org_name as "Parent Org Name",
     s.org_name        as "Org Name",
     s.hpms_id         as "Org HPMS ID",
     c.contract_number as "Contract Number",
     c.attested_on     as "Attested On"
   from sponsor s
     left join sponsor p on s.parent_id = p.id
     inner join contract c on s.id = c.sponsor_id
   where c.contract_number not like 'Z%'
   and c.attested_on is not null;
   ```

1. Spot check the results to see if the expected records have been inserted

## Upload static website to an Akamai Upload Directory within Akamai NetStorage

1. Note that these directions assume that the "Create an Akamai Upload Account in Akamai NetStorage" section in the "AB2D_Deployment_Greenfield" document has been completed

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create the static website

   *Akamai Prod (using website pulled from CloudFront):*

   ```ShellSession
   $ export AKAMAI_RSYNC_DOMAIN_PARAM="ab2d.rsync.upload.akamai.com" \
     && export AKAMAI_UPLOAD_DIRECTORY_PARAM="1055691" \
     && export GENERATE_WEBSITE_FROM_CODE_PARAM="false" \
     && export NETSTORAGE_SSH_KEY_PARAM="${HOME}/.ssh/ab2d-akamai" \
     && export WEBSITE_DEPLOYMENT_TYPE_PARAM="prod" \
     && export WEBSITE_DIRECTORY_PARAM="${HOME}/akamai-from-cloudfront/_site" \
     && ./bash/create-or-update-website-akamai.sh
   ```

   *Akamai Prod (using website generated from code):*

   ```ShellSession
   $ export AKAMAI_RSYNC_DOMAIN_PARAM="ab2d.rsync.upload.akamai.com" \
     && export AKAMAI_UPLOAD_DIRECTORY_PARAM="1055691" \
     && export GENERATE_WEBSITE_FROM_CODE_PARAM="true" \
     && export NETSTORAGE_SSH_KEY_PARAM="${HOME}/.ssh/ab2d-akamai" \
     && export WEBSITE_DEPLOYMENT_TYPE_PARAM="prod" \
     && export WEBSITE_DIRECTORY_PARAM="${HOME}/akamai/_site" \
     && ./bash/create-or-update-website-akamai.sh
   ```

1. Note that the following should have been created in the Akamai Upload Directory

   - timestamped backup of the new website

     **Akamai Prod Example:*

     ```
     /1055691/_site_2020-06-18_11-47-48
     ```

   - new website

     *Akamai Prod:*

     ```
     /1055691/_site
     ```

## Submit an "Internet DNS Change Request Form" to product owner for the production application load balancer

> *** TO DO ***

## Configure CloudWatch Log groups

### Configure CloudTrail CloudWatch Log group

1. Log on to the AWS production account

1. Create a CloudTrail CloudWatch Log group

   1. Select **Log groups** from the leftmost panel

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** cloudtrail-logs

   1. Select **Create**

1. Create a trail in CloudTrail

   1. Select **CloudTrail**

   1. Select **Create trail**

   1. Configure "Create Trail"

      - **Trail name:** cloudtrail-default

      - **Apply trail to all regions:** No

   1. Configure "Management events"

      - **Read/Write events:** All

      - **Log AWS KMS events:** Yes

   1. Configure "Insights events"

      - **Log Insights events:** No

   1. Configure "Data Events" for the "S3" tab

      - **Select all S3 buckets in your account:** Checked

   1. Configure "Storage location"

      *Format:*
      
      - **Create a new S3 bucket:** No

      - **S3 bucket:** {environment}-cloudtrail

   1. Select **Create**

1. Create a role for CloudTrail

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
      4 (Prod AWS account)
      ```

   1. Change to the shared directory
      
      ```ShellSession
      $ cd terraform/environments/ab2d-east-prod
      ```

   1. Create the "Ab2dCloudTrailAssumeRole" role

      ```ShellSession
      $ aws --region us-east-1 iam create-role \
        --role-name Ab2dCloudTrailAssumeRole \
        --assume-role-policy-document file://ab2d-cloudtrail-assume-role-policy.json
      ```

   1. Add a CloudWatch log group policy to the CloudTrail role

      ```ShellSession
      $ aws --region us-east-1 iam put-role-policy \
        --role-name Ab2dCloudTrailAssumeRole \
	--policy-name Ab2dCloudTrailPolicy \
	--policy-document file://ab2d-cloudtrail-cloudwatch-policy.json
      ```

1. Update the trail in CloudTrail with the log group and role information

   ```ShellSession
   $ aws --region us-east-1 cloudtrail update-trail \
     --name cloudtrail-default \
     --cloud-watch-logs-log-group-arn arn:aws:logs:us-east-1:595094747606:log-group:cloudtrail-logs:* \
     --cloud-watch-logs-role-arn arn:aws:iam::595094747606:role/Ab2dCloudTrailAssumeRole
   ```

### Configure VPC flow log CloudWatch Log group

1. Log on to the AWS production account

1. Create a VPC flow log CloudWatch Log group

   1. Select **CloudWatch**

   1. Select **Log groups** from the leftmost panel

   1. Select **Create log group**

   1. Configure the "Create log group" page as follows

      - **Log Group Name:** vpc-flowlogs

   1. Select **Create**

1. Create a VPC flow log

   1. Select **VPC**

   1. Select **Your VPCs** from the leftmost panel

   1. Select the following VPC

      *Example for "Prod" environment:*
      
      ```
      ab2d-east-prod
      ```

   1. Select the **Flow Logs** tab

   1. Select **Create flow log**

   1. Configure the "Create flow log" page

      *Format:*

      - **Filter:** All

      - **Maximum aggregation interval:** 10 minutes

      - **Destination:** Send to CloudWatch Logs

      - **Destination log group:** vpc-flowlogs

      - **IAM role:** Ab2dInstanceRole

   1. Select **Create**

   1. Select **Close** on the "Create flow log" page

### Configure CloudWatch Log groups for RDS

1. Log on to the AWS production account

1. Select **RDS**

1. Select **Parameter groups** from the leftmost panel

1. Select the following parameter group

   ```
   ab2d-rds-parameter-group
   ```

1. Type the following in the "Filter parameters" text box

   ```
   log_statement
   ```

1. Select **Edit parameters**

1. Select the following value from the dropdown beside "log_statement"

   ```
   all
   ```

1. Type the following in the "Filter parameters" text box

   ```
   log_min_duration_statement
   ```

1. Type the following value beside "log_min_duration_statement"

   ```
   1
   ```

1. Select **Preview changes**

1. Verify that the desired changes are correct

1. Select **Save changes**

1. Select **Databases** from the leftmost panel

1. Select the radio button beside the "ab2d" DB identifier

1. Select **Modify**

1. Scroll down to the "Log exports" section

1. Check **Postgresql log**

1. Check **Upgrade log**

1. Select **Continue**

1. Select **Modify DB Instance**

1. Wait for log groups to be configured

   *Note that this may take a couple minutes.*

1. Select **CloudWatch**

1. Select **Log groups**

1. Verify that the following log group now appears in the list

   ```
   /aws/rds/instance/ab2d/postgresql
   ```

### Submit a ticket to subscribe log groups to Splunk

> *** TO DO ***

*See this ticket:*

> https://jiraent.cms.gov/servicedesk/customer/portal/13/CLDSUPSD-7398

## Configure New Relic

### Create New Relic alerts

#### Create a CPU Percent above threshold alert

1. Open Chrome

1. Log on to New Relic

1. Select the **Infrastructure** tab

1. Select the **Settings** tab

1. Select **Alerts** from the leftmost panel

1. Select **Create alert condition**

1. Type the following in the **Name this alert condition** text box

   ```
   AB2D Prod - CPU Percent Above
   ```

1. Select **Host Metrics** under the "Choose an alert type" section

1. Select **FILTER HOSTS** under the "Narrow down hosts" section

1. Scroll down to **ec2KeyName**

1. Expand **ec2KeyName**

1. Check **ab2d-east-prod**

1. Collapse **ec2KeyName**

1. Select **FILTER HOSTS** again to collapse attributes

1. Configure the "Define thresholds" section as follows

   - **First dropdown:** CPU %

   - **has a value:** above

   - **percent:** 85

   - **time dropdown:** for at least

   - **minutes:** 5

1. Configure the "Configuration" section
 
   - **Alert policy - Name your policy:** AB2D Prod - CPU Percent Above

   - **Alert policy - Email address:** {devops engineer email}

   - **Condition status - Enabled:** checked

   - **Runbook:** "TO DO"

   - **Violation time limit - Close open violations after:** checked

   - **"Close open violations after" dropdown:** 24h

1. Select **Create**

#### Create a Memory Used percentage above threshold alert

1. Open Chrome

1. Log on to New Relic

1. Select the **Infrastructure** tab

1. Select the **Settings** tab

1. Select **Alerts** from the leftmost panel

1. Select **Create alert condition**

1. Type the following in the **Name this alert condition** text box

   ```
   AB2D Prod - Memory Used Percentage Above
   ```

1. Select **Host Metrics** under the "Choose an alert type" section

1. Select **FILTER HOSTS** under the "Narrow down hosts" section

1. Scroll down to **ec2KeyName**

1. Expand **ec2KeyName**

1. Check **ab2d-east-prod**

1. Collapse **ec2KeyName**

1. Select **FILTER HOSTS** again to collapse attributes

1. Configure the "Define thresholds" section as follows

   - **First dropdown:** Memory Used %

   - **has a value:** above

   - **percent:** 85

   - **time dropdown:** for at least

   - **minutes:** 5

1. Configure the "Configuration" section
 
   - **Alert policy - Name your policy:** AB2D Prod - Memory Used Percentage Above

   - **Alert policy - Email address:** {devops engineer email}

   - **Condition status - Enabled:** checked

   - **Runbook:** "TO DO"

   - **Violation time limit - Close open violations after:** checked

   - **"Close open violations after" dropdown:** 24h

1. Select **Create**

#### Create a Disk Used percentage above threshold alert

1. Open Chrome

1. Log on to New Relic

1. Select the **Infrastructure** tab

1. Select the **Settings** tab

1. Select **Alerts** from the leftmost panel

1. Select **Create alert condition**

1. Type the following in the **Name this alert condition** text box

   ```
   AB2D Prod - Disk Used Percentage Above
   ```

1. Select **Host Metrics** under the "Choose an alert type" section

1. Select **FILTER HOSTS** under the "Narrow down hosts" section

1. Scroll down to **ec2KeyName**

1. Expand **ec2KeyName**

1. Check **ab2d-east-prod**

1. Collapse **ec2KeyName**

1. Select **FILTER HOSTS** again to collapse attributes

1. Configure the "Define thresholds" section as follows

   - **First dropdown:** Disk Used %

   - **has a value:** above

   - **percent:** 85

   - **time dropdown:** for at least

   - **minutes:** 5

1. Configure the "Configuration" section
 
   - **Alert policy - Name your policy:** AB2D Prod - Disk Used Percentage Above

   - **Alert policy - Email address:** {devops engineer email}

   - **Condition status - Enabled:** checked

   - **Runbook:** "TO DO"

   - **Violation time limit - Close open violations after:** checked

   - **"Close open violations after" dropdown:** 24h

1. Select **Create**

## Configure SNS Topic for CloudWatch alarms

1. Log on to the AWS account

1. Select **Simple Notification Service**

1. Select **Topics** from the leftmost panel

1. Select **Create topic**

1. Configure the "Create topic" page as follows

   - **Name:** ab2d-east-prod-cloudwatch-alarms

   - **Display name:** ab2d-east-prod-cloudwatch-alarms

1. Select **Create topic**

## Configure VictorOps for production

### Create a route key and verify escalation policy

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Select the **Settings** tab

1. Scroll to the bottom of the page

1. Select **Add Key**

1. Type the following in the **Routing Key** text box

   ```
   ab2d-prod
   ```

1. Select the following from the **Escalation Policies** dropdown

   ```
   AB2D:Standard
   ```

1. Select the checkmark button to the right of the dropdown

1. Select the **Teams** tab

1. Select the following team

   ```
   AB2D
   ```

1. Select the **Escalation Policies** tab

1. Expand the escalation policy

1. Note that the following now appears under "Routes"

   ```
   ab2d-prod
   ```

### Forward New Relic alerts to the VictorOps alerting service

1. Open Chrome

1. Open New Relic

1. Select the **Alerts** tab

1. If a "Welcome to the new Alerts" page appears, do the following:

   1. Note the following information

      - "We've reimagined alerting for New Relic products so you can resolve issues faster and with less noise."

   1. Select **Next**

   1. Note the following information

      - "Monitor the metrics you care about across your entire infrastructure, including third-party-plugins."

   1. Select **Next**

   1. Note the following information

      - "Integrate with tools your team already uses, like PagerDuty and Slack, or use webhooks to integrate with virtually any software."

   1. Select **Finish**

1. Select **Notification channels** in the leftmost panel

1. Select **New notification channel**

1. Configure the "Channel details" as follows

   - **Select a channel:** VictorOps

   - **Channel name:** ab2d-prod

   - **Key:** {victors ops api key for new relic}

   - **Route key:** ab2d-prod

1. Select **Create channel**

1. Select the **Alert policies** tab

1. Select **Add alert policies**

1. Check all of the following

   - AB2D Prod - CPU Percent Above

   - AB2D Prod - Disk Used Percentage Above

   - AB2D Prod - Memory Used Percentage Above

1. Select **Save changes**

1. Select the **Channel details** tab

1. Select **Send a test notification**

1. Verify that the following is displayed

   ```
   Test notification successful

   {
     response: 200,

     {\n "result" : "success"\n}
   }
   ```

1. Select **Got it**

1. Select the **APM* tab

1. Open a new Chrome tab

1. Open VictorOps

1. Log on to VictorOps

1. Select the **Timeline** tab

1. Verify that a test notification from New Relic appears at the top of the Timeline that looks something like this

   ```
   New Relic        Jul. 7 - 9:01 PM
   Info:
   I am a test Violation -
   https://alerts.newrelic.com/accounts/2597286/incidents/0
   ```

1. Expand the **Alert Payload** of the test notification

1. Note the "Alert Data"

1. Note the "VictorOps Fields"

1. Note the "routing_key" that appears under "VictorOps Fields"

### Forward AWS CloudWatch alarms to the VictorOps alerting service

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Before proceeding, ensure you on call for on AB2D in VictorOps

   1. Select the **Timeline** tab

   1. Select **Users** tab under the "People" section in the leftmost panel

   1. Verify that AB2D and the leaf image appears for your user

   1. If your are not on call, override the current user so that you are on call

1. Select the **Integrations** tab

1. Type the following in the **Search** text box

   ```
   cloudwatch
   ```

1. If AWS CloudWatch does not display "enabled", do the following

   1. Select **AWS CloudWatch**

   1. Select **Enable Integration**

1. Select the **Integrations** tab

1. Type the following in the **Search** text box again

   ```
   cloudwatch
   ```

1. Verify that the following is displayed

   ```
   AWS CloudWatch
   Specialized Tools
   enabled
   ```

1. Select **AWS CloudWatch**

1. Copy and save the following information for next steps

   *Service API Endpoint:*

   ```
   {victors ops service api endpoint for aws cloudwatch}/{routing key}
   ```

1. Open a new Chrome tab

1. Log on to the AWS account

1. Select **Simple Notification Service**

1. Select **Topics** in the leftmost panel

1. Select the following

   ```
   ab2d-east-prod-cloudwatch-alarms
   ```

1. Select the **Subscriptions** tab

1. Select **Create subscription**

1. Configure the "Create subscription" page as follows

   - **Topic ARN:** {keep default}

   - **Protocol:** HTTPS

   - **Endpoint:** {victors ops service api endpoint for aws cloudwatch}/{routing key}

   - **Enable raw message delivery:** unchecked

1. Select **Create subscription**

1. Wait for "Status" to display the following

   *Note that you will likely need to refesh the page to see the status change to "Confirmed".*

   ```
   Confirmed
   ```

1. Select **Topics** from the leftmost panel

1. Select the following topic

   ```
   ab2d-east-prod-cloudwatch-alarms
   ```

1. Select **Publish message**

1. Configure the "Message details" section as follows

   - **Subject:** {keep blank}

   - **Time to Live (TTL):** {keep blank}

1. Configure the "Message body" section as follows

   - **Message structure:** Identical payload for all delivery protocols

   - **Message body to send to the endpoint:**

     ```
     {"AlarmName":"AB2D Prod - VictorOps - CloudWatch Integration TEST","NewStateValue":"ALARM","NewStateReason":"failure","StateChangeTime":"2017-12-14T01:00:00.000Z","AlarmDescription":"VictorOps - CloudWatch Integration TEST"}
     ```

1. Select **Publish message**

1. Return to the VictorOps Chrome tab

1. Verify that you see the following first new chronological message in the timeline

   *Format of first new chronological message:*

   ```
   AWS CloudWatch
   {datetime}
   Critical:VictorOps - CloudWatch Integration TEST
   / failure
   NS:
   Region:
   #{incident number}
   ```

   *Example of first new chronological message:*

   ```
   AWS CloudWatch
   Jul. 8 - 3:37 PM
   Critical:VictorOps - CloudWatch Integration TEST
   / failure
   NS:
   Region:
   #1055
   ```

1. Verify that you see the following second new chronological message in the timeline

   *Example of second new chronological message:*

   ```
   Incident #{incident number} AWS CloudWatch
   {datetime}
   AWS CloudWatch: VictorOps - CloudWatch Integration TEST
   Policies: AB2D : Standard
   ```

   *Example of second new chronological message:*

   ```
   Incident #1055 AWS CloudWatch
   Jul. 8 - 3:37 PM
   AWS CloudWatch: VictorOps - CloudWatch Integration TEST
   Policies: AB2D : Standard
   ```

1. Verify that you see the following third new chronological message in the timeline

   *Format of third new chronological message:*

   ```
   Trying to contact {user} for #{incident number}, sending SMS
   ```

   *Example of third new chronological message:*

   ```
   Trying to contact fred.smith for #1055, sending SMS
   ```

1. Look for a message on your mobile phone

1. Verify that am SMS message was received on your mobile phone that looks like this

   *Format:*

   ```
   {incident number}: VictorOps - CloudWatch Integration TEST - failure (Ack: {acknowledged value}, Res: {resolved value})
   ```

   *Example:*

   ```
   1055: VictorOps - CloudWatch Integration TEST - failure (Ack: 82603, Res: 20283)
   ```

1. Text the following on your mobile phone to acknowledge the incident

   *Format:*

   ```
   {acknowledged value}
   ```

   *Example:*

   ```
   82603
   ```

1. Return to the VictorOps Chrome tab

1. Note that an "acknowledged by" message appears in the timeline

   *Format of "acknowledged by" message:*

   ```
   Incident #{incident number} AWS CloudWatch
   {datetime}
   AWS CloudWatch: VictorOps - CloudWatch Integration TEST
   Policies: AB2D : Standard
   Acknowledged by: {user}
   ```

   *Example of "acknowledged by" message:*

   ```
   Incident #1055 AWS CloudWatch
   Jul. 8 - 3:40 PM
   AWS CloudWatch: VictorOps - CloudWatch Integration TEST
   Policies: AB2D : Standard
   Acknowledged by: fred.smith
   ```

1. Note that a "paging cancelled" message also appears in the timeline

   *Format of "paging cancelled" message:*

   ```
   Paging cancelled for fred.smith
   {datetime}
   ```

   *Example of "paging cancelled" message:*

   ```
   Paging cancelled for fred.smith
   Jul. 8 - 3:40 PM
   ```

1. Return to your mobile phone

1. Text the following to resolve the incident

   *Format:*

   ```
   {resolved value}
   ```

   *Example:*

   ```
   20283
   ```

1. Return to the VictorOps Chrome tab

1. Note that a "resolved by" message appears in the timeline

   *Format of "resolved by" message:*

   ```
   Incident #{incident number} AWS CloudWatch
   {datetime}
   AWS CloudWatch: VictorOps - CloudWatch Integration TEST
   Policies: AB2D : Standard
   Resolved by: {user}
   ```

   *Example of "resolved by" message:*

   ```
   Incident #1055 AWS CloudWatch
   Jul. 8 - 4:06 PM
   AWS CloudWatch: VictorOps - CloudWatch Integration TEST
   Policies: AB2D : Standard
   Resolved by: fred.smith
   ```

### Create a dedicated SemanticBits Slack channel for AB2D incidents

1. Open Slack

1. Select the SemanticBits workspace

1. Create the following Slack channel in the the SemanticBits workspace

   ```
   p-ab2d-incident-response
   ```

### Link slack user with VictorOps

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Open Slack

1. Scroll down to "Apps" in the leftmost panel

1. Select "+" beside "Apps" in the leftmost panel

1. Type the following in the **Search by name or category** text box

   ```
   victorops
   ```

1. Select **VictorOps**

1. Enter the following in the "victorops" channel

   ```
   /victor-linkuser @{slack user}
   ```

1. Select "Linking your Slack user"

1. Note that VictorOps opens in Chrome

1. Note that the following is displayed

   *Format:*

   ```
   Slack and VictorOps Connected!
   Your Slack user ({slack user}) and VictorOps user ({victorops user}) accounts have been successfully linked. Now you can take action on incidents from Slack.
   ```

1. Select **OK** on the "Slack and VictorOps Connected!" window

1. Make sure each technical user links their slack user to VictorOps

### Configure VictorOps alerting service to forward alerts to a dedicated Slack channel using Slack integration

1. Add the person that first integrated SemanticBits with VictorOps to the "p-ab2d-incident-response" slack channel by entering the following in the "p-ab2d-incident-response" slack channel

   ```
   @Clarence
   ```

1. Open Chrome

1. Enter the following in the address bar

   > https://portal.victorops.com/client/cms-ccxp

1. Log on (if not already logged in)

1. Select the **Integrations** tab

1. Type the following in the **Search** text box

   ```
   slack
   ```

1. Note that since we are using CCXP account, Slack is already enabled

1. Note that since we are using CCXP account, the default channel is owned bt CCXP

1. Select **Add Mapping**

1. Configure the "Add Channel Mapping" page as follows

   - **Select an Escalation Policy:** AB2D - Standard

   - **Select a channel to send VictorOps messages to:** p-ab2d-incident-response

   - **Chat Messages (Synced with VictorOps timeline):** checked

   - **On-Call change notifications:** checked

   - **Paging notifications:** checked

   - **Incidents:** checked

1. Select **Save**

1. Open a new Chrome tab

1. Log on to production AWS account

1. Select **SNS**

1. Select **Topics** from the leftmost panel

1. Select the following topic

   ```
   ab2d-east-prod-cloudwatch-alarms
   ```

1. Select **Publish message**

1. Configure the "Message details" section as follows

   - **Subject:** {keep blank}

   - **Time to Live (TTL):** {keep blank}

1. Configure the "Message body" section as follows

   - **Message structure:** Identical payload for all delivery protocols

   - **Message body to send to the endpoint:**

     ```
     {"AlarmName":"VictorOps - CloudWatch Integration TEST","NewStateValue":"ALARM","NewStateReason":"failure","StateChangeTime":"2017-12-14T01:00:00.000Z","AlarmDescription":"VictorOps - CloudWatch Integration TEST"}
     ```

1. Select **Publish message**

1. Verify that the incident appears in the "p-ab2d-incident-response" slack channel

## Configure Cloud Protection Manager

### Ensure that all instances have CPM backup tags

1. Open Chrome

1. Enter the following in the address bar

   > https://cloudtamer.cms.gov/portal

1. Select the target AWS account

   ```
   AB2D Prod
   ```

1. Ensure that the RDS instance has been tagged as follows

   1. Select **RDS**

   1. Select **Databases** from the leftmost panel

   1. Select the following database instance

      ```
      ab2d
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|------------------------
      cpm backup|4HR Daily Weekly Monthly

1. Ensure that the controller has been tagged as follows

   1. Select **EC2**

   1. Select **Instances** under he "Instances" section from the leftmost panel

   1. Select the following EC2 instance

      ```
      ab2d-deployment-controller
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|--------
      cpm backup|no-backup

1. Ensure that an API node has been tagged as follows

   1. Select the following EC2 instance

      ```
      ab2d-east-prod-api
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|------------------------
      cpm backup|4HR Daily Weekly Monthly

1. Ensure that a worker node has been tagged as follows

   1. Select the following EC2 instance

      ```
      ab2d-east-prod-worker
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|------------------------
      cpm backup|4HR Daily Weekly Monthly

### Complete CPM questionnaire

1. Open Chrome

1. Enter the following in the addess bar

   > https://confluence.cms.gov/pages/viewpage.action?spaceKey=GDITAQ&title=CPM+Backup+Setup

1. Select the following under the "Getting Started" section

   ```
   CPM Onboarding Questionnaire.docx
   ```

1. Select **Download**

1. Wait for the download to complete

1. Open the downloaded file

1. Fill out the form as follows

   - **Request Date:** {today's date}

   - **JIRA Ticket No:** {blank} <-- will be updated after ticket is created

   - **Project Name:** AB2D Prod

   - **Requestor Name:** {cms product owner}

   - **Requestor Organization:** {cms product owner organization}

   - **Requestor Phone Number:** {cms product owner phone number}

   - **Requestor Email:** {cms product owner email}

   - **Number of Windows Servers:** 0

   - **Number of Linux Servers:** 4

   - **Provide a description of current...:**

     ```
     RDS Snapshots - Automated backups - Enabled (7 Days)
     EBS Snapshots - Not yet configured
     ```

   - **Choose Snapshot Frequency (4 Hour):** checked

   - **Choose Snapshot Frequency (Daily):** checked

   - **Choose Snapshot Frequency (Weekly):** checked

   - **Choose Snapshot Frequency (Monthly):** checked

   - **Choose Snapshot Frequency (Annually):** unchecked

   - **Backups/snapshots maintained by...:** DevOps Team

   - **Provide email address for automated notification:** {ab2d team email}

   - **Users Requiring Access:** {list DevOps engineer(s), backend engineer(s), and engineering lead}

   - **Comments and Special Instructions:**

     ```
     Should retain snapshots for 7 years.
     ```

1. Save the form with the following name

   ```
   CPM Onboarding Questionnaire - AB2D Prod.docx
   ```

1. Close the form

### Create a CPM ticket in CMS Jira Enterprise

1. Note the latest CPM documentation can be found here

   > https://cloud.cms.gov/10789741/set-up-your-cpm-backup

1. Open Chrome

1. Go to CMS Cloud Support Portal in Jira Enterprise

   > https://jiraent.cms.gov/servicedesk/customer/portal/13

1. Select **CMS Cloud Service Request**

1. Configure the "CMS Cloud Service Request" page as follows

   - **Summary:** Onboard AB2D Prod to Cloud Protection Manager

   - **CMS Business Unit:** OIT

   - **Project Name:** AB2D Prod

   - **Account Alias:** aws-cms-oit-iusg-acct98

   - **Service Category:** Disaster Recovery Services

   - **Task:** Disaster Recovery as a Service

   - **Description:**

     ```
     Onboard AB2D Prod to Cloud Protection Manager

     See attached questionnaire.
     ```

   - **Severity:** Minimal

   - **Urgency:** Low

   - **Reported Source:** Self Service

   - **Requested Due Date:** {blank}

   - **Component/s:** {blank}

   - **Attachment:**

     ```
     {blank} <-- note that questionnaire will be added after ticket is created
     ```

1. Select **Create**

1. Copy the ticket number from the URL to the clipboard

   *Example:*

   ```
   CLDSUPSD-9726
   ```

1. Open the following form

   ```
   CPM Onboarding Questionnaire - AB2D Prod.docx
   ```

1. Update the form as follows

   - **JIRA Ticket No:** {jira ticket number}

1. Save and close the file

1. Return to the jira ticket

1. Select the ticket number link

1. Scroll down to the "Attachements" section

1. Select **browse**

1. Select and add the following file

   ```
   CPM Onboarding Questionnaire - AB2D Prod.docx
   ```
