import boto3
import json
import os
import urllib.request
from urllib.request import Request


def lambda_handler(event, context):
    print(event)
    finding = event['detail']['findings'][0]
    finding_arn = finding['Id']
    productArn = finding['ProductArn']
    client = boto3.client('securityhub')
    slack_webhook_url = os.environ['SLACK_WEBHOOK_URL']
    # Check if this finding has already been notified to Slack and ignore if so
    if 'UserDefinedFields' in finding and 'SentToSlack' in finding['UserDefinedFields'] and finding['UserDefinedFields']['SentToSlack'] == "True":
        print("Finding already notified - not forwarding")
        return None
    # Format the message to be sent to Slack
    else:
        message = f"New SecurityHub finding detected: {finding['Title']}\nResource: {finding['Resources'][0]['Type']}\nID: {finding['Resources'][0]['Id']}\nSeverity: {finding['Severity']['Label']}\nEnvironment: {event['account']}\nRemediation: {finding['Remediation']}"
        headers = {'Content-Type': 'application/json'}
        payload = {'text': message}
        # Send the message to Slack via the webhook URL
        req = urllib.request.Request(slack_webhook_url, headers=headers, data=json.dumps(payload).encode('utf-8'))
        response = urllib.request.urlopen(req)
        response_text =response.read().decode('utf-8')
        # Mark the finding as notified to Slack by adding 'SentToSlack': True to the finding's UserDefinedFields
        result = client.batch_update_findings(FindingIdentifiers=[
            {
                'Id': finding_arn,
                'ProductArn': productArn
            }],
                UserDefinedFields={
                    'SentToSlack': 'True'
                }
            )
        print(result)
