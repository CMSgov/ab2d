# AB2D Deployment Implementation

## Table of Contents

1. [Obtain and import impl.ab2d.cms.gov common certificate](#obtain-and-import-implab2dcmsgov-common-certificate)
1. [Deploy to implementation](#deploy-to-implementation)
   * [Initialize or verify base environment](#initialize-or-verify-base-environment)
   * [Encrypt and upload New Relic configuration file](#encrypt-and-upload-new-relic-configuration-file)
   * [Create, encrypt, and upload BFD AB2D keystore](#create-encrypt-and-upload-bfd-ab2d-keystore)
   * [Create or update AMI with latest gold disk](#create-or-update-ami-with-latest-gold-disk)
   * [Create or update infrastructure](#create-or-update-infrastructure)
   * [Create or update application for production](#create-or-update-application-for-production)
1. [Upload static website to an Akamai Upload Directory within Akamai NetStorage](#upload-static-website-to-an-akamai-upload-directory-within-akamai-netstorage)

## Obtain and import impl.ab2d.cms.gov common certificate](#obtain-and-import-implab2dcmsgov-common-certificate)

*Note that I am temporarily using "Appendix XX: Create a self-signed certificate for an EC2 load balancer"*

> *** TO DO ***: get real common cerificate

## Deploy to implementation

### Initialize or verify base environment

1. Ensure that you are connected to CMS Cisco VPN
   
1. Initialize or verify environment

   ```ShellShession
   $ cd ~/code/ab2d/Deploy \
     && export CMS_ENV_PARAM=ab2d-east-impl \
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
   3 (Impl AWS account)
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

1. Note that there is a "bfd-users" slack channel for cmsgov with BFD engineers

1. Create a password in 1Password with the following desrciption

   ```
   AB2D Impl - BFD Prod Sbx - Keystore Password
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
     -keyout client_data_server_ab2d_imp_certificate.key \
     -subj "/CN=ab2d-sbx-client" \
     -out client_data_server_ab2d_imp_certificate.pem
   ```

1. Note that the following two files were created

   - client_data_server_ab2d_imp_certificate.key (private key)

   - client_data_server_ab2d_imp_certificate.pem (self-signed crtificate)

1. Create a keystore that includes the self-signed SSL certificate for AB2D client to BFD Prod sandbox

   ```ShellSession
   $ openssl pkcs12 -export \
     -in client_data_server_ab2d_imp_certificate.pem \
     -inkey client_data_server_ab2d_imp_certificate.key \
     -out ab2d_imp_keystore \
     -name client_data_server_ab2d_imp_certificate
   ```
 
1. Copy the "AB2D Impl - BFD Prod Sbx - Keystore Password" password from 1Password to the clipboard

1. Paste the password at the "Enter Export Password" prompt

1. Paste the password again at the "Verifying - Enter Export Password" prompt

1. Note that the following file has been created

   - ab2d_imp_keystore (keystore)

1. Export the public key from the self-signed SSL certificate for AB2D client to BFD sandbox

   ```ShellSession
   $ openssl x509 -pubkey -noout \
     -in client_data_server_ab2d_imp_certificate.pem \
     > client_data_server_ab2d_imp_certificate.pub
   ```

1. Note that the following file has been created

   - client_data_server_ab2d_imp_certificate.pub (public key)

1. Give the public key associated with the self-signed SSL certificate to a BFD engineer that you find on the "bfd-users" slack channel

   - client_data_server_ab2d_imp_certificate.pub

1. Send output from "prod.bfd.cms.gov" that includes only the certificate to a file

   1. Ensure that you are connected to CMS Cisco VPN

   1. Set AWS environment variables using the CloudTamer API

      ```ShellSession
      $ source ~/code/ab2d/Deploy/bash/set-env.sh
      ```

   1. Enter the number of the desired AWS account where the desired logs reside

      ```
      3 (Impl AWS Account)
      ```

   1. Get certificate from "prod-sbx.bfd.cms.gov"

      ```ShellSession
      $ openssl s_client -connect prod-sbx.bfd.cms.gov:443 \
        2>/dev/null | openssl x509 -text \
        | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' \
        > prod-sbx.bfdcloud.pem
      ```

1. Note that the following file has been created

   - prod-sbx.bfdcloud.pem (certificate from the bfd prod sandbox server)

1. Verify that there is a certificate inside of "prod.bfd.cms.gov.pem"

   ```ShellSession
   $ cat prod-sbx.bfdcloud.pem
   ```

1. Import "prod.bfd.cms.gov" certificate into the keystore
   
   ```ShellSession
   $ keytool -import \
     -alias bfd-prod-sbx-selfsigned \
     -file prod-sbx.bfdcloud.pem \
     -storetype PKCS12 \
     -keystore ab2d_imp_keystore
   ```

1. Copy the "AB2D Impl - BFD Prod Sbx - Keystore Password" password from 1Password to the clipboard

1. Enter the keystore password at the "Enter keystore password" prompt

1. Enter the following at the "Trust this certificate" prompt

   ```
   yes
   ```

1. Verify that both the bfd sandbox and client certificates are present
   
   ```ShellSession
   $ keytool -list -v -keystore ab2d_imp_keystore
   ```

1. Copy the "AB2D Impl - BFD Prod Sbx - Keystore Password" password from 1Password to the clipboard

1. Enter the keystore password at the "Enter keystore password" prompt

1. Verify that there are sections for the following two aliases in the keystore list output
   
   - Alias name: bfd-prod-sbx-selfsigned

   - Alias name: client_data_server_ab2d_imp_certificate

1. Save the keystore, private key, self-signed certificate, and public key in the "ab2d" vault of 1Password

   Label                                             |File
   --------------------------------------------------|-------------------------------------------
   AB2D Impl - BFD Prod Sbx - Keystore               |ab2d_imp_keystore
   AB2D Impl - BFD Prod Sbx - Private Key            |client_data_server_ab2d_imp_certificate.key
   AB2D Impl - BFD Prod Sbx - Self-signed Certificate|client_data_server_ab2d_imp_certificate.pem
   AB2D Impl - BFD Prod Sbx - Public Key             |client_data_server_ab2d_imp_certificate.pub

### Create or update AMI with latest gold disk

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set gold disk test parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-impl \
     && export DEBUG_LEVEL_PARAM=WARN \
     && export EC2_INSTANCE_TYPE_PACKER_PARAM=m5.xlarge \
     && export OWNER_PARAM=743302140042 \
     && export REGION_PARAM=us-east-1 \
     && export SSH_USERNAME_PARAM=ec2-user \
     && export VPC_ID_PARAM=vpc-0e5d2e88de7f9cad4 \
     && export CLOUD_TAMER_PARAM=true
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

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-impl \
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

### Create or update application for production

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Set parameters

   ```ShellSession
   $ export CMS_ENV_PARAM=ab2d-east-impl \
     && export CMS_ECR_REPO_ENV_PARAM=ab2d-mgmt-east-dev \
     && export REGION_PARAM=us-east-1 \
     && export VPC_ID_PARAM=vpc-0e5d2e88de7f9cad4 \
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
     && export INTERNET_FACING_PARAM=true \
     && export CLOUD_TAMER_PARAM=true
   ``` 

1. Deploy application

   ```ShellSession
   $ ./bash/deploy-application.sh
   ```

## Upload static website to an Akamai Upload Directory within Akamai NetStorage

1. Note that these directions assume that the "Create an Akamai Upload Account in Akamai NetStorage" section in the "AB2D_Deployment_Greenfield" document has been completed

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create the static website

   *Akamai Stage (using website pulled from CloudFront):*

   ```ShellSession
   $ export AKAMAI_RSYNC_DOMAIN_PARAM="ab2d.rsync.upload.akamai.com" \
     && export AKAMAI_UPLOAD_DIRECTORY_PARAM="971498" \
     && export GENERATE_WEBSITE_FROM_CODE_PARAM="false" \
     && export NETSTORAGE_SSH_KEY_PARAM="${HOME}/.ssh/ab2d-akamai" \
     && export WEBSITE_DEPLOYMENT_TYPE_PARAM="stage" \
     && export WEBSITE_DIRECTORY_PARAM="${HOME}/akamai-from-cloudfront/_site" \
     && ./bash/create-or-update-website-akamai.sh
   ```

   *Akamai Stage (using website generated from code):*

   ```ShellSession
   $ export AKAMAI_RSYNC_DOMAIN_PARAM="ab2d.rsync.upload.akamai.com" \
     && export AKAMAI_UPLOAD_DIRECTORY_PARAM="971498" \
     && export GENERATE_WEBSITE_FROM_CODE_PARAM="true" \
     && export NETSTORAGE_SSH_KEY_PARAM="${HOME}/.ssh/ab2d-akamai" \
     && export WEBSITE_DEPLOYMENT_TYPE_PARAM="stage" \
     && export WEBSITE_DIRECTORY_PARAM="${HOME}/akamai/_site" \
     && ./bash/create-or-update-website-akamai.sh
   ```

1. Note that the following should have been created in the Akamai Upload Directory

   - timestamped backup of the new website

     **Akamai Stage Example:*

     ```
     /971498/_site_2020-06-18_11-47-48
     ```

   - new website

     *Akamai Stage:*

     ```
     /971498/_site
     ```
