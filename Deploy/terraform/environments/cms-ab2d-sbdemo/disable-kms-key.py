#!/usr/bin/env python3

import boto3
import sys

kms_string = sys.argv[1].replace('"', '')
client = boto3.client('kms')

response = client.disable_key(
    KeyId=kms_string
)

print("KMS",kms_string,"disabled...")
