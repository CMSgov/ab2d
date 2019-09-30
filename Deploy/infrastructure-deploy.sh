#!/bin/bash
set -e #Exit on first error
set -x #Be verbose


# Parse options
for i in "$@"
do
case $i in
  --environment=*)
  ENVIRONMENT="${i#*=}"
  shift # past argument=value
  ;;
  --ami=*)
  AMI_ID="${i#*=}"
  shift # past argument=value
  ;;
  --auto-approve)
  AUTOAPPROVE="true"
  shift # past argument=value
  ;;
esac
done


# Check vars are not empty before proceeding
if [ -z "${ENVIRONMENT}" ]; then
  echo "Try running the script like so:"
  echo "./infrastructure-deploy.sh --environment=prototype"
  exit 1
fi


# If no AMI is specified then create a new one
if [ -z "${AMI_ID}" ]; then
  cd packer/app/
  IP=$(curl ipinfo.io/ip)
  COMMIT=$(git rev-parse HEAD)
  packer build --var my_ip_address=$IP --var git_commit_hash=$COMMIT app.json  2>&1 | tee output.txt
  AMI_ID=$(cat output.txt | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }' | tail -1)
  cd ../../
fi


