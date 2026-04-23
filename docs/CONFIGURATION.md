# Configuration

## Frontend

Arquivo principal de desenvolvimento:

- `frontend/.env.development`

Valores atuais:

```env
VITE_API_URL=/api/v1
VITE_APP_NAME=Astro LeadFlow
VITE_USE_MOCKS=false
```

Observacoes:

- `VITE_API_URL=/api/v1` preserva same-origin via proxy do Vite
- para E2E, `frontend/.env.e2e` tambem usa integracao real

## Backend

Arquivos principais:

- `backend/src/main/resources/application.yaml`
- `backend/src/main/resources/application-dev.yml`

## Banco e servicos locais

Profile `dev`:

- Postgres: `jdbc:postgresql://localhost:5432/leadflow`
- usuario: `leadflow`
- senha: `leadflow123`
- Redis: `localhost:6379`
- senha Redis: `redis123`
- SMTP: `localhost:1025`
- LocalStack: `http://localhost:4566`

## Variaveis relevantes de auth

Cookies:

- `AUTH_REFRESH_COOKIE_NAME`
- `AUTH_REFRESH_COOKIE_PATH`
- `AUTH_REFRESH_COOKIE_SAME_SITE`
- `AUTH_REFRESH_COOKIE_SECURE`

Sessao:

- `AUTH_SESSION_IDLE_TIMEOUT_MS`

JWT:

- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `JWT_REFRESH_MS`

Defaults atuais:

- `JWT_EXPIRATION_MS=300000`
- `JWT_REFRESH_MS=900000`
- `AUTH_SESSION_IDLE_TIMEOUT_MS=900000`

## Bootstrap dev

Configuracao do usuario demo:

- `app.dev.bootstrap-admin.enabled`
- `app.dev.bootstrap-admin.organization-name`
- `app.dev.bootstrap-admin.organization-slug`
- `app.dev.bootstrap-admin.admin-name`
- `app.dev.bootstrap-admin.email`
- `app.dev.bootstrap-admin.password`

Defaults atuais:

- organizacao: `Astro Demo`
- slug: `astro-demo`
- email: `admin@astro.dev`
- senha: `senha123`

## CORS

Origins permitidas no `dev`:

- `http://localhost:*`
- `http://127.0.0.1:*`

## Recomendacoes para ambientes reais

- usar HTTPS
- manter `AUTH_REFRESH_COOKIE_SECURE=true`
- revisar `allowed-origins` e `allowed-origin-patterns`
- definir `JWT_SECRET` forte e exclusivo
- manter Redis externo e persistente
- nao reutilizar credenciais de `dev`
