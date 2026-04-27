# Estado atual do projeto - Astro LeadFlow

Gerado em: 2026-04-27 16:18:25 -03
Base analisada: workspace local `/home/ruy/projetos-ruy/Astro-LeadFlow`
Branch observada: `develop`

Este documento registra o estado atual do projeto a partir do codigo local, das rotas existentes, das migrations, dos modulos frontend/backend e das validacoes executadas na maquina.

## Resumo executivo

O Astro LeadFlow e um CRM SaaS multi-tenant para concessionarias. O produto cobre captura de leads, funil comercial, estoque, comunicacao, assinatura SaaS, onboarding de novas organizacoes, painel administrativo Astro e integracoes com marketplaces e AbacatePay.

Estado atual observado:

- Backend Spring Boot esta rodando em `localhost:8080` e respondeu `UP` no healthcheck.
- Login `admin@astro.dev` funciona e acessa o painel administrativo Astro.
- Login `manager@astro.dev` funciona e acessa rotas de concessionaria.
- A listagem de equipe do manager nao exibiu `admin@astro.dev`.
- Frontend possui rotas separadas para area de concessionaria (`/app`) e area Astro admin (`/admin`).
- AbacatePay esta implementado no codigo, mas o processo Java atual nao apresentou `ABACATEPAY_API_KEY` nem `ABACATEPAY_WEBHOOK_SECRET` no ambiente no momento da checagem. Antes do checkout real, reiniciar com as variaveis exportadas e preferencialmente sem Gradle Daemon.

## Validacoes executadas

### Runtime local

| Checagem | Resultado |
|---|---|
| `GET http://localhost:8080/actuator/health` | `200`, `status=UP` |
| Backend ouvindo em `8080` | Sim |
| `cloudflared` ativo | Sim, encaminhando para `localhost:8080` |
| `ABACATEPAY_API_KEY` no ambiente do processo Java | Nao detectado |
| `ABACATEPAY_WEBHOOK_SECRET` no ambiente do processo Java | Nao detectado |
| `POST /api/v1/saas/subscription/checkout/{planId}` | `502`, `API key nao configurada` |

Observacao importante: os arquivos de configuracao usam placeholders de ambiente para AbacatePay. Se as variaveis forem definidas no shell mas o Gradle Daemon ja estiver ativo, o processo pode subir sem esses valores. Para validar checkout, use:

```bash
cd backend
./gradlew --stop

export ABACATEPAY_API_KEY='valor-da-sua-chave'
export ABACATEPAY_WEBHOOK_SECRET='valor-do-seu-secret'

./gradlew --no-daemon bootRun
```

### Autenticacao e RBAC

| Fluxo | Resultado |
|---|---|
| `POST /api/v1/auth/login` com `admin@astro.dev` | `200` |
| `GET /api/v1/users/me` como admin | `200` |
| `GET /api/v1/admin/dashboard` como admin | `200` |
| `POST /api/v1/auth/login` com `manager@astro.dev` | `200` |
| `GET /api/v1/users/me` como manager | `200` |
| `GET /api/v1/users` como manager | `200` |
| `admin@astro.dev` aparece na equipe do manager | Nao |

Credenciais de desenvolvimento confirmadas pelo bootstrap:

| Perfil | Email | Senha | Uso esperado |
|---|---|---|---|
| Astro admin | `admin@astro.dev` | `senha123` | Painel Astro em `/admin` |
| Manager demo | `manager@astro.dev` | `senha123` | Organizacao demo em `/app` |
| Seller demo | `seller@astro.dev` | `senha123` | Operacao de vendedor em `/app` |

## Estrutura geral

```text
backend/   API Kotlin/Spring Boot, dominio, seguranca, integracoes e migrations
frontend/  SPA React/Vite, telas, hooks, services, modulos e testes
docs/      documentacao tecnica e operacional
infra/     monitoramento Prometheus
```

