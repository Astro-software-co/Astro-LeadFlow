## Purpose
Garantir que logs da aplicacao, inclusive os erros persistidos em banco, nao carreguem dados pessoais nem segredos.

## ADDED Requirements

### Requirement: Mascara de PII e segredos em log
Toda linha de log escrita no console ou persistida em `application_error` SHALL ter telefones, e-mails e valores de `access_token`, `refresh_token`, `client_secret`, `authorization` e `api-key` mascarados, inclusive em mensagens e stack traces de excecao.

#### Scenario: Telefone em log de erro
- **WHEN** um log de ERROR contem `+5511987654321`
- **THEN** o texto gravado contem `+55******4321` e nao contem o numero completo

#### Scenario: Token em log
- **WHEN** um log contem `access_token=abc123secret`
- **THEN** o valor do token nao aparece no texto gravado

### Requirement: Sem corpo de terceiros em log
O sistema SHALL NOT registrar em log o corpo de requisicoes ou respostas de terceiros (Meta, marketplaces, gateway de pagamento); SHALL registrar apenas identificadores e codigos HTTP.

#### Scenario: Erro HTTP da Meta
- **WHEN** a Meta responde com erro
- **THEN** o log traz o codigo HTTP e identificadores, sem o corpo da resposta nem o telefone completo

### Requirement: Excecoes sem valor cru
Mensagens de excecao de validacao de telefone ou de lead SHALL NOT conter o valor recebido.

#### Scenario: Telefone invalido
- **WHEN** um telefone invalido e recebido
- **THEN** a mensagem de erro informa o motivo sem repetir o numero
