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

PS3='Please enter your choice: '
options=("Dev AWS account" "Sbx AWS account" "Impl AWS account" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Dev AWS account")          
	    break
            ;;
        "Sbx AWS account")
	    break
            ;;
        "Impl AWS account")
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

# Get the API count of the dev environment