Stack principal:

- Backend: Kotlin 1.9, Spring Boot 3.2.5, Spring Security, JPA/Hibernate, Flyway, Redis, WebClient.
- Frontend: React 18, Vite 5, TypeScript, Tailwind, shadcn/Radix, TanStack Query, Axios, Recharts.
- Banco: PostgreSQL com Flyway.
- Cache/estado operacional: Redis para rate limit, sessoes autenticadas, blocklist de refresh token e anti-replay de webhook.
- Testes: JUnit/MockK no backend; Vitest, Testing Library e Playwright no frontend.
- Observabilidade: Actuator e alertas Prometheus em `infra/monitoring/prometheus/leadflow-alerts.yml`.

## Modulo backend: configuracao compartilhada

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/config`

Responsabilidades:

- `SecurityConfig`: regras de autorizacao HTTP, endpoints publicos, admin e concessionaria.
- `JwtAuthenticationFilter`: valida access token, sessao Redis e usuario ativo.
- `ApiKeyAuthFilter`: suporte a API key em fluxos externos.
- `RateLimitFilter`: limite de requisicoes por tipo de rota.
- `MetaWebhookSignatureFilter`: validacao de webhook Meta quando secret configurado.
- `AbacatePayWebhookSignatureFilter`: validacao de webhook AbacatePay por query secret, assinatura HMAC e anti-replay Redis.
- `CorsConfig`: CORS com credenciais.
- `RedisConfig`, `S3Config`, `SqsConfig`: infraestrutura de cache e integracoes AWS/LocalStack.
- `DevBootstrapConfig`: cria organizacao demo, manager, seller e Astro admin no profile `dev`.

Pontos importantes:

- `accessToken` expira em 5 minutos.
- `refreshToken` e sessao Redis expiram em 15 minutos.
- O refresh token fica em cookie `httpOnly`.
- O access token fica em memoria no frontend.
- `MANAGER` e `SELLER` recebem authority `ROLE_DEALERSHIP`.
- `ASTRO_ADMIN` recebe somente `ROLE_ASTRO_ADMIN` e nao tem organizacao vinculada.

## Modulo backend: IAM

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam`

Entidades e dominios:

- `User`
- `Organization`
- `Role`
- `UserStatus`
- `RbacPermissions`

Roles atuais:

| Role | Escopo |
|---|---|
| `ASTRO_ADMIN` | Astro/plataforma; sem organizacao; acessa `/admin` |
| `MANAGER` | Gestao da concessionaria; acessa `/app` e gerencia sellers |
| `SELLER` | Operacao comercial da concessionaria |

Permissoes principais:

- `ASTRO_ADMIN`: `finance:view`, `organizations:view`, `organizations:approve`, `users:manage_all`.
- `MANAGER`: equipe, billing, planos, seguranca, leads e estoque.
- `SELLER`: seguranca, leads e estoque.

Endpoints relevantes:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/2fa/verify`
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `PATCH /api/v1/users/me/password`
- `PATCH /api/v1/users/me/notifications`
- `PATCH /api/v1/users/me/two-factor`
- `GET /api/v1/users`
- `GET /api/v1/users/assignable`
- `POST /api/v1/users/invite`
- `PATCH /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/role`
- `PATCH /api/v1/users/{id}/suspend`
- `PATCH /api/v1/users/{id}/reactivate`
- `DELETE /api/v1/users/{id}`
- `GET /api/v1/organization`
- `PUT /api/v1/organization`
- `PATCH /api/v1/organization/whatsapp`

Estado atual:

- Perfil do usuario suporta `avatarUrl`; a troca de foto de perfil atualiza o usuario e replica no menu via frontend.
- Manager pode criar/editar/suspender/remover apenas usuarios `SELLER`.
- `admin@astro.dev` e filtrado da listagem de equipe.
- `admin@astro.dev` e garantido como `ASTRO_ADMIN`, sem organizacao.

## Modulo backend: Admin Astro

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/admin`

