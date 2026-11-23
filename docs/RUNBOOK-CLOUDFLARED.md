# Runbook: Cloudflared URL Dinamica

## Objetivo

Em dev, o backend pode ser publicado por quick tunnel do Cloudflare. Como a URL muda a cada restart, `PUBLIC_API_URL` deve ser atualizado antes de validar webhooks externos.

## Fluxo

1. Suba o backend local em `http://localhost:8080`.
2. Inicie o tunnel:
   ```bash
   cloudflared tunnel --url http://localhost:8080 > /tmp/cloudflared.log 2>&1 &
   ```
3. Atualize `.env.local`:
   ```bash
   scripts/update-public-url.sh /tmp/cloudflared.log .env.local
   ```
4. Reinicie o backend para recarregar `PUBLIC_API_URL`.
5. Atualize manualmente os paineis externos:
   - Meta Developer Console: webhook URL para `${PUBLIC_API_URL}/api/v1/webhooks/meta`
   - AbacatePay Painel: webhook URL para `${PUBLIC_API_URL}/api/v1/webhooks/abacatepay`

## Variaveis

- `APP_BASE_URL`: frontend usado em callbacks de checkout, por padrao `http://localhost:3001`.
- `PUBLIC_API_URL`: backend publico usado por webhooks, por padrao `http://localhost:8080`.
