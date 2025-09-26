#!/usr/bin/env bash

#Usage:
#chmod +x 02_push_images.sh
#./02_push_images.sh

set -euo pipefail

: "${AWS_PROFILE:?Missing AWS_PROFILE}"
: "${AWS_REGION:?Missing AWS_REGION}"
: "${AWS_ACCOUNT_ID:?Missing AWS_ACCOUNT_ID}"
: "${REPOS:?Missing REPOS}"

ECR="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

aws ecr get-login-password --region "$AWS_REGION" \
| docker login --username AWS --password-stdin "${ECR}" >/dev/null

for REPO in $REPOS; do
  echo ">>> Pushing tags back to ${REPO}"
  # discover local tags matching this repo
  mapfile -t LOCAL < <(docker images --format '{{.Repository}}:{{.Tag}}' \
    | grep -E "^${ECR}/${REPO}:" || true)

  if ((${#LOCAL[@]}==0)); then
    echo "    (no local images found for ${REPO})"
    continue
  fi

  for REF in "${LOCAL[@]}"; do
    echo "    pushing ${REF}"
    docker push "${REF}"
  done
done

echo ">>> Push complete."