Objetivo:

- Painel administrativo da plataforma LeadFlow, separado da visao da concessionaria.

Endpoint:

- `GET /api/v1/admin/dashboard`

Dados retornados:

- MRR atual.
- Receita do mes.
- Assinaturas ativas.
- Churn mensal.
- ARPA.
- Projecao em 12 meses.
- ARR.
- Novas assinaturas do mes.
- Cancelamentos do mes.
- Cadastros pendentes de aprovacao.
- Ticket medio.
- Receita dos ultimos 12 meses.
- Crescimento de assinantes.
- Novas assinaturas vs cancelamentos.
- Receita por plano.
- Projecoes de 3, 6 e 12 meses.
- Tabela de assinaturas.

Estado atual:

- A rota foi validada com `admin@astro.dev` e respondeu `200`.
- O frontend possui tooltips nos cards de insight para explicar siglas como MRR, ARPA e ARR.

## Modulo backend: Onboarding

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding`

Objetivo:

- Receber solicitacoes publicas de cadastro de novas organizacoes.
- Permitir que o Astro admin aprove ou rejeite solicitacoes.

Endpoints:

- `POST /api/onboarding`
- `GET /api/v1/admin/onboarding`
- `GET /api/v1/admin/onboarding/{id}`
- `POST /api/v1/admin/onboarding/{id}/approve`
- `POST /api/v1/admin/onboarding/{id}/reject`
- Alias admin legado: `/api/admin/onboarding`

Fluxo de aprovacao:

1. Usuario envia formulario publico de onboarding.
2. Astro admin avalia a solicitacao.
3. Ao aprovar, o backend cria `Organization`.
4. Cria owner com role `MANAGER` e status `PENDING_VERIFICATION`.
5. Resolve plano solicitado.
6. Cria assinatura SaaS.
7. Vincula referral se houver codigo valido.
8. Marca solicitacao como `APPROVED`.

Status relevantes:

- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`

## Modulo backend: CRM

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/crm`

Objetivo:

- Gerenciar leads, pipeline, responsaveis, historico e kanban.

Entidades:

- `Lead`
- `LeadStageHistory`
- `PipelineStage`

Pipeline atual:

1. `TALKING_TO_BOT`
2. `WAITING_ATTENDANCE`
3. `IN_ATTENDANCE`
4. `IN_NEGOTIATION`
5. `COMPLETED`
6. `ARCHIVED`

Endpoints:

- `POST /api/v1/leads`
- `GET /api/v1/leads`
- `GET /api/v1/leads/{id}`
- `GET /api/v1/leads/{id}/activities`
- `PUT /api/v1/leads/{id}`
- `PATCH /api/v1/leads/{id}/move`
- `PATCH /api/v1/leads/{id}/archive`
- `PATCH /api/v1/leads/{id}/assume`
- `PATCH /api/v1/leads/{id}/assign`
- `DELETE /api/v1/leads/{id}`
- `GET /api/v1/kanban/board`

Regras observadas:

- Toda movimentacao de etapa gera historico.
- Leads podem ser atribuidos a `SELLER` ou `MANAGER`.
- Leads duplicados sao marcados com `isDuplicate`.
- O board Kanban e derivado dos leads por etapa.

## Modulo backend: Analytics

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/analytics`

Objetivo:

- Gerar KPIs e graficos do dashboard da concessionaria.

Endpoints:

- `GET /api/v1/analytics/kpis`
- `GET /api/v1/analytics/charts/monthly-sales`
- `GET /api/v1/analytics/charts/sales-by-category`

KPIs calculados:

- Vendas do mes.
- Receita total do mes.
- Veiculos em estoque.
- Ticket medio.
- Leads convertidos.
- Tempo medio de resposta.
- Leads totais do mes.

Origem dos dados:

