#!/bin/bash

set -e #Exit on first error
set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Set up an encrpted S3 bucket in the SemanticBits demo account
#

# Set SemanticBits demo account credentials

export AWS_PROFILE=sbdemo-shared
export AWS_DEFAULT_REGION=us-east-1

# Set test bucket name

S3_TEST_BUCKET_NAME="ab2d-optout"

# Determine if the test bucket already exists

S3_TEST_BUCKET_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" s3api list-buckets \
  --query "Buckets[?Name=='${S3_TEST_BUCKET_NAME}'].Name" \
  --output text)

# Create the test bucket (if doesn't exist)

if [ -z "${S3_AUTOMATION_BUCKET_EXISTS}" ]; then

  # Create the test bucket
    
  aws --region "${AWS_DEFAULT_REGION}" s3api create-bucket \
    --bucket ${S3_TEST_BUCKET_NAME}

  # Block public access on the test bucket
  
  aws --region "${AWS_DEFAULT_REGION}" s3api put-public-access-block \
    --bucket ${S3_TEST_BUCKET_NAME} \
    --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
  
fi

# Set KMS key alias

S3_TEST_BUCKET_KEY_ALIAS="${S3_TEST_BUCKET_NAME}-key"

# Determine if a KMS key with the alias already exists

S3_TEST_BUCKET_KEY_ALIAS_EXISTS=$(aws --region "${AWS_DEFAULT_REGION}" kms list-aliases \
  --query "Aliases[?AliasName == 'alias/ab2d-optout']"
