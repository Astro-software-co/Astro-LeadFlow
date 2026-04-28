#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
META_VERIFY_TOKEN="${META_VERIFY_TOKEN:-${META_WEBHOOK_TOKEN:-dev-webhook-token}}"
META_APP_SECRET="${META_APP_SECRET:-${META_WEBHOOK_SECRET:-dev-secret}}"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

verify_response="$(curl -sS -w '\n%{http_code}' \
  "$BASE_URL/api/v1/webhooks/meta?hub.mode=subscribe&hub.challenge=test123&hub.verify_token=$META_VERIFY_TOKEN")"
verify_body="$(printf '%s' "$verify_response" | sed '$d')"
verify_status="$(printf '%s' "$verify_response" | tail -1)"
[[ "$verify_status" == "200" ]] || fail "GET verify expected 200, got $verify_status: $verify_body"
[[ "$verify_body" == "test123" ]] || fail "GET verify expected challenge body, got: $verify_body"

payload='{"object":"whatsapp_business_account","entry":[{"id":"entry-1","changes":[{"field":"messages","value":{"messaging_product":"whatsapp","metadata":{"display_phone_number":"5511999999999","phone_number_id":"dev-phone"},"contacts":[{"profile":{"name":"Lead Teste"},"wa_id":"5511999999999"}],"messages":[{"from":"5511999999999","id":"wamid.test","timestamp":"1710000000","type":"text","text":{"body":"oi"}}]}}]}]}'
sig="$(printf '%s' "$payload" | openssl dgst -sha256 -hmac "$META_APP_SECRET" | awk '{print $2}')"
post_status="$(curl -sS -o /tmp/meta-inbound-response.txt -w '%{http_code}' \
  -X POST \
  -H "X-Hub-Signature-256: sha256=$sig" \
  -H "Content-Type: application/json" \
  -d "$payload" \
  "$BASE_URL/api/v1/webhooks/meta")"
[[ "$post_status" == "200" ]] || fail "POST inbound expected 200, got $post_status: $(cat /tmp/meta-inbound-response.txt)"

echo "PASS: Meta webhook verify and signed inbound smoke completed."
