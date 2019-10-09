# Internal Test Environment Appendices

## Table of Contents

1. [Appendix AA: Retest terraform using existing AMI](#appendix-aa-retest-terraform-using-existing-ami)

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
   $ terraform destroy --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.app --auto-approve
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
   $ terraform apply --var "ami_id=$AMI_ID" --var "current_task_definition_arn=$API_TASK_DEFINITION" --target module.app --auto-approve
   ```
