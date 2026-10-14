#!/usr/bin/env bash
# Sequence-exhaustion monitoring for every app database on runs-server (native PG16 + runs-ai-analyzer-db container).
#
# How: a SECURITY DEFINER function monitoring.sequence_usage() is created in each app DB (reads pg_sequences as the
# DB superuser, so last_value is visible without granting the exporter any table/sequence access). Two NEW
# postgres_exporter instances (auto-discover all DBs, sequence query only) expose pg_sequence_usage_ratio on
# :9189 (native) and :9190 (container). Existing exporters and their queries.yaml are NOT touched.
#
# Usage (from runs-app/monitoring):
#   scp critical-alerts.yml vm-setup-sequence-monitor.sh vm:/tmp/ && ssh -t vm 'sudo bash /tmp/vm-setup-sequence-monitor.sh'
# Idempotent. Also (re)installs critical-alerts.yml so the sequence alert and the narrowed consumer rule go live.
set -euo pipefail
SRC=/tmp; PROM=/etc/prometheus; QDIR=/etc/prometheus-postgres-exporter; TS=$(date +%s)
CONTAINER=runs-ai-analyzer-db; CONT_SUPER=runsai_local; EXPORTER_ROLE=exporter_monitor
[ "$(id -u)" = 0 ] || { echo "run with sudo"; exit 1; }

promtool check rules "$SRC/critical-alerts.yml"

read -r -d '' FUNC_SQL <<'SQL' || true
CREATE SCHEMA IF NOT EXISTS monitoring;
CREATE OR REPLACE FUNCTION monitoring.sequence_usage()
RETURNS TABLE(schemaname name, sequencename name, ratio float8)
LANGUAGE sql SECURITY DEFINER SET search_path = pg_catalog AS $$
  SELECT s.schemaname, s.sequencename,
         COALESCE(s.last_value::float8 / NULLIF(s.max_value, 0)::float8, 0)
  FROM pg_sequences s
  WHERE NOT s.cycle AND s.increment_by > 0
$$;
REVOKE ALL ON FUNCTION monitoring.sequence_usage() FROM PUBLIC;
GRANT USAGE ON SCHEMA monitoring TO __ROLE__;
GRANT EXECUTE ON FUNCTION monitoring.sequence_usage() TO __ROLE__;
SQL
FUNC_SQL=${FUNC_SQL//__ROLE__/$EXPORTER_ROLE}

echo "== native PG16: installing function in each app database"
for db in $(runuser -u postgres -- psql -Atc "SELECT datname FROM pg_database WHERE NOT datistemplate AND datname <> 'postgres'"); do
  echo "  $db"; printf '%s\n' "$FUNC_SQL" | runuser -u postgres -- psql -q -v ON_ERROR_STOP=1 -d "$db"
done
echo "== container $CONTAINER: installing function in each app database"
for db in $(docker exec "$CONTAINER" psql -U "$CONT_SUPER" -d postgres -Atc "SELECT datname FROM pg_database WHERE NOT datistemplate AND datname <> 'postgres'"); do
  echo "  $db"; printf '%s\n' "$FUNC_SQL" | docker exec -i "$CONTAINER" psql -q -v ON_ERROR_STOP=1 -U "$CONT_SUPER" -d "$db"
done

cat > "$QDIR/queries-sequences.yaml" <<'YAML'
pg_sequence_usage:
  query: "SELECT current_database() AS datname, schemaname, sequencename, ratio FROM monitoring.sequence_usage()"
  metrics:
    - datname:      {usage: "LABEL", description: "Database"}
    - schemaname:   {usage: "LABEL", description: "Schema"}
    - sequencename: {usage: "LABEL", description: "Sequence"}
    - ratio:        {usage: "GAUGE", description: "last_value / max_value"}
YAML
chmod 644 "$QDIR/queries-sequences.yaml"

mkunit() {  # port envfile description
  cat > "/etc/systemd/system/prometheus-postgres-exporter-seq-$1.service" <<UNIT
[Unit]
Description=Postgres sequence-usage exporter ($3)
After=network.target

[Service]
User=prometheus
EnvironmentFile=$2
ExecStart=/usr/bin/prometheus-postgres-exporter --web.listen-address=127.0.0.1:$1 --disable-default-metrics --disable-settings-metrics --auto-discover-databases --exclude-databases=template0,template1,postgres --extend.query-path=$QDIR/queries-sequences.yaml
Restart=on-failure

[Install]
WantedBy=multi-user.target
UNIT
}
mkunit 9189 /etc/default/prometheus-postgres-exporter "native PG16"
mkunit 9190 /etc/default/prometheus-postgres-exporter-5444 "runs-ai-analyzer-db"
systemctl daemon-reload
systemctl enable --now prometheus-postgres-exporter-seq-9189 prometheus-postgres-exporter-seq-9190
systemctl restart prometheus-postgres-exporter-seq-9189 prometheus-postgres-exporter-seq-9190
sleep 8

echo "== exporter output (top sequences by usage)"
for p in 9189 9190; do
  echo "-- :$p"
  curl -s "127.0.0.1:$p/metrics" | grep -E '^pg_sequence_usage_ratio' | sort -t' ' -k2 -gr | head -5 || echo "no pg_sequence_usage_ratio series!"
  curl -s "127.0.0.1:$p/metrics" | grep -E '^pg_up|^pg_exporter_last_scrape_error' || true
done

cp -a "$PROM/prometheus.yml" "$PROM/prometheus.yml.bak.$TS"
grep -q 'job_name: postgres-sequences-runsapp' "$PROM/prometheus.yml" || cat >> "$PROM/prometheus.yml" <<'YML'

  - job_name: postgres-sequences-runsapp
    static_configs:
      - targets: ['127.0.0.1:9189']
        labels:
          instance: runsapp-db
  - job_name: postgres-sequences-runs-ai-analyzer
    static_configs:
      - targets: ['127.0.0.1:9190']
        labels:
          instance: runs-ai-analyzer-db
YML
promtool check config "$PROM/prometheus.yml"
install -o prometheus -g prometheus -m 664 "$SRC/critical-alerts.yml" "$PROM/rules/critical-alerts.yml"
# prometheus.service has no ExecReload, so `systemctl reload` fails; SIGHUP is the supported reload.
systemctl reload prometheus 2>/dev/null || kill -HUP "$(pidof prometheus)"
echo "Done. Paste the output above back to Claude to verify."
