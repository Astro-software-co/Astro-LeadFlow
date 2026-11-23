#!/usr/bin/env bash
# Deploy de uma tag de imagem na EC2, com verificação de saúde e rollback automático para a última tag boa.
# Uso: deploy.sh <image_tag>          (chamado pelo GitHub Actions via SSM)
set -euo pipefail
export HOME="${HOME:-/root}"
APP_DIR="${APP_DIR:-/opt/leadflow}"
cd "$APP_DIR"

TAG="${1:?uso: deploy.sh <image_tag>}"
[[ "$TAG" =~ ^[A-Za-z0-9._-]+$ ]] || { echo "tag inválida: $TAG" >&2; exit 1; }
COMPOSE=(docker compose -f docker-compose.prod.yml)
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-300}"

echo "==> gerando .env a partir do SSM"
./render-env.sh

REGISTRY="$(grep -E '^IMAGE_REGISTRY=' .env | cut -d= -f2-)"
[[ -n "$REGISTRY" ]] || { echo "IMAGE_REGISTRY ausente no SSM" >&2; exit 1; }
REGISTRY_HOST="${REGISTRY%%/*}"
REGION="$(echo "$REGISTRY_HOST" | cut -d. -f4)"
PREV_TAG="$(cat .last_good_tag 2>/dev/null || true)"

echo "==> login no ECR ($REGISTRY_HOST)"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY_HOST" >/dev/null

# Aguarda o container ficar saudável (usa o healthcheck do compose quando existe).
wait_healthy() { # serviço timeout
  local svc="$1" limit="$2" waited=0 cid state
  while (( waited < limit )); do
    cid="$("${COMPOSE[@]}" ps -q "$svc" 2>/dev/null || true)"
    if [[ -n "$cid" ]]; then
      state="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo none)"
      [[ "$state" == "healthy" || "$state" == "running" ]] && return 0
      [[ "$state" == "exited" || "$state" == "dead" ]] && return 1
    fi
    sleep 5; waited=$((waited + 5))
  done
  return 1
}

start_stack() { # tag
  export IMAGE_TAG="$1"
  echo "==> subindo a stack com a tag $IMAGE_TAG"
  "${COMPOSE[@]}" pull
  "${COMPOSE[@]}" up -d --remove-orphans
  wait_healthy backend "$HEALTH_TIMEOUT" && wait_healthy frontend 60 && wait_healthy caddy 60
}

if start_stack "$TAG"; then
  echo "$TAG" > .last_good_tag
  docker image prune -af --filter "until=168h" >/dev/null 2>&1 || true
  echo "==> deploy OK: $TAG"
  "${COMPOSE[@]}" ps
  exit 0
fi

echo "!! a tag $TAG não ficou saudável. Últimos logs do backend:" >&2
"${COMPOSE[@]}" logs --tail 80 backend >&2 || true

if [[ -n "$PREV_TAG" && "$PREV_TAG" != "$TAG" ]]; then
  echo "==> rollback para $PREV_TAG" >&2
  if start_stack "$PREV_TAG"; then echo "rollback concluído ($PREV_TAG)" >&2; else echo "ROLLBACK TAMBÉM FALHOU" >&2; fi
else
  echo "(sem tag anterior para rollback; primeiro deploy)" >&2
fi
exit 1
