#!/usr/bin/env bash
# Tear down the local AB2D harness
# Pass -v / --volumes to also remove the docker-compose volume

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
cd "$ROOT"

COMPOSE_FILES=(-f docker-compose.yml -f docker-compose.local.yml)

VOLUMES=0
for arg in "$@"; do
  case "$arg" in
    -v|--volumes) VOLUMES=1 ;;
    *) echo "Unknown flag: $arg" >&2; exit 2 ;;
  esac
done

# Pass through the same defaults up.sh uses so compose doesn't warn.
export API_PORT="${API_PORT:-8443}"
export AB2D_BFD_KEYSTORE_LOCATION="${AB2D_BFD_KEYSTORE_LOCATION:-}"
export AB2D_BFD_KEYSTORE_PASSWORD="${AB2D_BFD_KEYSTORE_PASSWORD:-}"

log() { printf '[down] %s\n' "$*"; }

log "Stopping stack..."
if [ "$VOLUMES" -eq 1 ]; then
  docker compose "${COMPOSE_FILES[@]}" down -v --remove-orphans
  log "Volume removed"
else
  docker compose "${COMPOSE_FILES[@]}" down --remove-orphans
fi

log "Done."
