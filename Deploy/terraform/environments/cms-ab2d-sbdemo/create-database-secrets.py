#!/usr/bin/env python3

import boto3
import sys

environment = sys.argv[1].replace('"', '')
kms_string = sys.argv[2].replace('"', '')
client = boto3.client('secretsmanager')

database_user = input("Enter desired database user name: ")
database_password = input("Enter desired database password: ")

# *** TO DO ***: add a unique datetime string to secret names

database_user_secret_name = 'ab2d/' + environment + '/module/db/database_user'
print(database_user_secret_name)

database_password_secret_name = 'ab2d/' + environment + '/module/db/database_password'
print(database_password_secret_name)

# response = client.create_secret(
#     Name='ab2d/sbdemo/module/db/database_user',
#     KmsKeyId=kms_string,
#     SecretString='ab2d_user'
# )

# response = client.create_secret(
#     Name='ab2d/sbdemo/module/db/database_password',
#     KmsKeyId=kms_string,
#     SecretString='ab2d_password'
# )

print("Database user and password stored in Secrets Manager")