- Leads.
- Estoque.
- Mensagens de chat.

## Modulo backend: Inventory

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/inventory`

Objetivo:

- Gerenciar veiculos, status de estoque e fotos.

Entidades:

- `Vehicle`
- `VehiclePhoto`
- `VehicleStatus`
- `VehicleCategory`

Endpoints:

- `POST /api/v1/vehicles`
- `GET /api/v1/vehicles`
- `GET /api/v1/vehicles/{id}`
- `PUT /api/v1/vehicles/{id}`
- `DELETE /api/v1/vehicles/{id}`
- `GET /api/v1/vehicles/summary`
- `POST /api/v1/vehicles/{vehicleId}/photos`
- `GET /api/v1/vehicles/{vehicleId}/photos`
- `DELETE /api/v1/vehicles/{vehicleId}/photos/{photoId}`
- `PATCH /api/v1/vehicles/{vehicleId}/photos/reorder`

Estados de veiculo:

- `AVAILABLE`
- `RESERVED`
- `SOLD`
- `MAINTENANCE`

Observacoes:

- Fotos sao modeladas como entidade propria.
- Existem servicos para URL de foto e reorder.
- S3/LocalStack existe como infraestrutura preparada.

## Modulo backend: Communication

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/communication`

Objetivo:

- Registrar mensagens, enviar mensagens via Meta/WhatsApp e receber webhooks Meta.

Entidades:

- `ChatMessage`
- `MessageDirection`
- `MessageStatus`
- `MessageType`

Endpoints:

- `GET /api/v1/chat/lead/{leadId}`
- `POST /api/v1/chat/send`
- `GET /api/v1/webhooks/meta`
- `POST /api/v1/webhooks/meta`

Fluxos:

- `MessageSenderService` envia mensagens usando credenciais WhatsApp da organizacao.
- `BotService` participa do fluxo inicial de atendimento.
- Webhook Meta pode validar assinatura quando secret configurado.
- SQS consumer existe para consumo assicrono quando habilitado.

## Modulo backend: Ingestion

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/ingestion`

Objetivo:

- Receber leads externos por webhook, normalizar, deduplicar, enfileirar ou processar localmente.

Endpoints:

- `POST /api/v1/webhooks/scraper`
- `POST /api/v1/webhooks/webmotors`
- `POST /api/v1/webhooks/mercadolivre`
- `GET /api/v1/auth/mercadolivre/connect`
- `GET /api/v1/auth/mercadolivre/callback`

Fluxo de lead:

1. Recebe payload externo.
2. Normaliza telefone e marketplace.
3. Busca organizacao por slug ou credencial.
4. Verifica duplicidade.
5. Se AWS estiver habilitado, publica em fila.
6. Se AWS estiver desabilitado, processa localmente.
7. Cria lead em `TALKING_TO_BOT`.
8. Tenta enviar mensagem de boas-vindas.
9. Incrementa uso para controle de quota.

Marketplaces previstos:

- Webmotors.
- Mercado Livre.
- Scraper generico.

Seguranca dos webhooks de marketplace:

- Webmotors/Mercado Livre usam header `X-Webhook-Token` quando `marketplace.webhook-token` estiver configurado.
- Se o token estiver vazio em ambiente dev, o backend aceita e registra warning.

## Modulo backend: SaaS

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/saas`

Objetivo:

- Planos, assinaturas, billing, uso, marketplaces por organizacao e checkout AbacatePay.

Entidades:

- `Plan`
- `Subscription`
- `BillingHistoryEntry`
- `MarketplaceConfig`
- `MarketplaceCredential`
- `UsageLog`

Endpoints:

