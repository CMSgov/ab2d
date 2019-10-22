# Internal Test Environment Appendices

## Table of Contents

1. [Appendix A: Destroy complete environment](#appendix-a-destroy-complete-environment)
1. [Appendix B: Retest terraform using existing AMI](#appendix-b-retest-terraform-using-existing-ami)
1. [Appendix C: Stop and restart the ECS cluster](#appendix-c-stop-and-restart-the-ecs-cluster)

## Appendix A: Destroy complete environment

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
