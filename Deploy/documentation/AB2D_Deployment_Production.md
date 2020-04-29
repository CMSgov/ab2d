# AB2D Deployment Production

## Table of Contents

1. [Deploy to production](#deploy-to-production)

## Deploy to production

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

1. Encrypt and upload New Relic configuration file

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

1. Create or update gold disk

   ```ShellSession
   $ ./bash/update-gold-disk.sh
   ```

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

