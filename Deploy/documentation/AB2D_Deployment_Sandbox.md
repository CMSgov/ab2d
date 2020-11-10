# AB2D Deployment Sandbox

## Table of Contents

1. [Obtain and import sandbox.ab2d.cms.gov entrust certificate](#obtain-and-import-sandboxab2dcmsgov-entrust-certificate)
   * [Download the sandbox domain certificates and get private key from CMS](#download-the-sandbox-domain-certificates-and-get-private-key-from-cms)
   * [Import the sandbox domain certificate into certificate manager](#import-the-sandbox-domain-certificate-into-certificate-manager)
1. [Deploy to sandbox](#deploy-to-sandbox)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
1. [Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer](#submit-an-internet-dns-change-request-form-to-product-owner-for-the-sandbox-application-load-balancer)
1. [Configure Cloud Protection Manager](#configure-cloud-protection-manager)
   * [Ensure that all instances have CPM backup tags](#ensure-that-all-instances-have-cpm-backup-tags)
   * [Complete CPM questionnaire](#complete-cpm-questionnaire)
   * [Create a CPM ticket in CMS Jira Enterprise](#create-a-cpm-ticket-in-cms-jira-enterprise)
1. [Configure SNS Topic for CloudWatch alarms](#configure-sns-topic-for-cloudwatch-alarms)

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

## Deploy to sandbox

### Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN
   
1. Initialize or verify environment

   ```ShellShession
   $ cd ~/code/ab2d/Deploy \
     && export CMS_ENV_PARAM=ab2d-sbx-sandbox \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export REGION_PARAM=us-east-1 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export CLOUD_TAMER_PARAM=true \
     && ./bash/initialize-environment.sh
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
   2 (Sbx AWS account)
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

1. Change to your "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set gold disk test parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-sbx-sandbox \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge \
     && export OWNER_PARAM=743302140042 \
     && export REGION_PARAM=us-east-1 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export VPC_ID_PARAM=vpc-08dbf3fa96684151c \
     && export CLOUD_TAMER_PARAM=true
   ```

1. Create or update AMI with latest gold disk

   ```ShellSession
   $ ./Deploy/bash/update-gold-disk.sh
   ```

### Create or update infrastructure

1. Change to your "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-sbx-sandbox \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export EC2_INSTANCE_TYPE_CONTROLLER_PARAM=m5.xlarge \
     && export REGION_PARAM=us-east-1 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export CLOUD_TAMER_PARAM=true
   ```

1. Deploy infrastructure
   
   ```ShellSession
   $ ./Deploy/deploy-infrastructure.sh
   ```

### Create or update application for production

1. Change to your "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-sbx-sandbox \
     && export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev \
     && export REGION_PARAM=us-east-1 \
     && export VPC_ID_PARAM=vpc-08dbf3fa96684151c \
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

## Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer

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

1. Fill out the "Requestor Information" as follows

   *Requestor Information:*

   - **Name:** {product owner first name} {product owner last name}

   - **Organization:** {product owner organization}

   - **Email:** {product owner email}

   - **Phone:** {product owner phone}

1. Fill out the "CMS Business Owner Information" as follows

   *CMS Business Owner Information*

   - **Name:** {business owner first name} {business owner last name}

   - **Organization:** {business owner organization}

   - **Email:** {business owner email}

   - **Phone:** {business owner phone}

   - **Reason:** {reason}

1. Fill out the "DNS Change Information" as follows

   *DNS Change Information*

   - **DNS Zone:** cms.gov

   - **Type of change:** CNAME

   - **Actual Change:** sandbox.ab2d.cms.gov CNAME ab2d-sbx-sandbox-{unique id}.us-east-1.elb.amazonaws.com

   - **Change Date & Time:** ASAP

   - **Purpose of the change:** {reason}

1. Print to PDF in order to preserve changes by doing the following

   1. Select the **File** menu

   1. Select **Print**

   1. Select **Save as PDF** from the dropdown at the bottom left of the dialog

   1. Type the following in the **Save As** text box

      ```
      Internet DNS Change Request - sandbox.ab2d.cms.gov - akamai - revised.pdf
      ```

   1. Select **Save**

1. Open Chrome

1. Enter the following in the address bar

   > https://jira.cms.gov/servicedesk/customer/portal/1

1. Select **Internet DNS Support** from the leftmost panel

1. Select **Internet DNS Change**

1. Complete the "Internet DNS Change" page as follows

   - **Summary:** Map existing AB2D sandbox domain to Akamai

   - **Site:** CMS.gov

   - **Component:** OIT

   - **Phone Number:** {product owner phone}

   - **Priority:** Medium - Interferes with my duties, but work can continue

   - **Project Lead:** {cms business owner}

   - **Description:** See attached form

   - **Attachment:** "Internet DNS Change Request - sandbox.ab2d.cms.gov - akamai - revised.pdf"

1. Select **Create**

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
      ab2d-sbx-sandbox-api
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|------------------------
      cpm backup|4HR Daily Weekly Monthly

1. Ensure that a worker node has been tagged as follows

   1. Select the following EC2 instance

      ```
      ab2d-sbx-sandbox-worker
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

   - **Project Name:** AB2D Sandbox

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
   CPM Onboarding Questionnaire - AB2D Sandbox.docx
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

   - **Summary:** Onboard AB2D Sandbox to Cloud Protection Manager

   - **CMS Business Unit:** OIT

   - **Project Name:** AB2D Sandbox

   - **Account Alias:** aws-cms-oit-iusg-acct95

   - **Service Category:** Disaster Recovery Services

   - **Task:** Disaster Recovery as a Service

   - **Description:**

     ```
     Onboard AB2D Sandbox to Cloud Protection Manager

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
   CPM Onboarding Questionnaire - AB2D Sandbox.docx
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
   CPM Onboarding Questionnaire - AB2D Sandbox.docx
   ```

## Configure SNS Topic for CloudWatch alarms

1. Log on to the AWS account

1. Select **Simple Notification Service**

1. Select **Topics** from the leftmost panel

1. Select **Create topic**

1. Configure the "Create topic" page as follows

   - **Name:** ab2d-sbx-sandbox-cloudwatch-alarms

   - **Display name:** ab2d-sbx-sandbox-cloudwatch-alarms

1. Select **Create topic**
