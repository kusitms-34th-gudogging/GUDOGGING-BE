#!/usr/bin/env bash
# 운영 DB 서버(가비아)용 MySQL 일일 백업. 압축 파일로 저장하고 보관 기간이 지난 파일은 삭제한다.
#
# 설치 (DB 서버에서 1회):
#   mkdir -p /home/ubuntu/gudogging/scripts /home/ubuntu/backups/mysql
#   (이 파일을 /home/ubuntu/gudogging/scripts/mysql-backup.sh 로 복사) && chmod +x /home/ubuntu/gudogging/scripts/mysql-backup.sh
#   crontab -e 에 추가 → 매일 새벽 4시 실행:
#   0 4 * * * /home/ubuntu/gudogging/scripts/mysql-backup.sh >> /home/ubuntu/backups/mysql/backup.log 2>&1
#
# 복구 (예시):
#   gunzip -c /home/ubuntu/backups/mysql/gudogging-YYYYmmdd-HHMM.sql.gz \
#     | docker exec -i gudogging-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'
#
# 비밀번호는 컨테이너 안의 환경변수(MYSQL_ROOT_PASSWORD)를 쓰므로 이 파일 · 명령줄 · cron에 노출되지 않는다.
set -euo pipefail

CONTAINER="${CONTAINER:-gudogging-mysql}"
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/backups/mysql}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
FILE="$BACKUP_DIR/gudogging-$(date +%Y%m%d-%H%M).sql.gz"
TMP="$FILE.partial"
trap 'rm -f "$TMP"' EXIT

# --single-transaction: 서비스 중에도 잠금 없이 일관된 스냅샷 (InnoDB)
docker exec "$CONTAINER" sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --triggers --events --databases "$MYSQL_DATABASE"' \
  | gzip > "$TMP"

# 덤프가 비정상 종료되면 pipefail로 여기 오지 않음. 마지막 줄의 완료 표시까지 확인
gunzip -c "$TMP" | tail -n 1 | grep -q "Dump completed" || { echo "$(date '+%F %T') 백업 실패: 덤프가 완료되지 않음"; exit 1; }
mv "$TMP" "$FILE"
chmod 600 "$FILE"

find "$BACKUP_DIR" -name 'gudogging-*.sql.gz' -type f -mtime +"$((RETENTION_DAYS - 1))" -delete
echo "$(date '+%F %T') 백업 완료: $(basename "$FILE") ($(du -h "$FILE" | cut -f1)), 보관 ${RETENTION_DAYS}일"
