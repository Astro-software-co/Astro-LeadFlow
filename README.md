# Astro LeadFlow

Astro LeadFlow e um CRM SaaS multi-tenant para concessionarias. O produto centraliza leads de marketplaces e WhatsApp, organiza o funil comercial e da visibilidade operacional para atendimento, negociacao, estoque e assinatura.

O repositorio contem:
- landing page publica
- aplicacao autenticada em React
- API Kotlin/Spring Boot
- ativos de infraestrutura e observabilidade
- documentacao canonica em `docs/`

## Visao do produto

Problemas que o projeto resolve:
- leads fragmentados entre Webmotors, Mercado Livre e WhatsApp
- falta de visibilidade do pipeline comercial
- demora de resposta que reduz conversao

Objetivo central:
- todo lead que entra no sistema deve ficar visivel, rastreavel e acionavel em segundos

Fluxo principal do lead:
1. lead entra por webhook de marketplace
2. sistema cria o registro em `TALKING_TO_BOT`
3. bot avanca para `WAITING_ATTENDANCE`
4. vendedor assume o lead e segue para `IN_ATTENDANCE` e `IN_NEGOTIATION`
5. lead termina em `COMPLETED` ou `ARCHIVED`

## O que existe hoje

Frontend:
- landing page publica em `/`
- area autenticada em `/app/*`
- dashboard, leads, kanban, estoque, comunicacao e configuracoes
- React Query com cache isolado por `organizationId`
- Vitest + Playwright + axe-core

Backend:
- Spring Boot 3.2.5 com Kotlin e Java 21
- JWT, RBAC, rate limiting e OpenAPI
- PostgreSQL + Flyway
- Redis para rate limit, anti-replay, blocklist de refresh token e sessao autenticada
- integracoes preparadas para Meta, Webmotors e Mercado Livre
- modulo `chatbot` para atendimento automatizado por plano, com mock deterministico em dev, Lex/Bedrock pendentes por flag, sessao canonica em Postgres e contexto quente em Redis
- modulo `monitoring` para health operacional, erros de aplicacao, eventos de webhook e endpoints de observability do Astro admin

SaaS:
- planos
- uso por organizacao
- historico de cobranca
- configuracoes por marketplace

## Stack

- Frontend: React 18, Vite 5, TypeScript, Tailwind 3, shadcn/ui, TanStack Query
- Backend: Kotlin 1.9, Spring Boot 3.2, Spring Security, Spring Data JPA, Redis
- Banco: PostgreSQL
- Cache e distribuicao: Redis
- Testes: Vitest, Testing Library, Playwright, JUnit 5, MockK, Testcontainers
- Observabilidade: Spring Boot Actuator, Prometheus alerts em `infra/monitoring/prometheus/leadflow-alerts.yml`

## Estrutura do repositorio

```text
backend/   API, dominio, seguranca, ingestao e servicos
frontend/  landing, CRM autenticado, hooks, services e testes
docs/      relatorios operacionais e pacote markdown para Notion
infra/     ativos de monitoramento
```

## Rotas principais

Publicas:
- `/`
- `/app/login`

Autenticadas:
- `/app/dashboard`
- `/app/leads`
- `/app/kanban`
- `/app/estoque`
- `/app/estoque/:id`
- `/app/comunicacao`
- `/app/chatbot`
- `/app/configuracoes`
- `/admin/system`

Observacao:
- `/app/configuracoes` e protegida por papel `ADMIN` ou `MANAGER`
- `/app/chatbot` e a tela manager de sessoes, metricas e configuracao somente leitura do bot
- `/admin/system` e a tela Astro admin de visao geral, observability, webhooks, chatbot insights e infraestrutura
- Comunicacao exibe mensagens de bot em modo somente leitura e bloqueia o input manual enquanto a sessao de bot esta `ACTIVE`

## API principal

Base local:
- `http://localhost:8080/api/v1`

Swagger:
- `http://localhost:8080/swagger-ui.html`

