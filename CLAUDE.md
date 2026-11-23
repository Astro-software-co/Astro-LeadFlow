# Astro LeadFlow — Repository Context

## Project

Astro LeadFlow is a multi-tenant CRM SaaS for automotive dealerships. The repository includes the public landing, the authenticated CRM frontend, the Kotlin/Spring backend, infra assets, and GSD planning artifacts.

**Core value:** Every lead that enters the system must be visible, traceable, and actionable within seconds.

## Current State (2026-11-23)

- Branch: `develop`
- Phase 14 (RBAC, users, plans, settings, billing) is implemented and green; only manual checks remain (Abacate Pay sandbox checkout, settings tab overflow at notebook width).
- Chatbot runs in mock mode: slots are written to the Lead, handoff records stage history, the bot qualifies 24h. The real Lex engine, the real Meta client and the marketplace ingestion hardening are open work.
- Live status, blockers and open business rules: `openspec/STATUS.md`. Implemented behavior: `openspec/specs/`. Open work: `openspec/changes/` (validate with `npx @fission-ai/openspec validate --all`).
- CI/CD (GitHub Actions), `frontend/Dockerfile` and `infra/prod/` exist as a standby scaffold; nothing has run yet. `application-prod.yml` exists but the prod boot is untested.
- Backend tests need JDK 21: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- Last verified 2026-11-16: `cd backend && ./gradlew test` green; frontend lint, 213 unit tests and typecheck green; E2E not re-run.
- Dev logins (password `senha123`): `manager@astro.dev`, `seller@astro.dev`, `admin@astro.dev` (ASTRO_ADMIN).

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
- Access tokens stay in memory; refresh session restores through the backend httpOnly cookie after browser refresh.
- Lead assignee filters use the dedicated `/api/v1/users/assignable` endpoint.
- Post-401 handling emits `auth-session-ended`; it no longer hard-redirects through `window.location.href`.
- Sonner is the only toast system.

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
- `GET /api/v1/users/assignable`
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

## Cross-Phase Closure

No active open work remains from phases other than Phase 14.

Closed on 2026-04-29:

1. Session persistence after refresh is official: restore through httpOnly refresh cookie, keep access token in memory.
2. Dedicated assignee directory exists at `/api/v1/users/assignable`.
3. Post-401 flow uses auth session events instead of `window.location.href`.
4. Legacy Radix toast wrapper/dependency was removed; Sonner remains canonical.
5. Phase 15 UAT is closed 20/20 with static checks plus focused frontend/backend automated coverage.
6. Notion markdown package remains a publish-ready export; workspace publication is external distribution, not a repo blocker.

## Canonical Docs

The consolidated project knowledge base is the Obsidian vault:

- Windows: `C:\Users\ruy.de.castro\Desktop\ruy\meuObsidian\Projetos\Leadflow`
- WSL/Linux: `/mnt/c/Users/ruy.de.castro/Desktop/ruy/meuObsidian/Projetos/Leadflow`
- Entry point: `00-index.md`
- Repository bridge: `docs/OBSIDIAN_VAULT.md`

Use the vault only on demand. Do not read it automatically in every session.

Read the vault when the user explicitly asks for Obsidian/project documentation, when a task depends on current business rules or architectural decisions, or when documentation needs to be updated. For specific tasks, read only the relevant note.

Repository references that remain useful:

- Planning state: `.planning/STATE.md`
- Roadmap: `.planning/ROADMAP.md`
- Notion export index: `docs/notion/INDEX.md`
- Claude project-docs skill: `.claude/skills/astro-leadflow-project-docs/SKILL.md`

## GSD Workflow Enforcement

Use the normal GSD workflow for any new implementation or closure action:

- `/gsd:quick` for small docs or cleanup
- `/openspec-propose` for a new feature or open item (see `openspec/changes/`), `/openspec-apply-change` to implement it
- `/gsd:verify-work 14` for the remaining manual checks of Phase 14

## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate the repository profile.
