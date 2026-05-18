#!/usr/bin/env bash
# Starts the local harness with docker compose + seed scripts

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
cd "$ROOT"

COMPOSE_FILES=(-f docker-compose.yml -f docker-compose.local.yml)
API_URL="https://localhost:${API_PORT:-8443}"
MOCK_URL="http://localhost:9999"

export API_PORT="${API_PORT:-8443}"
# will be empty for local
export NEW_RELIC_LICENSE_KEY="${NEW_RELIC_LICENSE_KEY:-}"
export NEW_RELIC_APP_NAME="${NEW_RELIC_APP_NAME:-local}"
export AB2D_KEYSTORE_LOCATION="${AB2D_KEYSTORE_LOCATION:-}"
export AB2D_KEYSTORE_PASSWORD="${AB2D_KEYSTORE_PASSWORD:-}"
export AB2D_KEY_ALIAS="${AB2D_KEY_ALIAS:-}"
export HPMS_AUTH_KEY_ID="${HPMS_AUTH_KEY_ID:-}"
export HPMS_AUTH_KEY_SECRET="${HPMS_AUTH_KEY_SECRET:-}"
export AB2D_HPMS_URL="${AB2D_HPMS_URL:-http://localhost}"
export AB2D_V2_ENABLED="${AB2D_V2_ENABLED:-false}"
export AB2D_V3_ENABLED="${AB2D_V3_ENABLED:-false}"

# suppress warnings
export AB2D_BFD_KEYSTORE_LOCATION="${AB2D_BFD_KEYSTORE_LOCATION:-}"
export AB2D_BFD_KEYSTORE_PASSWORD="${AB2D_BFD_KEYSTORE_PASSWORD:-}"

log() { printf '[up] %s\n' "$*"; }

if [ ! -d /opt/ab2d ]; then
  log "Creating /opt/ab2d (requires sudo)..."
  sudo mkdir -p /opt/ab2d
  sudo chmod 777 /opt/ab2d
fi

# find or build contracts image
if ! docker image inspect ab2d-contracts-local:latest >/dev/null 2>&1; then
  CONTRACTS_DIR="${CONTRACTS_DIR:-$ROOT/contracts}"
  if [ -f "$CONTRACTS_DIR/Dockerfile" ]; then
    if ! ls "$CONTRACTS_DIR"/build/libs/contracts-*.jar >/dev/null 2>&1; then
      log "Building contracts jar..."
      (cd "$CONTRACTS_DIR" && ./gradlew --no-daemon -x test build)
    fi
    log "Building ab2d-contracts-local:latest from $CONTRACTS_DIR ..."
    docker build -t ab2d-contracts-local:latest "$CONTRACTS_DIR"
  else
    log "ERROR: ab2d-contracts-local:latest missing" >&2
    exit 1
  fi
fi

log "Starting..."
docker compose "${COMPOSE_FILES[@]}" up -d

wait_for() {
  local name="$1" url="$2"
  shift 2
  local extra_args=("$@")
  local deadline=$((SECONDS + 180))
  log "Waiting for $name at $url ..."
  until curl -sf ${extra_args[@]+"${extra_args[@]}"} -o /dev/null "$url"; do
    if [ $SECONDS -ge $deadline ]; then
      log "ERROR: $name did not become ready within 180s"
      docker compose "${COMPOSE_FILES[@]}" ps
      exit 1
    fi
    sleep 2
  done
  log "$name ready."
}

wait_for "mock-bfd (expectations loaded)" "$MOCK_URL/v1/fhir/metadata"
wait_for "API"                            "$API_URL/health" -k

log "Applying seed-contract.sql..."
docker compose "${COMPOSE_FILES[@]}" exec -T db \
  psql -v ON_ERROR_STOP=1 -U ab2d -d ab2d < local-dev/sql/seed-contract.sql >/dev/null

log "Applying seed-coverage.sql..."
docker compose "${COMPOSE_FILES[@]}" exec -T db \
  psql -v ON_ERROR_STOP=1 -U ab2d -d ab2d < local-dev/sql/seed-coverage.sql >/dev/null

log "Submit a job: local-dev/submit-job.sh"