- `GET /api/v1/plans`
- `GET /api/v1/saas/subscription`
- `PATCH /api/v1/saas/subscription/plan/{planId}`
- `POST /api/v1/saas/subscription/checkout/{planId}`
- `POST /api/v1/saas/subscription/cancel`
- `GET /api/v1/saas/usage`
- `GET /api/v1/saas/billing-history`
- `GET /api/v1/saas/marketplaces`
- `POST /api/v1/saas/marketplaces`
- `PATCH /api/v1/saas/marketplaces/{id}`
- `DELETE /api/v1/saas/marketplaces/{id}`
- `POST /api/v1/webhooks/abacatepay`

Planos seedados:

- Basico mensal, semestral e anual.
- Profissional mensal, semestral e anual.
- Enterprise mensal, semestral e anual.

Regras importantes:

- Troca direta de plano (`PATCH /subscription/plan`) esta bloqueada e exige checkout AbacatePay.
- Checkout self-service chama AbacatePay.
- Webhook AbacatePay atualiza assinatura, historico de cobranca e comissao de referral.
- Eventos tratados: `subscription.completed`, `subscription.renewed`, `subscription.cancelled`.

Estado AbacatePay:

- Codigo do client existe e chama `https://api.abacatepay.com/v2`.
- Configuracao espera `ABACATEPAY_API_KEY`.
- Webhook espera `ABACATEPAY_WEBHOOK_SECRET`.
- `ABACATEPAY_WEBHOOK_PUBLIC_KEY` tem valor default no `application.yaml`, mas pode ser sobrescrito por env.
- O processo Java atual nao exibiu `ABACATEPAY_API_KEY` e `ABACATEPAY_WEBHOOK_SECRET` no ambiente. Com esse estado, o checkout tende a falhar com `API key nao configurada`.
- Validacao local confirmou esse ponto: o checkout respondeu `502` com mensagem de API key nao configurada.

Configuracao recomendada do webhook AbacatePay em dev:

```text
Nome: LeadFlow - Assinaturas
URL: https://<seu-tunnel>.trycloudflare.com/api/v1/webhooks/abacatepay?webhookSecret=<seu-secret>
Secret: mesmo valor de ABACATEPAY_WEBHOOK_SECRET
Eventos: subscription.completed, subscription.renewed, subscription.cancelled
```

O quick tunnel gratuito muda de URL quando o processo `cloudflared` e encerrado.

## Modulo backend: Referrals

Local: `backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals`

Objetivo:

- Gerenciar codigo de indicacao, empresas indicadas, saldo e comissoes.

Entidades:

- `ReferralRelationship`
- `CommissionRecord`
- `ReferralStatus`
- Value objects `ReferralCode` e `ReferralLink`.

Endpoints:

- `GET /api/referral/my-code`
- `GET /api/referral/my-referrals`
- `GET /api/referral/my-balance`
- Aliases em `/api/v1/referral/*`
- `GET /api/admin/referral/summary`

Estado atual:

- Referral code passou a ser user-scoped.
- Aprovacao de onboarding pode criar relacionamento de referral.
- Pagamento confirmado pela AbacatePay pode gerar comissao no primeiro pagamento.

## Frontend: organizacao geral

Local: `frontend/src`

Blocos principais:

- `App.tsx`: rotas, layouts, providers e lazy loading.
- `contexts/AuthContext.tsx`: login, logout, restore session, keepalive por atividade.
- `contexts/NotificationContext.tsx`: notificacoes da aplicacao.
- `contexts/ThemeContext.tsx`: tema.
- `lib/api.ts`: Axios, token em memoria, refresh centralizado e retry.
- `lib/rbac.ts`: permissoes por role.
- `lib/navigation.ts`: home por role.
- `services/*`: comunicacao HTTP com a API.
- `hooks/api/*`: camada React Query.
- `pages/*`: telas.
- `components/*`: layout, modais, UI e componentes por dominio.
- `modules/*`: barrels por dominio.
- `features/*`: componentes e utilitarios especificos de feature.

## Frontend: rotas

Rotas publicas:

- `/`
- `/onboarding`
- `/app/login`

Rotas de concessionaria:

