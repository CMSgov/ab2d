# AB2D Deployment

## Table of Contents

1. [Note the starting state of the customer AWS account](#note-the-starting-state-of-the-customer-aws-account)
   * [Note dev components](#note-dev-components)
   * [Note ignored components under the dev AWS account](#note-ignored-components-under-the-dev-aws-account)
   * [Note sbx components](#note-sbx-components)
   * [Note ignored components under the sbx AWS account](#note-ignored-components-under-the-sbx-aws-account)
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
   * [Download the AB2D domain certificates and get private key from CMS](#download-the-ab2d-domain-certificates-and-get-private-key-from-cms)
   * [Import the certificate into certificate manager](#import-the-certificate-into-certificate-manager)
   * [Generate and test the website](#generate-and-test-the-website)
   * [Create an S3 bucket for the website](#create-an-s3-bucket-for-the-website)
   * [Upload website to S3](#upload-website-to-s3)
   * [Create CloudFront distribution](#create-cloudfront-distribution)
   * [Determine how to integrate cms.gov Route 53 with AB2D CloudFront distribution](#determine-how-to-integrate-cmsgov-route-53-with-ab2d-cloudfront-distribution)
   * [Submit an "Internet DNS Change Request Form" to product owner](#submit-an-internet-dns-change-request-form-to-product-owner)
1. [Configure New Relic](#configure-new-relic)
   * [Set up a New Relic free trial](#set-up-a-new-relic-free-trial)
   * [Inventory the New Relic installation included with Gold Disk](#inventory-the-new-relic-installation-included-with-gold-disk)
   * [Configure the New Relic Infrastructure agent](#configure-the-new-relic-infrastructure-agent)
   * [Request access to the project's New Relic subaccount](#request-access-to-the-projects-new-relic-subaccount)
   * [Access the project's New Relic subaccount](#access-the-projects-new-relic-subaccount)
   * [Configure a solution for using New Relic with ECS](#configure-a-solution-for-using-new-relic-with-ecs)
1. [Configure Splunk](#configure-splunk)
   * [Inventory the Splunk installation included with Gold Disk](#inventory-the-splunk-installation-included-with-gold-disk)
   * [Request that the CMS technical contact add a new job code for Splunk](#request-that-the-cms-technical-contact-add-a-new-job-code-for-splunk)
   * [Request job code for your EUA ID user](#request-job-code-for-your-eua-id-user)
   * [Create a Jira ticket to connect job code to Splunk website](#create-a-jira-ticket-to-connect-job-code-to-splunk-website)
   * [Configure Splunk forwarder](#configure-splunk-forwarder)
1. [Configure New Relic and Splunk within the deployment AMI](#configure-new-relic-and-splunk-within-the-deployment-ami)
1. [Request an Entrust certificate for sandbox.ab2d.cms.gov](#request-an-entrust-certificate-for-sandboxab2dcmsgov)
1. [Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer](#submit-an-internet-dns-change-request-form-to-product-owner-for-the-sandbox-application-load-balancer)
1. [Create an ab2d vault in 1Password](#create-an-ab2d-vault-in-1password)
1. [Add an entrust certificate to the ab2d vault in 1Password](#add-an-entrust-certificate-to-the-ab2d-vault-in-1password)
1. [Add a private key to the ab2d vault in 1Password](#add-a-private-key-to-the-ab2d-vault-in-1password)
1. [Peer AB2D Dev, Sandbox, Impl environments with the BFD Sbx VPC and peer AB2D Prod with BFD Prod VPC](#peer-ab2d-dev-sandbox-impl-environments-with-the-bfd-sbx-vpc-and-peer-ab2d-prod-with-bfd-prod-vpc)

## Note the starting state of the customer AWS account

### Note dev components

1. Note the dev VPC

   Tag     |VPC ID               |IPv4 CIDR Count|IPv4 CIDR #1  |IPv4 CIDR #2   
   --------|---------------------|---------------|--------------|---------------
   ab2d-dev|vpc-0c6413ec40c5fdac3|2              |10.242.26.0/24|10.242.5.128/26

1. Note the dev VPC attributes

   Attribute       |Value
   ----------------|-------------
   DHCP options set|dopt-90a65fea
   DNS resolution  |enable
   DNS hostnames   |disable
   
1. Note the dev subnets

   Tag               |Subnet ID               |IPv4 CIDR       |Availability Zone
   ------------------|------------------------|----------------|-----------------
   ab2d-dev-private-a|subnet-03d5b59872d950c7d|10.242.26.0/25  |us-east-1a
   ab2d-dev-private-b|subnet-0118d0d6af946bd66|10.242.26.128/25|us-east-1b
   ab2d-dev-public-a |subnet-0044ee15d254fe18b|10.242.5.128/27 |us-east-1a
   ab2d-dev-public-b |subnet-05c149659e3061ef6|10.242.5.160/27 |us-east-1b

1. Note the dev public subnet attributes

   Public Subnet    |Attribute       |Value
   -----------------|----------------|--------
   ab2d-dev-public-a|Auto-assign IPv4|disabled
   ab2d-dev-public-b|Auto-assign IPv4|disabled

1. Note the dev route tables

   Tag               |Route Table ID       |Main|Associated Subnet Count|Associalted Subnet #1   |Associalted Subnet #2
   ------------------|---------------------|----|-----------------------|------------------------|------------------------
   None              |rtb-0950b57584f20d93b|Yes |0                      |                        |
   ab2d-dev-private-a|rtb-0c55acf18e7d3cd87|No  |1                      |subnet-03d5b59872d950c7d|
   ab2d-dev-private-b|rtb-09c40213a10ea6406|No  |1                      |subnet-0118d0d6af946bd66|
   ab2d-dev-public   |rtb-090372c9ee83aa450|No  |2                      |subnet-05c149659e3061ef6|subnet-0044ee15d254fe18b

1. Note the dev routes for "ab2d-dev-private-a"

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

1. Note the dev routes for "ab2d-dev-private-b"

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

1. Note the dev routes for "ab2d-dev-public"

   Destination      |Target
   -----------------|-------------
   10.242.5.128/26  |local
   10.242.26.0/24   |local
   0.0.0.0/0        |igw-0014bff62a3c1211d

1. Note the dev internet gateway

   Tag     |ID                   
   --------|---------------------
   ab2d-dev|igw-0014bff62a3c1211d

1. Note the dev DHCP options sets

   Tag |ID           |Owner       |Options
   ----|-------------|------------|--------------------------------------------------------------------
   None|dopt-90a65fea|349849222861|domain-name = ec2.internal; domain-name-servers = AmazonProvidedDNS;
   
1. Note the dev Elastic IPs

   Tag                   |Allocation ID             |Elastic IP   |Private IP  |Network Interface Availability Zone
   ----------------------|--------------------------|-------------|------------|-----------------------------------
   ab2d-dev-nat-gateway-a|eipalloc-0048cd31435409e20|54.208.106.70|10.242.5.158|us-east-1a
   ab2d-dev-nat-gateway-b|eipalloc-0fc6dbdc2a1e412ff|3.218.184.81 |10.242.5.187|us-east-1b

1. Note the dev NAT gateways

   Tag       |NAT Gateway ID       |Elastic IP Address|Private IP  |Network Interface Availability Zone
   ----------|---------------------|------------------|------------|-----------------------------------
   ab2d-dev-a|nat-060fbd5ddb57a2f18|54.208.106.70     |10.242.5.158|us-east-1a
   ab2d-dev-b|nat-0f4d22a3e997d73c2|3.218.184.81      |10.242.5.187|us-east-1b

1. Note the dev Network ACLs

   Tag |Network ACL ID       |Associated Subnet Count
   ----|---------------------|-----------------------
   None|acl-0eb267c6c0801f8c9|4


1. Note the dev Security Groups

   Tag |Group ID            
   ----|--------------------
   None|sg-05752fb69a1a89f86

1. Note the dev Transit Gateways

   Tag |Transit Gateway ID   |Owner account ID
   ----|---------------------|---------------------
   None|tgw-080644ad8f49ecafa|921617238787 (shared)

1. Note the dev Transit Gateway Attachments

   Tag                        |Transit Gateway attachment ID|Transit Gateway owner ID|Resource owner account ID
   ---------------------------|-----------------------------|------------------------|-------------------------
   ab2d-dev-InterVPC-East-Prod|tgw-attach-06bf47fa39e5d1578 |921617238787 (shared)   |349849222861

1. Note the dev Network Interfaces

   Tag |Network interface ID |IPv4 Public IP|Primary private IPv4 IP|Availability Zone|Description
   ----|---------------------|--------------|-----------------------|-----------------|-----------------------------------------------
   None|eni-01f9447520f6efa28|54.208.106.70 |10.242.5.158           |us-east-1a       |Interface for NAT Gateway nat-060fbd5ddb57a2f18
   None|eni-045fc3c9aa02b671c|3.218.184.81  |10.242.5.187           |us-east-1b       |Interface for NAT Gateway nat-0f4d22a3e997d73c2
   None|eni-0e8baa25388577d46|N/A           |10.242.26.60           |us-east-1a       |Network Interface for Transit Gateway Attachment tgw-attach-06bf47fa39e5d1578
   None|eni-0c4ed76f356525098|N/A           |10.242.26.148          |us-east-1b       |Network Interface for Transit Gateway Attachment tgw-attach-06bf47fa39e5d1578
   
### Note ignored components

1. Note that Stephen said that he doesn't know why these components were created

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

   Tag |ID                   
   ----|---------------------
   None|igw-0fd15274

1. Note the Network ACLs

   Tag |Network ACL ID       |Associated Subnet Count
   ----|---------------------|-----------------------
   None|acl-6a9e1917         |6

1. Note the Security Groups

   Tag |Group ID            
   ----|-----------
   None|sg-778f2822

### Note sbx components

1. Note the sbx VPC

   Tag             |VPC ID               |IPv4 CIDR Count|IPv4 CIDR #1  |IPv4 CIDR #2   
   ----------------|---------------------|---------------|--------------|---------------
   ab2d-sbx-sandbox|vpc-08dbf3fa96684151c|2              |10.242.31.0/24|10.242.36.0/26

1. Note the sbx VPC attributes

   Attribute       |Value
   ----------------|-------------
   DHCP options set|dopt-c105c4bb
   DNS resolution  |enable
   DNS hostnames   |disable
   
1. Note the sbx subnets

   Tag                       |Subnet ID               |IPv4 CIDR       |Availability Zone
   --------------------------|------------------------|----------------|-----------------
   ab2d-sbx-sandbox-private-a|subnet-0002caeb5751aec0d|10.242.31.0/25  |us-east-1a
   ab2d-sbx-sandbox-private-b|subnet-01dca7e0c3f93f55a|10.242.31.128/25|us-east-1b
   ab2d-sbx-sandbox-public-a |subnet-0e9f505bc63649211|10.242.36.0/27  |us-east-1a
   ab2d-sbx-sandbox-public-b |subnet-0c0b9e44faab73ba3|10.242.36.32/27 |us-east-1b

1. Note the sbx public subnet attributes

   Public Subnet    |Attribute       |Value
   -----------------|----------------|--------
   ab2d-dev-public-a|Auto-assign IPv4|disabled
   ab2d-dev-public-b|Auto-assign IPv4|disabled

1. Note the sbx route tables

   Tag                       |Route Table ID       |Main|Associated Subnet Count|Associalted Subnet #1   |Associalted Subnet #2
   --------------------------|---------------------|----|-----------------------|------------------------|------------------------
   None                      |rtb-0d09f616686be1dfb|Yes |0                      |                        |
   ab2d-sbx-sandbox-private-a|rtb-0b182cb712cb4481b|No  |1                      |subnet-0002caeb5751aec0d|
   ab2d-sbx-sandbox-private-b|rtb-0c1733bdc06688f19|No  |1                      |subnet-01dca7e0c3f93f55a|
   ab2d-sbx-sandbox-public   |rtb-0b9e43e78ed265975|No  |2                      |subnet-0c0b9e44faab73ba3|subnet-0e9f505bc63649211

1. Note the sbx routes for "ab2d-dev-private-a"

   Destination      |Target
   -----------------|-------------
   10.242.36.0/26   |local
   10.242.31.0/24   |local
   0.0.0.0/0        |nat-0b7abeab3b574cc67
   10.223.120.0/22  |tgw-080644ad8f49ecafa
   10.223.126.0/23  |tgw-080644ad8f49ecafa
   10.232.32.0/19   |tgw-080644ad8f49ecafa
   10.242.7.192/26  |tgw-080644ad8f49ecafa
   10.242.193.192/26|tgw-080644ad8f49ecafa
   10.244.96.0/19   |tgw-080644ad8f49ecafa

1. Note the sbx routes for "ab2d-dev-private-b"

   Destination      |Target
   -----------------|-------------
   10.242.36.0/26   |local
   10.242.31.0/24   |local
   0.0.0.0/0        |nat-055a7d8b4b86987b3
   10.223.120.0/22  |tgw-080644ad8f49ecafa
   10.223.126.0/23  |tgw-080644ad8f49ecafa
   10.232.32.0/19   |tgw-080644ad8f49ecafa
   10.242.7.192/26  |tgw-080644ad8f49ecafa
   10.242.193.192/26|tgw-080644ad8f49ecafa
   10.244.96.0/19   |tgw-080644ad8f49ecafa

1. Note the sbx routes for "ab2d-dev-public"

   Destination      |Target
   -----------------|-------------
   10.242.36.0/26   |local
   10.242.31.0/24   |local
   0.0.0.0/0        |igw-0cb6e5264d08610a0

1. Note the sbx internet gateway

   Tag             |ID                   
   ----------------|---------------------
   ab2d-sbx-sandbox|igw-0cb6e5264d08610a0

1. Note the sbx DHCP options sets

   Tag |ID           |Owner       |Options
   ----|-------------|------------|--------------------------------------------------------------------
   None|dopt-c105c4bb|777200079629|domain-name = ec2.internal; domain-name-servers = AmazonProvidedDNS;

1. Note the sbx Elastic IPs

   Tag                           |Allocation ID             |Elastic IP  |Private IP  |Network Interface Availability Zone
   ------------------------------|--------------------------|------------|------------|-----------------------------------
   ab2d-sbx-sandbox-nat-gateway-a|eipalloc-07133bdf5781c46cd|52.23.57.49 |10.242.36.30|us-east-1a
   ab2d-sbx-sandbox-nat-gateway-b|eipalloc-0688fab939ab8fcd7|52.0.216.183|10.242.36.38|us-east-1b

1. Note the sbx NAT gateways

   Tag               |NAT Gateway ID       |Elastic IP Address|Private IP  |Network Interface Availability Zone
   ------------------|---------------------|------------------|------------|-----------------------------------
   ab2d-sbx-sandbox-a|nat-0b7abeab3b574cc67|52.23.57.49       |10.242.36.30|us-east-1a
   ab2d-sbx-sandbox-b|nat-055a7d8b4b86987b3|52.0.216.183      |10.242.36.38|us-east-1b

1. Note the sbx Network ACLs

   Tag |Network ACL ID       |Associated Subnet Count
   ----|---------------------|-----------------------
   None|acl-002d000b5cf05cc55|4


1. Note the sbx Security Groups

   Tag       |Group ID            |Group Name
   ----------|--------------------|----------
   None      |sg-03bfe80b30b7b302b|default
   VPN access|sg-08448aec3f4c1bf3d|VPN access

1. Note the sbx Transit Gateways

   Tag |Transit Gateway ID   |Owner account ID
   ----|---------------------|---------------------
   None|tgw-080644ad8f49ecafa|921617238787 (shared)

1. Note the sbx Transit Gateway Attachments

   Tag                                |Transit Gateway attachment ID|Transit Gateway owner ID|Resource owner account ID
   -----------------------------------|-----------------------------|------------------------|-------------------------
   ab2d-sbx-sandbox-InterVPC-East-Prod|tgw-attach-0878b16d2bf55e7ad |921617238787 (shared)   |777200079629

1. Note the sbx Network Interfaces

   Tag |Network interface ID |IPv4 Public IP|Primary private IPv4 IP|Availability Zone|Description
   ----|---------------------|--------------|-----------------------|-----------------|-----------------------------------------------
   None|eni-01f9447520f6efa28|52.23.57.49   |10.242.36.30           |us-east-1a       |Interface for NAT Gateway nat-0b7abeab3b574cc67
   None|eni-045fc3c9aa02b671c|52.0.216.183  |10.242.36.38           |us-east-1b       |Interface for NAT Gateway nat-055a7d8b4b86987b3
   None|eni-0e8baa25388577d46|N/A           |10.242.31.78           |us-east-1a       |Network Interface for Transit Gateway Attachment tgw-attach-0878b16d2bf55e7ad
   None|eni-0c4ed76f356525098|N/A           |10.242.31.205          |us-east-1b       |Network Interface for Transit Gateway Attachment tgw-attach-0878b16d2bf55e7ad

### Note ignored components under the sbx AWS account

1. Note that Stephen said that he doesn't know why these components were created

1. Note the ignored VPC

   Tag |ID          |IPv4 CIDR Count|IPv4 CIDR  
   ----|------------|---------------|-------------
   None|vpc-daceeaa0|1              |172.31.0.0/16

1. Note the ignored subnets

   Tag |ID             |IPv4 CIDR     |Availability Zone
   ----|---------------|--------------|-----------------
   None|subnet-b8fef6df|172.31.0.0/20 |us-east-1b
   None|subnet-af865de2|172.31.16.0/20|us-east-1d
   None|subnet-5a7b6f06|172.31.32.0/20|us-east-1a
   None|subnet-be4d1280|172.31.48.0/20|us-east-1e
   None|subnet-0489440a|172.31.64.0/20|us-east-1f
   None|subnet-59180e77|172.31.80.0/20|us-east-1c

1. Note the ignored route tables

   Tag               |ID          |Main|Associated Subnet Count|Associalted Subnet #1   |Associalted Subnet #2
   ------------------|------------|----|-----------------------|------------------------|------------------------
   None              |rtb-5647e428|Yes |0                      |                        |

1. Note the ignored internet gateway

   Tag |ID                   
   ----|---------------------
   None|igw-39a82342

1. Note the Network ACLs

   Tag |Network ACL ID       |Associated Subnet Count
   ----|---------------------|-----------------------
   None|acl-5a7df227         |6

1. Note the Security Groups

   Tag |Group ID            
   ----|-----------
   None|sg-dcadb488

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

   - **AWS Management Console access:** checked

   - **Console password:** {desired password}

   - **Require password reset:** {unchecked}

1. Select **Next: Permissions**

1. Check the checkbox beside **Administrators**

1. Select **Next: Tags**

1. Select **Next: Review**

1. Select **Create user**

1. Select **Download .csv**

1. Select **Close**

1. Note that the following can be found in the "credentials.csv" file that you downloaded

   *Format:*
   
   - **User name:** {your semanticbits email}

   - **Password:** {blank because you did custom password}

   - **Access key ID:** {your access key}

   - **Secret access key:** {your secret access key}

   - **Console login link:** https://{aws account number}.signin.aws.amazon.com/console

1. Save these credentials someone safe like a personal slack channel

1. Note that if you want AWS console access with your IAM user, you will need to enable multi-factor authentication (MFA)

1. Note that there are three ways to enable MFA

   - Virtual MFA device (Authenticator app installed on your mobile device or computer)

   - U2F security key (YubiKey or any other compliant U2F device)

   - Other hardware MFA device (Gemalto token)

1. If you can't do any of those three options, disable console access for your user and access the AWS Console via CloudTamer instead

1. If you have a SemanticBits YubiKey, set up MFA

   1. Select your newly created user from the IAM Users list

   1. Select the **Security credentials** tab

   1. Select **Manage** beside "Assigned MFA device"

   1. Select **U2F security key**

   1. Select **Continue** on the "Choose the type of MFA device to assign" page

   1. When prompted, tap your YubiKey with your fingers

   1. If a "console.aws.amazon.com wants to 'See the make and model of your Security Key'" prompt appears within Chrome, select **Allow**

   1. Select **Close** on the "Set up U2F security key" page

   1. Note the following now appears beside "Assigned MFA device":

      *Format:*
      
      ```
      arn:aws:iam::{aws account number}:u2f/user/{iam user}/{u2f security key} (U2F security key) 
      ```

1. Verify MFA login

   1.Sign out of the current AWS console session

   1. Enter the following in the address bar

      *Format:*
      
      > https://{aws account number or alias}.signin.aws.amazon.com/console

   1. Enter the following on the console logon page

      *Format:*
      
      - **Account ID or alias:** {aws account number}

      - **IAM user name:** {your semanticbits email}

      - **Password:** {your password}

   1. When prompted, touch the SemanticBits YubiKey to complete the logon process

   1. Verify that you were able to successfully log on to the AWS console
   
## Configure AWS CLI

1. Configure AWS CLI

   *Format:*

   ```ShellSession
   $ aws configure --profile={vpc tag}
   ```

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ aws configure --profile=ab2d-dev
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ aws configure --profile=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ aws configure --profile=ab2d-east-impl
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

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
   ```

1. Create keypair

   1. Set the key name

      *Example for "Dev" environment:*
      
      ```ShellSession
      $ export KEY_NAME=ab2d-dev
      ```

      *Example for "Sbx" environment:*

      ```ShellSession
      $ export KEY_NAME=ab2d-sbx-sandbox
      ```

      *Example for "Impl" environment:*

      ```ShellSession
      $ export KEY_NAME=ab2d-east-impl
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

1. Update the "authorized_keys" file for the environment

   1. Open a second terminal
   
   1. Open the "authorized_keys" file for the environment

      *Example for "Dev" environment:*
      
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/ab2d-dev-shared/authorized_keys
      ```

      *Example for "Sbx" environment:*

      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox-shared/authorized_keys
      ```

      *Example for "Impl" environment:*

      ```ShellSession
      $ vim ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl-shared/authorized_keys
      ```

   1. Return to the first terminal
   
   1. Output the public key to the clipboard

      *Used for accessing controller instance(s):*

      ```ShellSession
      $ ssh-keygen -y -f ~/.ssh/${KEY_NAME}.pem | pbcopy
      ```

   1. Return to the second terminal
   
   1. Paste the public key under the "Keys included with gold image" section
   
   1. Save and close the file

### Create policies

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
   ```

1. Change to the shared environment directory

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-dev-shared
   ```
   
   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox-shared
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl-shared
   ```

1. Create "Ab2dAccessPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dAccessPolicy \
     --policy-document file://ab2d-access-policy.json
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

1. Change to the environment-specific directory

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-dev
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
   ```

1. Create "Ab2dAssumePolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dAssumePolicy \
     --policy-document file://ab2d-assume-policy.json
   ```

1. Create "Ab2dPermissionToPassRolesPolicy"

   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dPermissionToPassRolesPolicy \
     --policy-document file://ab2d-permission-to-pass-roles-policy.json
   ```

1. Create "Ab2dSecretsPolicy"

   > *** TO DO ***: Create process to implement this
   
   ```ShellSession
   $ aws iam create-policy \
     --policy-name Ab2dSecretsPolicy \
     --policy-document file://ab2d-secrets-policy.json
   ```

### Create roles

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
   ```

1. Change to the shared environment directory

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-dev-shared
   ```
   
   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox-shared
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl-shared
   ```

1. Create "Ab2dInstanceRole" role

   ```ShelSession
   $ aws iam create-role \
     --role-name Ab2dInstanceRole \
     --assume-role-policy-document file://ab2d-instance-role-trust-relationship.json
   ```

1. Set AWS account number

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_ACCOUNT_NUMBER=349849222861
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_ACCOUNT_NUMBER=777200079629
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_ACCOUNT_NUMBER=330810004472
   ```

1. Attach required policies to the "Ab2dInstanceRole" role

   ```ShellSession
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn "arn:aws:iam::${AWS_ACCOUNT_NUMBER}:policy/Ab2dAssumePolicy"
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn "arn:aws:iam::${AWS_ACCOUNT_NUMBER}:policy/Ab2dPackerPolicy"
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn "arn:aws:iam::${AWS_ACCOUNT_NUMBER}:policy/Ab2dS3AccessPolicy"
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
   ```

1. Attach secrets policy to the "Ab2dInstanceRole" role

   > *** TO DO ***: Create process to implement this
   
   ```ShellSession
   $ aws iam attach-role-policy \
     --role-name Ab2dInstanceRole \
     --policy-arn "arn:aws:iam::${AWS_ACCOUNT_NUMBER}:policy/Ab2dSecretsPolicy"
   ```

1. Change to the environment-specific directory

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-dev
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/ab2d-east-impl
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
     --policy-arn arn:aws:iam::${AWS_ACCOUNT_NUMBER}:policy/Ab2dAccessPolicy
   ```

### Create instance profiles

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
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

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
   ```

1. Set AWS account number

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_ACCOUNT_NUMBER=349849222861
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_ACCOUNT_NUMBER=777200079629
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_ACCOUNT_NUMBER=330810004472
   ```

1. Attach the Ab2dPermissionToPassRolesPolicy to an IAM user that runs the automation

   *Example for lonnie.hanekamp@semanticbits.com:*
   
   ```ShellSession
   $ aws iam attach-user-policy \
     --policy-arn arn:aws:iam::${AWS_ACCOUNT_NUMBER}:policy/Ab2dPermissionToPassRolesPolicy \
     --user-name lonnie.hanekamp@semanticbits.com
   ```

2. Repeat this step for all users

### Create required S3 buckets

1. Set target AWS profile

   *Example for "Dev" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

   *Example for "Sbx" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-sbx-sandbox
   ```

   *Example for "Impl" environment:*

   ```ShellSession
   $ export AWS_PROFILE=ab2d-east-impl
   ```

1. Set automation bucket name

   *Example for "Dev" environment:*
   
   ```ShellSession
   $ export S3_AUTOMATION_BUCKET=ab2d-dev-automation
   ```

   *Example for "Sbx" environment:*
   
   ```ShellSession
   $ export S3_AUTOMATION_BUCKET=ab2d-sbx-sandbox-automation
   ```

   *Example for "Impl" environment:*
   
   ```ShellSession
   $ export S3_AUTOMATION_BUCKET=ab2d-east-impl-automation
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
   
1. If creating the AWS environment for Dev, do one of the following

   *Deploy Dev by creating new api and worker images:*

   ```ShellSession
   $ ./deploy-ab2d-to-cms.sh \
     --environment=ab2d-dev \
     --shared-environment=ab2d-dev-shared \
     --ecr-repo-environment=ab2d-dev \
     --region=us-east-1 \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --auto-approve
   ```

   *Deploy Dev by using the latest existing api and worker images:*

   ```ShellSession
   $ ./deploy-ab2d-to-cms.sh \
     --environment=ab2d-dev \
     --shared-environment=ab2d-dev-shared \
     --ecr-repo-environment=ab2d-dev \
     --region=us-east-1 \
     --vpc-id=vpc-0c6413ec40c5fdac3 \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --use-existing-images \
     --auto-approve
   ```

1. If creating the AWS environment for Sandbox, do one of the following

   *Deploy Sandbox by creating new api and worker images:*

   ```ShellSession
   $ ./deploy-ab2d-to-cms.sh \
     --environment=ab2d-sbx-sandbox \
     --shared-environment=ab2d-sbx-sandbox-shared \
     --ecr-repo-environment=ab2d-dev \
     --region=us-east-1 \
     --vpc-id=vpc-08dbf3fa96684151c \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2020-01-02-09-15-01 \
     --build-new-images \
     --auto-approve
   ```

   *Deploy Sandbox by using the latest existing api and worker images:*

   ```ShellSession
   $ ./deploy-ab2d-to-cms.sh \
     --environment=ab2d-sbx-sandbox \
     --shared-environment=ab2d-sbx-sandbox-shared \
     --ecr-repo-environment=ab2d-dev \
     --region=us-east-1 \
     --vpc-id=vpc-08dbf3fa96684151c \
     --ssh-username=ec2-user \
     --owner=842420567215 \
     --ec2-instance-type=m5.xlarge \
     --database-secret-datetime=2020-01-02-09-15-01 \
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

### Download the AB2D domain certificates and get private key from CMS

1. Note that CMS will request a domain certificate from Entrust for the following domain

   ```
   ab2d.cms.gov
   ```

1. Wait for CMS to provide the following

   - private key used to make the domain certificate request

   - forwarded email from Entrust to download the certificate

1. Select the link under "Use the following URL to pick up and install your certificate" in the forwarded Entrust email

1. Select "Other" from the "...server type" dropdown

1. Select **Next** on the "Select Server Type" page

1. Select **Download Certificates**

1. Wait for the download to complete

1. Note the following file has been downloaded

   ```
   entrust.zip
   ```

1. Open a terminal

1. Change to the downloads directory

   ```ShellSession
   $ cd ~/Downloads
   ```
   
1. Unzip "entrust.zip"

   ```ShellSession
   $ 7z x entrust.zip
   ```

### Import the certificate into certificate manager

1. Open Chrome

1. Log on to AWS

1. Navigate to Certificate Manager

1. Select **Get Started** under "Provision certificates"

1. Select **Import a certificate**

1. Open a terminal

1. Copy the contents of "ServerCertificate.crt" to the clipboard

   ```ShellSession
   $ cat ~/Downloads/ServerCertificate.crt | pbcopy
   ```

1. Return to the "Import a Certificate" page in Chrome

1. Paste the contents of the "ServerCertificate.crt" into the **Certificate body** text box

1. Copy the contents of the private key to the clipboard

   ```ShellSession
   $ cat ~/Downloads/ab2d_cms_gov.key | pbcopy
   ```
   
1. Paste the contents of the the private key that was provided separately by CMS into the **Certificate private key** text box

1. Return to the terminal

1. Copy the certificate key chain (Intermediate.crt + Root.crt) to the clipboard

   ```ShellSession
   $ echo -ne "$(cat ~/Downloads/Intermediate.crt)\n$(cat ~/Downloads/Root.crt)" | pbcopy
   ```

1. Paste the combined intermediate and root certificates into the **Certificate chain** text box

1. Select **Next** on the "Import certificate" page

1. Select **Review and import**

1. Note that the following information should be displayed

   *Format:*

   **Domains:** ab2d.cms.gov

   **Expires in:** {number} Days

   **Public key info:** RSA-2048

   **Signature algorithm:** SHA256WITHRSA
   
1. Select **Import**

### Generate and test the website

1. Change to the "website" directory

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

1. Generate and test the website

   1. Ensure required gems are installed

      ```ShellSession
      $ bundle install
      ```

   1. Generate and serve website on the jekyll server

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
    
   1. Note the following two files that will be used in CloudFront distribution configuration

      - index.html

      - 404.html

### Create an S3 bucket for the website

1. Set the target AWS profile

   *Format:*
   
   ```ShellSession
   $ export AWS_PROFILE={target aws profile}
   ```

   *Example for CMS:*
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

1. Create S3 bucket for the website

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api create-bucket \
     --bucket {unique id}-ab2d-website
   ```

   *Example for CMS:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api create-bucket \
     --bucket cms-ab2d-website
   ```

1. Block public access on the bucket

   *Format:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api put-public-access-block \
      --bucket {unique id}-ab2d-website \
      --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

   *Example for CMS:*
   
   ```ShellSession
   $ aws --region us-east-1 s3api put-public-access-block \
      --bucket cms-ab2d-website \
      --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
   ```

### Upload website to S3

1. Note that the uploaded website will be used to create an S3 API endpoint as the origin within CloudFront

1. Change to the "website" directory

   ```ShellSession
   $ cd ~/code/ab2d/website
   ```

1. Set the target AWS profile

   *Format:*
   
   ```ShellSession
   $ export AWS_PROFILE={target aws profile}
   ```

   *Example for CMS:*
   
   ```ShellSession
   $ export AWS_PROFILE=ab2d-dev
   ```

1. Upload website to S3

   *Format:*
   
   ```ShellSession
   $ aws s3 cp --recursive _site/ s3://{unique id}-ab2d-website/
   ```

   *Example for CMS:*
   
   ```ShellSession
   $ aws s3 cp --recursive _site/ s3://cms-ab2d-website/
   ```

### Create CloudFront distribution

1. Open Chrome

1. Log on to AWS

1. Navigate to CloudFront

1. Select **Create Distribution**

1. Select **Get Started** under the *Web* section

1. Note the following very important information before configuring the "Origin Settings"

   1. Note that there are two "Origin Domain Name" values that can be used for doing a distribution for an S3 website

      - S3 API Endpoint <-- this is the method that we want to use

      - S3 Website Endpoint

   1. Note that the "Origin ID" will automatically fill-in based on the "Origin Domain Name"
   
1. Configure "Origin Settings" as follows:

   *Format:*
   
   - **Origin Domain Name:** {unique id}-ab2d-website.s3.amazonaws.com

   - **Origin ID:** S3-{unique id}-ab2d-website

   - **Restrict Bucket Access:** Yes

   - **Origin Access Identity:** Create a New Identity

   - **Comment:** access-identity-{unique id}-ab2d-website.s3.amazonaws.com

   - **Grant Read Permissions on Bucket:** Yes, Update Bucket Policy

   *Example for semanticbitsdemo:*
   
   - **Origin Domain Name:** cms-ab2d-website.s3.amazonaws.com
   
   - **Origin ID:** S3-cms-ab2d-website

   - **Restrict Bucket Access:** Yes

   - **Origin Access Identity:** Create a New Identity

   - **Comment:** access-identity-cms-ab2d-website.s3.amazonaws.com

   - **Grant Read Permissions on Bucket:** Yes, Update Bucket Policy

1. Configure "Default Cache Behavior Settings" as follows

   - **Viewer Protocol Policy:** Redirect HTTP to HTTPS

1. Configure "Distribution Settings" as follows
   
   **Alternate Domain Names (CNAMES):** ab2d.cms.gov

   **SSL Certificate:** Custom SSL Certificate

   **Custom SSL Certificate:** ab2d.cms.gov

   **Default Root Object:** index.html

1. Select **Create Distribution**

1. Select **Distributions** in the leftmost panel

1. Note the distribution row that was created

   *Format:*
   
   - **Delivery Method:** Web

   - **Domain Name:** {unique id}.cloudfront.net

   - **Origin:** cms-ab2d-website.s3.amazonaws.com

   - **CNAMEs:** ab2d.cms.gov

1. Note that it will take about 30 minutes for the CloudFront distribution to complete

1. Wait for the "Status" to change to the following

   ```
   Deployed
   ```

### Submit an "Internet DNS Change Request Form" to product owner

1. Open Chrome

1. Enter the following in the address bar

   > https://confluence.cms.gov/pages/viewpage.action?pageId=138595233

1. If the Confluence logon page appears, log on to Confluence

1. Note that the "CNAME/DNS Change Requests" page should be displayed

1. Select the **DNS change request form** link under the "Process" section

1. Select the **Download** icon in the top right of the page

1. Wait for the download to complete

1. Open the downloaded form

   ```
   Internet DNS Change Request (2).pdf
   ```

1. Note that Lonnie Hanekamp has product owner and business owner information saved in his "stakeholders" private slack channel

1. Fill out the form as follows

   *Requestor Information:*

   - **Name:** {product owner first name} {product owner last name}

   - **Organization:** {product owner organization}

   - **Email:** {product owner email}

   - **Phone:** {product owner phone}

   *CMS Business Owner Information*

   - **Name:** {business owner first name} {business owner last name}

   - **Organization:** {business owner organization}

   - **Email:** {business owner email}

   - **Phone:** {business owner phone}

   - **Reason:** To support data sharing with PDP sponsor.  This static webpage will provide information for the PDP sponsors.

   *DNS Change Information*

   - **DNS Zone:** cms.gov

   - **Type of change:** CNAME

   - **Actual Change:** ab2d.cms.gov CNAME {unique id}.cloudfront.net

   - **Change Date & Time:** ASAP

   - **Purpose of the change:** Initial launch of new informational page for AB2D API

1. Submit the completed for to the product owner

1. Note that the product owner will complete the process

## Configure New Relic

### Set up a New Relic free trial

1. Open Chrome

1. Enter the following in the address bar

   > https://newrelic.com

1. Select **Sign Up**

1. Enter the following information

   - **First Name:** {your first name}

   - **Last Name:** {your last name}

   - **Your Business Email Address:** {your semanticbits email}

   - **Retype Your Business Email Address:** {your semanticbits email}

   - **Your Phone Number:** {your phone number}

   - **Select Your Country:** {your country}

   - **Select Your State:** {your state (if displayed)}

   - **What's Your Postal Code:** {your postal code (if displayed)}

   - **Where do you want your data house:** {United States|European Union}

   - **Your company:** SemanticBits

   - **Your role:** {your role}

   - **Number of Employees:** 101-1,000

   - **Number of App Servers:** 3-10 Servers

   - **I'm not a robot:** checked

   - **I agree to the Terms and Service:** checked

1. Select **Sign Up for New Relic**

1. Wait for email from New Relic

1. Select **Verify your email** in the email from New Relic

1. Set your password

   - **Password:** {your password}

   - **Password confirmation:** {your password}

1. Select **Update**

1. Log into New Relic

   - **Email:** {your semanticbits email}

   - **Password:** {your password}

1. Select **Sign in**

1. Select **New Relic Infrastructure**

1. Select **Start my 30 day free trial**

1. Note that you have a New Relic subaccount for your trial

   *Example:*

   ```
   SemanticBits_16
   ```
   
1. Note that you will receive access to a New Relic subaccount for your project that you can switch to later

### Inventory the New Relic installation included with Gold Disk

1. Inventory New Relic files

   *New Relic configuration file*
   
   ```
   /etc/newrelic-infra.yml <--- need to configure license
   ```
   
   *New Relic infrastrucure agent*
   
   ```
   /etc/systemd/system/newrelic-infra.service
   /etc/systemd/system/multi-user.target.wants/newrelic-infra.service
   /usr/bin/newrelic-infra
   /usr/bin/newrelic-infra-ctl
   /usr/bin/newrelic-infra-service
   ```
   
   *New Relic user*
   
   ```
   /home/newrelic <--- empty directory
   /var/spool/mail/newrelic
   ```
   
   *New Relic opt directory*
   
   ```
   /opt/newrelic <--- empty directory
   ```
   
   *New Relic log directory*
   
   ```
   /var/log/newrelic-infra <--- empty directory
   ```
   
   *New Relic integrations*
   
   ```
   /etc/newrelic-infra/integrations.d
   /var/db/newrelic-infra/LICENSE.txt
   /var/db/newrelic-infra/custom-integrations
   /var/db/newrelic-infra/integrations.d
   /var/db/newrelic-infra/newrelic-integrations
   ```
   
   *New Relic yum repo, cache, and gpg*
   
   ```
   /etc/yum.repos.d/newrelic-infra.repo
   /var/cache/yum/x86_64/7Server/newrelic-infra
   /var/cache/yum/x86_64/7Server/newrelic-infra/cachecookie
   /var/cache/yum/x86_64/7Server/newrelic-infra/gen
   /var/cache/yum/x86_64/7Server/newrelic-infra/packages
   /var/cache/yum/x86_64/7Server/newrelic-infra/primary.sqlite.bz2
   /var/cache/yum/x86_64/7Server/newrelic-infra/repomd.xml
   /var/cache/yum/x86_64/7Server/newrelic-infra/repomd.xml.asc
   /var/cache/yum/x86_64/7Server/newrelic-infra/gen/filelists_db.sqlite
   /var/cache/yum/x86_64/7Server/newrelic-infra/gen/other_db.sqlite
   /var/cache/yum/x86_64/7Server/newrelic-infra/gen/primary_db.sqlite
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir-ro
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir/gpg.conf
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir/pubring.gpg
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir/pubring.gpg~
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir/secring.gpg
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir/trustdb.gpg
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir-ro/gpg.conf
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir-ro/pubring.gpg
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir-ro/pubring.gpg~
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir-ro/secring.gpg
   /var/lib/yum/repos/x86_64/7Server/newrelic-infra/gpgdir-ro/trustdb.gpg
   ```

1. Note the initial state of New Relic

   1. Check the status of the New Relic infrastructure agent

      ```ShellSession
      $ sudo systemctl status newrelic-infra
      ```

   1. Note the output

      ```
      newrelic-infra.service - New Relic Infrastructure Agent
       Loaded: loaded (/etc/systemd/system/newrelic-infra.service; enabled; vendor preset: disabled)
       Active: activating (auto-restart) (Result: exit-code) since Wed 2019-12-11 17:48:28 EST; 3s ago
       Process: 11823 ExecStart=/usr/bin/newrelic-infra-service (code=exited, status=2)
      Main PID: 11823 (code=exited, status=2)
      ```

   1. Note the following from the system log "/var/log/messages"

      ```
      Dec 11 17:53:52 ip-10-242-26-192 newrelic-infra-service: time="2019-12-11T17:53:52-05:00" level=error
      msg="can't load configuration file" component="New Relic Infrastructure Agent" error="no license key,
      please add it to agent's config file or NRIA_LICENSE_KEY environment variable"
      ```
   
   1. Note that New Relic keeps stopping and starting because there is no license key configured
      
### Configure the New Relic Infrastructure agent

1. Check the status of the New Relic infrastructure agent

   ```ShellSession
   $ sudo systemctl status newrelic-infra
   ```

1. Stop the New Relic infrastructure agent

   ```ShellSession
   $ sudo systemctl stop newrelic-infra
   ```

1. Start the New Relic infrastructure agent

   ```ShellSession
   $ sudo systemctl start newrelic-infra
   ```

1. Restart the New Relic infrastructure agent

   ```ShellSession
   $ sudo systemctl restart newrelic-infra
   ```

### Request access to the project's New Relic subaccount

1. Open Chrome

1. Enter the following in the address bar

   > https://jira.cms.gov/servicedesk/customer/portal/1/create/355

1. Complete the form as follows

   *Format:*
   
   - **Summary:** New Relic Access

   - **Organization:** CMS

   - **Phone Number:** {your mobile phone number}

   - **OC Tool:** New Relic

   - **Description:**

     ```
     I need access to New Relic. I am a DevOps Engineer working on the AB2D project which falls under BCDA.
     ```

1. Wait for access to be provided

### Access the project's New Relic subaccount

1. Open Chrome

1. Enter the following in the address bar

   > http://newrelic.com

1. Select **Log In**

1. Log in to New Relic

   - **Email:** {your semanticbits email}

   - **Password:** {your password for New Relic}

1. Select the subaccount dropdown in the upper right of the page

1. Select **Switch account**

1. Select your project's account

   *Example:*

   ```
   AWS BlueButton
   ```

### Configure a solution for using New Relic with ECS

> *** TO DO ***

*See the following:*

> https://confluence.cms.gov/display/BEDAP/New+Relic

## Configure Splunk

### Inventory the Splunk installation included with Gold Disk

1. Inventory Splunk files

   *Splunk indexer service:*

   ```
   /etc/rc.d/init.d/splunk
   /etc/rc.d/rc0.d/K60splunk
   /etc/rc.d/rc1.d/K60splunk
   /etc/rc.d/rc2.d/S90splunk
   /etc/rc.d/rc3.d/S90splunk
   /etc/rc.d/rc4.d/S90splunk
   /etc/rc.d/rc5.d/S90splunk
   /etc/rc.d/rc6.d/K60splunk
   ```

   *Splunk Forwarder:*
   
   ```
   /opt/splunkforwarder/*
   /opt/splunkforwarder/README-splunk.txt
   /opt/splunkforwarder/bin/*
   /opt/splunkforwarder/etc/*
   /opt/splunkforwarder/etc/system/local/README
   /opt/splunkforwarder/etc/system/local/inputs.conf
   /opt/splunkforwarder/etc/system/local/server.conf
   /opt/splunkforwarder/etc/apps/ccs_all_deploymentclients/local/deploymentclient.conf
   /opt/splunkforwarder/include/*
   /opt/splunkforwarder/lib/*
   /opt/splunkforwarder/openssl/*
   /opt/splunkforwarder/share/*
   /opt/splunkforwarder/var/*
   ```
   
1. Note the initial state of New Relic

   1. Check the status of the New Relic infrastructure agent

      ```ShellSession
      $ systemctl status splunk
      ```

   1. Note the output

      ```
      splunk.service - SYSV: Splunk indexer service
       Loaded: loaded (/etc/rc.d/init.d/splunk; bad; vendor preset: disabled)
       Active: active (running) since Tue 2019-12-03 18:52:02 EST; 1 weeks 1 days ago
         Docs: man:systemd-sysv-generator(8)
       Memory: 166.6M
       CGroup: /system.slice/splunk.service
               ├─2547 splunkd -p 8089 start
               └─2549 [splunkd pid=2547] splunkd -p 8089 start [process-runner]

      Warning: Journal has been rotated since unit was started. Log output is incomplete or unavailable.
      ```

### Request that the CMS technical contact add a new job code for Splunk

1. Ask a CMS technical contact to add a new job for Splunk

1. Note that the new job code will like look like this

   *Format:*
   
   ```
   AWS_{project}_SPLUNK
   ```

   *Example:*
   
   ```
   AWS_AB2D_SPLUNK
   ```

### Request job code for your EUA ID user

1. Open Chrome

1. Log on to the EUA site

   > https://eua.cms.gov/iam/im/pri/

1. Expand **Home** in the leftmost panel

1. Select **Modify My Job Codes**

1. Select **Add a Job Code**

1. Select the following in the **Search for a group** section

   *Example searching for job codes with "SPLUNK" in the name:*
   
   ```
   where  Job Code  = AWS_AB2D_SPLUNK
   ```

1. Select **Search**

1. Check the checkbox beside the following

   ```
   AWS_AB2D_SPLUNK
   ```

1. Select **Select**

1. Select **Next** on the "My Job Codes" page

1. Type the following in the **Justification Reason** checkbox

   ```
   I am a DevOps engineer working on the AB2D project.
   ```

1. Select the "No" radio button beside **Is Job Code Temporary**

1. Select **Finish**

1. Note that two users should request and be approved for to the Splunk Job code before proceeding to the next section

### Create a Jira ticket to connect job code to Splunk website

1. Open Chrome

1. Enter the following in the address bar

   > https://jira.cms.gov/projects/CMSAWSOPS/issues/CMSAWSOPS-49590?filter=allopenissues

1. Select **Create**

1. Fill out the form as follows

   **Issue Type:** Task

   **Summary:** Request "AWS_AB2D_SPLUNK" job code to be plumbed to Splunk website

   **Project Name:** Project 058 BCDA

   **Account Alias:** None

   **Service Category:** Access Management

   **Task:** None

   **Severity:** Minimal

   **Urgency:** Medium

   **Description:**

   ```
   We need the new "AWS_AB2D_SPLUNK" job code to be plumbed to the Splunk website, so that anyone that that has been approved for the "AWS_AB2D_SPLUNK" job code can access the Splunk website:

   https://splunk.aws.healthcare.gov/en-US

   This is for the AB2D project that falls under the BCDA project.

   Thanks for your consideration.
   ```

   **Reported Source:** Self Service

1. Select **Create**

1. Verify that you receive an email regarding the issue

### Configure Splunk forwarder

1. Note that the controller cannot connect to the splunk server

1. Note that the API and worker nodes can connect to the splunk server

1. Connect to an API or worker node from the controller

1. Change to the root user

   ```ShellSession
   sudo su
   ```
   
1. Check the status of the Splunk indexer service

   ```ShellSession
   $ systemctl status splunk
   ```

1. Note the IP addess and port for the splunk server

   1. View the contents of the "deploymentclient.conf" that is included with Gold Disk

      ```ShellSession
      $ cat /opt/splunkforwarder/etc/apps/ccs_all_deploymentclients/local/deploymentclient.conf
      ```

   1. Note the output

      ```
      [deployment-client]

      [target-broker:deploymentServer]
      targetUri = 10.244.112.51:8089
      ```

   1. Note the target URI

      ```
      10.244.112.51:8089
      ```

1. Verify that you can connect to the target URI via telnet

   1. Enter the following
   
      ```ShellSession
      $ telnet 10.244.112.51 8089
      ```

   1. Verify that the following output is displayed

      ```
      Trying 10.244.112.51...
      Connected to 10.244.112.51.
      Escape character is '^]'.
      ```

   1. Press **control+c** on the keyboard to return to the terminal

1. Configure "deploymentclient.conf" file in the "ab2d" repo

   1. Enter the following
   
      ```ShellSession
      $ vim ~/code/ab2d/Deploy/packer/app/splunk-deploymentclient.conf
      ```

   1. Modify the file as follows

      > *** TO DO ***: verify that these settings are correct after Splunk on-boarding complete
            
      ```
      [deployment-client]
      clientName = cms-ab2d

      [target-broker:deployment-server]
      targetUri = 10.244.112.51:8089
      ```

## Configure New Relic and Splunk within the deployment AMI

> *** TO DO ***

1. Change to the "app" directory under packer

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/packer/app
   ```

1. Open the "provision-app-instance.sh" file

   ```ShellSession
   $ vim provision-app-instance.sh
   ```

1. Modify splunk and new relic code so that it deploys Splunk and New Relic configuration with the AMI

1. Save and close the file

## Request an Entrust certificate for sandbox.ab2d.cms.gov

> *** TO DO ***

## Submit an "Internet DNS Change Request Form" to product owner for the sandbox application load balancer

> *** TO DO ***

1. Open Chrome

1. Enter the following in the address bar

   > https://confluence.cms.gov/pages/viewpage.action?pageId=138595233

1. If the Confluence logon page appears, log on to Confluence

1. Note that the "CNAME/DNS Change Requests" page should be displayed

1. Select the **DNS change request form** link under the "Process" section

1. Select the **Download** icon in the top right of the page

1. Wait for the download to complete

1. Open the downloaded form

   ```
   Internet DNS Change Request (2).pdf
   ```

1. Fill out the form as follows

   *Requestor Information:*

   - **Name:** {product owner first name} {product owner last name}

   - **Organization:** {product owner organization}

   - **Email:** {product owner email}

   - **Phone:** {product owner phone}

   *CMS Business Owner Information*

   - **Name:** {business owner first name} {business owner last name}

   - **Organization:** {business owner organization}

   - **Email:** {business owner email}

   - **Phone:** {business owner phone}

   - **Reason:** To support data sharing with PDP sponsor.  This static webpage will provide information for the PDP sponsors.

   *DNS Change Information*

   - **DNS Zone:** cms.gov

   - **Type of change:** CNAME

   - **Actual Change:** ab2d.cms.gov CNAME {unique id}.cloudfront.net

   - **Change Date & Time:** ASAP

   - **Purpose of the change:** Initial launch of new informational page for AB2D API

1. Submit the completed for to the product owner

1. Note that the product owner will complete the process

## Create an ab2d vault in 1Password

1. Open Chrome

1. Enter the following in the address bar

   > https://my.1password.com/signin?l=en

1. Log on to 1Password

1. Select **New Vault**

1. Type the following in the **Vault Title** text box

   ```
   ab2d
   ```

1. Keep **Let administrators manage this vault** checked

1. Select **Create Vault**

1. Note the newly created "ab2d" vault

1. Select the gear icon within the "ab2d" vault

1. Select **Manage** beside "People"

1. Add team members

   1. Type a team member's name in the search box

   1. Check the checkbox beside the team member in the search results

   1. Clear the search box

   1. Repeat this step for all team members

1. Note that the person creating the vault already has full access

1. Select **Update Team Members**

1. Note that added team members default to "View & Edit"

1. Grant full acess to desired users

   1. Note that the following people will be granted full access

      - Business Analyst

      - DevOps Engineer
      
      - Lead Engineer
      
      - Project Manager

      - Scrum Master

      - Security Engineer

      - SemanticBits Systems Architect
      
   1. Select the gear icon beside the user to get full access

   1. Select **Allow Managing**

   1. Note that a checkmark icon now appears beside "Allow Managing"

## Add an entrust certificate to the ab2d vault in 1Password

> *** TO DO ***: Move this section elsewhere within the document

1. Open Chrome

1. Enter the following in the address bar

   > https://my.1password.com/signin?l=en

1. Log on to 1Password

1. Select the gear icon within the "ab2d" vault

1. Select **Open Vault** from the left side of the page

1. Select the "+" icon at the bottom middle of the page

1. Select **Document**

1. Type the domain name related to the certificate that will be uploaded in the **Title** text box

   *Example:*

   ```
   ab2d.cms.gov certificate chain
   ```

1. Select **Drag File to Upload**

1. Navigate to the zip file of the downloaded certificates

   *Example:*
   
   ```
   ~/Downloads/entrust.zip
   ```

1. Select **Open**

1. Create the following labels

   label           |new field
   ----------------|---------
   certificate type|entrust

1. Select **Save**

## Add a private key to the ab2d vault in 1Password

> *** TO DO ***: Move this section elsewhere within the document

1. Open Chrome

1. Enter the following in the address bar

   > https://my.1password.com/signin?l=en

1. Log on to 1Password

1. Select the gear icon within the "ab2d" vault

1. Select **Open Vault** from the left side of the page

1. Select the "+" icon at the bottom middle of the page

1. Select **Document**

1. Type the domain name related to the certificate that will be uploaded in the **Title** text box

   *Example:*

   ```
   ab2d dev private key
   ```

1. Select **Drag File to Upload**

1. Navigate to the zip file of the downloaded certificates

   *Example:*
   
   ```
   ~/Downloads/ab2d-dev.pem
   ```

1. Select **Open**

1. Create the following labels

   label   |new field
   --------|---------
   key type|pem file

1. Select **Save**

## Peer AB2D Dev, Sandbox, Impl environments with the BFD Sbx VPC and peer AB2D Prod with BFD Prod VPC

1. Note that CCS usually likes to handle the peering between project environments and BFD and then the ADO teams can authorize access

1. Request that CMS technical contact (e.g. Stephen) create a ticket like the following

   > https://jira.cms.gov/browse/CMSAWSOPS-53861
