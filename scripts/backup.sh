#!/usr/bin/env bash
set -euo pipefail

# MyBlog 生产数据自动备份脚本
# 支持备份 MySQL 数据库 (youlai_admin)、Redis 快照及上传文件卷 (myblog_myblog_uploads)
# 建议加入 crontab 每日凌晨自动执行：
#   0 3 * * * /root/projects/myblog/scripts/backup.sh >> /var/log/myblog-backup.log 2>&1

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_BASE="${BACKUP_DIR:-${ROOT_DIR}/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
TARGET_DIR="${BACKUP_BASE}/${TIMESTAMP}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line#"${line%%[![:space:]]*}"}"
    [[ -z "${line}" || "${line}" == \#* || "${line}" != *=* ]] && continue
    key="${line%%=*}"
    key="${key%"${key##*[![:space:]]}"}"
    val="${line#*=}"
    val="${val#\"}"
    val="${val%\"}"
    val="${val#\'}"
    val="${val%\'}"
    if [[ "${key}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      export "${key}=${val}"
    fi
  done < "${ROOT_DIR}/.env"
fi

MYSQL_CONTAINER="${MYSQL_CONTAINER:-myblog-mysql}"
REDIS_CONTAINER="${REDIS_CONTAINER:-myblog-redis}"
UPLOADS_VOLUME="${UPLOADS_VOLUME:-myblog_myblog_uploads}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-123456}"
REDIS_PASSWORD="${REDIS_PASSWORD:-123456}"
DB_NAME="${DB_NAME:-youlai_admin}"

mkdir -p "${TARGET_DIR}"

echo "[$(date -Is)] 开始备份 MySQL 数据库 (${DB_NAME})..."
docker exec "${MYSQL_CONTAINER}" mysqldump \
  -uroot \
  -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  "${DB_NAME}" | gzip > "${TARGET_DIR}/mysql-${DB_NAME}-${TIMESTAMP}.sql.gz"

echo "[$(date -Is)] 开始备份 Redis 持久化快照..."
if docker exec "${REDIS_CONTAINER}" redis-cli -a "${REDIS_PASSWORD}" BGSAVE >/dev/null 2>&1; then
  sleep 2
  docker cp "${REDIS_CONTAINER}:/data/dump.rdb" "${TARGET_DIR}/redis-dump-${TIMESTAMP}.rdb" || true
fi

echo "[$(date -Is)] 开始备份上传资源卷 (${UPLOADS_VOLUME})..."
if docker volume inspect "${UPLOADS_VOLUME}" >/dev/null 2>&1; then
  docker run --rm \
    -v "${UPLOADS_VOLUME}:/source:ro" \
    -v "${TARGET_DIR}:/backup" \
    alpine:latest \
    tar czf "/backup/uploads-${TIMESTAMP}.tar.gz" -C /source .
fi

echo "[$(date -Is)] 清理超过 ${RETENTION_DAYS} 天的历史备份..."
find "${BACKUP_BASE}" -mindepth 1 -maxdepth 1 -type d -mtime +"${RETENTION_DAYS}" -exec rm -rf {} +

echo "[$(date -Is)] 备份完成 -> ${TARGET_DIR}"
ls -lh "${TARGET_DIR}"
