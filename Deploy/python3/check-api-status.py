#!/usr/bin/env python3

import json
import requests


def lambda_handler(event, context):
    # print statement for local testing (not deployed to lambda)
    print(event + context)
    status = "Up"
    health_response = requests.get("https://api.ab2d.cms.gov/health")
    if health_response.status_code != 200:
        status = "Down"
    status_response = requests.get("https://api.ab2d.cms.gov/status")
    if status_response.status_code != 200:
        status = "Down"
    status_response_json = json.loads(status_response.text)
    if status_response_json['maintenanceMode'] == 'true':
        status = "Down"
    status_json_string = {"apiStatus": status}
    return {
        'statusCode': 200,
        'body': json.dumps(status_json_string)
    }


# print statement for local testing (not deployed to lambda)
print(lambda_handler('', ''))
