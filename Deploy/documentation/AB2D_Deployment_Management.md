# AB2D Management

## Table of Contents

1. [Setup Jenkins master in management AWS account](#setup-jenkins-master-in-management-aws-account)
1. [Setup Jenkins agents in management AWS account](#setup-jenkins-agent-in-management-aws-account)
1. [Allow jenkins users to use a login shell](#allow-jenkins-users-to-use-a-login-shell)
   * [Modify the jenkins user on Jenkins master to allow it to use a login shell](#modify-the-jenkins-user-on-jenkins-master-to-allow-it-to-use-a-login-shell)
   * [Create a jenkins user on Jenkins agent and allow it to use a login shell](#create-a-jenkins-user-on-jenkins-agent-and-allow-it-to-use-a-login-shell)
1. [Set up SSH communication for jenkins](#set-up-ssh-communication-for-jenkins)
1. [Configure Jenkins master](#configure-jenkins-master)
   * [Enable Jenkins](#enable-jenkins)
   * [Initialize the Jenkins GUI](#initialize-the-jenkins-gui)
1. [Configure Jenkins agent](#configure-jenkins-master)
   * [Install development tools on Jenkins agent](#install-development-tools-on-jenkins-agent)
   * [Configure AWS CLI for management environment on Jenkins agent](#configure-aws-cli-for-management-environment-on-jenkins-agent)
   * [Configure AWS CLI for Dev environment on Jenkins agent](#configure-aws-cli-for-dev-environment-on-jenkins-agent)
   * [Create a directory for jobs on the Jenkins agent](#create-a-directory-for-jobs-on-the-jenkins-agent)
   * [Install python3, pip3, and required pip modules on Jenkins agent](#install-python3-pip3-and-required-pip-modules-on-jenkins-agent)
   * [Install Terraform on Jenkins agent](#install-terraform-on-jenkins-agent)
   * [Configure Terraform logging on Jenkins agent](#configure-terraform-logging-on-jenkins-agent)
   * [Install maven on Jenkins agent](#install-maven-on-jenkins-agent)
   * [Install jq on Jenkins agent](#install-jq-on-jenkins-agent)
   * [Add the jenkins user to the docker group](#add-the-jenkins-user-to-the-docker-group)
   * [Ensure jenkins can use the Unix socket for the Docker daemon](#ensure-jenkins-can-use-the-unix-socket-for-the-docker-daemon)
   * [Install packer on Jenkins agent](#install-packer-on-jenkins-agent)
   * [Setup ruby environment on Jenkins agent](#setup-ruby-environment-on-jenkins-agent)
   * [Install postgresql10-devel on Jenkins agent](#install-postgresql10-devel-on-jenkins-agent)
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
   * [Install and configure the GitHub Authentication plugin including adding GitHub users](#install-and-configure-the-github-authentication-plugin-including-adding-github-users)
   * [Log on to Jenkins using GitHub OAuth authentication](#log-on-to-jenkins-using-github-oauth-authentication)
   * [Add the Jenkins agent node](#add-the-jenkins-agent-node)
   * [Create a "development" folder in Jenkins](#create-a-development-folder-in-jenkins)
   * [Configure a Jenkins project for development application deploy](#configure-a-jenkins-project-for-development-application-deploy)
   * [Create a "sandbox" folder in Jenkins](#create-a-sandbox-folder-in-jenkins)
   * [Configure a Jenkins project for sandbox application deploy](#configure-a-jenkins-project-for-sandbox-application-deploy)
1. [Configure Jenkins user to allow for SSH](#configure-jenkins-user-to-allow-for-ssh)
1. [Configure executors for Jenkins master](#configure-executors-for-jenkins-master)
1. [Configure Jenkins CLI](#configure-jenkins-cli)
1. [Export Jenkins projects](#export-jenkins-projects)

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

1. Set the target environment to the "Mgmt AWS account"

   ```ShellSession
   $ source ./Deploy/bash/set-env.sh
   ```

1. Change to the management terraform directory

   ```ShellSession
   $ cd ./Deploy/terraform/environments/ab2d-mgmt-east-dev-shared
   ```

1. Remove the Jenkins master instance from terraform state

   ```ShellSesssion
   $ terraform state rm module.jenkins_master.aws_instance.jenkins_master
   ```

1. Change to the "ab2d" repo directory again

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

1. If there is an existing "ab2d-jenkins-agent" EC2 instance, rename it in the AWS management account as follows

   ```
   ab2d-jenkins-agent-old
   ```

1. Open a terminal

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create EC2 instance for Jenkins master

   ```ShellSession
   $ ./bash/deploy-jenkins-agent.sh
   ```
   
1. Wait for the automation to complete

## Allow jenkins users to use a login shell

### Modify the jenkins user on Jenkins master to allow it to use a login shell

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

### Create a jenkins user on Jenkins agent and allow it to use a login shell

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

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

1. Authorize jenkins master to SSH into itself

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
      
1. Verify that jenkins master can SSH into itself again from the jenkins home directory

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Enter the following
   
      ```ShellSession
      $ ssh jenkins@localhost
      ```

   1. Exit the second jenkins user session

      ```ShellSession
      $ exit
      ```

   1. Exit first jenkins user session

      ```ShellSession
      $ exit
      ```

1. Verify that jenkins master can SSH into itself from the ec2-user home directory

   1. Verify that you are back to the ec2-user user session
   
   1. Enter the following
   
      ```ShellSession
      $ sudo -u jenkins ssh jenkins@localhost
      ```

   1. Exit the internal SSH session

      ```ShellSession
      $ exit
      ```

1. Authorize jenkins master to SSH into jenkins agent

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@$JENKINS_AGENT_PRIVATE_IP
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Copy the password from the following 1Password entry to the clipboard

      ```
      jenkins agent redhat user
      ```
      
   1. Paste the jenkins master redhat user password at the "jenkins@{jenkins agent private ip}'s password" prompt

   1. Test ssh from jenkins user session

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_PRIVATE_IP
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

1. Connect to Jenkins agent

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Connect to Jenkins master

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PRIVATE_IP
   ```
   
1. Verify that Jenkins master can SSH into Jenkins agent

   1. Switch to the jenkins user

      ```ShellSession
      $ sudo su - jenkins
      ```
   
   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh jenkins@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Exit Jenkins agent

   ```ShellSession
   $ exit
   ```

1. Exit jenkins user on Jenkins master

   ```ShellSession
   $ exit
   ```

1. Exit Jenkins master

   ```ShellSession
   $ exit
   ```

## Configure Jenkins master

### Enable Jenkins

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

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Get credentials for the Management AWS account

   ```ShellSession
   $ source ./bash/set-env.sh
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

   1. Return to the terminal

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
   
## Configure Jenkins agent

### Install development tools on Jenkins agent

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

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Install development tools

   ```ShellSession
   $ sudo yum group install "Development Tools" -y
   ```

1. Note that the following tools were installed

   - autoconf

   - automake

   - bison

   - byacc

   - cscope

   - ctags

   - diffstat

   - doxygen

   - elfutils
   
   - flex

   - gcc

   - gcc-c++

   - gcc-gfortran

   - indent

   - intltool

   - libtool

   - patch

   - patchutils

   - rcs

   - redhat-rpm-config

   - rpm-build

   - rpm-sign

   - subversion

   - swig

   - systemtap

### Configure AWS CLI for management environment on Jenkins agent

> *** TO TO ***: Remove this section after verification

1. Connect to Jenkins agent

   1. Ensure that you are connected to the Cisco VPN

   1. Get credentials for the Management AWS account

      ```ShellSession
      $ source ./bash/set-env.sh
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the private IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```

1. Configure AWS CLI
   
   ```ShellSession
   $ aws configure --profile=ab2d-mgmt-east-dev
   ```

1. Enter {your aws access key} at the **AWS Access Key ID** prompt

1. Enter {your aws secret access key} at the AWS Secret Access Key prompt

1. Enter the following at the **Default region name** prompt

   ```
   us-east-1
   ```

1. Enter the following at the **Default output format** prompt

   ```
   json
   ```

1. Examine the contents of your AWS credentials file

   ```ShellSession
   $ cat ~/.aws/credentials
   ```

1. Exit the Jenkins agent node

   ```ShellSession
   $ exit
   ```
   
### Configure AWS CLI for Dev environment on Jenkins agent

> *** TO TO ***: Remove this section after verification

1. Connect to Jenkins agent

   1. Ensure that you are connected to the Cisco VPN

   1. Set the management AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Get the private IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. Ensure that you are connected to the Cisco VPN

   1. SSH into the instance using the private IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Configure AWS CLI
   
   ```ShellSession
   $ aws configure --profile=ab2d-dev
   ```

1. Enter {your aws access key} at the **AWS Access Key ID** prompt

1. Enter {your aws secret access key} at the AWS Secret Access Key prompt

1. Enter the following at the **Default region name** prompt

   ```
   us-east-1
   ```

1. Enter the following at the **Default output format** prompt

   ```
   json
   ```

1. Examine the contents of your AWS credentials file

   ```ShellSession
   $ cat ~/.aws/credentials
   ```

1. Exit the Jenkins agent node

   ```ShellSession
   $ exit
   ```

### Create a directory for jobs on the Jenkins agent

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

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Switch to the jenkins user

   ```ShellSession
   $ sudo su - jenkins
   ```
   
1. Create a directory for jobs that are built on the Jenkins agent on behalf of the Jenkins Master

   ```ShellSession
   $ mkdir -p ~/jenkins_agent
   ```

1. Exit the jenkins user

   ```ShellSession
   $ exit
   ```
   
1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```
   
### Install python3, pip3, and required pip modules on Jenkins agent

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

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance
   
      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

1. Exit the Jenkins agent node

   ```ShellSession
   $ exit
   ```

### Install Terraform on Jenkins agent

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
   Terraform v0.12.9
   ```
   
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

1. Connect to Jenkins agent

   1. Get the public IP address of Jenkins EC2 instance

      ```ShellSession
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

1. Exit the Jenkins agent node

   ```ShellSession
   $ exit
   ```

### Configure Terraform logging on Jenkins agent

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

### Install maven on Jenkins agent

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

### Install jq on Jenkins agent

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```

1. Install jq

   ```ShellSession
   $ sudo yum install jq -y
   ```

1. Verify jq by checking its version

   ```ShellSession
   $ jq --version
   ```

### Add the jenkins user to the docker group

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

### Ensure jenkins can use the Unix socket for the Docker daemon

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

### Install packer on Jenkins agent

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

1. Download paker

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

1. Exit the Jenkins agent node

   ```ShellSession
   $ exit
   ```

### Install Postgres 11 on Jenkins agent

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

### Setup ruby environment on Jenkins agent

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
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

1. Exit the Jenkins agent

   ```ShellSession
   $ exit
   ```

### Install postgresql10-devel on Jenkins agent

1. Note that I couldn't install postgresql11-devel due to dependencies that could not be installed, so I am using "postgresql10-devel" instead

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
      $ JENKINS_AGENT_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_AGENT_PRIVATE_IP
      ```
      
1. Install postgresql10-devel

   ```ShellSession
   $ sudo yum -y install postgresql10-devel
   ```

> *** TO DO ***: Stopping point for new Jenkins setup

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

1. Get the public IP address of Jenkins master

   1. Ensure that you are connected to the Cisco VPN

   1. Set the management AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Get the public IP address of Jenkins master

      ```ShellSession
      $ JENKINS_MASTER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
        --output text)
      ```

1. Display the contents of the jenkins ssh private key

   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PUBLIC_IP \
     sudo -u jenkins cat /var/lib/jenkins/.ssh/id_rsa \
     | pbcopy
   ```

1. Copy the private key to the clipboard

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Credentials** from the leftmost panel

1. Select **System** under "Credentials" from the leftmost panel

1. Select **Global credentials (unrestricted)**

1. Select **Add credentials** from the leftmost panel

1. Note that the username to configure in the next step is the "jenkins" linux user associated with the SSH private key

1. Configure the credentials

   - **Kind:** SSH Username with private key

   - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

   - **Username:** jenkins

   - **Private Key - Enter directly:** selected

   - Select **Add**

   - **Enter New Secret Below:** {paste contents of private key for jenkins}

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
      
1. Select **Credentials** from the leftmost panel

1. Select **System** under *Credentials* from the leftmost panel

1. Select **Global credentials (unrestricted)**

1. Select **Add credentials** from the leftmost panel

1. Configure the credentials

   - **Kind:** Secret text

   - **Scope:** Global (Jenkins, nodes, items, all child items, etc)

   - **Secret:** {personal access token for lhanekam}

   - **ID:** GITHUB_AB2D_JENKINS

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

   - **Username:** ab2d-jenkins

   - **Password:** {personal access token for ab2d-jenkins}

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

1. Open a terminal

1. Copy the private IP address of Jenkins agent to the clipboard

   1. Set the management AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Copy the private IP address of Jenkins agent to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text \
	| pbcopy
      ```

1. Return to the Jenkins GUI

1. Select **Add** below the "Successful connection" display

1. Configure Jenkins agent

   - **Hostname:** 10.123.1.203 (use the private IP address)

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

1. Scroll down to "Authorize Project"

1. Check **Authorize Project**

1. Select **Download now and install after restart**

1. Check **Restart Jenkins when installation is complete and no jobs are running**

1. Select **Jenkins** in the top left of the page

1. Select **Manage Jenkins**

1. Scroll down to the "Security" section

1. Select **Configure Global Security**

1. Scroll down to the the "Access Control for Builds" section

1. Note that there are two "Access Control for Builds" sections that can be configured

   - Pre-project configurable Build Authorization

   - Project default Build Authorization

1. Note that at this point only "Project default Build Authorization" is being configured to behave with default "Run as SYSTEM" behavior

   > *** TO DO ***: Consider additional strategies for configuring "Access Control for Builds" as additional users are added.

1. Select **Add**

1. Select **Project default Build Authorization**

1. Select "Run as SYSTEM" from the **Strategy** dropdown

1. Select **Apply**

1. Select **Save**

### Register a new OAuth application with GitHub

1. Open Chome

1. Enter the following in the address bar

   > https://github.com

1. Select the profile icon in the top right of the page

1. Select **Sign out**

1. Select **Sign in**

1. Log on using the "ab2d-jenkins" user (see 1Password)

1. Type the authentication code that was sent to Lonnie's phone in the **Authentication code** text box

1. Select **Verify**

1. Select the profile icon in the top right of the page

1. Select **Settings**

1. Select **Developer settings**

1. Select **OAuth Apps**

1. Select **Register a new application**

1. Configure "Register a new OAuth application" as follows

   - **Application name:** jenkins-github-authentication

   - **Homepage URL:** https://ab2d.cms.gov

   - **Application description:** {blank}

   - **Authorization callback URL:** https://{private ip address of jenkins master}:8080/securityRealm/finishLogin

1. Select **Register application

1. Note the following

   - Client ID

   - Client Secret

1. Create the following in 1Password

   - **jenkins-github-authentication Client ID:** {client id}

   - **jenkins-github-authentication Client Secret:** {client secret}

1. Log out of GitHub
   
### Install and configure the GitHub Authentication plugin including adding GitHub users

1. Connect to Jenkins master

   1. Set the management AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Get the public IP address of Jenkins master

      ```ShellSession
      $ JENKINS_MASTER_PUBLIC_IP=$(aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-master" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PublicIpAddress" \
        --output text)
      ```

   1. SSH into the instance using the public IP address

      ```ShellSession
      $ ssh -i ~/.ssh/ab2d-mgmt-east-dev.pem ec2-user@$JENKINS_MASTER_PUBLIC_IP
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

1. Scroll down to the "Security" section

1. Select **Configure Global Security** under the "Security" section

1. Select the **Github Authentication Plugin** radio button under "Security Realm" within the "Authentication" section

1. Configure "Global GitHub OAuth Settings" as follows

   - **GitHub Web URI:** https://github.com

   - **GitHub API URI:** https://api.github.com

   - **Client ID:** {jenkins-github-authentication client id from 1password}

   - **Client Secret:** {jenkins-github-authentication client secret from 1password}

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

1. Open Jenkins again

1. Note that you will be prompted to log on to GitHub, if you are not already logged in

1. Log on to GitHub

1. Note the following appears on the "Authorize jenkins-github-authentication" page

   - **jenkins-github-authentication by lhanekam:** wants to access your {your github user} account

   - **Organizations and teams:** Read-only access

   - **Repositories:** Public and private

   - **Personal user data:** Email addresses (read-only)

   - **Organization access:** CMSgov

1. Select **Authorize lhanekam**

1. Verify that the Jenkins page loads

### Add the Jenkins agent node

1. Open a terminal

1. Get and note the private IP address of Jenkins agent to the clipboard

   1. Set the management AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Copy the private IP address of Jenkins agent to the clipboard
   
      ```ShellSession
      $ aws --region us-east-1 ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
        --output text
      ```

   1. Note the private IP address of the Jenkins agent

      *Format:*

      ```
      {private ip address of the jenkins agent}
      ```

      *Example:*

      ```
      10.242.37.53
      ```

1. Get and note the number of CPUs for Jenkins agent

   1. Set the management AWS profile

      ```ShellSession
      $ export AWS_PROFILE=ab2d-mgmt-east-dev
      ```

   1. Set the region

      ```ShellSession
      $ export REGION=us-east-1
      ```
      
   1. Get the instance type of the Jenkins agent
   
      ```ShellSession
      $ JENKINS_AGENT_INSTANCE_TYPE=$(aws --region "${REGION}" ec2 describe-instances \
        --filters "Name=tag:Name,Values=ab2d-jenkins-agent" \
        --query="Reservations[*].Instances[?State.Name == 'running'].InstanceType" \
        --output text)
      ```

   1. Get the numbr of CPUs for Jenkins agent

      ```ShellSession
      $ aws --region "${REGION}" ec2 describe-instance-types \
        --instance-types "${JENKINS_AGENT_INSTANCE_TYPE}" --query "InstanceTypes[*].VCpuInfo.DefaultVCpus" \
        --output text
      ```

   1. Note the number that is output

      *Format:*

      ```
      {cpu count for jenkins agent}
      ```

      *Example:*

      ```
      4
      ```
      
1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI
      
1. Select **Manage Jenkins**

1. Select **Manage Nodes and Clouds** under the "System Configuration" section

1. Select **New Node** from the leftmost panel

1. Type the following in the **Node name** text box

   ```
   agent01
   ```

1. Check **Permanent Agent**

1. Select **OK** on the "New Node" page

1. Configure the Jenkins agent node

   *Format:*
   
   - **Name:** agent01

   - **Description:** Agent 01

   - **# of executors:** {cpu count for jenkins agent}

   - **Remote root directory:** /home/jenkins/jenkins_agent

   - **Labels:** {leave blank}

   - **Usage:** Use this node as much as possible

   - **Launch method:** Launch agent agents via SSH

   - **Host:** {private ip address of the jenkins agent}

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

   *Example:*
   
   - **Name:** agent01

   - **Description:** Agent 01

   - **# of executors:** 4

   - **Remote root directory:** /home/jenkins/jenkins_agent

   - **Labels:** {leave blank}

   - **Usage:** Use this node as much as possible

   - **Launch method:** Launch agent agents via SSH

   - **Host:** 10.242.37.53

   - **Credentials:** jenkins

   - **Host Key Verification Strategy:** Known hosts file Verification Strategy

   - **Availability:** Keep this agent online as much as possible

1. Note that the "Labels" property that we are not currently using is typically used to group nodes that are used for different functions

   *Examples:*

   - slave01 to slave05 might all have the label "development_builds"

   - slave31 to slave35 might all have the label "production_builds"

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

      > http://{jenkins master private ip}:8080

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

      > http://{jenkins master private ip}:8080

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

      > http://{jenkins master private ip}:8080

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

      > http://{jenkins master private ip}:8080

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

## Configure Jenkins user to allow for SSH

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** from the leftmost panel

1. Scroll to the "Security" section

1. Select **Manage Credentials**

1. Select the user with "jenkins" ID

1. Select **Configure** from the leftmost panel

1. Scroll down to the "SSH Public Keys" section

1. Copy the public key for the user to the clipboard

   1. Open a terminal

   1. Copy the public key for the user to the clipboard

      ```ShellSession
      $ cat ~/.ssh/id_rsa.pub | pbcopy
      ```

1. Return to the Jenkins GUI

1. Paste the public key into the **SSH Public Keys** text box

1. Select **Apply**

1. Select **Save**

## Configure executors for Jenkins master

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

   1. Log on to the Jenkins GUI

1. Select **Manage Jenkins** in the leftmost panel

1. Select **Configure System**

1. Note the following

   - the "number of executors" defaulted to 2

   - the Jenkins master has 4 cores for its instance type

   - typically you will want the number of executors to match the number of cores

1. Type the following in the **# of executors** text box

   ```
   4
   ```

1. Select **Apply**

1. Select **Save**

## Configure Jenkins CLI

1. Log on to the Jenkins GUI (if not already logged in)

   1. Ensure that you are connected to the Cisco VPN

   1. Open Chrome

   1. Enter the following in the address bar

      *Format:*

      > http://{jenkins master private ip}:8080

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
     -s "http:/{jenkins master private ip}:8080" \
     -auth {jenkins user}:{jenkins password} \
     list-jobs
   ```

1. Verify that you can list Jenkins projects within the development folder

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master private ip}:8080" \
     -auth {jenkins user}:{jenkins password} \
     list-jobs development
   ```

## Export Jenkins projects

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create a jenkins directory

   ```ShellSession
   $ mkdir -p jenkins
   ```

1. Export the development folder

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master private ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job development \
     > ./jenkins/development.xml
   ```

1. Create a "development" directory under the "jenkins" directory

   ```ShellSession
   $ mkdir -p jenkins/development
   ```

1. Export the "deploy-to-development" project

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master private ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job development/deploy-to-development \
     > ./jenkins/development/deploy-to-development.xml
   ```

1. Export the sandbox folder

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master private ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job sandbox \
     > ./jenkins/sandbox.xml
   ```

1. Create a "sandbox" directory under the "jenkins" directory

   ```ShellSession
   $ mkdir -p jenkins/sandbox
   ```

1. Export the "deploy-to-sandbox" project

   *Format:*

   ```ShellSession
   $ java -jar /opt/jenkins-cli/jenkins-cli.jar \
     -s "http://{jenkins master private ip}:8080" \
     -auth {github user}:{github personal access token} \
     get-job sandbox/deploy-to-sandbox \
     > ./jenkins/sandbox/deploy-to-sandbox.xml
   ```
