## ADDED Requirements

### Requirement: Motor Lex real
Com `app.chatbot.engine.mode=aws`, o motor Lex SHALL chamar o Amazon Lex V2 (pt_BR) e SHALL devolver os slots no contrato canonico. Qualquer falha SHALL resultar em resposta do motor de template, sem excecao.

#### Scenario: Lex indisponivel
- **WHEN** a chamada ao Lex expira ou falha
- **THEN** o lead recebe a resposta do template e o webhook responde 200

### Requirement: Uso do Lex medido
O sistema SHALL registrar cada requisicao ao Lex para acompanhamento de custo e de cota.

#### Scenario: Chamada ao Lex
- **WHEN** o motor Lex responde uma mensagem
- **THEN** a requisicao e contabilizada nas metricas de uso

### Requirement: Horario comercial por organizacao
O horario de atendimento SHALL ser configuravel por organizacao, e o handoff fora do horario SHALL informar quando o cliente sera atendido.

#### Scenario: Handoff fora do horario
- **WHEN** um lead e qualificado fora do horario da organizacao
- **THEN** o cliente recebe a informacao de quando sera atendido
