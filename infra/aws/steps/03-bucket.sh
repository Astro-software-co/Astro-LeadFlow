#!/usr/bin/env bash
# Bucket único do ambiente: artifacts/ (pacote de deploy por release) e backups/postgres/.
source "$(dirname "$0")/../lib.sh"

if aws s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
  log "bucket $BUCKET já existe"
else
  log "criando bucket $BUCKET"
  if [[ "$AWS_REGION" == "us-east-1" ]]; then
    w aws s3api create-bucket --bucket "$BUCKET" >/dev/null
  else
    w aws s3api create-bucket --bucket "$BUCKET" --create-bucket-configuration "LocationConstraint=$AWS_REGION" >/dev/null
  fi
fi
w aws s3api put-public-access-block --bucket "$BUCKET" --public-access-block-configuration \
  BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
w aws s3api put-bucket-encryption --bucket "$BUCKET" --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
w aws s3api put-bucket-tagging --bucket "$BUCKET" --tagging "TagSet=[{Key=Project,Value=$PROJECT},{Key=Environment,Value=$ENVNAME}]"
# Backups contêm dados pessoais: a retenção abaixo precisa constar da política de privacidade.
w aws s3api put-bucket-lifecycle-configuration --bucket "$BUCKET" --lifecycle-configuration "{
  \"Rules\":[
    {\"ID\":\"expira-backups\",\"Status\":\"Enabled\",\"Filter\":{\"Prefix\":\"backups/\"},\"Expiration\":{\"Days\":${BACKUP_RETENTION_DAYS}}},
    {\"ID\":\"expira-artifacts\",\"Status\":\"Enabled\",\"Filter\":{\"Prefix\":\"artifacts/\"},\"Expiration\":{\"Days\":30}}
  ]}"
state_set BUCKET "$BUCKET"
log "ok (retenção de backups: ${BACKUP_RETENTION_DAYS} dias)"
