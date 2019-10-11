# TO DO

## Table of Contents

1. [To do after granted access to AWS account](#to-do-after-granted-access-to-aws-account)
1. [To do later](#to-do-later)
1. [Preparation notes](#preparation-notes)
1. [Issues](#issues)

## To do after granted access to AWS account

1. Complete "Setup develoment machine" section for AWS access

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

1. Open "variables.tf" for DEV environment

   ```ShellSession
   $ vim ./Deploy/terraform/cms-ab2d-dev/variables.tf
   ```

1. Change the following sections

   ```
   variable "vpc_id" {
     default = "vpc-09a9ad141cc53e39d"
   }

   variable "private_subnet_ids" {
     type        = list(string)
     default     = ["subnet-078ffc37780c010af", "subnet-030e47900032cb0bb", "subnet-0c14499ea66bd9dc1"]
     description = "App instances and DB go here"
   }
   
   variable "deployment_controller_subnet_ids" {
     type        = list(string)
     default     = ["subnet-0169444a442ed9d28", "subnet-0581df111f086f55b", "subnet-091dde638f2bc571d"]
     description = "Deployment controllers go here"
   }

   variable "s3_username_whitelist" {
     default = ["HV7K"]
   }

   variable "gold_image_name" {
     default = "EAST-RH 7-6 Gold Image V.1.09 (HVM) 06-26-19"
   }
   
   variable "enterprise-tools-sec-group-id" {
     default = "sg-0566ad330966d8ba7"
   }
   
   variable "vpn-private-sec-group-id" {
     default = "sg-07fbbd710a8b15851"
   }
   ```
   
## To do later

1. Open "provision-app-instance.sh"

   ```ShellSession
   $ vim ./Deploy/packer/app/provision-app-instance.sh
   ```

1. Uncomment and work on the "Install newrelic infrastructure agent" section

   *Note that the section references an encrypted file that is copied from S3.*

## Preparation notes

1. Change to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Backup the "app.json" file to be used for the government AWS account

   ```ShellSession
   $ cp Deploy/packer/app/app.json Deploy/packer/app/app.json.gov
   ```

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Get the latest CentOS AMI

   ```ShellSession
   $ aws --region us-east-1 ec2 describe-images \
     --owners aws-marketplace \
     --filters Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce \
     --query 'Images[*].[ImageId,CreationDate]' \
     --output text \
     | sort -k2 -r \
     | head -n1
   ```

1. Note the AMI in the output

   *Example:*
   
   ```
   ami-02eac2c0129f6376b
   ```

1. Open the "app.json" file

   ```ShellSession
   $ vim Deploy/packer/app/app.json
   ```

1. Change the following with the noted AMI

   *Example:*
   
   ```
   "gold_ami": "ami-02eac2c0129f6376b"
   ```

1. Change the networking settings based on the VPC that was created

   *Example:*
   
   ```
   "subnet_id": "subnet-0b8ba5ef9b89b07ed",
   "vpc_id": "vpc-064c3621b7205922a",
   ```

1. Change the builders settings

   *Example:*

   ```
   "iam_instance_profile": "lonnie.hanekamp@semanticbits.com",
   "ssh_username": "centos",
   ```

1. Save and close "app.json"

1. Backup the "provision-app-instance.sh" file to be used for the government AWS account

   ```ShellSession
   $ cp Deploy/packer/app/provision-app-instance.sh Deploy/packer/app/provision-app-instance.sh.gov
   ```

1. Open "provision-app-instance.sh"

   ```ShellSession
   $ vim Deploy/packer/app/provision-app-instance.sh
   ```

1. Comment out gold disk related items

   1. Comment out "Update splunk forwarder config" section

      ```
      #
      # LSH Comment out gold disk related section
      #
      # # Update splunk forwarder config
      # sudo chown splunk:splunk /tmp/splunk-deploymentclient.conf
      # sudo mv -f /tmp/splunk-deploymentclient.conf /opt/splunkforwarder/etc/system/local/deploymentclient.conf
      ```

   1. Comment out "Make sure splunk can read all logs" section

      ```
      #
      # LSH Comment out gold disk related section
      #
      # # Make sure splunk can read all logs
      # sudo /opt/splunkforwarder/bin/splunk stop
      # sudo /opt/splunkforwarder/bin/splunk clone-prep-clear-config
      # sudo /opt/splunkforwarder/bin/splunk start
      ```

1. Save and close "provision-app-instance.sh"

## Issues

1. Note and resolve the following output from "deploy.sh" process

   ```
   + terraform taint -allow-missing null_resource.authorized_keys_file
   
   Warning: No such resource instance
   
   Resource instance %s was not found, but this is not an error because
   -allow-missing was set.
   
   + '[' -z true ']'
   + terraform apply --auto-approve --var ami_id=ami-02a271acc8ef134ab --var current_task_definition_arn= -target=null_resource.authorized_keys_file
   
   Apply complete! Resources: 0 added, 0 changed, 0 destroyed.
   + ACTUAL_API_COUNT=0
   + RETRIES_API=0
   + '[' 0 -lt '' ']'
   ./deploy.sh: line 133: [: : integer expression expected
   ```
