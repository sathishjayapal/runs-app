#!/usr/bin/env bash
# Adds monitoring for the runs-ai-analyzer-db container (pgvector pg17, host port 5444) on runs-server.
# Second postgres_exporter instance (port 9188) + Prometheus scrape job. The existing PostgresExporter
# and PostgresPerApp rules apply automatically (they have no instance filter).
#
# Run ON THE VM as root (copy it over first so sudo can prompt for the password):
#   scp scripts/vm-setup-pg-exporter-5444.sh vm:/home/skminfotech/
#   ssh -t vm 'sudo bash /home/skminfotech/vm-setup-pg-exporter-5444.sh'
#
# Idempotent. Re-running rotates the exporter password and skips the scrape job if present.
# The password is generated on the VM, never printed, and never put on a command line.
# Prerequisite: scripts/vm-setup-pg-exporter.sh already ran (it creates the shared queries file).
set -euo pipefail

CONTAINER="${CONTAINER:-runs-ai-analyzer-db}"
HOST_PORT=5444
EXPORTER_PORT=9188
ROLE=exporter_monitor
SVC=prometheus-postgres-exporter-5444
EXPORTER_BIN=/usr/bin/prometheus-postgres-exporter
EXPORTER_ENV=/etc/default/$SVC
UNIT=/etc/systemd/system/$SVC.service
QUERIES=/etc/prometheus-postgres-exporter/queries.yaml
PROM_YML=/etc/prometheus/prometheus.yml
STAMP=$(date +%Y%m%d%H%M%S)

[ "$(id -u)" -eq 0 ] || { echo "run as root (sudo)"; exit 1; }

echo "==> 0/5 preflight"
[ -x "$EXPORTER_BIN" ] || { echo "$EXPORTER_BIN missing"; exit 1; }
[ -f "$QUERIES" ] || { echo "$QUERIES missing; run vm-setup-pg-exporter.sh first"; exit 1; }
[ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" = "true" ] \
  || { echo "container $CONTAINER is not running"; exit 1; }
if ss -ltn | grep -q ":$EXPORTER_PORT " && ! systemctl is-active --quiet "$SVC"; then
  echo "port $EXPORTER_PORT is already in use by something else"; exit 1
fi
last_top=$(grep -nE '^[A-Za-z_]+:' "$PROM_YML" | tail -1 | cut -d: -f2)
[ "$last_top" = "scrape_configs" ] \
  || { echo "scrape_configs is not the last section of $PROM_YML; add the job by hand"; exit 1; }

PW=$(openssl rand -hex 16)

echo "==> 1/5 monitoring role $ROLE inside $CONTAINER (pg_monitor only, no data access)"
# Password goes in via stdin, not argv. Uses the container's own superuser from its env.
{
  cat <<SQL
SELECT 'CREATE ROLE $ROLE LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$ROLE') \gexec
SQL
  printf "ALTER ROLE %s WITH LOGIN PASSWORD '%s';\n" "$ROLE" "$PW"
  echo "GRANT pg_monitor TO $ROLE;"
} | docker exec -i "$CONTAINER" sh -c 'psql -v ON_ERROR_STOP=1 -q -U "${POSTGRES_USER:-postgres}" -d postgres'

echo "==> 2/5 exporter service on :$EXPORTER_PORT"
[ -f "$EXPORTER_ENV" ] && cp -a "$EXPORTER_ENV" "$EXPORTER_ENV.bak.$STAMP"
printf "DATA_SOURCE_NAME='postgresql://%s:%s@127.0.0.1:%s/postgres?sslmode=disable'\n" "$ROLE" "$PW" "$HOST_PORT" > "$EXPORTER_ENV"
chown prometheus:prometheus "$EXPORTER_ENV"; chmod 640 "$EXPORTER_ENV"
cat > "$UNIT" <<UNITFILE
[Unit]
Description=Prometheus exporter for PostgreSQL ($CONTAINER :$HOST_PORT)
After=network.target docker.service

