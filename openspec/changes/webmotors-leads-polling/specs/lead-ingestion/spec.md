## MODIFIED Requirements

### Requirement: Fontes de lead do MVP
O sistema SHALL receber leads do Mercado Livre por webhook e de scrapers externos em `POST /api/v1/webhooks/scraper` autenticado por API Key. Leads da Webmotors SHALL entrar somente por polling da API de Consulta de Leads (capability `webmotors-integration`); o sistema SHALL NOT expor endpoint de webhook para a Webmotors. OLX SHALL ficar fora do MVP.

#### Scenario: Lead de scraper
- **WHEN** um scraper envia `POST /api/v1/webhooks/scraper` com API Key valida
- **THEN** o lead e aceito com 202 e vinculado a organizacao da chave

#### Scenario: Webhook Webmotors inexistente
- **WHEN** alguem envia `POST /api/v1/webhooks/webmotors`
- **THEN** nenhum lead e criado e o endpoint nao existe

#### Scenario: Lead Webmotors por polling
- **WHEN** a sincronizacao da conta Webmotors de uma organizacao encontra uma proposta nova
- **THEN** a proposta entra na caixa de entrada da organizacao e so vira lead no CRM apos o aceite
