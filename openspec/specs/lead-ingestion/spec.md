# lead-ingestion Specification

## Purpose
Entrada de leads vindos de marketplaces e scrapers. Estado: implementado com limitacoes (ver changes `ingestion-hardening`).

## Requirements

### Requirement: Fontes de lead do MVP
O sistema SHALL receber leads da Webmotors e do Mercado Livre por webhook, e de scrapers externos em `POST /api/v1/webhooks/scraper` autenticado por API Key. OLX SHALL ficar fora do MVP.

#### Scenario: Lead de scraper
- **WHEN** um scraper envia `POST /api/v1/webhooks/scraper` com API Key valida
- **THEN** o lead e aceito com 202 e vinculado a organizacao da chave

### Requirement: Normalizacao e criacao do lead
Todo lead ingerido SHALL ser normalizado, vinculado a organizacao correta e criado no stage `TALKING_TO_BOT`, disparando o template `welcome_lead` pelo WhatsApp.

#### Scenario: Lead novo com telefone
- **WHEN** um lead chega com telefone valido
- **THEN** ele e criado em `TALKING_TO_BOT` e o template de boas-vindas e enviado

### Requirement: Duplicados
Um lead com telefone ja existente na organizacao SHALL ser criado com `isDuplicate=true` e SHALL continuar disparando a mensagem.

#### Scenario: Telefone repetido
- **WHEN** chega um lead com telefone ja existente na organizacao
- **THEN** o lead e criado com `isDuplicate=true` e a mensagem ainda e enviada

### Requirement: Reconciliacao do Mercado Livre
Quando `marketplace.reconciliation.enabled=true`, um job diario as 3h SHALL buscar os leads das ultimas 25 horas de cada credencial ativa do Mercado Livre.

#### Scenario: Job das 3h
- **WHEN** a reconciliacao esta habilitada
- **THEN** os leads das ultimas 25 horas de cada credencial ativa sao buscados e ingeridos
