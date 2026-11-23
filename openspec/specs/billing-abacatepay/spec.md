# billing-abacatepay Specification

## Purpose
Assinatura SaaS cobrada pelo Abacate Pay. Estado: implementado, ainda sem teste ponta a ponta com credenciais reais.

## Requirements

### Requirement: Troca de plano somente apos pagamento confirmado
O plano de uma organizacao SHALL mudar apenas depois de o webhook de pagamento ser processado. `PATCH /saas/subscription/plan/{id}` SHALL ser recusado.

#### Scenario: Troca direta
- **WHEN** o `MANAGER` chama `PATCH /api/v1/saas/subscription/plan/{id}`
- **THEN** a resposta e um erro de operacao invalida e o plano nao muda

### Requirement: Credenciais ausentes falham de forma clara
O checkout SHALL falhar com erro explicito quando `app.abacatepay.api-key` estiver vazia, sem fingir sucesso.

#### Scenario: Sem chave de API
- **WHEN** o `MANAGER` inicia um checkout sem `ABACATEPAY_API_KEY`
- **THEN** a resposta e 502 com a mensagem "API key nao configurada"

### Requirement: Autenticidade do webhook
O webhook SHALL exigir o secret na URL (`webhookSecret`) e a assinatura HMAC-SHA256 em base64 no header `X-Webhook-Signature`, calculada sobre o corpo bruto com a chave publica do Abacate Pay. Em producao o startup SHALL falhar se secret ou chave estiverem vazios.

#### Scenario: Assinatura invalida
- **WHEN** chega um webhook com secret ou HMAC incorreto
- **THEN** a resposta e 403 e o evento nao e processado

### Requirement: Eventos de assinatura
O sistema SHALL tratar `subscription.completed` e `subscription.renewed` (aplica o plano, marca `ACTIVE`, grava historico e gera comissao de indicacao no primeiro pagamento), `subscription.payment_failed` (marca `PAST_DUE`, sem rebaixar assinatura `CANCELLED`) e `subscription.cancelled` (marca `CANCELLED`). Eventos repetidos SHALL ser idempotentes pelo id do evento.

#### Scenario: Falha de pagamento
- **WHEN** chega `subscription.payment_failed` para uma assinatura `ACTIVE`
- **THEN** a assinatura passa a `PAST_DUE`

### Requirement: Produto por plano
Cada plano vendavel (preco maior que zero) SHALL ter um produto no Abacate Pay, criado no primeiro checkout e com id gravado em `plans.abacate_product_id`.

#### Scenario: Primeiro checkout do plano
- **WHEN** um plano sem `abacate_product_id` entra em checkout
- **THEN** o produto e criado no Abacate Pay e o id e gravado no plano
