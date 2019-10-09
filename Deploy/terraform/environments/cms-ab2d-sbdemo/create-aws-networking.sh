#!/bin/bash

# Create base AWS networking

export AWS_PROFILE="sbdemo"

# *** TO DO ***: Get a list of existing VPCs

aws ec2 describe-vpcs \
    


# If there are no existing VPCs, create the VPC
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.124.0.0/16 \
  --query 'Vpc.{VpcId:VpcId}' \
  --output text \
  --region us-east-1)



