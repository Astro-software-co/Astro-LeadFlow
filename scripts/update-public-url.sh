#!/usr/bin/env bash
set -euo pipefail

LOG_FILE="${1:-/tmp/cloudflared.log}"
ENV_FILE="${2:-./.env.local}"

URL=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOG_FILE" | tail -1)
if [ -z "$URL" ]; then
  echo "ERRO: nao encontrei URL trycloudflare em $LOG_FILE"
  echo "Sugestao: rode 'cloudflared tunnel --url http://localhost:8080 > /tmp/cloudflared.log 2>&1 &'"
  exit 1
fi

if grep -q '^PUBLIC_API_URL=' "$ENV_FILE" 2>/dev/null; then
  sed -i.bak "s|^PUBLIC_API_URL=.*|PUBLIC_API_URL=$URL|" "$ENV_FILE"
else
  echo "PUBLIC_API_URL=$URL" >> "$ENV_FILE"
fi

echo "PUBLIC_API_URL=$URL"
echo ""
echo "Lembre-se de atualizar manualmente:"
echo "  - Meta Developer Console -> Webhook URL"
echo "  - AbacatePay Painel -> Webhook URL"
