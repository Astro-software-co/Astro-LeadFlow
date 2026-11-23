# chatbot-qualification Specification

## Purpose
Bot que conversa com o lead no WhatsApp, qualifica e entrega ao vendedor. Estado: funcional em modo mock; motor Lex real pendente (change `lex-real-engine`).

## Requirements

### Requirement: Contrato canonico de slots
Os motores (mock e Lex) SHALL emitir os slots `vehicle`, `paymentType` (`CASH_PURCHASE`, `FINANCING`, `TRADE_IN`), `timeline` (`IMEDIATO`, `30_DIAS`, `90_DIAS_OU_MAIS`) e `budget`.

#### Scenario: Mensagem de financiamento
- **WHEN** o cliente diz que quer financiar
- **THEN** o motor emite `paymentType=FINANCING`

### Requirement: Qualificacao grava no Lead
Os slots SHALL ser gravados em `vehicleInterest`, `interestType`, `purchaseForecast`/`urgency` e `budget`. O sistema SHALL NOT sobrescrever dado ja preenchido; interesse divergente SHALL ir para `notes` com prefixo `[Bot]`, sem repetir a mesma linha.

#### Scenario: Lead de marketplace com veiculo diferente do dito no chat
- **WHEN** o lead tem `vehicleInterest="Corolla"` e o cliente diz "Civic"
- **THEN** `vehicleInterest` continua "Corolla" e `notes` recebe "[Bot] Interesse informado no chat: Civic"

### Requirement: Handoff
O lead SHALL ir de `TALKING_TO_BOT` para `WAITING_ATTENDANCE` quando estiver qualificado (veiculo, pagamento e prazo preenchidos), pedir um humano, indicar compra ou acumular 3 fallbacks consecutivos. O handoff SHALL registrar `LeadStageHistory` com `movedBy` nulo e SHALL NOT mover lead que ja esta com humano.

#### Scenario: Qualificado
- **WHEN** os tres slots obrigatorios estao preenchidos
- **THEN** o handoff ocorre com motivo `QUALIFIED` e o cliente recebe a mensagem de resumo

### Requirement: Atendimento 24 horas
O bot SHALL continuar qualificando fora do horario comercial; o horario nao SHALL disparar handoff por si so.

#### Scenario: Mensagem a noite
- **WHEN** o cliente escreve fora do horario comercial
- **THEN** o bot continua qualificando e nao ha handoff so por causa do horario

### Requirement: Falha do motor nao derruba o webhook
Se o motor de IA lancar excecao, o roteador SHALL responder com o motor de template e SHALL NOT propagar a excecao.

#### Scenario: Excecao no motor
- **WHEN** o motor de IA lanca excecao
- **THEN** o template responde e o webhook retorna 200

### Requirement: Selecao de motor por plano
O motor SHALL seguir o plano: Basico usa template, Profissional usa Lex, Enterprise usa Lex com fallback para Bedrock, limitado a 500 mil tokens por mes.

#### Scenario: Plano Profissional
- **WHEN** a organizacao esta no plano Profissional
- **THEN** o motor usado e o Lex, sem fallback para Bedrock
