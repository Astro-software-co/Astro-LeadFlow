#!/usr/bin/env bash
# Repositórios ECR (backend e frontend) com varredura no push e retenção das 20 imagens mais recentes.
source "$(dirname "$0")/../lib.sh"

LIFECYCLE='{"rules":[{"rulePriority":1,"description":"manter as 20 imagens mais recentes","selection":{"tagStatus":"any","countType":"imageCountMoreThan","countNumber":20},"action":{"type":"expire"}}]}'
for svc in backend frontend; do
  repo="astro-leadflow-${svc}"
  if aws ecr describe-repositories --repository-names "$repo" >/dev/null 2>&1; then
    log "repositório $repo já existe"
  else
    log "criando repositório $repo"
    w aws ecr create-repository --repository-name "$repo" \
      --image-scanning-configuration scanOnPush=true \
      --encryption-configuration encryptionType=AES256 \
      --tags Key=Project,Value="$PROJECT" Key=Environment,Value="$ENVNAME" >/dev/null
  fi
  w aws ecr put-lifecycle-policy --repository-name "$repo" --lifecycle-policy-text "$LIFECYCLE" >/dev/null
done
state_set ECR_REGISTRY "$ECR_REGISTRY"
log "registry: $ECR_REGISTRY"