- `/app`
- `/app/dashboard`
- `/app/leads`
- `/app/kanban`
- `/app/estoque`
- `/app/estoque/:id`
- `/app/comunicacao`
- `/app/configuracoes`

Rotas Astro admin:

- `/admin`
- `/admin/dashboard`
- `/admin/onboarding`

Regras de acesso:

- `/app` aceita `MANAGER` e `SELLER`.
- `/admin` aceita somente `ASTRO_ADMIN`.
- Login usa `getHomePathForRole`; `ASTRO_ADMIN` vai para `/admin/onboarding`, demais vao para `/app/dashboard`.

## Frontend: modulos e telas

### Admin

Arquivos principais:

- `frontend/src/pages/admin/AdminDashboard.tsx`
- `frontend/src/pages/admin/AdminOnboarding.tsx`
- `frontend/src/components/admin/AdminOnboardingDetailDialog.tsx`
- `frontend/src/components/admin/RejectOnboardingDialog.tsx`
- `frontend/src/services/admin-dashboard.service.ts`
- `frontend/src/services/admin-onboarding.service.ts`
- `frontend/src/hooks/api/useAdminDashboard.ts`
- `frontend/src/hooks/api/useAdminOnboarding.ts`

Estado:

- Dashboard financeiro com graficos Recharts.
- Cards de metricas possuem legendas em hover.
- Onboarding admin permite analisar, aprovar e rejeitar solicitacoes.

### CRM e Leads

Arquivos principais:

- `frontend/src/pages/Dashboard.tsx`
- `frontend/src/pages/Leads.tsx`
- `frontend/src/pages/Kanban.tsx`
- `frontend/src/features/leads/*`
- `frontend/src/components/kanban/*`
- `frontend/src/hooks/api/useLeads.ts`
- `frontend/src/hooks/api/useKanbanBoard.ts`
- `frontend/src/hooks/api/useDashboard.ts`

Estado:

- Dashboard de concessionaria consome KPIs e graficos.
- Leads possuem filtros, paginacao, historico e operacoes de mover/arquivar.
- Kanban consome `/kanban/board`.

### Estoque

Arquivos principais:

- `frontend/src/pages/Estoque.tsx`
- `frontend/src/pages/VehicleDetail.tsx`
- `frontend/src/services/vehicles.service.ts`
- `frontend/src/hooks/api/useVehicles.ts`
- `frontend/src/modules/inventory/index.ts`

Estado:

- CRUD de veiculos.
- Tela de detalhe por veiculo.
- Suporte a fotos no backend.

### Comunicacao

Arquivos principais:

- `frontend/src/pages/Comunicacao.tsx`
- `frontend/src/services/chat.service.ts`
- `frontend/src/hooks/api/useChat.ts`
- `frontend/src/modules/communication/index.ts`

Estado:

- Consulta mensagens por lead.
- Envio de mensagem via service.
- Integra com historico de chat do backend.

### Configuracoes e SaaS

Arquivos principais:

- `frontend/src/pages/Configuracoes.tsx`
- `frontend/src/components/settings/GeneralTab.tsx`
- `frontend/src/components/settings/AssinaturaTab.tsx`
- `frontend/src/components/settings/NotificationsTab.tsx`
- `frontend/src/components/settings/PaymentsTab.tsx`
- `frontend/src/components/settings/ReferralTab.tsx`
- `frontend/src/components/settings/SecurityTab.tsx`
- `frontend/src/services/saas.service.ts`
- `frontend/src/services/billing-history.service.ts`
- `frontend/src/services/referrals.service.ts`
- `frontend/src/hooks/api/useSaas.ts`
- `frontend/src/hooks/api/useBillingHistory.ts`
- `frontend/src/hooks/api/useReferrals.ts`
- `frontend/src/hooks/api/useTeam.ts`

Estado:

