#!/usr/bin/env bash
# Submit an export job, poll until complete, download the output
# submit-job.sh [--version v1|v2|v3] default v1
#
#   prototype requires the flag pause-resume.prototype.enabled=true and seeded V3 data
#   do this locally with:
#   docker exec ab2d-db-1 psql -U ab2d -d ab2d \
#  -c "UPDATE property.properties SET value='true' WHERE key='pause-resume.prototype.enabled';"
#   Seed v3 benes:
#   docker exec -i ab2d-db-1 psql -U ab2d -d ab2d < local-dev/sql/seed-v3.sql


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

# --- prototype path -----
DB_CONTAINER="${DB_CONTAINER:-ab2d-db-1}"
DB_USER="${DB_USER:-ab2d}"
DB_NAME="${DB_NAME:-ab2d}"
CONTRACT="${CONTRACT:-Z0001}"
ORG="${ORG:-Local Test Org}"

WORKER_CONTAINER="${WORKER_CONTAINER:-ab2d-worker-1}"
EFS_MOUNT="${EFS_MOUNT:-/opt/ab2d}"

psql_q() { docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$1"; }

submit_v3() {
  local uuid="33333333-$(date +%s)-${RANDOM}"

  log "Inserting V3 job for contract $CONTRACT ..."
  psql_q "INSERT INTO ab2d.job (id, job_uuid, created_at, status, fhir_version, started_by,
                                contract_number, organization, resource_types, output_format, progress)
          VALUES ((SELECT COALESCE(MAX(id),0)+1 FROM ab2d.job), '$uuid', now(), 'SUBMITTED', 'R4V3', 'PDP',
                  '$CONTRACT', '$ORG', 'ExplanationOfBenefit', 'application/fhir+ndjson', 0);" >/dev/null
  log "Job UUID: $uuid"

  log "Polling job status ..."
  local status="" tries=0
  while :; do
    status="$(psql_q "SELECT status FROM ab2d.job WHERE job_uuid='$uuid'")"
    case "$status" in
      SUCCESSFUL|FAILED|CANCELLED) break ;;
    esac
    tries=$(( tries + 1 ))
    if [ "$tries" -gt 30 ]; then echo; log "Timed out waiting (last status: ${status:-none})"; break; fi
    printf '.'
    sleep "$POLL_SECONDS"
  done
  echo

  local msg
  msg="$(psql_q "SELECT COALESCE(status_message,'') FROM ab2d.job WHERE job_uuid='$uuid'")"
  log "Final status: ${status:-unknown}${msg:+ - $msg}"

  log "Partition tasks created (Spring Batch step executions):"
  docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
    "SELECT se.step_name, se.status
     FROM ab2d.batch_step_execution se
     JOIN ab2d.batch_job_execution_params p ON p.job_execution_id = se.job_execution_id
     WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = '$uuid'
     ORDER BY se.step_execution_id;"

  if [ "$status" = "SUCCESSFUL" ]; then
    local dest="$OUT_DIR/$uuid"
    # copy the files out of the docker volume so we can see them in the local directory
    # only needed if we're running the worker in docker
    local src=""
    if ls "$dest/finished"/*.ndjson >/dev/null 2>&1; then
      src="$dest/finished"
      log "files already present locally in $src ..."
    else
      mkdir -p "$dest"
      log "Copying files from $WORKER_CONTAINER:$EFS_MOUNT/$uuid/finished -> $dest ..."
      if docker cp "$WORKER_CONTAINER:$EFS_MOUNT/$uuid/finished/." "$dest/" 2>/dev/null; then
        src="$dest"
      else
        log "  (nothing to copy)"
      fi
    fi
    if [ -n "$src" ]; then
      for f in "$src"/*.ndjson; do
        [ -e "$f" ] || { log "  (no files produced)"; break; }
        log "  $(basename "$f"): $(wc -c < "$f" | tr -d ' ') bytes, $(wc -l < "$f" | tr -d ' ') line(s)"
      done
    fi
  fi

  [ "$status" = "SUCCESSFUL" ] || exit 1
}

if [ "$VERSION" = "v3" ]; then
  submit_v3
  log "Done. Files in $OUT_DIR/"
  exit 0
fi

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
