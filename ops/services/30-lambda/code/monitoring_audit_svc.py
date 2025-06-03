import base64
import gzip
import json
import os

from urllib.request import Request, urlopen

# Microservices webhook, connected to #ab2d-slack-alerts
SLACK_WEBHOOK = os.environ["SLACK_WEBHOOK_URL"]

def lambda_handler(event, context):
    payload = decode_logpayload(event)
    attachments = [{'text': f"{e['message']}", 'color': 'danger'} for e in payload['logEvents']]
    slack_msg = {
        'text': 'Audit Lambda Failed - cloudwatch_log_group: 'f"{payload['logGroup']}" ,
        'attachments': attachments
    }
  
    # post the message
    req = Request(SLACK_WEBHOOK, json.dumps(slack_msg).encode('utf-8'))
    urlopen(req)

def decode_logpayload(event):
    compressed_payload = base64.b64decode(event['awslogs']['data'])
    uncompressed_payload = gzip.decompress(compressed_payload)
    return json.loads(uncompressed_payload)
