#!/bin/bash

set -e #Exit on first error
# set -x #Be verbose

#
# Check that there is a local "_site" directory in the current directory
#

if [ ! -d "_site" ]; then
  echo ""
  echo "***************************************************************"
  echo "ERROR: There is no '_site' directory in the current directory."
  echo ""
  echo "Change to the directory that has the desired '_site' directory."
  echo "***************************************************************"
  echo ""
fi

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"
