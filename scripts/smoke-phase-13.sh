#!/usr/bin/env bash
set -Eeuo pipefail

: "${ASTRO_ADMIN_TOKEN:?required}"
: "${DEALERSHIP_TOKEN:?required}"

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
TMP_DIR="$(mktemp -d)"
LAST_BODY=""
LAST_STATUS=""

trap 'rm -rf "$TMP_DIR"' EXIT

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

request() {
  local name="$1"
  local method="$2"
  local path="$3"
  local token="${4:-}"
  local data="${5:-}"
  local body="$TMP_DIR/${name//[^a-zA-Z0-9]/_}.json"
  local status
  local args=(-sS -o "$body" -w "%{http_code}" -X "$method" "$BASE_URL$path" -H "Accept: application/json")

  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer $token")
  fi

  if [[ -n "$data" ]]; then
    args+=(-H "Content-Type: application/json" -d "$data")
  fi

  status="$(curl "${args[@]}")" || status="000"
  LAST_BODY="$body"
  LAST_STATUS="$status"
  echo "SMOKE ${name}: status=${status} body=$(head -c 500 "$body" | tr '\n' ' ')"
}

expect_status_one_of() {
  local status="$1"
  shift
  local expected
  for expected in "$@"; do
    if [[ "$status" == "$expected" ]]; then
      return 0
    fi
  done
  fail "Expected status ${*}, got ${status}. Body: $(cat "$LAST_BODY")"
}

expect_2xx() {
  local status="$1"
  [[ "$status" =~ ^2[0-9][0-9]$ ]] || fail "Expected 2xx, got ${status}. Body: $(cat "$LAST_BODY")"
}

expect_body_contains() {
  local pattern="$1"
  grep -q "$pattern" "$LAST_BODY" || fail "Expected body to contain ${pattern}. Body: $(cat "$LAST_BODY")"
}

stamp="$(date +%s%N)"
cnpj="$(printf '91%012d' "$((stamp % 1000000000000))")"
owner_email="phase13.${stamp}@astro.test"
payload="$(cat <<JSON
{
  "companyName": "Smoke Phase 13 ${stamp}",
  "tradeName": "Smoke 13",
  "cnpj": "${cnpj}",
  "planCode": "PROFISSIONAL_MONTHLY",
  "ownerName": "Smoke Owner",
  "ownerCpf": "12345678901",
  "ownerEmail": "${owner_email}",
  "ownerPhone": "11999999999",
  "cep": "01001000",
  "street": "Praca da Se",
  "number": "100",
  "neighborhood": "Se",
  "city": "Sao Paulo",
  "state": "SP",
  "ownerRole": "Diretor",
  "dealershipSegment": "Seminovos",
  "branchCount": 1,
  "employeeCount": 8,
  "stockCount": 40,
  "monthlyLeadsAvg": 120,
  "marketYears": 5,
  "intendedMarketplaces": ["WEBMOTORS"]
}
JSON
)"

request "public onboarding submit" POST "/api/onboarding" "" "$payload"
expect_2xx "$LAST_STATUS"
onboarding_id="$(grep -oE '"id"[[:space:]]*:[[:space:]]*"[^"]+"' "$LAST_BODY" | head -1 | sed -E 's/.*"([^"]+)"/\1/')"
[[ -n "$onboarding_id" ]] || fail "Could not parse onboarding id. Body: $(cat "$LAST_BODY")"

request "admin onboarding unauthenticated" GET "/api/admin/onboarding" "" ""
expect_status_one_of "$LAST_STATUS" 401 403

request "admin dashboard unauthenticated" GET "/api/v1/admin/dashboard" "" ""
expect_status_one_of "$LAST_STATUS" 401 403

request "admin dashboard dealership forbidden" GET "/api/v1/admin/dashboard" "$DEALERSHIP_TOKEN" ""
expect_status_one_of "$LAST_STATUS" 403

request "admin onboarding list" GET "/api/admin/onboarding" "$ASTRO_ADMIN_TOKEN" ""
expect_2xx "$LAST_STATUS"

request "admin onboarding approve" POST "/api/admin/onboarding/${onboarding_id}/approve" "$ASTRO_ADMIN_TOKEN" ""
expect_2xx "$LAST_STATUS"
expect_body_contains "referralCode"

request "admin dashboard authorized" GET "/api/v1/admin/dashboard" "$ASTRO_ADMIN_TOKEN" ""
expect_2xx "$LAST_STATUS"
expect_body_contains "metrics"
expect_body_contains "mrrAtual"
expect_body_contains "receitaDoMes"
expect_body_contains "subscriptions"

request "dealership referral my-code" GET "/api/referral/my-code" "$DEALERSHIP_TOKEN" ""
expect_2xx "$LAST_STATUS"
expect_body_contains "referralLink"
expect_body_contains "balanceBrl"

request "dealership referral my-referrals" GET "/api/referral/my-referrals" "$DEALERSHIP_TOKEN" ""
expect_2xx "$LAST_STATUS"

request "dealership referral my-balance" GET "/api/referral/my-balance" "$DEALERSHIP_TOKEN" ""
expect_2xx "$LAST_STATUS"
expect_body_contains "balanceBrl"

request "admin referral summary" GET "/api/admin/referral/summary" "$ASTRO_ADMIN_TOKEN" ""
expect_2xx "$LAST_STATUS"
expect_body_contains "topReferrers"

echo "PASS: Phase 13 smoke tests completed."
