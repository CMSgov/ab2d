# AB2D Deployment Development

## Table of Contents

1. [Obtain and import dev.ab2d.cms.gov common certificate](#obtain-and-import-devab2dcmsgov-common-certificate)
   * [Download the dev domain certificate and get private key from CMS](#download-the-dev-domain-certificate-and-get-private-key-from-cms)
   * [Import the dev domain certificate into certificate manager](#import-the-dev-domain-certificate-into-certificate-manager)
1. [Peer AB2D Dev environment with the BFD Sbx VPC](#peer-ab2d-dev-environment-with-the-bfd-sbx-vpc)
1. [Encrypt BFD keystore and put in S3](#encrypt-bfd-keystore-and-put-in-s3)
1. [Create a keystore for API nodes](#create-a-keystore-for-api-nodes)
1. [Complete Okta test process](#complete-okta-test-process)
   * [Register for Okta test](#register-for-okta-test)
   * [Save the test Okta Client ID and Client Secret for the AB2D administrator account](#save-the-test-okta-client-id-and-client-secret-for-the-ab2d-administrator-account)
1. [Deploy to development](#deploy-to-development)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
   * [Encrypt and upload New Relic configuration file](#encrypt-and-upload-new-relic-configuration-file)
   * [Create or update AMI with latest gold disk](#create-or-update-ami-with-latest-gold-disk)
   * [Create or update infrastructure](#create-or-update-infrastructure)
   * [Create or update application](#create-or-update-application)
1. [Submit an "Internet DNS Change Request Form" to product owner for the development application load balancer](#submit-an-internet-dns-change-request-form-to-product-owner-for-the-development-application-load-balancer)
1. [Configure New Relic](#configure-new-relic)
   * [Create New Relic alerts](#create-new-relic-alerts)
     * [Create a CPU Percent above threshold alert](#create-a-cpu-percent-above-threshold-alert)
     * [Create a Memory Used percentage above threshold alert](#create-a-memory-used-percentage-above-threshold-alert)
     * [Create a Disk Used percentage above threshold alert](#create-a-disk-used-percentage-above-threshold-alert)
1. [Configure SNS Topic for CloudWatch alarms](#configure-sns-topic-for-cloudwatch-alarms)
1. [Configure VictorOps for development](#configure-victorops-for-development)
   * [Create a route key and verify escalation policy](#create-a-route-key-and-verify-escalation-policy)
   * [Forward New Relic alerts to the VictorOps alerting service](#forward-new-relic-alerts-to-the-victorops-alerting-service)
   * [Forward AWS CloudWatch alarms to the VictorOps alerting service](#forward-aws-cloudwatch-alarms-to-the-victorops-alerting-service)
   * [Create a dedicated SemanticBits Slack channel for AB2D incidents](#create-a-dedicated-semanticbits-slack-channel-for-ab2d-incidents)
   * [Configure VictorOps alerting service to forward alerts to a dedicated Slack channel using Slack integration](#configure-victorops-alerting-service-to-forward-alerts-to-a-dedicated-slack-channel-using-slack-integration)
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

   1Password Entry                            |File Name
   -------------------------------------------|--------------------------------------------
   AB2D Dev : Domain Certificate : Certificate|dev_ab2d_cms_gov.crt
   AB2D Dev : Domain Certificate : CSR        |dev_ab2d_cms_gov.csr
   AB2D Dev : Domain Certificate : Private Key|dev_ab2d_cms_gov.key
   AB2D Dev : Domain Certificate : CA         |dev_ab2d_cms_gov_certification_authority.crt

### Import the dev domain certificate into certificate manager

1. Download the following files from 1Password to the "~/Downloads" directory (if not already there)

   1Password Entry                            |File Name
   -------------------------------------------|--------------------------------------------
   AB2D Dev : Domain Certificate : Certificate|dev_ab2d_cms_gov.crt
   AB2D Dev : Domain Certificate : CSR        |dev_ab2d_cms_gov.csr
   AB2D Dev : Domain Certificate : Private Key|dev_ab2d_cms_gov.key
   AB2D Dev : Domain Certificate : CA         |dev_ab2d_cms_gov_certification_authority.crt

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

## Peer AB2D Dev environment with the BFD Sbx VPC

1. Note that peering the lower enviroment to the BFD Sbx (AKA prod-sbx.bfd.cms.gov) is no longer required

## Encrypt BFD keystore and put in S3

1. Get the keystore from 1Password and copy it to the "/tmp" directory

   ```
   /tmp/ab2d_dev_keystore
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
   1 (Dev AWS account)
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
   $ bundle exec rake encrypt_and_put_file_into_s3['/tmp/ab2d_dev_keystore','ab2d-dev-automation']
   ```

1. Verify that you can get the encrypted keystore from S3 and decrypt it
   
   1. Remove existing keyfile from the "/tmp" directory (if exists)

      ```ShellSession
      $ rm -f /tmp/ab2d_dev_keystore
      ```

   1. Get keystore from S3 and decrypt it

      ```ShellSession
      $ bundle exec rake get_file_from_s3_and_decrypt['/tmp/ab2d_dev_keystore','ab2d-dev-automation']
      ```

   1. Verify that both the bfd sandbox and client certificates are present

      ```ShellSession
      $ keytool -list -v -keystore /tmp/ab2d_dev_keystore
      ```

   1. Copy the "AB2D_BFD_KEYSTORE_PASSWORD in Prod" password from 1Password to the clipboard

   1. Enter the keystore password at the "Enter keystore password" prompt

   1. Verify that there are sections for the following two aliases in the keystore list output

      - Alias name: bfd-prod-sbx-selfsigned

      - Alias name: client_data_server_ab2d_prod_certificate

## Create a keystore for API nodes

1. Note that the keystore for API nodes that is used for the lower environments is stored in GitHub

   ```
   api/src/main/resources/ab2d.p12
   ```

## Complete Okta test process

### Register for Okta test

> *** TO DO ***: Need to find out how to access Okta Test

### Save the test Okta Client ID and Client Secret for the AB2D administrator account

> *** TO DO ***: I (Lonnie) didn't do this setup, but assume the process is the same as in Okta Prod

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

1. Change to your "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set gold disk test parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge \
     && export OWNER_PARAM=743302140042 \
     && export REGION_PARAM=us-east-1 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3 \
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
   $ export CMS_ENV_PARAM=ab2d-dev \
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

### Create or update application

1. Change to your "ab2d" repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set parameters

   *Example for standard Dev deployment of one api and one worker node:*
   
   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev \
     && export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev \
     && export REGION_PARAM=us-east-1 \
     && export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge \
     && export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.xlarge \
     && export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=1 \
     && export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=1 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=1 \
     && export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=1 \
     && export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=1 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=1 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export INTERNET_FACING_PARAM=false \
     && export CLOUD_TAMER_PARAM=true
   ``` 

   *Example for temporarily upgrading Dev to two api and two worker nodes:*
   
   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-dev \
     && export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev \
     && export REGION_PARAM=us-east-1 \
     && export VPC_ID_PARAM=vpc-0c6413ec40c5fdac3 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export EC2_INSTANCE_TYPE_API_PARAM=m5.xlarge \
     && export EC2_INSTANCE_TYPE_WORKER_PARAM=m5.4xlarge \
     && export EC2_DESIRED_INSTANCE_COUNT_API_PARAM=2 \
     && export EC2_MINIMUM_INSTANCE_COUNT_API_PARAM=2 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM=2 \
     && export EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM=2 \
     && export EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM=2 \
     && export EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM=2 \
     && export DATABASE_SECRET_DATETIME_PARAM=2020-01-02-09-15-01 \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export INTERNET_FACING_PARAM=false \
     && export CLOUD_TAMER_PARAM=true
   ``` 

1. Deploy application

   ```ShellSession
   $ ./Deploy/bash/deploy-application.sh
   ```

### Submit an "Internet DNS Change Request Form" to product owner for the development application load balancer

> *** TO DO ***

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
   AB2D Dev - CPU Percent Above
   ```

1. Select **Host Metrics** under the "Choose an alert type" section

1. Select **FILTER HOSTS** under the "Narrow down hosts" section

1. Scroll down to **ec2KeyName**

1. Expand **ec2KeyName**

1. Check **ab2d-dev**

1. Collapse **ec2KeyName**

1. Select **FILTER HOSTS** again to collapse attributes

1. Configure the "Define thresholds" section as follows

   - **First dropdown:** CPU %

   - **has a value:** above

   - **percent:** 85

   - **time dropdown:** for at least

   - **minutes:** 5

1. Select **Create a new policy**

1. Configure the "Configuration" section
 
   - **Alert policy - Name your policy:** AB2D Dev - CPU Percent Above

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
   AB2D Dev - Memory Used Percentage Above
   ```

1. Select **Host Metrics** under the "Choose an alert type" section

1. Select **FILTER HOSTS** under the "Narrow down hosts" section

1. Scroll down to **ec2KeyName**

1. Expand **ec2KeyName**

1. Check **ab2d-dev**

1. Collapse **ec2KeyName**

1. Select **FILTER HOSTS** again to collapse attributes

1. Configure the "Define thresholds" section as follows

   - **First dropdown:** Memory Used %

   - **has a value:** above

   - **percent:** 85

   - **time dropdown:** for at least

   - **minutes:** 5

1. Select **Create a new policy**

1. Configure the "Configuration" section
 
   - **Alert policy - Name your policy:** AB2D Dev - Memory Used Percentage Above

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
   AB2D Dev - Disk Used Percentage Above
   ```

1. Select **Host Metrics** under the "Choose an alert type" section

1. Select **FILTER HOSTS** under the "Narrow down hosts" section

1. Scroll down to **ec2KeyName**

1. Expand **ec2KeyName**

1. Check **ab2d-dev**

1. Collapse **ec2KeyName**

1. Select **FILTER HOSTS** again to collapse attributes

1. Configure the "Define thresholds" section as follows

   - **First dropdown:** Disk Used %

   - **has a value:** above

   - **percent:** 85

   - **time dropdown:** for at least

   - **minutes:** 5

1. Select **Create a new policy**

1. Configure the "Configuration" section
 
   - **Alert policy - Name your policy:** AB2D Dev - Disk Used Percentage Above

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

   - **Name:** ab2d-dev-cloudwatch-alarms

   - **Display name:** ab2d-dev-cloudwatch-alarms

1. Select **Create topic**

## Configure VictorOps for development

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
   ab2d-dev
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
   ab2d-dev
   ```

### Forward New Relic alerts to the VictorOps alerting service

1. Open Chrome

1. Open New Relic

1. Select the **Alerts & AI** tab

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

   - **Channel name:** ab2d-dev

   - **Key:** {victors ops api key for new relic}

   - **Route key:** ab2d-dev

1. Select **Create channel**

1. Select the **Alert policies** tab

1. Select **Add alert policies**

1. Check all of the following

   - AB2D Dev - CPU Percent Above

   - AB2D Dev - Disk Used Percentage Above

   - AB2D Dev - Memory Used Percentage Above

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
   ab2d-dev-cloudwatch-alarms
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
   ab2d-dev-cloudwatch-alarms
   ```

1. Select **Publish message**

1. Configure the "Message details" section as follows

   - **Subject:** {keep blank}

   - **Time to Live (TTL):** {keep blank}

1. Configure the "Message body" section as follows

   - **Message structure:** Identical payload for all delivery protocols

   - **Message body to send to the endpoint:**

     ```
     {"AlarmName":"AB2D Dev - VictorOps - CloudWatch Integration TEST","NewStateValue":"ALARM","NewStateReason":"failure","StateChangeTime":"2017-12-14T01:00:00.000Z","AlarmDescription":"VictorOps - CloudWatch Integration TEST"}
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
   ab2d-dev-cloudwatch-alarms
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
