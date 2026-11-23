#!/usr/bin/env bash
# Define/atualiza UM parâmetro do SSM sem deixar o valor no histórico do shell.
#   ./infra/aws/set-param.sh ABACATEPAY_API_KEY       (pede o valor, sem eco)
#   echo -n "valor" | ./infra/aws/set-param.sh CHAVE  (lê do stdin)
# Depois, faça um novo deploy (Actions > Deploy) para a EC2 regenerar o .env.
source "$(dirname "$0")/lib.sh"
KEY="${1:?uso: set-param.sh CHAVE}"
[[ "$KEY" =~ ^[A-Z][A-Z0-9_]*$ ]] || die "nome de chave inválido: $KEY"
if [[ -t 0 ]]; then read -r -s -p "Valor de $KEY: " VALUE; echo; else VALUE="$(cat)"; fi
[[ -n "$VALUE" ]] || die "valor vazio (o SSM não aceita vazio)"
w aws ssm put-parameter --name "/${PROJECT}/${ENVNAME}/${KEY}" --type SecureString --overwrite --value "$VALUE" >/dev/null
log "atualizado: /${PROJECT}/${ENVNAME}/${KEY}"
