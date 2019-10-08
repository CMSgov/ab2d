# Internal Test Environment

## Table of Contents

1. [Create base AWS networking](#create-base-aws-networking)
1. [Create AWS security components](#create-aws-security-components)
1. [Update files for AWS test environment](#update-files-for-aws-test-environment)
1. [Create S3 bucket for automation](#create-s3-bucket-for-automation)
1. [Create policies](#create-policies)
1. [Create roles](#create-roles)
1. [Create an image in an AWS Elastic Container Registry](#create-an-image-in-an-aws-elastic-container-egistry)
1. [Prepare terraform](#prepare-terraform)
1. [Test deployment](#test-deployment)

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
   $ VPC_ID=$(aws ec2 create-vpc \
     --cidr-block 10.124.0.0/16 \
     --query 'Vpc.{VpcId:VpcId}' \
     --output text \
     --region us-east-1)
   ```

1. Create first public subnet

   ```ShellSession
   $ SUBNET_PUBLIC_1_ID=$(aws ec2 create-subnet \
     --vpc-id $VPC_ID \
     --cidr-block 10.124.1.0/24 \
     --availability-zone us-east-1a \
     --query 'Subnet.{SubnetId:SubnetId}' \
     --output text \
     --region us-east-1)
   ```

1. Tag first public subnet

   ```ShellSession
   $ aws ec2 create-tags \
     --resources $SUBNET_PUBLIC_1_ID \
     --tags "Key=Name,Value=ab2d-public-subnet-01" \
     --region us-east-1
   ```

1. Create second public subnet

   ```ShellSession
   $ SUBNET_PUBLIC_2_ID=$(aws ec2 create-subnet \
     --vpc-id $VPC_ID \
     --cidr-block 10.124.2.0/24 \
     --availability-zone us-east-1b \
     --query 'Subnet.{SubnetId:SubnetId}' \
     --output text \
     --region us-east-1)
   ```

1. Tag second public subnet

   ```ShellSession
   $ aws ec2 create-tags \
     --resources $SUBNET_PUBLIC_2_ID \
     --tags "Key=Name,Value=ab2d-public-subnet-02" \
     --region us-east-1
   ```

1. Create third public subnet

   ```ShellSession
   $ SUBNET_PUBLIC_3_ID=$(aws ec2 create-subnet \
     --vpc-id $VPC_ID \
     --cidr-block 10.124.3.0/24 \
     --availability-zone us-east-1c \
     --query 'Subnet.{SubnetId:SubnetId}' \
     --output text \
     --region us-east-1)
   ```

1. Tag third public subnet

   ```ShellSession
   $ aws ec2 create-tags \
     --resources $SUBNET_PUBLIC_3_ID \
     --tags "Key=Name,Value=ab2d-public-subnet-03" \
     --region us-east-1
   ```

1. Create first private subnet

   ```ShellSession
   $ SUBNET_PRIVATE_1_ID=$(aws ec2 create-subnet \
     --vpc-id $VPC_ID \
     --cidr-block 10.124.4.0/24 \
     --availability-zone us-east-1d \
     --query 'Subnet.{SubnetId:SubnetId}' \
     --output text \
     --region us-east-1)
   ```

1. Tag first private subnet

   ```ShellSession
   $ aws ec2 create-tags \
     --resources $SUBNET_PRIVATE_1_ID \
     --tags "Key=Name,Value=ab2d-private-subnet-01" \
     --region us-east-1
   ```

1. Create second private subnet

   ```ShellSession
   $ SUBNET_PRIVATE_2_ID=$(aws ec2 create-subnet \
     --vpc-id $VPC_ID \
     --cidr-block 10.124.5.0/24 \
     --availability-zone us-east-1e \
     --query 'Subnet.{SubnetId:SubnetId}' \
     --output text \
     --region us-east-1)
   ```

1. Tag second private subnet

   ```ShellSession
   $ aws ec2 create-tags \
     --resources $SUBNET_PRIVATE_2_ID \
     --tags "Key=Name,Value=ab2d-private-subnet-02" \
     --region us-east-1
   ```

1. Create third private subnet

   ```ShellSession
   $ SUBNET_PRIVATE_3_ID=$(aws ec2 create-subnet \
     --vpc-id $VPC_ID \
     --cidr-block 10.124.6.0/24 \
     --availability-zone us-east-1f \
     --query 'Subnet.{SubnetId:SubnetId}' \
     --output text \
     --region us-east-1)
   ```

1. Tag third private subnet

   ```ShellSession
   $ aws ec2 create-tags \
     --resources $SUBNET_PRIVATE_3_ID \
     --tags "Key=Name,Value=ab2d-private-subnet-03" \
     --region us-east-1
   ```

1. Create internet gateway

   ```ShellSession
   $ IGW_ID=$(aws ec2 create-internet-gateway \
     --query 'InternetGateway.{InternetGatewayId:InternetGatewayId}' \
     --output text \
     --region us-east-1)
   ```

1. Attach internet gateway to VPC

   ```ShellSession
   $ aws --region us-east-1 ec2 attach-internet-gateway \
     --internet-gateway-id $IGW_ID \
     --vpc-id $VPC_ID
   ```

1. Note that the main route table was automatically created and will be used by the private subnets

1. Create a custom route table for public subnets

   ```ShellSession
   $ ROUTE_TABLE_ID=$(aws ec2 create-route-table \
     --vpc-id $VPC_ID \
     --query 'RouteTable.{RouteTableId:RouteTableId}' \
     --output text \
     --region us-east-1)
   ```

1. Add route for internet gateway to the custom route table for public subnets

   ```ShellSession
   $ aws --region us-east-1 ec2 create-route \
     --destination-cidr-block 0.0.0.0/0 \
     --gateway-id $IGW_ID \
     --route-table-id $ROUTE_TABLE_ID
   ```

1. Associate the first public subnet with the custom route table for public subnets

   ```ShellSession
   $ aws ec2 associate-route-table  \
     --subnet-id $SUBNET_PUBLIC_1_ID \
     --route-table-id $ROUTE_TABLE_ID \
     --region us-east-1
   ```

1. Associate the second public subnet with the custom route table for public subnets

   ```ShellSession
   $ aws ec2 associate-route-table  \
     --subnet-id $SUBNET_PUBLIC_2_ID \
     --route-table-id $ROUTE_TABLE_ID \
     --region us-east-1
   ```

1. Associate the third public subnet with the custom route table for public subnets

   ```ShellSession
   $ aws ec2 associate-route-table  \
     --subnet-id $SUBNET_PUBLIC_3_ID \
     --route-table-id $ROUTE_TABLE_ID \
     --region us-east-1
   ```

1. Enable Auto-assign Public IP on the first public subnet

   ```ShellSession
   $ aws ec2 modify-subnet-attribute \
     --subnet-id $SUBNET_PUBLIC_1_ID \
     --map-public-ip-on-launch \
     --region us-east-1
   ```

1. Enable Auto-assign Public IP on the second public subnet

   ```ShellSession
   $ aws ec2 modify-subnet-attribute \
     --subnet-id $SUBNET_PUBLIC_2_ID \
     --map-public-ip-on-launch \
     --region us-east-1
   ```

1. Enable Auto-assign Public IP on the second public subnet

   ```ShellSession
   $ aws ec2 modify-subnet-attribute \
     --subnet-id $SUBNET_PUBLIC_3_ID \
     --map-public-ip-on-launch \
     --region us-east-1
   ```

1. Create NAT Gateway for the first public subnet

   1. Allocate Elastic IP Address for first NAT Gateway
   
      ```ShellSession
      $ EIP_ALLOC_1_ID=$(aws ec2 allocate-address \
        --domain vpc \
        --query '{AllocationId:AllocationId}' \
        --output text \
        --region us-east-1)
      ```
   
   1. Create NAT gateway
   
      ```ShellSession
      $ NAT_GW_1_ID=$(aws ec2 create-nat-gateway \
        --subnet-id $SUBNET_PUBLIC_1_ID \
        --allocation-id $EIP_ALLOC_1_ID \
        --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
        --output text \
        --region us-east-1)
      ```

   1. Wait for the NAT gateway to have a status of AVAILABLE before proceeding

1. Associate the first private subnet with the NAT Gateway for the first public subnet

   1. Create a custom route table for the first private subnet
   
      ```ShellSession
      $ ROUTE_TABLE_FOR_PRIVATE_SUBNET_1_ID=$(aws ec2 create-route-table \
        --vpc-id $VPC_ID \
        --query 'RouteTable.{RouteTableId:RouteTableId}' \
        --output text \
        --region us-east-1)
      ```

   1. Create route to NAT Gateway

      ```ShellSession
      $ aws ec2 create-route \
        --route-table-id $ROUTE_TABLE_FOR_PRIVATE_SUBNET_1_ID \
        --destination-cidr-block 0.0.0.0/0 \
        --gateway-id $NAT_GW_1_ID \
        --region us-east-1
      ```

   1. Associate the first private subnet with the custom route table for public subnets
   
      ```ShellSession
      $ aws ec2 associate-route-table  \
        --subnet-id $SUBNET_PRIVATE_1_ID \
        --route-table-id $ROUTE_TABLE_FOR_PRIVATE_SUBNET_1_ID \
        --region us-east-1
      ```

1. Create NAT Gateway for the second public subnet

   1. Allocate Elastic IP Address for second NAT Gateway
   
      ```ShellSession
      $ EIP_ALLOC_2_ID=$(aws ec2 allocate-address \
        --domain vpc \
        --query '{AllocationId:AllocationId}' \
        --output text \
        --region us-east-1)
      ```
   
   1. Create NAT gateway
   
      ```ShellSession
      $ NAT_GW_2_ID=$(aws ec2 create-nat-gateway \
        --subnet-id $SUBNET_PUBLIC_2_ID \
        --allocation-id $EIP_ALLOC_2_ID \
        --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
        --output text \
        --region us-east-1)
      ```

   1. Wait for the NAT gateway to have a status of AVAILABLE before proceeding

1. Associate the second private subnet with the NAT Gateway for the second public subnet

   1. Create a custom route table for the second private subnet
   
      ```ShellSession
      $ ROUTE_TABLE_FOR_PRIVATE_SUBNET_2_ID=$(aws ec2 create-route-table \
        --vpc-id $VPC_ID \
        --query 'RouteTable.{RouteTableId:RouteTableId}' \
        --output text \
        --region us-east-1)
      ```

   1. Create route to NAT Gateway

      ```ShellSession
      $ aws ec2 create-route \
        --route-table-id $ROUTE_TABLE_FOR_PRIVATE_SUBNET_2_ID \
        --destination-cidr-block 0.0.0.0/0 \
        --gateway-id $NAT_GW_2_ID \
        --region us-east-1
      ```

   1. Associate the second private subnet with the custom route table for public subnets
   
      ```ShellSession
      $ aws ec2 associate-route-table  \
        --subnet-id $SUBNET_PRIVATE_2_ID \
        --route-table-id $ROUTE_TABLE_FOR_PRIVATE_SUBNET_2_ID \
        --region us-east-1
      ```





1. Create NAT Gateway for the third public subnet

   1. Allocate Elastic IP Address for third NAT Gateway
   
      ```ShellSession
      $ EIP_ALLOC_3_ID=$(aws ec2 allocate-address \
        --domain vpc \
        --query '{AllocationId:AllocationId}' \
        --output text \
        --region us-east-1)
      ```
   
   1. Create NAT gateway
   
      ```ShellSession
      $ NAT_GW_3_ID=$(aws ec2 create-nat-gateway \
        --subnet-id $SUBNET_PUBLIC_3_ID \
        --allocation-id $EIP_ALLOC_3_ID \
        --query 'NatGateway.{NatGatewayId:NatGatewayId}' \
        --output text \
        --region us-east-1)
      ```

   1. Wait for the NAT gateway to have a status of AVAILABLE before proceeding

1. Associate the third private subnet with the NAT Gateway for the third public subnet

   1. Create a custom route table for the third private subnet
   
      ```ShellSession
      $ ROUTE_TABLE_FOR_PRIVATE_SUBNET_3_ID=$(aws ec2 create-route-table \
        --vpc-id $VPC_ID \
        --query 'RouteTable.{RouteTableId:RouteTableId}' \
        --output text \
        --region us-east-1)
      ```

   1. Create route to NAT Gateway

      ```ShellSession
      $ aws ec2 create-route \
        --route-table-id $ROUTE_TABLE_FOR_PRIVATE_SUBNET_3_ID \
        --destination-cidr-block 0.0.0.0/0 \
        --gateway-id $NAT_GW_3_ID \
        --region us-east-1
      ```

   1. Associate the third private subnet with the custom route table for public subnets
   
      ```ShellSession
      $ aws ec2 associate-route-table  \
        --subnet-id $SUBNET_PRIVATE_3_ID \
        --route-table-id $ROUTE_TABLE_FOR_PRIVATE_SUBNET_3_ID \
        --region us-east-1
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

## Create an image in an AWS Elastic Container Registry

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

## Prepare terraform

1. Change to the "cms-ab2d-dev" environment

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-dev
   ```

1. Initialize terraform

   ```ShellSession
   $ terraform init
   ```

1. Validate terraform

   ```ShellSession
   $ terraform validate
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
   $ ./deploy.sh --environment=sbdemo --auto-approve
   ```
