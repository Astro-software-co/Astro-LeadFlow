#!/usr/bin/env bash
# Checagem prévia (somente leitura / dry-run): mostra quem você é e descobre travas de permissão ou de
# políticas da organização (SCP) ANTES de criar qualquer recurso. Não altera nada na conta.
#   ./infra/aws/preflight.sh
source "$(dirname "$0")/lib.sh"

ARN="$(aws sts get-caller-identity --query Arn --output text)"
log "identidade : $ARN"
log "conta/região: $ACCOUNT_ID / $AWS_REGION"
[[ "$ARN" == *":root" ]] && warn "você está usando o usuário RAIZ: evite (crie um administrador e ative o MFA)"

ORG_OUT="$(aws organizations describe-organization --query 'Organization.MasterAccountId' --output text 2>&1 || true)"
case "$ORG_OUT" in
  *AWSOrganizationsNotInUseException*) log "organização : conta independente (sem AWS Organizations)" ;;
  *AccessDenied*|*explicit\ deny*)     warn "organização : a conta parece fazer parte de uma Organization (consulta negada)" ;;
  [0-9]*)                              warn "organização : conta-membro; gerenciada pela conta $ORG_OUT (SCPs podem bloquear serviços)" ;;
  *)                                   log  "organização : $ORG_OUT" ;;
esac

FAILS=0
probe() { # descrição  comando...
  local desc="$1"; shift
  local out; out="$("$@" 2>&1)" && rc=0 || rc=$?
  if [[ $rc -eq 0 ]]; then printf '  \033[32mOK\033[0m        %s\n' "$desc"
  elif [[ "$out" == *"explicit deny in a service control policy"* ]]; then
    printf '  \033[31mBLOQUEADO\033[0m %s  (política da organização/SCP)\n' "$desc"; FAILS=$((FAILS+1))
  elif [[ "$out" == *DryRunOperation* ]]; then printf '  \033[32mOK\033[0m        %s (dry-run autorizado)\n' "$desc"
  elif [[ "$out" == *AccessDenied* || "$out" == *UnauthorizedOperation* || "$out" == *"not authorized"* ]]; then
    printf '  \033[33mSEM PERMISSÃO\033[0m %s\n' "$desc"; FAILS=$((FAILS+1))
  else printf '  \033[33m?\033[0m         %s -> %s\n' "$desc" "$(echo "$out" | head -1 | cut -c1-110)"; fi
}

echo "Leitura nos serviços usados:"
probe "EC2   (listar instâncias)"          aws ec2 describe-instances --max-items 1
probe "ECR   (listar repositórios)"         aws ecr describe-repositories --max-items 1
probe "S3    (listar buckets)"              aws s3api list-buckets
probe "IAM   (listar provedores OIDC)"      aws iam list-open-id-connect-providers
probe "SSM   (listar parâmetros)"           aws ssm describe-parameters --max-items 1
probe "Budgets (listar orçamentos)"         aws budgets describe-budgets --account-id "$ACCOUNT_ID" --max-items 1

echo "Escrita (dry-run, nada é criado):"
AMI="$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 --query Parameter.Value --output text 2>/dev/null || true)"
if [[ -n "$AMI" ]]; then
  probe "EC2 ${INSTANCE_TYPE} pode ser lançada" aws ec2 run-instances --dry-run --image-id "$AMI" --instance-type "$INSTANCE_TYPE"
else warn "não consegui obter a AMI do Amazon Linux 2023 (SSM)"; fi
probe "Elastic IP pode ser alocado"          aws ec2 allocate-address --domain vpc --dry-run

echo
if [[ $FAILS -eq 0 ]]; then log "nenhuma trava encontrada nas checagens acima"
else warn "$FAILS checagem(ns) com bloqueio/sem permissão. Resolva (ou me envie esta saída) antes de rodar o bootstrap."; fi
log "limites que esta checagem NÃO cobre: criar roles/OIDC (IAM) e orçamento só são testados de fato nos passos 4 e 1."
exit 0
