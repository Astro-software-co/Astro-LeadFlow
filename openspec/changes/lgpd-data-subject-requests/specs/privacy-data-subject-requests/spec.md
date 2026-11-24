## Purpose
Permitir que a concessionária registre e atenda solicitações de titulares (LGPD art. 18) com prazo, anonimize dados pessoais sem perder métricas e limite a retenção de dados brutos.

## ADDED Requirements

### Requirement: Registro de solicitação sem identificador em claro
O sistema SHALL registrar solicitações (`ANONYMIZATION`, `ACCESS`, `CORRECTION`, `DELETION`) com prazo `due_at = received_at + privacy.sla-days` (padrão 15) e SHALL guardar apenas o HMAC-SHA256 do identificador normalizado e uma dica mascarada, nunca o telefone ou e-mail em claro.

#### Scenario: Nova solicitação por telefone
- **WHEN** o `MANAGER` registra uma solicitação para `(11) 98765-4321`
- **THEN** a solicitação fica `RECEIVED` com prazo de 15 dias e a dica `******4321`, e o telefone não é gravado

#### Scenario: Solicitação atrasada
- **WHEN** a lista é consultada e uma solicitação aberta passou do prazo
- **THEN** ela aparece com `overdue=true`

### Requirement: Execução da anonimização
Ao executar uma solicitação de anonimização, o sistema SHALL exigir o identificador de novo (conferido contra o hash) e SHALL anonimizar, na mesma transação, todos os leads da organização com aquele identificador: nome, telefone, e-mail e notas do lead; conteúdo das mensagens; dados coletados pelo bot; payloads brutos de eventos de marketplace e da caixa Webmotors; e ocorrências do identificador em erros persistidos. Estágio, valores e métricas SHALL ser mantidos. A operação SHALL ser idempotente e o relatório SHALL NOT conter PII.

#### Scenario: Vários leads do mesmo telefone
- **WHEN** a organização tem dois leads com o telefone do titular
- **THEN** os dois são anonimizados e o relatório informa as contagens

#### Scenario: Outra organização
- **WHEN** outra organização tem lead com o mesmo telefone
- **THEN** esse lead não é alterado

#### Scenario: Identificador sem dados
- **WHEN** não há lead com o identificador
- **THEN** a solicitação é concluída com "nenhum dado encontrado"

#### Scenario: Identificador divergente
- **WHEN** a execução informa um identificador cujo hash não confere
- **THEN** a resposta é 422 e nada é alterado

### Requirement: Contato bloqueado para anonimizados
O sistema SHALL NOT enviar mensagem (template, texto, bot) a lead anonimizado, e uma mensagem recebida pelo WhatsApp SHALL NOT ser associada a lead anonimizado.

#### Scenario: Envio bloqueado
- **WHEN** algo tenta enviar mensagem a um lead anonimizado
- **THEN** o envio é recusado e nada chega à Meta

### Requirement: Acesso restrito
Os endpoints `/api/v1/privacy/**` SHALL exigir `MANAGER` da organização do JWT.

#### Scenario: Vendedor
- **WHEN** um `SELLER` chama `/api/v1/privacy/requests`
- **THEN** a resposta é 403

### Requirement: Retenção de dados brutos
Quando habilitado, um job diário SHALL limpar o `raw_payload` de eventos de marketplace e da caixa Webmotors com mais de `privacy.retention.webhook-payload-days` (padrão 90) e SHALL apagar erros persistidos com mais de `app.monitoring.error-retention-days` (padrão 90).

#### Scenario: Payload antigo
- **WHEN** o job roda e há evento recebido há 91 dias
- **THEN** o `raw_payload` dele fica vazio e o evento recente não muda
