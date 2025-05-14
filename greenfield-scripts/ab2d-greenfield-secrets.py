# This script queries AWS for secrets starting with 'ab2d/ab2d-dev/*' and creates a new secret 'ab2d/dev/*' with same secret value
# Note this is hardcoded for dev - need to parameterize script or hardcode values for other environments
import subprocess
import json

LOG_CMD=True
DEBUG=False
DRY_RUN=False

def run(command):
    if LOG_CMD:
        print(f"*** {command}")
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE)
    p.wait()

next_token=None
out_file="out.txt"
while True:
    if next_token is None:
        run(f"aws secretsmanager batch-get-secret-value --filters Key=name,Values='ab2d/ab2d-dev' > {out_file}")
    else:
        run(f"aws secretsmanager batch-get-secret-value --filters Key=name,Values='ab2d/ab2d-dev' --next-token {next_token} > {out_file}")

    with open(out_file, 'r') as f:
        document = json.load(f)
        next_token=document.get('NextToken')
        secrets = document.get('SecretValues')

        if not secrets:
            break

        for secret in secrets:
            name=secret['Name']
            value=secret['SecretString']

            # replace first occurrence only
            new_name=name.replace("ab2d-dev","dev",1)

            if DEBUG:
                print(f"name={name}")
                print(f"new_name={new_name}")
                print(f"value={value}")
                print("----------")

            command=f"aws secretsmanager create-secret --name \"{new_name}\" --secret-string \"{value}\""
            if DRY_RUN:
                print(command)
                print("----------")
            else:
                run(command)

    if not next_token:
            break

run(f"rm {out_file}")

