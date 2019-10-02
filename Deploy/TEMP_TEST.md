# TEMP TEST

## Table of Contents

1. [Create base AWS networking](#create-base-aws-networking)

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
