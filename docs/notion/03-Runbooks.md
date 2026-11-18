# Runbooks

## Desenvolvimento Local

### 1. Subir infraestrutura

```bash
cd infra
docker compose up -d
```

Serviços iniciados: PostgreSQL, Redis, MailHog, LocalStack.

### 2. Subir backend

```bash
cd backend
./gradlew bootRun
```

API disponível em `http://localhost:8080`.

### 3. Subir frontend

```bash
cd frontend
npm install
npm run dev
```

App disponível em `http://localhost:3000`.

---

## Comandos de Validação

### Frontend

```bash
cd frontend
npm run lint                          # ESLint — deve terminar sem erros
npm run test:coverage                 # Vitest com relatório de cobertura
npm run test -- --run src/test/module-boundaries.test.ts  # Boundary regression
npm run test:e2e                      # Playwright (requer backend + infra ativos)
npm run build                         # Build de produção
```

Targets de cobertura (CI):

| Métrica | Mínimo |
|---|---|
| Statements | 80% |
| Branches | 80% |
| Functions | 80% |
| Lines | 80% |

Estado em 2026-04-20: statements 92.79% / branches 83.76% / functions 86.82%.

### Backend

```bash
cd backend
./gradlew test                                              # Suíte completa
./gradlew test --tests "*MetaWebhookSignatureFilterTest"    # Anti-replay
./gradlew test --tests "*RateLimitFilterTest"               # Rate limiting
./gradlew test --tests "*ApiKeyAuthFilterTest"              # API key auth
./gradlew test --tests "*ArchitectureTest"                  # Namespace boundary
```

---

## Incidente: Webhook Meta com Falhas

**Sintomas:**

- `leadflow_webhook_meta_rejected_total` elevado.
- Respostas 403 em `POST /api/v1/webhooks/meta`.

**Checklist:**

1. Confirmar que `META_WEBHOOK_SECRET` está configurado no ambiente.
2. Validar que o header `X-Hub-Signature-256` está presente e correto no payload de origem.
3. Verificar se é replay: mesma assinatura enviada repetidamente dentro da janela de deduplicação.
4. Verificar saúde do Redis — o anti-replay depende de escrita/leitura no cache.
5. Inspecionar a regra `LeadflowWebhookSignatureFailures` no Prometheus Alertmanager.

---

## Incidente: Pico de Rate Limiting

**Sintomas:**

- `leadflow_rate_limit_blocked_total` elevado.
- Respostas 429 em endpoints de auth ou webhook.

**Checklist:**

1. Identificar o bucket quente: `auth`, `webhook` ou `general`.
2. Verificar saúde e latência do Redis.
3. Validar comportamento do cliente upstream (retries agressivos, burst de requests).
4. Ajustar limites somente após confirmar legitimidade do tráfego.

---

## Incidente: Falha de Autenticação / Token Inválido

**Sintomas:**

- 401 em endpoints protegidos mesmo após login.
- Frontend redireciona para login em loop.

**Checklist:**

1. Verificar se o cookie `httpOnly` está sendo enviado pelo browser (`withCredentials: true`).
2. Confirmar que o backend em `POST /auth/refresh` retorna o novo `accessToken`.
3. Verificar se o refresh token foi revogado via `POST /auth/logout` em sessão anterior.
4. Inspecionar o Redis se o token está na blocklist de revogação.

---

## Incidente: E2E Playwright com Falhas

**Sintomas:**

- Testes de autenticação, kanban ou leads falham localmente.

**Checklist:**

1. Confirmar que o backend está ativo em `127.0.0.1:8080` antes de rodar `npm run test:e2e`.
2. Confirmar que o frontend está ativo em `127.0.0.1:3000`.
3. Confirmar que o banco tem os fixtures de seed corretos (usuário `admin@test.com` + org de teste).
4. Inspecionar o report do Playwright em `frontend/playwright-report/index.html`.

---

## Páginas Relacionadas

- [Architecture](./02-Architecture.md)
- [API Reference](./05-API-Reference.md)
