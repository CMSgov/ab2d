# TO DO

## Table of Contents

1. [To do after granted access to AWS account](#to-do-after-granted-access-to-aws-account)
1. [To do later](#to-do-later)

## To do after granted access to AWS account

1. Verify the gold AMI

   ```
   ami-01962ba1fa3692903
   ```
   
1. Open "app.json" for "app" under packer

   ```ShellSession
   $ vim ./Deploy/packer/app/app.json
   ```

1. Update the following

   ```
   "gold_ami": "ami-01962ba1fa3692903"
   "subnet_id": "SUBNET_ID",
   "vpc_id": "VPC_ID",
   ```

1. Open "app.json" for "app" under packer

   ```ShellSession
   $ vim ./Deploy/packer/app/splunk-deploymentclient.conf
   ```

1. Update the following

   ```
   targetUri = DEPLOYMENT_SERVER_PRIVATE_IP:8089
   ```

## To do later

1. Open "provision-app-instance.sh"

   ```ShellSession
   $ vim ./Deploy/packer/app/provision-app-instance.sh
   ```

1. Uncomment and work on the "Install newrelic infrastructure agent" section

   *Note that the section references an encrypted file that is copied from S3.*
