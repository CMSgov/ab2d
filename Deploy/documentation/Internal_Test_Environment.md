# Internal Test Environment

## Table of Contents

1. [Create AWS keypair](#create-aws-keypair)
1. [Update files for AWS test environment](#update-files-for-aws-test-environment)
1. [Create required S3 buckets](#create-required-s3-buckets)
1. [Create policies](#create-policies)
1. [Create roles](#create-roles)
1. [Create an image in an AWS Elastic Container Registry](#create-an-image-in-an-aws-elastic-container-egistry)
1. [Create base aws environment](#create-base-aws-environment)
1. [Deploy to test environment](#deploy-to-test-environment)
         
## Create AWS keypair

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

## Create required S3 buckets

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
   
1. Add this bucket policy to the "cms-ab2d-cloudtrail" S3 bucket via the AWS console

   > *** TO DO ***: Need to script this using AWS CLI
   
   ```
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Principal": {
           "AWS": "arn:aws:iam::127311923021:root"
         },
         "Action": "s3:PutObject",
         "Resource": "arn:aws:s3:::cms-ab2d-cloudtrail/*"
       }
     ]
   }
   ```

1. Block public access on bucket

   ```ShellSession
   $ aws s3api put-public-access-block \
     --bucket cms-ab2d-cloudtrail \
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

1. Create "Ab2dEcsForEc2Policy"

   ```ShellSession
   $ aws iam create-policy --policy-name Ab2dEcsForEc2Policy --policy-document file://ab2d-ecs-for-ec2-policy.json
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

## Create base aws environment

1. Change to the environment directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy/terraform/environments/cms-ab2d-sbdemo
   ```

1. Create the AWS networking

   ```ShellSession
   $ ./create-base-environment.sh
   ```

## Deploy to test environment

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
   
1. Set target profile

   *Example for the "semanticbitsdemo" AWS account:*
   
   ```ShellSession
   $ export AWS_PROFILE="sbdemo"
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

1. Use the AWS Console to give "Write objects" and "Read bucket permissions" to the "S3 log delivery group" of the "cms-ab2d-cloudtrail" bucket

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

1. Add this bucket policy to the "cms-ab2d-cloudtrail" S3 bucket via the AWS console

   ```ShellSession
   $ aws s3api put-bucket-policy --bucket cms-ab2d-cloudtrail --policy file://cms-ab2d-cloudtrail-bucket-policy.json
   ```
   
1. Configure the networking

   1. Open the "variables.tf"

      ```ShellSession
      $ vim variables.tf
      ```

   1. Change the following lines to the correct VPC ID

      ```
      variable "vpc_id" {
        default = "{vpc id}"
      }
      ```

   1. Change the following lines to the correct private subnet IDs

      ```
      variable "private_subnet_ids" {
        type        = list(string)
        default     = ["{first private subnet id}", "{second private subnet id}", "{third private subnet id}"]
        description = "App instances and DB go here"
      }
      ```

   1. Change the following lines to the correct public subnet IDs

      ```
      variable "deployment_controller_subnet_ids" {
        type        = list(string)
        default     = ["{first public subnet id}", "{second public subnet id}", "{third public subnet id}"]
        description = "Deployment controllers go here"
      }
      ```

   1. Save and close the file
      
1. Configure database

   *Format:*
   
   ```ShellSession
   $ terraform apply \
     --var "db_username={db username}" \
     --var "db_password={db password}" \
     --target module.db --auto-approve
   ```

1. Change to the "Deploy" directory

   ```ShellSession
   $ cd ~/code/ab2d/Deploy
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
   
1. Configure packer

   1. Open the "app.json" file

      ```ShellSession
      $ vim ~/code/ab2d/Deploy/packer/app/app.json
      ```

   1. Change the gold disk AMI to the noted CentOS AMI

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
      
1. Deploy application components

   ```ShellSession
   $ ./deploy.sh --environment=sbdemo --auto-approve
   ```
   