#!/usr/bin/env bash
# Troca o domínio do ambiente (ex.: de <ip>.sslip.io para app.seudominio.com.br).
# Antes: crie no DNS um registro A apontando para o IP elástico (ver state: PUBLIC_IP).
#   ./infra/aws/switch-domain.sh app.seudominio.com.br
source "$(dirname "$0")/lib.sh"
NEW="${1:?uso: switch-domain.sh <dominio>}"
[[ "$NEW" =~ ^[a-zA-Z0-9.-]+$ ]] || die "domínio inválido"
P="/${PROJECT}/${ENVNAME}"
for kv in "DOMAIN=$NEW" "APP_BASE_URL=https://$NEW" "PUBLIC_API_URL=https://$NEW" \
          "APP_CORS_ALLOWED_ORIGINS=https://$NEW" "ML_REDIRECT_URI=https://$NEW/api/v1/auth/mercadolivre/callback"; do
  w aws ssm put-parameter --name "$P/${kv%%=*}" --type SecureString --overwrite --value "${kv#*=}" >/dev/null
  log "atualizado ${kv%%=*}"
done
state_set DOMAIN "$NEW"
log "Agora: (1) atualize a variável STAGING_URL do environment no GitHub para https://$NEW; (2) rode o deploy."
log "IP para o registro A: $(state_get PUBLIC_IP)"
