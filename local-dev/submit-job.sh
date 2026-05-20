#!/usr/bin/env bash
# Submit an export job, poll until complete, download the output
# submit-job.sh [--v v1|v2] default v1


set -euo pipefail

VERSION="v1"
while [ $# -gt 0 ]; do
  case "$1" in
    -v|--version)
      [ $# -ge 2 ] || { echo "error: $1 requires an argument (v1|v2|v3)" >&2; exit 2; }
      VERSION="$2"
      shift 2
      ;;
    *)
      echo "error: unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

API="${API:-https://localhost:8443}"
TOKEN="${TOKEN:-local-dev-token}"
TYPE="${TYPE:-ExplanationOfBenefit}"
OUT_DIR="${OUT_DIR:-local-dev/out}"
POLL_SECONDS="${POLL_SECONDS:-10}"
API_BASE="/api/$VERSION/fhir"

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
cd "$ROOT"

mkdir -p "$OUT_DIR"

log() { printf '[job] %s\n' "$*"; }

log "GET $API_BASE/Patient/\$export ($TYPE) ..."
RAW="$(curl -ski -X GET "$API$API_BASE/Patient/\$export?_type=$TYPE" \
  -H 'Accept: application/fhir+json' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Prefer: respond-async')"

# Content-Location header looks like "Content-Location: https://host/api/v1/fhir/Job/<id>/$status"
JOB_ID="$(printf '%s' "$RAW" | tr -d '\r' \
  | sed -nE 's|^[Cc]ontent-[Ll]ocation:.*/Job/([^/]+)/.*|\1|p')"

if [ -z "$JOB_ID" ]; then
  echo "[job] Could not extract Job ID. Response:" >&2
  echo "$RAW" >&2
  exit 1
fi
log "Job ID: $JOB_ID"

STATUS_FILE="$OUT_DIR/status-$JOB_ID.json"

sleep "$POLL_SECONDS"
backoff="$POLL_SECONDS"
while :; do
  CODE="$(curl -sk -o "$STATUS_FILE" -w '%{http_code}' \
    "$API$API_BASE/Job/$JOB_ID/\$status" \
    -H "Authorization: Bearer $TOKEN")"
  case "$CODE" in
    200)
      log "Job complete."
      break
      ;;
    202)
      printf '.'
      sleep "$backoff"
      ;;
    429)
      printf 'r'
      backoff=$(( backoff * 2 ))
      if [ "$backoff" -gt 60 ]; then
        backoff=60
      fi
      sleep "$backoff"
      ;;
    *)
      echo
      echo "[job] Unexpected HTTP $CODE:" >&2
      cat "$STATUS_FILE" >&2
      exit 1
      ;;
  esac
done
echo

if ! command -v jq >/dev/null 2>&1; then
  log "Install 'jq' to download files automatically"
  exit 0
fi

URLS_RAW="$(jq -r '.output[]?.url // empty' "$STATUS_FILE")"
if [ -z "$URLS_RAW" ]; then
  log "No file URLs"
  cat "$STATUS_FILE"
  exit 1
fi

while IFS= read -r url; do
  fname="$(basename "$url")"
  dest="$OUT_DIR/$fname"
  # recreate api url to match
  without_scheme="${url#*://}"
  path="/${without_scheme#*/}"
  log "GET $path -> $dest"
  curl -sk "$API$path" \
    -H 'Accept: application/fhir+ndjson' \
    -H "Authorization: Bearer $TOKEN" \
    -o "$dest"
  log "  $(wc -c < "$dest" | tr -d ' ') bytes, $(wc -l < "$dest" | tr -d ' ') line(s)"
done <<<"$URLS_RAW"

log "Done. Files in $OUT_DIR/"
