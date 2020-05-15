#!/bin/bash

# set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Retrieve user input
#

# Ask user of chooose an environment

echo ""
echo "--------------------------"
echo "Choose desired AWS account"
echo "--------------------------"
echo ""
PS3='Please enter your choice: '
options=("Dev AWS account" "Sbx AWS account" "Impl AWS account" "Prod AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Dev AWS account")
	    export AWS_ACCOUNT_NUMBER=349849222861
	    export CMS_ENV=ab2d-dev
	    SSH_PRIVATE_KEY=ab2d-dev.pem
	    break
            ;;
        "Sbx AWS account")
	    export AWS_ACCOUNT_NUMBER=777200079629
	    export CMS_ENV=ab2d-sbx-sandbox
	    SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    break
            ;;
        "Impl AWS account")
	    export AWS_ACCOUNT_NUMBER=330810004472
	    export CMS_ENV=ab2d-east-impl
	    SSH_PRIVATE_KEY=ab2d-east-impl.pem
	    break
            ;;
        "Prod AWS account")
	    export AWS_ACCOUNT_NUMBER=595094747606
	    export CMS_ENV=ab2d-east-prod
	    SSH_PRIVATE_KEY=ab2d-east-prod.pem
	    break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

if [ $REPLY -eq 5 ]; then
  exit 0
fi

#
# Acquire temporary credentials via CloudTamer API
#

if [ -z $CLOUDTAMER_USER_NAME ] || [ -z $CLOUDTAMER_PASSWORD ]; then
  echo ""
  echo "----------------------------"
  echo "Enter CloudTamer credentials"
  echo "----------------------------"
fi

if [ -z $CLOUDTAMER_USER_NAME ]; then
  echo ""
  echo "Enter your CloudTamer user name (EUA ID):"
  read CLOUDTAMER_USER_NAME
fi

if [ -z $CLOUDTAMER_PASSWORD ]; then
  echo ""
  echo "Enter your CloudTamer password:"
  read CLOUDTAMER_PASSWORD
fi

echo ""
echo "--------------------"
echo "Getting bearer token"
echo "--------------------"
echo ""

BEARER_TOKEN=$(curl --location --request POST 'https://cloudtamer.cms.gov/api/v2/token' \
  --header 'Accept: application/json' \
  --header 'Accept-Language: en-US,en;q=0.5' \
  --header 'Content-Type: application/json' \
  --data-raw "{\"username\":\"${CLOUDTAMER_USER_NAME}\",\"password\":\"${CLOUDTAMER_PASSWORD}\",\"idms\":{\"id\":2}}" \
  | jq --raw-output ".data.access.token")

if [ "${BEARER_TOKEN}" == "null" ]; then
  echo "**********************************************************************************************"
  echo "ERROR: Retrieval of bearer token failed."
  echo ""
  echo "Do you need to update your "CLOUDTAMER_PASSWORD" environment variable?"
  echo ""
  echo "Have you been locked out due to the failed password attempts?"
  echo ""
  echo "If you have gotten locked out due to failed password attempts, do the following:"
  echo "1. Go to this site:"
  echo "   https://jiraent.cms.gov/servicedesk/customer/portal/13"
  echo "2. Select 'CMS Cloud Access Request'"
  echo "3. Configure page as follows:"
  echo "   - Summary: CloudTamer & CloudVPN account password reset for {your eua id}"
  echo "   - CMS Business Unit: OEDA"
  echo "   - Project Name: Project 058 BCDA"
  echo "   - Types of Access/Resets: Cisco AnyConnect and AWS Console Password Resets [not MFA]"
  echo "   - Approvers: Stephen Walter"
  echo "   - Description"
  echo "     I am locked out of VPN access due to failed password attempts."
  echo "     Can you reset my CloudTamer & CloudVPN account password?"
  echo "     EUA: {your eua id}"
  echo "     email: {your email}"
  echo "     cell phone: {your cell phone number}"
  echo "4. After you submit your ticket, call the following number and give them your ticket number."
  echo "   888-533-4777"
  echo "**********************************************************************************************"
  echo ""
  exit 1
fi

echo ""
echo "-----------------------------"
echo "Getting temporary credentials"
echo "-----------------------------"
echo ""

JSON_OUTPUT=$(curl --location --request POST 'https://cloudtamer.cms.gov/api/v3/temporary-credentials' \
  --header 'Accept: application/json' \
  --header 'Accept-Language: en-US,en;q=0.5' \
  --header 'Content-Type: application/json' \
  --header "Authorization: Bearer ${BEARER_TOKEN}" \
  --header 'Content-Type: application/json' \
  --data-raw "{\"account_number\":\"${AWS_ACCOUNT_NUMBER}\",\"iam_role_name\":\"ab2d-spe-developer\"}" \
  | jq --raw-output ".data")

# Get temporary AWS credentials

export AWS_ACCESS_KEY_ID=$(echo $JSON_OUTPUT | jq --raw-output ".access_key")
export AWS_SECRET_ACCESS_KEY=$(echo $JSON_OUTPUT | jq --raw-output ".secret_access_key")

# Get AWS session token (required for temporary credentials)

export AWS_SESSION_TOKEN=$(echo $JSON_OUTPUT | jq --raw-output ".session_token")

# Verify temporary AWS credentials

