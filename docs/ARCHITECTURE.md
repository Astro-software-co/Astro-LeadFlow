# Architecture

## Visao geral

Astro LeadFlow e um CRM SaaS multi-tenant para operacao comercial automotiva. O repositorio contem:

- `frontend/`: landing publica e SPA autenticada em React
- `backend/`: API Kotlin/Spring Boot com seguranca, dominio e integracoes
- `infra/`: monitoramento e ativos de operacao
- `docs/`: documentacao canonica e historico operacional

## Topologia local

- frontend Vite em `http://localhost:3000`
- backend Spring Boot em `http://localhost:8080`
- PostgreSQL em `localhost:5432`
- Redis em `localhost:6379`
- MailHog em `localhost:1025` e `http://localhost:8025`
- LocalStack em `localhost:4566`

No frontend, `VITE_API_URL=/api/v1` usa o proxy do Vite para manter chamadas same-origin durante desenvolvimento.

## Frontend

Principais blocos:

- `src/App.tsx`: composition root, React Query, contexts e rotas
- `src/contexts/AuthContext.tsx`: bootstrap da sessao, login, logout, keepalive por atividade e limpeza de cache
- `src/lib/api.ts`: cliente Axios com `withCredentials`, header Bearer e refresh centralizado
- `src/services/*`: contratos HTTP da SPA com a API
- `src/hooks/api/*`: adaptacao para React Query
- `src/pages/*`: telas principais
- `src/modules/*` e `src/features/*`: exportacoes e componentes por dominio

Padrao principal:

1. `service` fala com a API
2. `hook` encapsula cache, loading e invalidacao
3. `page` orquestra estado de tela
4. `component` renderiza UI pura

## Backend

Modulos principais em `backend/src/main/kotlin/com/astro/leadflow/shared/modules/`:

- `iam`: autenticacao, usuarios, organizacao, 2FA
- `crm`: leads, pipeline, kanban, historico de movimentacao
- `inventory`: veiculos e fotos
- `analytics`: KPIs e graficos do dashboard
- `communication`: timeline/mensageria e webhook Meta
- `saas`: planos, assinatura, uso e marketplaces
- `ingestion`: webhooks e integracoes com marketplaces
- `chatbot`: engines, roteamento por plano, sessao de bot, handoff e quota de tokens
- `monitoring`: health operacional, erros persistidos, logs de webhook e observability para Astro admin

Camadas recorrentes:

- `api`: controllers, requests e responses
- `service`: regra de negocio
- `domain`: entidades e repositories
- `mapper`: conversao dominio <-> DTO

## Autenticacao e sessao

### Estrategia atual

- `accessToken` JWT fica apenas em memoria no frontend
- `refreshToken` JWT fica em cookie `httpOnly`
- `accessToken` expira em 5 minutos
- `refreshToken` expira em 15 minutos e e rotacionado a cada refresh
- sessao server-side e guardada no Redis com timeout deslizante de 15 minutos
- cada token carrega `sid` de sessao
- refresh token tambem carrega `jti`

### Fluxo

1. `POST /api/v1/auth/login` cria sessao no Redis
2. backend responde com `accessToken` no body e cookie de refresh
3. frontend guarda apenas o access token em memoria
4. em reload, `AuthContext` chama `POST /api/v1/auth/refresh`
5. se o refresh for valido, backend renova access token, gira refresh token e estende a sessao
6. em logout, o refresh token entra em blocklist e a sessao Redis e removida

### Protecao das chamadas autenticadas

- `JwtAuthenticationFilter` valida assinatura, expiração e tipo do token
- o filtro tambem valida se a sessao `sid` ainda existe no Redis
- cada chamada autenticada toca o TTL da sessao
- se a sessao for encerrada, a API volta a responder `401`
- o frontend limpa estado local e deixa o React Router redirecionar para `/app/login`

### Keepalive por atividade

Para preservar UX sem transformar a sessao em algo permanente:

- o frontend observa atividade recente do usuario
- se houver atividade e o ultimo keepalive ja estiver antigo, ele chama `POST /auth/refresh` em background
- sem atividade recente, nao ha keepalive
- resultado: sessao continua viva durante uso real e expira apos inatividade

## Multi-tenancy e RBAC

