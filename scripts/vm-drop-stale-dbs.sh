#!/usr/bin/env bash
# Drops two stale databases from the NATIVE Postgres 16 cluster on runs-server (port 5432):
#   runs_ai_analyzer_db   (stale duplicate; the live one is in the runs-ai-analyzer-db CONTAINER on :5444, untouched)
#   test_db
# NEVER touches: the :5444 container, my-github-cleaner, or any other database.
#
# Run ON THE VM (copy first so sudo and the confirmation prompt get a terminal):
#   scp scripts/vm-drop-stale-dbs.sh vm:/home/skminfotech/
#   ssh -t vm 'sudo bash /home/skminfotech/vm-drop-stale-dbs.sh'
#
# Safety: shows size/tables/rows/connections, takes a pg_dump backup (verified readable),
# requires typing "drop" to continue, and uses plain DROP DATABASE (no FORCE), so Postgres
# itself refuses if anything is still connected.
set -euo pipefail

DBS=(runs_ai_analyzer_db test_db)
BACKUP_DIR=/var/backups/pg-dropped-$(date +%Y%m%d%H%M%S)
PSQL=(sudo -u postgres psql -X -v ON_ERROR_STOP=1 -tA)

[ "$(id -u)" -eq 0 ] || { echo "run as root (sudo)"; exit 1; }

port=$("${PSQL[@]}" -d postgres -c "SHOW port")
[ "$port" = "5432" ] || { echo "connected to port $port, expected the native cluster on 5432; aborting"; exit 1; }
echo "Native cluster (port $port). The container on :5444 is a different server and is not touched."
echo

echo "==> what is in each database"
for db in "${DBS[@]}"; do
  exists=$("${PSQL[@]}" -d postgres -c "SELECT count(*) FROM pg_database WHERE datname='$db'")
  [ "$exists" = "1" ] || { echo "  $db: does not exist, skipping"; continue; }
  size=$("${PSQL[@]}" -d postgres -c "SELECT pg_size_pretty(pg_database_size('$db'))")
  conns=$("${PSQL[@]}" -d postgres -c "SELECT count(*) FROM pg_stat_activity WHERE datname='$db'")
  tables=$("${PSQL[@]}" -d "$db" -c "SELECT count(*) FROM pg_stat_user_tables")
  rows=$("${PSQL[@]}" -d "$db" -c "SELECT COALESCE(sum(n_live_tup),0) FROM pg_stat_user_tables")
  echo "  $db: size=$size user_tables=$tables approx_rows=$rows open_connections=$conns"
  [ "$conns" = "0" ] || { echo "  $db still has connections; aborting so nothing is dropped"; exit 1; }
done
echo

echo "==> backing up to $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"; chmod 700 "$BACKUP_DIR"
for db in "${DBS[@]}"; do
  [ "$("${PSQL[@]}" -d postgres -c "SELECT count(*) FROM pg_database WHERE datname='$db'")" = "1" ] || continue
  sudo -u postgres pg_dump -Fc "$db" > "$BACKUP_DIR/$db.dump"
  pg_restore -l "$BACKUP_DIR/$db.dump" > /dev/null   # fails (aborting) if the dump is unreadable
  echo "  $db -> $BACKUP_DIR/$db.dump ($(du -h "$BACKUP_DIR/$db.dump" | cut -f1)), verified readable"
done
echo

echo "About to DROP from the native cluster: ${DBS[*]}"
echo "Backups are in $BACKUP_DIR. my-github-cleaner and the :5444 container are NOT affected."
read -r -p 'Type "drop" to continue: ' answer
[ "$answer" = "drop" ] || { echo "cancelled, nothing dropped"; exit 0; }

for db in "${DBS[@]}"; do
  [ "$("${PSQL[@]}" -d postgres -c "SELECT count(*) FROM pg_database WHERE datname='$db'")" = "1" ] || continue
  "${PSQL[@]}" -d postgres -c "DROP DATABASE \"$db\""
  echo "  dropped $db"
done

echo
echo "==> remaining databases on the native cluster"
"${PSQL[@]}" -d postgres -c "SELECT datname FROM pg_database WHERE NOT datistemplate ORDER BY 1" | sed 's/^/  /'
echo
echo "Restore if ever needed:  sudo -u postgres createdb <name> && sudo -u postgres pg_restore -d <name> $BACKUP_DIR/<name>.dump"
