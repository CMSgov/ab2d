#!/usr/bin/env python3

import boto3
import sys

# Ensure command line arguments were passed

command_line_arguments_count = sys.argv.__len__() - 1
if command_line_arguments_count != 2:
    print("Try running the script like so:")
    print("./create-database-secrets.py {environment} {vpc id}")
    exit(1)

# Eliminate double quotes from command line arguments

environment = sys.argv[1].replace('"', '')
vpc_id = sys.argv[2].replace('"', '')

def get_new_cidr(environment, vpc_id):

    region_name = "us-east-1"

    # Create a EC2 client
    
    session = boto3.session.Session()
    ec2_client = session.client(
        service_name='ec2',
        region_name=region_name
    )

    # Get VPC

    response = ec2_client.describe_vpcs(
        VpcIds=[
            vpc_id
        ]
    )

    vpc_cidr_block = response['Vpcs'][0]['CidrBlock']

    # Get components of vpc_cidr_block
    
    vpc_cidr_block_ip_address = vpc_cidr_block.split('/')[0]
    vpc_cidr_block_mask = vpc_cidr_block.split('/')[1]
    subnet_cidr_block_ip_address_position_1 = vpc_cidr_block_ip_address.split('.')[0]
    subnet_cidr_block_ip_address_position_2 = vpc_cidr_block_ip_address.split('.')[1]
    subnet_cidr_block_ip_address_position_4 = vpc_cidr_block_ip_address.split('.')[3]
    
    if vpc_cidr_block_mask < '24':
        subnet_cidr_block_mask = '24'
    else:
        print('ERROR: this script currently requires a VPC CIDR block mask that is less than 24')
        exit(1)

    subnet_cidr_block_ip_address_position_3 = get_next_subnet_cidr_block_ip_address_position_3(ec2_client, vpc_id)

    subnet_cidr_block = subnet_cidr_block_ip_address_position_1 + '.' \
        + subnet_cidr_block_ip_address_position_2 + '.' \
        + subnet_cidr_block_ip_address_position_3 + '.' \
        + subnet_cidr_block_ip_address_position_4 + '/' \
        + subnet_cidr_block_mask

    return(subnet_cidr_block)
    
def get_next_subnet_cidr_block_ip_address_position_3(ec2_client, vpc_id):
    response = ec2_client.describe_subnets(
        Filters=[
            {
                'Name': 'vpc-id',
                'Values': [
                    vpc_id
                ]
            }
        ]
    )
    subnets = response['Subnets']
    subnet_count = len(subnets)
    if subnet_count == 0:
        subnet_cidr_block_ip_address_position_3 = '1'
    else:
        existing_position_3_dict = {}
        for item in subnets:
            position_3 = item['CidrBlock'].split('.')[2]
            existing_position_3_dict[position_3] = "used"
        for index in range(256):
            if index != 0:
                if existing_position_3_dict.get(str(index)) is None:
                    subnet_cidr_block_ip_address_position_3 = str(index)
                    break
                
    return(subnet_cidr_block_ip_address_position_3)

print(get_new_cidr(environment, vpc_id))
