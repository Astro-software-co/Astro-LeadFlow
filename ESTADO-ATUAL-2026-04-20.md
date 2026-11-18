# Astro LeadFlow — Reconstrucao do Estado Atual

**Data:** 2026-04-20  
**Branch:** `develop`  
**Ultimo commit na branch:** `8bffc64 feat(08-01): extract configuracoes tabs into settings components`  
**Fonte de verdade:** codigo do repositorio, cross-check com `.planning/` e `docs/`

---

## 1. Resumo executivo

- Milestone ativo: `v1.1 - Hardening + Infra + Docs`
- Fase mais recente: `Phase 12 - Plano de Acao Varredura 2026-04-17`
- Os cinco plans da Phase 12 (`12-01` a `12-05`) estao implementados no codigo e possuem `SUMMARY.md` com `status: completed`
- O problema principal deixou de ser implementacao e passou a ser fechamento de workflow:
  - commitar a entrega
  - executar `/gsd:verify-work 12`
  - executar `/gsd:ship 12`
  - fechar o milestone com `/gsd:complete-milestone v1.1`

**Situacao real:** o roadmap esta 100% implementado no codigo atual. Restam governanca, decisao de produto e cleanup controlado.

---

## 2. Validacao objetiva executada em 2026-04-20

### Frontend

- `cd frontend && npm run lint` -> PASS com 15 warnings conhecidos
- `cd frontend && npm run test:coverage` -> PASS
  - Statements 92.79%
  - Branches 83.76%
  - Functions 86.82%
  - Lines 92.79%
- `cd frontend && npm run test:e2e` -> PASS (15 passed, 0 failed, 1.6m)

### Backend

- `cd backend && ./gradlew test` -> BUILD SUCCESSFUL

### Observacao importante

Para o `test:e2e` passar, o backend precisa estar ativo em `127.0.0.1:8080`. A primeira falha da rodada foi de ambiente (backend desligado), nao de regressao do produto.

---

## 3. O que esta fechado

### Phase 12-01

- `GET /api/v1/saas/billing-history` existe e atende o frontend
- `POST /api/v1/auth/logout` revoga refresh token no backend
- `RefreshTokenBlocklistService` esta ativo

### Phase 12-02

- `MetaWebhookSignatureFilter` usa Redis para anti-replay
- a chave principal de deduplicacao e `X-Hub-Delivery`
- assinatura fica como fallback quando o header nao existe

### Phase 12-03

- `usersService.changePassword`
- `usersService.updateNotifications`
- `usersService.updateTwoFactor`
- `usersService.inviteUser`
- `usersService.deleteUser`
- `AuthContext.logout` chama backend antes de limpar estado local

### Phase 12-04

- arquivamento usa `LeadArchiveConfirmDialog`
- `PaymentsTab` deixou de ser estatico e segue dados reais

### Phase 12-05

- `kanban.service.ts`, `useKanbanBoard.ts` e `types/api/kanban.ts` estao ativos
- `Kanban.tsx` usa o endpoint dedicado `/api/v1/kanban/board`
- coverage e E2E full run ficaram verdes

---

## 4. Regras de negocio consolidadas

### Fluxo de lead

1. Lead entra via webhook marketplace e nasce em `TALKING_TO_BOT`
2. Bot interage e o lead segue para `WAITING_ATTENDANCE`
3. Vendedor assume em `PATCH /leads/{id}/assume` e vai para `IN_ATTENDANCE`
4. Negociacao evolui para `IN_NEGOTIATION`
5. Saida final e `COMPLETED` ou `ARCHIVED`
6. Toda mudanca de etapa gera activity em `/leads/{id}/activities`

### Multi-tenancy

- `organizationId` vem do JWT
- query keys de React Query sempre comecam em `[orgId, ...]`
- logout chama `queryClient.clear()`

### RBAC

- papeis: `ADMIN`, `MANAGER`, `SELLER` / `VENDEDOR`
- `/app/configuracoes` e restrita a `ADMIN` e `MANAGER`
- `assign` e restrito a admin/manager; `assume` e liberado ao fluxo operacional

