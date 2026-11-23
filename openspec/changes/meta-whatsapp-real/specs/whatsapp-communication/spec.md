## ADDED Requirements

### Requirement: Template com variaveis
O envio de template SHALL aceitar `components` para templates com variaveis (nome do cliente, veiculo).

#### Scenario: Template com nome do cliente
- **WHEN** o template aprovado tem variaveis
- **THEN** o envio inclui `components` com os valores

### Requirement: Versao da Graph API configuravel
A versao da Graph API SHALL ser configuravel em um unico lugar.

#### Scenario: Troca de versao
- **WHEN** a versao e alterada na configuracao
- **THEN** todas as chamadas passam a usar a nova versao sem mudar codigo

### Requirement: Validacao em modo real
O modo `real` SHALL ser validado com um POST assinado e uma mensagem de template entregue antes de ser usado em producao.

#### Scenario: Mensagem de teste
- **WHEN** o modo `real` e ativado com um numero de teste
- **THEN** um template e entregue e o POST assinado do webhook e aceito
