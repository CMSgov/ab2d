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

aws_credentials_parser ()
{
    ini="$(<$1)"                # read the file
    ini="${ini//[/\[}"          # escape [
    ini="${ini//]/\]}"          # escape ]
    IFS=$'\n' && ini=( ${ini} ) # convert to line-array
    ini=( ${ini[*]//;*/} )      # remove comments with ;
    ini=( ${ini[*]/\    =/=} )  # remove tabs before =
    ini=( ${ini[*]/=\   /=} )   # remove tabs after =
    ini=( ${ini[*]/\ =\ /=} )   # remove anything with a space around =
    ini=( ${ini[*]/#\\[/\}$'\n'aws_credentials.section.} ) # set section prefix
    ini=( ${ini[*]/%\\]/ \(} )    # convert text2function (1)
    ini=( ${ini[*]/=/=\( } )    # convert item to array
    ini=( ${ini[*]/%/ \)} )     # close array parenthesis
    ini=( ${ini[*]/%\\ \)/ \\} ) # the multiline trick
    ini=( ${ini[*]/%\( \)/\(\) \{} ) # convert text2function (2)
    ini=( ${ini[*]/%\} \)/\}} ) # remove extra parenthesis
    ini[0]="" # remove first element
    ini[${#ini[*]} + 1]='}'    # add the last brace
    eval "$(echo "${ini[*]}")" # eval the result
}

#
# Retrieve user input
#

# Ask user of chooose an environment

echo ""
PS3='Please enter your choice: '
options=("Dev AWS account" "Sbx AWS account" "Impl AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Dev AWS account")
	    export AWS_PROFILE=ab2d-dev
	    SSH_PRIVATE_KEY=ab2d-dev.pem
	    break
            ;;
        "Sbx AWS account")
	    export AWS_PROFILE=ab2d-sbx-sandbox
	    SSH_PRIVATE_KEY=ab2d-sbx-sandbox.pem
	    break
            ;;
        "Impl AWS account")
	    export AWS_PROFILE=ab2d-east-impl
	    SSH_PRIVATE_KEY=ab2d-east-impl.pem
	    break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

if [ $REPLY -eq 4 ]; then
  exit 0
fi

echo ""
echo "Retreiving logs from option $REPLY ($opt)..."
echo ""

# Verify AWS credentials exist

if [ -f "${HOME}/.aws/credentials" ]; then
  echo "Verified AWS credentials exist..."
  echo ""
else
  echo "***********************************"
  echo "ERROR: AWS credentials do not exist"
  echo "***********************************"
  echo ""
  exit 1
fi

# Verify AWS profile exists

aws_credentials_parser "${HOME}/.aws/credentials"
aws_credentials.section.ab2d-dev

if [ -n "${aws_access_key_id}" ] && [ -n "${aws_secret_access_key}" ]; then
  echo "AWS credentials exist for the ${AWS_PROFILE} AWS profile..."
  echo ""
else
  echo "**********************************************************************"
  echo "ERROR: AWS credentials do not exist for the ${AWS_PROFILE} AWS profile"
  echo "**********************************************************************"
  echo ""
  exit 1
fi

# Verify SSH private key exists

if [ -f "${HOME}/.ssh/${SSH_PRIVATE_KEY}" ]; then
  echo "SSH private key for ${AWS_PROFILE} exists..."
  echo ""
else
  echo "**********************************************************************"
  echo "ERROR: SSH private key for ${AWS_PROFILE} do not exist..."
  echo "**********************************************************************"
  echo ""
  exit 1
fi

# Get the controller private ip address

CONTROLLER_PRIVATE_IP=$(aws --region us-east-1 ec2 describe-instances \
  --filters "Name=tag:Name,Values=ab2d-deployment-controller" \
  --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
  --output text)

if [ -n "${CONTROLLER_PRIVATE_IP}" ]; then
  echo "The private IP address of the controller for ${AWS_PROFILE} has been retrieved..."
  echo ""
else
  echo "*******************************************************************************************"
  echo "ERROR: The private IP address of the controller for ${AWS_PROFILE} could not be retrieved."
  echo "*******************************************************************************************"
  echo ""
  exit 1
fi

# Verify VPN access to controller

nc -v -z -G 5 "${CONTROLLER_PRIVATE_IP}" 22 &> /dev/null

if [ $? -eq 0 ]; then
  echo "VPN access to controller verified..."
  echo ""
else
  echo "*****************************************************"
  echo "ERROR: VPN access to controller could not be verified"
  echo "*****************************************************"
  echo ""
  exit 1
fi

# Get the private IP addresses of API nodes

ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${CONTROLLER_PRIVATE_IP}" \
  ./list-api-instances.sh \
  | grep "10." \
  | awk '{print $2}' \
  > "/tmp/${AWS_PROFILE}-api-nodes.txt"

# Get the private IP addresses of worker nodes

ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${CONTROLLER_PRIVATE_IP}" \
  ./list-worker-instances.sh \
  | grep "10." \
  | awk '{print $2}' \
  > "/tmp/${AWS_PROFILE}-worker-nodes.txt"

# Delete old logs

echo ""
echo "Delete existing API and worker logs in the 'Downloads' directory..."

rm -f ${HOME}/Downloads/messages-api-node-*.txt
rm -f ${HOME}/Downloads/messages-worker-node-*.txt

# Get logs from API nodes

input="/tmp/${AWS_PROFILE}-api-nodes.txt"
COUNTER=0
while IFS= read -r IP_ADDRESS
do
  echo "IP_ADDRESS=${IP_ADDRESS}"
  COUNTER=$((COUNTER+1))
  nc -v -z -G 5 "${IP_ADDRESS}" 22 &> /dev/null
  if [ $? -eq 0 ]; then
    echo ""
    echo "VPN access to API node $COUNTER ($IP_ADDRESS) verified..."
  else
    echo ""
    echo "**************************************************************************"
    echo "ERROR: VPN access to API node $COUNTER ($IP_ADDRESS) could not be verified"
    echo "**************************************************************************"
    echo ""
    exit 1
  fi
  echo ""
  echo "Getting log from API node $COUNTER ($IP_ADDRESS)..."
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo cp /var/log/messages /home/ec2-user \
    < /dev/null
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo chown ec2-user:ec2-user /home/ec2-user/messages \
    < /dev/null
  scp -i ~/.ssh/${SSH_PRIVATE_KEY} \
    "ec2-user@${IP_ADDRESS}:~/messages" \
    "${HOME}/Downloads/messages-api-node-${COUNTER}.txt" \
    < /dev/null
done < "$input"
API_COUNT=$COUNTER

# Get logs from worker nodes

input="/tmp/${AWS_PROFILE}-worker-nodes.txt"
COUNTER=0
while IFS= read -r IP_ADDRESS
do
  echo "IP_ADDRESS=${IP_ADDRESS}"
  COUNTER=$((COUNTER+1))
  nc -v -z -G 5 "${IP_ADDRESS}" 22 &> /dev/null
  if [ $? -eq 0 ]; then
    echo ""
    echo "VPN access to worker node $COUNTER ($IP_ADDRESS) verified..."
  else
    echo ""
    echo "*****************************************************************************"
    echo "ERROR: VPN access to worker node $COUNTER ($IP_ADDRESS) could not be verified"
    echo "*****************************************************************************"
    echo ""
    exit 1
  fi
  echo ""
  echo "Getting log from worker node $COUNTER ($IP_ADDRESS)..."
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo cp /var/log/messages /home/ec2-user \
    < /dev/null
  ssh -i "~/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS}" \
    sudo chown ec2-user:ec2-user /home/ec2-user/messages \
    < /dev/null
  scp -i ~/.ssh/${SSH_PRIVATE_KEY} \
    "ec2-user@${IP_ADDRESS}:~/messages" \
    "${HOME}/Downloads/messages-worker-node-${COUNTER}.txt" \
    < /dev/null
done < "$input"
WORKER_COUNT=$COUNTER

# Determine if logs were downloaded

if [ $API_COUNT -eq 0 ] && [ $WORKER_COUNT -eq 0 ]; then
  LOGS_DOWNLOADED=NO
else
  LOGS_DOWNLOADED=YES    
fi

# Echo environment settings

if [ $REPLY -lt 4 ]; then
  echo ""
  echo "**********************************************"
  echo "AWS_PROFILE=${AWS_PROFILE}"
  echo "SSH_PRIVATE_KEY=${SSH_PRIVATE_KEY}"
  echo "CONTROLLER_PRIVATE_IP=${CONTROLLER_PRIVATE_IP}"
  echo "API_COUNT=${API_COUNT}"
  echo "WORKER_COUNT=${WORKER_COUNT}"
  echo "LOGS_DOWNLOADED=${LOGS_DOWNLOADED}"
  echo "**********************************************"
  echo ""
fi
