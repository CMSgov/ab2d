# AB2D Deployment

## Table of Contents

1. [Note the starting state of the customer AWS account](#note-the-starting-state-of-the-customer-aws-account)
   * [Note dev and sbx components](#note-dev-and-sbx-components)
   * [Note ignored components](#note-ignored-components)
1. [Create an Administrators IAM group](#create-an-administrators-iam-group)
1. [Create an AWS IAM user](#create-an-aws-iam-user)
1. [Configure AWS CLI](#configure-aws-cli)
1. [Configure Terraform logging](#configure-terraform-logging)
1. [Configure base AWS components](#configure-base-aws-components)
   * [Create AWS keypair](#create-aws-keypair)
   * [Create policies](#create-policies)
   * [Create roles](#create-roles)
   * [Create instance profiles](#create-instance-profiles)
   * [Configure IAM user deployers](#configure-iam-user-deployers)
   * [Create required S3 buckets](#create-required-s3-buckets)
1. [Create or update base aws environment](#create-or-update-base-aws-environment)
1. [Update application](#update-application)
1. [Deploy and configure Jenkins](#deploy-and-configure-jenkins)
1. [Deploy AB2D static site](#deploy-ab2d-static-site)
   * [Create static website in S3](#create-static-website-in-s3)

## Note the starting state of the customer AWS account

### Note dev and sbx components

1. Note the dev and sbx VPC

   Tag     |VPC ID               |IPv4 CIDR Count|IPv4 CIDR #1  |IPv4 CIDR #2   
   --------|---------------------|---------------|--------------|---------------
   ab2d-dev|vpc-0c6413ec40c5fdac3|2              |10.242.26.0/24|10.242.5.128/26

1. Note the dev and sbx VPC attributes

   Attribute       |Value
   ----------------|-------------
   DHCP options set|dopt-90a65fea
   DNS resolution  |enable
   DNS hostnames   |disable
   
1. Note the dev and sbx subnets

   Tag               |Subnet ID               |IPv4 CIDR       |Availability Zone
   ------------------|------------------------|----------------|-----------------
   ab2d-dev-private-a|subnet-03d5b59872d950c7d|10.242.26.0/25  |us-east-1a
   ab2d-dev-private-b|subnet-0118d0d6af946bd66|10.242.26.128/25|us-east-1b
   ab2d-dev-public-a |subnet-0044ee15d254fe18b|10.242.5.128/27 |us-east-1a
   ab2d-dev-public-b |subnet-05c149659e3061ef6|10.242.5.160/27 |us-east-1b

1. Note the dev and sbx public subnet attributes

   Public Subnet    |Attribute       |Value
   -----------------|----------------|--------
   ab2d-dev-public-a|Auto-assign IPv4|disabled
   ab2d-dev-public-b|Auto-assign IPv4|disabled

1. Note the dev and sbx route tables

   Tag               |Route Table ID       |Main|Associated Subnet Count|Associalted Subnet #1   |Associalted Subnet #2
   ------------------|---------------------|----|-----------------------|------------------------|------------------------
   None              |rtb-0950b57584f20d93b|Yes |0                      |                        |
   ab2d-dev-private-a|rtb-0c55acf18e7d3cd87|No  |1                      |subnet-03d5b59872d950c7d|
   ab2d-dev-private-b|rtb-09c40213a10ea6406|No  |1                      |subnet-0118d0d6af946bd66|
   ab2d-dev-public   |rtb-090372c9ee83aa450|No  |2                      |subnet-05c149659e3061ef6|subnet-0044ee15d254fe18b

1. Note the dev and sbx routes for "ab2d-dev-private-a"

   Destination      |Target
   -----------------|-------------
   10.242.5.128/26  |local
   10.242.26.0/24   |local
   0.0.0.0/0        |nat-060fbd5ddb57a2f18
   10.223.120.0/22  |tgw-080644ad8f49ecafa
   10.223.126.0/23  |tgw-080644ad8f49ecafa
   10.232.32.0/19   |tgw-080644ad8f49ecafa
   10.242.7.192/26  |tgw-080644ad8f49ecafa
   10.242.193.192/26|tgw-080644ad8f49ecafa
   10.244.96.0/19   |tgw-080644ad8f49ecafa

1. Note the dev and sbx routes for "ab2d-dev-private-b"

   Destination      |Target
   -----------------|-------------
   10.242.5.128/26  |local
   10.242.26.0/24   |local
   0.0.0.0/0        |nat-060fbd5ddb57a2f18
   10.223.120.0/22  |tgw-080644ad8f49ecafa
   10.223.126.0/23  |tgw-080644ad8f49ecafa
   10.232.32.0/19   |tgw-080644ad8f49ecafa
   10.242.7.192/26  |tgw-080644ad8f49ecafa
   10.242.193.192/26|tgw-080644ad8f49ecafa
   10.244.96.0/19   |tgw-080644ad8f49ecafa

1. Note the dev and sbx routes for "ab2d-dev-public"

   Destination      |Target
   -----------------|-------------
   10.242.5.128/26  |local
   10.242.26.0/24   |local
   0.0.0.0/0        |igw-0014bff62a3c1211d

1. Note the dev and sbx internet gateway

   Tag     |ID                   
   --------|---------------------
   ab2d-dev|igw-0014bff62a3c1211d

1. Note the dev and sbx Elastic IPs

   Tag                   |Allocation ID             |Elastic IP   |Private IP  |Network Interface Availability Zone
   ----------------------|--------------------------|-------------|------------|-----------------------------------
   ab2d-dev-nat-gateway-a|eipalloc-0048cd31435409e20|54.208.106.70|10.242.5.158|us-east-1a
   ab2d-dev-nat-gateway-b|eipalloc-0fc6dbdc2a1e412ff|3.218.184.81 |10.242.5.187|us-east-1b

1. Note the dev and sbx NAT gateways

   Tag       |NAT Gateway ID       |Elastic IP Address|Private IP  |Network Interface Availability Zone
   ----------|---------------------|------------------|------------|-----------------------------------
   ab2d-dev-a|nat-060fbd5ddb57a2f18|54.208.106.70     |10.242.5.158|us-east-1a
   ab2d-dev-b|nat-0f4d22a3e997d73c2|3.218.184.81      |10.242.5.187|us-east-1b

1. Note the Network ACLs

   Tag |Network ACL ID       |Associated Subnet Count
   ----|---------------------|-----------------------
   None|acl-0eb267c6c0801f8c9|4


1. Note the Security Groups

   Tag |Group ID            
   ----|--------------------
   None|sg-05752fb69a1a89f86

1. Note the Transit Gateways

   Tag |Transit Gateway ID   |Owner account ID
   ----|---------------------|---------------------
   None|tgw-080644ad8f49ecafa|921617238787 (shared)

1. Note the Transit Gateway Attachments

   Tag                        |Transit Gateway attachment ID|Transit Gateway owner ID|Resource owner account ID
   ---------------------------|-----------------------------|------------------------|-------------------------
   ab2d-dev-InterVPC-East-Prod|tgw-attach-06bf47fa39e5d1578 |921617238787 (shared)   |349849222861

1. Note the Network Interfaces

   Tag |Network interface ID |IPv4 Public IP|Primary private IPv4 IP|Availability Zone|Description
   ----|---------------------|--------------|-----------------------|-----------------|-----------------------------------------------
   None|eni-01f9447520f6efa28|54.208.106.70 |10.242.5.158           |us-east-1a       |Interface for NAT Gateway nat-060fbd5ddb57a2f18
   None|eni-045fc3c9aa02b671c|3.218.184.81  |10.242.5.187           |us-east-1b       |Interface for NAT Gateway nat-0f4d22a3e997d73c2
   None|eni-0e8baa25388577d46|N/A           |10.242.26.60           |us-east-1a       |Network Interface for Transit Gateway Attachment tgw-attach-06bf47fa39e5d1578
   None|eni-0c4ed76f356525098|N/A           |10.242.26.148          |us-east-1b       |Network Interface for Transit Gateway Attachment tgw-attach-06bf47fa39e5d1578
   
### Note ignored components

1. Note that Stephen Walter said that he doesn't know why these components were created

1. Note the ignored VPC

   Tag |ID          |IPv4 CIDR Count|IPv4 CIDR  
   ----|------------|---------------|-------------
   None|vpc-85520eff|1              |172.31.0.0/16

1. Note the ignored subnets

   Tag |ID             |IPv4 CIDR     |Availability Zone
   ----|---------------|--------------|-----------------
   None|subnet-23212144|172.31.0.0/20 |us-east-1c
   None|subnet-5d2fff10|172.31.16.0/20|us-east-1a
   None|subnet-c9909d95|172.31.32.0/20|us-east-1b
   None|subnet-0214ee0c|172.31.48.0/20|us-east-1f
   None|subnet-77eeba49|172.31.64.0/20|us-east-1e
   None|subnet-fea7a8d0|172.31.80.0/20|us-east-1d

1. Note the ignored route tables

   Tag               |ID          |Main|Associated Subnet Count|Associalted Subnet #1   |Associalted Subnet #2
   ------------------|------------|----|-----------------------|------------------------|------------------------
   None              |rtb-e9d60d97|Yes |0                      |                        |

1. Note the ignored internet gateway

   Tag     |ID                   
   --------|---------------------
   ab2d-dev|igw-0fd15274

1. Note the Network ACLs

   Tag |Network ACL ID       |Associated Subnet Count
   ----|---------------------|-----------------------
   None|acl-6a9e1917         |6

1. Note the Security Groups

   Tag |Group ID            
   ----|-----------
   None|sg-778f2822

## Create an Administrators IAM group

1. Access the CMS AWS console

   *See the following appendix, if you don't know how to access the CMS AWS console:*

   [Instructions for accessing the CMS AWS console](./AB2D_Deployment_Appendices.md#appendix-a-access-the-cms-aws-console)

1. Select **IAM**

1. Select **Groups** in the leftmost panel

1. Select **Create New Group**

1. Type the following in the **Group Name** text box

   ```
   Administrators
   ```

1. Select **Next Step** on the "Set Group Name" page

1. Type the following in the **Search** text box

   ```
   AdministratorAccess
   ```

1. Check the checkbox beside **AdministratorAccess**

1. Select **Next Step** on the "Attach Policy" page

1. Select **Create Group**

## Create an AWS IAM user

1. Access the CMS AWS console

   *See the following appendix, if you don't know how to access the CMS AWS console:*

   [Instructions for accessing the CMS AWS console](./AB2D_Deployment_Appendices.md#appendix-a-access-the-cms-aws-console)
   
1. Select **IAM**

1. Select **Users** in the leftmost panel

1. Select **Add user**

1. Configure the user as follows

   - **User name:** {semanticbits email}

   - **Programmatic access:** checked

   - **AWS Management Console access:** unchecked

1. Select **Next: Permissions**

1. Check the checkbox beside **Administrators**

1. Select **Next: Tags**

1. Select **Next: Review**

1. Select **Create user**

1. Select **Download .csv**

1. Select **Close**

1. Note that the following can be found in the "credentials.csv" file that you downloaded
   
   - **User name:** {your semanticbits email}

   - **Password:** {blank because you did not set AWS console access}

   - **Access key ID:** {your access key}

   - **Secret access key:** {your secret access key}

   - **Console login link:** https://aws-hhs-cms-oeda-ab2d.signin.aws.amazon.com/console

1. Save these credentials someone safe like a personal slack channel

## Configure AWS CLI

1. Configure AWS CLI

   ```ShellSession
   $ aws configure --profile=ab2d-shared
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

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Create keypair

   1. Set the key name

      ```ShellSession
      $ export KEY_NAME=ab2d-shared
      ```

   1. Create the keypair
   
      ```ShellSession
      $ aws --region us-east-1 ec2 create-key-pair \
        --key-name ${KEY_NAME} \
        --query 'KeyMaterial' \
        --output text \
        > ~/.ssh/${KEY_NAME}.pem
      ```

1. Change permissions of the key

   ```ShellSession
   $ chmod 600 ~/.ssh/${KEY_NAME}.pem
   ```

1. Output the public key to the clipboard

   *Used for accessing controller instance(s):*

   ```ShellSession
   $ ssh-keygen -y -f ~/.ssh/${KEY_NAME}.pem | pbcopy
   ```

1. Update the "authorized_keys" file for the "ab2d-dev" environment

   1. Open the "authorized_keys" file for the environment
   
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/ab2d-dev/authorized_keys
      ```

   1. Paste the public key under the "Keys included with gold image" section
   
   1. Save and close the file

1. Update the "authorized_keys" file for the "ab2d-sbx" environment

   1. Open the "authorized_keys" file for the environment
   
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx/authorized_keys
      ```

   1. Paste the public key under the "Keys included with gold image" section
   
   1. Save and close the file
   
### Create policies

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Change to the "ab2d-shared" environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-shared
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
     --policy-name Ab2dPermissionToPassRolesPolicy \
     --policy-document file://ab2d-permission-to-pass-roles-policy.json
   ```

### Create roles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Change to the "ab2d-shared" environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-shared
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
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAssumePolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dPackerPolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dS3AccessPolicy
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dSecretsPolicy
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
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dAccessPolicy
   ```

### Create instance profiles

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
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
   $ export AWS_PROFILE=ab2d-shared
   ```
   
1. Attach the Ab2dPermissionToPassRolesPolicy to an IAM user that runs the automation

   *Example for lonnie.hanekamp@semanticbits.com:*
   
   ```ShellSession
   $ aws iam attach-user-policy \
     --policy-arn arn:aws:iam::349849222861:policy/Ab2dPermissionToPassRolesPolicy \
     --user-name lonnie.hanekamp@semanticbits.com
   ```

2. Repeat this step for all users

### Create required S3 buckets

1. Set target AWS profile
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-shared
   ```

1. Set automation bucket name

   ```ShellSession
   $ export S3_AUTOMATION_BUCKET=cms-ab2d-automation
   ```
   
1. Create S3 bucket for automation

   ```ShellSession
   $ aws --region us-east-1 s3api create-bucket \
     --bucket ${S3_AUTOMATION_BUCKET}
   ```

1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket ${S3_AUTOMATION_BUCKET} \
     --region us-east-1 \
     --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

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

1. Set the target VPC ID

   *Format:*
   
   ```ShellSession
   $ export VPC_ID={vpc id}
   ```
   
1. Create base AWS environment

   *Example for Dev environment:*

   ```ShellSession
   $ ./ab2d-deploy.sh \
     --environment=dev \
     --shared-environment=shared \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2019-12-03-15-19-01
   ```

   *Example for Sandbox environment:*

   ```ShellSession
   $ ./ab2d-deploy.sh \
     --environment=sbx \
     --shared-environment=shared \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2019-12-03-15-19-01
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

## Update application

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Deploy application components

   *Example for Dev environment testing within SemanticBits demo environment:*
   
   ```ShellSession
   $ ./deploy.sh \
     --environment=sbdemo-dev \
     --shared-environment=sbdemo-shared \
     --ssh-username=centos \
     --database-secret-datetime=2019-10-25-14-55-07 \
     --auto-approve
   ```

   *Example for Sandbox environment testing within SemanticBits demo environment:*

   ```ShellSession
   $ ./deploy.sh \
     --environment=sbdemo-sbx \
     --shared-environment=sbdemo-shared \
     --ssh-username=centos \
     --database-secret-datetime=2019-10-25-14-55-07 \
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

### Create static website in S3

1. Note that this process will create an S3 website endpoint as the origin within CloudFront

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
      $ export AWS_PROFILE=ab2d-shared
      ```

   1. Copy the website to the "cms-ab2d-website" S3 bucket

      ```ShellSession
      $ aws s3 cp --recursive _site/ s3://cms-ab2d-website/
      ```

   1. Configure static website hosting for the bucket

      ```ShellSession
      $ aws --region us-east-1 s3 website s3://cms-ab2d-website/ \
        --index-document index.html \
	--error-document 404.html
      ```

1. Note the static website endpoint

   ```
   sbdemo-ab2d-website.s3-website-us-east-1.amazonaws.com
   ```

### Create CloudFront distribution

1. Open Chrome

1. Log on to AWS

1. Navigate to CloudFront

1. Select **Create Distribution**

1. Select **Get Started** under the *Web* section

1. Note the following very important information before configuring the "Origin Settings"

   1. Note that there are two "Origin Domain Name" values that can be used for doing a distribution for an S3 website

      - S3 API Endpoint

      - S3 Website Endpoint <-- this is the method that we want to use

   1. Note that it is important that you do not select the S3 API endpoint for the website that appears in the list for the text box

   1. Note that you want to copy and paste the noted static website endpoint instead

   1. Note that the "Origin ID" will automatically fill-in based on the "Origin Domain Name", so if your settings do not match what you see in the next step that may mean that you select the API endpoint by mistake
   
1. Configure "Origin Settings" as follows:

   - **Origin Domain Name:** sbdemo-ab2d-website.s3-website-us-east-1.amazonaws.com

   - **Origin ID:** S3-Website-sbdemo-ab2d-website.s3-website-us-east-1.amazonaws.com

1. Configure "Default Cache Behavior Settings" as follows

   - **Viewer Protocol Policy:** Redirect HTTP to HTTPS

1. Configure "Distribution Settings" as follows

   > *** TO DO ***