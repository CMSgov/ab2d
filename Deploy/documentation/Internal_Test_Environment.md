# Internal Test Environment

## Table of Contents

1. [Create an AWS IAM user](#create-an-aws-iam-user)
1. [Configure AWS CLI](#configure-aws-cli)
1. [Configure base AWS environment](#configure-base-aws-environment)
   * [Create AWS keypair](#create-aws-keypair)
   * [Create required S3 buckets](#create-required-s3-buckets)
   * [Create policies](#create-policies)
   * [Create roles](#create-roles)
   * [Create instance profiles](#create-instance-profiles)
   * [Configure IAM user deployers](#configure-iam-user-deployers)
   * [Create AWS Elastic Container Registry repositories for images](#create-aws-elastic-container-registry-repositories-for-images)
   * [Create base aws environment](#create-base-aws-environment)
1. [Deploy and configure Jenkins](#deploy-and-configure-jenkins)
1. [Deploy to test environment](#deploy-to-test-environment)
   * [Configure terraform](#configure-terraform)
   * [Deploy AWS dependency modules](#deploy-aws-dependency-modules)
   * [Deploy AWS application modules](#deploy-aws-application-modules)

## Create an AWS IAM user

1. Request AWS administrator to create a user that has both console and programmatic access to the semanticbitsdemo AWS account

1. Note that the administrator will provide you with a "credentials.csv" file that will include the following information
   
   - User name

   - Password

   - Access key ID

   - Secret access key

   - Console login link

## Configure AWS CLI

1. Configure AWS CLI
   
   ```ShellSession
   $ aws configure --profile=sbdemo
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
   
## Configure base AWS environment

### Create AWS keypair

1. Create keypair
   
   ```ShellSession
   $ aws --region us-east-1 ec2 create-key-pair \
     --key-name ab2d-sbdemo \
     --query 'KeyMaterial' \
     --output text \
     > ~/.ssh/ab2d-sbdemo.pem
   ```

1. Change permissions of the key

   *Example for test cluster:*
   
   ```ShellSession
   $ chmod 600 ~/.ssh/ab2d-sbdemo.pem
   ```

1. Output the public key to the clipboard

   ```ShellSession
   $ ssh-keygen -y -f ~/.ssh/ab2d-sbdemo.pem | pbcopy
   ```

1. Update the "authorized_keys" file for the environment

   1. Open the "authorized_keys" file for the environment
   
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo/authorized_keys
      ```

   1. Paste the public key under the "Keys included with CentOS image" section

   1. Save and close the file

### Create required S3 buckets

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Create S3 bucket for automation

   ```ShellSession
   $ aws s3api create-bucket --bucket cms-ab2d-automation --region us-east-1
   ```

1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket cms-ab2d-automation \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

1. Create S3 file bucket

   ```ShellSession
   $ aws s3api create-bucket --bucket cms-ab2d-dev --region us-east-1
   ```

1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket cms-ab2d-dev \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

1. Create S3 file bucket

   ```ShellSession
   $ aws s3api create-bucket --bucket cms-ab2d-cloudtrail --region us-east-1
   ```

1. Note that the "Elastic Load Balancing Account ID for us-east-1" is the following:

   ```
   127311923021
   ```

1. Note that the "Elastic Load Balancing Account ID" for other regions can be found here

   > See https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/enable-access-logs.html

1. Change to the s3 bucket policies directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/aws/s3-bucket-policies
   ```
   
1. Add this bucket policy to the "cms-ab2d-cloudtrail" S3 bucket

   ```ShellSession
   $ aws s3api put-bucket-policy \
     --bucket cms-ab2d-cloudtrail \
     --policy file://cms-ab2d-cloudtrail-bucket-policy.json
   ```
   
1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket cms-ab2d-cloudtrail \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

### Create policies

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Change to the "iam-policies" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/aws/iam-policies
   ```

1. Create "Ab2dAccessPolicy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dAccessPolicy --policy-document file://ab2d-access-policy.json
   ```

1. Create "Ab2dAssumePolicy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dAssumePolicy --policy-document file://ab2d-assume-policy.json
   ```

1. Create "Ab2dInitPolicy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dInitPolicy --policy-document file://ab2d-init-policy.json
   ```

1. Create "Ab2dPackerPolicy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dPackerPolicy --policy-document file://ab2d-packer-policy.json
   ```

1. Create "Ab2dS3AccessPolicy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dS3AccessPolicy --policy-document file://ab2d-s3-access-policy.json
   ```

1. Create "Ab2dEcsForEc2Policy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dEcsForEc2Policy --policy-document file://ab2d-ecs-for-ec2-policy.json
   ```

1. Create "Ab2dPermissionToPassRolesPolicy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dPermissionToPassRolesPolicy --policy-document file://ab2d-permission-to-pass-roles-policy.json
   ```

### Create roles

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Change to the "iam-roles-trust-relationships" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/aws/iam-roles-trust-relationships
   ```

1. Create "Ab2dInstanceRole" role

   ```ShelSession
   $ aws iam create-role --role-name Ab2dInstanceRole --assume-role-policy-document file://ab2d-instance-role.json
   ```

1. Attach required policies to the "Ab2dInstanceRole" role

   ```ShellSession
   $ aws iam attach-role-policy --role-name Ab2dInstanceRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy
   $ aws iam attach-role-policy --role-name Ab2dInstanceRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dPackerPolicy
   $ aws iam attach-role-policy --role-name Ab2dInstanceRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dS3AccessPolicy
   $ aws iam attach-role-policy --role-name Ab2dInstanceRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dInitPolicy
   $ aws iam attach-role-policy --role-name Ab2dInstanceRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dEcsForEc2Policy
   ```

1. Create "Ab2dManagedRole" role

   ```ShelSession
   $ aws iam create-role --role-name Ab2dManagedRole --assume-role-policy-document file://ab2d-managed-role.json
   ```

1. Attach required policies to the "Ab2dManagedRole" role

   ```ShellSession
   $ aws iam attach-role-policy --role-name Ab2dManagedRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dAccessPolicy
   ```

### Create instance profiles

1. Create instance profile

   ```ShellSession
   $ aws iam create-instance-profile --instance-profile-name Ab2dInstanceProfile
   ```

1. Attach "Ab2dInstanceRole" to "Ab2dInstanceProfile"

   ```ShellSession
   $ aws iam add-role-to-instance-profile \
     --role-name Ab2dInstanceRole \
     --instance-profile-name Ab2dInstanceProfile
   ```

### Configure IAM user deployers

1. Attach the Ab2dPermissionToPassRolesPolicy to an IAM user that runs the automation

   *Example for lonnie.hanekamp@semanticbits.com:*
   
   ```ShellSession
   $ aws iam attach-user-policy \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dPermissionToPassRolesPolicy \
     --user-name lonnie.hanekamp@semanticbits.com
   ```

2. Repeat this step for all users

### Create AWS Elastic Container Registry repositories for images

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Authenticate Docker to default Registry
   
   ```ShellSession
   $ read -sra cmd < <(aws ecr get-login --no-include-email)
   $ pass="${cmd[5]}"
   $ unset cmd[4] cmd[5]
   $ "${cmd[@]}" --password-stdin <<< "$pass"
   ```

1. Note that the authentication is good for a 12 hour session

1. Change to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```
   
1. Build the docker images of API and Worker nodes

   1. Build all docker images

      ```ShellSession
      $ make docker-build
      ```

   1. Check the docker images

      ```ShellSession
      $ docker image ls
      ```

   1. Note the output only includes the following

      *Format:*

      ```
      {repository}:{tag}
      ```

      *Example:*

      ```
      maven:3-jdk-12
      ```
      
   1. Build with "docker-compose"

      ```ShellSession
      $ docker-compose build
      ```
      
   1. Check the docker images

      ```ShellSession
      $ docker image ls
      ```

   1. Note the output includes the following

      - ab2d_worker:latest

      - ab2d_api:latest

      - maven:3-jdk-12

      - openjdk:12

1. Create an AWS Elastic Container Registry (ECR) for "ab2d_api"

   ```ShellSession
   $ aws ecr create-repository --repository-name ab2d_api
   ```

1. Tag the "ab2d_api" image for ECR

   ```ShellSession
   $ docker tag ab2d_api:latest 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

1. Push the "ab2d_api" image to ECR

   ```ShellSession
   $ docker push 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_api:latest
   ```

1. Create an AWS Elastic Container Registry (ECR) for "ab2d_worker"

   ```ShellSession
   $ aws ecr create-repository --repository-name ab2d_worker
   ```

1. Tag the "ab2d_worker" image for ECR

   ```ShellSession
   $ docker tag ab2d_worker:latest 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

1. Push the "ab2d_worker" image to ECR

   ```ShellSession
   $ docker push 114601554524.dkr.ecr.us-east-1.amazonaws.com/ab2d_worker:latest
   ```

### Create base aws environment

1. Change to the environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo
   ```

1. Create the AWS networking

   ```ShellSession
   $ ./create-base-environment.sh
   ```

## Deploy and configure Jenkins

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
   
1. Determine and note the latest CentOS AMI

   ```ShellSession
   $ aws --region us-east-1 ec2 describe-images \
     --owners aws-marketplace \
     --filters Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce \
     --query 'Images[*].[ImageId,CreationDate]' \
     --output text \
     | sort -k2 -r \
     | head -n1
   ```
   
1. Configure packer for jenkins

   1. Open the "app.json" file

      ```ShellSession
      $ vim ~/code/ab2d/Deploy/packer/jenkins/app.json
      ```

   1. Change the gold disk AMI to the noted CentOS AMI

      *Example:*
      
      ```
      ami-02eac2c0129f6376b
      ```
      
   1. Change the subnet to the first public subnet

      ```
      "subnet_id": "subnet-077269e0fb659e953",
      ```

   1. Change the VPC ID

      ```
      "vpc_id": "vpc-00dcfaadb3fe8e3a2",
      ```

   1. Change the ssh_username
   
      *Example:*
   
      ```
      "ssh_username": "centos",
      ```

   1. Save and close the file

1. Change to the environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo
   ```

1. Create Jenkins AMI

   ```ShellSession
   $ ./create-jenkins-ami.sh --environment=sbdemo
   ```

1. Launch an EC2 instance from the Jenkins AMI

1. Note that the following steps will likely be different on the gold disk version.*

1. Connection to the Jenkins instance

   *Format:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbdemo.pem centos@54.208.238.51
   ```

   *Example:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbdemo.pem centos@54.208.238.51
   ```

1. Install, enable, and start firewalld

   1. Install firewalld

      ```ShellSession
      $ sudo yum install firewalld -y
      ```

   1. Enable firewalld

      ```ShellSession
      $ sudo systemctl enable firewalld
      ```

   1. Start firewalld

      ```ShellSession
      $ sudo systemctl start firewalld
      ```

   1. Check the firewall status

      ```ShellSession
      $ sudo firewall-cmd --state
      ```

   1. Verify that the following is displayed in the output

      ```
      running
      ```

   1. Check the firewalld daemon status

      ```ShellSession
      $ systemctl status firewalld | grep running
      ```

   1. Verify that the following is displayed

      ```
      Active: active (running)...
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

1. Add HTTP as a permanent service for the public zone

   ```ShellSession
   $ sudo firewall-cmd --zone=public --add-service=http --permanent
   ```

1. Determine what ports the server is expecting for network traffic

   1. Install Security-Enhanced Linux (SELinux) tools
   
      ```ShellSession
      $ sudo yum install policycoreutils-devel -y
      $ sudo yum install setroubleshoot-server -y
      ```

   1. Determine what ports SELinux has configured for incoming network traffic

      ```ShellSession
      $ sepolicy network -t http_port_t
      ```

   1. Note the ports that are output

      ```
      http_port_t: tcp: 80,81,443,488,8008,8009,8443,9000
      ```

   1. Note that port 8080 is not listed

1. Add port 8080 to SELinux policy configuration

   ```ShellSession
   $ sudo semanage port -m -t http_port_t -p tcp 8080
   ```

1. Verify that port 8080 is now included in SELinux policy configuration

   ```ShellSession
   $ sepolicy network -t http_port_t
   ```

1. Note the ports that are output

   ```
   http_port_t: tcp: 8080,80,81,443,488,8008,8009,8443,9000
   ```
   
1. Configure the Jenkins servive for the firewall

   1. Note the Jenkins wiki lists steps that are not necessary with current versions of CentOS
      
   1. Add Jenkins as a permanent service

      ```ShellSession
      $ sudo firewall-cmd --permanent --add-service=jenkins
      ```

   1. Reload

      ```ShellSession
      $ sudo firewall-cmd --reload
      ```

   1. Verify the port forwarding

      ```ShellSession
      $ sudo firewall-cmd --list-all
      ```

   1. Verify the following settings looks like this

      - **services:** dhcpv6-client http jenkins ssh

      - **masquerade:** no

      - **forward-ports:**

1. Check the current status of Jenkins

   1. Enter the following
   
      ```ShellSession
      $ systemctl status jenkins
      ```

   1. Note the following from the output

      ```
      Active: inactive (dead)
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

1. Start Jenkins

   ```ShellSession
   $ sudo systemctl start jenkins
   ```
   
1. Check the current status of Jenkins

   1. Enter the following
    
      ```ShellSession
      $ systemctl status jenkins -l
      ```
 
   1. Note the following from the output
 
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

1. Open Chrome

1. Enter the following in the address bar

   > http://{jenkins public ip address}:8080

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

      - Username

      - Password

      - Confirm password

      - Full name

      - E-mail address

   1. Select **Save and Continue**

   1. Note the Jenkins URL

   1. Select **Save and Finish**

   1. Select **Start using Jenkins**

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

   1. Enter a password at the **New password** prompt

   1. Reenter the password at the **Retype new password** prompt

1. Note that if the jenkins user needs to run a build on a remote system, it will need SSH keys to do so

1. Allow password authentication

   1. Open the "sshd_config" file

      ```ShellSession
      $ sudo vim /etc/ssh/sshd_config
      ```

   1. Change the following line to look like this

      ```
      PasswordAuthentication yes
      ```

   1. Restart sshd

      ```ShellSession
      $ sudo systemctl restart sshd
      ```

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

   1. Allow the machine to SSH into itself

      ```ShellSession
      $ ssh-copy-id jenkins@localhost
      ```
   
   1. Enter the following at the "Are you sure you want to continue connecting (yes/no)" prompt

      ```
      yes
      ```

   1. Enter the jenkins user password at the "jenkins@localhost's password" prompt

   1. Switch back to the centos user

      ```ShellSession
      $ exit
      ```

1. Disallow password authentication

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

1. Verify that SSH to "jenkins@localhost" now works

   ```ShellSession
   $ sudo -u jenkins ssh jenkins@localhost
   ```

1. Exit the shell

   ```ShellSession
   $ exit
   ```
   
1. Note that the resaon why jenkins needs ssh access to its server is so that can run builds locally (if required), since jenkins will do builds over SSH by default

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

## Deploy to test environment

### Configure terraform

1. Modify the SSH config file

   1. Open the SSH config file

      ```ShellSession
      $ vim ~/.ssh/config
      ```

   2. Add or modify SSH config file to include the following line

      ```
      StrictHostKeyChecking no
      ```

1. Ensure terraform log directory exists

   ```ShellSession
   $ sudo mkdir -p /var/log/terraform
   $ sudo chown -R "$(id -u)":"$(id -g -nr)" /var/log/terraform
   ```

1. Note the following terraform logging levels are available

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

### Deploy AWS dependency modules

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

1. Initialize terraform

   ```ShellSession
   $ terraform init
   ```

1. Validate terraform

   ```ShellSession
   $ terraform validate
   ```

1. Deploy KMS

   ```ShellSession
   $ terraform apply \
     --target module.kms --auto-approve
   ```

1. Create S3 "cms-ab2d-cloudtrail" bucket

   ```ShellSession
   $ aws s3api create-bucket --bucket cms-ab2d-cloudtrail --region us-east-1
   ```

1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket cms-ab2d-cloudtrail \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

1. Give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cms-ab2d-cloudtrail" bucket

   ```ShellSession
   $ aws s3api put-bucket-acl \
     --bucket cms-ab2d-cloudtrail \
     --grant-write URI=http://acs.amazonaws.com/groups/s3/LogDelivery \
     --grant-read-acp URI=http://acs.amazonaws.com/groups/s3/LogDelivery
   ```
   
1. Deploy additional S3 configuration

   ```ShellSession
   $ terraform apply \
     --target module.s3 --auto-approve
   ```

1. Add the bucket policy to the "cms-ab2d-cloudtrail" S3 bucket

   ```ShellSession
   $ aws s3api put-bucket-policy --bucket cms-ab2d-cloudtrail --policy file://cms-ab2d-cloudtrail-bucket-policy.json
   ```
   
1. Deploy Elastic File System (EFS)

   ```ShellSession
   $ terraform apply \
     --target module.efs --auto-approve
   ```

1. Get the file system id of EFS

   ```ShellSession
   $ aws efs describe-file-systems | grep FileSystemId
   ```

1. Note the file system id

   *Example:
   
   ```
   fs-225c97a3
   ```

1. Update "provision-app-instance.sh" with the file system id

   1. Open "provision-app-instance.sh"

      ```ShellSession
      $ vim ~/code/ab2d/Deploy/packer/app/provision-app-instance.sh
      ```

   1. Update the following line

      *Format:*
      
      ```
      echo '{file system id}:/ /mnt/efs efs defaults,_netdev 0 0' | sudo tee -a /etc/fstab
      ```

      *Example:*
      
      ```
      echo 'fs-7f7bb9fe:/ /mnt/efs efs defaults,_netdev 0 0' | sudo tee -a /etc/fstab
      ```

   1. Save and close the file
      
1. Deploy database

   *Format:*
   
   ```ShellSession
   $ terraform apply \
     --var "db_username={db username}" \
     --var "db_password={db password}" \
     --target module.db --auto-approve
   ```

1. Determine and note the latest CentOS AMI

   ```ShellSession
   $ aws --region us-east-1 ec2 describe-images \
     --owners aws-marketplace \
     --filters Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce \
     --query 'Images[*].[ImageId,CreationDate]' \
     --output text \
     | sort -k2 -r \
     | head -n1
   ```
   
1. Configure packer for the application

   1. Open the "app.json" file

      ```ShellSession
      $ vim ~/code/ab2d/Deploy/packer/app/app.json
      ```

   1. Change the gold disk AMI to the noted CentOS AMI

      *Example:*
       
      ```
      ami-02eac2c0129f6376b
      ```
      
   1. Change the subnet to the first public subnet

      ```
      "subnet_id": "subnet-077269e0fb659e953",
      ```

   1. Change the VPC ID

      ```
      "vpc_id": "vpc-00dcfaadb3fe8e3a2",
      ```

   1. Change the builders settings
   
      *Example:*
   
      ```
      "iam_instance_profile": "lonnie.hanekamp@semanticbits.com",
      "ssh_username": "centos",
      ```

   1. Save and close the file

### Deploy AWS application modules

1. Set the AWS profile for the target environment

   ```ShellSession
   $ source ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo/set-aws-profile.sh
   ```
   
1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy application components

   ```ShellSession
   $ ./deploy.sh --environment=sbdemo --auto-approve
   ```
   