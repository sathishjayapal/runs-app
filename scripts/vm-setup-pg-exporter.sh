#!/usr/bin/env bash
# Wires the native Postgres 16 cluster on runs-server (runsapp_db) into Prometheus,
# with per-application metrics (grouped by role + application_name) because several
# apps share this one database.
#
# Run ON THE VM as root:
#   ssh -t vm 'sudo bash -s' < scripts/vm-setup-pg-exporter.sh
#
# Idempotent. Re-running rotates the exporter password, rewrites the queries/rules files,
# and skips the scrape job if it already exists. The password is generated on the VM and
# written only to the root-managed exporter env file (never printed).
# Every file it edits is backed up next to the original as *.bak.<timestamp>.
set -euo pipefail

DB_NAME="${DB_NAME:-runsapp_db}"
EXPORTER_ENV=/etc/default/prometheus-postgres-exporter
EXPORTER_DIR=/etc/prometheus-postgres-exporter
QUERIES="$EXPORTER_DIR/queries.yaml"
PROM_YML=/etc/prometheus/prometheus.yml
RULES=/etc/prometheus/rules/postgres-per-app.yml
STAMP=$(date +%Y%m%d%H%M%S)

[ "$(id -u)" -eq 0 ] || { echo "run as root (sudo)"; exit 1; }
EXPORTER_BIN=/usr/bin/prometheus-postgres-exporter

echo "==> 0/6 preflight"
"$EXPORTER_BIN" --help 2>&1 | grep -q 'extend.query-path' \
  || { echo "this exporter build has no --extend.query-path; aborting before any change"; exit 1; }
# Prometheus config must end with scrape_configs so appending a job is safe.
last_top=$(grep -nE '^[A-Za-z_]+:' "$PROM_YML" | tail -1 | cut -d: -f2)
[ "$last_top" = "scrape_configs" ] \
  || { echo "scrape_configs is not the last section of $PROM_YML; add the job by hand (see step 4)"; exit 1; }

PW=$(openssl rand -hex 16)

echo "==> 1/6 monitoring role exporter_monitor (pg_monitor only, no data access)"
sudo -u postgres psql -v ON_ERROR_STOP=1 -v pw="$PW" -d postgres <<'SQL'
SELECT 'CREATE ROLE exporter_monitor LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'exporter_monitor') \gexec
ALTER ROLE exporter_monitor WITH LOGIN PASSWORD :'pw';
GRANT pg_monitor TO exporter_monitor;
SQL

echo "==> 2/6 per-application queries"
mkdir -p "$EXPORTER_DIR"
[ -f "$QUERIES" ] && cp -a "$QUERIES" "$QUERIES.bak.$STAMP"
cat > "$QUERIES" <<'YML'
# Per-app visibility for a shared database. "app" = application_name set via the JDBC
# URL (?ApplicationName=runs-app); apps that don't set it show up as application_name="unset".
pg_app_connections:
  master: true
  query: |
    SELECT datname,
           usename,
           COALESCE(NULLIF(application_name, ''), 'unset') AS application_name,
           COALESCE(state, 'unknown') AS state,
           count(*) AS count
    FROM pg_stat_activity
    WHERE backend_type = 'client backend' AND datname IS NOT NULL
    GROUP BY 1, 2, 3, 4
  metrics:
    - datname:          {usage: "LABEL", description: "Database"}
    - usename:          {usage: "LABEL", description: "Role"}
    - application_name: {usage: "LABEL", description: "Application"}
    - state:            {usage: "LABEL", description: "Connection state"}
    - count:            {usage: "GAUGE", description: "Connections"}

pg_app_oldest_xact:
  master: true
  query: |
    SELECT datname,
           usename,
           COALESCE(NULLIF(application_name, ''), 'unset') AS application_name,
           max(EXTRACT(EPOCH FROM (now() - xact_start))) AS seconds
    FROM pg_stat_activity
    WHERE backend_type = 'client backend' AND xact_start IS NOT NULL AND pid <> pg_backend_pid()
    GROUP BY 1, 2, 3
  metrics:
    - datname:          {usage: "LABEL", description: "Database"}
    - usename:          {usage: "LABEL", description: "Role"}
    - application_name: {usage: "LABEL", description: "Application"}
    - seconds:          {usage: "GAUGE", description: "Age of the oldest open transaction"}

pg_app_idle_in_xact:
  master: true
  query: |
    SELECT datname,
           usename,
           COALESCE(NULLIF(application_name, ''), 'unset') AS application_name,
           max(EXTRACT(EPOCH FROM (now() - state_change))) AS seconds
    FROM pg_stat_activity
    WHERE backend_type = 'client backend' AND state = 'idle in transaction'
    GROUP BY 1, 2, 3
  metrics:
    - datname:          {usage: "LABEL", description: "Database"}
    - usename:          {usage: "LABEL", description: "Role"}
    - application_name: {usage: "LABEL", description: "Application"}
    - seconds:          {usage: "GAUGE", description: "Longest time a connection has sat idle in transaction"}
YML
chown -R prometheus:prometheus "$EXPORTER_DIR"; chmod 640 "$QUERIES"

