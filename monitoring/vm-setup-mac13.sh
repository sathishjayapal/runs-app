#!/usr/bin/env bash
# Phase 2 wiring for mac13-server: scrape its cAdvisor and install the Nextcloud/Immich container + host alerts.
# Prereq: cAdvisor deployed on mac13 (mac13-cadvisor-compose.yml) and reachable on 192.168.4.236:9099.
# Usage (from runs-app/monitoring):
#   scp mac13-alerts.yml vm-setup-mac13.sh vm:/tmp/ && ssh -t vm 'sudo bash /tmp/vm-setup-mac13.sh'
set -euo pipefail
SRC=/tmp; PROM=/etc/prometheus; TS=$(date +%s)
[ "$(id -u)" = 0 ] || { echo "run with sudo"; exit 1; }

curl -sf -m5 http://192.168.4.236:9099/metrics | grep -q '^container_last_seen' \
  || { echo "cAdvisor on mac13 (192.168.4.236:9099) is not answering. Deploy the stack first."; exit 1; }
promtool check rules "$SRC/mac13-alerts.yml"

cp -a "$PROM/prometheus.yml" "$PROM/prometheus.yml.bak.$TS"
grep -q 'job_name: cadvisor-mac13' "$PROM/prometheus.yml" || cat >> "$PROM/prometheus.yml" <<'YML'

  - job_name: cadvisor-mac13
    static_configs:
      - targets: ['192.168.4.236:9099']
        labels:
          app: "cadvisor-mac13"
YML
promtool check config "$PROM/prometheus.yml"
install -o prometheus -g prometheus -m 664 "$SRC/mac13-alerts.yml" "$PROM/rules/mac13-alerts.yml"
kill -HUP "$(pidof prometheus)"
sleep 20
echo "--- mac13 containers seen by cAdvisor ---"
curl -sG localhost:9090/api/v1/query --data-urlencode 'query=container_last_seen{job="cadvisor-mac13",name!=""}' \
  | python3 -c "import sys,json;print(sorted(r['metric']['name'] for r in json.load(sys.stdin)['data']['result']))"
echo "Done. Paste the output above back to Claude to verify."
