# Internal Test Environment Appendices

## Table of Contents

1. [Appendix A: Destroy complete environment](#appendix-a-destroy-complete-environment)
1. [Appendix B: Retest terraform using existing AMI](#appendix-b-retest-terraform-using-existing-ami)
1. [Appendix C: Stop and restart the ECS cluster](#appendix-c-stop-and-restart-the-ecs-cluster)

## Appendix A: Destroy complete environment

1. Set the AWS profile for the target environment

   ```ShellSession
   $ source ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo/set-aws-profile.sh
   ```

1. Change to the environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo
   ```

1. Destroy the environment

   *Example to destroy the complete environment:*
   
   ```ShellSession
   $ ./destroy-environment.sh --environment=sbdemo
   ```

   *Example to destroy the environment, but preserve the AMIs:*
   
   ```ShellSession
   $ ./destroy-environment.sh --environment=sbdemo --keep-ami
   ```

   *Example to destroy the environment, but preserve the networking:*
   
   ```ShellSession
   $ ./destroy-environment.sh --environment=sbdemo --keep-networking
   ```

   *Example to destroy the environment, but preserve both the AMIs and the networking:*
   
   ```ShellSession
   $ ./destroy-environment.sh --environment=sbdemo --keep-ami --keep-networking
   ```

1. Turn off "delete protection" for the application load balancer

   1. Get application load balancer ARN

      ```ShellSession
      $ ALB_ARN=$(aws --region us-east-1 elbv2 describe-load-balancers \
        --name=ab2d-sbdemo \
	--query 'LoadBalancers[*].[LoadBalancerArn]' \
        --output text)
      ```

   1. Turn off "delete protection" for the application load balancer
      
      ```ShellSession
      $ aws elbv2 modify-load-balancer-attributes \
        --load-balancer-arn $ALB_ARN \
        --attributes Key=deletion_protection.enabled,Value=false
      ```

1. Destroy the environment of the "worker" module

   ```ShellSession
   $ terraform destroy \
     --target module.worker --auto-approve
   ```

1. Destroy the environment of the "api" module

   ```ShellSession
   $ terraform destroy \
     --target module.api --auto-approve
   ```

1. Note that if you want to retest the api module using this existing AMI and networking, jump to the following section:

   [Appendix B: Retest terraform using existing AMI](#appendix-aa-retest-terraform-using-existing-ami)

1. Destroy the environment of the "db" module

   ```ShellSession
   $ terraform destroy \
     --target module.db --auto-approve
   ```

1. Destroy the changes made via the "efs" module

   ```ShellSession
   $ terraform destroy \
     --target module.efs --auto-approve
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

1. Note that if you want to preserve the AMIs, you can stop here

1. Deregister the application AMI
      
   1. Get ami id for the application AMI

      ```ShellSession
      $ AMI_ID=$(aws --region us-east-1 ec2 describe-images \
        --owners self \
        --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-AMI" \
        --query "Images[*].[ImageId]" \
        --output text)
      ```

   1. Deregister the application AMI
   
      ```ShellSession
      $ aws ec2 deregister-image \
        --image-id $AMI_ID
      ```

1. Deregister the Jenkins AMI
      
   1. Get ami id for the Jenkins AMI

      ```ShellSession
      $ AMI_ID=$(aws --region us-east-1 ec2 describe-images \
        --owners self \
        --filters "Name=tag:Name,Values=AB2D-$CMS_ENV-JENKINS-AMI" \
        --query "Images[*].[ImageId]" \
        --output text)
      ```

   1. Deregister the Jenkins AMI
   
      ```ShellSession
      $ aws ec2 deregister-image \
        --image-id $AMI_ID
      ```

1. Note that if you want to preserve the networking, you can stop here

1. Delete the first NAT gateway

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

1. Delete the second NAT gateway

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

1. Delete the third NAT gateway

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

1. Manually release the first Elastic IP address

   1. Get the allocation id of the first Elastic IP address

      ```ShellSession
      $ NGW_EIP_1_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
        --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-EIP-1" \
        --query 'Addresses[*].[AllocationId]' \
        --output text)
      ```

   1. Release the first Elastic IP address
   
      ```ShellSession
      $ aws --region us-east-1 ec2 release-address \
        --allocation-id $NGW_EIP_1_ALLOCATION_ID
      ```

1. Manually release the second Elastic IP address

   1. Get the allocation id of the first Elastic IP address

      ```ShellSession
      $ NGW_EIP_2_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
        --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-EIP-2" \
        --query 'Addresses[*].[AllocationId]' \
        --output text)
      ```

   1. Release the first Elastic IP address
   
      ```ShellSession
      $ aws --region us-east-1 ec2 release-address \
        --allocation-id $NGW_EIP_2_ALLOCATION_ID
      ```

1. Manually release the third Elastic IP address

   1. Get the allocation id of the first Elastic IP address

      ```ShellSession
      $ NGW_EIP_3_ALLOCATION_ID=$(aws --region us-east-1 ec2 describe-addresses \
        --filter "Name=tag:Name,Values=AB2D-$CMS_ENV-NGW-EIP-3" \
        --query 'Addresses[*].[AllocationId]' \
        --output text)
      ```

   1. Release the first Elastic IP address
   
      ```ShellSession
      $ aws --region us-east-1 ec2 release-address \
        --allocation-id $NGW_EIP_3_ALLOCATION_ID
      ```

1. Manually delete the VPC through the AWS console

   *Note that you can't delete the VPC from the AWS CLI when there are resources within the VPC.*

## Appendix B: Retest terraform using existing AMI

1. If you haven't yet destroyed the existing API module, jump to the following section

   [Appendix A: Destroy complete environment](#appendix-a-destroy-complete-environment)
   
1. Set AWS profile

   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Delete existing log file

   ```ShelSession
   $ rm -f /var/log/terraform/tf.log
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy application components

   ```ShellSession
   $ ./deploy.sh --environment=sbdemo --auto-approve
   ```

## Appendix C: Stop and restart the ECS cluster

1. Connect to the deployment controller instance

   *Format:*

   ```ShellSession
   $ ssh centos@{public ip address of deployment controller}
   ```
   
   *Example:*
   
   ```ShellSession
   $ ssh centos@3.84.160.10
   ```

1. Connect to the first ECS instance from the deployment controller

   *Format:*
   
   ```ShellSession
   $ ssh centos@{private ip address of first ecs instance}
   ```
   
   *Example:*
   
   ```ShellSession
   $ ssh centos@10.124.4.246
   ```

1. Stop the ecs agent on the API ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.246 'docker stop ecs-agent'
   $ ssh centos@10.124.5.116 'docker stop ecs-agent'
   ```

1. Stop the ecs agent on the worker ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.163 'docker stop ecs-agent'
   $ ssh centos@10.124.5.89 'docker stop ecs-agent'
   ```

1. Start the ecs agent on the API ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.246 'docker start ecs-agent'
   $ ssh centos@10.124.5.116 'docker start ecs-agent'
   ```

1. Start the ecs agent on the worker ECS instances

   ```ShellSession
   $ ssh centos@10.124.4.163 'docker start ecs-agent'
   $ ssh centos@10.124.5.89 'docker start ecs-agent'
   ```
