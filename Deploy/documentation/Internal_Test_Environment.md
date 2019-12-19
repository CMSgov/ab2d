# Internal Test Environment

## Table of Contents

1. [Create an AWS IAM user](#create-an-aws-iam-user)
1. [Configure AWS CLI](#configure-aws-cli)
1. [Configure Terraform logging](#configure-terraform-logging)
1. [Configure base AWS components](#configure-base-aws-components)
   * [Create AWS keypair](#create-aws-keypair)
   * [Create required S3 buckets](#create-required-s3-buckets)
   * [Create policies](#create-policies)
   * [Create roles](#create-roles)
   * [Create instance profiles](#create-instance-profiles)
   * [Configure IAM user deployers](#configure-iam-user-deployers)
1. [Create or update base aws environment](#create-or-update-base-aws-environment)
1. [Update existing environment](#update-existing-environment)
1. [Deploy and configure Jenkins](#deploy-and-configure-jenkins)
1. [Deploy AB2D static site](#deploy-ab2d-static-site)

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

   *Example for testing Shared environment in SemanticBits demo environment:*

   ```ShellSession
   $ aws configure --profile=sbdemo-shared
   ```

   *Example for testing Dev environment in SemanticBits demo environment:*

   ```ShellSession
   $ aws configure --profile=sbdemo-dev
   ```

   *Example for testing Sandbox environment in SemanticBits demo environment:*

   ```ShellSession
   $ aws configure --profile=sbdemo-sbx
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

## Configure Terraform logging

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

## Configure base AWS components

### Create AWS keypair

1. Create keypair

   *Example for controllers within SemanticBits demo environment:*

   ```ShellSession
   $ aws --region us-east-1 ec2 create-key-pair \
     --key-name ab2d-sbdemo-shared \
     --query 'KeyMaterial' \
     --output text \
     > ~/.ssh/ab2d-sbdemo-shared.pem
   ```

1. Change permissions of the key

   *Example for controllers within SemanticBits demo environment:*

   ```ShellSession
   $ chmod 600 ~/.ssh/ab2d-sbdemo-shared.pem
   ```

1. Output the public key to the clipboard

   *Example for controllers within SemanticBits demo environment:*

   ```ShellSession
   $ ssh-keygen -y -f ~/.ssh/ab2d-sbdemo-shared.pem | pbcopy
   ```

1. Update the "authorized_keys" file for the environment

   1. Open the "authorized_keys" file for the environment
   
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/ab2d-sbdemo-dev/authorized_keys
      ```

   1. Paste the public key under the "Keys included with CentOS image" section

   1. Save and close the file

### Create required S3 buckets

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-dev"
   ```

1. Create S3 bucket for automation

   ```ShellSession
   $ aws s3api create-bucket --bucket ab2d-automation --region us-east-1
   ```

1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket ab2d-automation \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

### Create policies

1. Set target profile
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-shared"
   ```

1. Change to the "ab2d-sbdemo-shared" environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbdemo-shared
   ```

1. Create "Ab2dAccessPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dAccessPolicy \
     --policy-document file://ab2d-access-policy.json
   ```

1. Create "Ab2dAssumePolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dAssumePolicy \
     --policy-document file://ab2d-assume-policy.json
   ```

1. Create "Ab2dPackerPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dPackerPolicy \
     --policy-document file://ab2d-packer-policy.json
   ```

1. Create "Ab2dS3AccessPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dS3AccessPolicy \
     --policy-document file://ab2d-s3-access-policy.json
   ```

1. Create "Ab2dPermissionToPassRolesPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dPermissionToPassRolesPolicy \
     --policy-document file://ab2d-permission-to-pass-roles-policy.json
   ```

1. Create "Ab2dSecretsPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dSecretsPolicy \
     --policy-document file://ab2d-secrets-policy.json
   ```

### Create roles

1. Set target profile
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-shared"
   ```

1. Change to the "ab2d-sbdemo-shared" environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbdemo-shared
   ```

1. Create "Ab2dInstanceRole" role

   ```ShelSession
   $ aws iam create-role \
     --role-name Ab2dInstanceRole \
     --assume-role-policy-document file://ab2d-instance-role-trust-relationship.json
   ```

1. Attach required policies to the "Ab2dInstanceRole" role

   ```ShellSession
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAssumePolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dPackerPolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dS3AccessPolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dSecretsPolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role     
   ```

1. Create "Ab2dManagedRole" role

   ```ShelSession
   $ aws iam create-role \
     --role-name Ab2dManagedRole \
     --assume-role-policy-document file://ab2d-managed-role-trust-relationship.json
   ```

1. Attach required policies to the "Ab2dManagedRole" role

   ```ShellSession
   $ aws iam attach-role-policy \
     --role-name Ab2dManagedRole \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dAccessPolicy
   ```

### Create instance profiles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-shared"
   ```

1. Create instance profile

   ```ShellSession
   $ aws iam create-instance-profile \
     --instance-profile-name Ab2dInstanceProfile
   ```

1. Attach "Ab2dInstanceRole" to "Ab2dInstanceProfile"

   ```ShellSession
   $ aws iam add-role-to-instance-profile \
     --role-name Ab2dInstanceRole \
     --instance-profile-name Ab2dInstanceProfile
   ```

### Configure IAM user deployers

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo-shared"
   ```
   
1. Attach the Ab2dPermissionToPassRolesPolicy to an IAM user that runs the automation

   *Example for lonnie.hanekamp@semanticbits.com:*
   
   ```ShellSession
   $ aws iam attach-user-policy \
     --policy-arn arn:aws:iam::114601554524:policy/Ab2dPermissionToPassRolesPolicy \
     --user-name lonnie.hanekamp@semanticbits.com
   ```

2. Repeat this step for all users

## Create or update base aws environment

1. Change to the deploy directory

   *Format:*
   
   ```ShellSession
   $ cd {code directory}/ab2d/Deploy
   ```

   *Example:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Create the VPC for the dev environment
   
   ```ShellSession
   $ ./create-vpc-for-sbdemo.sh \
     --environment=dev \
     --region=us-east-2 \
     --vpc-cidr-block-1=10.242.26.0/24 \
     --vpc-cidr-block-2=10.242.5.128/26 \
     --subnet-public-1-cidr-block=10.242.5.128/27 \
     --subnet-public-2-cidr-block=10.242.5.160/27 \
     --subnet-private-1-cidr-block=10.242.26.0/25 \
     --subnet-private-2-cidr-block=10.242.26.128/25
   ```

1. Create the VPC for the sbx environment

   > *** TO DO ***: Change CIDR values to match assigned values from CMS AWS account
   
   ```ShellSession
   $ ./create-vpc-for-sbdemo.sh \
     --environment=sbx \
     --region=us-east-2 \
     --vpc-cidr-block-1=10.242.27.0/24 \
     --vpc-cidr-block-2=10.242.6.128/26 \
     --subnet-public-1-cidr-block=10.242.6.128/27 \
     --subnet-public-2-cidr-block=10.242.6.160/27 \
     --subnet-private-1-cidr-block=10.242.27.0/25 \
     --subnet-private-2-cidr-block=10.242.27.128/25
   ```

1. If creating the AWS environment for Dev, do one of the following

   *Deploy Dev by creating new api and worker images:*

   ```ShellSession
   $ ./create-base-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --vpc-id=$VPC_ID \
     --ssh-username=centos \
     --seed-ami-product-code=aw0evgkw8e5c1q413zgy5pjce \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --build-new-images \
     --auto-approve
   ```

   *Deploy Dev by using the latest existing api and worker images:*

   ```ShellSession
   $ ./create-base-environment.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --vpc-id=$VPC_ID \
     --ssh-username=centos \
     --seed-ami-product-code=aw0evgkw8e5c1q413zgy5pjce \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --use-existing-images \
     --auto-approve
   ```

1. If creating the AWS environment for Sandbox, do one of the following

   *Deploy Sandbox by creating new api and worker images:*

   ```ShellSession
   $ ./create-base-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --vpc-id=$VPC_ID \
     --ssh-username=centos \
     --seed-ami-product-code=aw0evgkw8e5c1q413zgy5pjce \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --build-new-images \
     --auto-approve
   ```

   *Deploy Sandbox by using the latest existing api and worker images:*

   ```ShellSession
   $ ./create-base-environment.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --vpc-id=$VPC_ID \
     --ssh-username=centos \
     --seed-ami-product-code=aw0evgkw8e5c1q413zgy5pjce \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --use-existing-images \
     --auto-approve
   ```

1. If prompted, enter database user at the "Enter desired database_user" prompt

1. If prompted, enter database password at the "Enter desired database_password" prompt

1. If prompted, enter database name at the "Enter desired database_name" prompt

   *IMPORTANT: Since databases are sharing the same database instance, the database names should be unique for each environment and must contain only alphanumeric characters.*
   
   *Example database names:*

   - dev

   - sbx

   - impl

   - prod

1. If prompted, enter database user at the "Enter desired database_user" prompt

1. If prompted, enter database password at the "Enter desired database_password" prompt

1. If prompted, enter database name at the "Enter desired database_name" prompt

   *IMPORTANT: Since databases are sharing the same database instance, the database names should be unique for each environment and must contain only alphanumeric characters.*
   
   *Example database names:*

   - dev

   - sbx

   - impl

   - prod

## Update existing environment

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. If updating the Dev environment, do one of the following

   *Update Dev by creating new api and worker images:*
   
   ```ShellSession
   $ ./deploy.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --ssh-username=centos \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --build-new-images \
     --auto-approve
   ```

   *Update Dev by using the latest existing api and worker images:*

   ```ShellSession
   $ ./deploy.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --ssh-username=centos \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --use-existing-images \
     --auto-approve
   ```

1. If updating the Sandbox environment, do one of the following

   *Update Sandbox by creating new api and worker images:*

   ```ShellSession
   $ ./deploy.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --ssh-username=centos \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --build-new-images \
     --auto-approve
   ```

   *Update Sandbox by using the latest existing api and worker images:*

   ```ShellSession
   $ ./deploy.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --ssh-username=centos \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --use-existing-images \
     --auto-approve
   ```

## Deploy and configure Jenkins

1. Launch an EC2 instance from the Jenkins AMI

1. Note that the following steps will likely be different on the gold disk version.*

1. Connection to the Jenkins instance

   *Example:*
   
   ```ShellSession
   $ ssh -i ~/.ssh/ab2d-sbdemo-shared.pem centos@54.208.238.51
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

## Deploy AB2D static site

1. Note that this process will create an S3 website endpoint as the origin within CloudFront

1. Generate the website

   1. Install Bundler
   
      ```ShellSession
      $ gem install bundler
      ```

   1. Update Ruby Gems
   
      ```ShellSession
      $ gem update --system
      ```
      
   1. Install Jekyll
   
      ```ShellSession
      $ gem install jekyll
      ```

1. Change to the "website" directory

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

1. Test the website

   1. Ensure required gems are installed

      ```ShellSession
      $ bundle install
      ```

   1. Serve website on the jekyll server

      ```ShellSession
     $ bundle exec jekyll serve
     ```
     
   1. Open Chrome

   1. Enter the following in the address bar
   
      > http://127.0.0.1:4000
      
   1. Verify that the website comes up

   1. Return to the terminal where the jekyll server is running
   
   1. Press **control+c** on the keyboard to stop the Jekyll server

1. Verify the generated site

   1. Note that a "_site" directory was automatically generated when you ran "bundle exec jekyll serve"
   
   1. List the contents of the directory

      ```ShellSession
       $ ls _site
      ```
    
   1. Note the following two files that will be used in S3 website hosting configuration

      - index.html

      - 404.html
      
1. Configure the S3 website

   1. Set the target AWS profile

      ```ShellSession
      $ export AWS_PROFILE="sbdemo-shared"
      ```

   1. Copy the website to the "ab2d-website" S3 bucket

      ```ShellSession
      $ aws s3 cp --recursive _site/ s3://sbdemo-ab2d-website/
      ```

   1. Configure static website hosting for the bucket

      ```ShellSession
      $ aws --region us-east-1 s3 website s3://sbdemo-ab2d-website/ \
        --index-document index.html \
	--error-document 404.html
      ```