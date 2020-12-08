# AB2D Management

## Table of Contents

1. [Setup Jenkins master in management AWS account](#setup-jenkins-master-in-management-aws-account)
1. [Setup Jenkins agents in management AWS account](#setup-jenkins-agent-in-management-aws-account)
1. [Configure second ELB volumes for Jenkins nodes](#configure-second-elb-volumes-for-jenkins-nodes)
   * [Configure Jenkins master second ELB volume](#configure-jenkins-master-second-elb-volume)
   * [Configure Jenkins agent 01 second ELB volume](#configure-jenkins-agent-01-second-elb-volume)
   * [Configure Jenkins agent 02 second ELB volume](#configure-jenkins-agent-02-second-elb-volume)
   * [Configure Jenkins agent 03 second ELB volume](#configure-jenkins-agent-03-second-elb-volume)
   * [Configure Jenkins agent 04 second ELB volume](#configure-jenkins-agent-04-second-elb-volume)
   * [Configure Jenkins agent 05 second ELB volume](#configure-jenkins-agent-05-second-elb-volume)
1. [Install Jenkins](#install-jenkins)
1. [Allow jenkins users to use a login shell](#allow-jenkins-users-to-use-a-login-shell)
   * [Modify the jenkins user on Jenkins master to allow it to use a login shell](#modify-the-jenkins-user-on-jenkins-master-to-allow-it-to-use-a-login-shell)
   * [Create a jenkins user on Jenkins agent 01 and allow it to use a login shell](#create-a-jenkins-user-on-jenkins-agent-01-and-allow-it-to-use-a-login-shell)
   * [Create a jenkins user on Jenkins agent 02 and allow it to use a login shell](#create-a-jenkins-user-on-jenkins-agent-02-and-allow-it-to-use-a-login-shell)
   * [Create a jenkins user on Jenkins agent 03 and allow it to use a login shell](#create-a-jenkins-user-on-jenkins-agent-03-and-allow-it-to-use-a-login-shell)
   * [Create a jenkins user on Jenkins agent 04 and allow it to use a login shell](#create-a-jenkins-user-on-jenkins-agent-04-and-allow-it-to-use-a-login-shell)
   * [Create a jenkins user on Jenkins agent 05 and allow it to use a login shell](#create-a-jenkins-user-on-jenkins-agent-05-and-allow-it-to-use-a-login-shell)
1. [Set up SSH communication for jenkins](#set-up-ssh-communication-for-jenkins)
   * [Create or use existing Jenkins compatible key pair](#create-or-use-existing-jenkins-compatible-key-pair)
   * [Set up SSH communication for Jenkins master](#set-up-ssh-communication-for-jenkins-master)
     * [Authorize jenkins master to SSH into itself with default key](#authorize-jenkins-master-to-ssh-into-itself-with-default-key)
     * [Authorize jenkins master to SSH into itself with Jenkins compatible key](#authorize-jenkins-master-to-ssh-into-itself-with-jenkins-compatible-key)
     * [Verify that jenkins master can SSH into itself](#verify-that-jenkins-master-can-ssh-into-itself)
     * [Authorize jenkins master to SSH into Jenkins agent 01 with default key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-01-with-default-key)
     * [Authorize jenkins master to SSH into Jenkins agent 01 with Jenkins compatible key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-01-with-jenkins-compatible-key)
     * [Verify that jenkins master can SSH into Jenkins agent 01](#verify-that-jenkins-master-can-ssh-into-jenkins-agent-01)
     * [Authorize jenkins master to SSH into Jenkins agent 02 with default key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-02-with-default-key)
     * [Authorize jenkins master to SSH into Jenkins agent 02 with Jenkins compatible key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-02-with-jenkins-compatible-key)
     * [Verify that jenkins master can SSH into Jenkins agent 02](#verify-that-jenkins-master-can-ssh-into-jenkins-agent-02)
     * [Authorize jenkins master to SSH into Jenkins agent 03 with default key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-03-with-default-key)
     * [Authorize jenkins master to SSH into Jenkins agent 03 with Jenkins compatible key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-03-with-jenkins-compatible-key)
     * [Verify that jenkins master can SSH into Jenkins agent 03](#verify-that-jenkins-master-can-ssh-into-jenkins-agent-03)
     * [Authorize jenkins master to SSH into Jenkins agent 04 with default key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-04-with-default-key)
     * [Authorize jenkins master to SSH into Jenkins agent 04 with Jenkins compatible key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-04-with-jenkins-compatible-key)
     * [Verify that jenkins master can SSH into Jenkins agent 04](#verify-that-jenkins-master-can-ssh-into-jenkins-agent-04)
     * [Authorize jenkins master to SSH into Jenkins agent 05 with default key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-05-with-default-key)
     * [Authorize jenkins master to SSH into Jenkins agent 05 with Jenkins compatible key](#authorize-jenkins-master-to-ssh-into-jenkins-agent-05-with-jenkins-compatible-key)
     * [Verify that jenkins master can SSH into Jenkins agent 05](#verify-that-jenkins-master-can-ssh-into-jenkins-agent-05)
1. [Upgrade docker compose on all Jenkins nodes to the latest version](#upgrade-docker-compose-on-all-jenkins-nodes-to-the-latest-version)
1. [Install htop on Jenkins agents](#install-htop-on-jenkins-agents)
1. [Configure Jenkins master](#configure-jenkins-master)
   * [Enable Jenkins](#enable-jenkins)
   * [Initialize the Jenkins GUI](#initialize-the-jenkins-gui)
1. [Configure Jenkins agent 01](#configure-jenkins-agent-01)
   * [Install development tools on Jenkins agent 01](#install-development-tools-on-jenkins-agent-01)
   * [Create a directory for jobs on Jenkins agent 01](#create-a-directory-for-jobs-on-jenkins-agent-01)
   * [Install python3, pip3, and required pip modules on Jenkins agent 01](#install-python3-pip3-and-required-pip-modules-on-jenkins-agent-01)
   * [Install Terraform on Jenkins agent 01](#install-terraform-on-jenkins-agent-01)
   * [Configure Terraform logging on Jenkins agent 01](#configure-terraform-logging-on-jenkins-agent-01)
   * [Install maven on Jenkins agent 01](#install-maven-on-jenkins-agent-01)
   * [Install jq on Jenkins agent 01](#install-jq-on-jenkins-agent-01)
   * [Add the jenkins user to the docker group on Jenkins agent 01](#add-the-jenkins-user-to-the-docker-group-on-jenkins-agent-01)
   * [Ensure jenkins agent 01 can use the Unix socket for the Docker daemon](#ensure-jenkins-agent-01-can-use-the-unix-socket-for-the-docker-daemon)
   * [Install packer on Jenkins agent 01](#install-packer-on-jenkins-agent-01)
   * [Install Postgres 11 on Jenkins agent 01](#install-postgres-11-on-jenkins-agent-01)
   * [Setup ruby environment on Jenkins agent 01](#setup-ruby-environment-on-jenkins-agent-01)
   * [Install postgresql10-devel on Jenkins agent 01](#install-postgresql10-devel-on-jenkins-agent-01)
1. [Configure Jenkins agent 02](#configure-jenkins-agent-02)
   * [Install development tools on Jenkins agent 02](#install-development-tools-on-jenkins-agent-02)
   * [Create a directory for jobs on Jenkins agent 02](#create-a-directory-for-jobs-on-jenkins-agent-02)
   * [Install python3, pip3, and required pip modules on Jenkins agent 02](#install-python3-pip3-and-required-pip-modules-on-jenkins-agent-02)
   * [Install Terraform on Jenkins agent 02](#install-terraform-on-jenkins-agent-02)
   * [Configure Terraform logging on Jenkins agent 02](#configure-terraform-logging-on-jenkins-agent-02)
   * [Install maven on Jenkins agent 02](#install-maven-on-jenkins-agent-02)
   * [Install jq on Jenkins agent 02](#install-jq-on-jenkins-agent-02)
   * [Add the jenkins user to the docker group on Jenkins agent 02](#add-the-jenkins-user-to-the-docker-group-on-jenkins-agent-02)
   * [Ensure jenkins agent 02 can use the Unix socket for the Docker daemon](#ensure-jenkins-agent-02-can-use-the-unix-socket-for-the-docker-daemon)
   * [Install packer on Jenkins agent 02](#install-packer-on-jenkins-agent-02)
   * [Install Postgres 11 on Jenkins agent 02](#install-postgres-11-on-jenkins-agent-02)
   * [Setup ruby environment on Jenkins agent 02](#setup-ruby-environment-on-jenkins-agent-02)
   * [Install postgresql10-devel on Jenkins agent 02](#install-postgresql10-devel-on-jenkins-agent-02)
1. [Configure Jenkins agent 03](#configure-jenkins-agent-03)
   * [Install development tools on Jenkins agent 03](#install-development-tools-on-jenkins-agent-03)
   * [Create a directory for jobs on Jenkins agent 03](#create-a-directory-for-jobs-on-jenkins-agent-03)
   * [Install python3, pip3, and required pip modules on Jenkins agent 03](#install-python3-pip3-and-required-pip-modules-on-jenkins-agent-03)
   * [Install Terraform on Jenkins agent 03](#install-terraform-on-jenkins-agent-03)
   * [Configure Terraform logging on Jenkins agent 03](#configure-terraform-logging-on-jenkins-agent-03)
   * [Install maven on Jenkins agent 03](#install-maven-on-jenkins-agent-03)
   * [Install jq on Jenkins agent 03](#install-jq-on-jenkins-agent-03)
   * [Add the jenkins user to the docker group on Jenkins agent 03](#add-the-jenkins-user-to-the-docker-group-on-jenkins-agent-03)
   * [Ensure jenkins agent 03 can use the Unix socket for the Docker daemon](#ensure-jenkins-agent-03-can-use-the-unix-socket-for-the-docker-daemon)
   * [Install packer on Jenkins agent 03](#install-packer-on-jenkins-agent-03)
   * [Install Postgres 11 on Jenkins agent 03](#install-postgres-11-on-jenkins-agent-03)
   * [Setup ruby environment on Jenkins agent 03](#setup-ruby-environment-on-jenkins-agent-03)
   * [Install postgresql10-devel on Jenkins agent 03](#install-postgresql10-devel-on-jenkins-agent-03)
1. [Configure Jenkins agent 04](#configure-jenkins-agent-04)
   * [Install development tools on Jenkins agent 04](#install-development-tools-on-jenkins-agent-04)
   * [Create a directory for jobs on Jenkins agent 04](#create-a-directory-for-jobs-on-jenkins-agent-04)
   * [Install python3, pip3, and required pip modules on Jenkins agent 04](#install-python3-pip3-and-required-pip-modules-on-jenkins-agent-04)
   * [Install Terraform on Jenkins agent 04](#install-terraform-on-jenkins-agent-04)
   * [Configure Terraform logging on Jenkins agent 04](#configure-terraform-logging-on-jenkins-agent-04)
   * [Install maven on Jenkins agent 04](#install-maven-on-jenkins-agent-04)
   * [Install jq on Jenkins agent 04](#install-jq-on-jenkins-agent-04)
   * [Add the jenkins user to the docker group on Jenkins agent 04](#add-the-jenkins-user-to-the-docker-group-on-jenkins-agent-04)
   * [Ensure jenkins agent 04 can use the Unix socket for the Docker daemon](#ensure-jenkins-agent-04-can-use-the-unix-socket-for-the-docker-daemon)
   * [Install packer on Jenkins agent 04](#install-packer-on-jenkins-agent-04)
   * [Install Postgres 11 on Jenkins agent 04](#install-postgres-11-on-jenkins-agent-04)
   * [Setup ruby environment on Jenkins agent 04](#setup-ruby-environment-on-jenkins-agent-04)
   * [Install postgresql10-devel on Jenkins agent 04](#install-postgresql10-devel-on-jenkins-agent-04)
1. [Configure Jenkins agent 05](#configure-jenkins-agent-05)
   * [Install development tools on Jenkins agent 05](#install-development-tools-on-jenkins-agent-05)
   * [Create a directory for jobs on Jenkins agent 05](#create-a-directory-for-jobs-on-jenkins-agent-05)
   * [Install python3, pip3, and required pip modules on Jenkins agent 05](#install-python3-pip3-and-required-pip-modules-on-jenkins-agent-05)
   * [Install Terraform on Jenkins agent 05](#install-terraform-on-jenkins-agent-05)
   * [Configure Terraform logging on Jenkins agent 05](#configure-terraform-logging-on-jenkins-agent-05)
   * [Install maven on Jenkins agent 05](#install-maven-on-jenkins-agent-05)
   * [Install jq on Jenkins agent 05](#install-jq-on-jenkins-agent-05)
   * [Add the jenkins user to the docker group on Jenkins agent 05](#add-the-jenkins-user-to-the-docker-group-on-jenkins-agent-05)
   * [Ensure jenkins agent 05 can use the Unix socket for the Docker daemon](#ensure-jenkins-agent-05-can-use-the-unix-socket-for-the-docker-daemon)
   * [Install packer on Jenkins agent 05](#install-packer-on-jenkins-agent-05)
   * [Install Postgres 11 on Jenkins agent 05](#install-postgres-11-on-jenkins-agent-05)
   * [Setup ruby environment on Jenkins agent 05](#setup-ruby-environment-on-jenkins-agent-05)
   * [Install postgresql10-devel on Jenkins agent 05](#install-postgresql10-devel-on-jenkins-agent-05)
1. [Create GitHub user for Jenkins automation](#create-github-user-for-jenkins-automation)
1. [Configure Jenkins for AB2D](#configure-jenkins-for-ab2d)
   * [Configure jenkins SSH credentials](#configure-jenkins-ssh-credentials)
   * [Configure "personal access token" public GitHub credentials](#configure-personal-access-token-public-github-credentials)
   * [Configure public GitHub credentials](#configure-public-github-credentials)
   * [Install the SSH plugin](#install-the-ssh-plugin)
   * [Configure the SSH plugin](#configure-the-ssh-plugin)
   * [Install the "Scheduled Build" plugin](#install-the-scheduled-build-plugin)
   * [Configure GitHub plugin](#configure-github-plugin)
   * [Install and configure the Authorize Project plugin](#install-and-configure-the-authorize-project-plugin)
   * [Register a new OAuth application with GitHub](#register-a-new-oauth-application-with-github)
   * [Install and configure the GitHub Authentication plugin including adding GitHub users](#install-and-configure-the-github-authentication-plugin-including-adding-github-users)
   * [Configure global tool configuration](#configure-global-tool-configuration)
   * [Log on to Jenkins using GitHub OAuth authentication](#log-on-to-jenkins-using-github-oauth-authentication)
   * [Install the "Parameterized Trigger" plugin](#install-the-parameterized-trigger-plugin)
   * [Install or verify the Docker Commons plugin](#install-or-verify-the-docker-commons-plugin)
   * [Install or verify the Docker Pipeline plugin](#install-or-verify-the-docker-pipeline-plugin)
   * [Configure Jenkins secrets used by builds](#configure-jenkins-secrets-used-by-builds)
   * [Add Jenkins agent 01](#add-jenkins-agent-01)
   * [Add Jenkins agent 02](#add-jenkins-agent-02)
   * [Add Jenkins agent 03](#add-jenkins-agent-03)
   * [Add Jenkins agent 04](#add-jenkins-agent-04)
   * [Add Jenkins agent 05](#add-jenkins-agent-05)
   * [Create a "development" folder in Jenkins](#create-a-development-folder-in-jenkins)
   * [Configure a Jenkins project for development application deploy](#configure-a-jenkins-project-for-development-application-deploy)
   * [Create a "sandbox" folder in Jenkins](#create-a-sandbox-folder-in-jenkins)
   * [Configure a Jenkins project for sandbox application deploy](#configure-a-jenkins-project-for-sandbox-application-deploy)
1. [Dismiss Jenkins SYSTEM user warning](#dismiss-jenkins-system-user-warning)
1. [Configure executors for Jenkins master](#configure-executors-for-jenkins-master)
1. [Configure Jenkins CLI](#configure-jenkins-cli)
1. [Setup Webhook payload delivery service as an intermediary between firewalled Jenkins and GitHub](#setup-webhook-payload-delivery-service-as-an-intermediary-between-firewalled-jenkins-and-github)
1. [Configure Multibranch Pipeline](#configure-multibranch-pipeline)

## Setup Jenkins master in management AWS account

1. If there is an existing "ab2d-jenkins-master-ami AMI", rename it in the AWS management account as follows

   *Format:*

   ```
   ab2d-jenkins-master-ami-YYYY-MM-DD
   ```

   *Example:*

   ```
   ab2d-jenkins-master-ami-2020-03-18
   ```

1. If there is an existing "ab2d-jenkins-master" EC2 instance, rename it in the AWS management account as follows

   ```
   ab2d-jenkins-master-old
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Create EC2 instance for Jenkins master

   ```ShellSession
   $ ./Deploy/bash/deploy-jenkins-master.sh
   ```
   
1. Wait for the automation to complete

## Setup Jenkins agents in management AWS account

1. If there is an existing "ab2d-jenkins-master-ami AMI", rename it in the AWS management account as follows

   *Format:*

   ```
   ab2d-jenkins-agent-ami-YYYY-MM-DD
   ```

   *Example:*

   ```
   ab2d-jenkins-agent-ami-2020-03-18
   ```

1. If there is an existing "ab2d-jenkins-agent-XX" EC2 instance that you want to replace, rename it in the AWS management account as follows

   *Format:*

   ```
   ab2d-jenkins-agent-XX-old
   ```

   *Example:*

   ```
   ab2d-jenkins-agent-01-old
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory again

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Create EC2 instance for Jenkins agent 01 (used for deployments)

   ```ShellSession
   $ export EC2_INSTANCE_TAG_PARAM="ab2d-jenkins-agent-01" \
     && export EC2_INSTANCE_TYPE_PARAM="m5.xlarge" \
     && ./Deploy/bash/deploy-jenkins-agent.sh
   ```

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get EC2 instance id of Jenkins agent 01

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```
   
1. Ensure that Jenkins agent 01 is running and has passed status checks before proceeding

   1. Enter the following
   
      ```ShellSession
      $ echo "*************" \
        && echo "InstanceState" \
        && echo "*************" \
        && aws ec2 describe-instance-status \
        --instance-ids "${EC2_INSTANCE_ID}" \
        --query "InstanceStatuses[*].InstanceState.Name" \
        --output text \
        && echo "**************" \
        && echo "InstanceStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].InstanceStatus.Details[*].Status" \
           --output text \
        && echo "**************" \
        && echo "SystemStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].SystemStatus.Details[*].Status" \
           --output text
      ```

   1. Verify that you get the following output

      ```
      *************
      InstanceState
      *************
      running
      **************
      InstanceStatus
      **************
      passed
      **************
      SystemStatus
      **************
      passed
      ```

1. Create EC2 instance for Jenkins agent 02 (used for builds)

   ```ShellSession
   $ export EC2_INSTANCE_TAG_PARAM="ab2d-jenkins-agent-02" \
     && export EC2_INSTANCE_TYPE_PARAM="m5.large" \
     && ./Deploy/bash/deploy-jenkins-agent.sh
   ```

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get EC2 instance id of Jenkins agent 02

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```
   
1. Ensure that Jenkins agent 02 is running and has passed status checks before proceeding

   1. Enter the following
   
      ```ShellSession
      $ echo "*************" \
        && echo "InstanceState" \
        && echo "*************" \
        && aws ec2 describe-instance-status \
        --instance-ids "${EC2_INSTANCE_ID}" \
        --query "InstanceStatuses[*].InstanceState.Name" \
        --output text \
        && echo "**************" \
        && echo "InstanceStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].InstanceStatus.Details[*].Status" \
           --output text \
        && echo "**************" \
        && echo "SystemStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].SystemStatus.Details[*].Status" \
           --output text
      ```

   1. Verify that you get the following output

      ```
      *************
      InstanceState
      *************
      running
      **************
      InstanceStatus
      **************
      passed
      **************
      SystemStatus
      **************
      passed
      ```

1. Create EC2 instance for Jenkins agent 03 (used for builds)

   ```ShellSession
   $ export EC2_INSTANCE_TAG_PARAM="ab2d-jenkins-agent-03" \
     && export EC2_INSTANCE_TYPE_PARAM="m5.large" \
     && ./Deploy/bash/deploy-jenkins-agent.sh
   ```

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get EC2 instance id of Jenkins agent 03

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```
   
1. Ensure that Jenkins agent 03 is running and has passed status checks before proceeding

   1. Enter the following
   
      ```ShellSession
      $ echo "*************" \
        && echo "InstanceState" \
        && echo "*************" \
        && aws ec2 describe-instance-status \
        --instance-ids "${EC2_INSTANCE_ID}" \
        --query "InstanceStatuses[*].InstanceState.Name" \
        --output text \
        && echo "**************" \
        && echo "InstanceStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].InstanceStatus.Details[*].Status" \
           --output text \
        && echo "**************" \
        && echo "SystemStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].SystemStatus.Details[*].Status" \
           --output text
      ```

   1. Verify that you get the following output

      ```
      *************
      InstanceState
      *************
      running
      **************
      InstanceStatus
      **************
      passed
      **************
      SystemStatus
      **************
      passed
      ```

1. Create EC2 instance for Jenkins agent 04 (used for builds)

   ```ShellSession
   $ export EC2_INSTANCE_TAG_PARAM="ab2d-jenkins-agent-04" \
     && export EC2_INSTANCE_TYPE_PARAM="m5.large" \
     && ./Deploy/bash/deploy-jenkins-agent.sh
   ```

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get EC2 instance id of Jenkins agent 04

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```
   
1. Ensure that Jenkins agent 04 is running and has passed status checks before proceeding

   1. Enter the following
   
      ```ShellSession
      $ echo "*************" \
        && echo "InstanceState" \
        && echo "*************" \
        && aws ec2 describe-instance-status \
        --instance-ids "${EC2_INSTANCE_ID}" \
        --query "InstanceStatuses[*].InstanceState.Name" \
        --output text \
        && echo "**************" \
        && echo "InstanceStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].InstanceStatus.Details[*].Status" \
           --output text \
        && echo "**************" \
        && echo "SystemStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].SystemStatus.Details[*].Status" \
           --output text
      ```

   1. Verify that you get the following output

      ```
      *************
      InstanceState
      *************
      running
      **************
      InstanceStatus
      **************
      passed
      **************
      SystemStatus
      **************
      passed
      ```

1. Create EC2 instance for Jenkins agent 05 (used for builds)

   ```ShellSession
   $ export EC2_INSTANCE_TAG_PARAM="ab2d-jenkins-agent-05" \
     && export EC2_INSTANCE_TYPE_PARAM="m5.large" \
     && ./Deploy/bash/deploy-jenkins-agent.sh
   ```

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get EC2 instance id of Jenkins agent 05

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```
   
1. Ensure that Jenkins agent 05 is running and has passed status checks before proceeding

   1. Enter the following
   
      ```ShellSession
      $ echo "*************" \
        && echo "InstanceState" \
        && echo "*************" \
        && aws ec2 describe-instance-status \
        --instance-ids "${EC2_INSTANCE_ID}" \
        --query "InstanceStatuses[*].InstanceState.Name" \
        --output text \
        && echo "**************" \
        && echo "InstanceStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].InstanceStatus.Details[*].Status" \
           --output text \
        && echo "**************" \
        && echo "SystemStatus" \
        && echo "**************" \
        && aws ec2 describe-instance-status \
           --instance-ids "${EC2_INSTANCE_ID}" \
           --query "InstanceStatuses[*].SystemStatus.Details[*].Status" \
           --output text
      ```

   1. Verify that you get the following output

      ```
      *************
      InstanceState
      *************
      running
      **************
      InstanceStatus
      **************
      passed
      **************
      SystemStatus
      **************
      passed
      ```

## Configure second ELB volumes for Jenkins nodes

### Configure Jenkins master second ELB volume

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get availability zone of Jenkins master

   ```ShellSession
   $ JENKINS_MASTER_AZ=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query "Reservations[*].Instances[*].Placement.AvailabilityZone" \
     --output text)
   ```

1. Create volume for Jenkins master

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 create-volume \
     --size 250 \
     --availability-zone "${JENKINS_MASTER_AZ}" \
     --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=ab2d-jenkins-master-second-vol}]'
   ```

1. Ensure the status of "ab2d-jenkins-master-second-vol" volume is "available" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master-second-vol" \
     --query "Volumes[*].State" \
     --output text
   ```

1. Get EC2 instance id of Jenkins master

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```

1. Get volume ID of "ab2d-jenkins-master-second-vol" volume

   ```ShellSession
   $ VOLUME_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master-second-vol" \
     --query "Volumes[*].VolumeId" \
     --output text)
   ```

1. Attach "ab2d-jenkins-master-second-vol" volume to Jenkins master

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 attach-volume \
     --device "/dev/sdf" \
     --instance-id "${EC2_INSTANCE_ID}" \
     --volume-id "${VOLUME_ID}"
   ```

1. Ensure the status of "ab2d-jenkins-master-second-vol" volume is "attached" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master-second-vol" \
     --query "Volumes[*].Attachments[*].State" \
     --output text
   ```

1. Get private ip address of Jenkins master

   ```ShellSession
   $ JENKINS_MASTER_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins master

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_MASTER_PRIVATE_IP}"
   ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Create volume group
   
   ```ShellSession
   $ sudo vgcreate VolGroup00 /dev/nvme1n1p1
   ```

1. Create new "/var/log" mount

   1. Create log logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 145G -n logVol VolGroup00
      ```
   
   1. Create file system on log logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/logVol
      ```
   
   1. Create a temporary mount point for log
   
      ```ShellSession
      $ sudo mkdir -p /mnt/log
      ```
   
   1. Mount logical volume to temporary mount point for log
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /mnt/log
      ```
   
   1. Stop rsyslog
   
      ```ShellSession
      $ sudo systemctl stop rsyslog
      ```
      
   1. Sync existing log directory to temporary log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/* /mnt/log/
      ```
   
   1. Move the original log directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log /var/log.deleteme
      ```
   
   1. Create new log directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log
      ```
   
   1. Mount logical volume to the new log directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /var/log
      ```
   
   1. Sync the backup log data to the new log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log.deleteme/* /var/log/
      ```
   
   1. Restore SELinux Context for log
   
      ```ShellSession
      $ sudo restorecon -rv /var/log
      ```
   
   1. Restart rsyslog
   
      ```ShellSession
      $ sudo systemctl start rsyslog
      ```
   
   1. Add the log mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-logVol            /var/log                 xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file
   
1. Create new "/var/lib" mount

   1. Create lib logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 100G -n libVol VolGroup00
      ```
   
   1. Create file system on lib logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/libVol
      ```
   
   1. Create a temporary mount point for lib
   
      ```ShellSession
      $ sudo mkdir -p /mnt/lib
      ```
   
   1. Mount logical volume to temporary mount point for lib
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /mnt/lib
      ```
       
   1. Sync existing lib directory to temporary lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib/* /mnt/lib/
      ```
   
   1. Move the original lib directory out of the way
   
      ```ShellSession
      $ sudo mv /var/lib /var/lib.deleteme
      ```
   
   1. Create new lib directory
   
      ```ShellSession
      $ sudo mkdir -p /var/lib
      ```
   
   1. Mount logical volume to the new lib directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /var/lib
      ```
   
   1. Sync the backup lib data to the new lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib.deleteme/* /var/lib/
      ```
   
   1. Restore SELinux Context for lib
   
      ```ShellSession
      $ sudo restorecon -rv /var/lib
      ```
   
   1. Add the lib mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-libVol            /var/lib                 xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Create new "/var/log/audit" mount

   1. Create audit logical volume
   
      ```ShellSession
      $ sudo lvcreate -l +100%FREE -n auditVol VolGroup00
      ```
   
   1. Create file system on audit logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/auditVol
      ```
   
   1. Create a temporary mount point for audit
   
      ```ShellSession
      $ sudo mkdir -p /mnt/audit
      ```
   
   1. Mount logical volume to temporary mount point for audit
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /mnt/audit
      ```
       
   1. Sync existing audit directory to temporary audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit/ /mnt/audit/
      ```
   
   1. Move the original audit directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log/audit /var/log/audit.deleteme
      ```
   
   1. Create new audit directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log/audit
      ```
   
   1. Mount logical volume to the new audit directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /var/log/audit
      ```
   
   1. Sync the backup audit data to the new audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit.deleteme/ /var/log/audit/
      ```
   
   1. Restore SELinux Context for audit
   
      ```ShellSession
      $ sudo restorecon -rv /var/log/audit
      ```
   
   1. Add the audit mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-auditVol          /var/log/audit     xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

### Configure Jenkins agent 01 second ELB volume

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get availability zone of Jenkins agent 01

   ```ShellSession
   $ JENKINS_AGENT_01_AZ=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
     --query "Reservations[*].Instances[*].Placement.AvailabilityZone" \
     --output text)
   ```

1. Create volume for Jenkins agent 01

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 create-volume \
     --size 1000 \
     --availability-zone "${JENKINS_AGENT_01_AZ}" \
     --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=ab2d-jenkins-agent-01-second-vol}]'
   ```

1. Note the output

   ```
   {
       "AvailabilityZone": "us-east-1a",
       "CreateTime": "2020-10-05T18:42:14+00:00",
       "Encrypted": false,
       "Size": 1000,
       "SnapshotId": "",
       "State": "creating",
       "VolumeId": "vol-0fd3fc15c5deddb87",
       "Iops": 3000,
       "Tags": [
           {
               "Key": "Name",
               "Value": "ab2d-jenkins-agent-01-second-vol"
           }
       ],
       "VolumeType": "gp2",
       "MultiAttachEnabled": false
   }
   ```

1. Ensure the status of "ab2d-jenkins-agent-01-second-vol" volume is "available" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01-second-vol" \
     --query "Volumes[*].State" \
     --output text
   ```

1. Get EC2 instance id of Jenkins agent 01

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```

1. Get volume ID of "ab2d-jenkins-agent-01-second-vol" volume

   ```ShellSession
   $ VOLUME_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01-second-vol" \
     --query "Volumes[*].VolumeId" \
     --output text)
   ```

1. Attach "ab2d-jenkins-agent-01-second-vol" volume to Jenkins agent 01

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 attach-volume \
     --device "/dev/sdf" \
     --instance-id "${EC2_INSTANCE_ID}" \
     --volume-id "${VOLUME_ID}"
   ```

1. Ensure the status of "ab2d-jenkins-agent-01-second-vol" volume is "attached" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01-second-vol" \
     --query "Volumes[*].Attachments[*].State" \
     --output text
   ```

1. Get private ip address of Jenkins agent 01

   ```ShellSession
   $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins agent 01

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_01_PRIVATE_IP}"
   ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Create volume group
   
   ```ShellSession
   $ sudo vgcreate VolGroup00 /dev/nvme1n1p1
   ```

1. Create new "/var/log" mount

   1. Create log logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 895G -n logVol VolGroup00
      ```
   
   1. Create file system on log logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/logVol
      ```
   
   1. Create a temporary mount point for log
   
      ```ShellSession
      $ sudo mkdir -p /mnt/log
      ```
   
   1. Mount logical volume to temporary mount point for log
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /mnt/log
      ```
   
   1. Stop rsyslog
   
      ```ShellSession
      $ sudo systemctl stop rsyslog
      ```
      
   1. Sync existing log directory to temporary log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/* /mnt/log/
      ```
   
   1. Move the original log directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log /var/log.deleteme
      ```
   
   1. Create new log directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log
      ```
   
   1. Mount logical volume to the new log directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /var/log
      ```
   
   1. Sync the backup log data to the new log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log.deleteme/* /var/log/
      ```
   
   1. Restore SELinux Context for log
   
      ```ShellSession
      $ sudo restorecon -rv /var/log
      ```
   
   1. Restart rsyslog
   
      ```ShellSession
      $ sudo systemctl start rsyslog
      ```
   
   1. Add the log mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-logVol            /var/log           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file
   
1. Create new "/var/lib" mount

   1. Create lib logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 100G -n libVol VolGroup00
      ```
   
   1. Create file system on lib logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/libVol
      ```
   
   1. Create a temporary mount point for lib
   
      ```ShellSession
      $ sudo mkdir -p /mnt/lib
      ```
   
   1. Mount logical volume to temporary mount point for lib
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /mnt/lib
      ```
       
   1. Sync existing lib directory to temporary lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib/* /mnt/lib/
      ```
   
   1. Move the original lib directory out of the way
   
      ```ShellSession
      $ sudo mv /var/lib /var/lib.deleteme
      ```
   
   1. Create new lib directory
   
      ```ShellSession
      $ sudo mkdir -p /var/lib
      ```
   
   1. Mount logical volume to the new lib directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /var/lib
      ```
   
   1. Sync the backup lib data to the new lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib.deleteme/* /var/lib/
      ```
   
   1. Restore SELinux Context for lib
   
      ```ShellSession
      $ sudo restorecon -rv /var/lib
      ```
   
   1. Add the lib mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-libVol            /var/lib           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Create new "/var/log/audit" mount

   1. Create audit logical volume
   
      ```ShellSession
      $ sudo lvcreate -l +100%FREE -n auditVol VolGroup00
      ```
   
   1. Create file system on audit logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/auditVol
      ```
   
   1. Create a temporary mount point for audit
   
      ```ShellSession
      $ sudo mkdir -p /mnt/audit
      ```
   
   1. Mount logical volume to temporary mount point for audit
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /mnt/audit
      ```
       
   1. Sync existing audit directory to temporary audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit/ /mnt/audit/
      ```
   
   1. Move the original audit directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log/audit /var/log/audit.deleteme
      ```
   
   1. Create new audit directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log/audit
      ```
   
   1. Mount logical volume to the new audit directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /var/log/audit
      ```
   
   1. Sync the backup audit data to the new audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit.deleteme/ /var/log/audit/
      ```
   
   1. Restore SELinux Context for audit
   
      ```ShellSession
      $ sudo restorecon -rv /var/log/audit
      ```
   
   1. Add the audit mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-auditVol          /var/log/audit     xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Exit Jenkins agent 01 node

   ```ShellSession
   $ exit
   ```

### Configure Jenkins agent 02 second ELB volume

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get availability zone of Jenkins agent 02

   ```ShellSession
   $ JENKINS_AGENT_02_AZ=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
     --query "Reservations[*].Instances[*].Placement.AvailabilityZone" \
     --output text)
   ```

1. Create volume for Jenkins agent 02

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 create-volume \
     --size 1000 \
     --availability-zone "${JENKINS_AGENT_02_AZ}" \
     --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=ab2d-jenkins-agent-02-second-vol}]'
   ```

1. Ensure the status of "ab2d-jenkins-agent-02-second-vol" volume is "available" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02-second-vol" \
     --query "Volumes[*].State" \
     --output text
   ```

1. Get EC2 instance id of Jenkins agent 02

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```

1. Get volume ID of "ab2d-jenkins-agent-02-second-vol" volume

   ```ShellSession
   $ VOLUME_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02-second-vol" \
     --query "Volumes[*].VolumeId" \
     --output text)
   ```

1. Attach "ab2d-jenkins-agent-02-second-vol" volume to Jenkins agent 02

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 attach-volume \
     --device "/dev/sdf" \
     --instance-id "${EC2_INSTANCE_ID}" \
     --volume-id "${VOLUME_ID}"
   ```

1. Ensure the status of "ab2d-jenkins-agent-02-second-vol" volume is "attached" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02-second-vol" \
     --query "Volumes[*].Attachments[*].State" \
     --output text
   ```

1. Get private ip address of Jenkins agent 02

   ```ShellSession
   $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins agent 02

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_02_PRIVATE_IP}"
   ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Create volume group
   
   ```ShellSession
   $ sudo vgcreate VolGroup00 /dev/nvme1n1p1
   ```

1. Create new "/var/log" mount

   1. Create log logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 895G -n logVol VolGroup00
      ```
   
   1. Create file system on log logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/logVol
      ```
   
   1. Create a temporary mount point for log
   
      ```ShellSession
      $ sudo mkdir -p /mnt/log
      ```
   
   1. Mount logical volume to temporary mount point for log
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /mnt/log
      ```
   
   1. Stop rsyslog
   
      ```ShellSession
      $ sudo systemctl stop rsyslog
      ```
      
   1. Sync existing log directory to temporary log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/* /mnt/log/
      ```
   
   1. Move the original log directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log /var/log.deleteme
      ```
   
   1. Create new log directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log
      ```
   
   1. Mount logical volume to the new log directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /var/log
      ```
   
   1. Sync the backup log data to the new log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log.deleteme/* /var/log/
      ```
   
   1. Restore SELinux Context for log
   
      ```ShellSession
      $ sudo restorecon -rv /var/log
      ```
   
   1. Restart rsyslog
   
      ```ShellSession
      $ sudo systemctl start rsyslog
      ```
   
   1. Add the log mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-logVol            /var/log           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file
   
1. Create new "/var/lib" mount

   1. Create lib logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 100G -n libVol VolGroup00
      ```
   
   1. Create file system on lib logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/libVol
      ```
   
   1. Create a temporary mount point for lib
   
      ```ShellSession
      $ sudo mkdir -p /mnt/lib
      ```
   
   1. Mount logical volume to temporary mount point for lib
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /mnt/lib
      ```
       
   1. Sync existing lib directory to temporary lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib/* /mnt/lib/
      ```
   
   1. Move the original lib directory out of the way
   
      ```ShellSession
      $ sudo mv /var/lib /var/lib.deleteme
      ```
   
   1. Create new lib directory
   
      ```ShellSession
      $ sudo mkdir -p /var/lib
      ```
   
   1. Mount logical volume to the new lib directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /var/lib
      ```
   
   1. Sync the backup lib data to the new lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib.deleteme/* /var/lib/
      ```
   
   1. Restore SELinux Context for lib
   
      ```ShellSession
      $ sudo restorecon -rv /var/lib
      ```
   
   1. Add the lib mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-libVol            /var/lib           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Create new "/var/log/audit" mount

   1. Create audit logical volume
   
      ```ShellSession
      $ sudo lvcreate -l +100%FREE -n auditVol VolGroup00
      ```
   
   1. Create file system on audit logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/auditVol
      ```
   
   1. Create a temporary mount point for audit
   
      ```ShellSession
      $ sudo mkdir -p /mnt/audit
      ```
   
   1. Mount logical volume to temporary mount point for audit
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /mnt/audit
      ```
       
   1. Sync existing audit directory to temporary audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit/ /mnt/audit/
      ```
   
   1. Move the original audit directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log/audit /var/log/audit.deleteme
      ```
   
   1. Create new audit directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log/audit
      ```
   
   1. Mount logical volume to the new audit directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /var/log/audit
      ```
   
   1. Sync the backup audit data to the new audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit.deleteme/ /var/log/audit/
      ```
   
   1. Restore SELinux Context for audit
   
      ```ShellSession
      $ sudo restorecon -rv /var/log/audit
      ```
   
   1. Add the audit mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-auditVol          /var/log/audit     xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Exit Jenkins agent 02 node

   ```ShellSession
   $ exit
   ```

### Configure Jenkins agent 03 second ELB volume

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get availability zone of Jenkins agent 03

   ```ShellSession
   $ JENKINS_AGENT_03_AZ=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
     --query "Reservations[*].Instances[*].Placement.AvailabilityZone" \
     --output text)
   ```

1. Create volume for Jenkins agent 03

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 create-volume \
     --size 1000 \
     --availability-zone "${JENKINS_AGENT_03_AZ}" \
     --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=ab2d-jenkins-agent-03-second-vol}]'
   ```

1. Ensure the status of "ab2d-jenkins-agent-03-second-vol" volume is "available" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03-second-vol" \
     --query "Volumes[*].State" \
     --output text
   ```

1. Get EC2 instance id of Jenkins agent 03

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```

1. Get volume ID of "ab2d-jenkins-agent-03-second-vol" volume

   ```ShellSession
   $ VOLUME_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03-second-vol" \
     --query "Volumes[*].VolumeId" \
     --output text)
   ```

1. Attach "ab2d-jenkins-agent-03-second-vol" volume to Jenkins agent 03

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 attach-volume \
     --device "/dev/sdf" \
     --instance-id "${EC2_INSTANCE_ID}" \
     --volume-id "${VOLUME_ID}"
   ```

1. Ensure the status of "ab2d-jenkins-agent-03-second-vol" volume is "attached" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03-second-vol" \
     --query "Volumes[*].Attachments[*].State" \
     --output text
   ```

1. Get private ip address of Jenkins agent 03

   ```ShellSession
   $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins agent 03

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_03_PRIVATE_IP}"
   ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Create volume group
   
   ```ShellSession
   $ sudo vgcreate VolGroup00 /dev/nvme1n1p1
   ```

1. Create new "/var/log" mount

   1. Create log logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 895G -n logVol VolGroup00
      ```
   
   1. Create file system on log logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/logVol
      ```
   
   1. Create a temporary mount point for log
   
      ```ShellSession
      $ sudo mkdir -p /mnt/log
      ```
   
   1. Mount logical volume to temporary mount point for log
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /mnt/log
      ```
   
   1. Stop rsyslog
   
      ```ShellSession
      $ sudo systemctl stop rsyslog
      ```
      
   1. Sync existing log directory to temporary log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/* /mnt/log/
      ```
   
   1. Move the original log directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log /var/log.deleteme
      ```
   
   1. Create new log directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log
      ```
   
   1. Mount logical volume to the new log directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /var/log
      ```
   
   1. Sync the backup log data to the new log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log.deleteme/* /var/log/
      ```
   
   1. Restore SELinux Context for log
   
      ```ShellSession
      $ sudo restorecon -rv /var/log
      ```
   
   1. Restart rsyslog
   
      ```ShellSession
      $ sudo systemctl start rsyslog
      ```
   
   1. Add the log mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-logVol            /var/log           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file
   
1. Create new "/var/lib" mount

   1. Create lib logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 100G -n libVol VolGroup00
      ```
   
   1. Create file system on lib logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/libVol
      ```
   
   1. Create a temporary mount point for lib
   
      ```ShellSession
      $ sudo mkdir -p /mnt/lib
      ```
   
   1. Mount logical volume to temporary mount point for lib
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /mnt/lib
      ```
       
   1. Sync existing lib directory to temporary lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib/* /mnt/lib/
      ```
   
   1. Move the original lib directory out of the way
   
      ```ShellSession
      $ sudo mv /var/lib /var/lib.deleteme
      ```
   
   1. Create new lib directory
   
      ```ShellSession
      $ sudo mkdir -p /var/lib
      ```
   
   1. Mount logical volume to the new lib directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /var/lib
      ```
   
   1. Sync the backup lib data to the new lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib.deleteme/* /var/lib/
      ```
   
   1. Restore SELinux Context for lib
   
      ```ShellSession
      $ sudo restorecon -rv /var/lib
      ```
   
   1. Add the lib mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-libVol            /var/lib           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Create new "/var/log/audit" mount

   1. Create audit logical volume
   
      ```ShellSession
      $ sudo lvcreate -l +100%FREE -n auditVol VolGroup00
      ```
   
   1. Create file system on audit logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/auditVol
      ```
   
   1. Create a temporary mount point for audit
   
      ```ShellSession
      $ sudo mkdir -p /mnt/audit
      ```
   
   1. Mount logical volume to temporary mount point for audit
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /mnt/audit
      ```
       
   1. Sync existing audit directory to temporary audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit/ /mnt/audit/
      ```
   
   1. Move the original audit directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log/audit /var/log/audit.deleteme
      ```
   
   1. Create new audit directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log/audit
      ```
   
   1. Mount logical volume to the new audit directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /var/log/audit
      ```
   
   1. Sync the backup audit data to the new audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit.deleteme/ /var/log/audit/
      ```
   
   1. Restore SELinux Context for audit
   
      ```ShellSession
      $ sudo restorecon -rv /var/log/audit
      ```
   
   1. Add the audit mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-auditVol          /var/log/audit     xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Exit Jenkins agent 03 node

   ```ShellSession
   $ exit
   ```

### Configure Jenkins agent 04 second ELB volume

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get availability zone of Jenkins agent 04

   ```ShellSession
   $ JENKINS_AGENT_04_AZ=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
     --query "Reservations[*].Instances[*].Placement.AvailabilityZone" \
     --output text)
   ```

1. Create volume for Jenkins agent 04

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 create-volume \
     --size 1000 \
     --availability-zone "${JENKINS_AGENT_04_AZ}" \
     --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=ab2d-jenkins-agent-04-second-vol}]'
   ```

1. Ensure the status of "ab2d-jenkins-agent-04-second-vol" volume is "available" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04-second-vol" \
     --query "Volumes[*].State" \
     --output text
   ```

1. Get EC2 instance id of Jenkins agent 04

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```

1. Get volume ID of "ab2d-jenkins-agent-04-second-vol" volume

   ```ShellSession
   $ VOLUME_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04-second-vol" \
     --query "Volumes[*].VolumeId" \
     --output text)
   ```

1. Attach "ab2d-jenkins-agent-04-second-vol" volume to Jenkins agent 04

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 attach-volume \
     --device "/dev/sdf" \
     --instance-id "${EC2_INSTANCE_ID}" \
     --volume-id "${VOLUME_ID}"
   ```

1. Ensure the status of "ab2d-jenkins-agent-04-second-vol" volume is "attached" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04-second-vol" \
     --query "Volumes[*].Attachments[*].State" \
     --output text
   ```

1. Get private ip address of Jenkins agent 04

   ```ShellSession
   $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins agent 04

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_04_PRIVATE_IP}"
   ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Create volume group
   
   ```ShellSession
   $ sudo vgcreate VolGroup00 /dev/nvme1n1p1
   ```

1. Create new "/var/log" mount

   1. Create log logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 895G -n logVol VolGroup00
      ```
   
   1. Create file system on log logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/logVol
      ```
   
   1. Create a temporary mount point for log
   
      ```ShellSession
      $ sudo mkdir -p /mnt/log
      ```
   
   1. Mount logical volume to temporary mount point for log
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /mnt/log
      ```
   
   1. Stop rsyslog
   
      ```ShellSession
      $ sudo systemctl stop rsyslog
      ```
      
   1. Sync existing log directory to temporary log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/* /mnt/log/
      ```
   
   1. Move the original log directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log /var/log.deleteme
      ```
   
   1. Create new log directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log
      ```
   
   1. Mount logical volume to the new log directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /var/log
      ```
   
   1. Sync the backup log data to the new log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log.deleteme/* /var/log/
      ```
   
   1. Restore SELinux Context for log
   
      ```ShellSession
      $ sudo restorecon -rv /var/log
      ```
   
   1. Restart rsyslog
   
      ```ShellSession
      $ sudo systemctl start rsyslog
      ```
   
   1. Add the log mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-logVol            /var/log           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file
   
1. Create new "/var/lib" mount

   1. Create lib logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 100G -n libVol VolGroup00
      ```
   
   1. Create file system on lib logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/libVol
      ```
   
   1. Create a temporary mount point for lib
   
      ```ShellSession
      $ sudo mkdir -p /mnt/lib
      ```
   
   1. Mount logical volume to temporary mount point for lib
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /mnt/lib
      ```
       
   1. Sync existing lib directory to temporary lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib/* /mnt/lib/
      ```
   
   1. Move the original lib directory out of the way
   
      ```ShellSession
      $ sudo mv /var/lib /var/lib.deleteme
      ```
   
   1. Create new lib directory
   
      ```ShellSession
      $ sudo mkdir -p /var/lib
      ```
   
   1. Mount logical volume to the new lib directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /var/lib
      ```
   
   1. Sync the backup lib data to the new lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib.deleteme/* /var/lib/
      ```
   
   1. Restore SELinux Context for lib
   
      ```ShellSession
      $ sudo restorecon -rv /var/lib
      ```
   
   1. Add the lib mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-libVol            /var/lib           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Create new "/var/log/audit" mount

   1. Create audit logical volume
   
      ```ShellSession
      $ sudo lvcreate -l +100%FREE -n auditVol VolGroup00
      ```
   
   1. Create file system on audit logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/auditVol
      ```
   
   1. Create a temporary mount point for audit
   
      ```ShellSession
      $ sudo mkdir -p /mnt/audit
      ```
   
   1. Mount logical volume to temporary mount point for audit
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /mnt/audit
      ```
       
   1. Sync existing audit directory to temporary audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit/ /mnt/audit/
      ```
   
   1. Move the original audit directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log/audit /var/log/audit.deleteme
      ```
   
   1. Create new audit directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log/audit
      ```
   
   1. Mount logical volume to the new audit directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /var/log/audit
      ```
   
   1. Sync the backup audit data to the new audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit.deleteme/ /var/log/audit/
      ```
   
   1. Restore SELinux Context for audit
   
      ```ShellSession
      $ sudo restorecon -rv /var/log/audit
      ```
   
   1. Add the audit mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-auditVol          /var/log/audit     xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Exit Jenkins agent 04 node

   ```ShellSession
   $ exit
   ```

### Configure Jenkins agent 05 second ELB volume

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get availability zone of Jenkins agent 05

   ```ShellSession
   $ JENKINS_AGENT_05_AZ=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
     --query "Reservations[*].Instances[*].Placement.AvailabilityZone" \
     --output text)
   ```

1. Create volume for Jenkins agent 05

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 create-volume \
     --size 1000 \
     --availability-zone "${JENKINS_AGENT_05_AZ}" \
     --tag-specifications 'ResourceType=volume,Tags=[{Key=Name,Value=ab2d-jenkins-agent-05-second-vol}]'
   ```

1. Ensure the status of "ab2d-jenkins-agent-05-second-vol" volume is "available" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05-second-vol" \
     --query "Volumes[*].State" \
     --output text
   ```

1. Get EC2 instance id of Jenkins agent 05

   ```ShellSession
   $ EC2_INSTANCE_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
     --query "Reservations[*].Instances[*].InstanceId" \
     --output text)
   ```

1. Get volume ID of "ab2d-jenkins-agent-05-second-vol" volume

   ```ShellSession
   $ VOLUME_ID=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05-second-vol" \
     --query "Volumes[*].VolumeId" \
     --output text)
   ```

1. Attach "ab2d-jenkins-agent-05-second-vol" volume to Jenkins agent 05

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 attach-volume \
     --device "/dev/sdf" \
     --instance-id "${EC2_INSTANCE_ID}" \
     --volume-id "${VOLUME_ID}"
   ```

1. Ensure the status of "ab2d-jenkins-agent-05-second-vol" volume is "attached" before proceeding

   ```ShellSession
   $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-volumes \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05-second-vol" \
     --query "Volumes[*].Attachments[*].State" \
     --output text
   ```

1. Get private ip address of Jenkins agent 05

   ```ShellSession
   $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins agent 05

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_05_PRIVATE_IP}"
   ```

1. Create a new partition on the "/dev/nvme1n1" disk

   ```ShellSession
   (
   echo n # Add a new partition
   echo p # Primary partition
   echo   # Partition number (Accept default)
   echo   # First sector (Accept default)
   echo   # Last sector (Accept default)
   echo w # Write changes
   ) | sudo fdisk /dev/nvme1n1
   ```

1. Request that the operating system re-reads the partition table

   ```ShellSession
   $ sudo partprobe
   ```

1. Create physical volume by initializing the partition for use by the Logical Volume Manager (LVM)

   ```ShellSession
   $ sudo pvcreate /dev/nvme1n1p1
   ```

1. Create volume group
   
   ```ShellSession
   $ sudo vgcreate VolGroup00 /dev/nvme1n1p1
   ```

1. Create new "/var/log" mount

   1. Create log logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 895G -n logVol VolGroup00
      ```
   
   1. Create file system on log logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/logVol
      ```
   
   1. Create a temporary mount point for log
   
      ```ShellSession
      $ sudo mkdir -p /mnt/log
      ```
   
   1. Mount logical volume to temporary mount point for log
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /mnt/log
      ```
   
   1. Stop rsyslog
   
      ```ShellSession
      $ sudo systemctl stop rsyslog
      ```
      
   1. Sync existing log directory to temporary log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/* /mnt/log/
      ```
   
   1. Move the original log directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log /var/log.deleteme
      ```
   
   1. Create new log directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log
      ```
   
   1. Mount logical volume to the new log directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/logVol /var/log
      ```
   
   1. Sync the backup log data to the new log directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log.deleteme/* /var/log/
      ```
   
   1. Restore SELinux Context for log
   
      ```ShellSession
      $ sudo restorecon -rv /var/log
      ```
   
   1. Restart rsyslog
   
      ```ShellSession
      $ sudo systemctl start rsyslog
      ```
   
   1. Add the log mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-logVol            /var/log           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file
   
1. Create new "/var/lib" mount

   1. Create lib logical volume
   
      ```ShellSession
      $ sudo lvcreate -L 100G -n libVol VolGroup00
      ```
   
   1. Create file system on lib logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/libVol
      ```
   
   1. Create a temporary mount point for lib
   
      ```ShellSession
      $ sudo mkdir -p /mnt/lib
      ```
   
   1. Mount logical volume to temporary mount point for lib
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /mnt/lib
      ```
       
   1. Sync existing lib directory to temporary lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib/* /mnt/lib/
      ```
   
   1. Move the original lib directory out of the way
   
      ```ShellSession
      $ sudo mv /var/lib /var/lib.deleteme
      ```
   
   1. Create new lib directory
   
      ```ShellSession
      $ sudo mkdir -p /var/lib
      ```
   
   1. Mount logical volume to the new lib directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/libVol /var/lib
      ```
   
   1. Sync the backup lib data to the new lib directory
   
      ```ShellSession
      $ sudo rsync -avz /var/lib.deleteme/* /var/lib/
      ```
   
   1. Restore SELinux Context for lib
   
      ```ShellSession
      $ sudo restorecon -rv /var/lib
      ```
   
   1. Add the lib mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-libVol            /var/lib           xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Create new "/var/log/audit" mount

   1. Create audit logical volume
   
      ```ShellSession
      $ sudo lvcreate -l +100%FREE -n auditVol VolGroup00
      ```
   
   1. Create file system on audit logical volume
   
      ```ShellSession
      $ sudo mkfs.xfs -f /dev/VolGroup00/auditVol
      ```
   
   1. Create a temporary mount point for audit
   
      ```ShellSession
      $ sudo mkdir -p /mnt/audit
      ```
   
   1. Mount logical volume to temporary mount point for audit
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /mnt/audit
      ```
       
   1. Sync existing audit directory to temporary audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit/ /mnt/audit/
      ```
   
   1. Move the original audit directory out of the way
   
      ```ShellSession
      $ sudo mv /var/log/audit /var/log/audit.deleteme
      ```
   
   1. Create new audit directory
   
      ```ShellSession
      $ sudo mkdir -p /var/log/audit
      ```
   
   1. Mount logical volume to the new audit directory
   
      ```ShellSession
      $ sudo mount /dev/VolGroup00/auditVol /var/log/audit
      ```
   
   1. Sync the backup audit data to the new audit directory
   
      ```ShellSession
      $ sudo rsync -avz /var/log/audit.deleteme/ /var/log/audit/
      ```
   
   1. Restore SELinux Context for audit
   
      ```ShellSession
      $ sudo restorecon -rv /var/log/audit
      ```
   
   1. Add the audit mount to the "/etc/fstab" file
   
      1. Open the "/etc/fstab" file
   
         ```ShellSession
         $ sudo vim /etc/fstab
         ```
   
      1. Add the following line to the file
      
         ```
         /dev/mapper/VolGroup00-auditVol          /var/log/audit     xfs    defaults,noatime 0 2
         ```
   
      1. Save and close the file

1. Exit Jenkins agent 05 node

   ```ShellSession
   $ exit
   ```

## Install Jenkins

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Install Jenkins

   ```ShellSession
   $ sudo yum install jenkins -y
   ```

## Allow jenkins users to use a login shell

### Create PKCS1 SSH credentials and save to 1Password

1. Note the following

   - the Jenkins GUI currently does not support the use of PKCS8 private keys (e.g. -----BEGIN PRIVATE KEY-----)

   - the Jenkins GUI supports PKCS1 private keys (e.g. -----BEGIN RSA PRIVATE KEY-----)

   - Lonnie wasn't able to determine a way to create a PKCS1 private key on the latest version of gold disk image

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 01

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_01_PRIVATE_IP}"
      ```
   
### Modify the jenkins user on Jenkins master to allow it to use a login shell

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Get information about the jenkins user account

   1. Enter the following
   
      ```ShellSession
      $ getent passwd jenkins
      ```

   2. Note the output

      *Example:*
      
      ```
      jenkins:x:997:994:Jenkins Automation Server:/var/lib/jenkins:/bin/false
      ```

   3. Note that the "/bin/false" in the output means that the user will not be able to use a login shell when running various commands

1. Modify the jenkins user to allow it to use a login shell

   1. Enter the following
   
      ```ShellSession
      $ sudo usermod -s /bin/bash jenkins
      ```

   1. Verify the change

      ```ShellSession
      $ getent passwd jenkins
      ```

   1. Note the output

      *Example:*
      
      ```
      jenkins:x:997:994:Jenkins Automation Server:/var/lib/jenkins:/bin/bash
      ```

1. Give the local jenkins user a password for intial SSH connections
      
   1. Enter the following
   
      ```ShellSession
      $ sudo passwd jenkins
      ```

   1. Copy the password from the following 1Password entry to the clipboards

      ```
      jenkins master redhat user
      ```
      
   1. Paste the password at the **New password** prompt

   1. Paste the password at the **Retype new password** prompt

1. Add jenkins user to sudoers file

   1. Enter the following

      ```ShellSession
      $ sudo visudo
      ```

   1. Modify this section as follows

      ```
      ## Allow root to run any commands anywhere
      root    ALL=(ALL)       ALL
      jenkins ALL=(ALL)       NOPASSWD: ALL
      ```

    1. Note that the above setting for the jenkins user is not ideal since it means that the jenkins user will be able to do "sudo" on all commands

    1. Note that it would be better to lock down the jenkins user so that it can only do "sudo" on a certain subset of commands based on the requirements

1. Set up SSH keys for jenkins user

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. View the jenkins user home directory

      ```ShellSession
      $ pwd
      ```

   1. Note the home directory

      ```
      /var/lib/jenkins
      ```

   1. Initiate creation of SSH keys

      ```ShellSession
      $ ssh-keygen
      ```

   1. Press **return** on the keyboard at the "Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa)" prompt

   1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

   1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Exit jenkins user session

   ```ShellSession
   $ exit
   ```

1. Temporarily allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Save and close the file
   
   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```
   
1. Exit the Jenkins master

   ```ShellSession
   $ exit
   ```

### Create a jenkins user on Jenkins agent 01 and allow it to use a login shell

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 01

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_01_PRIVATE_IP}"
      ```

1. Add a jenkins user

   ```ShellSession
   $ sudo useradd -d /var/lib/jenkins -s /bin/bash jenkins
   ```

1. Set ownership of the jenkins home directory

   ```ShellSession
   $ sudo chown -R jenkins:jenkins /var/lib/jenkins
   ```
   
1. Give the local jenkins user a password

   1. Create a "jenkins agent redhat user" entry in 1Password that meets the following requirements
      - at least 15 characeters

      - at least 1 special character
      
   1. Enter the following
   
      ```ShellSession
      $ sudo passwd jenkins
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the password at the **New password** prompt

   1. Paste the password at the **Retype new password** prompt

1. Add jenkins user to sudoers file

   1. Enter the following

      ```ShellSession
      $ sudo visudo
      ```

   1. Modify this section as follows

      ```
      ## Allow root to run any commands anywhere
      root    ALL=(ALL)       ALL
      jenkins ALL=(ALL)       NOPASSWD: ALL
      ```

    1. Note that the above setting for the jenkins user is not ideal since it means that the jenkins user will be able to do "sudo" on all commands

    1. Note that it would be better to lock down the jenkins user so that it can only do "sudo" on a certain subset of commands based on the requirements

1. Set up SSH keys for jenkins user

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. View the jenkins user home directory

      ```ShellSession
      $ pwd
      ```

   1. Note the home directory

      ```
      /var/lib/jenkins
      ```

   1. Initiate creation of SSH keys

      ```ShellSession
      $ ssh-keygen
      ```

   1. Press **return** on the keyboard at the "Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa)" prompt

   1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

   1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Exit jenkins user session

   ```ShellSession
   $ exit
   ```

1. Temporarily allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Save and close the file
   
   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```

### Create a jenkins user on Jenkins agent 02 and allow it to use a login shell

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 02

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_02_PRIVATE_IP}"
      ```

1. Add a jenkins user

   ```ShellSession
   $ sudo useradd -d /var/lib/jenkins -s /bin/bash jenkins
   ```

1. Set ownership of the jenkins home directory

   ```ShellSession
   $ sudo chown -R jenkins:jenkins /var/lib/jenkins
   ```
   
1. Give the local jenkins user a password

   1. Create a "jenkins agent redhat user" entry in 1Password that meets the following requirements
      - at least 15 characeters

      - at least 1 special character
      
   1. Enter the following
   
      ```ShellSession
      $ sudo passwd jenkins
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the password at the **New password** prompt

   1. Paste the password at the **Retype new password** prompt

1. Add jenkins user to sudoers file

   1. Enter the following

      ```ShellSession
      $ sudo visudo
      ```

   1. Modify this section as follows

      ```
      ## Allow root to run any commands anywhere
      root    ALL=(ALL)       ALL
      jenkins ALL=(ALL)       NOPASSWD: ALL
      ```

    1. Note that the above setting for the jenkins user is not ideal since it means that the jenkins user will be able to do "sudo" on all commands

    1. Note that it would be better to lock down the jenkins user so that it can only do "sudo" on a certain subset of commands based on the requirements

1. Set up SSH keys for jenkins user

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. View the jenkins user home directory

      ```ShellSession
      $ pwd
      ```

   1. Note the home directory

      ```
      /var/lib/jenkins
      ```

   1. Initiate creation of SSH keys

      ```ShellSession
      $ ssh-keygen
      ```

   1. Press **return** on the keyboard at the "Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa)" prompt

   1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

   1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Exit jenkins user session

   ```ShellSession
   $ exit
   ```

1. Temporarily allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Save and close the file
   
   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```

### Create a jenkins user on Jenkins agent 03 and allow it to use a login shell

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 03

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_03_PRIVATE_IP}"
      ```

1. Add a jenkins user

   ```ShellSession
   $ sudo useradd -d /var/lib/jenkins -s /bin/bash jenkins
   ```

1. Set ownership of the jenkins home directory

   ```ShellSession
   $ sudo chown -R jenkins:jenkins /var/lib/jenkins
   ```
   
1. Give the local jenkins user a password

   1. Create a "jenkins agent redhat user" entry in 1Password that meets the following requirements
      - at least 15 characeters

      - at least 1 special character
      
   1. Enter the following
   
      ```ShellSession
      $ sudo passwd jenkins
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the password at the **New password** prompt

   1. Paste the password at the **Retype new password** prompt

1. Add jenkins user to sudoers file

   1. Enter the following

      ```ShellSession
      $ sudo visudo
      ```

   1. Modify this section as follows

      ```
      ## Allow root to run any commands anywhere
      root    ALL=(ALL)       ALL
      jenkins ALL=(ALL)       NOPASSWD: ALL
      ```

    1. Note that the above setting for the jenkins user is not ideal since it means that the jenkins user will be able to do "sudo" on all commands

    1. Note that it would be better to lock down the jenkins user so that it can only do "sudo" on a certain subset of commands based on the requirements

1. Set up SSH keys for jenkins user

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. View the jenkins user home directory

      ```ShellSession
      $ pwd
      ```

   1. Note the home directory

      ```
      /var/lib/jenkins
      ```

   1. Initiate creation of SSH keys

      ```ShellSession
      $ ssh-keygen
      ```

   1. Press **return** on the keyboard at the "Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa)" prompt

   1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

   1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Exit jenkins user session

   ```ShellSession
   $ exit
   ```

1. Temporarily allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Save and close the file
   
   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```

### Create a jenkins user on Jenkins agent 04 and allow it to use a login shell

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 04

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_04_PRIVATE_IP}"
      ```

1. Add a jenkins user

   ```ShellSession
   $ sudo useradd -d /var/lib/jenkins -s /bin/bash jenkins
   ```

1. Set ownership of the jenkins home directory

   ```ShellSession
   $ sudo chown -R jenkins:jenkins /var/lib/jenkins
   ```
   
1. Give the local jenkins user a password

   1. Create a "jenkins agent redhat user" entry in 1Password that meets the following requirements
      - at least 15 characeters

      - at least 1 special character
      
   1. Enter the following
   
      ```ShellSession
      $ sudo passwd jenkins
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the password at the **New password** prompt

   1. Paste the password at the **Retype new password** prompt

1. Add jenkins user to sudoers file

   1. Enter the following

      ```ShellSession
      $ sudo visudo
      ```

   1. Modify this section as follows

      ```
      ## Allow root to run any commands anywhere
      root    ALL=(ALL)       ALL
      jenkins ALL=(ALL)       NOPASSWD: ALL
      ```

    1. Note that the above setting for the jenkins user is not ideal since it means that the jenkins user will be able to do "sudo" on all commands

    1. Note that it would be better to lock down the jenkins user so that it can only do "sudo" on a certain subset of commands based on the requirements

1. Set up SSH keys for jenkins user

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. View the jenkins user home directory

      ```ShellSession
      $ pwd
      ```

   1. Note the home directory

      ```
      /var/lib/jenkins
      ```

   1. Initiate creation of SSH keys

      ```ShellSession
      $ ssh-keygen
      ```

   1. Press **return** on the keyboard at the "Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa)" prompt

   1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

   1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Exit jenkins user session

   ```ShellSession
   $ exit
   ```

1. Temporarily allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Save and close the file
   
   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```

### Create a jenkins user on Jenkins agent 05 and allow it to use a login shell

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 05

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_AGENT_05_PRIVATE_IP}"
      ```

1. Add a jenkins user

   ```ShellSession
   $ sudo useradd -d /var/lib/jenkins -s /bin/bash jenkins
   ```

1. Set ownership of the jenkins home directory

   ```ShellSession
   $ sudo chown -R jenkins:jenkins /var/lib/jenkins
   ```
   
1. Give the local jenkins user a password

   1. Create a "jenkins agent redhat user" entry in 1Password that meets the following requirements
      - at least 15 characeters

      - at least 1 special character
      
   1. Enter the following
   
      ```ShellSession
      $ sudo passwd jenkins
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the password at the **New password** prompt

   1. Paste the password at the **Retype new password** prompt

1. Add jenkins user to sudoers file

   1. Enter the following

      ```ShellSession
      $ sudo visudo
      ```

   1. Modify this section as follows

      ```
      ## Allow root to run any commands anywhere
      root    ALL=(ALL)       ALL
      jenkins ALL=(ALL)       NOPASSWD: ALL
      ```

    1. Note that the above setting for the jenkins user is not ideal since it means that the jenkins user will be able to do "sudo" on all commands

    1. Note that it would be better to lock down the jenkins user so that it can only do "sudo" on a certain subset of commands based on the requirements

1. Set up SSH keys for jenkins user

   1. Switch to the jenkins user
   
      ```ShellSession
      $ sudo su - jenkins
      ```

   1. View the jenkins user home directory

      ```ShellSession
      $ pwd
      ```

   1. Note the home directory

      ```
      /var/lib/jenkins
      ```

   1. Initiate creation of SSH keys

      ```ShellSession
      $ ssh-keygen
      ```

   1. Press **return** on the keyboard at the "Enter file in which to save the key (/var/lib/jenkins/.ssh/id_rsa)" prompt

   1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

   1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Exit jenkins user session

   ```ShellSession
   $ exit
   ```

1. Temporarily allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Save and close the file
   
   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```

## Set up SSH communication for jenkins

### Create or use existing Jenkins compatible key pair

1. Note the following

   - Jenkins GUI currently requires a PKCS1 private key for SSH

   - the latest gold disks are FIPS enabled and will not allow the creation of PKCS1 private key

   - as a workaround, the PKCS1 keypair must be created on Mac, an older Linux machine that is not FIPS enabled, or a Linux docker container

   - you can determine if a Linux EC2 instance is FIPS enabled by doing the following

     - Check the FIPS status

       ```ShellSession
       $ cat /proc/sys/crypto/fips_enabled
       ```
       
     - Check the output (0 = FIPS disabled; 1 = FIPS enabled)

1. Check to see if the following keys are already in 1Password

   Label                                             |File
   --------------------------------------------------|--------------------
   AB2D Mgmt - Jenkins Agent SSH Access - Private Key|jenkins-agent-rsa
   AB2D Mgmt - Jenkins Agent SSH Access - Public Key |jenkins-agent-rsa.pub

1. If the keys are already in 1Password, do the following:

   1. Copy the keys from 1Password to your "~/Downloads" directory

   1. Jump to the following section

      [Set up SSH communication for Jenkins master](#set-up-ssh-communication-for-jenkins-master)
   
1. Run the following command on your Mac

   ```ShellSession
   $ ssh-keygen -f ~/.ssh/jenkins-agent-rsa -t rsa -b 4096
   ```

1. Press **return** on the keyboard at the "Enter passphrase (empty for no passphrase)" prompt

1. Press **return** on the keyboard at the "Enter same passphrase again" prompt

1. Note that the following two files were created

   - ~/.ssh/jenkins-agent-rsa

   - ~/.ssh/jenkins-agent-rsa.pub

1. Save the keys to 1Password "ab2d" vault with the following entries

   Label                                             |File
   --------------------------------------------------|--------------------
   AB2D Mgmt - Jenkins Agent SSH Access - Private Key|jenkins-agent-rsa
   AB2D Mgmt - Jenkins Agent SSH Access - Public Key |jenkins-agent-rsa.pub

1. Copy the keys to the "~/Downloads" directory

   ```ShellSession
   $ cp ~/.ssh/jenkins-agent-rsa ~/Downloads \
     && cp ~/.ssh/jenkins-agent-rsa.pub ~/Downloads
   ```

### Set up SSH communication for Jenkins master

#### Authorize jenkins master to SSH into itself with default key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into itself with default key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@localhost
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins master redhat user
      ```
      
   1. Paste the jenkins master redhat user password at the "jenkins@localhost's password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@localhost
      ```

   1. Exit second jenkins user session

      ```ShellSession
      $ exit
      ```

   1. Exit first jenkins user session

      ```ShellSession
      $ exit
      ```

   1. Test ssh from ec2-user session

      ```ShellSession
      $ sudo -u jenkins ssh jenkins@localhost
      ```

   1. Exit jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into itself with Jenkins compatible key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into itself with the Jenkins compatible key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Allow the machine to SSH into itself using the Jenkins compatible key

      ```ShellSession
      $ ssh-copy-id -i ~/.ssh/jenkins-agent-rsa jenkins@localhost
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins master redhat user
      ```
      
   1. Paste the jenkins master redhat user password at the "jenkins@localhost's password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@localhost
      ```

   1. Exit second jenkins user session

      ```ShellSession
      $ exit
      ```

   1. Exit first jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Verify that jenkins master can SSH into itself

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Disallow password authentication on jenkins master

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication no
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Verify that jenkins master can still SSH using default key

   1. Enter the following

      ```ShellSession
      $ ssh jenkins@localhost
      ```

   1. Exit the second jenkins user session

      ```ShellSession
      $ exit
      ```

1. Verify that jenkins master can still SSH using Jenkins compatible key

   1. Enter the following

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@localhost
      ```

   1. Exit the second jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit first jenkins user session

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 01 with default key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 01 with default key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@$JENKINS_AGENT_01_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
         
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_01_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 01 with Jenkins compatible key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 01 with the Jenkins compatible key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into Jenkins agent 01 using the Jenkins compatible key

      ```ShellSession
      $ ssh-copy-id -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_01_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt
   
   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_01_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit the jenkins user session on jenkins master
   
      ```ShellSession
      $ exit
      ```
   
1. Exit the jenkins master

   ```ShellSession
   $ exit
   ```

#### Verify that jenkins master can SSH into Jenkins agent 01

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Connect to Jenkins agent 01

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```
   
1. Disallow password authentication on jenkins agent

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication no
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Set jenkins password to never expires

   *Note that even though password is not allowed, you still need to set password expiration to never. If you don't, password expirtation will lock the jenkins account, thus causing SSH to fail.*
   
   ```ShellSession
   $ sudo passwd -x -1 -n -1 -w -1 jenkins
   ```

1. Verify the jenkins user password settings

   1. Enter the following

      ```ShellSession
      $ sudo chage -l jenkins
      ```

   1. Note the following six settings should look like this

      ```
      Password expires                                  : never
      Password inactive                                 : never
      Account expires                                   : never
      Minimum number of days between password change    : -1
      Maximum number of days between password change    : -1
      Number of days of warning before password expires : -1
      ```
      
1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Get the private IP address of Jenkins EC2 instance

   ```ShellSession
   $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Verify that Jenkins master can still SSH into Jenkins agent 01 using default key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_01_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 01
   
      ```ShellSession
      $ exit
      ```

1. Verify that Jenkins master can still SSH into Jenkins agent 01 using Jenkins compatible key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_01_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 01
   
      ```ShellSession
      $ exit
      ```

1. Exit first jenkins user session

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 02 with default key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 02 with default key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@$JENKINS_AGENT_02_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
         
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_02_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 02 with Jenkins compatible key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 02 with the Jenkins compatible key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into Jenkins agent 02 using the Jenkins compatible key

      ```ShellSession
      $ ssh-copy-id -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_02_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt
   
   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_02_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit the jenkins user session on jenkins master
   
      ```ShellSession
      $ exit
      ```
   
1. Exit the jenkins master

   ```ShellSession
   $ exit
   ```

#### Verify that jenkins master can SSH into Jenkins agent 02

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Connect to Jenkins agent 02

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Disallow password authentication on jenkins agent

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication no
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Set jenkins password to never expires

   *Note that even though password is not allowed, you still need to set password expiration to never. If you don't, password expirtation will lock the jenkins account, thus causing SSH to fail.*
   
   ```ShellSession
   $ sudo passwd -x -1 -n -1 -w -1 jenkins
   ```

1. Verify the jenkins user password settings

   1. Enter the following

      ```ShellSession
      $ sudo chage -l jenkins
      ```

   1. Note the following six settings should look like this

      ```
      Password expires                                  : never
      Password inactive                                 : never
      Account expires                                   : never
      Minimum number of days between password change    : -1
      Maximum number of days between password change    : -1
      Number of days of warning before password expires : -1
      ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Get the private IP address of Jenkins EC2 instance

   ```ShellSession
   $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Verify that Jenkins master can still SSH into Jenkins agent 02 using default key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_02_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 02
   
      ```ShellSession
      $ exit
      ```

1. Verify that Jenkins master can still SSH into Jenkins agent 02 using Jenkins compatible key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_02_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 02
   
      ```ShellSession
      $ exit
      ```

1. Exit first jenkins user session

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 03 with default key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 03 with default key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@$JENKINS_AGENT_03_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
         
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_03_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 03 with Jenkins compatible key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 03 with the Jenkins compatible key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into Jenkins agent 03 using the Jenkins compatible key

      ```ShellSession
      $ ssh-copy-id -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_03_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt
   
   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_03_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit the jenkins user session on jenkins master
   
      ```ShellSession
      $ exit
      ```
   
1. Exit the jenkins master

   ```ShellSession
   $ exit
   ```

#### Verify that jenkins master can SSH into Jenkins agent 03

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Connect to Jenkins agent 03

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```
   
1. Disallow password authentication on jenkins agent

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication no
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Set jenkins password to never expires

   *Note that even though password is not allowed, you still need to set password expiration to never. If you don't, password expirtation will lock the jenkins account, thus causing SSH to fail.*
   
   ```ShellSession
   $ sudo passwd -x -1 -n -1 -w -1 jenkins
   ```

1. Verify the jenkins user password settings

   1. Enter the following

      ```ShellSession
      $ sudo chage -l jenkins
      ```

   1. Note the following six settings should look like this

      ```
      Password expires                                  : never
      Password inactive                                 : never
      Account expires                                   : never
      Minimum number of days between password change    : -1
      Maximum number of days between password change    : -1
      Number of days of warning before password expires : -1
      ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Get the private IP address of Jenkins EC2 instance

   ```ShellSession
   $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Verify that Jenkins master can still SSH into Jenkins agent 03 using default key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_03_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 03
   
      ```ShellSession
      $ exit
      ```

1. Verify that Jenkins master can still SSH into Jenkins agent 03 using Jenkins compatible key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_03_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 03
   
      ```ShellSession
      $ exit
      ```

1. Exit first jenkins user session

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 04 with default key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 04 with default key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@$JENKINS_AGENT_04_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
         
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_04_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 04 with Jenkins compatible key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 04 with the Jenkins compatible key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into Jenkins agent 04 using the Jenkins compatible key

      ```ShellSession
      $ ssh-copy-id -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_04_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt
   
   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_04_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit the jenkins user session on jenkins master
   
      ```ShellSession
      $ exit
      ```
   
1. Exit the jenkins master

   ```ShellSession
   $ exit
   ```

#### Verify that jenkins master can SSH into Jenkins agent 04

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Connect to Jenkins agent 04

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```
   
1. Disallow password authentication on jenkins agent

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication no
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Set jenkins password to never expires

   *Note that even though password is not allowed, you still need to set password expiration to never. If you don't, password expirtation will lock the jenkins account, thus causing SSH to fail.*
   
   ```ShellSession
   $ sudo passwd -x -1 -n -1 -w -1 jenkins
   ```

1. Verify the jenkins user password settings

   1. Enter the following

      ```ShellSession
      $ sudo chage -l jenkins
      ```

   1. Note the following six settings should look like this

      ```
      Password expires                                  : never
      Password inactive                                 : never
      Account expires                                   : never
      Minimum number of days between password change    : -1
      Maximum number of days between password change    : -1
      Number of days of warning before password expires : -1
      ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Get the private IP address of Jenkins EC2 instance

   ```ShellSession
   $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Verify that Jenkins master can still SSH into Jenkins agent 04 using default key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_04_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 04
   
      ```ShellSession
      $ exit
      ```

1. Verify that Jenkins master can still SSH into Jenkins agent 04 using Jenkins compatible key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_04_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 04
   
      ```ShellSession
      $ exit
      ```

1. Exit first jenkins user session

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```















#### Authorize jenkins master to SSH into Jenkins agent 05 with default key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 05 with default key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@$JENKINS_AGENT_05_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
         
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_05_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit jenkins user session

      ```ShellSession
      $ exit
      ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

#### Authorize jenkins master to SSH into Jenkins agent 05 with Jenkins compatible key

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Authorize jenkins master to SSH into jenkins agent 05 with the Jenkins compatible key

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into Jenkins agent 05 using the Jenkins compatible key

      ```ShellSession
      $ ssh-copy-id -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_05_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the jenkins agent redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt
   
   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_05_PRIVATE_IP
      ```

   1. Switch back to jenkins master

      ```ShellSession
      $ exit
      ```

   1. Exit the jenkins user session on jenkins master
   
      ```ShellSession
      $ exit
      ```
   
1. Exit the jenkins master

   ```ShellSession
   $ exit
   ```

#### Verify that jenkins master can SSH into Jenkins agent 05

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Connect to Jenkins agent 05

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```
   
1. Disallow password authentication on jenkins agent

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication no
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

1. Set jenkins password to never expires

   *Note that even though password is not allowed, you still need to set password expiration to never. If you don't, password expirtation will lock the jenkins account, thus causing SSH to fail.*
   
   ```ShellSession
   $ sudo passwd -x -1 -n -1 -w -1 jenkins
   ```

1. Verify the jenkins user password settings

   1. Enter the following

      ```ShellSession
      $ sudo chage -l jenkins
      ```

   1. Note the following six settings should look like this

      ```
      Password expires                                  : never
      Password inactive                                 : never
      Account expires                                   : never
      Minimum number of days between password change    : -1
      Maximum number of days between password change    : -1
      Number of days of warning before password expires : -1
      ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Get the private IP address of Jenkins EC2 instance

   ```ShellSession
   $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Verify that Jenkins master can still SSH into Jenkins agent 05 using default key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_05_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 05
   
      ```ShellSession
      $ exit
      ```

1. Verify that Jenkins master can still SSH into Jenkins agent 05 using Jenkins compatible key

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh -i ~/.ssh/jenkins-agent-rsa jenkins@$JENKINS_AGENT_05_PRIVATE_IP
      ```
   
   1. Exit Jenkins agent 05
   
      ```ShellSession
      $ exit
      ```

1. Exit first jenkins user session

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   exit
   ```

## Upgrade docker compose on all Jenkins nodes to the latest version

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Upgrade the docker compose version on Jenkins master

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

   1. Upgrade the docker compose version

      *Example:*

      ```ShellSession
      $ DOCKER_COMPOSE_VERSION=1.27.4 \
        && sudo curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose \
        && sudo chmod 755 /usr/local/bin/docker-compose \
        && docker-compose --version
      ```

   1. Exit Jenkins master

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 01

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

   1. Upgrade the docker compose version

      *Example:*

      ```ShellSession
      $ DOCKER_COMPOSE_VERSION=1.27.4 \
        && sudo curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose \
        && sudo chmod 755 /usr/local/bin/docker-compose \
        && docker-compose --version
      ```

   1. Exit Jenkins agent 01

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 02

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

   1. Upgrade the docker compose version

      *Example:*

      ```ShellSession
      $ DOCKER_COMPOSE_VERSION=1.27.4 \
        && sudo curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose \
        && sudo chmod 755 /usr/local/bin/docker-compose \
        && docker-compose --version
      ```

   1. Exit Jenkins agent 02

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 03

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

   1. Upgrade the docker compose version

      *Example:*

      ```ShellSession
      $ DOCKER_COMPOSE_VERSION=1.27.4 \
        && sudo curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose \
        && sudo chmod 755 /usr/local/bin/docker-compose \
        && docker-compose --version
      ```

   1. Exit Jenkins agent 03

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 04

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

   1. Upgrade the docker compose version

      *Example:*

      ```ShellSession
      $ DOCKER_COMPOSE_VERSION=1.27.4 \
        && sudo curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose \
        && sudo chmod 755 /usr/local/bin/docker-compose \
        && docker-compose --version
      ```

   1. Exit Jenkins agent 04

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 05

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

   1. Upgrade the docker compose version

      *Example:*

      ```ShellSession
      $ DOCKER_COMPOSE_VERSION=1.27.4 \
        && sudo curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose \
        && sudo chmod 755 /usr/local/bin/docker-compose \
        && docker-compose --version
      ```

   1. Exit Jenkins agent 05

      ```ShellSession
      $ exit
      ```

## Install htop on Jenkins agents

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Upgrade the docker compose version on Jenkins agent 01

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

   1. Install htop

      ```ShellSession
      $ sudo yum install htop -y
      ```

   1. Exit Jenkins agent 01

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 02

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

   1. Install htop

      ```ShellSession
      $ sudo yum install htop -y
      ```

   1. Exit Jenkins agent 02

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 03

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

   1. Install htop

      ```ShellSession
      $ sudo yum install htop -y
      ```

   1. Exit Jenkins agent 03

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 04

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

   1. Install htop

      ```ShellSession
      $ sudo yum install htop -y
      ```

   1. Exit Jenkins agent 04

      ```ShellSession
      $ exit
      ```

1. Upgrade the docker compose version on Jenkins agent 05

   1. Get the private IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

   1. Install htop

      ```ShellSession
      $ sudo yum install htop -y
      ```

   1. Exit Jenkins agent 05

      ```ShellSession
      $ exit
      ```

## Configure Jenkins master

### Enable Jenkins

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Note the default Jenkins port

   1. Open the Jenkins config file

      ```ShellSession
      $ sudo cat /etc/sysconfig/jenkins | grep JENKINS_PORT
      ```

   1. Note the output

      *Example:*

      ```
      JENKINS_PORT="8080"
      ```

1. Enable Jenkins

   1. Enter the following
  
      ```ShellSession
      $ sudo systemctl enable jenkins
      ```

   1. Note that the output shows that the system automatically did "chkconfig" on the sercvice because it is not a native service

      ```
      jenkins.service is not a native service, redirecting to /sbin/chkconfig.
      Executing /sbin/chkconfig jenkins on
      ```

1. Check the current status of Jenkins
   
   ```ShellSession
   $ systemctl status jenkins -l
   ```

1. If Jenkins is not running, do the following
   
   1. Start Jenkins

      ```ShellSession
      $ sudo systemctl start jenkins
      ```
   
   1. Re-check the current status of Jenkins

      ```ShellSession
      $ systemctl status jenkins -l
      ```

   1. Note that you should see the following in the output
 
      ```
      Active: active (running)
      ``` 

1. Ensure that Jenkins is running on the default port 8080

   1. Enter the following
    
      ```ShellSession
      $ netstat -an | grep 8080
      ```

   1. Verify that the following is output

      ```
      tcp6       0      0 :::8080                 :::*                    LISTEN
      ```

   1. If no output is displayed, do the following

      1. Restart Jenkins

         ```ShellSession
         $ sudo systemctl restart jenkins
         ```

      1. Repeat the "netstat" port step above

1. Note the public IP of Jenkins master

   ```ShellSession
   $ curl http://checkip.amazonaws.com
   ```

1. Note the private IP of Jenkins master

   ```ShellSession
   $ hostname --ip-address | awk '{print $2}'
   ```

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

### Initialize the Jenkins GUI

1. Open Chrome

1. Enter the following in the address bar

   *Format:*

   > http://{jenkins master public ip}:8080

1. Bookmark Jenkins for your browser

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins master

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
      ```

1. Configure Jenkins

   1. Get the Jenkins administrator password

      ```ShellSession
      $ sudo cat /var/lib/jenkins/secrets/initialAdminPassword
      ```

   1. Copy and paste the password into the **Administrator password** text box of the Jenkins page displayed in Chrome

   1. Select **Continue** on the *Unlock Jenkins* page

   1. Select **Install suggested plugins**

   1. Wait for installation to complete

   1. Enter the following items for your user

      - Username (alphanumeric characters, underscore and dash; e.g. fred-smith)

      - Password

      - Confirm password

      - Full name

      - E-mail address (e.g. fred.smith@semanticbits.com)

   1. Select **Save and Continue**

   1. Note the Jenkins URL

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Select **Save and Finish**

   1. Select **Start using Jenkins**

1. Return to the terminal

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```
   
## Configure Jenkins agent 01

### Install development tools on Jenkins agent 01

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 01

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Install development tools

   ```ShellSession
   $ sudo yum group install "Development Tools" -y
   ```

### Create a directory for jobs on Jenkins agent 01

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 01

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```
   
1. Create a directory for jobs that are built on the Jenkins agent on behalf of the Jenkins Master

   ```ShellSession
   $ mkdir -p /var/lib/jenkins/jenkins_agent
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```
   
1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```
   
### Install python3, pip3, and required pip modules on Jenkins agent 01

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 01

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Check to see if python3 is already installed by entering the following

   ```ShellSession
   $ python3 --version
   ```

1. If python3 is not installed, do the following:

   1. Install python3 requirements

      ```ShellSession
      $ sudo yum install gcc openssl-devel bzip2-devel libffi-devel -y
      ```

   1. Change to the "src" directory

      ```ShellSession
      $ cd /usr/src
      ```

   1. Get the desired version of Python 3

      ```ShellSession
      $ sudo wget https://www.python.org/ftp/python/3.7.5/Python-3.7.5.tgz
      ```

   1. Unzip the Python 3 package

      ```ShellSession
      $ sudo tar xzf Python-3.7.5.tgz
      ```

   1. Install Python 3

      ```ShellSession
      $ cd Python-3.7.5
      $ sudo ./configure --enable-optimizations
      $ sudo make altinstall
      ```

   1. Delete the python 3 package

      ```ShellSession
      $ sudo rm -f Python-3.7.5.tgz
      ```

   1. Verify that Python 3 is installed by checking its version

      ```ShellSession
      $ python3.7 --version
      ```

   1. Create a python3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/python3
      $ sudo ln -s /usr/local/bin/python3.7 /usr/local/bin/python3
      ```

   1. Create a pip3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/pip3
      $ sudo ln -s /usr/local/bin/pip3.7 /usr/local/bin/pip3
      ```

1. Upgrade pip

   ```ShellSession
   $ sudo pip install --upgrade pip
   ```

1. Upgrade pip3

   ```ShellSession
   $ sudo /usr/local/bin/pip3.7 install --upgrade pip
   ```

1. Upgrade pip3 site packages for user

   ```ShellSession
   $ pip3 install --upgrade pip --user
   ```

1. Install required pip modules for jenkins user

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Install lxml

      *Note that "lxml" is a library for parsing XML and HTML.*

      ```ShellSession
      $ pip3 install lxml
      ```

   1. Install requests

      ```ShellSession
      $ pip3 install requests
      ```

   1. Install boto3

      ```ShellSession
      $ pip3 install boto3
      ```

   1. Exit the Jenkins user

      ```ShellSession
      $ exit
      ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Install Terraform on Jenkins agent 01

1. Check the version of terraform that is installed on your development machine

   ```ShellSession
   $ terraform --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Terraform v{terraform version}
   ```

   *Example:*

   ```
   Terraform v0.12.29
   ```
   
1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Set desired terraform version

   *Format:*
   
   ```ShellSession
   $ TERRAFORM_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ TERRAFORM_VERSION=0.12.9
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```
   
1. Download terraform

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip"
   ```

1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ terraform --version
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Configure Terraform logging on Jenkins agent 01

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Create or modify the SSH config file

   1. Open the SSH config file

      ```ShellSession
      $ vim ~/.ssh/config
      ```

   1. Create of modify SSH config file to include the following line at the end of the file

      ```
      StrictHostKeyChecking no
      ```

   1. Save and close the file

1. Ensure terraform log directory exists

   ```ShellSession
   $ sudo mkdir -p /var/log/terraform
   $ sudo chown -R jenkins:jenkins /var/log/terraform
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Install maven on Jenkins agent 01

1. Check the version of maven that is installed on your development machine

   ```ShellSession
   $ mvn --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Apache Maven {maven version}
   ```

   *Example:*

   ```
   Apache Maven 3.6.3
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Change to the "/opt" directory

   ```ShellSession
   $ cd /opt
   ```

1. Set desired maven version

   ```ShellSession
   MAVEN_VERSION=3.6.3
   ```

1. Download maven

   ```ShellSession
   $ sudo wget https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Extract maven from the archive

   ```ShellSession
   $ sudo tar xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Delete the maven archive

   ```ShellSession
   $ sudo rm -f apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```
   
1. Create a symbolic link for maven

   ```ShellSession
   $ sudo ln -s apache-maven-${MAVEN_VERSION} maven
   ```

1. Setup M2_HOME environment variable

   ```ShellSession
   $ echo 'export M2_HOME=/opt/maven' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Add the M2_HOME environment variable to PATH

   ```ShellSession
   $ echo 'export PATH=${M2_HOME}/bin:${PATH}' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Load the maven environment variables

   ```ShellSession
   $ source /etc/profile.d/maven.sh
   ```

1. Verify the maven version

   ```ShellSession
   $ mvn --version
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Install jq on Jenkins agent 01

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Install jq

   ```ShellSession
   $ sudo yum install jq -y
   ```

1. Verify jq by checking its version

   ```ShellSession
   $ jq --version
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Add the jenkins user to the docker group on Jenkins agent 01

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Determine if the jenkins user is already part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. If there is no output, add the jenkins user to the docker group

   ```ShellSession
   $ sudo usermod -a -G docker jenkins
   ```

1. Verify that the jenkins user is now a part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. Note the output

   *Example:*

   ```
   docker:x:988:ec2-user,jenkins
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Ensure jenkins agent 01 can use the Unix socket for the Docker daemon

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Note the default permissions for the Unix socket for the Docker daemon

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the default permissions

      ```
      srw-rw----
      ```

1. Give "other" read and write permissions so that Jenkins build will work

   1. Enter the following

      ```ShellSession
      $ sudo chmod 666 /var/run/docker.sock
      ```

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the modified permissions

      ```
      srw-rw-rw-
      ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Install packer on Jenkins agent 01

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Set desired packer version

   *Format:*
   
   ```ShellSession
   $ PACKER_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ PACKER_VERSION=1.4.3
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Download packer

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip"
   ```
   
1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./packer_${PACKER_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ packer --version
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Install Postgres 11 on Jenkins agent 01

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Get the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
   ```

1. Import the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ sudo rpm --import RPM-GPG-KEY-PGDG-11
   ```

1. Install PostgreSQL 11 RPM

   ```ShellSession
   $ sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ sudo yum -y install postgresql11
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Setup ruby environment on Jenkins agent 01

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```
      
1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Switch to the Jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Add rbenv initialization to "bashrc"

   ```ShellSession
   $ echo 'export PATH="$HOME/.gem/ruby/2.6.0/bin:$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   ```

1. Initialize rbenv for the current session

   ```ShellSession
   $ source ~/.bashrc
   ```

1. Install Ruby 2.6.5

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Set the global version of Ruby

   ```SehellSession
   $ rbenv global 2.6.5
   ```

1. Install bundler to the jenkins user ".gem" directory

   ```ShellSession
   $ gem install --user-install bundler
   ```

1. Note that bundler is now installed where Inspec wants to see it

   ```ShellSession
   $ ls /var/lib/jenkins/.gem/ruby/2.6.0/gems
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --user-install --system
   ```

1. Verify ruby by checking its version

   ```ShellSession
   $ ruby --version
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```

1. Exit the Jenkins agent 01

   ```ShellSession
   $ exit
   ```

### Install postgresql10-devel on Jenkins agent 01

1. Note that I couldn't install postgresql11-devel due to dependencies that could not be installed, so I am using "postgresql10-devel" instead

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_01_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_01_PRIVATE_IP
      ```
      
1. Install postgresql10-devel

   ```ShellSession
   $ sudo yum -y install postgresql10-devel
   ```

## Configure Jenkins agent 02

### Install development tools on Jenkins agent 02

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 02

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Install development tools

   ```ShellSession
   $ sudo yum group install "Development Tools" -y
   ```

### Create a directory for jobs on Jenkins agent 02

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 02

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```
   
1. Create a directory for jobs that are built on the Jenkins agent on behalf of the Jenkins Master

   ```ShellSession
   $ mkdir -p /var/lib/jenkins/jenkins_agent
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```
   
1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```
   
### Install python3, pip3, and required pip modules on Jenkins agent 02

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 02

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Check to see if python3 is already installed by entering the following

   ```ShellSession
   $ python3 --version
   ```

1. If python3 is not installed, do the following:

   1. Install python3 requirements

      ```ShellSession
      $ sudo yum install gcc openssl-devel bzip2-devel libffi-devel -y
      ```

   1. Change to the "src" directory

      ```ShellSession
      $ cd /usr/src
      ```

   1. Get the desired version of Python 3

      ```ShellSession
      $ sudo wget https://www.python.org/ftp/python/3.7.5/Python-3.7.5.tgz
      ```

   1. Unzip the Python 3 package

      ```ShellSession
      $ sudo tar xzf Python-3.7.5.tgz
      ```

   1. Install Python 3

      ```ShellSession
      $ cd Python-3.7.5
      $ sudo ./configure --enable-optimizations
      $ sudo make altinstall
      ```

   1. Delete the python 3 package

      ```ShellSession
      $ sudo rm -f Python-3.7.5.tgz
      ```

   1. Verify that Python 3 is installed by checking its version

      ```ShellSession
      $ python3.7 --version
      ```

   1. Create a python3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/python3
      $ sudo ln -s /usr/local/bin/python3.7 /usr/local/bin/python3
      ```

   1. Create a pip3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/pip3
      $ sudo ln -s /usr/local/bin/pip3.7 /usr/local/bin/pip3
      ```

1. Upgrade pip

   ```ShellSession
   $ sudo pip install --upgrade pip
   ```

1. Upgrade pip3

   ```ShellSession
   $ sudo /usr/local/bin/pip3.7 install --upgrade pip
   ```

1. Upgrade pip3 site packages for user

   ```ShellSession
   $ pip3 install --upgrade pip --user
   ```

1. Install required pip modules for jenkins user

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Install lxml

      *Note that "lxml" is a library for parsing XML and HTML.*

      ```ShellSession
      $ pip3 install lxml
      ```

   1. Install requests

      ```ShellSession
      $ pip3 install requests
      ```

   1. Install boto3

      ```ShellSession
      $ pip3 install boto3
      ```

   1. Exit the Jenkins user

      ```ShellSession
      $ exit
      ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Install Terraform on Jenkins agent 02

1. Check the version of terraform that is installed on your development machine

   ```ShellSession
   $ terraform --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Terraform v{terraform version}
   ```

   *Example:*

   ```
   Terraform v0.12.29
   ```
   
1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Set desired terraform version

   *Format:*
   
   ```ShellSession
   $ TERRAFORM_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ TERRAFORM_VERSION=0.12.9
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```
   
1. Download terraform

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip"
   ```

1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ terraform --version
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Configure Terraform logging on Jenkins agent 02

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Create or modify the SSH config file

   1. Open the SSH config file

      ```ShellSession
      $ vim ~/.ssh/config
      ```

   1. Create of modify SSH config file to include the following line at the end of the file

      ```
      StrictHostKeyChecking no
      ```

   1. Save and close the file

1. Ensure terraform log directory exists

   ```ShellSession
   $ sudo mkdir -p /var/log/terraform
   $ sudo chown -R jenkins:jenkins /var/log/terraform
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Install maven on Jenkins agent 02

1. Check the version of maven that is installed on your development machine

   ```ShellSession
   $ mvn --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Apache Maven {maven version}
   ```

   *Example:*

   ```
   Apache Maven 3.6.3
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Change to the "/opt" directory

   ```ShellSession
   $ cd /opt
   ```

1. Set desired maven version

   ```ShellSession
   MAVEN_VERSION=3.6.3
   ```

1. Download maven

   ```ShellSession
   $ sudo wget https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Extract maven from the archive

   ```ShellSession
   $ sudo tar xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Delete the maven archive

   ```ShellSession
   $ sudo rm -f apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```
   
1. Create a symbolic link for maven

   ```ShellSession
   $ sudo ln -s apache-maven-${MAVEN_VERSION} maven
   ```

1. Setup M2_HOME environment variable

   ```ShellSession
   $ echo 'export M2_HOME=/opt/maven' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Add the M2_HOME environment variable to PATH

   ```ShellSession
   $ echo 'export PATH=${M2_HOME}/bin:${PATH}' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Load the maven environment variables

   ```ShellSession
   $ source /etc/profile.d/maven.sh
   ```

1. Verify the maven version

   ```ShellSession
   $ mvn --version
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Install jq on Jenkins agent 02

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Install jq

   ```ShellSession
   $ sudo yum install jq -y
   ```

1. Verify jq by checking its version

   ```ShellSession
   $ jq --version
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Add the jenkins user to the docker group on Jenkins agent 02

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Determine if the jenkins user is already part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. If there is no output, add the jenkins user to the docker group

   ```ShellSession
   $ sudo usermod -a -G docker jenkins
   ```

1. Verify that the jenkins user is now a part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. Note the output

   *Example:*

   ```
   docker:x:988:ec2-user,jenkins
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Ensure jenkins agent 02 can use the Unix socket for the Docker daemon

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Note the default permissions for the Unix socket for the Docker daemon

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the default permissions

      ```
      srw-rw----
      ```

1. Give "other" read and write permissions so that Jenkins build will work

   1. Enter the following

      ```ShellSession
      $ sudo chmod 666 /var/run/docker.sock
      ```

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the modified permissions

      ```
      srw-rw-rw-
      ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Install packer on Jenkins agent 02

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Set desired packer version

   *Format:*
   
   ```ShellSession
   $ PACKER_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ PACKER_VERSION=1.4.3
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Download packer

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip"
   ```
   
1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./packer_${PACKER_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ packer --version
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Install Postgres 11 on Jenkins agent 02

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Get the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
   ```

1. Import the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ sudo rpm --import RPM-GPG-KEY-PGDG-11
   ```

1. Install PostgreSQL 11 RPM

   ```ShellSession
   $ sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ sudo yum -y install postgresql11
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Setup ruby environment on Jenkins agent 02

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```
      
1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Switch to the Jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Add rbenv initialization to "bashrc"

   ```ShellSession
   $ echo 'export PATH="$HOME/.gem/ruby/2.6.0/bin:$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   ```

1. Initialize rbenv for the current session

   ```ShellSession
   $ source ~/.bashrc
   ```

1. Install Ruby 2.6.5

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Set the global version of Ruby

   ```SehellSession
   $ rbenv global 2.6.5
   ```

1. Install bundler to the jenkins user ".gem" directory

   ```ShellSession
   $ gem install --user-install bundler
   ```

1. Note that bundler is now installed where Inspec wants to see it

   ```ShellSession
   $ ls /var/lib/jenkins/.gem/ruby/2.6.0/gems
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --user-install --system
   ```

1. Verify ruby by checking its version

   ```ShellSession
   $ ruby --version
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```

1. Exit the Jenkins agent 02

   ```ShellSession
   $ exit
   ```

### Install postgresql10-devel on Jenkins agent 02

1. Note that I couldn't install postgresql11-devel due to dependencies that could not be installed, so I am using "postgresql10-devel" instead

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_02_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_02_PRIVATE_IP
      ```
      
1. Install postgresql10-devel

   ```ShellSession
   $ sudo yum -y install postgresql10-devel
   ```

## Configure Jenkins agent 03

### Install development tools on Jenkins agent 03

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 03

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Install development tools

   ```ShellSession
   $ sudo yum group install "Development Tools" -y
   ```

### Create a directory for jobs on Jenkins agent 03

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 03

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```
   
1. Create a directory for jobs that are built on the Jenkins agent on behalf of the Jenkins Master

   ```ShellSession
   $ mkdir -p /var/lib/jenkins/jenkins_agent
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```
   
1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```
   
### Install python3, pip3, and required pip modules on Jenkins agent 03

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 03

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Check to see if python3 is already installed by entering the following

   ```ShellSession
   $ python3 --version
   ```

1. If python3 is not installed, do the following:

   1. Install python3 requirements

      ```ShellSession
      $ sudo yum install gcc openssl-devel bzip2-devel libffi-devel -y
      ```

   1. Change to the "src" directory

      ```ShellSession
      $ cd /usr/src
      ```

   1. Get the desired version of Python 3

      ```ShellSession
      $ sudo wget https://www.python.org/ftp/python/3.7.5/Python-3.7.5.tgz
      ```

   1. Unzip the Python 3 package

      ```ShellSession
      $ sudo tar xzf Python-3.7.5.tgz
      ```

   1. Install Python 3

      ```ShellSession
      $ cd Python-3.7.5
      $ sudo ./configure --enable-optimizations
      $ sudo make altinstall
      ```

   1. Delete the python 3 package

      ```ShellSession
      $ sudo rm -f Python-3.7.5.tgz
      ```

   1. Verify that Python 3 is installed by checking its version

      ```ShellSession
      $ python3.7 --version
      ```

   1. Create a python3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/python3
      $ sudo ln -s /usr/local/bin/python3.7 /usr/local/bin/python3
      ```

   1. Create a pip3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/pip3
      $ sudo ln -s /usr/local/bin/pip3.7 /usr/local/bin/pip3
      ```

1. Upgrade pip

   ```ShellSession
   $ sudo pip install --upgrade pip
   ```

1. Upgrade pip3

   ```ShellSession
   $ sudo /usr/local/bin/pip3.7 install --upgrade pip
   ```

1. Upgrade pip3 site packages for user

   ```ShellSession
   $ pip3 install --upgrade pip --user
   ```

1. Install required pip modules for jenkins user

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Install lxml

      *Note that "lxml" is a library for parsing XML and HTML.*

      ```ShellSession
      $ pip3 install lxml
      ```

   1. Install requests

      ```ShellSession
      $ pip3 install requests
      ```

   1. Install boto3

      ```ShellSession
      $ pip3 install boto3
      ```

   1. Exit the Jenkins user

      ```ShellSession
      $ exit
      ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Install Terraform on Jenkins agent 03

1. Check the version of terraform that is installed on your development machine

   ```ShellSession
   $ terraform --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Terraform v{terraform version}
   ```

   *Example:*

   ```
   Terraform v0.12.29
   ```
   
1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Set desired terraform version

   *Format:*
   
   ```ShellSession
   $ TERRAFORM_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ TERRAFORM_VERSION=0.12.9
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```
   
1. Download terraform

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip"
   ```

1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ terraform --version
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Configure Terraform logging on Jenkins agent 03

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Create or modify the SSH config file

   1. Open the SSH config file

      ```ShellSession
      $ vim ~/.ssh/config
      ```

   1. Create of modify SSH config file to include the following line at the end of the file

      ```
      StrictHostKeyChecking no
      ```

   1. Save and close the file

1. Ensure terraform log directory exists

   ```ShellSession
   $ sudo mkdir -p /var/log/terraform
   $ sudo chown -R jenkins:jenkins /var/log/terraform
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Install maven on Jenkins agent 03

1. Check the version of maven that is installed on your development machine

   ```ShellSession
   $ mvn --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Apache Maven {maven version}
   ```

   *Example:*

   ```
   Apache Maven 3.6.3
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Change to the "/opt" directory

   ```ShellSession
   $ cd /opt
   ```

1. Set desired maven version

   ```ShellSession
   MAVEN_VERSION=3.6.3
   ```

1. Download maven

   ```ShellSession
   $ sudo wget https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Extract maven from the archive

   ```ShellSession
   $ sudo tar xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Delete the maven archive

   ```ShellSession
   $ sudo rm -f apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```
   
1. Create a symbolic link for maven

   ```ShellSession
   $ sudo ln -s apache-maven-${MAVEN_VERSION} maven
   ```

1. Setup M2_HOME environment variable

   ```ShellSession
   $ echo 'export M2_HOME=/opt/maven' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Add the M2_HOME environment variable to PATH

   ```ShellSession
   $ echo 'export PATH=${M2_HOME}/bin:${PATH}' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Load the maven environment variables

   ```ShellSession
   $ source /etc/profile.d/maven.sh
   ```

1. Verify the maven version

   ```ShellSession
   $ mvn --version
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Install jq on Jenkins agent 03

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Install jq

   ```ShellSession
   $ sudo yum install jq -y
   ```

1. Verify jq by checking its version

   ```ShellSession
   $ jq --version
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Add the jenkins user to the docker group on Jenkins agent 03

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Determine if the jenkins user is already part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. If there is no output, add the jenkins user to the docker group

   ```ShellSession
   $ sudo usermod -a -G docker jenkins
   ```

1. Verify that the jenkins user is now a part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. Note the output

   *Example:*

   ```
   docker:x:988:ec2-user,jenkins
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Ensure jenkins agent 03 can use the Unix socket for the Docker daemon

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Note the default permissions for the Unix socket for the Docker daemon

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the default permissions

      ```
      srw-rw----
      ```

1. Give "other" read and write permissions so that Jenkins build will work

   1. Enter the following

      ```ShellSession
      $ sudo chmod 666 /var/run/docker.sock
      ```

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the modified permissions

      ```
      srw-rw-rw-
      ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Install packer on Jenkins agent 03

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Set desired packer version

   *Format:*
   
   ```ShellSession
   $ PACKER_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ PACKER_VERSION=1.4.3
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Download packer

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip"
   ```
   
1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./packer_${PACKER_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ packer --version
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Install Postgres 11 on Jenkins agent 03

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Get the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
   ```

1. Import the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ sudo rpm --import RPM-GPG-KEY-PGDG-11
   ```

1. Install PostgreSQL 11 RPM

   ```ShellSession
   $ sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ sudo yum -y install postgresql11
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Setup ruby environment on Jenkins agent 03

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```
      
1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Switch to the Jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Add rbenv initialization to "bashrc"

   ```ShellSession
   $ echo 'export PATH="$HOME/.gem/ruby/2.6.0/bin:$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   ```

1. Initialize rbenv for the current session

   ```ShellSession
   $ source ~/.bashrc
   ```

1. Install Ruby 2.6.5

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Set the global version of Ruby

   ```SehellSession
   $ rbenv global 2.6.5
   ```

1. Install bundler to the jenkins user ".gem" directory

   ```ShellSession
   $ gem install --user-install bundler
   ```

1. Note that bundler is now installed where Inspec wants to see it

   ```ShellSession
   $ ls /var/lib/jenkins/.gem/ruby/2.6.0/gems
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --user-install --system
   ```

1. Verify ruby by checking its version

   ```ShellSession
   $ ruby --version
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```

1. Exit the Jenkins agent 03

   ```ShellSession
   $ exit
   ```

### Install postgresql10-devel on Jenkins agent 03

1. Note that I couldn't install postgresql11-devel due to dependencies that could not be installed, so I am using "postgresql10-devel" instead

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_03_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_03_PRIVATE_IP
      ```
      
1. Install postgresql10-devel

   ```ShellSession
   $ sudo yum -y install postgresql10-devel
   ```

## Configure Jenkins agent 04

### Install development tools on Jenkins agent 04

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 04

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Install development tools

   ```ShellSession
   $ sudo yum group install "Development Tools" -y
   ```

### Create a directory for jobs on Jenkins agent 04

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 04

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```
   
1. Create a directory for jobs that are built on the Jenkins agent on behalf of the Jenkins Master

   ```ShellSession
   $ mkdir -p /var/lib/jenkins/jenkins_agent
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```
   
1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```
   
### Install python3, pip3, and required pip modules on Jenkins agent 04

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 04

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Check to see if python3 is already installed by entering the following

   ```ShellSession
   $ python3 --version
   ```

1. If python3 is not installed, do the following:

   1. Install python3 requirements

      ```ShellSession
      $ sudo yum install gcc openssl-devel bzip2-devel libffi-devel -y
      ```

   1. Change to the "src" directory

      ```ShellSession
      $ cd /usr/src
      ```

   1. Get the desired version of Python 3

      ```ShellSession
      $ sudo wget https://www.python.org/ftp/python/3.7.5/Python-3.7.5.tgz
      ```

   1. Unzip the Python 3 package

      ```ShellSession
      $ sudo tar xzf Python-3.7.5.tgz
      ```

   1. Install Python 3

      ```ShellSession
      $ cd Python-3.7.5
      $ sudo ./configure --enable-optimizations
      $ sudo make altinstall
      ```

   1. Delete the python 3 package

      ```ShellSession
      $ sudo rm -f Python-3.7.5.tgz
      ```

   1. Verify that Python 3 is installed by checking its version

      ```ShellSession
      $ python3.7 --version
      ```

   1. Create a python3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/python3
      $ sudo ln -s /usr/local/bin/python3.7 /usr/local/bin/python3
      ```

   1. Create a pip3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/pip3
      $ sudo ln -s /usr/local/bin/pip3.7 /usr/local/bin/pip3
      ```

1. Upgrade pip

   ```ShellSession
   $ sudo pip install --upgrade pip
   ```

1. Upgrade pip3

   ```ShellSession
   $ sudo /usr/local/bin/pip3.7 install --upgrade pip
   ```

1. Upgrade pip3 site packages for user

   ```ShellSession
   $ pip3 install --upgrade pip --user
   ```

1. Install required pip modules for jenkins user

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Install lxml

      *Note that "lxml" is a library for parsing XML and HTML.*

      ```ShellSession
      $ pip3 install lxml
      ```

   1. Install requests

      ```ShellSession
      $ pip3 install requests
      ```

   1. Install boto3

      ```ShellSession
      $ pip3 install boto3
      ```

   1. Exit the Jenkins user

      ```ShellSession
      $ exit
      ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Install Terraform on Jenkins agent 04

1. Check the version of terraform that is installed on your development machine

   ```ShellSession
   $ terraform --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Terraform v{terraform version}
   ```

   *Example:*

   ```
   Terraform v0.12.29
   ```
   
1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Set desired terraform version

   *Format:*
   
   ```ShellSession
   $ TERRAFORM_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ TERRAFORM_VERSION=0.12.9
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```
   
1. Download terraform

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip"
   ```

1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ terraform --version
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Configure Terraform logging on Jenkins agent 04

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Create or modify the SSH config file

   1. Open the SSH config file

      ```ShellSession
      $ vim ~/.ssh/config
      ```

   1. Create of modify SSH config file to include the following line at the end of the file

      ```
      StrictHostKeyChecking no
      ```

   1. Save and close the file

1. Ensure terraform log directory exists

   ```ShellSession
   $ sudo mkdir -p /var/log/terraform
   $ sudo chown -R jenkins:jenkins /var/log/terraform
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Install maven on Jenkins agent 04

1. Check the version of maven that is installed on your development machine

   ```ShellSession
   $ mvn --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Apache Maven {maven version}
   ```

   *Example:*

   ```
   Apache Maven 3.6.3
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Change to the "/opt" directory

   ```ShellSession
   $ cd /opt
   ```

1. Set desired maven version

   ```ShellSession
   MAVEN_VERSION=3.6.3
   ```

1. Download maven

   ```ShellSession
   $ sudo wget https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Extract maven from the archive

   ```ShellSession
   $ sudo tar xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Delete the maven archive

   ```ShellSession
   $ sudo rm -f apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```
   
1. Create a symbolic link for maven

   ```ShellSession
   $ sudo ln -s apache-maven-${MAVEN_VERSION} maven
   ```

1. Setup M2_HOME environment variable

   ```ShellSession
   $ echo 'export M2_HOME=/opt/maven' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Add the M2_HOME environment variable to PATH

   ```ShellSession
   $ echo 'export PATH=${M2_HOME}/bin:${PATH}' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Load the maven environment variables

   ```ShellSession
   $ source /etc/profile.d/maven.sh
   ```

1. Verify the maven version

   ```ShellSession
   $ mvn --version
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Install jq on Jenkins agent 04

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Install jq

   ```ShellSession
   $ sudo yum install jq -y
   ```

1. Verify jq by checking its version

   ```ShellSession
   $ jq --version
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Add the jenkins user to the docker group on Jenkins agent 04

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Determine if the jenkins user is already part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. If there is no output, add the jenkins user to the docker group

   ```ShellSession
   $ sudo usermod -a -G docker jenkins
   ```

1. Verify that the jenkins user is now a part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. Note the output

   *Example:*

   ```
   docker:x:988:ec2-user,jenkins
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Ensure jenkins agent 04 can use the Unix socket for the Docker daemon

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Note the default permissions for the Unix socket for the Docker daemon

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the default permissions

      ```
      srw-rw----
      ```

1. Give "other" read and write permissions so that Jenkins build will work

   1. Enter the following

      ```ShellSession
      $ sudo chmod 666 /var/run/docker.sock
      ```

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the modified permissions

      ```
      srw-rw-rw-
      ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Install packer on Jenkins agent 04

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Set desired packer version

   *Format:*
   
   ```ShellSession
   $ PACKER_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ PACKER_VERSION=1.4.3
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Download packer

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip"
   ```
   
1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./packer_${PACKER_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ packer --version
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Install Postgres 11 on Jenkins agent 04

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Get the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
   ```

1. Import the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ sudo rpm --import RPM-GPG-KEY-PGDG-11
   ```

1. Install PostgreSQL 11 RPM

   ```ShellSession
   $ sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ sudo yum -y install postgresql11
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Setup ruby environment on Jenkins agent 04

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```
      
1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Switch to the Jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Add rbenv initialization to "bashrc"

   ```ShellSession
   $ echo 'export PATH="$HOME/.gem/ruby/2.6.0/bin:$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   ```

1. Initialize rbenv for the current session

   ```ShellSession
   $ source ~/.bashrc
   ```

1. Install Ruby 2.6.5

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Set the global version of Ruby

   ```SehellSession
   $ rbenv global 2.6.5
   ```

1. Install bundler to the jenkins user ".gem" directory

   ```ShellSession
   $ gem install --user-install bundler
   ```

1. Note that bundler is now installed where Inspec wants to see it

   ```ShellSession
   $ ls /var/lib/jenkins/.gem/ruby/2.6.0/gems
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --user-install --system
   ```

1. Verify ruby by checking its version

   ```ShellSession
   $ ruby --version
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```

1. Exit the Jenkins agent 04

   ```ShellSession
   $ exit
   ```

### Install postgresql10-devel on Jenkins agent 04

1. Note that I couldn't install postgresql11-devel due to dependencies that could not be installed, so I am using "postgresql10-devel" instead

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_04_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_04_PRIVATE_IP
      ```
      
1. Install postgresql10-devel

   ```ShellSession
   $ sudo yum -y install postgresql10-devel
   ```

## Configure Jenkins agent 05

### Install development tools on Jenkins agent 05

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent 05

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Install development tools

   ```ShellSession
   $ sudo yum group install "Development Tools" -y
   ```

### Create a directory for jobs on Jenkins agent 05

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 05

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```
   
1. Create a directory for jobs that are built on the Jenkins agent on behalf of the Jenkins Master

   ```ShellSession
   $ mkdir -p /var/lib/jenkins/jenkins_agent
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```
   
1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```
   
### Install python3, pip3, and required pip modules on Jenkins agent 05

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent 05

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Check to see if python3 is already installed by entering the following

   ```ShellSession
   $ python3 --version
   ```

1. If python3 is not installed, do the following:

   1. Install python3 requirements

      ```ShellSession
      $ sudo yum install gcc openssl-devel bzip2-devel libffi-devel -y
      ```

   1. Change to the "src" directory

      ```ShellSession
      $ cd /usr/src
      ```

   1. Get the desired version of Python 3

      ```ShellSession
      $ sudo wget https://www.python.org/ftp/python/3.7.5/Python-3.7.5.tgz
      ```

   1. Unzip the Python 3 package

      ```ShellSession
      $ sudo tar xzf Python-3.7.5.tgz
      ```

   1. Install Python 3

      ```ShellSession
      $ cd Python-3.7.5
      $ sudo ./configure --enable-optimizations
      $ sudo make altinstall
      ```

   1. Delete the python 3 package

      ```ShellSession
      $ sudo rm -f Python-3.7.5.tgz
      ```

   1. Verify that Python 3 is installed by checking its version

      ```ShellSession
      $ python3.7 --version
      ```

   1. Create a python3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/python3
      $ sudo ln -s /usr/local/bin/python3.7 /usr/local/bin/python3
      ```

   1. Create a pip3 symlink

      ```ShellSession
      $ sudo rm -f /usr/local/bin/pip3
      $ sudo ln -s /usr/local/bin/pip3.7 /usr/local/bin/pip3
      ```

1. Upgrade pip

   ```ShellSession
   $ sudo pip install --upgrade pip
   ```

1. Upgrade pip3

   ```ShellSession
   $ sudo /usr/local/bin/pip3.7 install --upgrade pip
   ```

1. Upgrade pip3 site packages for user

   ```ShellSession
   $ pip3 install --upgrade pip --user
   ```

1. Install required pip modules for jenkins user

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Install lxml

      *Note that "lxml" is a library for parsing XML and HTML.*

      ```ShellSession
      $ pip3 install lxml
      ```

   1. Install requests

      ```ShellSession
      $ pip3 install requests
      ```

   1. Install boto3

      ```ShellSession
      $ pip3 install boto3
      ```

   1. Exit the Jenkins user

      ```ShellSession
      $ exit
      ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Install Terraform on Jenkins agent 05

1. Check the version of terraform that is installed on your development machine

   ```ShellSession
   $ terraform --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Terraform v{terraform version}
   ```

   *Example:*

   ```
   Terraform v0.12.29
   ```
   
1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Set desired terraform version

   *Format:*
   
   ```ShellSession
   $ TERRAFORM_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ TERRAFORM_VERSION=0.12.9
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```
   
1. Download terraform

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip"
   ```

1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./terraform_${TERRAFORM_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ terraform --version
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Configure Terraform logging on Jenkins agent 05

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Create or modify the SSH config file

   1. Open the SSH config file

      ```ShellSession
      $ vim ~/.ssh/config
      ```

   1. Create of modify SSH config file to include the following line at the end of the file

      ```
      StrictHostKeyChecking no
      ```

   1. Save and close the file

1. Ensure terraform log directory exists

   ```ShellSession
   $ sudo mkdir -p /var/log/terraform
   $ sudo chown -R jenkins:jenkins /var/log/terraform
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Install maven on Jenkins agent 05

1. Check the version of maven that is installed on your development machine

   ```ShellSession
   $ mvn --version
   ```

1. Note the terraform version

   *Format:*

   ```
   Apache Maven {maven version}
   ```

   *Example:*

   ```
   Apache Maven 3.6.3
   ```

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Change to the "/opt" directory

   ```ShellSession
   $ cd /opt
   ```

1. Set desired maven version

   ```ShellSession
   MAVEN_VERSION=3.6.3
   ```

1. Download maven

   ```ShellSession
   $ sudo wget https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Extract maven from the archive

   ```ShellSession
   $ sudo tar xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```

1. Delete the maven archive

   ```ShellSession
   $ sudo rm -f apache-maven-${MAVEN_VERSION}-bin.tar.gz
   ```
   
1. Create a symbolic link for maven

   ```ShellSession
   $ sudo ln -s apache-maven-${MAVEN_VERSION} maven
   ```

1. Setup M2_HOME environment variable

   ```ShellSession
   $ echo 'export M2_HOME=/opt/maven' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Add the M2_HOME environment variable to PATH

   ```ShellSession
   $ echo 'export PATH=${M2_HOME}/bin:${PATH}' \
     | sudo tee --append /etc/profile.d/maven.sh \
     > /dev/null
   ```

1. Load the maven environment variables

   ```ShellSession
   $ source /etc/profile.d/maven.sh
   ```

1. Verify the maven version

   ```ShellSession
   $ mvn --version
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Install jq on Jenkins agent 05

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Install jq

   ```ShellSession
   $ sudo yum install jq -y
   ```

1. Verify jq by checking its version

   ```ShellSession
   $ jq --version
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Add the jenkins user to the docker group on Jenkins agent 05

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Determine if the jenkins user is already part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. If there is no output, add the jenkins user to the docker group

   ```ShellSession
   $ sudo usermod -a -G docker jenkins
   ```

1. Verify that the jenkins user is now a part of the docker group

   ```ShellSession
   $ cat /etc/group | grep docker | grep jenkins
   ```

1. Note the output

   *Example:*

   ```
   docker:x:988:ec2-user,jenkins
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Ensure jenkins agent 05 can use the Unix socket for the Docker daemon

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Note the default permissions for the Unix socket for the Docker daemon

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the default permissions

      ```
      srw-rw----
      ```

1. Give "other" read and write permissions so that Jenkins build will work

   1. Enter the following

      ```ShellSession
      $ sudo chmod 666 /var/run/docker.sock
      ```

   1. Enter the following

      ```ShellSession
      $ ls -al /var/run/docker.sock
      ```

   1. Note the modified permissions

      ```
      srw-rw-rw-
      ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Install packer on Jenkins agent 05

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Set desired packer version

   *Format:*
   
   ```ShellSession
   $ PACKER_VERSION={terraform version}
   ```

   *Example:*
   
   ```ShellSession
   $ PACKER_VERSION=1.4.3
   ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Download packer

   ```ShellSession
   $ sudo wget "https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip"
   ```
   
1. Extract the downloaded file

   ```ShellSession
   $ sudo unzip ./packer_${PACKER_VERSION}_linux_amd64.zip -d /usr/local/bin
   ```

1. Check the terraform version

   ```ShellSession
   $ packer --version
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Install Postgres 11 on Jenkins agent 05

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```

1. Change to the "/tmp" directory

   ```ShellSession
   $ cd /tmp
   ```

1. Get the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ wget https://download.postgresql.org/pub/repos/yum/RPM-GPG-KEY-PGDG-11
   ```

1. Import the RPM GPG key for PostgreSQL 11

   ```ShellSession
   $ sudo rpm --import RPM-GPG-KEY-PGDG-11
   ```

1. Install PostgreSQL 11 RPM

   ```ShellSession
   $ sudo yum -y install https://download.postgresql.org/pub/repos/yum/11/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
   ```

1. Install PostgreSQL 11

   ```ShellSession
   $ sudo yum -y install postgresql11
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Setup ruby environment on Jenkins agent 05

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```
      
1. Install rbenv dependencies

   ```ShellSession
   $ sudo yum install -y git-core zlib zlib-devel gcc-c++ patch readline \
     readline-devel libyaml-devel libffi-devel openssl-devel make bzip2 \
     autoconf automake libtool bison curl sqlite-devel
   ```

1. Switch to the Jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Install rbenv and ruby-build

   ```ShellSession
   $ curl -sL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-installer | bash -
   ```

1. Add rbenv initialization to "bashrc"

   ```ShellSession
   $ echo 'export PATH="$HOME/.gem/ruby/2.6.0/bin:$HOME/.rbenv/bin:$PATH"' >> ~/.bashrc
   $ echo 'eval "$(rbenv init -)"' >> ~/.bashrc
   ```

1. Initialize rbenv for the current session

   ```ShellSession
   $ source ~/.bashrc
   ```

1. Install Ruby 2.6.5

   ```ShellSession
   $ rbenv install 2.6.5
   ```

1. Set the global version of Ruby

   ```SehellSession
   $ rbenv global 2.6.5
   ```

1. Install bundler to the jenkins user ".gem" directory

   ```ShellSession
   $ gem install --user-install bundler
   ```

1. Note that bundler is now installed where Inspec wants to see it

   ```ShellSession
   $ ls /var/lib/jenkins/.gem/ruby/2.6.0/gems
   ```

1. Update Ruby Gems

   ```ShellSession
   $ gem update --user-install --system
   ```

1. Verify ruby by checking its version

   ```ShellSession
   $ ruby --version
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```

1. Exit the Jenkins agent 05

   ```ShellSession
   $ exit
   ```

### Install postgresql10-devel on Jenkins agent 05

1. Note that I couldn't install postgresql11-devel due to dependencies that could not be installed, so I am using "postgresql10-devel" instead

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_05_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_05_PRIVATE_IP
      ```
      
1. Install postgresql10-devel

   ```ShellSession
   $ sudo yum -y install postgresql10-devel
   ```

## Create GitHub user for Jenkins automation

1. Create a login entry in 1Password for the the GitHub user

   1. Open 1Password
   
   1. Select **+**

   1. Select **Login**

   1. Configure the login entry as follows
   
      - **Login:** github.com

      - **Vault:** ab2d

      - **username:** ab2d-jenkins

      - **password:** {genrate one using 1password}

      - **website:** https://github.com

      - **label:** email

      - **new field:** ab2d@semanticbits.com

   1. Select **Save**

1. Open Chrome

1. Enter the following in the address bar

   > https://github.com/

1. If you are logged into an existing Public GitHub account, log off of it

1. Type the desired account name in the **Username** text box

   *Note that it will warn you if the username is already taken.*

   ```
   ab2d-jenkins
   ```

1. Type the email that will be tied to this account

   *Note that it will warn you if the email is already tied to a different GitHib account.*

   *Example:*
   
   ```
   ab2d@semanticbits.com
   ```

1. Complete the initial setup (no need to create a repository)

1. Select the profile icon in the upper right of the page

1. Select **Settings**

1. Select **Security** from the leftmost panel

1. Select **Enable two-factor authentication**

1. Select **Set up using SMS**

1. Select **Download** to download the recovery codes

1. Print out a hard-copy of the recovery codes

1. Save the first three recovery codes from the left column in 1Password

   1. Open 1Password

   1. Select github.com for ab2d-jenkins

   1. Select **Edit**

   1. Add to the entry as follows

      *Format:*
      
      - **label:** recovery code 1

      - **new field:** {first recovery code}

      - **label:** recovery code 2

      - **new field:** {second recovery code}

      - **label:** recovery code 3

      - **new field:** {third recovery code}

   1. Select **Save**

   1. Return to the current GitHub page in Chrome

   1. Select **Next**

   1. Type in your cell phone number in the **Phone number** text box

   1. Select **Send authentication code**

   1. Type in the authentication code that you received into the **Enter the six-digit code sent to your phone** text box

   1. Select **Enable**

1. If you have a second cell phone, do the following

   1. Select **Add fallback SMS number**

   1. Type in your sceond cell phone number in the **Phone number** text box

   1. Select **Set fallback**

1. Create a personal access token

   1. Scroll down

   1. Select **Back to settings**

   1. Select **Developer settings** from the leftmost section

   1. Select **Personal access tokens**

   1. Select **Generate new token**

   1. Configure the "New personal access token" page as follows

      **Note:** jenkins

      **Repo:** checked

      **admin:repo_hook:** checked

      **admin:org_hook:** checked

   1. Select **Generate token**

   1. Select the copy to clipboard icon

   1. Open 1Password

   1. Select github.com for ab2d-jenkins

   1. Select **Edit**

   1. Add to the entry as follows

      *Format:*
      
      - **label:** personal access token

      - **new field:** {personal access token}

   1. Select **Save**

1. Return to the current GitHub page in Chrome

1. Log off the ab2d-jenkins user

## Configure Jenkins for AB2D

### Configure jenkins SSH credentials

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get the public IP address of Jenkins master

   ```ShellSession
   $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Copy the private key to the clipboard

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP \
     sudo -u jenkins cat /var/lib/jenkins/.ssh/id_rsa \
     | pbcopy
   ```

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Credentials** under the "Security" section

1. Select **System** under "Credentials" from the leftmost panel

1. Select **(global)**

1. Select **Add credentials** from the leftmost panel

1. Note that the username to configure in the next step is the "jenkins" linux user associated with the SSH private key

1. Configure the credentials

   - **Kind:** SSH Username with private key

   - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

   - **ID:** {blank}

   - **Description:** {blank}

   - **Username:** jenkins

   - **Private Key - Enter directly:** selected

   - Select **Add**

   - **Enter New Secret Below:** {paste contents of private key for jenkins}

   - **Passphrase:** {blank}

1. Select **OK** on the *Add Credentials* page

1. Select **Jenkins** in the top left of the page

### Configure "personal access token" public GitHub credentials

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Credentials** under the "Security" section

1. Select **System** under "Credentials" from the leftmost panel

1. Select **(global)** under the "Credentials" section

1. Select **Add credentials** from the leftmost panel

1. Configure the credentials

   - **Kind:** Secret text

   - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

   - **Secret:** {personal access token for lhanekam}

   - **ID:** GITHUB_AB2D_JENKINS

   - **Description:** {blank}

1. Select **OK** on the *Add Credentials* page

1. Select **Jenkins** in the top left of the page

### Configure public GitHub credentials

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Note that username and password for the GitHub credentials will be as follows

   - username: ab2d-jenkins

   - password: {personal access token for ab2d-jenkins; not the github password}

1. Select **Credentials** from the leftmost panel

1. Select **System** under *Credentials* from the leftmost panel

1. Select **Global credentials (unrestricted)**

1. Select **Add credentials** from the leftmost panel

1. Configure the credentials

   - **Kind:** Username with password

   - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

   - **Username:** ab2d-jenkins (switched to lhanekam until ab2d-jenkins can be added to repo)

   - **Password:** {personal access token for ab2d-jenkins} (switched to lhanekam PAT until ab2d-jenkins can be added to repo)

   - **ID:** GITHUB_AB2D_JENKINS_PAT

1. Select **OK** on the *Add Credentials* page

1. Select **Jenkins** in the top left of the page

### Install the SSH plugin

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins** under the "System Configuration" section

1. Select the **Available** tab

1. Type the following in the **Filter** text box on the top right of the page

   ```
   ssh
   ```

1. Check **SSH**

1. Select **Download now and install after restart**

1. Note that if there are no jobs running, you can safely restart Jenkins

1. Check **Restart Jenkins when installation is complete and no jobs are running

1. Wait for Jenkins to restart

1. Log on to Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins** under the "System Configuration" section

1. Select the **Installed** tab

1. Verify that the **SSH plugin** now appears on this tab

1. Select **Jenkins** in the top left of the page to retrun to the home page

### Configure the SSH plugin

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Configure System** under the "System Configuration" section

1. Scroll down to the **SSH remote hosts** section

1. Select **Add** beside *SSH sites*

1. Configure localhost

   - **Hostname:** localhost

   - **Port:** 22

   - **Credentials:** jenkins

1. Select **Check connection**

1. Verify that "Successful connection" is displayed

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Configure Jenkins agent 01

   1. Copy the private IP address of Jenkins agent 01 to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
	| pbcopy
      ```
   
   1. Return to the Jenkins GUI
   
   1. Select **Add** below the "Successful connection" display
   
   1. Configure Jenkins agent
   
      - **Hostname:** {paste private ip address of jenkin agent 01}
   
      - **Port:** 22
   
      - **Credentials:** jenkins
   
   1. Select **Check connection**
   
   1. Verify that "Successful connection" is displayed

1. Configure Jenkins agent 02

   1. Copy the private IP address of Jenkins agent 02 to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
	| pbcopy
      ```
   
   1. Return to the Jenkins GUI
   
   1. Select **Add** below the "Successful connection" display
   
   1. Configure Jenkins agent
   
      - **Hostname:** {paste private ip address of jenkin agent 02}
   
      - **Port:** 22
   
      - **Credentials:** jenkins
   
   1. Select **Check connection**
   
   1. Verify that "Successful connection" is displayed

1. Configure Jenkins agent 03

   1. Copy the private IP address of Jenkins agent 03 to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
	| pbcopy
      ```
   
   1. Return to the Jenkins GUI
   
   1. Select **Add** below the "Successful connection" display
   
   1. Configure Jenkins agent
   
      - **Hostname:** {paste private ip address of jenkin agent 03}
   
      - **Port:** 22
   
      - **Credentials:** jenkins
   
   1. Select **Check connection**
   
   1. Verify that "Successful connection" is displayed

1. Configure Jenkins agent 04

   1. Copy the private IP address of Jenkins agent 04 to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
	| pbcopy
      ```
   
   1. Return to the Jenkins GUI
   
   1. Select **Add** below the "Successful connection" display
   
   1. Configure Jenkins agent
   
      - **Hostname:** {paste private ip address of jenkin agent 04}
   
      - **Port:** 22
   
      - **Credentials:** jenkins
   
   1. Select **Check connection**
   
   1. Verify that "Successful connection" is displayed

1. Configure Jenkins agent 05

   1. Copy the private IP address of Jenkins agent 05 to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
	| pbcopy
      ```
   
   1. Return to the Jenkins GUI
   
   1. Select **Add** below the "Successful connection" display
   
   1. Configure Jenkins agent
   
      - **Hostname:** {paste private ip address of jenkin agent 05}
   
      - **Port:** 22
   
      - **Credentials:** jenkins
   
   1. Select **Check connection**
   
   1. Verify that "Successful connection" is displayed

1. Select **Save**

### Install the "Scheduled Build" plugin

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Note that the "Scheduled Build" plugin acts like a Linux "at" command

   *See the following for information about the Linux "at" command:*

   > https://tecadmin.net/one-time-task-scheduling-using-at-commad-in-linux

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins** under the "System Configuration" section

1. Select the **Available** tab

1. Type the following in the **Filter** text box on the top right of the page

   ```
   schedule build
   ```

1. Check **Schedule Build**

1. Select **Download now and install after restart**

1. Note that if there are no jobs running, you can safely restart Jenkins

1. Check **Restart Jenkins when installation is complete and no jobs are running

1. Wait for Jenkins to restart

1. Log on to Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins**

1. Select the **Installed** tab

1. Verify that the **Schedule Build plugin** now appears on this tab

1. Select **Jenkins** in the top left of the page to retrun to the home page

### Configure GitHub plugin

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Configure System**

1. Scroll down to the **GitHub** section

1. Select **Add GitHub Server**

1. Select **GitHub Server**

1. Configure the GitHub Server

   - **Name:** github_com

   - **API URL:** https://api.github.com

   - **Credentials:** GITHUB_AB2D_JENKINS

1. Check **Manage hooks**

1. Select **Test connection**

1. Verify that a message similar to this is displayed

   ```
   Credentials verified for user lhanekam, rate limit: 4998
   ```

1. Select **Save**

### Install and configure the Authorize Project plugin

1. Note the following

   - Builds in Jenkins run as the virtual Jenkins SYSTEM user with full Jenkins permissions by default.

   - This can be a problem if some users have restricted or no access to some jobs, but can configure others.

   - The Authorize Project Plugin can be used to handle these situations.

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins**

1. Select **Manage Plugins**

1. Select the **Available** tab

1. Type the following in the **Filter** text box on the top right of the page

   ```
   authorize project
   ```

1. Check **Authorize Project**

1. Select **Download now and install after restart**

1. Check **Restart Jenkins when installation is complete and no jobs are running**

1. Wait for Jenkins to restart

1. Log on to Jenkins

1. Select **Manage Jenkins**

1. Select **Configure Global Security** under the "Security" section

1. Scroll down to the the "Access Control for Builds" section

1. Note that there are two "Access Control for Builds" sections that can be configured

   - Pre-project configurable Build Authorization

   - Project default Build Authorization

1. Note that at this point only "Project default Build Authorization" is being configured to behave with default "Run as SYSTEM" behavior

   > *** TO DO ***: Consider additional strategies for configuring "Access Control for Builds" as additional users are added.

1. Select **Add**

1. Select **Project default Build Authorization**

1. *** TO DO ***: Determine correct setting; will the new jenkins server work for all processes?

   - OLD JENKINS SERVER: Select "Run as SYSTEM" from the **Strategy** dropdown

   - NEW JENKINS SERVER: Select "Run as User who Triggered Build" from the **Strategy** dropdown

1. Select **Apply**

1. Select **Save**

### Register a new OAuth application with GitHub

1. Open Chome

1. Enter the following in the address bar

   > https://github.com

1. Log on to GitHub as lhanekam

1. Type the authentication code that was sent to Lonnie's phone in the **Authentication code** text box

1. Select **Verify**

1. Select the profile icon in the top right of the page

1. Select **Settings**

1. Select **Developer settings**

1. Select **OAuth Apps**

1. Select **Register a new application**

1. Configure "Register a new OAuth application" as follows

   - **Application name:** jenkins-github-authentication-v2

   - **Homepage URL:** https://ab2d.cms.gov

   - **Application description:** {blank}

   - **Authorization callback URL:** https://{private ip address of jenkins master}:8080/securityRealm/finishLogin

1. Select **Register application

1. Note the following

   - Client ID

   - Client Secret

1. Create the following in 1Password

   - **jenkins-github-authentication-v2 Client ID:** {client id}

   - **jenkins-github-authentication-v2 Client Secret:** {client secret}

1. Log out of GitHub

### Install and configure the GitHub Authentication plugin including adding GitHub users

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get the public IP address of Jenkins master

   ```ShellSession
   $ JENKINS_MASTER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins master

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_MASTER_PRIVATE_IP}"
   ```

1. Backup the exiting Jenkins configuration file before proceeding

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Open the "config.xml"

      ```ShellSession
      $ vim config.xml
      ```

   1. Note that the default settings for authorization strategy and  security realm

      ```
      <authorizationStrategy class="hudson.security.FullControlOnceLoggedInAuthorizationStrategy">
        <denyAnonymousReadAccess>true</denyAnonymousReadAccess>
      </authorizationStrategy>
      <securityRealm class="hudson.security.HudsonPrivateSecurityRealm">
        <disableSignup>true</disableSignup>
        <enableCaptcha>false</enableCaptcha>
      </securityRealm>
      ```

   1. Change to the '/var/lib/jenkins' directory

      ```ShellSession
      $ cd /var/lib/jenkins
      ```

   1. Backup the jenkins config file

      ```ShellSession
      $ cp config.xml config.xml.backup
      ```

1. Open Chome

1. Enter the following in the address bar

   > https://github.com

1. Log on as your user

1. Open a new Chrome tab

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins**

1. Select the **Available** tab

1. Type the following in the **search** text box

   ```
   github authentication
   ```

1. Check GitHub Authentication

1. Select **Download now and install after restart**

1. Note that if there are no jobs running, you can safely restart Jenkins

1. Check **Restart Jenkins when installation is complete and no jobs are running

1. Wait for Jenkins to restart

1. Log on to Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Configure Global Security** under the "Security" section

1. Select the **Github Authentication Plugin** radio button under "Security Realm" within the "Authentication" section

1. Configure "Global GitHub OAuth Settings" as follows

   - **GitHub Web URI:** https://github.com

   - **GitHub API URI:** https://api.github.com

   - **Client ID:** {jenkins-github-authentication-v2 client id from 1password}

   - **Client Secret:** {jenkins-github-authentication-v2 client secret from 1password}

   - **OAuth Scope(s):** read:org,user:email,repo

1. Select **Apply**

1. Scroll down to the **Authorization** section

1. Select the **Matrix-based security** radio button

1. Add desired GitHub users

   1. Select **Add user or group**

   1. Type the desired GitHub user into the **User or group name** text box

   1. Select **OK**

   1. Set desired permissions for the user using the checkboxes

   1. Select **Apply**

   1. Repeat this step for any additional users

1. Select **Save**

1. Log out of Jenkins

### Log on to Jenkins using GitHub OAuth authentication

1. Log on to Jenkins

1. Note that you will be prompted to log on to GitHub, if you are not already logged in

1. Log on to GitHub

1. Note the following appears on the "Authorize jenkins-github-authentication-v2" page

   - **jenkins-github-authentication-v2 by lhanekam:** wants to access your {your github user} account

   - **Organizations and teams:** Read-only access

   - **Repositories:** Public and private

   - **Personal user data:** Email addresses (read-only)

   - **Organization access:** CMSgov

1. Select **Authorize lhanekam**

1. Verify that the Jenkins page loads

### Install the "Parameterized Trigger" plugin

1. Log on to Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins**

1. Select the **Available** tab

1. Type the following in the **search** text box

   ```
   parameterized trigger
   ```

1. Check **Parameterized Trigger**

1. Select **Download now and install after restart**

1. Note that if there are no jobs running, you can safely restart Jenkins

1. Check **Restart Jenkins when installation is complete and no jobs are running

1. Wait for "Running" to be blinking beside "Restarting Jenkins"

1. Select **Jenkins** in the top left of the page

### Install or verify the Docker Commons plugin

1. Log on to Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins**

1. Select the **Installed** tab

1. Type the following in the **filter** text box

   ```
   docker commons
   ```

1. If the "Docker Commons Plugin" does not appear in the list, do the following:

   1. Select the **Available** tab

   1. Enter the following in the **search** text box

      ```
      docker commons
      ```

   1. Check **Docker Commons Plugin**

   1. Select **Download now and install after restart**

   1. Note that if there are no jobs running, you can safely restart Jenkins

   1. Check **Restart Jenkins when installation is complete and no jobs are running

   1. Wait for "Running" to be blinking beside "Restarting Jenkins"

   1. Select **Jenkins** in the top left of the page

### Install or verify the Docker Pipeline plugin

1. Log on to Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Plugins**

1. Select the **Installed** tab

1. Type the following in the **filter** text box

   ```
   docker pipeline
   ```

1. If the "Docker Pipeline" does not appear in the list, do the following:

   1. Select the **Available** tab

   1. Enter the following in the **search** text box

      ```
      docker pipeline
      ```

   1. Check **Docker Pipeline**

   1. Select **Download now and install after restart**

   1. Note that if there are no jobs running, you can safely restart Jenkins

   1. Check **Restart Jenkins when installation is complete and no jobs are running

   1. Wait for "Running" to be blinking beside "Restarting Jenkins"

   1. Select **Jenkins** in the top left of the page

### Configure global tool configuration

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Global Tool Configuration**

1. Select **JDK installations**

1. Select **Add JDK**

1. Configure the JDK as follows

   *Note that you will be configuring the exact version installed on the Jenkins agents.*

   - **Name:** adoptjdk13

   - **JAVA_HOME:** /usr/lib/jvm/jdk-13.0.2

   - **Install automatically:** {unchecked}

1. Ignore the "/usr/lib/jvm/jdk-13.0.2 is not a directory on the Jenkins master (but perhaps it exists on some agents)" message

1. Scroll down to the "Maven" section

1. Select **Maven installations**

1. Select **Add Maven**

1. Configure Maven as follows

   *Note that you will be configuring the exact version installed on the Jenkins agents.*

   - **Name:** maven-3.6.3

   - **JAVA_HOME:** /opt/maven

   - **Install automatically:** {unchecked}

1. Scroll down to the "Docker" section

1. Select **Docker installations**

1. Select **Add Docker**

1. Configure Docker as follows

   *Note that you will be configuring the exact version installed on the Jenkins agents.*

   - **Name:** docker

   - **JAVA_HOME:** /usr/bin/docker

   - **Install automatically:** {unchecked}

1. Select **Apply**

1. Select **Save**

### Configure Jenkins secrets used by builds

1. Open Jenkins

1. Select **Manage Jenkins** from the leftmost panel

1. Select **Manage Credentials** under the "Security" section

1. Scroll down to the "Stores scoped to Jenkins" section

1. Select **Jenkins** under the "Stores scoped to Jenkins" section

1. Select **Global credentials (unresticted)**

1. Add "CC_TEST_REPORTER_ID" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"CC_TEST_REPORTER_ID" from 1Password}

      - **ID:** CC_TEST_REPORTER_ID

      - **Description:** code climate test reporter id for running code climate checks

   1. Select **OK**

1. Add "HICN_HASH_PEPPER" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : BFD Prod Sbx : HICN Hash Pepper" from 1Password}

      - **ID:** HICN_HASH_PEPPER

      - **Description:**

   1. Select **OK**

1. Add "HPMS_AUTH_KEY_ID" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : HPMS IMPL/staging : API Key ID" from 1Password}

      - **ID:** HPMS_AUTH_KEY_ID

      - **Description:** hpms auth key id for dev

   1. Select **OK**

1. Add "HPMS_AUTH_KEY_SECRET" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : HPMS IMPL/staging : API Key Secret" from 1Password}

      - **ID:** HPMS_AUTH_KEY_SECRET

      - **Description:** hpms auth key secret for dev

   1. Select **OK**

1. Add "OKTA_CLIENT_ID" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : OKTA Test : PDP-100 : Client ID (PUBLIC)" from 1Password}

      - **ID:** OKTA_CLIENT_ID

      - **Description:** Test idp okta client id

   1. Select **OK**

1. Add "OKTA_CLIENT_PASSWORD" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : OKTA Test : PDP-100 : Client Secret (PUBLIC)" from 1Password}

      - **ID:** OKTA_CLIENT_PASSWORD

      - **Description:** Test idp okta client password

   1. Select **OK**

1. Add "SANDBOX_BFD_KEYSTORE" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret file

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : BFD Prod Sbx : Keystore" from 1Password}

      - **ID:** SANDBOX_BFD_KEYSTORE

      - **Description:** bfd prod sandbox keystore file

   1. Select **OK**

1. Add "SANDBOX_BFD_KEYSTORE_PASSWORD" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : BFD Prod Sbx : Keystore Password" from 1Password}

      - **ID:** SANDBOX_BFD_KEYSTORE_PASSWORD

      - **Description:** bfd prod sandbox keystore password

   1. Select **OK**

1. Add "SECONDARY_USER_OKTA_CLIENT_ID" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : OKTA Test : PDP-1000 : Client ID (PUBLIC)" from 1Password}

      - **ID:** SECONDARY_USER_OKTA_CLIENT_ID

      - **Description:** Test idp second okta client id

   1. Select **OK**

1. Add "SECONDARY_USER_OKTA_CLIENT_PASSWORD" secret

   1. Select **Add Credentials**

   1. Configure the credentials page as follows

      - **Kind:** Secret text

      - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

      - **Secret:** {"AB2D Dev : OKTA Test : PDP-1000 : Client Secret (PUBLIC)" from 1Password}

      - **ID:** SECONDARY_USER_OKTA_CLIENT_PASSWORD

      - **Description:** Test idp second okta client password

   1. Select **OK**

### Add Jenkins agent 01

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get and note the private IP address of Jenkins agent 01
   
   ```ShellSession
   $ aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text
   ```

1. Get and note the number of CPUs for Jenkins agent
      
   1. Get the instance type of the Jenkins agent
   
      ```ShellSession
      $ JENKINS_AGENT_INSTANCE_TYPE=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-01" \
        --query="Reservations[*].Instances[?State.Name == 'running'].InstanceType" \
        --output text)
      ```

   1. Get and note the number of CPUs for Jenkins agent

      ```ShellSession
      $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
        --instance-types "${JENKINS_AGENT_INSTANCE_TYPE}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
        --output text
      ```
      
1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Manage Nodes and Clouds** under the "System Configuration" section

1. Select **New Node** from the leftmost panel

1. Type the following in the **Node name** text box

   ```
   agent01-deployment
   ```

1. Check **Permanent Agent**

1. Select **OK** on the "New Node" page

1. Configure the Jenkins agent 01 node

   *Format:*
   
   - **Name:** agent01-deployment

   - **Description:** Agent 01 for Deployments

   - **# of executors:** {cpu count for jenkins agent}

   - **Remote root directory:** /var/lib/jenkins/jenkins_agent

   - **Labels:** deployment

   - **Usage:** Only build jobs with label expressions matching this node

   - **Launch method:** Launch agent agents via SSH

   - **Host:** {private ip address of the jenkins agent}

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

1. Note that the "Labels" property is typically used to group nodes that are used for different functions

   - agent01-deployment has a "deployment" label and will be used for deployments

1. Select **Save**

1. Note that the new agent node will have an X on it which means that it is still being setup

1. Select the new agent node

1. Select **Log** from the leftmost panel

1. Verify that the following appears at the end of the log file

   ```
   Agent successfully connected and online
   ```

1. Select **Jenkins** at the top left of the page

### Add Jenkins agent 02

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get and note the private IP address of Jenkins agent 02
   
   ```ShellSession
   $ aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text
   ```

1. Get and note the number of CPUs for Jenkins agent
      
   1. Get the instance type of the Jenkins agent
   
      ```ShellSession
      $ JENKINS_AGENT_INSTANCE_TYPE=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-02" \
        --query="Reservations[*].Instances[?State.Name == 'running'].InstanceType" \
        --output text)
      ```

   1. Get and note the number of CPUs for Jenkins agent

      ```ShellSession
      $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
        --instance-types "${JENKINS_AGENT_INSTANCE_TYPE}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
        --output text
      ```
      
1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Manage Nodes and Clouds** under the "System Configuration" section

1. Select **New Node** from the leftmost panel

1. Type the following in the **Node name** text box

   ```
   agent02-build
   ```

1. Check **Permanent Agent**

1. Select **OK** on the "New Node" page

1. Configure the Jenkins agent 02 node

   *Format:*
   
   - **Name:** agent02-build

   - **Description:** Agent 02 for Builds

   - **# of executors:** 1

   - **Remote root directory:** /var/lib/jenkins/jenkins_agent

   - **Labels:** build

   - **Usage:** Only build jobs with label expressions matching this node

   - **Launch method:** Launch agent agents via SSH

   - **Host:** {private ip address of the jenkins agent}

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

1. Note that the "Labels" property is typically used to group nodes that are used for different functions

   - agent02-build has a "build" label and will be used for builds

1. Select **Save**

1. Note that the new agent node will have an X on it which means that it is still being setup

1. Select the new agent node

1. Select **Log** from the leftmost panel

1. Verify that the following appears at the end of the log file

   ```
   Agent successfully connected and online
   ```

1. Select **Jenkins** at the top left of the page

### Add Jenkins agent 03

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get and note the private IP address of Jenkins agent 03
   
   ```ShellSession
   $ aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text
   ```

1. Get and note the number of CPUs for Jenkins agent
      
   1. Get the instance type of the Jenkins agent
   
      ```ShellSession
      $ JENKINS_AGENT_INSTANCE_TYPE=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-03" \
        --query="Reservations[*].Instances[?State.Name == 'running'].InstanceType" \
        --output text)
      ```

   1. Get and note the number of CPUs for Jenkins agent

      ```ShellSession
      $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
        --instance-types "${JENKINS_AGENT_INSTANCE_TYPE}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
        --output text
      ```
      
1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Manage Nodes and Clouds** under the "System Configuration" section

1. Select **New Node** from the leftmost panel

1. Type the following in the **Node name** text box

   ```
   agent03-build
   ```

1. Check **Permanent Agent**

1. Select **OK** on the "New Node" page

1. Configure the Jenkins agent 03 node

   *Format:*
   
   - **Name:** agent03-build

   - **Description:** Agent 03 for Builds

   - **# of executors:** 1

   - **Remote root directory:** /var/lib/jenkins/jenkins_agent

   - **Labels:** build

   - **Usage:** Only build jobs with label expressions matching this node

   - **Launch method:** Launch agent agents via SSH

   - **Host:** {private ip address of the jenkins agent}

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

1. Note that the "Labels" property is typically used to group nodes that are used for different functions

   - agent03-build has a "build" label and will be used for builds

1. Select **Save**

1. Note that the new agent node will have an X on it which means that it is still being setup

1. Select the new agent node

1. Select **Log** from the leftmost panel

1. Verify that the following appears at the end of the log file

   ```
   Agent successfully connected and online
   ```

1. Select **Jenkins** at the top left of the page

### Add Jenkins agent 04

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get and note the private IP address of Jenkins agent 04
   
   ```ShellSession
   $ aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text
   ```

1. Get and note the number of CPUs for Jenkins agent
      
   1. Get the instance type of the Jenkins agent
   
      ```ShellSession
      $ JENKINS_AGENT_INSTANCE_TYPE=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-04" \
        --query="Reservations[*].Instances[?State.Name == 'running'].InstanceType" \
        --output text)
      ```

   1. Get and note the number of CPUs for Jenkins agent

      ```ShellSession
      $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
        --instance-types "${JENKINS_AGENT_INSTANCE_TYPE}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
        --output text
      ```
      
1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Manage Nodes and Clouds** under the "System Configuration" section

1. Select **New Node** from the leftmost panel

1. Type the following in the **Node name** text box

   ```
   agent04-build
   ```

1. Check **Permanent Agent**

1. Select **OK** on the "New Node" page

1. Configure the Jenkins agent 04 node

   *Format:*
   
   - **Name:** agent04-build

   - **Description:** Agent 04 for Builds

   - **# of executors:** 1

   - **Remote root directory:** /var/lib/jenkins/jenkins_agent

   - **Labels:** build

   - **Usage:** Only build jobs with label expressions matching this node

   - **Launch method:** Launch agent agents via SSH

   - **Host:** {private ip address of the jenkins agent}

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

1. Note that the "Labels" property is typically used to group nodes that are used for different functions

   - agent04-build has a "build" label and will be used for builds

1. Select **Save**

1. Note that the new agent node will have an X on it which means that it is still being setup

1. Select the new agent node

1. Select **Log** from the leftmost panel

1. Verify that the following appears at the end of the log file

   ```
   Agent successfully connected and online
   ```

1. Select **Jenkins** at the top left of the page

### Add Jenkins agent 05

1. Ensure that you are connected to the Cisco VPN

1. Open a terminal

1. Change to the "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get and note the private IP address of Jenkins agent 05
   
   ```ShellSession
   $ aws --region us-east-1 ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
     --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
     --output text
   ```

1. Get and note the number of CPUs for Jenkins agent
      
   1. Get the instance type of the Jenkins agent
   
      ```ShellSession
      $ JENKINS_AGENT_INSTANCE_TYPE=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent-05" \
        --query="Reservations[*].Instances[?State.Name == 'running'].InstanceType" \
        --output text)
      ```

   1. Get and note the number of CPUs for Jenkins agent

      ```ShellSession
      $ aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instance-types \
        --instance-types "${JENKINS_AGENT_INSTANCE_TYPE}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
        --output text
      ```
      
1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Manage Nodes and Clouds** under the "System Configuration" section

1. Select **New Node** from the leftmost panel

1. Type the following in the **Node name** text box

   ```
   agent05-build
   ```

1. Check **Permanent Agent**

1. Select **OK** on the "New Node" page

1. Configure the Jenkins agent 05 node

   *Format:*
   
   - **Name:** agent05-build

   - **Description:** Agent 05 for Builds

   - **# of executors:** 1

   - **Remote root directory:** /var/lib/jenkins/jenkins_agent

   - **Labels:** build

   - **Usage:** Only build jobs with label expressions matching this node

   - **Launch method:** Launch agent agents via SSH

   - **Host:** {private ip address of the jenkins agent}

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

1. Note that the "Labels" property is typically used to group nodes that are used for different functions

   - agent05-build has a "build" label and will be used for builds

1. Select **Save**

1. Note that the new agent node will have an X on it which means that it is still being setup

1. Select the new agent node

1. Select **Log** from the leftmost panel

1. Verify that the following appears at the end of the log file

   ```
   Agent successfully connected and online
   ```

1. Select **Jenkins** at the top left of the page

### Create a "development" folder in Jenkins

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **New Item** from the leftmost panel

1. Type the following in the **Enter an item name** text box

   ```
   development
   ```

1. Select **Folder**

1. Select **OK** on the "Enter an item name" page

1. Select **Save**

1. Select the **Jenkins** bread crumb in the top left of the page

### Configure a Jenkins project for development application deploy

1. Note that a Jenkins project is the same as the deprecated Jenkins job (even though job is still used in the GUI)

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select the **development** link

1. Select **New Item** from the leftmost panel

1. Type the following in the **Enter an item name** text box

   ```
   deploy-to-development
   ```

1. Select **Freestyle project**

1. Select **OK** on the "Enter an item name" page

1. Configure log rotation strategy for jenkins builds

   1. Check **Discard old builds**

   1. Configure "Discard old builds" as follows

      - **Strategy:** Log Rotation

      - **Days to keep builds:** 14

      - **Max # of builds to keep:** 14

1. Configure GitHub project

   1. Check **GitHub project**

   1. Type the following in the **Project url** text box

      ```
      https://github.com/CMSgov/ab2d
      ```

   1. Select **Apply**

1. Check **This project is parameterized**

1. Add the "CMS_ENV_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** CMS_ENV_PARAM

      - **Default Value:** ab2d-dev

      - **Description:**

        ```
	Corresponds to the deployment environment associated with an AWS account.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "CMS_ECR_REPO_ENV_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** CMS_ECR_REPO_ENV_PARAM

      - **Default Value:** ab2d-mgmt-east-dev

      - **Description:**

        ```
	Corresponds to the management environment associated with an AWS account.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "REGION_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** REGION_PARAM

      - **Default Value:** us-east-1

      - **Description:**

        ```
	Corresponds to AWS region of the target VPC.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "VPC_ID_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** VPC_ID_PARAM

      - **Default Value:** vpc-0c6413ec40c5fdac3

      - **Description:**

        ```
	Corresponds to the VPC ID of the target VPC.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "SSH_USERNAME_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** SSH_USERNAME_PARAM

      - **Default Value:** ec2-user

      - **Description:**

        ```
	Corresponds to the main linux user for EC2 instances.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_INSTANCE_TYPE_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_INSTANCE_TYPE_API_PARAM

      - **Default Value:** m5.xlarge

      - **Description:**

        ```
	Corresponds to the EC2 instance type of API nodes.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_INSTANCE_TYPE_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_INSTANCE_TYPE_WORKER_PARAM

      - **Default Value:** m5.xlarge

      - **Description:**

        ```
	Corresponds to the EC2 instance type of worker nodes.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_DESIRED_INSTANCE_COUNT_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_DESIRED_INSTANCE_COUNT_API_PARAM

      - **Default Value:** 1

      - **Description:**

        ```
	Corresponds to the desired API node count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MINIMUM_INSTANCE_COUNT_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MINIMUM_INSTANCE_COUNT_API_PARAM

      - **Default Value:** 1

      - **Description:**

        ```
	Corresponds to the minumum API node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM

      - **Default Value:** 1

      - **Description:**

        ```
	Corresponds to the maximum API node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM

      - **Default Value:** 1

      - **Description:**

        ```
	Corresponds to the desired worker node count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM

      - **Default Value:** 1

      - **Description:**

        ```
	Corresponds to the minumum worker node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM

      - **Default Value:** 1

      - **Description:**

        ```
	Corresponds to the maximum worker node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "DATABASE_SECRET_DATETIME_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** DATABASE_SECRET_DATETIME_PARAM

      - **Default Value:** 2020-01-02-09-15-01

      - **Description:**

        ```
	Corresponds to a datatime string that is needed to get secrets in secrets manager.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "DEBUG_LEVEL_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** DEBUG_LEVEL_PARAM

      - **Default Value:** WARN

      - **Description:**

        ```
	Corresponds to terraform logging level.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "INTERNET_FACING_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** INTERNET_FACING_PARAM

      - **Default Value:** false

      - **Description:**

        ```
	Corresponds to whether the application load balancer is internet facing or only available to VPN.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Restrict where the project can be run

   1. Check **Restrict where this project can be run**

   1. Type the following in the **Label Expression** text box

      ```
      agent01
      ```

   1. Select **Apply**

1. Scroll down to the "Source Code Management" section

1. Add source control management

   1. Select the **Git** radio button under "Source Code Management"

   1. Note that credentials are not needed since this is a public repository

   1. Configure the "Repositories" section as follows

      *Example for "master" branch"*

      - **Repository URL:** https://github.com/CMSgov/ab2d

      - **Credentials:** - none -

      - **Branch Specifier:** */master

   1. Select **Apply**

1. Scroll down to the "Build Triggers" section

1. Configure build triggers

   1. Check **Poll SCM**

   1. Type the following in the **Schedule** text box

      ```
      H/5 * * * *
      ```

   1. Select **Apply**

1. Scroll down to the "Build Environment" section

1. Check **Add timestamps to the Console Output**

1. Scroll down to the "Build" section

1. Configure the build

   1. Select **Add build step**

   1. Select **Execute shell**

   1. Type the following in **Command** text box

      ```
      cd ./Deploy
      chmod +x ./bash/deploy-application.sh
      ./bash/deploy-application.sh
      ```

   1. Select **Apply**

1. Select **Save**

1. Test the deployment

   1. Select **Build with Parameters**

   1. Note that the parameters and their default values are displayed

   1. Scroll down to the bottom of the parameters

   1. Select **Build**

1. View the deployment output

   1. Select the build number under "Build History" for the current build

      *Example:*

      ```
      #1
      ```

   1. Select **Console Output**

   1. Observe the output

1. If the last line of the output is "Finished: FAILURE", do the following:

   1. Review the deployment output

   1. Resolve the issue

   1. Try running the the build again

1. Verify that the last line of output is as follows:

   ```
   Finished: SUCCESS
   ```

1. Collect timing metrics based on the output and observation of the "Destroy old deployment" process

   *New deployment active:* 16:41

   *Total time including cleanup:* 24:01

   Process                                   |Start Time|End Time|Process Time
   ------------------------------------------|----------|--------|------------
   Prepare for deployment                    |16:07:07  |16:07:31|00:24
   Build API and worker                      |16:07:31  |16:08:31|01:00
   Push API and worker images to ECR         |16:08:31  |16:09:39|01:08
   Complete API module automation            |16:09:39  |16:10:40|01:01
   Complete worker module automation         |16:10:40  |16:11:31|00:51
   Complete CloudWatch, WAF, and Shield      |16:11:31  |16:11:38|00:07
   Wait for API and Worker ECS tasks to start|16:11:38  |16:23:48|12:10
   New deployment active                     |16:23:48  |16:23:48|00:00
   Destroy old deployment                    |16:23:48  |16:31:08|07:20

### Create a "sandbox" folder in Jenkins

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **New Item** from the leftmost panel

1. Type the following in the **Enter an item name** text box

   ```
   sandbox
   ```

1. Select **Folder**

1. Select **OK** on the "Enter an item name" page

1. Select **Save**

1. Select the **Jenkins** bread crumb in the top left of the page

### Configure a Jenkins project for sandbox application deploy

1. Note that a Jenkins project is the same as the deprecated Jenkins job (even though job is still used in the GUI)

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select the **sandbox** link

1. Select **New Item** from the leftmost panel

1. Type the following in the **Enter an item name** text box

   ```
   deploy-to-sandbox
   ```

1. Select **Freestyle project**

1. Select **OK** on the "Enter an item name" page

1. Configure log rotation strategy for jenkins builds

   1. Check **Discard old builds**

   1. Configure "Discard old builds" as follows

      - **Strategy:** Log Rotation

      - **Days to keep builds:** 14

      - **Max # of builds to keep:** 14

1. Configure GitHub project

   1. Check **GitHub project**

   1. Type the following in the **Project url** text box

      ```
      https://github.com/CMSgov/ab2d
      ```

   1. Select **Apply**

1. Check **This project is parameterized**

1. Add the "CMS_ENV_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** CMS_ENV_PARAM

      - **Default Value:** ab2d-sbx-sandbox

      - **Description:**

        ```
	Corresponds to the sandbox environment associated with an AWS account.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "CMS_ECR_REPO_ENV_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** CMS_ECR_REPO_ENV_PARAM

      - **Default Value:** ab2d-mgmt-east-dev

      - **Description:**

        ```
	Corresponds to the management environment associated with an AWS account.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "REGION_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** REGION_PARAM

      - **Default Value:** us-east-1

      - **Description:**

        ```
	Corresponds to AWS region of the target VPC.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "VPC_ID_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** VPC_ID_PARAM

      - **Default Value:** vpc-08dbf3fa96684151c

      - **Description:**

        ```
	Corresponds to the VPC ID of the target VPC.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "SSH_USERNAME_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** SSH_USERNAME_PARAM

      - **Default Value:** ec2-user

      - **Description:**

        ```
	Corresponds to the main linux user for EC2 instances.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_INSTANCE_TYPE_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_INSTANCE_TYPE_API_PARAM

      - **Default Value:** m5.xlarge

      - **Description:**

        ```
	Corresponds to the EC2 instance type of API nodes.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_INSTANCE_TYPE_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_INSTANCE_TYPE_WORKER_PARAM

      - **Default Value:** m5.4xlarge

      - **Description:**

        ```
	Corresponds to the EC2 instance type of worker nodes.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_DESIRED_INSTANCE_COUNT_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_DESIRED_INSTANCE_COUNT_API_PARAM

      - **Default Value:** 2

      - **Description:**

        ```
	Corresponds to the desired API node count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MINIMUM_INSTANCE_COUNT_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MINIMUM_INSTANCE_COUNT_API_PARAM

      - **Default Value:** 2

      - **Description:**

        ```
	Corresponds to the minumum API node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MAXIMUM_INSTANCE_COUNT_API_PARAM

      - **Default Value:** 2

      - **Description:**

        ```
	Corresponds to the maximum API node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_DESIRED_INSTANCE_COUNT_WORKER_PARAM

      - **Default Value:** 2

      - **Description:**

        ```
	Corresponds to the desired worker node count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MINIMUM_INSTANCE_COUNT_WORKER_PARAM

      - **Default Value:** 2

      - **Description:**

        ```
	Corresponds to the minumum worker node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** EC2_MAXIMUM_INSTANCE_COUNT_WORKER_PARAM

      - **Default Value:** 2

      - **Description:**

        ```
	Corresponds to the maximum worker node(s) count.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "DATABASE_SECRET_DATETIME_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** DATABASE_SECRET_DATETIME_PARAM

      - **Default Value:** 2020-01-02-09-15-01

      - **Description:**

        ```
	Corresponds to a datatime string that is needed to get secrets in secrets manager.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "DEBUG_LEVEL_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** DEBUG_LEVEL_PARAM

      - **Default Value:** WARN

      - **Description:**

        ```
	Corresponds to terraform logging level.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Add the "INTERNET_FACING_PARAM" parameter

   1. Select **Add Parameter**

   1. Select **String Parameter**

   1. Configure the "String Parameter" as follows

      - **Name:** INTERNET_FACING_PARAM

      - **Default Value:** true

      - **Description:**

        ```
	Corresponds to whether the application load balancer is internet facing or only available to VPN.
	```

   1. Check **Trim the string**

   1. Select **Apply**

1. Restrict where the project can be run

   1. Check **Restrict where this project can be run**

   1. Type the following in the **Label Expression** text box

      ```
      agent01
      ```

   1. Select **Apply**

1. Scroll down to the "Source Code Management" section

1. Add source control management

   1. Select the **Git** radio button under "Source Code Management"

   1. Note that credentials are not needed since this is a public repository

   1. Configure the "Repositories" section as follows

      *Example for "master" branch"*

      - **Repository URL:** https://github.com/CMSgov/ab2d

      - **Credentials:** - none -

      - **Branch Specifier:** */master

   1. Select **Apply**

1. Scroll down to the "Build Environment" section

1. Check **Add timestamps to the Console Output**

1. Scroll down to the "Build" section

1. Configure the build

   1. Select **Add build step**

   1. Select **Execute shell**

   1. Type the following in **Command** text box

      ```
      cd ./Deploy
      chmod +x ./bash/deploy-application.sh
      ./bash/deploy-application.sh
      ```

   1. Select **Apply**

1. Select **Save**

1. Test the deployment

   1. Select **Build with Parameters**

   1. Note that the parameters and their default values are displayed

   1. Scroll down to the bottom of the parameters

   1. Select **Build**

1. View the deployment output

   1. Select the build number under "Build History" for the current build

      *Example:*

      ```
      #1
      ```

   1. Select **Console Output**

   1. Observe the output

1. If the last line of the output is "Finished: FAILURE", do the following:

   1. Review the deployment output

   1. Resolve the issue

   1. Try running the the build again

1. Verify that the last line of output is as follows:

   ```
   Finished: SUCCESS
   ```

1. Collect timing metrics based on the output and observation of the "Destroy old deployment" process

   > *** TO DO ***: Update metrics
   
   Process                                   |Start Time|End Time|Process Time
   ------------------------------------------|----------|--------|------------
   Prepare for deployment                    |16:30:42  |16:31:03|00:21
   Build API and worker                      |16:31:03  |16:32:03|01:00
   Push API and worker images to ECR         |16:32:03  |16:33:01|00:58
   Complete API module automation            |16:33:01  |16:34:05|01:04
   Complete worker module automation         |16:34:05  |16:34:56|00:51
   Wait for API and Worker ECS tasks to start|16:34:56  |16:48:02|13:06
   New deployment active                     |16:48:02  |16:48:02|00:00
   Destroy old deployment                    |16:48:02  |16:49:30|07:44

## Dismiss Jenkins SYSTEM user warning

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** in the leftmost panel

1. Note that if and when the following message is dispalyed, select **Dismiss** since all users will have full access for running jobs (this is needed for incident handling)

   ```
   Builds in Jenkins run as the virtual Jenkins SYSTEM user with full Jenkins permissions by default.
   This can be a problem if some users have restricted or no access to some jobs, but can configure others.
   If that is the case, it is recommended to install a plugin implementing build authentication, and to override this default.
   - An implementation of access control for builds is present.
   - Access control for builds is configured.
   - Builds have been observed as launching as the internal SYSTEM user despite access control for builds being set up. It is recommended that you review the access control for builds configuration to ensure builds are not generally running as the SYSTEM user.
   ```

## Configure executors for Jenkins master

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** in the leftmost panel

1. Select **Configure System**

1. Note the following

   - the "number of executors" defaulted to 2

   - the Jenkins master has 4 cores for its instance type

   - typically you will want the number of executors to match the number of cores

1. Configure the "master" node as follows

   - **# of executors:** 4

   - **Labels:** master

   - **Usage:** Only build jobs with label expressions matching this node

1. Select **Save**

## Configure Jenkins CLI

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** in the leftmost panel

1. Scroll down to the "Tools and Actions" section

1. Select **Jenkins CLI**

1. Note the list of commands available for the Jenkins CLI

1. Select **jenkins-cli.jar** to download the file

1. Wait for the download to complete

1. If propted by browser, select **Keep**

1. Create a directory for the Jenkins CLI

   ```ShellSession
   $ sudo mkdir -p /opt/jenkins-cli
   ```

1. Move the extracted directory to the "/opt" directory

   ```ShellSession
   $ sudo mv ~/Downloads/jenkins-cli.jar /opt/jenkins-cli
   ```

1. Set permssions on jenkins-cli

   ```ShellSession
   $ sudo chown -R $(id -u):$(id -g -nr) /opt/jenkins-cli
   $ sudo chmod 755 /opt/jenkins-cli
   ```

1. Verify that you can list the top level Jenkins projects

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master public ip}:8080" \
     -auth {your github user}:{your personal access token} \
     list-jobs
   ```

1. Verify that you can list Jenkins projects within the development folder

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master public ip}:8080" \
     -auth {your github user}:{your personal access token} \
     list-jobs development
   ```

## Setup Webhook payload delivery service as an intermediary between firewalled Jenkins and GitHub

1. Note the following about the webhook payload delivery service that will be used

   - smee.io will act as the webhook payload delivery service between firewalled Jenkins and GitHub

   - GitHub pushes an event to smee.io

   - Jenkins subscribes to smee.io via an outgoing connection

1. Prepare and save the following 1Password entry for smee.io webhook proxy URL

   - **1Password Entry Type:** Secure Note

   - **1Password Entry:** AB2D Mgmt - Smee.io - Webhooks Proxy URL

   - **Note:** {nothing yet}

1. Open Chrome

1. Enter the following in the address bar

   > https://smee.io

1. Select **Start a new channel**

1. Copy the **Webhook Proxy URL** value to the clipboard

1. Edit and save the following 1Password entry for smee.io webhook proxy URL

   - **1Password Entry Type:** Secure Note

   - **1Password Entry:** AB2D Mgmt - Smee.io - Webhooks Proxy URL

   - **Note:** {smee.io webhooks proxy url}

1. Open a terminal

1. Change to your "ab2d" repo directory

   *Example:*

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Set AWS environment to the management account

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Get private ip address of Jenkins master

   ```ShellSession
   $ JENKINS_MASTER_PRIVATE_IP=$(aws --region "${AWS_DEFAULT_REGION}" ec2 describe-instances \
     --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
     --query "Reservations[*].Instances[*].PrivateIpAddress" \
     --output text)
   ```

1. Connect to Jenkins master

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_MASTER_PRIVATE_IP}"
   ```

1. Install Node.js and npm on Jenkins master

   ```ShellSession
   $ sudo yum install nodejs -y
   ```

1. Return to the terminal

1. Switch to root

   ```ShellSesssion
   $ sudo su
   ```

1. Open Chrome

1. Enter the following in the address bar

   > https://github.com/nodesource/distributions

1. Select "Installation instructions" under "Enterprise Linux based distributions" in the "Table of Contents"

1. Copy the line under "# As root" for the latest version of Node.js to the clipboard

1. Return to the terminal

1. Paste and the line at the terminal prompt

   *Example for Node.js v15.x:*

   ```ShellSession
   $ curl -sL https://rpm.nodesource.com/setup_15.x | bash -
   ```

1. Exit the root shell

   ```ShellSession
   $ exit
   ```

1. Uninstall the old versions of Node.js and npm

   ```ShellSession
   $ sudo yum remove -y nodejs npm
   ```

1. Install the latest versions of Node.js and npm

   ```ShellSession
   $ sudo yum install -y nodejs
   ```

1. Install the smee.io client

   ```ShellSession
   $ sudo npm install --global smee-client
   ```

1. Install lsb

   ```ShellSession
   $ sudo yum install redhat-lsb -y
   ```

1. Get the following value from 1Password

   - **1Password Entry Type:** Secure Note

   - **1Password Entry:** AB2D Mgmt - Smee.io - Webhooks Proxy URL

1. Set an environment variable for the

   *Format:*

   ```ShellSession
   $ SMEE_IO_WEBHOOKS_PROXY_URL={ab2d mgmt - smee.io - webhooks proxy url}
   ```

1. Start the smee client and point it to Jenkins master port 8080

   ```ShellSession
   $ smee --url "${SMEE_IO_WEBHOOKS_PROXY_URL}" --path /github-webhook/ --port 8080
   ```

1. Verify that the following is displayed

   *Format:*

   ```
   Forwarding {ab2d mgmt - smee.io - webhooks proxy url} to http://127.0.0.1:8080/github-webhook/
   Connected {ab2d mgmt - smee.io - webhooks proxy url}
   ```

1. Note that it does not return to the dollar sign prompt while running this way

1. Stop the service from running by pressing **control+c** on the keyboard

   *Note that we are stopping smee because we will need to run it as a service instead.*

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

1. Copy "smee" file to Jenkins master

   ```ShellSession
   $ scp -i ~/.ssh/ab2d-mgmt-east-dev.pem ./Deploy/smee.io/smee "ec2-user@${JENKINS_MASTER_PRIVATE_IP}":~
   ```

1. Copy "smee.service" file to Jenkins master

   ```ShellSession
   $ scp -i ~/.ssh/ab2d-mgmt-east-dev.pem ./Deploy/smee.io/smee.service "ec2-user@${JENKINS_MASTER_PRIVATE_IP}":~
   ```

1. Connect to Jenkins master

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem "ec2-user@${JENKINS_MASTER_PRIVATE_IP}"
   ```

1. Open the "smee.service" file

   ```ShellSession
   $ vim smee.service
   ```

1. Edit the following line as follows

   *Format:*

   ```
   ExecStart=/usr/bin/smee -u {smee.io webhooks proxy url} --path /github-webhook/ --port 8080
   ```

1. Save and close the file

1. Move the "smee" file to the "/etc/init.d" directory

   ```ShellSession
   $ sudo mv smee /etc/init.d/
   ```

1. Change ownership of the smee file

   ```ShellSession
   $ sudo chown root:root /etc/init.d/smee
   ```

1. Change the permissions on the smee file

   ```ShellSession
   $ sudo chmod 755 /etc/init.d/smee
   ```

1. Move the "smee.service" file to the "/etc/systemd/system" directory

   ```ShellSession
   $ sudo mv smee.service /etc/systemd/system/
   ```

1. Change ownership of the smee file

   ```ShellSession
   $ sudo chown root:root /etc/systemd/system/smee.service
   ```

1. Change the permissions on the smee file

   ```ShellSession
   $ sudo chmod 755 /etc/systemd/system/smee.service
   ```

1. Start the "smee" service

   ```ShellSession
   $ sudo /etc/init.d/smee start
   ```

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

1. Open Chrome

1. Enter the following in the address bar

   > https://github.com/CMSgov/ab2d

1. Select the **Settings** tab

1. Select **Webhooks** from the leftmost panel

1. Select **Add webhook**

1. Log on to GitHub

1. Configure the "Webhooks / Add webhook" page as follows

   - **Payload URL:** {ab2d mgmt - smee.io - webhooks proxy url}

   - **Content type:** application/json

   - **Secret:** {blank}

   - **SSL verification:** Enable SSL verification

   - **Which events would you like to trigger this webhook?:** Let me select individual events.

     - **Pull requests:** checked

     - **Pushes:** checked

   - **Active:** {checked}

1. Select **Add webhook**

1. Notice that GitHub displays the following message on the top of the page

   ```
   Okay, that hook was successfully created. We sent a ping payload to test it out! Read more about it at https://docs.github.com/webhooks/#ping-event.
   ```

1. Open a Chrome tab

1. Enter the following in the address bar

   ```
   {ab2d mgmt - smee.io - webhooks proxy url}
   ```

1. Verify that a "ping" message appears in smee under the "All" section

   *Example:*

   ```
   ping          less than a minute ago
   ```

1. Open a Chrome tab

1. Open Jenkins

1. Select **Manage Jenkins**

1. Select **Configure System** under the "System Configuration" section

1. Scroll down to the "GitHub" section

1. Select the second **Advanced**

1. Check **Specify another hook URL for GitHub configuration**

1. Change **Specify another hook URL for GitHub configuration** text box to the following

   ```
   http://localhost:8080/github-webhook/
   ```

1. Select **Apply**

1. Select **Save**

## Configure Multibranch Pipeline

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master public ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** in the leftmost panel

1. Select **New Item** from the leftmost panel

1. Enter the following in the **Enter an item name** text box

   ```
   AB2D Repo
   ```

1. Select **Multibranch Pipeline**

1. Select **OK** on the "Enter an item name" page

1. Conigure the "General" tab as follows

   - **Display Name:** AB2D Repo

   - **Description:** AB2D Main repository

   - **Disable:** unchecked

1. Select the **Branch Sources** tab

1. Select the following from the **Add source** dropdown

   ```
   GitHub
   ```

1. Configure the "Branch Sources" section as follows

   - **Credentials:** ab2d-jenkins/****** (temporarily lhanekam/****** until we get approval to move ab2d-jenkins into the github org)

   - **Repository HTTPS URL:** {radio button selected}

   - **Repository HTTPS URL:** https://github.com/CMSgov/ab2d

   - **Behaviors - Discover branches - Strategy:** Exclude branches that are also filed as PRs

   - **Behaviors - Discover pull requests from origin - Strategy:** The current pull request revision

   - **Behaviors - Discover pull requests from forks - Strategy:** The current pull request revision

   - **Behaviors - Discover pull requests from forks - Trust:** From users with Admin or Write permission

   - **Property strategy:** All branches get the same properties

1. Select **Apply**

1. Select the **Build Configuration** tab

1. Configure the "Build Configuration" section as follows

   - **Mode:** by Jenkinsfile

   - **Script Path:** Jenkinsfile

1. Select **Apply**

1. Select the **Scan Repository Triggers** tab

1. Check **Periodically if not otherwise run**

1. Select the following from the **Interval** dropdown

   ```
   2 minutes
   ```

1. Select **Apply**

1. Select the **Orphaned Item Strategy** tab

1. Configure the "Orphaned Item Strategy" section as follows

   - **Discard old items:** checked

   - **Days to keep old items:** 3

   - **Max # of old items to keep:** 30

1. Select **Apply**

1. Select **Save**

1. Open a Chrome tab

1. Enter the following in the address bar

   > https://github.com/CMSgov/ab2d

1. Select the **Settings** tab

1. Select **Branches** in the leftmost panel

1. Select **Edit** beside "master" under the "Branch protection rules" section

1. Check **Require status checks to pass before merging**

1. Check **Require branches to be up to date before merging**

1. Check **continuous-integration/jenkins/branch** within the "Status checks found in the last week for this repository" list box

1. Select **Save changes**
