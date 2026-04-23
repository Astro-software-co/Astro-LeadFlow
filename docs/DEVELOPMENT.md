# Development

## Fluxo recomendado

1. subir infraestrutura local
2. iniciar backend
3. iniciar frontend
4. validar login real com `admin@astro.dev`
5. desenvolver com testes focados por modulo

## Convencoes importantes

- manter `accessToken` apenas em memoria
- nunca usar `localStorage` para tokens
- escopo de cache React Query deve incluir `organizationId`
- endpoints autenticados devem continuar seguros sem depender do frontend
- filtros de UI nao devem depender de recortes incompletos do dataset quando existir diretorio dedicado

## Frontend

Padroes usados:

- `services` para HTTP
- `hooks/api` para React Query
- `pages` para orquestracao
- `features` para componentes reutilizaveis por fluxo
- `modules` para reexport de dominio

Regras praticas:

- preferir `VITE_API_URL=/api/v1`
- usar Sonner como sistema de toast
- evitar criar novo estado global quando um hook + React Query resolve
- ao lidar com `401`, limpar sessao via evento e deixar o router resolver a navegacao

## Backend

Padroes usados:

- controller fino
- regra de negocio no service
- repository sem logica de apresentacao
- mapper para DTOs

Regras praticas:

- multi-tenancy sempre por organizacao autenticada
- nao expor mais dados do que o necessario nos DTOs
- para sessao/auth, considerar Redis como parte do contrato
- tokens precisam carregar `sid`, e refresh tokens tambem `jti`

## Auth em desenvolvimento

Arquivos mais importantes:

- `frontend/src/contexts/AuthContext.tsx`
- `frontend/src/lib/api.ts`
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/service/AuthService.kt`
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/service/AuthSessionService.kt`
- `backend/src/main/kotlin/com/astro/leadflow/shared/config/JwtAuthenticationFilter.kt`

## Areas de atencao

- `docs/notion/` agora e material exportado/historico
- `docs/*.md` na raiz de `docs/` sao a referencia principal
- `backend/src/main/resources/db/migration/v1__init_schema.sql` continua suspeito por naming e deve ser removido ou renomeado num cleanup futuro
- algumas features SaaS ainda assumem assinatura existente; o bootstrap dev cobre esse caso para a org demo
