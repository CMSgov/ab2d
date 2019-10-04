# Internal Test Environment

## Table of Contents

1. [Create base AWS networking](#create-base-aws-networking)
1. [Create AWS security components](#create-aws-security-components)
1. [Update files for AWS test environment](#update-files-for-aws-test-environment)
1. [Create S3 bucket for automation](#create-s3-bucket-for-automation)
1. [Create policies](#create-policies)
1. [Create roles](#create-roles)
1. [Test deployment](#test-deployment)
1. [Create an image in an AWS Elastic Container Registry](#create-an-image-in-an-aws-elastic-container-egistry)

## Create AWS networking

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Verify the AWS_PROFILE has been set

   ```ShellSession
   $ echo $AWS_PROFILE
   ```

1. List available regions

   ```ShellSession
   $ aws ec2 describe-regions
   ```

1. Verify that the region you want to target has the following "OptInStatus"

   ```
   opt-in-not-required
   ```

1. List availability zones for the target region

   *Example for "us-east-1":*
   
   ```ShellSession
   $ aws --region us-east-1 ec2 describe-availability-zones
   ```

1. Create VPC

   *Example:*
   
   ```ShellSession
   $ aws --region us-east-1 ec2 create-vpc \
     --cidr-block 10.124.0.0/16
   ```

1. Set VPC_ID environment variable

   *Example:*
   
   ```ShellSession
   $ export VPC_ID=vpc-064c3621b7205922a
   ```

1. Create internet gateway

   ```ShellSession
   $ aws --region us-east-1 ec2 create-internet-gateway
   ```

1. Set IGW_ID environment variable

   *Example:*
   
   ```ShellSession
   $ export IGW_ID=igw-001a4056026aef4b4
   ```

1. Attach internet gateway to VPC

   ```ShellSession
   $ aws --region us-east-1 ec2 attach-internet-gateway \
     --internet-gateway-id $IGW_ID \
     --vpc-id $VPC_ID
   ```

1. Describe route table

   ```ShellSession
   $ aws --region us-east-1 ec2 describe-route-tables \
     --filter "Name=vpc-id,Values=$VPC_ID"
   ```

1. Set RT_ID environment variable

   *Example:*
   
   ```ShellSession
   $ export RT_ID=rtb-0404876fb61e5fe2f
   ```

1. Add route for internet gateway

   ```ShellSession
   $ aws --region us-east-1 ec2 create-route \
     --destination-cidr-block 0.0.0.0/0 \
     --gateway-id $IGW_ID \
     --route-table-id $RT_ID
   ```

1. Create first subnet

   ```ShellSession
   $ aws --region us-east-1 ec2 create-subnet \
     --availability-zone us-east-1a \
     --cidr-block 10.124.1.0/24 \
     --vpc-id $VPC_ID
   ```

1. Set SUBNET_ID environment variable

   *Example:*
   
   ```ShellSession
   $ export SUBNET_ID=subnet-0b8ba5ef9b89b07ed
   ```

1. Associate route table with the subnet

   ```ShellSession
   $ aws --region us-east-1 ec2 associate-route-table \
     --route-table-id $RT_ID \
     --subnet-id $SUBNET_ID
   ```

## Create AWS security components

1. Create security group

   *Example for test cluster:*
   
   ```ShellSession
   $ aws --region us-east-1 ec2 create-security-group \
     --description "test cluster" \
     --group-name test-cluster-sg \
     --vpc-id $VPC_ID
   ```

1. Set SG_ID environment variable

   *Example:*
   
   ```ShellSession
   $ export SG_ID=sg-032be103af7f834fc
   ```

1. Add SSH security group ingress rule

   ```ShellSession
   $ aws --region us-east-1 ec2 authorize-security-group-ingress \
     --group-id $SG_ID \
     --protocol tcp \
     --port 22 \
     --cidr 0.0.0.0/0
   ```

1. Create keypair

   *Example for test cluster:*
   
   ```ShellSession
   $ aws --region us-east-1 ec2 create-key-pair \
     --key-name test-cluster \
     --query 'KeyMaterial' \
     --output text \
     > ~/.ssh/test-cluster.pem
   ```

1. Change permissions of the key

   *Example for test cluster:*
   
   ```ShellSession
   $ chmod 600 ~/.ssh/test-cluster.pem
   ```

## Update files for AWS test environment

1. Change to the repo directory

   ```ShellSession
   $ cd ~/code/ab2d
   ```

1. Backup the "app.json" file to be used for the government AWS account

   ```ShellSession
   $ cp Deploy/packer/app/app.json Deploy/packer/app/app.json.gov
   ```

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Get the latest CentOS AMI

   ```ShellSession
   $ aws --region us-east-1 ec2 describe-images \
     --owners aws-marketplace \
     --filters Name=product-code,Values=aw0evgkw8e5c1q413zgy5pjce \
     --query 'Images[*].[ImageId,CreationDate]' \
     --output text \
     | sort -k2 -r \
     | head -n1
   ```

1. Note the AMI in the output

   *Example:*
   
   ```
   ami-02eac2c0129f6376b
   ```

1. Open the "app.json" file

   ```ShellSession
   $ vim Deploy/packer/app/app.json
   ```

1. Change the following with the noted AMI

   *Example:*
   
   ```
   "gold_ami": "ami-02eac2c0129f6376b"
   ```

1. Change the networking settings based on the VPC that was created

   *Example:*
   
   ```
   "subnet_id": "subnet-0b8ba5ef9b89b07ed",
   "vpc_id": "vpc-064c3621b7205922a",
   ```

1. Change the builders settings

   *Example:*

   ```
   "iam_instance_profile": "lonnie.hanekamp@semanticbits.com",
   "ssh_username": "centos",
   ```

1. Save and close "app.json"

1. Backup the "provision-app-instance.sh" file to be used for the government AWS account

   ```ShellSession
   $ cp Deploy/packer/app/provision-app-instance.sh Deploy/packer/app/provision-app-instance.sh.gov
   ```

1. Open "provision-app-instance.sh"

   ```ShellSession
   $ vim Deploy/packer/app/provision-app-instance.sh
   ```

1. Comment out gold disk related items

   1. Comment out "Update splunk forwarder config" section

      ```
      #
      # LSH Comment out gold disk related section
      #
      # # Update splunk forwarder config
      # sudo chown splunk:splunk /tmp/splunk-deploymentclient.conf
      # sudo mv -f /tmp/splunk-deploymentclient.conf /opt/splunkforwarder/etc/system/local/deploymentclient.conf
      ```

   1. Comment out "Make sure splunk can read all logs" section

      ```
      #
      # LSH Comment out gold disk related section
      #
      # # Make sure splunk can read all logs
      # sudo /opt/splunkforwarder/bin/splunk stop
      # sudo /opt/splunkforwarder/bin/splunk clone-prep-clear-config
      # sudo /opt/splunkforwarder/bin/splunk start
      ```

1. Save and close "provision-app-instance.sh"

## Create S3 bucket for automation

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
   
## Create policies

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

## Create roles

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
   ```

1. Create "Ab2dManagedRole" role

   ```ShelSession
   $ aws iam create-role --role-name Ab2dManagedRole --assume-role-policy-document file://ab2d-managed-role.json
   ```

1. Attach required policies to the "Ab2dManagedRole" role

   ```ShellSession
   $ aws iam attach-role-policy --role-name Ab2dManagedRole --policy-arn arn:aws:iam::114601554524:policy/Ab2dAccessPolicy
   ```

## Create instance profiles

1. Note that instance profiles are not visible within the AWS console

1. Create instance profile

   ```ShellSession
   $ aws iam create-instance-profile --instance-profile-name Ab2dInstanceProfile
   ```

1. Attach "Ab2dInstanceRole" to "Ab2dInstanceProfile"

   ```ShellSession
   $ aws iam add-role-to-instance-profile --role-name Ab2dInstanceRole --instance-profile-name Ab2dInstanceProfile
   ```
   
## Test deployment

1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
   ```

1. Test deployment

   ```ShellSession
   $ ./deploy.sh --environment=cms-ab2d-dev
   ```


## Create an image in an AWS Elastic Container Registry

> *** TO DO ***
