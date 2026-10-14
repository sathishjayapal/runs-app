#!/usr/bin/env bash
# Phase 1 alerting for runs-server: critical-only rules, Alertmanager -> ntfy + email, RabbitMQ scrape.
# Usage (from runs-app/monitoring):
#   scp critical-alerts.yml alertmanager.yml.tpl vm-setup-alerting.sh vm:/tmp/ \
#     && ssh -t vm 'sudo bash /tmp/vm-setup-alerting.sh'
# Prereq: RabbitMQ port 15692 published to 127.0.0.1 (Portainer stack "sathish-stack", see README step).
# Idempotent. Backs up every file it changes. Secrets are prompted for / generated on the VM, never in this repo.
set -euo pipefail
SRC=/tmp; AM=/var/lib/alertmanager; PROM=/etc/prometheus; TS=$(date +%s)
[ "$(id -u)" = 0 ] || { echo "run with sudo"; exit 1; }

promtool check rules "$SRC/critical-alerts.yml"

# ntfy topic doubles as the secret on the public server: generated once, root-only.
TOPIC_FILE=/etc/alertmanager-ntfy.topic
[ -s "$TOPIC_FILE" ] || { echo "runs-alerts-$(openssl rand -hex 10)" > "$TOPIC_FILE"; chmod 600 "$TOPIC_FILE"; }
TOPIC=$(cat "$TOPIC_FILE")

# Gmail app password: prompted once, readable by the alertmanager user only.
SMTP_FILE=/etc/alertmanager-smtp.pass
if [ ! -s "$SMTP_FILE" ]; then
  read -rsp "Gmail app password for sathishk.dot@gmail.com (input hidden): " PW; echo
  printf '%s' "${PW// /}" > "$SMTP_FILE"; unset PW
  chown root:prometheus "$SMTP_FILE"; chmod 640 "$SMTP_FILE"
fi

cp -a "$AM/alertmanager.yml" "$AM/alertmanager.yml.bak.$TS"
sed "s/__NTFY_TOPIC__/$TOPIC/g" "$SRC/alertmanager.yml.tpl" > "$AM/alertmanager.yml.new"
"$AM/amtool" check-config "$AM/alertmanager.yml.new"
chown prometheus:prometheus "$AM/alertmanager.yml.new"; chmod 640 "$AM/alertmanager.yml.new"
mv "$AM/alertmanager.yml.new" "$AM/alertmanager.yml"

# Retire every old rule file (kept in a backup dir), install the new set.
OLD="$PROM/rules.removed.$TS"; mkdir -p "$OLD"
for f in "$PROM"/rules/*.yml; do [ "$(basename "$f")" = critical-alerts.yml ] || mv "$f" "$OLD"/; done
[ -f "$PROM/prometheus-alert-rules.yml" ] && mv "$PROM/prometheus-alert-rules.yml" "$OLD"/ || true
install -o prometheus -g prometheus -m 664 "$SRC/critical-alerts.yml" "$PROM/rules/critical-alerts.yml"

cp -a "$PROM/prometheus.yml" "$PROM/prometheus.yml.bak.$TS"
# That file never existed at this path (the rules/*.yml glob loads everything); promtool rejects the dangling entry.
sed -i '/^[[:space:]]*- "prometheus-alert-rules.yml"[[:space:]]*$/d' "$PROM/prometheus.yml"
grep -q 'localhost:9093' "$PROM/prometheus.yml" || sed -i 's|# - alertmanager:9093|- localhost:9093|' "$PROM/prometheus.yml"
grep -q 'job_name: alertmanager' "$PROM/prometheus.yml" || cat >> "$PROM/prometheus.yml" <<'YML'

  - job_name: alertmanager
    static_configs:
      - targets: ['localhost:9093']
YML
grep -q 'job_name: rabbitmq' "$PROM/prometheus.yml" || cat >> "$PROM/prometheus.yml" <<'YML'

  # Per-queue metrics (needed for dead-letter / consumer / backlog alerts)
  - job_name: rabbitmq
    metrics_path: /metrics/detailed
    params:
      family: [queue_coarse_metrics, queue_consumer_count]
    static_configs:
      - targets: ['localhost:15692']
  - job_name: rabbitmq-node
    static_configs:
      - targets: ['localhost:15692']
YML
promtool check config "$PROM/prometheus.yml"

# Alertmanager runs as uid 999 (prometheus) but its data dir was left owned by uid 755, so it could not
# persist silences/notification state ("permission denied" in its log). Fix ownership before restarting.
chown -R prometheus:prometheus "$AM/data"

systemctl restart alertmanager
systemctl reload prometheus || kill -HUP "$(pidof prometheus)"
sleep 20

echo "--- rabbitmq scrape ---"
curl -s localhost:9090/api/v1/targets | grep -o '"job":"rabbitmq[^"]*"[^}]*"health":"[a-z]*"' || echo "rabbitmq targets not found"
echo "--- test alert (arrives in ~30s via ntfy + email) ---"
"$AM/amtool" alert add TestCriticalAlert severity=critical instance=setup-test \
  --annotation=summary="alerting wiring test - safe to ignore" --alertmanager.url=http://localhost:9093
echo
echo "Subscribe in the ntfy app (or https://ntfy.sh/$TOPIC) to topic: $TOPIC"
echo "Removed old rules are in $OLD"
