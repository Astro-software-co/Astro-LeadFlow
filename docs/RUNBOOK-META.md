# Runbook: Meta WhatsApp Business

## Variaveis

- `META_API_MODE=mock` em dev; use `real` apenas com credenciais reais.
- `META_APP_SECRET`: segredo usado para validar `X-Hub-Signature-256`.
- `META_VERIFY_TOKEN`: token configurado no GET de verificacao da Meta.
- `META_WHATSAPP_ACCESS_TOKEN`: bearer token para envio.
- `META_WHATSAPP_PHONE_NUMBER_ID`: phone number id remetente.
- `META_WEBHOOK_API_VERSION=v18.0`.

## Configurar Webhook

Base de implementacao: documentacao oficial Meta em
https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started

1. Gere ou escolha um `META_VERIFY_TOKEN` local e salve em `.env.local`.
2. Publique o backend via Cloudflared e atualize `PUBLIC_API_URL`.
3. No Meta Developer Console, configure:
   - Callback URL: `${PUBLIC_API_URL}/api/v1/webhooks/meta`
   - Verify token: valor de `META_VERIFY_TOKEN`
4. Assine eventos de mensagens e statuses.

## Validacao Local

Com o backend rodando em modo mock:

```bash
META_VERIFY_TOKEN=dev-webhook-token META_APP_SECRET=dev-secret scripts/validate-meta-integration.sh
```

Para sair do mock, configure `META_API_MODE=real`, `META_WHATSAPP_ACCESS_TOKEN` e `META_WHATSAPP_PHONE_NUMBER_ID`, reinicie o backend e refaca o teste de envio com uma conversa permitida pela Meta.

## Envio via Graph API

Em modo real, o backend chama `POST /{phone-number-id}/messages` em `https://graph.facebook.com/v18.0` com bearer `META_WHATSAPP_ACCESS_TOKEN`. O payload de texto contem `messaging_product=whatsapp`, `recipient_type=individual`, `to`, `type=text` e `text.body`.
