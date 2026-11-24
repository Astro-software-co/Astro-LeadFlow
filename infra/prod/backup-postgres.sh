#!/usr/bin/env bash
# Dump diário do Postgres do Compose para s3://<bucket>/backups/postgres/. Chamado pelo timer systemd.
set -euo pipefail
export HOME="${HOME:-/root}"
APP_DIR="${APP_DIR:-/opt/leadflow}"
cd "$APP_DIR"
getenv() { grep -E "^$1=" .env | head -1 | cut -d= -f2-; }

USER_="$(getenv POSTGRES_USER)"; DB="$(getenv POSTGRES_DB)"; BUCKET="$(getenv BACKUP_BUCKET)"
[[ -n "$USER_" && -n "$DB" && -n "$BUCKET" ]] || { echo "POSTGRES_USER/POSTGRES_DB/BACKUP_BUCKET ausentes no .env" >&2; exit 1; }
TOKEN="$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')"
export AWS_REGION="$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)"

KEY="backups/postgres/${DB}-$(date -u +%Y-%m-%dT%H%M%SZ).sql.gz"
docker compose -f docker-compose.prod.yml exec -T postgres pg_dump -U "$USER_" -d "$DB" --no-owner \
  | gzip | aws s3 cp - "s3://${BUCKET}/${KEY}" --only-show-errors
echo "backup enviado: s3://${BUCKET}/${KEY}"
