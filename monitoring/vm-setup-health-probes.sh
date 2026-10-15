#!/usr/bin/env bash
# Install blackbox_exporter on runs-server and probe each app's health endpoint.
# Usage (from runs-app/monitoring):
#   scp blackbox.yml health-probes.yml vm-setup-health-probes.sh vm:/tmp/ && ssh -t vm 'sudo bash /tmp/vm-setup-health-probes.sh'
# Idempotent. Backs up what it changes. Targets: 5 Java apps on this host + Nextcloud and Immich on Mac 13.
set -euo pipefail
SRC=/tmp; PROM=/etc/prometheus; TS=$(date +%s)
[ "$(id -u)" = 0 ] || { echo "run with sudo"; exit 1; }

promtool check rules "$SRC/health-probes.yml"

command -v prometheus-blackbox-exporter >/dev/null || DEBIAN_FRONTEND=noninteractive apt-get install -y prometheus-blackbox-exporter
CFG=/etc/prometheus/blackbox.yml
[ -f "$CFG" ] && cp -a "$CFG" "$CFG.bak.$TS"
install -o root -g prometheus -m 644 "$SRC/blackbox.yml" "$CFG"

# Listen on loopback only; Prometheus is on this host. The package reads ARGS from /etc/default.
cat > /etc/default/prometheus-blackbox-exporter <<EOF
ARGS="--config.file=$CFG --web.listen-address=127.0.0.1:9115"
EOF
systemctl enable prometheus-blackbox-exporter
systemctl restart prometheus-blackbox-exporter
sleep 2
curl -sf "127.0.0.1:9115/probe?module=spring_health&target=http://localhost:8080/actuator/health" | grep -E '^probe_success'

cp -a "$PROM/prometheus.yml" "$PROM/prometheus.yml.bak.$TS"
grep -q 'job_name: blackbox-health' "$PROM/prometheus.yml" || cat >> "$PROM/prometheus.yml" <<'YML'

  # Health probes. __param_module picks the blackbox module; the "app" label names the service in alerts.
  - job_name: blackbox-health
    metrics_path: /probe
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: 127.0.0.1:9115
    static_configs:
      - targets: ['http://localhost:8080/actuator/health']
        labels: {app: runs-app, __param_module: spring_health}
      - targets: ['http://localhost:8081/actuator/health']
        labels: {app: runs-ai-analyzer, __param_module: spring_health}
      - targets: ['http://localhost:9081/actuator/health']
        labels: {app: eventstracker, __param_module: spring_health}
      - targets: ['http://localhost:8090/actuator/health']
        labels: {app: sathishlogger, __param_module: spring_health}
      - targets: ['http://192.168.4.236:2281/status.php']
        labels: {app: nextcloud, __param_module: nextcloud_status}
      - targets: ['http://192.168.4.236:2283/api/server/ping']
        labels: {app: immich, __param_module: immich_ping}
YML
promtool check config "$PROM/prometheus.yml"
install -o prometheus -g prometheus -m 664 "$SRC/health-probes.yml" "$PROM/rules/health-probes.yml"
kill -HUP "$(pidof prometheus)"
sleep 20
echo "--- probe results ---"
curl -sG localhost:9090/api/v1/query --data-urlencode 'query=probe_success{job="blackbox-health"}' \
  | python3 -c "import sys,json;[print(r['metric']['app'],r['value'][1]) for r in json.load(sys.stdin)['data']['result']]"
echo "Done. Paste the output above back to Claude to verify."
