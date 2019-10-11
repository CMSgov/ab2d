# Internal Test Environment Appendices

## Table of Contents

1. [Appendix AA: Retest terraform using existing AMI](#appendix-aa-retest-terraform-using-existing-ami)
1. [Appendix BB: Destroy complete environment](#appendix-bb-destroy-complete-environment)
1. [Appendix CC: Preparation notes](#appendix-cc-preparation-notes)

## Appendix AA: Retest terraform using existing AMI

1. Set AWS profile

   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```
   
1. Set the AMI_ID used by the deployment

   ```ShellSession
   $ export AMI_ID=ami-07d8d582ab4e0b101
   ```

1. Note that "API_TASK_DEFINITION" will be empty in the next step

1. Change to the environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo
   ```

1. Destroy the environment

   ```ShellSession
   $ terraform destroy \
     --var "ami_id=$AMI_ID" \
     --var "current_task_definition_arn=$API_TASK_DEFINITION" \
     --target module.app --auto-approve
   ```

1. Manually destroy any remnants of the deployment that might have been caused by a failed deployment

   *List of components that needs to be deleted manually:*
   
   - cms-ab2d-sbdemo-DatabaseSecurityGroup

1. Note the following logging levels are available

   - TRACE

   - DEBUG

   - INFO

   - WARN

   - ERROR
   
1. Turn on terraform logging

   *Example of logging "WARN" and "ERROR":*
   
   ```ShellSession
   $ export TF_LOG=WARN
   $ export TF_LOG_PATH=/var/log/terraform/tf.log
   ```

   *Example of logging everything:*

   ```ShellSession
   $ export TF_LOG=TRACE
   $ export TF_LOG_PATH=/var/log/terraform/tf.log
   ```

1. Delete existing log file

   ```ShelSession
   $ rm -f /var/log/terraform/tf.log
   ```
   
1. Rerun the terraform apply

   ```ShellSession
   $ terraform apply \
     --var "ami_id=$AMI_ID" \
     --var "current_task_definition_arn=$API_TASK_DEFINITION" \
     --target module.app --auto-approve
   ```

## Appendix BB: Destroy complete environment

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Set CMS environment

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export CMS_ENV="SBDEMO"
   ```

1. Change to the environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo
   ```

1. Manually turn off "delete protection" for the load balancer in the AWS console

   > *** TO DO ***: need to script this with AWS CLI

1. Destroy the environment of the "app" module

   ```ShellSession
   $ terraform destroy \
     --target module.api --auto-approve
   ```

1. Destroy the environment of the "db" module

   ```ShellSession
   $ terraform destroy \
     --target module.db --auto-approve
   ```

1. Destroy the changes made via the "s3" module

   ```ShellSession
   $ terraform destroy \
     --target module.s3 --auto-approve
   ```

1. Destroy the changes made via the "kms" module

   ```ShellSession
   $ terraform destroy \
     --target module.kms --auto-approve
   ```

1. Get the ID of the first NAT gateway

   ```ShellSession
   $ NAT_GW_1_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
     --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-1" "Name=state,Values=available" \
     --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
     --output text)
   ```

1. Delete the first NAT gateway

   ```ShellSession
   $ aws --region us-east-1 ec2 delete-nat-gateway \
     --nat-gateway-id $NAT_GW_1_ID
   ```

1. Get the ID of the second NAT gateway

   ```ShellSession
   $ NAT_GW_2_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
     --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-2" "Name=state,Values=available" \
     --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
     --output text)
   ```

1. Delete the second NAT gateway

   ```ShellSession
   $ aws --region us-east-1 ec2 delete-nat-gateway \
     --nat-gateway-id $NAT_GW_2_ID
   ```

1. Get the ID of the third NAT gateway

   ```ShellSession
   $ NAT_GW_3_ID=$(aws --region us-east-1 ec2 describe-nat-gateways \
     --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-3" "Name=state,Values=available" \
     --query 'NatGateways[*].{NatGatewayId:NatGatewayId}' \
     --output text)
   ```

1. Delete the third NAT gateway

   ```ShellSession
   $ aws --region us-east-1 ec2 delete-nat-gateway \
     --nat-gateway-id $NAT_GW_3_ID
   ```

1. Verify that all three NAT gateways are in the deleted state

   1. Enter the following
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-nat-gateways \
        --filter "Name=state,Values=available,pending,deleting" \
        --query 'NatGateways[*].{NatGatewayId:NatGatewayId,State:State}' \
        --output text
      ```

   1. Verify that there is no output

   1. If there is output, wait for a few minutes and check again before proceeding

1. Manually release all Elastic IP addresses through the AWS console

    > *** TO DO ***: need to script this with AWS CLI

1. Manually deregister the AMI through the AWS console

   > *** TO DO ***: need to script this with AWS CLI

1. Manually delete the empty ECS cluster through the AWS console

   > *** TO DO ***: need to script this with AWS CLI
   
1. Manually delete the VPC through the AWS console

   *Note that you can't delete the VPC from the AWS CLI when there are resources within the VPC.*

## Appendix CC: Preparation notes

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