if [ -z "${AWS_ACCESS_KEY_ID}" ] \
    || [ -z "${AWS_SECRET_ACCESS_KEY}" ] \
    || [ -z "${AWS_SESSION_TOKEN}" ]; then
  echo "**********************************************************************"
  echo "ERROR: AWS credentials do not exist for the ${CMS_ENV} AWS account"
  echo "**********************************************************************"
  echo ""
  exit 1
fi

# Verify SSH private key exists

if [ ! -f "${HOME}/.ssh/${SSH_PRIVATE_KEY}" ]; then
  echo "**********************************************************************"
  echo "ERROR: SSH private key for ${CMS_ENV} do not exist..."
  echo "**********************************************************************"
  echo ""
  exit 1
fi

# Get the private IP addresses of API nodes

aws --region us-east-1 ec2 describe-instances \
  --filters "Name=tag:Name,Values=${CMS_ENV}-api" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
  --output text \
  > "/tmp/${CMS_ENV}-api-nodes.txt"

# Get the private IP addresses of worker nodes

aws --region us-east-1 ec2 describe-instances \
  --filters "Name=tag:Name,Values=${CMS_ENV}-worker" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
  --output text \
  > "/tmp/${CMS_ENV}-worker-nodes.txt"

# Delete old logs

rm -f ${HOME}/Downloads/messages-api-node-*.txt
rm -f ${HOME}/Downloads/messages-worker-node-*.txt

# Get logs from API nodes

input="/tmp/${CMS_ENV}-api-nodes.txt"
COUNTER=0
while IFS= read -r IP_ADDRESS
do
  COUNTER=$((COUNTER+1))
  nc -v -z -G 5 "${IP_ADDRESS}" 22 &> /dev/null
  if [ $? -ne 0 ]; then
    echo ""
    echo "**************************************************************************"
    echo "ERROR: VPN access to API node $COUNTER ($IP_ADDRESS) could not be verified"
    echo "**************************************************************************"
    echo ""
    exit 1
  fi
  echo ""
  echo "-------------------------------------------------------"
  echo "Getting log from API node $COUNTER ($IP_ADDRESS)"
  echo "-------------------------------------------------------"
  echo ""
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo cp /var/log/messages /home/ec2-user \
    < /dev/null
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo chown ec2-user:ec2-user /home/ec2-user/messages \
    < /dev/null
  scp -i ~/.ssh/${SSH_PRIVATE_KEY} \
    "ec2-user@${IP_ADDRESS}:~/messages" \
    "${HOME}/Downloads/messages-api-node-${IP_ADDRESS}.txt" \
    < /dev/null
done < "$input"
API_COUNT=$COUNTER

# Get logs from worker nodes

input="/tmp/${CMS_ENV}-worker-nodes.txt"
COUNTER=0
while IFS= read -r IP_ADDRESS
do
  COUNTER=$((COUNTER+1))
  nc -v -z -G 5 "${IP_ADDRESS}" 22 &> /dev/null
  if [ $? -ne 0 ]; then
    echo ""
    echo "*****************************************************************************"
    echo "ERROR: VPN access to worker node $COUNTER ($IP_ADDRESS) could not be verified"
    echo "*****************************************************************************"
    echo ""
    exit 1
  fi
  echo ""
  echo "-------------------------------------------------------"
  echo "Getting log from Worker node $COUNTER ($IP_ADDRESS)"
  echo "-------------------------------------------------------"
  echo ""
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo cp /var/log/messages /home/ec2-user \
    < /dev/null
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo chown ec2-user:ec2-user /home/ec2-user/messages \
    < /dev/null
  scp -i ~/.ssh/${SSH_PRIVATE_KEY} \
    "ec2-user@${IP_ADDRESS}:~/messages" \
    "${HOME}/Downloads/messages-worker-node-${IP_ADDRESS}.txt" \
    < /dev/null
done < "$input"
WORKER_COUNT=$COUNTER

# Determine the logs downloaded clount

if [ $API_COUNT -eq 0 ] && [ $WORKER_COUNT -eq 0 ]; then
  LOGS_DOWNLOADED_COUNT=0
else
  LOGS_DOWNLOADED_COUNT="$((API_COUNT+WORKER_COUNT))"
fi

# Create summary

if [ $REPLY -lt 4 ]; then

  echo ""
  echo "**********************************************"
  echo "ENVIRONMENT SUMMARY"
  echo "**********************************************"
  echo ""
  echo "CMS_ENV=${CMS_ENV}"
  echo "SSH_PRIVATE_KEY=${SSH_PRIVATE_KEY}"
  echo "API_COUNT=${API_COUNT}"
  echo "WORKER_COUNT=${WORKER_COUNT}"
  echo "LOGS_DOWNLOADED_COUNT=${LOGS_DOWNLOADED_COUNT}"

  echo ""
  echo "**********************************************"
  echo "LOGS DOWNLOADED"
  echo "**********************************************"
  echo ""
  if [ $API_COUNT -eq 0 ] && [ $WORKER_COUNT -eq 0 ]; then
    echo "NONE"
  fi
  if [ $API_COUNT -ge 1 ]; then
    for filename in $HOME/Downloads/messages-api-node-*.txt; do	  
      BASE_NAME=$(basename "${filename}")	  
      echo "~/Downloads/${BASE_NAME}"
    done
  fi
  if [ $WORKER_COUNT -ge 1 ]; then
    for filename in $HOME/Downloads/messages-worker-node-*.txt; do
      BASE_NAME=$(basename "${filename}")	  
      echo "~/Downloads/${BASE_NAME}"
    done
  fi
  echo ""

fi
