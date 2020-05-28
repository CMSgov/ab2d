# AB2D Deployment Development

## Table of Contents

1. [Obtain and import dev.ab2d.cms.gov common certificate](#obtain-and-import-devab2dcmsgov-common-certificate)
   * [Download the dev domain certificate and get private key from CMS](#download-the-dev-domain-certificate-and-get-private-key-from-cms)
   * [Import the dev domain certificate into certificate manager](#import-the-dev-domain-certificate-into-certificate-manager)
1. [Deploy to development](#deploy-to-development)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)

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

   *Example for "Prod" environment:*

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

### Create or update application for production

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
