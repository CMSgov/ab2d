# AB2D Deployment Production

## Table of Contents

1. [Obtain and import ab2d.cms.gov certificate](#obtain-and-import-ab2dcmsgov-certificate)
   * [Download the AB2D domain certificates and get private key from CMS](#download-the-ab2d-domain-certificates-and-get-private-key-from-cms)
   * [Import the AB2D domain certificate into certificate manager](#import-the-ab2d-domain-certificate-into-certificate-manager)
1. [Create initial AB2D static website](#create-initial-ab2d-static-website)
1. [Create CloudFront distribution](#create-cloudfront-distribution)
1. [Submit an "Internet DNS Change Request Form" to product owner](#submit-an-internet-dns-change-request-form-to-product-owner)
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

   *Example for "Prod" environment:*

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

