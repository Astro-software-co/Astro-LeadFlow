# whatsapp-communication Specification

## Purpose
Mensageria via WhatsApp Cloud API (Meta). Estado: mock por padrao; nunca validada contra a Meta real (change `meta-whatsapp-real`).

## Requirements

### Requirement: Comunicacao somente leitura
A tela de Comunicacao SHALL exibir o historico e o botao "Abrir no WhatsApp". O sistema SHALL NOT oferecer chat in-app para o vendedor.

#### Scenario: Vendedor abre uma conversa
- **WHEN** o vendedor abre o historico de um lead
- **THEN** ve as mensagens e o botao 'Abrir no WhatsApp', sem campo de envio

### Requirement: Mensagem inbound
Uma mensagem recebida SHALL ser associada ao lead mais antigo da organizacao com aquele telefone; se nao existir, SHALL criar o lead com origem `WHATSAPP_INBOUND` em `TALKING_TO_BOT`.

#### Scenario: Telefone com leads duplicados
- **WHEN** existem duas linhas de lead com o mesmo telefone
- **THEN** a mensagem e associada a mais antiga e o webhook nao falha

### Requirement: Webhook assinado
O webhook da Meta SHALL validar `X-Hub-Signature-256` quando o secret estiver configurado; em producao o startup SHALL falhar sem `app.meta.webhook-secret`.

#### Scenario: Assinatura da Meta invalida
- **WHEN** chega um POST sem `X-Hub-Signature-256` valido
- **THEN** a resposta e 403 e a mensagem nao e processada

### Requirement: Modo do cliente Meta
`meta.api.mode=mock` SHALL ser o padrao; `real` SHALL ativar o `RealMetaApiClient`.

#### Scenario: Modo padrao
- **WHEN** `meta.api.mode` nao esta definido
- **THEN** o `MockMetaApiClient` e usado
