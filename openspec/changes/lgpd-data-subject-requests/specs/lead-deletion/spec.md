## Purpose
Permitir que o dono da concessionária exclua um lead e tudo que depende dele, de forma irreversível e auditada sem guardar dados pessoais.

## ADDED Requirements

### Requirement: Exclusão manual de lead
`DELETE /api/v1/leads/{id}` SHALL ser restrito a `MANAGER`, SHALL remover o lead e seus dados dependentes (histórico de estágio, mensagens, sessões do bot, propostas Webmotors vinculadas) e SHALL desvincular e limpar os eventos de marketplace do lead. Com `includeSamePhone=true`, SHALL excluir também os demais leads da organização com o mesmo telefone. A resposta SHALL ser 204.

#### Scenario: Lead de marketplace com evento
- **WHEN** o `MANAGER` exclui um lead que tem evento de marketplace e sessão de bot
- **THEN** a resposta é 204 e o evento fica sem vínculo e sem payload

#### Scenario: Mesmo telefone
- **WHEN** a exclusão pede `includeSamePhone=true`
- **THEN** todos os leads da organização com aquele telefone são excluídos

#### Scenario: Outra organização
- **WHEN** o lead pertence a outra organização
- **THEN** a resposta é 404

#### Scenario: Vendedor
- **WHEN** um `SELLER` tenta excluir
- **THEN** a resposta é 403

### Requirement: Auditoria sem PII
Cada exclusão SHALL gravar um registro com organização, autor, data, hash do telefone, quantidade de cards e de mensagens excluídos e motivo, sem nome, telefone ou e-mail.

#### Scenario: Registro de auditoria
- **WHEN** uma exclusão é concluída
- **THEN** existe um registro de auditoria sem o telefone em claro
