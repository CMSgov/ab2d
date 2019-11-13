#!/bin/bash

#
# Change to working directory
#

START_DIR="$( cd "$(dirname "$0")" ; pwd -P )"
cd "${START_DIR}"
cd ../terraform

#  Do a linting pass on each terraform file
for tfile in $(find . -exec ls -dl \{\} \; | awk '{print $10}' | grep ".tf" | grep -v ".tfstate" | grep -v ".sh" | grep -v ".tfvars")
do
   echo "Checking file: $tfile"
   tflint $tfile
done
echo "*******************************************************************"
echo "Check the output above. If there are no errors, all terraform files"
echo "have passed the linting pass."
echo "*******************************************************************"


