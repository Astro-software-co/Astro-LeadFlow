## ADDED Requirements

### Requirement: Processamento sem fila
Quando a fila estiver desligada, o sistema SHALL processar o evento de marketplace no proprio request, sem perder o lead.

#### Scenario: AWS desligada
- **WHEN** chega um webhook valido e `app.aws.enabled=false`
- **THEN** o lead e criado e o evento e marcado como processado

### Requirement: Webhook de marketplace autenticado
Em producao, o sistema SHALL rejeitar com 401 qualquer webhook de marketplace sem autenticacao valida, e SHALL NOT aceitar requests quando o token estiver vazio.

#### Scenario: Request sem autenticacao
- **WHEN** chega um webhook sem credencial valida em producao
- **THEN** a resposta e 401 e nenhum lead e criado

### Requirement: Refresh serializado de token do Mercado Livre
O sistema SHALL garantir que apenas um refresh por credencial rode por vez, porque o refresh token e rotativo e de uso unico.

#### Scenario: Refresh concorrente
- **WHEN** o consumer e o job de reconciliacao precisam renovar a mesma credencial
- **THEN** apenas um refresh e executado e o outro reutiliza o token novo

### Requirement: Lead sem telefone
Um lead sem telefone SHALL NOT ser descartado; SHALL ser criado como pendente para tratamento (regra a definir).

#### Scenario: Contato sem telefone
- **WHEN** chega um lead do marketplace sem telefone
- **THEN** o lead e criado como pendente e nao vai para a DLQ

### Requirement: Dados do veiculo do Mercado Livre
O lead do Mercado Livre SHALL ter marca, modelo e ano preenchidos a partir do anuncio.

#### Scenario: Lead com item_id
- **WHEN** chega um lead do Mercado Livre com `item_id`
- **THEN** marca, modelo e ano sao preenchidos a partir do anuncio