echo "==> 3/6 exporter DSN + query path, restart"
cp -a "$EXPORTER_ENV" "$EXPORTER_ENV.bak.$STAMP"
old_args=$(set +u; . "$EXPORTER_ENV" >/dev/null 2>&1; printf '%s' "${ARGS:-}")
sed -i -e '/^DATA_SOURCE_NAME=/d' -e '/^ARGS=/d' "$EXPORTER_ENV"
new_args="$old_args"
case "$old_args" in *extend.query-path*) ;; *) new_args="$old_args --extend.query-path=$QUERIES";; esac
printf "DATA_SOURCE_NAME='postgresql://exporter_monitor:%s@127.0.0.1:5432/%s?sslmode=disable'\n" "$PW" "$DB_NAME" >> "$EXPORTER_ENV"
printf "ARGS='%s'\n" "$(echo "$new_args" | sed 's/^ *//')" >> "$EXPORTER_ENV"
chown prometheus:prometheus "$EXPORTER_ENV"; chmod 640 "$EXPORTER_ENV"
systemctl restart prometheus-postgres-exporter
for _ in $(seq 1 15); do   # first DB connection takes a few seconds after restart
  m=$(curl -s -m 8 localhost:9187/metrics || true)
  # plain string matching: `echo | grep -q` + pipefail gives false failures (SIGPIPE) on big output
  [[ $m == *$'\npg_app_connections_count'* ]] && break
  sleep 2
done
[[ $m == *$'\npg_up 1'* ]] || { echo "pg_up != 1; see: journalctl -u prometheus-postgres-exporter -n 30"; exit 1; }
echo "pg_up 1"
[[ $m == *$'\npg_app_connections_count'* ]] \
  || { echo "custom queries not loaded; see: journalctl -u prometheus-postgres-exporter -n 30"; exit 1; }
echo "per-app series present:"; echo "$m" | grep '^pg_app_connections_count' | sed -E 's/^/  /'

echo "==> 4/6 Prometheus scrape job"
if grep -q 'job_name: postgres-runsapp' "$PROM_YML"; then
  echo "job already present, skipping"
else
  cp -a "$PROM_YML" "$PROM_YML.bak.$STAMP"
  ind=$(grep -m1 -E '^\s*-\s+job_name:' "$PROM_YML" | sed -E 's/^(\s*)-.*/\1/')
  [ -n "$(tail -c1 "$PROM_YML")" ] && echo >> "$PROM_YML"
  {
    echo ""
    echo "${ind}- job_name: postgres-runsapp"
    echo "${ind}  static_configs:"
    echo "${ind}    - targets: ['localhost:9187']"
    echo "${ind}      labels:"
    echo "${ind}        instance: runsapp-db"
  } >> "$PROM_YML"
  # promtool rejects the existing rule_files entry "prometheus-alert-rules.yml" (the file
  # lives in rules/ and is loaded by the glob; Prometheus itself ignores the missing path).
  # Validate a copy without that one line so we don't edit the user's rule_files.
  chk=$(mktemp /etc/prometheus/.check.XXXXXX)
  grep -vE '^\s*-\s*"prometheus-alert-rules\.yml"\s*$' "$PROM_YML" > "$chk"
  if ! promtool check config "$chk"; then
    rm -f "$chk"; echo "config invalid, restoring backup"; cp -a "$PROM_YML.bak.$STAMP" "$PROM_YML"; exit 1
  fi
  rm -f "$chk"
fi

echo "==> 5/6 per-app alert rules"
[ -f "$RULES" ] && cp -a "$RULES" "$RULES.bak.$STAMP"
cat > "$RULES" <<'YML'
groups:
  - name: PostgresPerApp
    rules:
      - alert: PostgresAppLongTransaction
        expr: pg_app_oldest_xact_seconds > 300
        for: 2m
        labels: {severity: warning}
        annotations:
          summary: "{{ $labels.application_name }} (role {{ $labels.usename }}) has a transaction open for {{ $value | humanizeDuration }}"
      - alert: PostgresAppIdleInTransaction
        expr: pg_app_idle_in_xact_seconds > 600
        for: 5m
        labels: {severity: warning}
        annotations:
          summary: "{{ $labels.application_name }} (role {{ $labels.usename }}) has a connection idle in transaction for {{ $value | humanizeDuration }}"
      - alert: PostgresAppConnectionsHigh
        expr: sum by (instance, datname, usename, application_name) (pg_app_connections_count) > 20
        for: 5m
        labels: {severity: warning}
        annotations:
          summary: "{{ $labels.application_name }} (role {{ $labels.usename }}) is holding {{ $value }} connections"
YML
chown prometheus:prometheus "$RULES"; chmod 644 "$RULES"
promtool check rules "$RULES"
systemctl reload prometheus || systemctl restart prometheus

echo "==> 6/6 verify (waiting for a scrape + rule evaluation)"
sleep 30
echo -n "pg_up:     "; curl -s 'localhost:9090/api/v1/query?query=pg_up%7Bjob%3D%22postgres-runsapp%22%7D' | python3 -c 'import sys,json; r=json.load(sys.stdin)["data"]["result"]; print([x["value"][1] for x in r] or "NO SERIES")'
echo -n "per-app:   "; curl -s 'localhost:9090/api/v1/query?query=sum%20by%20(usename%2Capplication_name)(pg_app_connections_count)' | python3 -c 'import sys,json; r=json.load(sys.stdin)["data"]["result"]; print(["%s/%s=%s"%(x["metric"].get("usename"),x["metric"].get("application_name"),x["value"][1]) for x in r] or "NO SERIES")'
echo -n "rules:     "; curl -s localhost:9090/api/v1/rules | python3 -c 'import sys,json; g=[x["name"] for x in json.load(sys.stdin)["data"]["groups"]]; print("PostgresPerApp loaded" if "PostgresPerApp" in g else "NOT LOADED - add a rule_files glob covering /etc/prometheus/rules/postgres-per-app.yml")'
