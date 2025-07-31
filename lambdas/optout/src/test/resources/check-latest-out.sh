#!/bin/bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

latest=$(aws s3api list-objects-v2 \
  --bucket bfd-test-eft \
  --prefix bfdeft01/ab2d/out/T#EFT.ON.AB2D.NGD.CONF \
  --query 'sort_by(Contents, &LastModified)[-1].Key' \
  --output=text)
aws s3 cp --no-progress s3://bfd-test-eft/$latest test-confirmation.txt
bash "$SCRIPT_DIR/check-conf-file.sh" test-confirmation.txt