### Produto

- Comunicacao e **logs viewer**, nao chat embutido
- Duplicata entra com `isDuplicate: true` e ainda dispara mensagem
- Quota segue modelo dual: global + por marketplace
- Reset de usage acontece por `renewsAt`, nao por mes calendario
- Registro publico esta removido; onboarding e por convite administrativo

### Seguranca

- access token e refresh token ficam somente em memoria
- `localStorage` esta fora do fluxo de auth
- reload de pagina atualmente implica relogin
- logout revoga refresh token no backend

---

## 5. Objetivo, premissas e restricoes

### Objetivo

Astro LeadFlow e um CRM SaaS multi-tenant para concessionarias. O objetivo do produto e centralizar leads de marketplaces e WhatsApp, tornar o pipeline visivel em segundos e evitar perda de lead por resposta lenta.

### Premissas

- backend Kotlin/Spring e a fonte de verdade
- frontend adapta contratos, nao inventa contratos
- stack fixa: React 18 + Vite + Tailwind 3 + shadcn/ui
- PT-BR e idioma default
- BRL e moeda default
- `VITE_USE_MOCKS=true` e fallback de dev, nao validacao oficial

### Restricoes

- tokens nunca em `localStorage`
- responsividade obrigatoria em 375 / 768 / 1024 / 1440
- sem emoji como icone
- sem dark mode default
- namespace backend canonico: `com.astro.leadflow.shared.modules.*`
- fora do v1: OLX, app nativo, pagamento in-app, i18n, websocket real-time, report builder

---

## 6. Pendencias reais que continuam abertas

1. **Persistencia de sessao apos refresh**
   - hoje: relogin intencional
   - falta: decisao oficial de produto

2. **Diretorio dedicado para filtro `assignedTo`**
   - hoje: depende do dataset carregado
   - falta: endpoint dedicado ou confirmacao da restricao atual

3. **Redirect router-aware em 401**
   - hoje: `window.location.href`
   - falta: evento + `navigate('/app/login')`

4. **Cleanup do toast legado**
   - hoje: Sonner e canonico, mas `use-toast.ts` ainda existe
   - falta: task de limpeza

5. **Publicacao Notion**
   - hoje: markdown package pronto em `docs/notion/`
   - falta: importar e publicar no workspace certo

6. **Arquivos suspeitos no working tree**
   - limpos em 2026-04-20 apos confirmacao do usuario

---

## 7. Passo a passo de teste para decisoes abertas

### Teste da regra atual de sessao apos refresh

1. Fazer login em `/app/login`
2. Confirmar acesso a `/app/dashboard`
3. Recarregar a pagina com `Cmd/Ctrl + R`
4. Resultado esperado hoje: app redireciona para `/app/login`
5. Se esse comportamento for considerado bug de produto, a estrategia atual precisa mudar

### Teste do filtro `assignedTo`

1. Abrir `/app/leads`
2. Carregar a listagem com um dataset pequeno
3. Verificar quais responsaveis aparecem no filtro
4. Aplicar outro filtro que mude o dataset
5. Verificar se o diretorio de responsaveis mudou junto
6. Se isso for indesejado, o endpoint dedicado precisa entrar no proximo ciclo

### Teste do redirect pos-401

1. Fazer login normalmente
2. Invalidar manualmente o refresh token ou forcar 401 sem refresh valido
3. Disparar uma chamada autenticada
4. Resultado esperado hoje: full reload e ida para `/app/login`
5. Resultado desejado no backlog: navegacao via React Router sem reload completo

---

## 8. Perguntas simples que ainda precisam de resposta

1. O comportamento oficial do v1.1 deve continuar sendo `reload = relogin`, ou voce quer elevar persistencia de sessao para o proximo milestone?

---

## 9. Proximo passo recomendado

1. Fechar a decisao sobre persistencia de sessao
2. Fazer commits atomicos da Phase 12
3. Rodar `/gsd:verify-work 12`
4. Abrir PR com `/gsd:ship 12`
5. Encerrar o milestone com `/gsd:complete-milestone v1.1`
