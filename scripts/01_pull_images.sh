#!/usr/bin/env bash

#Usage:
#chmod +x 01_pull_images.sh
#./01_pull_images.sh

set -euo pipefail

: "${AWS_PROFILE:?Missing AWS_PROFILE}"
: "${AWS_REGION:?Missing AWS_REGION}"
: "${AWS_ACCOUNT_ID:?Missing AWS_ACCOUNT_ID}"
: "${REPOS:?Missing REPOS}"

ECR="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

aws ecr get-login-password --region "$AWS_REGION" \
| docker login --username AWS --password-stdin "${ECR}" >/dev/null

# Collect tags used by active ECS services across clusters
mapfile -t CLUSTERS < <(aws ecs list-clusters --query 'clusterArns[]' -o text)
declare -A INUSE_TAGS

get_tags_for_digest() {
  local repo="$1" digest="$2"
  # returns a space-separated list of tags for this digest (may be empty)
  aws ecr describe-images \
    --repository-name "$repo" \
    --image-ids imageDigest="$digest" \
    --query 'imageDetails[0].imageTags' \
    --output text 2>/dev/null || true
}

if ((${#CLUSTERS[@]})); then
  for CL in "${CLUSTERS[@]}"; do
    # Services (ACTIVE only)
    mapfile -t SERVICES < <(aws ecs list-services --cluster "$CL" --query 'serviceArns[]' -o text)
    if ((${#SERVICES[@]})); then
      # Describe in chunks of 10
      for ((i=0; i<${#SERVICES[@]}; i+=10)); do
        slice=( "${SERVICES[@]:i:10}" )
        DESC=$(aws ecs describe-services --cluster "$CL" --services "${slice[@]}")
        mapfile -t TDS < <(jq -r '.services[].taskDefinition' <<<"$DESC")
        for TD in "${TDS[@]}"; do
          TDJSON=$(aws ecs describe-task-definition --task-definition "$TD")
          mapfile -t IMAGES < <(jq -r '.taskDefinition.containerDefinitions[].image' <<<"$TDJSON")
          for IMG in "${IMAGES[@]}"; do
            # Expect formats:
            # <acct>.dkr.ecr.<region>.amazonaws.com/<repo>:<tag>
            # <acct>.dkr.ecr.<region>.amazonaws.com/<repo>@sha256:<digest>
            if [[ "$IMG" =~ ^${AWS_ACCOUNT_ID}\.dkr\.ecr\.${AWS_REGION}\.amazonaws\.com/([^:@]+)([:@].*)?$ ]]; then
              REPO="${BASH_REMATCH[1]}"
              if [[ " $REPOS " != *" $REPO "* ]]; then
                continue
              fi
              if [[ "$IMG" == *@sha256:* ]]; then
                DIGEST="${IMG##*@}"
                TAGS="$(get_tags_for_digest "$REPO" "$DIGEST")"
                [[ -n "$TAGS" ]] && INUSE_TAGS["$REPO"]="${INUSE_TAGS[$REPO]} $TAGS"
              elif [[ "$IMG" == *:* ]]; then
                TAG="${IMG##*:}"
                INUSE_TAGS["$REPO"]="${INUSE_TAGS[$REPO]} $TAG"
              fi
            fi
          done
        done
      done
    fi
  done
fi

# Build a tag list per repo (latest + release-like + ECS in-use)
for REPO in $REPOS; do
  echo ">>> Discovering tags for $REPO"
  mapfile -t ALL_TAGS < <(aws ecr list-images \
    --repository-name "$REPO" --filter tagStatus=TAGGED \
    --query 'imageIds[].imageTag' -o text | tr '\t' '\n' | sort -u)

  WANT=()
  for T in "${ALL_TAGS[@]}"; do
    if [[ "$T" == "latest" ]] \
       || [[ "$T" =~ ^v[0-9].* ]] \
       || [[ "$T" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-.*)?$ ]] \
       || [[ "$T" =~ ^release-.*$ ]] \
       || [[ "$T" =~ ^prod-.*$ ]] \
       || [[ "$T" =~ ^test-.*$ ]]; then
      WANT+=("$T")
    fi
  done
  # add ECS in-use tags
  if [[ -n "${INUSE_TAGS[$REPO]:-}" ]]; then
    for T in ${INUSE_TAGS[$REPO]}; do
      WANT+=("$T")
    done
  fi

  # unique
  mapfile -t WANT_UNIQ < <(printf "%s\n" "${WANT[@]}" | awk 'NF' | sort -u)

  if ((${#WANT_UNIQ[@]}==0)); then
    echo "    (no tags to back up)"
    continue
  fi

  # Pull tags locally
  for T in "${WANT_UNIQ[@]}"; do
    IMG="${ECR}/${REPO}:${T}"
    echo "    pulling ${IMG}"
    docker pull "${IMG}"
  done
done

echo ">>> Pull complete."
