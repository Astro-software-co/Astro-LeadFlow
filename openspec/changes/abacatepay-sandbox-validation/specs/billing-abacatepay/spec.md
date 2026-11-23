## ADDED Requirements

### Requirement: Estado de inadimplencia
Uma assinatura em `PAST_DUE` SHALL ter regra de acesso definida (a decidir: manter, limitar ou bloquear), exibida ao `MANAGER` em Configuracoes.

#### Scenario: Assinatura vencida
- **WHEN** a assinatura esta em `PAST_DUE`
- **THEN** o MANAGER ve o aviso e a regra de acesso definida em Configuracoes

### Requirement: Validacao em sandbox
O fluxo de checkout e webhook SHALL ser validado ponta a ponta no sandbox do Abacate Pay antes de ser usado em producao.

#### Scenario: Checkout de teste
- **WHEN** um checkout sandbox e pago
- **THEN** o webhook aplica o plano e grava o historico de cobranca