- Aba geral atualiza perfil, telefone e avatar.
- Aba assinatura lista plano, billing, marketplace configs e checkout.
- Aba referral mostra codigo, link, saldo e indicacoes.
- Aba seguranca atualiza senha e 2FA.
- Aba notificacoes atualiza preferencias.

### Onboarding publico

Arquivos principais:

- `frontend/src/pages/Onboarding.tsx`
- `frontend/src/features/onboarding/OnboardingStepper.tsx`
- `frontend/src/features/onboarding/schema.ts`
- `frontend/src/features/onboarding/viacep.ts`
- `frontend/src/services/onboarding.service.ts`

Estado:

- Formulario multi-step para solicitacao de nova organizacao.
- Suporte a CEP/ViaCEP.
- Suporte a plano e codigo de referral.

## Persistencia e migrations

Migrations atuais:

| Migration | Tema |
|---|---|
| `V1__init_schema.sql` | Schema inicial |
| `V2__add_api_key_to_organizations.sql` | API key por organizacao |
| `V3__marketplace_integration.sql` | Integracao marketplaces |
| `V4__abacatepay_subscription_integration.sql` | AbacatePay, checkout e billing history |
| `V5__saas_plan_offers.sql` | Ofertas de planos SaaS |
| `V6__onboarding_admin_referrals.sql` | Onboarding admin e referrals |
| `V7__fix_onboarding_state_varchar.sql` | Ajuste de status onboarding |
| `V8__onboarding_referral_contract_parity.sql` | Paridade do contrato de referral |
| `V9__referral_commission_records.sql` | Registros de comissao |
| `V10__rbac_user_referral_codes.sql` | RBAC e referral codes por usuario |
| `V11__ensure_platform_admin_identity.sql` | Identidade do Astro admin |

Ponto de atencao:

- Existe tambem `v1__init_schema.sql` com `v` minusculo. O Flyway detecta e ignora por convencao de nome. Isso gera warning no boot. Recomenda-se remover o duplicado, renomear conscientemente ou validar historico antes de mexer se algum ambiente ja aplicou algo manualmente.

## Configuracao local

Backend dev:

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Postgres: `jdbc:postgresql://localhost:5432/leadflow`
- Redis: `localhost:6379`
- Mail local: `localhost:1025`
- LocalStack: `localhost:4566`

Frontend dev:

- Vite default: `http://localhost:3000`
- Se a porta estiver ocupada: usar `3001`.
- API base em dev: `VITE_API_URL=/api/v1` com proxy do Vite.

Comandos recomendados:

```bash
# Backend
cd backend
./gradlew --stop
export ABACATEPAY_API_KEY='valor'
export ABACATEPAY_WEBHOOK_SECRET='valor'
./gradlew --no-daemon bootRun

# Frontend
cd frontend
source ~/.nvm/nvm.sh
npm install
npm run dev -- --host 127.0.0.1 --port 3001

# Tunnel para webhook em desenvolvimento
cloudflared tunnel --url http://localhost:8080
```

## Testes existentes

Backend:

- Testes de AuthService.
- Testes de RBAC/authority.
- Testes de onboarding approval.
- Testes de referral.
- Testes de subscription e AbacatePay subscription service.

Frontend:

- Testes de AuthContext.
- Testes de dashboard admin e onboarding admin.
- Testes de configuracoes.
- Testes de hooks React Query.
- Testes de services.
- Testes de modulos, paginas e componentes.
- Playwright configurado.

Comandos:

```bash
cd backend
./gradlew test

cd frontend
source ~/.nvm/nvm.sh
npm run test
npm run test:e2e
```

Nota: nesta geracao eu executei validacoes runtime via HTTP, mas nao rodei as suites completas de teste.

## Pendencias e riscos abertos

1. AbacatePay no processo atual
   - O backend esta UP, mas as variaveis AbacatePay nao apareceram no ambiente do processo Java.
   - Risco: checkout continuar falhando com `API key nao configurada`.
   - Como fechar: reiniciar com `./gradlew --stop`, exportar variaveis e usar `./gradlew --no-daemon bootRun`.

