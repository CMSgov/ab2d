import boto3
import json


def lambda_handler(event, context):
    jenkins_agent_instance_ids = []
    region = 'us-east-1'
    ec2 = boto3.client('ec2', region_name=region)
    response = ec2.describe_instances()
    for reservation in response['Reservations']:
        for instance in reservation['Instances']:
            instance_id = instance['InstanceId']
            for tags in instance['Tags']:
                if tags["Key"] == "Name":
                    instance_name = tags["Value"]
                    if instance_name.find('jenkins-agent') != -1:
                        jenkins_agent_instance_ids.append(instance_id)
    ec2.stop_instances(InstanceIds=jenkins_agent_instance_ids)
    print('Stopped jenkins agents: ' + str(jenkins_agent_instance_ids))
    return {
        'statusCode': 200,
        'body': json.dumps('Jenkins agents stopped')
    }
