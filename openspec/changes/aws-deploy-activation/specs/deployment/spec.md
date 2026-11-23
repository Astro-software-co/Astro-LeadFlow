## ADDED Requirements

### Requirement: Perfil de producao fail-fast
No perfil `prod`, o backend SHALL falhar no startup quando banco, Redis, `JWT_SECRET`, URLs ou CORS nao estiverem configurados, e SHALL NOT usar valores de desenvolvimento.

#### Scenario: Sem JWT_SECRET
- **WHEN** o backend sobe com `SPRING_PROFILES_ACTIVE=prod` sem `JWT_SECRET`
- **THEN** o startup falha com placeholder nao resolvido

### Requirement: Deploy manual e sem chaves estaticas
O deploy SHALL ser disparado manualmente e SHALL autenticar na AWS por OIDC, sem chaves de acesso estaticas.

#### Scenario: Disparo do deploy
- **WHEN** alguem aciona o workflow `deploy.yml`
- **THEN** a AWS e acessada por role OIDC e nenhuma chave estatica e usada

### Requirement: Integracao continua
Todo pull request SHALL executar testes do backend e do frontend, lint, build e varredura de segredos.

#### Scenario: Pull request aberto
- **WHEN** um PR e aberto contra develop ou main
- **THEN** testes, lint, build e varredura de segredos rodam e bloqueiam o merge em caso de falha
