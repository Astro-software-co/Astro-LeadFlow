#!/usr/bin/env bash
# OIDC do GitHub + roles (push no ECR, deploy via SSM) + role/instance profile da EC2. Sem chaves de acesso de longa duração.
source "$(dirname "$0")/../lib.sh"
POL="$AWS_DIR/policies"
OIDC_ARN="arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"

if aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$OIDC_ARN" >/dev/null 2>&1; then
  log "provedor OIDC do GitHub já existe"
else
  log "criando provedor OIDC do GitHub"
  w aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 1c58a3a8518e8759bf075b76b750d4f2df264fcd >/dev/null
fi

ensure_role() { # nome trust.json policy.json descrição
  local role="$1" trust="$2" policy="$3" desc="$4"
  if aws iam get-role --role-name "$role" >/dev/null 2>&1; then
    log "role $role existe; atualizando trust e política"
    w aws iam update-assume-role-policy --role-name "$role" --policy-document "$(render "$POL/$trust")"
  else
    log "criando role $role"
    w aws iam create-role --role-name "$role" --description "$desc" \
      --assume-role-policy-document "$(render "$POL/$trust")" \
      --tags Key=Project,Value="$PROJECT" Key=Environment,Value="$ENVNAME" >/dev/null
  fi
  w aws iam put-role-policy --role-name "$role" --policy-name inline --policy-document "$(render "$POL/$policy")"
}

ensure_role "${NAME}-gha-ecr"    github-trust-ecr.json    ecr-push.json "GitHub Actions: publicar imagens no ECR"
ensure_role "${NAME}-gha-deploy" github-trust-deploy.json deploy.json   "GitHub Actions: deploy via SSM"
ensure_role "${NAME}-instance"   ec2-trust.json           instance.json "EC2: puxar imagens, ler parâmetros, backups"
w aws iam attach-role-policy --role-name "${NAME}-instance" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore

if aws iam get-instance-profile --instance-profile-name "${NAME}-instance" >/dev/null 2>&1; then
  log "instance profile já existe"
else
  log "criando instance profile ${NAME}-instance"
  w aws iam create-instance-profile --instance-profile-name "${NAME}-instance" >/dev/null
  w aws iam add-role-to-instance-profile --instance-profile-name "${NAME}-instance" --role-name "${NAME}-instance"
  [[ "$DRY_RUN" == "1" ]] || { log "aguardando propagação do IAM (15s)"; sleep 15; }
fi

state_set ECR_ROLE_ARN    "arn:aws:iam::${ACCOUNT_ID}:role/${NAME}-gha-ecr"
state_set DEPLOY_ROLE_ARN "arn:aws:iam::${ACCOUNT_ID}:role/${NAME}-gha-deploy"
log "ok"
