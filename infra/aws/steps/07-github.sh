#!/usr/bin/env bash
# Configura no GitHub: environment "staging" e as variáveis usadas pelos workflows. Requer a CLI "gh" autenticada.
source "$(dirname "$0")/../lib.sh"
ECR_ROLE="$(state_get ECR_ROLE_ARN)"; DEPLOY_ROLE="$(state_get DEPLOY_ROLE_ARN)"
INSTANCE_ID="$(state_get INSTANCE_ID)"; SITE="${DOMAIN:-$(state_get SSLIP_DOMAIN)}"
[[ -n "$ECR_ROLE" && -n "$DEPLOY_ROLE" ]] || die "rode o passo 04 antes"

declare -A REPO_VARS=( [AWS_REGION]="$AWS_REGION" [AWS_ECR_ROLE_ARN]="$ECR_ROLE" )
declare -A ENV_VARS=( [AWS_DEPLOY_ROLE_ARN]="$DEPLOY_ROLE" [AWS_INSTANCE_ID]="$INSTANCE_ID"
                      [ARTIFACT_BUCKET]="$BUCKET" [STAGING_URL]="https://${SITE}" )

if ! command -v gh >/dev/null || ! gh auth status >/dev/null 2>&1; then
  warn "gh ausente ou não autenticado. Configure manualmente em Settings > Secrets and variables > Actions:"
  echo "  Variáveis do REPOSITÓRIO:"; for k in "${!REPO_VARS[@]}"; do echo "    $k = ${REPO_VARS[$k]}"; done
  echo "  Crie o environment 'staging' e adicione as variáveis dele:"; for k in "${!ENV_VARS[@]}"; do echo "    $k = ${ENV_VARS[$k]}"; done
  exit 0
fi
log "criando o environment staging em $GH_REPO"
w gh api -X PUT "repos/${GH_REPO}/environments/staging" >/dev/null
for k in "${!REPO_VARS[@]}"; do w gh variable set "$k" --repo "$GH_REPO" --body "${REPO_VARS[$k]}"; done
for k in "${!ENV_VARS[@]}"; do w gh variable set "$k" --repo "$GH_REPO" --env staging --body "${ENV_VARS[$k]}"; done
log "ok. Falta (manual): restringir o environment staging à branch master e proteger a master (ver docs/DEPLOY-AWS-STAGING.md)"
