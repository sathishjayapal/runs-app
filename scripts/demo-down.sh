#!/usr/bin/env bash
set -euo pipefail

# Stops the local runs-app demo started by scripts/demo-up.sh.
#   scripts/demo-down.sh           stop the app and the demo Postgres (data is kept)
#   scripts/demo-down.sh --wipe    also delete the demo database volume and .env.demo.local

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DEMO_APP_PORT="${DEMO_APP_PORT:-8080}"
PG_CONTAINER="runs-app-demo-postgres"
PG_VOLUME="runs-app-demo-pgdata"
MQ_CONTAINER="runs-app-demo-rabbitmq"
ENV_FILE="$ROOT/.env.demo.local"
PID_FILE="$ROOT/target/demo-app.pid"

WIPE=false
[ "${1:-}" = "--wipe" ] && WIPE=true

# No pipe into grep -q here: under pipefail an early-exiting grep can make the pipeline report failure.
container_exists() { [ -n "$(docker ps -a --filter "name=^${1}\$" --format '{{.Names}}')" ]; }

# Only the process that LISTENS on the demo port, plus the Maven wrapper that started it.
# A different app on another port is never touched.
if [ -f "$PID_FILE" ]; then
  kill "$(cat "$PID_FILE")" 2>/dev/null || true
  rm -f "$PID_FILE"
fi
pids="$(lsof -ti tcp:"$DEMO_APP_PORT" -sTCP:LISTEN 2>/dev/null || true)"
if [ -n "$pids" ]; then
  # shellcheck disable=SC2086
  kill $pids 2>/dev/null || true
  echo "✓ Stopped the app listening on port ${DEMO_APP_PORT}"
fi

# The demo broker holds no data worth keeping, so it is always removed.
if container_exists "$MQ_CONTAINER"; then
  docker rm -f "$MQ_CONTAINER" >/dev/null
  echo "✓ Demo RabbitMQ removed"
fi

if container_exists "$PG_CONTAINER"; then
  if $WIPE; then
    docker rm -f "$PG_CONTAINER" >/dev/null
    docker volume rm "$PG_VOLUME" >/dev/null 2>&1 || true
    rm -f "$ENV_FILE"
    echo "✓ Demo database, volume and .env.demo.local deleted"
  else
    docker stop "$PG_CONTAINER" >/dev/null
    echo "✓ Demo Postgres stopped (data kept; --wipe deletes it)"
  fi
elif $WIPE; then
  docker volume rm "$PG_VOLUME" >/dev/null 2>&1 || true
  rm -f "$ENV_FILE"
  echo "✓ Demo volume and .env.demo.local deleted"
fi
