# Architecture

## Estrutura do Monorepo

```
Astro-LeadFlow/
├── backend/     Kotlin + Spring Boot 3.2 — API REST, domínio, segurança
├── frontend/    React 18 + Vite + Tailwind + shadcn/ui — SPA
├── infra/       Docker Compose, assets de monitoramento Prometheus
└── docs/        Relatórios operacionais e pacote Notion
```

---

## Frontend

### Roteamento

```
/                         → Landing page pública (LandingLayout)
/app/login                → Login (redireciona se autenticado)
/app/dashboard            → Dashboard KPIs + charts
/app/leads                → Listagem de leads com filtros e timeline
/app/kanban               → Pipeline Kanban drag-and-drop
/app/estoque              → Catálogo de veículos
/app/estoque/:id          → Detalhe do veículo
/app/comunicacao          → Histórico de WhatsApp por lead
/app/configuracoes        → Configurações (ADMIN + MANAGER via RoleGate)
```

O `AppLayout` (sidebar + navbar) envolve todas as rotas `/app/*`. A `AuthGuard` redireciona não-autenticados para `/app/login`. Todas as rotas do app usam `React.lazy` + `Suspense`.

### Camada de Serviços

```
frontend/src/
├── services/
│   ├── api.ts                         axios instance + interceptors
│   ├── auth.service.ts                login / refresh / logout
│   ├── users.service.ts               perfil, senha, 2FA, convite, equipe
│   ├── leads.service.ts               CRUD + filtros + atividades
│   ├── kanban.service.ts              board endpoint dedicado
│   ├── vehicles.service.ts            catálogo + detalhe
│   ├── saas.service.ts                planos + uso + assinatura
│   └── billing-history.service.ts     histórico de cobrança paginado
├── hooks/api/
│   ├── useProfile.ts                  perfil do usuário logado
│   ├── useLeads.ts                    listagem + filtros
│   ├── useKanbanBoard.ts              board com colunas e cards
│   ├── useVehicles.ts                 catálogo
│   ├── useDashboard.ts                KPIs + charts
│   ├── useSaas.ts                     assinatura + uso
│   └── useBillingHistory.ts           histórico de cobrança
└── modules/                           module boundaries por domínio
    ├── crm/index.ts
    ├── inventory/index.ts
    ├── communication/index.ts
    └── saas/index.ts
```

### Module Boundaries

Páginas de rota importam domínio exclusivamente via `@/modules/*`. A regra é reforçada por:

- ESLint `no-restricted-imports` nas páginas de rota.
- Teste de regressão em `src/test/module-boundaries.test.ts`.

### Gerenciamento de Estado

- React Query com query keys sempre org-scoped: `[orgId, 'leads', filters]`.
- `queryClient.clear()` obrigatório no logout para evitar vazamento entre organizações.
- Dados em memória no store local do React Query — sem Redux ou Zustand.

### Autenticação e Sessão

```
Login →  accessToken  → memória JS (nunca localStorage)
      →  refreshToken → cookie httpOnly (set pelo backend)

Interceptor de request: anexa accessToken do estado do AuthContext
Interceptor de response: 401 → POST /auth/refresh usando o cookie → retenta request
Logout: POST /auth/logout (revoga refresh no backend) → limpa contexto → clear queryClient
```

A sessão sobrevive a reload da página: o interceptor de 401 dispara o refresh antes de redirecionar para login.

---

## Backend

### Pacote Canônico

```
com.astro.leadflow.shared.modules.*
```

Nunca `com.astro.leadflow.modules.*` — testes de pacote garantem isso em CI.

### Cadeia de Filtros de Segurança

```
Request →  ApiKeyAuthFilter          (scraper webhook: API key)
        →  RateLimitFilter           (Redis sliding-window: auth / webhook / general)
        →  MetaWebhookSignatureFilter (HMAC-SHA256 + anti-replay Redis)
        →  Spring Security JWT        (todas as rotas autenticadas)
```

#### Anti-replay do Webhook

- Chave primária de deduplicação: header `X-Hub-Delivery`.
- Fallback: hash da assinatura quando o header não existe.
- Janela configurável via `app.meta.replay-window-seconds`.
- Em produção (`app.meta.webhook-secret` em branco): startup falha imediatamente.

#### Rate Limiting

Buckets Redis sliding-window:

| Bucket | Rota-alvo |
|---|---|
| `auth` | `/api/v1/auth/**` |
| `webhook` | `/api/v1/webhooks/**` |
| `general` | Demais rotas |

#### Revogação de Refresh Token

- `RefreshTokenBlocklistService` armazena tokens revogados no Redis.
- `POST /auth/logout` revoga o token antes de responder.
- Tentativa de refresh com token revogado retorna 401.

---

## Observabilidade

Contadores emitidos pelo backend:

| Contador | Significado |
|---|---|
| `leadflow.rate_limit.allowed` | Requests aprovados pelo rate limiter |
| `leadflow.rate_limit.blocked` | Requests bloqueados (429) |
| `leadflow.rate_limit.errors` | Erros internos no limiter |
| `leadflow.webhook.meta.accepted` | Webhooks Meta aceitos |
| `leadflow.webhook.meta.rejected` | Webhooks Meta rejeitados (assinatura / replay) |

Alertas Prometheus: `infra/monitoring/prometheus/leadflow-alerts.yml`

---

## Infraestrutura Local

```
PostgreSQL   → localhost:5432  banco: leadflow / usuário: leadflow
Redis        → localhost:6379  rate limit, anti-replay, blocklist
MailHog      → localhost:1025  SMTP dev
LocalStack   → localhost:4566  AWS simulado (S3, SES, Lex — dev)
```

---

## Páginas Relacionadas

- [Runbooks](./03-Runbooks.md)
- [API Reference](./05-API-Reference.md)
- [Roadmap e Próximos Passos](./06-Roadmap.md)
