#!/usr/bin/env python3

# Use this code snippet in your app.
# If you need more information about configurations or implementing the sample code, visit the AWS docs:   
# https://aws.amazon.com/developers/getting-started/python/

import boto3
import base64
import sys
from botocore.exceptions import ClientError

# Ensure command line arguments were passed
command_line_arguments_count = sys.argv.__len__() - 1
if command_line_arguments_count != 3:
    print("Try running the script like so:")
    print("./get-database-secret.py {environment} {database_user|database_password|database_name} {date_time}")
    exit(1)

# Eliminate double quotes from command line arguments
environment = sys.argv[1].replace('"', '')
secret_item = sys.argv[2].replace('"', '')
date_time = sys.argv[3].replace('"', '')

def get_secret(environment, secret_item, date_time):

    secret_name = "ab2d/" + environment + "/module/db/" + secret_item + "/" + date_time
    region_name = "us-east-1"
    secret_not_found = bool(False);

    # Create a Secrets Manager client
    session = boto3.session.Session()
    client = session.client(
        service_name='secretsmanager',
        region_name=region_name
    )

    # In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
    # See https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
    # We rethrow the exception by default.

    try:
        get_secret_value_response = client.get_secret_value(
            SecretId=secret_name
        )
    except ClientError as e:
        if e.response['Error']['Code'] == 'DecryptionFailureException':
            # Secrets Manager can't decrypt the protected secret text using the provided KMS key.
            # Deal with the exception here, and/or rethrow at your discretion.
            raise e
        elif e.response['Error']['Code'] == 'InternalServiceErrorException':
            # An error occurred on the server side.
            # Deal with the exception here, and/or rethrow at your discretion.
            raise e
        elif e.response['Error']['Code'] == 'InvalidParameterException':
            # You provided an invalid value for a parameter.
            # Deal with the exception here, and/or rethrow at your discretion.
            raise e
        elif e.response['Error']['Code'] == 'InvalidRequestException':
            # You provided a parameter value that is not valid for the current state of the resource.
            # Deal with the exception here, and/or rethrow at your discretion.
            raise e
        elif e.response['Error']['Code'] == 'ResourceNotFoundException':
            # We can't find the resource that you asked for.
            # Deal with the exception here, and/or rethrow at your discretion.
            # raise e
            secret_not_found = bool(True)
    else:
        # Decrypts secret using the associated KMS CMK.
        # Depending on whether the secret is a string or binary, one of these fields will be populated.
        if 'SecretString' in get_secret_value_response:
            secret = get_secret_value_response['SecretString']
        else:
            decoded_binary_secret = base64.b64decode(get_secret_value_response['SecretBinary'])

    if secret_not_found:
        return ""
    else:
        return get_secret_value_response['SecretString']
    
print(get_secret(environment, secret_item, date_time))