- `organizationId` e transportado no JWT
- controllers e services usam `SecurityUtils` para obter usuario e organizacao atual
- chaves do React Query sao sempre org-scoped
- `ADMIN`, `MANAGER`, `SELLER` e `SUPER_ADMIN` governam acesso
- endpoints SaaS administrativos usam `@PreAuthorize`
- filtro de responsaveis usa apenas usuarios ativos que podem receber leads

## Fluxo de leads

Pipeline consolidado:

1. `TALKING_TO_BOT`
2. `WAITING_ATTENDANCE`
3. `IN_ATTENDANCE`
4. `IN_NEGOTIATION`
5. `COMPLETED`
6. `ARCHIVED`

Regras relevantes:

- toda mudanca de etapa gera activity
- duplicatas continuam marcadas com `isDuplicate`
- `assume` move do bot para atendimento quando aplicavel
- atribuicao explicita permite `SELLER` e `MANAGER`
- filtro `assignedTo` agora usa diretorio dedicado de assignable users

## Chatbot

O modulo `chatbot` vive em `com.astro.leadflow.shared.modules.chatbot` e segue a divisao:

- `domain`: entidades `BotSession` e `AiTokenUsage`, enums e value objects
- `engine`: contrato `ChatbotEngine`, engines mock/template e stubs AWS marcados como `[AWS-PENDING]`
- `routing`: selecao de engine, avaliacao de handoff e roteamento da conversa
- `session`: persistencia canonica em Postgres e cache Redis com TTL de 30 minutos
- `usage`: guarda de quota, tracker de tokens e alerta por email em 80%
- `controller`: endpoints manager para sessoes, metricas e handoff manual

O tier do plano controla o comportamento:

- Basico: sem bot, resposta por template fixo
- Profissional: Lex puro
- Enterprise: Lex com fallback Bedrock apenas em `FallbackIntent`

Em desenvolvimento, `chatbot.engine.mode=mock` usa keyword matching deterministico em `pt_BR`.

## Comunicação e Meta API

O webhook Meta continua em `communication`, mas o envio para WhatsApp passa por `MetaApiClient`:

- `MockMetaApiClient` e o default local (`META_API_MODE=mock`)
- `RealMetaApiClient` usa a Graph API quando `META_API_MODE=real`
- o `MessageSenderService` mantem o contrato de envio e incrementa `MESSAGE_SENT` apenas em sucesso
- inbound Meta roteia para `ChatbotRouter` quando o lead esta em `TALKING_TO_BOT`

Mensagens geradas pelo bot sao gravadas em `chat_messages` com `sender_type=BOT`, `bot_session_id`, `intent_detected` e `engine_used`. A tela `/app/comunicacao` mostra essas mensagens como somente leitura e desabilita o input enquanto a sessao de bot esta `ACTIVE`.

## Observability

O modulo `monitoring` vive em `com.astro.leadflow.shared.modules.monitoring` e adiciona:

- `GET /api/v1/admin/system/health` com flags booleanas mascaradas para segredos e dependencias
- `application_error` para WARN/ERROR persistidos via appender Logback async
- `webhook_event_log` para source, outcome, status HTTP, duracao e correlation id
- endpoints `/api/v1/admin/observability/*` para overview, performance, erros, webhooks, chatbot insights e infraestrutura

A tela Astro admin `/admin/system` consome esses endpoints em cinco abas: Visao Geral, Observability, Webhooks, Chatbot Insights e Infraestrutura.

## Persistencia

- PostgreSQL guarda entidades de negocio
- Flyway controla schema
- Redis suporta:
  - rate limit
  - anti-replay
  - blocklist de refresh tokens
  - sessoes autenticadas deslizantes
  - contexto quente de sessoes do chatbot

## Integracoes

- Meta/WhatsApp
- Webmotors
- Mercado Livre
- S3/SQS/SNS via LocalStack em desenvolvimento

## Decisoes recentes importantes

- `reload` nao deve mais implicar relogin no fluxo integrado
- sessao oficial passou a ser sliding session de 15 min
- `GET /users/assignable` substitui o filtro derivado do dataset da pagina
- redirect pos-401 nao usa mais `window.location.href`
- wrappers de toast legados foram removidos em favor do Sonner
