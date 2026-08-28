#!/usr/bin/env bash
# MySQL 每日备份 + 保留策略
# crontab: 30 3 * * * /opt/recycle/deploy/backup-mysql.sh >> /data/backup/backup.log 2>&1
set -euo pipefail

BACKUP_ROOT=/data/backup/mysql
DAILY_DIR=$BACKUP_ROOT/daily
WEEKLY_DIR=$BACKUP_ROOT/weekly
DAILY_KEEP_DAYS=7
WEEKLY_KEEP_DAYS=35
DB_NAME=recycle
CONTAINER=recycle-mysql

ENV_FILE="$(dirname "$0")/.env"
MYSQL_ROOT_PASSWORD=$(grep -E '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)

mkdir -p "$DAILY_DIR" "$WEEKLY_DIR"
STAMP=$(date +%Y%m%d_%H%M%S)
OUT="$DAILY_DIR/${DB_NAME}_${STAMP}.sql.gz"

echo "[$(date '+%F %T')] backup start -> $OUT"
docker exec "$CONTAINER" mysqldump \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF "$DB_NAME" | gzip > "$OUT"

gzip -t "$OUT"
[ "$(stat -c%s "$OUT")" -gt 1024 ] || { echo "backup too small, abort"; exit 1; }

if [ "$(date +%u)" = "7" ]; then
  cp "$OUT" "$WEEKLY_DIR/"
fi

find "$DAILY_DIR" -name '*.sql.gz' -mtime +$DAILY_KEEP_DAYS -delete
find "$WEEKLY_DIR" -name '*.sql.gz' -mtime +$WEEKLY_KEEP_DAYS -delete

echo "[$(date '+%F %T')] backup done, daily=$(ls "$DAILY_DIR" | wc -l) weekly=$(ls "$WEEKLY_DIR" | wc -l)"
