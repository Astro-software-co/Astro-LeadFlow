#!/usr/bin/env bash
# Cria os parâmetros do SSM (SecureString) em /leadflow/<env>/ a partir de infra/prod/staging.params.template.
# Segredos são gerados uma única vez; reexecutar NÃO altera o que já existe (use set-param.sh para trocar).
source "$(dirname "$0")/../lib.sh"
TEMPLATE="$REPO_ROOT/infra/prod/staging.params.template"
PREFIX="/${PROJECT}/${ENVNAME}"

SITE_DOMAIN="${DOMAIN:-$(state_get SSLIP_DOMAIN)}"
[[ -n "$SITE_DOMAIN" ]] || die "sem DOMAIN e sem IP da instância: rode o passo 05 antes (ou defina DOMAIN)"

# Subshell sem pipefail: 'tr' recebe SIGPIPE quando o 'head' fecha o pipe, e isso derrubaria o script.
gen() ( set +o pipefail; LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c "$1" )
declare -A VAL
while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
  key="${line%%=*}"; val="${line#*=}"
  val="${val//\{\{DOMAIN\}\}/$SITE_DOMAIN}"; val="${val//\{\{IMAGE_REGISTRY\}\}/$ECR_REGISTRY}"
  val="${val//\{\{BUCKET\}\}/$BUCKET}";      val="${val//\{\{REGION\}\}/$AWS_REGION}"
  VAL["$key"]="$val"
done < "$TEMPLATE"

created=0; kept=0
put_param() { # chave  (resolve placeholders de geração e SAME)
  local key="$1" val ref
  if aws ssm get-parameter --name "$PREFIX/$key" >/dev/null 2>&1; then kept=$((kept+1)); return 0; fi
  val="${VAL[$key]}"
  case "$val" in
    "{{GEN:alnum32}}") val="$(gen 32)" ;;
    "{{GEN:alnum64}}") val="$(gen 64)" ;;
    "{{SAME:"*"}}")    ref="${val#\{\{SAME:}"; ref="${ref%\}\}}"
                       val="$(aws ssm get-parameter --name "$PREFIX/$ref" --with-decryption --query Parameter.Value --output text 2>/dev/null || true)"
                       [[ -n "$val" ]] || { warn "$key depende de $ref, que não existe"; return 0; } ;;
  esac
  w aws ssm put-parameter --name "$PREFIX/$key" --type SecureString --value "$val" \
    --tags Key=Project,Value="$PROJECT" Key=Environment,Value="$ENVNAME" >/dev/null
  created=$((created+1))
}
# 1ª passada: tudo que não depende de outro parâmetro; 2ª: os {{SAME:...}}
for key in "${!VAL[@]}"; do [[ "${VAL[$key]}" == "{{SAME:"* ]] || put_param "$key"; done
for key in "${!VAL[@]}"; do [[ "${VAL[$key]}" == "{{SAME:"* ]] && put_param "$key"; done
log "parâmetros criados: $created | já existiam (mantidos): $kept | prefixo $PREFIX"
log "domínio configurado: $SITE_DOMAIN"
