# Internal Test Environment Appendices

## Table of Contents

1. [Appendix A: Destroy complete environment](#appendix-bb-destroy-complete-environment)
1. [Appendix B: Retest terraform using existing AMI](#appendix-aa-retest-terraform-using-existing-ami)

## Appendix A: Destroy complete environment

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

1. Destroy the environment of the "api" module

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

1. Manually delete the empty ECS cluster through the AWS console

   > *** TO DO ***: need to script this with AWS CLI
   
1. Manually delete the VPC through the AWS console

   *Note that you can't delete the VPC from the AWS CLI when there are resources within the VPC.*

1. Note that if you want to retest using this existing AMI, jump to the following section:

   [Appendix B: Retest terraform using existing AMI](#appendix-aa-retest-terraform-using-existing-ami)

1. Manually deregister the AMI through the AWS console

   > *** TO DO ***: need to script this with AWS CLI

## Appendix B: Retest terraform using existing AMI

1. Destroy existing environment by completing the following section first

   [Appendix A: Destroy complete environment](#appendix-bb-destroy-complete-environment)
   
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
