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
  echo ""
  exit 0
fi

#
# Get Temporary AWS credentials via CloudTamer API
#

fn_get_temporary_aws_credentials_via_cloudtamer_api "${AWS_ACCOUNT_NUMBER}" "${CMS_ENV}"

#
# Verify SSH private key exists
#

if [ ! -f "${HOME}/.ssh/${SSH_PRIVATE_KEY}" ]; then
  echo "**********************************************************************"
  echo "ERROR: SSH private key for ${CMS_ENV} do not exist..."
  echo "**********************************************************************"
  echo ""
  exit 1
fi

#
# Choose node type
#

echo ""
echo "----------------"
echo "Choose node type"
echo "----------------"
echo ""
PS3='What type of node do you want to connect to?: '
options=("API" "Worker" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "API")
	    break
            ;;
        "Worker")
	    break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

#
# Choose desired node
#

if [ $opt == 'API' ] || [ $opt == 'Worker' ]; then
  echo ""
  echo "-------------------"
  echo "Choose desired node"
  echo "-------------------"
fi

if [ $opt == 'API' ]; then

  # Get the private IP addresses of API nodes
  
  aws --region us-east-1 ec2 describe-instances \
    --filters "Name=tag:Name,Values=${CMS_ENV}-api" \
    --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
    --output text \
    > "/tmp/${CMS_ENV}-api-nodes.txt"

  echo ""
  IFS=$'\n' read -d '' -r -a API_NODES < /tmp/${CMS_ENV}-api-nodes.txt
  PS3="Please enter the desired API node number: "
  select IP_ADDRESS_SELECTED in "${API_NODES[@]}"; do
    for ITEM in "${API_NODES[@]}"; do
      if [[ $ITEM == $IP_ADDRESS_SELECTED ]]; then
        break 2
      fi
    done
  done

fi

if [ $opt == 'Worker' ]; then
    
  # Get the private IP addresses of worker nodes
  
  aws --region us-east-1 ec2 describe-instances \
    --filters "Name=tag:Name,Values=${CMS_ENV}-worker" \
    --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
    --output text \
    > "/tmp/${CMS_ENV}-worker-nodes.txt"

  echo ""
  IFS=$'\n' read -d '' -r -a WORKER_NODES < /tmp/${CMS_ENV}-worker-nodes.txt
  PS3="Please enter the desired worker node number: "
  select IP_ADDRESS_SELECTED in "${WORKER_NODES[@]}"; do
    for ITEM in "${WORKER_NODES[@]}"; do
      if [[ $ITEM == $IP_ADDRESS_SELECTED ]]; then
        break 2
      fi
    done
  done

fi

if [ $REPLY -eq 3 ]; then
  echo ""
  exit 0
fi

if [ -z $IP_ADDRESS_SELECTED ]; then
  echo "**************************************************************************"
  echo "ERROR: There are no $opt nodes in that environment"
  echo "**************************************************************************"
  echo ""
  exit 1  
fi

if [ $opt == 'API' ] || [ $opt == 'Worker' ]; then

  nc -v -z -G 5 "${IP_ADDRESS_SELECTED}" 22 &> /dev/null
  if [ $? -eq 0 ]; then
    ssh -i "${HOME}/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS_SELECTED}"
  else
    echo ""
    echo "**************************************************************************"
    echo "ERROR: VPN access to $opt node $COUNTER ($IP_ADDRESS) could not be verified"
    echo "**************************************************************************"
    echo ""
    exit 1
  fi

fi
