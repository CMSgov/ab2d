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

