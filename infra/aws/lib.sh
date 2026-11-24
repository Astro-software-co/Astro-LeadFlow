#!/usr/bin/env bash
# Funções comuns dos passos de infraestrutura. Uso: source "$(dirname "$0")/../lib.sh"
set -euo pipefail

AWS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$AWS_DIR/../.." && pwd)"
CONFIG_FILE="${CONFIG_FILE:-$AWS_DIR/config.env}"
STATE_DIR="$AWS_DIR/.state"
mkdir -p "$STATE_DIR"

[[ -f "$CONFIG_FILE" ]] || { echo "ERRO: crie $CONFIG_FILE a partir de config.env.example" >&2; exit 1; }
# shellcheck disable=SC1090
source "$CONFIG_FILE"

: "${GH_REPO:?defina GH_REPO em config.env}"
: "${AWS_REGION:=us-east-1}"
: "${ENVNAME:=staging}"
: "${BUDGET_LIMIT_USD:=100}"
: "${INSTANCE_TYPE:=t3.medium}"
: "${ROOT_VOLUME_GB:=30}"
: "${BACKUP_RETENTION_DAYS:=30}"
: "${DOMAIN:=}"
: "${BUDGET_EMAIL:=}"
export AWS_REGION AWS_DEFAULT_REGION="$AWS_REGION" AWS_PAGER=""
[[ -n "${AWS_PROFILE:-}" ]] && export AWS_PROFILE

PROJECT=leadflow
NAME="${PROJECT}-${ENVNAME}"
STATE_FILE="$STATE_DIR/${ENVNAME}.env"
DRY_RUN="${DRY_RUN:-0}"

command -v aws >/dev/null || { echo "ERRO: AWS CLI v2 não encontrado" >&2; exit 1; }
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)" \
  || { echo "ERRO: sem credenciais AWS válidas (aws sts get-caller-identity falhou)" >&2; exit 1; }
BUCKET="${NAME}-${ACCOUNT_ID}"
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
TAGS_KV="Key=Project,Value=${PROJECT} Key=Environment,Value=${ENVNAME}"

log()  { printf '\033[1;34m[%s]\033[0m %s\n' "$(basename "${0%.sh}")" "$*"; }
warn() { printf '\033[1;33m[aviso]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[erro]\033[0m %s\n' "$*" >&2; exit 1; }

# Executa comandos que ALTERAM a conta. Em DRY_RUN=1 só imprime.
w() {
  if [[ "$DRY_RUN" == "1" ]]; then printf '  (dry-run) %s\n' "$*" >&2; return 0; fi
  "$@"
}

# Substitui placeholders __X__ de um template (policies/*.json) e imprime o resultado.
render() {
  sed -e "s#__ACCOUNT_ID__#${ACCOUNT_ID}#g" -e "s#__REGION__#${AWS_REGION}#g" \
      -e "s#__GH_REPO__#${GH_REPO}#g" -e "s#__BUCKET__#${BUCKET}#g" \
      -e "s#__ENV__#${ENVNAME}#g" -e "s#__PROJECT__#${PROJECT}#g" "$1"
}

# Persistência simples de valores entre passos (infra/aws/.state/<env>.env, ignorado pelo git).
state_set() { touch "$STATE_FILE"; grep -v "^$1=" "$STATE_FILE" > "$STATE_FILE.tmp" || true; echo "$1=$2" >> "$STATE_FILE.tmp"; mv "$STATE_FILE.tmp" "$STATE_FILE"; }
state_get() { [[ -f "$STATE_FILE" ]] && grep "^$1=" "$STATE_FILE" | tail -1 | cut -d= -f2- || true; }
