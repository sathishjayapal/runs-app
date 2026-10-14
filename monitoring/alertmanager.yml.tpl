# Critical alerts -> ntfy push AND email (backup). Daily Watchdog heartbeat -> ntfy only. Everything else is dropped.
# __NTFY_TOPIC__ is substituted by vm-setup-alerting.sh; the SMTP password is read from a root-only file.
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'sathishk.dot@gmail.com'
  smtp_auth_username: 'sathishk.dot@gmail.com'
  smtp_auth_password_file: '/etc/alertmanager-smtp.pass'
  smtp_require_tls: true
route:
  receiver: "null"
  group_by: ['alertname', 'instance', 'name']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - matchers: [severity="heartbeat"]
      receiver: ntfy-heartbeat
      group_wait: 0s
      repeat_interval: 24h
    - matchers: [severity="critical"]
      receiver: critical
receivers:
  - name: "null"
  - name: ntfy-heartbeat
    webhook_configs:
      - url: 'https://ntfy.sh/__NTFY_TOPIC__?template=alertmanager&priority=min'
        send_resolved: false
  - name: critical
    webhook_configs:
      - url: 'https://ntfy.sh/__NTFY_TOPIC__?template=alertmanager&priority=high'
        send_resolved: true
    email_configs:
      - to: 'sathishk.dot@gmail.com'
        send_resolved: true
