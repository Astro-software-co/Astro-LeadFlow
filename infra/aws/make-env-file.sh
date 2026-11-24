#!/usr/bin/env bash
# Gera, SEM acessar a AWS, o bloco de configuração do staging (chaves, segredos aleatórios, URLs) para colar
# no console do SSM como UM parâmetro SecureString chamado /leadflow/staging/ENV_FILE.
#   ./infra/aws/make-env-file.sh --account 123456789012 --ip 3.91.10.20            -> domínio <ip>.sslip.io
#   ./infra/aws/make-env-file.sh --account 123456789012 --domain app.seudominio.com.br
# O resultado contém SEGREDOS: salve num arquivo local fora do git, cole no console e apague o arquivo.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE="$DIR/../prod/staging.params.template"
ACCOUNT=""; DOMAIN=""; IP=""; REGION="us-east-1"; ENVNAME="staging"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --account) ACCOUNT="$2"; shift ;; --domain) DOMAIN="$2"; shift ;; --ip) IP="$2"; shift ;;
    --region) REGION="$2"; shift ;; --env) ENVNAME="$2"; shift ;;
    -h|--help) sed -n '2,8p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "opção desconhecida: $1" >&2; exit 1 ;;
  esac; shift
done
[[ "$ACCOUNT" =~ ^[0-9]{12}$ ]] || { echo "informe --account com os 12 dígitos da conta AWS" >&2; exit 1; }
if [[ -z "$DOMAIN" ]]; then
  [[ "$IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "informe --domain ou --ip (IP elástico)" >&2; exit 1; }
  DOMAIN="${IP//./-}.sslip.io"
fi
REGISTRY="${ACCOUNT}.dkr.ecr.${REGION}.amazonaws.com"
BUCKET="leadflow-${ENVNAME}-${ACCOUNT}"

gen() ( set +o pipefail; LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c "$1" )
KEYS=(); declare -A VAL
while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
  k="${line%%=*}"; v="${line#*=}"
  v="${v//\{\{DOMAIN\}\}/$DOMAIN}"; v="${v//\{\{IMAGE_REGISTRY\}\}/$REGISTRY}"
  v="${v//\{\{BUCKET\}\}/$BUCKET}"; v="${v//\{\{REGION\}\}/$REGION}"
  case "$v" in "{{GEN:alnum32}}") v="$(gen 32)" ;; "{{GEN:alnum64}}") v="$(gen 64)" ;; esac
  KEYS+=("$k"); VAL["$k"]="$v"
done < "$TEMPLATE"
for k in "${KEYS[@]}"; do
  if [[ "${VAL[$k]}" == "{{SAME:"* ]]; then ref="${VAL[$k]#\{\{SAME:}"; ref="${ref%\}\}}"; VAL["$k"]="${VAL[$ref]}"; fi
done
for k in "${KEYS[@]}"; do printf '%s=%s\n' "$k" "${VAL[$k]}"; done
