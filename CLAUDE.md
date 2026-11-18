# Astro LeadFlow — Repository Context

## Project

Astro LeadFlow is a multi-tenant CRM SaaS for automotive dealerships. The repository includes the public landing, the authenticated CRM frontend, the Kotlin/Spring backend, infra assets, and GSD planning artifacts.

**Core value:** Every lead that enters the system must be visible, traceable, and actionable within seconds.

## Current State (2026-04-20)

- Branch: `develop`
- Last commit on branch: `8bffc64 feat(08-01): extract configuracoes tabs into settings components`
- Phase 12 is implemented in the working tree and revalidated by:
  - `cd frontend && npm run lint`
  - `cd frontend && npm run test:coverage`
  - `cd frontend && npm run test:e2e`
  - `cd backend && ./gradlew test`
- Planning docs are synced to the current codebase (`STATE`, `ROADMAP`, `PROJECT`, health report, scan report)
- Formal closure is still pending: commits, `/gsd:verify-work 12`, `/gsd:ship 12`, `/gsd:complete-milestone v1.1`

## Business Rules

### Lead lifecycle

1. `TALKING_TO_BOT`
2. `WAITING_ATTENDANCE`
3. `IN_ATTENDANCE`
4. `IN_NEGOTIATION`
5. `COMPLETED` or `ARCHIVED`

Every stage move generates timeline activity at `/api/v1/leads/{id}/activities`.

### Multi-tenancy

- `organizationId` comes from JWT claims
- React Query keys must start with `orgId`
- logout must clear the full query cache

### RBAC

- Roles: `ADMIN`, `MANAGER`, `SELLER` / `VENDEDOR`
- `/app/configuracoes` is restricted to `ADMIN` and `MANAGER`
- `assign` is restricted; `assume` remains operational

### Product decisions already taken

- Communication is a read-only log viewer with "Abrir no WhatsApp", not an in-app chat
- Public registration is removed; onboarding is invite/admin-driven
- Duplicate leads are flagged with `isDuplicate: true` and still trigger message flow
- Usage reset follows `renewsAt`, not calendar month
- Tokens stay in memory only; current behavior is `reload = relogin`

## Frontend Architecture

### Routes

- `/` -> landing
- `/app/dashboard`
- `/app/leads`
- `/app/kanban`
- `/app/estoque`
- `/app/estoque/:id`
- `/app/comunicacao`
- `/app/configuracoes`
- `/app/login`

### Shell and guards

- `AuthGuard` wraps authenticated routes
- `RoleGate` protects `/app/configuracoes`
- `/app` redirects to `/app/dashboard`
- routes use `React.lazy()` and `Suspense`

### Canonical frontend integrations

- auth: `frontend/src/services/auth.service.ts`
- users/settings: `frontend/src/services/users.service.ts`
- leads: `frontend/src/services/leads.service.ts`
- kanban: `frontend/src/services/kanban.service.ts`
- vehicles: `frontend/src/services/vehicles.service.ts`
- saas/billing: `frontend/src/services/saas.service.ts`, `frontend/src/services/billing-history.service.ts`
- hooks: `frontend/src/hooks/api/*`

## Backend Contracts Used by the Frontend

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me/password`
- `PATCH /api/v1/users/me/notifications`
- `PATCH /api/v1/users/me/two-factor`
- `GET /api/v1/users`
- `POST /api/v1/users/invite`
- `DELETE /api/v1/users/{id}`
- `GET /api/v1/leads`
- `GET /api/v1/leads/{id}/activities`
- `PATCH /api/v1/leads/{id}/move`
- `PATCH /api/v1/leads/{id}/archive`
- `PATCH /api/v1/leads/{id}/assume`
- `PATCH /api/v1/leads/{id}/assign`
- `GET /api/v1/kanban/board`
- `GET /api/v1/vehicles`
- `GET /api/v1/analytics/kpis`
- `GET /api/v1/analytics/charts/monthly-sales`
- `GET /api/v1/analytics/charts/sales-by-category`
- `GET /api/v1/chat/lead/{leadId}`
- `GET /api/v1/saas/subscription`
- `PATCH /api/v1/saas/subscription/plan/{planId}`
- `GET /api/v1/saas/usage`
- `GET /api/v1/saas/billing-history`

## Constraints

- Keep React 18 + Vite + Tailwind 3 + shadcn/ui
- Backend contracts are the source of truth
- Never store auth tokens in `localStorage`
- Preserve org-scoped cache keys and `queryClient.clear()` on logout
- Responsiveness is mandatory at 375 / 768 / 1024 / 1440
- PT-BR default, BRL default, Lucide icons only
- No default dark mode

## Open Decisions / Pending Cleanup

1. Confirm whether session persistence after refresh remains an official product rule
2. Decide whether to add a dedicated assignee directory endpoint
3. Replace `window.location.href` in `frontend/src/lib/api.ts`
4. Remove legacy toast files
5. Publish the Notion markdown package to the target workspace

## Canonical Docs

- Reconstructed state: `ESTADO-ATUAL-2026-04-20.md`
- Health report: `docs/HEALTH_REPORT.md`
- Scan report: `docs/varredura-geral-2026-04-17.md`
- Planning state: `.planning/STATE.md`
- Roadmap: `.planning/ROADMAP.md`
- Notion docs index: `docs/notion/INDEX.md`

## GSD Workflow Enforcement

Use the normal GSD workflow for any new implementation or closure action:

- `/gsd:quick` for small docs or cleanup
- `/gsd:verify-work 12` for Phase 12 UAT closure
- `/gsd:ship 12` for PR preparation
- `/gsd:complete-milestone v1.1` after merge

## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate the repository profile.
