# TO DO

## Table of Contents

1. [To do after granted access to AWS account](#to-do-after-granted-access-to-aws-account)
1. [To do later](#to-do-later)

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
