#!/usr/bin/env bash

if [ "$1" == "--help" ]
then
  printf \
"Usage: \n
  <command> (-prod | -sandbox) --auth <base64 username:password> [--contract <contract number>] [--directory <dir>]\n
Arguments:\n
  -sandbox -- if running against ab2d sandbox environment
  -prod -- if running against ab2d production environment
  --auth -- base64 encoded \"clientid:password\"
  --contract -- if searching specific contract then give contract number ex. Z0001
  --directory -- if you want files and job info saved to specific directory"
fi

# Process command line args
DIRECTORY=$(pwd)
while (($#)) ;
do
  case $1 in
    "-sandbox")
      export IDP_URL="https://test.idp.idm.cms.gov/oauth2/aus2r7y3gdaFMKBol297/v1/token"
      export API_URL="https://sandbox.ab2d.cms.gov/api/v1/fhir"
      ;;
    "-prod")
      export IDP_URL="https://idm.cms.gov/oauth2/aus2ytanytjdaF9cr297/v1/token"
      export API_URL="https://api.ab2d.cms.gov/api/v1/fhir"
      ;;
    "--auth")
      export AUTH=$2
      shift
      ;;
    "--contract")
      export CONTRACT=$2
      shift
      ;;
    "--directory")
      DIRECTORY=$2
      shift
      ;;
  esac
  shift
done

export DIRECTORY="$DIRECTORY"