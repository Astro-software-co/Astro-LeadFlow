# Overview

## Produto

Astro LeadFlow é um CRM SaaS multi-tenant para concessionárias de veículos.

O produto centraliza leads de marketplaces (Webmotors, Mercado Livre) e WhatsApp em um único pipeline, dá visibilidade operacional para toda a equipe e reduz o tempo de resposta — o principal fator de conversão no setor automotivo.

**Problema central resolvido:** leads que chegam em canais diferentes são perdidos, respondidos tarde ou duplicados. O LeadFlow garante que todo lead seja visível, rastreável e acionável em segundos.

---

## Domínios do Produto

| Domínio | Função |
|---|---|
| **IAM** | Autenticação, papéis (ADMIN / MANAGER / SELLER) e isolamento por organização |
| **CRM** | CRUD de leads, pipeline Kanban, histórico de atividades |
| **Inventory** | Catálogo de veículos, status e filtros avançados |
| **Communication** | Visualizador do histórico de WhatsApp com atalho para atendimento |
| **SaaS** | Planos, uso por organização, histórico de cobrança |
| **Ingestion** | Webhooks de marketplace (Webmotors, Mercado Livre, Meta) |

---

## Fluxo Principal do Lead

```
Marketplace / WhatsApp
        ↓ webhook
   TALKING_TO_BOT          ← bot ativo (Lex + Bedrock — v1.3)
        ↓
  WAITING_ATTENDANCE        ← aguarda vendedor
        ↓ PATCH /leads/{id}/assume
    IN_ATTENDANCE           ← vendedor atende
        ↓
   IN_NEGOTIATION           ← proposta em andamento
        ↓
  COMPLETED  ou  ARCHIVED
```

Toda mudança de etapa gera uma entrada no histórico (`/leads/{id}/activities`).

---

## Decisões de Produto Fixadas

- Comunicação é um **visualizador de logs** — não há chat in-app. O vendedor abre o WhatsApp diretamente via botão.
- Registro público foi removido. Onboarding é por convite administrativo.
- Leads duplicados entram com `isDuplicate: true` e ainda disparam o fluxo de mensagem.
- Cota de uso segue modelo dual: global soberano + por marketplace organizacional.
- Reset de uso segue `renewsAt` da assinatura, não o mês calendário.
- `accessToken` em memória + `refreshToken` em cookie `httpOnly` — sessão sobrevive a reload.

---

## Snapshot de Versão

### v1.0 — CRM Fundacional (completo)

- Landing page pública com pricing, features e depoimentos.
- Autenticação JWT real, guards de rota e RBAC.
- Dashboard com KPIs reais, Kanban com drag-and-drop integrado ao backend.
- Leads CRUD, filtros avançados, timeline de atividades.
- Catálogo de veículos com página de detalhe.
- Comunicação: visualizador de histórico + "Abrir no WhatsApp".
- SaaS: aba de assinatura com planos, uso e histórico de cobrança.
- Configurações: perfil, senha, 2FA, notificações, equipe — todos com persistência real.
- Vitest 92% cobertura + Playwright E2E full verde.

### v1.1 — Hardening + Infraestrutura + Docs (completo)

- Arquitetura: module boundaries formalizados no frontend, testes de namespace no backend.
- Segurança: anti-replay de webhook via Redis, logout com revogação server-side, rate limiting com contadores Prometheus.
- Endpoint dedicado `GET /kanban/board` desacoplando pipeline do endpoint geral de leads.
- Endpoint `GET /saas/billing-history` com paginação e tipagem completa.
- Sessão persistente pós-reload via refresh cookie `httpOnly`.
- Documentação operacional publicável no Notion.

---

## Páginas Relacionadas

- [Architecture](./02-Architecture.md)
- [Runbooks](./03-Runbooks.md)
- [Onboarding](./04-Onboarding.md)
- [API Reference](./05-API-Reference.md)
- [Roadmap e Próximos Passos](./06-Roadmap.md)
