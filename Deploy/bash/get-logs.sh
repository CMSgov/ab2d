#!/bin/bash

# set -e #Exit on first error
# set -x #Be verbose

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"

#
# Define functions
#

# Import the "get temporary AWS credentials via CloudTamer API" function

source "${START_DIR}/functions/fn_get_temporary_aws_credentials_via_cloudtamer_api.sh"

#
# Ask user of chooose an environment
#

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
# Get Temporary AWS credentials via CloudTamer API
#

fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"

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
