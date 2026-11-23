## Purpose
Integracao com a API de Consulta de Leads da Webmotors por polling: conta por organizacao, caixa de entrada de propostas, aceite/recusa e entrada do lead aceito no CRM.

## ADDED Requirements

### Requirement: Acesso restrito ao MANAGER
Os endpoints `/api/v1/integrations/webmotors/**` SHALL exigir o papel `MANAGER` e SHALL operar somente sobre a organizacao do JWT.

#### Scenario: Vendedor
- **WHEN** um `SELLER` chama qualquer endpoint de integracao
- **THEN** a resposta e 403

#### Scenario: Sem token
- **WHEN** a chamada nao tem token
- **THEN** a resposta e 401

#### Scenario: Proposta de outra organizacao
- **WHEN** um `MANAGER` tenta aceitar uma proposta de outra organizacao
- **THEN** a resposta e 404

#### Scenario: Usuario interno sem organizacao
- **WHEN** um `ASTRO_ADMIN` chama um endpoint de integracao
- **THEN** a resposta e 403, nunca 500

### Requirement: Conta e segredos
Cada organizacao SHALL ter no maximo uma conta Webmotors; credenciais (client id/secret, usuario/senha do integrador) SHALL ser guardadas cifradas e nunca devolvidas pela API.

#### Scenario: Consulta da conta
- **WHEN** o `MANAGER` consulta a conta
- **THEN** a resposta mostra apenas dicas (ex.: final do client id) e preferencias, sem segredos

### Requirement: Padroes da conta
Uma conta nova SHALL ter aceite `AUTO` e a mensagem de abordagem ligada; ambos SHALL ser configuraveis por loja.

#### Scenario: Conta criada sem preferencias
- **WHEN** o `MANAGER` cria a conta informando so as credenciais
- **THEN** o modo de aceite e `AUTO` e a mensagem de abordagem esta ligada

### Requirement: Entrada do lead aceito no CRM
Um lead aceito com telefone valido SHALL entrar no CRM pela mesma regra da ingestao oficial: normalizacao do telefone, controle de cota, e telefone ja existente na organizacao criando lead com `isDuplicate=true` que ainda recebe a mensagem. Falha de cota ou da Meta SHALL NOT desfazer o lead.

#### Scenario: Lead novo
- **WHEN** uma proposta aceita traz um telefone sem lead na organizacao
- **THEN** um lead e criado e a mensagem de abordagem e enviada se a conta a tiver ligada

#### Scenario: Telefone repetido
- **WHEN** uma proposta aceita traz um telefone que ja tem lead na organizacao
- **THEN** um novo lead e criado com `isDuplicate=true` e a mensagem e enviada

#### Scenario: Cota excedida
- **WHEN** a organizacao esgotou a cota de mensagens
- **THEN** o lead e criado sem mensagem