2. Webhook AbacatePay assinado
   - O endpoint existe e os eventos corretos estao implementados.
   - Falta validar com evento real da AbacatePay em tunnel publico.
   - Como fechar: usar `subscription.completed`, `subscription.renewed`, `subscription.cancelled` no painel e confirmar logs/alteracao de assinatura.

3. URL publica temporaria
   - Quick tunnel gratuito muda quando o processo cai.
   - Como fechar: usar dominio proprio ou Cloudflare named tunnel antes de producao.

4. Migration duplicada com nome invalido
   - `v1__init_schema.sql` e ignorado pelo Flyway.
   - Como fechar: remover duplicata ou regularizar apos comparar conteudo e historico.

5. Documentacao existente parcialmente defasada
   - Alguns docs antigos ainda citam roles antigas como `ADMIN`/`SUPER_ADMIN`.
   - Estado atual do codigo usa `ASTRO_ADMIN`, `MANAGER`, `SELLER`.
   - Como fechar: atualizar `docs/ARCHITECTURE.md`, `README.md` e docs correlatos.

6. Segredos expostos em fluxo local
   - A API key e o secret foram colados durante troubleshooting.
   - Como fechar: rotacionar no painel da AbacatePay ao final dos testes e manter secrets em `.env` local ignorado ou secret manager.

7. `APP_BASE_URL`
   - Backend usa `app.base-url` como fallback para retorno do checkout.
   - Frontend hoje envia `returnUrl` e `completionUrl`, mas se o fallback for usado ele aponta para `localhost:8080/app/configuracoes`.
   - Como fechar: configurar `APP_BASE_URL` para a URL correta do frontend/publica em ambientes reais.

8. Validacao E2E completa
   - Login admin/manager e endpoints principais foram validados via HTTP.
   - Falta rodar fluxo browser completo com Playwright depois de estabilizar AbacatePay/tunnel.

## Oportunidades de melhoria

1. Healthcheck de configuracao operacional
   - Criar endpoint admin mascarado para indicar `abacatePay.apiKeyConfigured`, `webhookSecretConfigured`, `metaConfigured`, `redisUp`, `dbUp`.
   - Ajuda a evitar o ciclo de "subiu, mas checkout falha".

2. Script de dev padronizado
   - Criar `scripts/dev-backend.sh` que carrega `.env.local`, para o Gradle Daemon e sobe com `--no-daemon`.
   - Reduz erro de variavel nao exportada.

3. Runbook de webhooks
   - Criar `docs/RUNBOOK-WEBHOOKS.md` com AbacatePay, Meta, Webmotors e Mercado Livre.
   - Incluir exemplos de payload, headers, eventos e teste local.

4. Tela admin de integracoes
   - No painel Astro, exibir status das integracoes por ambiente.
   - Opcionalmente incluir teste controlado de credenciais AbacatePay sem expor segredo.

5. Suite E2E por papel
   - Criar testes Playwright para `ASTRO_ADMIN`, `MANAGER` e `SELLER`.
   - Cobrir redirecionamento pos-login, acesso negado, equipe sem admin e dashboard admin.

6. Revisao de docs canonicos
   - Atualizar README e docs canonicos para refletir os roles atuais, painel admin e AbacatePay.

## Proximo passo recomendado

1. Reiniciar backend com AbacatePay usando `--no-daemon`.
2. Confirmar que o processo Java recebeu `ABACATEPAY_API_KEY` e `ABACATEPAY_WEBHOOK_SECRET`.
3. Testar checkout em `/app/configuracoes`.
4. Confirmar webhook real AbacatePay pelo tunnel.
5. Rodar testes backend e frontend.
6. Atualizar docs canonicos que ainda usam nomenclatura antiga.
