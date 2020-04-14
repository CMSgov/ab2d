#!/bin/bash

set -e #Exit on first error
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

nc -z -v "${CONTROLLER_PRIVATE_IP}" 22 > "/tmp/verify-controller.txt" 2>&1
VERIFY_VPN_ACCESS_TO_CONTROLLER=$(cat /tmp/verify-controller.txt | grep "succeeded")

if [ -n "${VERIFY_VPN_ACCESS_TO_CONTROLLER}" ]; then
  echo "VPN access to controller verified..."
  echo ""
else
  echo "*****************************************************"
  echo "ERROR: VPN access to controller could not be verified"
  echo "*****************************************************"
  echo ""
  exit 1
fi

rm -f /tmp/verify-controller.txt

# Get the API count of the dev environment

# Get the worker count of the dev environment

# Echo environment settings

if [ $REPLY -lt 4 ]; then
  echo "**********************************************"
  echo "AWS_PROFILE=${AWS_PROFILE}"
  echo "SSH_PRIVATE_KEY=${SSH_PRIVATE_KEY}"
  echo "CONTROLLER_PRIVATE_IP=${CONTROLLER_PRIVATE_IP}"
  echo "VERIFY_VPN_ACCESS_TO_CONTROLLER=verified"
  echo "**********************************************"
  echo ""
fi
