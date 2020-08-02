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

# Verify AWS credentials exist

if [ ! -f "${HOME}/.aws/credentials" ]; then
  echo "***********************************"
  echo "ERROR: AWS credentials do not exist"
  echo "***********************************"
  echo ""
  exit 1
fi

# Verify AWS profile exists

aws_credentials_parser "${HOME}/.aws/credentials"
aws_credentials.section.ab2d-dev

if [ -z "${aws_access_key_id}" ] || [ -z "${aws_secret_access_key}" ]; then
  echo "**********************************************************************"
  echo "ERROR: AWS credentials do not exist for the ${AWS_PROFILE} AWS profile"
  echo "**********************************************************************"
  echo ""
  exit 1
fi

# Verify SSH private key exists

if [ ! -f "${HOME}/.ssh/${SSH_PRIVATE_KEY}" ]; then
  echo "**********************************************************************"
  echo "ERROR: SSH private key for ${AWS_PROFILE} do not exist..."
  echo "**********************************************************************"
  echo ""
  exit 1
fi

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

if [ $opt == 'API' ]; then

  # Get the private IP addresses of API nodes
  
  aws --region us-east-1 ec2 describe-instances \
    --filters "Name=tag:Name,Values=${AWS_PROFILE}-api" \
    --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
    --output text \
    > "/tmp/${AWS_PROFILE}-api-nodes.txt"

  echo ""
  IFS=$'\n' read -d '' -r -a API_NODES < /tmp/${AWS_PROFILE}-api-nodes.txt
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
    --filters "Name=tag:Name,Values=${AWS_PROFILE}-worker" \
    --query="Reservations[*].Instances[?State.Name == 'running'].PrivateIpAddress" \
    --output text \
    > "/tmp/${AWS_PROFILE}-worker-nodes.txt"

  echo ""
  IFS=$'\n' read -d '' -r -a WORKER_NODES < /tmp/${AWS_PROFILE}-worker-nodes.txt
  PS3="Please enter the desired worker node number: "
  select IP_ADDRESS_SELECTED in "${WORKER_NODES[@]}"; do
    for ITEM in "${WORKER_NODES[@]}"; do
      if [[ $ITEM == $IP_ADDRESS_SELECTED ]]; then
        break 2
      fi
    done
  done

fi

if [ $opt == 'API' ] || [ $opt == 'Worker' ]; then

  nc -v -z -G 5 "${IP_ADDRESS_SELECTED}" 22 &> /dev/null
  if [ $? -eq 0 ]; then
    ssh -i "${HOME}/.ssh/${SSH_PRIVATE_KEY}" ec2-user@"${IP_ADDRESS_SELECTED}"
  else
    echo ""
    echo "**************************************************************************"
    echo "ERROR: VPN access to API node $COUNTER ($IP_ADDRESS) could not be verified"
    echo "**************************************************************************"
    echo ""
    exit 1
  fi

fi
