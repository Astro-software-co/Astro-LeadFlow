#!/usr/bin/env bash
# Cria TODA a infraestrutura do ambiente (budget, ECR, bucket, IAM/OIDC, EC2, parâmetros, GitHub). Idempotente.
#   ./infra/aws/bootstrap.sh              executa tudo
#   ./infra/aws/bootstrap.sh --dry-run    mostra o que seria criado, sem alterar nada
#   ./infra/aws/bootstrap.sh --only 4     executa só o passo 4 (ou 4,5)
#   ./infra/aws/bootstrap.sh --yes        não pede confirmação
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ONLY=""; ASSUME_YES=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) export DRY_RUN=1 ;;
    --only) ONLY="$2"; shift ;;
    --yes|-y) ASSUME_YES=1 ;;
    -h|--help) sed -n '2,8p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "opção desconhecida: $1" >&2; exit 1 ;;
  esac; shift
done

# shellcheck disable=SC1091
source "$DIR/lib.sh"
echo "Conta AWS : $ACCOUNT_ID"
echo "Região    : $AWS_REGION"
echo "Ambiente  : $ENVNAME   (repo GitHub: $GH_REPO)"
echo "Instância : $INSTANCE_TYPE   Bucket: $BUCKET"
[[ "${DRY_RUN:-0}" == "1" ]] && echo ">>> DRY RUN: nada será alterado <<<"
if [[ "$ASSUME_YES" != "1" && "${DRY_RUN:-0}" != "1" ]]; then
  read -r -p "Criar/atualizar os recursos acima nesta conta? [digite 'sim'] " ans
  [[ "$ans" == "sim" ]] || { echo "cancelado"; exit 1; }
fi

STEPS=(01-budget 02-ecr 03-bucket 04-iam 05-ec2 06-params 07-github)
n=0
for step in "${STEPS[@]}"; do
  n=$((n+1))
  if [[ -n "$ONLY" ]] && ! [[ ",$ONLY," == *",$n,"* ]]; then continue; fi
  echo; echo "=== passo $n: $step ==="
  bash "$DIR/steps/$step.sh"
done

echo; echo "Pronto. Estado salvo em $STATE_FILE"; [[ -f "$STATE_FILE" ]] && sed 's/^/  /' "$STATE_FILE"
echo "Próximo: criar a branch master, proteger e disparar o primeiro deploy (docs/DEPLOY-AWS-STAGING.md)."
