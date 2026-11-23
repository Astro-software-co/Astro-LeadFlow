#!/usr/bin/env bash
# Orçamento mensal com alertas em 50% e 80% (real) e 100% (previsto). Faça ANTES de criar qualquer recurso.
source "$(dirname "$0")/../lib.sh"
[[ -n "$BUDGET_EMAIL" ]] || die "defina BUDGET_EMAIL em config.env"

if aws budgets describe-budget --account-id "$ACCOUNT_ID" --budget-name "${NAME}-monthly" >/dev/null 2>&1; then
  log "orçamento ${NAME}-monthly já existe; nada a fazer"; exit 0
fi
log "criando orçamento de US\$ ${BUDGET_LIMIT_USD}/mês com alertas para ${BUDGET_EMAIL}"
notif() { # tipo limiar
  printf '{"Notification":{"NotificationType":"%s","ComparisonOperator":"GREATER_THAN","Threshold":%s,"ThresholdType":"PERCENTAGE"},"Subscribers":[{"SubscriptionType":"EMAIL","Address":"%s"}]}' "$1" "$2" "$BUDGET_EMAIL"
}
w aws budgets create-budget --account-id "$ACCOUNT_ID" \
  --budget "{\"BudgetName\":\"${NAME}-monthly\",\"BudgetLimit\":{\"Amount\":\"${BUDGET_LIMIT_USD}\",\"Unit\":\"USD\"},\"TimeUnit\":\"MONTHLY\",\"BudgetType\":\"COST\"}" \
  --notifications-with-subscribers "[$(notif ACTUAL 50),$(notif ACTUAL 80),$(notif FORECASTED 100)]"
log "ok"