[Service]
User=prometheus
EnvironmentFile=$EXPORTER_ENV
ExecStart=$EXPORTER_BIN --web.listen-address=:$EXPORTER_PORT --extend.query-path=$QUERIES
Restart=on-failure

[Install]
WantedBy=multi-user.target
UNITFILE
systemctl daemon-reload
systemctl enable "$SVC" >/dev/null 2>&1
systemctl restart "$SVC"

for _ in $(seq 1 15); do   # first DB connection takes a few seconds after start
  m=$(curl -s -m 8 "localhost:$EXPORTER_PORT/metrics" || true)
  [[ $m == *$'\npg_app_connections_count'* ]] && break
  sleep 2
done
# plain string matching: `echo | grep -q` + pipefail gives false failures (SIGPIPE) on big output
[[ $m == *$'\npg_up 1'* ]] || { echo "pg_up != 1; see: journalctl -u $SVC -n 30"; exit 1; }
echo "pg_up 1"
[[ $m == *$'\npg_app_connections_count'* ]] \
  || { echo "per-app queries not loaded; see: journalctl -u $SVC -n 30"; exit 1; }
echo "databases seen by the exporter:"
echo "$m" | grep '^pg_stat_database_numbackends' | sed -E 's/.*datname="([^"]*)".*\} ([0-9.]+)/  \1 conns=\2/'

echo "==> 3/5 Prometheus scrape job"
if grep -q 'job_name: postgres-runs-ai-analyzer' "$PROM_YML"; then
  echo "job already present, skipping"
else
  cp -a "$PROM_YML" "$PROM_YML.bak.$STAMP"
  ind=$(grep -m1 -E '^\s*-\s+job_name:' "$PROM_YML" | sed -E 's/^(\s*)-.*/\1/')
  [ -n "$(tail -c1 "$PROM_YML")" ] && echo >> "$PROM_YML"
  {
    echo ""
    echo "${ind}- job_name: postgres-runs-ai-analyzer"
    echo "${ind}  static_configs:"
    echo "${ind}    - targets: ['localhost:$EXPORTER_PORT']"
    echo "${ind}      labels:"
    echo "${ind}        instance: runs-ai-analyzer-db"
  } >> "$PROM_YML"
  # promtool rejects the existing rule_files entry "prometheus-alert-rules.yml" (the file lives in
  # rules/ and is loaded by the glob; Prometheus ignores the missing path). Validate a copy without it.
  chk=$(mktemp /etc/prometheus/.check.XXXXXX)
  grep -vE '^\s*-\s*"prometheus-alert-rules\.yml"\s*$' "$PROM_YML" > "$chk"
  if ! promtool check config "$chk"; then
    rm -f "$chk"; echo "config invalid, restoring backup"; cp -a "$PROM_YML.bak.$STAMP" "$PROM_YML"; exit 1
  fi
  rm -f "$chk"
  echo "==> 4/5 reload Prometheus"
  systemctl reload prometheus || systemctl restart prometheus
fi

echo "==> 5/5 verify (waiting for a scrape)"
sleep 30
q() { curl -s -G 'localhost:9090/api/v1/query' --data-urlencode "query=$1"; }
echo -n "pg_up:   "; q 'pg_up' | python3 -c 'import sys,json; print(sorted((x["metric"].get("job"),x["value"][1]) for x in json.load(sys.stdin)["data"]["result"]))'
echo -n "targets: "; curl -s localhost:9090/api/v1/targets | python3 -c 'import sys,json; print([(t["labels"]["job"],t["health"]) for t in json.load(sys.stdin)["data"]["activeTargets"] if t["labels"]["job"].startswith("postgres")])'
echo -n "per-app: "; q 'sum by (instance,datname,usename)(pg_app_connections_count{instance="runs-ai-analyzer-db"})' | python3 -c 'import sys,json; print(["%s/%s=%s"%(x["metric"].get("datname"),x["metric"].get("usename"),x["value"][1]) for x in json.load(sys.stdin)["data"]["result"]] or "NO SERIES")'
