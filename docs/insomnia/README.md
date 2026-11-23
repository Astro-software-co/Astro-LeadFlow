# Astro LeadFlow — Insomnia Collection

Arquivo importavel:

- `astro-leadflow-insomnia.json`

## Como importar

1. Abra o Insomnia.
2. Use `Create > Import from File`.
3. Selecione `docs/insomnia/astro-leadflow-insomnia.json`.
4. Ajuste o ambiente `Base Environment`.

## Variaveis principais

- `base_url`: backend local ou remoto. Exemplo: `http://localhost:8080`.
- `access_token`: JWT usado no Bearer Auth das rotas autenticadas.
- `x_api_key`: chave da organizacao para `POST /api/v1/webhooks/scraper`.
- `webhook_token`: token estatico para webhooks Webmotors e Mercado Livre.
- `meta_verify_token`: token usado no GET de verificacao do webhook Meta.
- `abacatepay_webhook_secret`: secret usado no query param do webhook AbacatePay.
- IDs de teste: `lead_id`, `vehicle_id`, `plan_id`, `onboarding_id`, `bot_session_id`, etc.

## Fluxo recomendado

1. Rode `00 Auth & Sessao > Login`.
2. Copie `accessToken` da resposta para a variavel `access_token`.
3. Teste primeiro:
   - `01 IAM - Usuarios > Get Me`
   - `02 IAM - Organizacao > Get My Organization`
   - `11 SaaS - Planos Billing Marketplaces > List Public Plans`
4. Substitua os IDs do ambiente pelos IDs reais retornados nas respostas.

## Regenerar

Quando novos endpoints entrarem:

```bash
node scripts/generate-insomnia-collection.mjs
```

