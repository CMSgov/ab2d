#!/usr/bin/env bash

fn_get_token()
{
  IDP_URL=$1
  TOKEN=$2

  BEARER_TOKEN=$(curl -X POST "$IDP_URL?grant_type=client_credentials&scope=clientCreds" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -H "Accept: application/json" \
      -H "Authorization: Basic ${TOKEN}" | jq --raw-output ".access_token")

  echo "$BEARER_TOKEN"
}