Alguns endpoints relevantes:
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /users/me`
- `PATCH /users/me/password`
- `PATCH /users/me/notifications`
- `PATCH /users/me/two-factor`
- `GET /leads`
- `PATCH /leads/{id}/move`
- `PATCH /leads/{id}/archive`
- `GET /kanban/board`
- `GET /vehicles`
- `GET /analytics/kpis`
- `GET /saas/subscription`
- `GET /saas/billing-history`
- `GET /chat/lead/{leadId}/bot-session`
- `GET /chatbot/sessions`
- `GET /chatbot/metrics`
- `POST /chatbot/sessions/{id}/handoff`
- `GET /admin/system/health`
- `GET /admin/observability/overview`
- `GET /admin/observability/errors`
- `GET /admin/observability/webhooks`
- `GET /admin/observability/chatbot-insights`
- `GET /admin/observability/infrastructure`
- `POST /webhooks/meta`
- `POST /webhooks/mercadolivre`

## Autenticacao e sessao

O projeto usa JWT com dois niveis:
- `accessToken` fica apenas em memoria no frontend
- `refreshToken` fica em cookie `httpOnly`

Comportamento atual:
- a sessao sobrevive a reload da pagina via `POST /auth/refresh`
- a sessao expira apos 15 minutos de inatividade e e renovada enquanto o usuario continua ativo
- o backend valida a sessao no Redis mesmo para chamadas autenticadas com Bearer token
- o frontend envia `withCredentials` nas chamadas de autenticacao
- `logout` revoga o refresh token no backend
- tokens nao sao persistidos em `localStorage`

Isso melhora a experiencia do usuario sem abrir mao da restricao de nao expor refresh token ao JavaScript da aplicacao.

## Requisitos locais

Minimos para desenvolvimento:
- Java 21
- Node.js 20+
- npm
- PostgreSQL
- Redis

Servicos auxiliares usados no profile `dev`:
- SMTP local em `localhost:1025`
- LocalStack em `localhost:4566` para recursos AWS simulados

Configuracao default de desenvolvimento no backend:
- Postgres: `jdbc:postgresql://localhost:5432/leadflow`
- usuario: `leadflow`
- senha: `leadflow123`
- Redis: `localhost:6379`

## Executando localmente

### 1. Backend

```bash
cd backend
./gradlew bootRun
```

API local:
- `http://localhost:8080`

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

App local:
- `http://localhost:3000`

Variaveis ja presentes no repositorio:
- `frontend/.env.development`
- `frontend/.env.e2e`

Defaults atuais do frontend:
- `VITE_API_URL=/api/v1`
- `VITE_USE_MOCKS=false` em desenvolvimento
- `VITE_USE_MOCKS=false` em E2E

URLs publicas em desenvolvimento:
- `APP_BASE_URL` controla callbacks de frontend
- `PUBLIC_API_URL` controla webhooks externos quando o backend esta atras de `cloudflared`
- `scripts/update-public-url.sh` atualiza `PUBLIC_API_URL` em `.env.local` a partir do log do tunnel
- `scripts/validate-meta-integration.sh` valida o ciclo Meta local em modo mock

Bootstrap dev do backend:
- organizacao demo criada automaticamente quando ausente
- usuario admin garantido: `admin@astro.dev`
- senha: `senha123`

## Build

Frontend:

```bash
cd frontend
npm run build
```

Backend:

```bash
cd backend
./gradlew bootJar
```

Docker do backend:

```bash
cd backend
docker build -t astro-leadflow-backend .
```

## Testes

Frontend:

```bash
cd frontend
npm run lint
npm run test:coverage
npm run test:e2e
```

Backend:

```bash
cd backend
./gradlew test
```

Observacoes:
- o Playwright espera o frontend em `127.0.0.1:3000`
- os cenarios E2E autenticados dependem de backend acessivel durante a execucao

## Regras e restricoes importantes

- multi-tenancy vem do `organizationId` do JWT
- query keys do React Query devem sempre ser org-scoped
- `logout` limpa o cache do cliente para evitar vazamento entre organizacoes
- registro publico foi removido; onboarding real acontece por convite administrativo
- duplicata continua marcada com `isDuplicate: true`, mas ainda pode disparar mensagem
- cota de uso e soberana no nivel global da assinatura
- reset de uso segue `renewsAt`, nao o mes calendario
- sem dark mode por default
- sem persistir token em `localStorage`

## Documentacao adicional

- [`docs/HEALTH_REPORT.md`](docs/HEALTH_REPORT.md)
- [`docs/RUNBOOK-CHATBOT.md`](docs/RUNBOOK-CHATBOT.md)
- [`docs/RUNBOOK-CLOUDFLARED.md`](docs/RUNBOOK-CLOUDFLARED.md)
- [`docs/RUNBOOK-META.md`](docs/RUNBOOK-META.md)
- [`docs/varredura-geral-2026-04-17.md`](docs/varredura-geral-2026-04-17.md)
- [`docs/README.md`](docs/README.md)
- [`docs/notion/INDEX.md`](docs/notion/INDEX.md)

## Branching

- branch de trabalho atual: `develop`
- fluxo esperado: trabalho em `develop`, validacao, PR para `main`

## Status de publicacao

O branch `develop` contem a estrategia atual de sessao baseada em:
- access token curto em memoria
- refresh token em cookie `httpOnly`
- sessao deslizante de 15 minutos no Redis

Essa combinacao e o comportamento oficial esperado para o produto no fluxo web autenticado.
