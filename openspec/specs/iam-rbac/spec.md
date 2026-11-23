# iam-rbac Specification

## Purpose
Autenticacao, papeis e isolamento entre organizacoes. Estado: implementado (Fase 14).

## Requirements

### Requirement: Papeis primarios
O sistema SHALL aceitar apenas os papeis `ASTRO_ADMIN`, `MANAGER` e `SELLER`. Papeis legados (`ADMIN`, `SUPER_ADMIN`, `VENDEDOR`) SHALL ser migrados e rejeitados por constraint no banco.

#### Scenario: Migracao de papeis legados
- **WHEN** a migration V10 roda sobre usuarios com papel `ADMIN` ou `SUPER_ADMIN`
- **THEN** eles viram `MANAGER` e `ASTRO_ADMIN`, e o `ASTRO_ADMIN` perde o vinculo com a organizacao

### Requirement: Gestao de equipe pelo MANAGER
O `MANAGER` SHALL convidar, editar, suspender, reativar e remover vendedores apenas da propria organizacao. O `SELLER` SHALL NOT acessar a gestao de equipe.

#### Scenario: Vendedor tenta listar a equipe
- **WHEN** um `SELLER` chama `GET /api/v1/users`
- **THEN** a resposta e 403

### Requirement: Configuracoes por papel
O `SELLER` SHALL ver somente as abas Geral e Seguranca em Configuracoes. Cobranca, planos, pagamentos e indicacoes SHALL ser exclusivos do `MANAGER`.

#### Scenario: Vendedor abre Configuracoes
- **WHEN** um SELLER abre Configuracoes
- **THEN** somente as abas Geral e Seguranca aparecem

### Requirement: Indicacao restrita ao MANAGER
Os endpoints `/api/referral/**` e `/api/v1/referral/**` SHALL exigir o papel `MANAGER`. O codigo de indicacao pertence ao usuario, e unico e tem exatamente 6 digitos gerados pelo backend.

#### Scenario: Vendedor chama a API de indicacao
- **WHEN** um `SELLER` chama `GET /api/v1/referral/my-code`
- **THEN** a resposta e 403

### Requirement: Sessao
O access token SHALL ficar apenas em memoria; a sessao SHALL ser restaurada apos refresh do navegador pelo cookie httpOnly. O logout SHALL revogar o refresh token no backend e limpar o cache do React Query.

#### Scenario: Refresh do navegador
- **WHEN** o usuario atualiza a pagina com sessao ativa
- **THEN** a sessao e restaurada pelo cookie httpOnly sem token em localStorage
