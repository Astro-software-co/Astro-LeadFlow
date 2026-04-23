# Astro LeadFlow - Notion Documentation Index

Historical note: `docs/notion/` continua disponivel como pacote de exportacao/publicacao. A referencia principal do estado atual agora esta na raiz de `docs/`.

This folder contains the Markdown source used to publish the Astro LeadFlow documentation into Notion.

**Last updated:** 2026-04-20 — v1.1 complete; v1.2 roadmap (chatbot Lex + Bedrock) added.

## Top-level Pages

1. [Overview](./01-Overview.md)
2. [Architecture](./02-Architecture.md)
3. [Runbooks](./03-Runbooks.md)
4. [Onboarding](./04-Onboarding.md)
5. [API Reference](./05-API-Reference.md)
6. [Roadmap e Próximos Passos](./06-Roadmap.md)

## Publishing Workflow

1. Import each Markdown page into Notion as a top-level page under the `Astro LeadFlow` workspace.
2. Create a navigation page named `Index` and paste the links from this file.
3. Add cross-links between sections using the related links listed in each page.
4. Publish the workspace pages with internal visibility (team) or public visibility (if needed).

## v1.1 Highlights Captured

- Session persistence via `httpOnly` refresh cookie — reload no longer forces relogin.
- Dedicated `GET /kanban/board` endpoint decoupling pipeline from the generic leads query.
- `GET /saas/billing-history` endpoint with pagination and typed response.
- Logout revokes refresh token server-side (`RefreshTokenBlocklistService` via Redis).
- Distributed webhook anti-replay via Redis (`X-Hub-Delivery` deduplication key).
- Frontend real mutations for password, 2FA, notifications, invite, and delete user.
- Frontend module boundaries enforced by ESLint + regression test.
- Backend package-boundary regression test (`ArchitectureTest`).
- Vitest 92% statement coverage; Playwright E2E full suite green.
- Prometheus alert rules for auth/webhook risk signals.

## v1.2 Roadmap Captured

- Chatbot de atendimento automático: AWS Lex V2 (dialog) + Amazon Bedrock (generation).
- Notificações em tempo real via WebSocket / SSE.
- Envio de WhatsApp pelo frontend usando templates.
- Integração OLX (enum já existe no backend).
