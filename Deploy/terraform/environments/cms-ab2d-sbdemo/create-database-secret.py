#!/usr/bin/env python3

import boto3
import sys

# Ensure command line arguments were passed
command_line_arguments_count = sys.argv.__len__() - 1
if command_line_arguments_count != 4:
    print("Try running the script like so:")
    print("./create-database-secrets.py {environment} {database_user|database_password|database_name} {kms key id} {date time}")
    exit(1)

# Eliminate double quotes from command line arguments
environment = sys.argv[1].replace('"', '')
secret_item = sys.argv[2].replace('"', '')
kms_string = sys.argv[3].replace('"', '')
date_time = sys.argv[4].replace('"', '')

# Initialize the AWS Secrets Manager client
client = boto3.client('secretsmanager')

# Create sercret name
secret_name = 'ab2d/' + environment + '/module/db/' + secret_item + '/' + date_time

# Prompt for secret
secret = input("Enter desired " + secret_item + ": ")

# Create database user secret in AWS Secrets Manager
response = client.create_secret(
    Name=secret_name,
    KmsKeyId=kms_string,
    SecretString=secret
)

print(secret)
