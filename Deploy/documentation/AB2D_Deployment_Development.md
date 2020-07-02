# AB2D Deployment Development

## Table of Contents

1. [Obtain and import dev.ab2d.cms.gov common certificate](#obtain-and-import-devab2dcmsgov-common-certificate)
   * [Download the dev domain certificate and get private key from CMS](#download-the-dev-domain-certificate-and-get-private-key-from-cms)
   * [Import the dev domain certificate into certificate manager](#import-the-dev-domain-certificate-into-certificate-manager)
1. [Deploy to development](#deploy-to-development)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
   * [Encrypt and upload New Relic configuration file](#encrypt-and-upload-new-relic-configuration-file)
   * [Create or update AMI with latest gold disk](#create-or-update-ami-with-latest-gold-disk)
   * [Create or update infrastructure](#create-or-update-infrastructure)
   * [Create or update application](#create-or-update-application)
1. [Configure Cloud Protection Manager](#configure-cloud-protection-manager)
   * [Ensure that all instances have CPM backup tags](#ensure-that-all-instances-have-cpm-backup-tags)
   * [Complete CPM questionnaire](#complete-cpm-questionnaire)
   * [Create a CPM ticket in CMS Jira Enterprise](#create-a-cpm-ticket-in-cms-jira-enterprise)

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

## Deploy to development

### Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN
   
1. Initialize or verify environment

   ```ShellShession
   $ cd ~/code/ab2d/Deploy \
     && export CMS_ENV_PARAM=ab2d-dev \
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
   1 (Dev AWS account)
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

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge
   $ export OWNER_PARAM=743302140042
   $ export REGION_PARAM=us-east-1
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3
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
     --environment=ab2d-dev \
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

### Create or update application

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set parameters

   *Example for standard Dev deployment of one api and one worker node:*
   
   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev
   $ export REGION_PARAM=us-east-1
   $ export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3
   $ export SSH_USERNAME_PARAM=ec2-user
   $ export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge
   $ export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.xlarge
   $ export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=1
   $ export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=1
   $ export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=1
   $ export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=1
   $ export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01
   $ export DEBUG_LEVEL_PARAM=WARN
   $ export INTERNET_FACING_PARAM=false
   $ export CLOUD_TAMER_PARAM=true
   ``` 

   *Example for temporarily upgrading Dev to two api and two worker nodes:*
   
   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev
   $ export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev
   $ export REGION_PARAM=us-east-1
   $ export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3
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
   $ export INTERNET_FACING_PARAM=false
   $ export CLOUD_TAMER_PARAM=true
   ``` 

1. Deploy application

   ```ShellSession
   $ ./bash/deploy-application.sh
   ```

### Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer

> *** TO DO ***

## Configure Cloud Protection Manager

### Ensure that all instances have CPM backup tags

1. Open Chrome

1. Enter the following in the address bar

   > https://cloudtamer.cms.gov/portal

1. Select the target AWS account

   ```
   AB2D Dev
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
      cpm backup|Monthly

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
      ab2d-dev-api
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|------------------------
      cpm backup|Monthly

1. Ensure that a worker node has been tagged as follows

   1. Select the following EC2 instance

      ```
      ab2d-dev-worker
      ```

   1. Select the **Tags** tab

   1. Verify that the following tag and value is configured

      Tags      |Value
      ----------|------------------------
      cpm backup|Monthly

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

   - **Project Name:** AB2D Dev

   - **Requestor Name:** {cms product owner}

   - **Requestor Organization:** {cms product owner organization}

   - **Requestor Phone Number:** {cms product owner phone number}

   - **Requestor Email:** {cms product owner email}

   - **Number of Windows Servers:** 0

   - **Number of Linux Servers:** 2

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
   CPM Onboarding Questionnaire - AB2D Dev.docx
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

   - **Summary:** Onboard AB2D Dev to Cloud Protection Manager

   - **CMS Business Unit:** OIT

   - **Project Name:** AB2D Dev

   - **Account Alias:** aws-cms-oit-iusg-acct66

   - **Service Category:** Disaster Recovery Services

   - **Task:** Disaster Recovery as a Service

   - **Description:**

     ```
     Onboard AB2D Dev to Cloud Protection Manager

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
   CLDSUPSD-9730
   ```

1. Open the following form

   ```
   CPM Onboarding Questionnaire - AB2D Dev.docx
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
   CPM Onboarding Questionnaire - AB2D Dev.docx
   ```
