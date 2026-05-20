#!/bin/sh
# Registers BFD stub expectations against MockServer.
set -eu

MS="${MOCKSERVER_URL:-http://mock-bfd:1080}"
FIX="${FIXTURES_DIR:-/fixtures}"
CONTRACT="${CONTRACT:-Z0001}"
MOCK_PUBLIC_URL="${MOCK_PUBLIC_URL:-http://host.docker.internal:9999}"

echo "[mock-bfd-init] Waiting for MockServer at $MS ..."
i=0
until curl -sf -o /dev/null -X PUT "$MS/mockserver/status" 2>/dev/null; do
  i=$((i + 1))
  if [ $i -gt 60 ]; then
    echo "[mock-bfd-init] MockServer did not become ready in 60s — aborting" >&2
    exit 1
  fi
  sleep 1
done
echo "[mock-bfd-init] MockServer ready. Loading expectations ..."

# Clear existing expectations
curl -sS -X PUT "$MS/mockserver/reset" -o /dev/null

# eob_query <bene_id> [startIndex]
eob_query() {
  if [ -n "${2:-}" ]; then
    printf '{"patient":["%s"],"count":["10"],"startIndex":["%s"],"excludeSAMHSA":["true"]}' "$1" "$2"
  else
    printf '{"patient":["%s"],"excludeSAMHSA":["true"]}' "$1"
  fi
}

# coverage_query <month> <contract>
coverage_query() {
  printf '{"_has:Coverage.extension":["https://bluebutton.cms.gov/resources/variables/ptdcntrct%s|%s"]}' "$1" "$2"
}

# register <url-path> <query-json> <fixture-path>
# Body is base64-encoded so we don't have to JSON-escape file content in shell
register() {
  path=$1
  query=$2
  fixture=$3

  if [ ! -f "$FIX/$fixture" ]; then
    echo "[mock-bfd-init] WARN: fixture not found: $FIX/$fixture — skipping $path" >&2
    return 0
  fi

  # replace urls so worker can follow them
  body64=$(sed \
    -e "s|http://localhost:8083|$MOCK_PUBLIC_URL|g" \
    -e "s|https://prod-sbx.bfd.cms.gov|$MOCK_PUBLIC_URL|g" \
    "$FIX/$fixture" | base64 | tr -d '\n')

  query_field=""
  [ -n "$query" ] && query_field=",\"queryStringParameters\":$query"

  echo "  + GET $path ${query:+($query)}"
  curl -sS -X PUT "$MS/mockserver/expectation" \
    -H 'Content-Type: application/json' \
    --data-binary @- \
    -o /dev/null <<EOF
{
  "httpRequest": { "method": "GET", "path": "$path"$query_field },
  "httpResponse": {
    "statusCode": 200,
    "headers": { "Content-Type": ["application/json;charset=UTF-8"] },
    "body": { "type": "BINARY", "base64Bytes": "$body64" }
  }
}
EOF
}

# === V1 stubs ===

# STU3 CapabilityStatement
register "/v1/fhir/metadata" "" "v1/meta.json"

# Individual lookups by bene id.
for id in 20140000008325 20140000009893; do
  register "/v1/fhir/Patient/$id" "" "v1/patient/$id.json"
done

# Patient search by contract
for month in 01 02 03 04 05 06 07 08 09 10 11 12; do
  register "/v1/fhir/Patient" "$(coverage_query "$month" "$CONTRACT")" "v1/patient/bundle/patientbundle.json"
done

# EOB pagination
for startIndex in 10 20 30; do
  register "/v1/fhir/ExplanationOfBenefit" "$(eob_query 20140000008325 "$startIndex")" "v1/eob/20140000008325_$startIndex.json"
done

# EOB by patient — the first-page request from the worker.
for id in 20140000008325 20140000009893; do
  register "/v1/fhir/ExplanationOfBenefit" "$(eob_query "$id")" "v1/eob/$id.json"
done

# === V2 stubs ===

register "/v2/fhir/metadata" "" "v2/meta.json"

for id in 20140000008325 20140000009893; do
  register "/v2/fhir/ExplanationOfBenefit" "$(eob_query "$id")" "v2/eob/$id.json"
done

echo "[mock-bfd-init] Done."
