#!/bin/bash

set -e #Exit on first error
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

echo ""
echo "****************************************************"
echo "Retreiving logs from option $REPLY ($opt)"
echo "****************************************************"
echo ""

# Echo environment settings

if [ $REPLY -lt 4 ]; then
  echo "AWS_PROFILE=${AWS_PROFILE}"
  echo "SSH_PRIVATE_KEY=${SSH_PRIVATE_KEY}"
  echo ""
fi

# Verify AWS profile exists

# Verify SSH private key exists

# Get the controller private ip address

# Get the API count of the dev environment

# Get the worker count of the dev environment
