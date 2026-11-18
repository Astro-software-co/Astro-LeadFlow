# Astro LeadFlow — Health Report

Historical note: este arquivo registra um snapshot operacional anterior. Para o estado atual consolidado, use a documentacao canonica na raiz de `docs/`, comecando por `docs/README.md`.

**Data:** 2026-04-20  
**Revisao de:** Phases 08-12 (milestone v1.1)  
**Auditoria:** Codex com leitura direta do codigo e rerun do quality gate

---

## Resumo executivo

O projeto esta em estado solido. A Phase 12, que era o consolidado da varredura de 2026-04-17, esta tecnicamente entregue no codigo e foi revalidada no ambiente local em 2026-04-20. Nao ha bloqueador funcional dentro do escopo atual do roadmap.

O que ainda resta esta concentrado em tres grupos:

1. **Governanca de workflow**: commitar a Phase 12, executar `/gsd:verify-work 12`, `/gsd:ship 12`, e fechar o milestone.
2. **Decisoes de produto**: persistencia de sessao apos refresh e estrategia do diretorio de responsaveis (`assignedTo`).
3. **Debito controlado**: redirect router-aware em 401, limpeza do toast legado e definicao sobre arquivos suspeitos no working tree.

| Dimensao | Status | Observacao |
|---|---|---|
| Integracao Frontend-Backend | Verde | Billing history, logout, settings e kanban dedicados estao alinhados |
| Seguranca | Verde com ressalvas baixas | Tokens em memoria, logout revoga refresh, anti-replay Redis; redirect 401 ainda usa `window.location.href` |
| Testes | Verde | `lint`, `test:coverage`, `test:e2e`, `./gradlew test` revalidados em 2026-04-20 |
| UX / Produto | Verde com backlog claro | Dialog de archive e payments reais entregues; sessao apos refresh continua uma decisao de produto |
| Docs / Planejamento | Verde | `STATE`, `ROADMAP`, `PROJECT`, `README`, `CLAUDE` e varredura sincronizados nesta rodada |

---

## Validacao objetiva (2026-04-20)

- `cd frontend && npm run lint` -> PASS com 15 warnings conhecidos
- `cd frontend && npm run test:coverage` -> PASS
  - Statements 92.79%
  - Branches 83.76%
  - Functions 86.82%
  - Lines 92.79%
- `cd frontend && npm run test:e2e` -> PASS (15 passed, 0 failed, 1.6m)
- `cd backend && ./gradlew test` -> BUILD SUCCESSFUL

**Prerequisito importante para E2E:** o frontend sobe via Playwright `webServer`, mas o backend precisa estar ativo em `127.0.0.1:8080`.

---

## O que fechou desde a varredura de 2026-04-17

### Integracao

- `/api/v1/saas/billing-history` existe no backend e atende o frontend com payload tipado.
- Logout deixou de ser no-op: o frontend envia refresh token e o backend revoga via blocklist.
- `Configuracoes` persiste senha, 2FA, notificacoes, convite e remocao de usuario em endpoints reais.
- `Kanban.tsx` saiu de `useLeads` e passou a consumir `/api/v1/kanban/board`.

### Seguranca

- Anti-replay do webhook Meta agora usa Redis com TTL de 5 minutos.
- Deduplicacao usa `X-Hub-Delivery` quando presente, com fallback por assinatura apenas quando necessario.
- O backend segue com fail-fast para secret ausente em profile de producao.

### UX / Frontend

- Arquivamento de lead usa dialog da aplicacao, nao `window.confirm`.
- `PaymentsTab` passou a refletir dados reais da assinatura.
- Fluxos de seguranca em `Configuracoes` deixaram de exibir falso sucesso local.

### Testes

- Cobertura ficou estavel e acima do threshold de branches.
- `users.service` e `billing-history.service` deixaram de ter cobertura zero.
- A suite E2E completa saiu de "mapeada" para "executada full green".

---

## Pontos ainda abertos

| Item | Gravidade | Estado atual | Recomendacao |
|---|---|---|---|
| Persistencia de sessao apos refresh | Media (produto) | Tokens em memoria; reload exige relogin | Confirmar se vira regra oficial do v1.1 ou se sobe para v1.2 com cookie httpOnly |
| Diretorio de responsaveis para filtro de leads | Media | Ainda depende do dataset carregado | Criar `/api/v1/users/assignable` ou confirmar modelo atual |
| Redirect pos-401 router-aware | Baixa | `frontend/src/lib/api.ts` usa `window.location.href` | Substituir por evento + `navigate('/app/login')` |
| Toast legado Radix | Baixa | `hooks/use-toast.ts` e wrappers ainda existem, mas nao sao o canonico | Limpar em task de cleanup |
| Publicacao Notion | Baixa | Pacote markdown pronto no repo | Definir workspace, responsavel e visibilidade |

---

## Riscos residuais

1. Se a decisao de sessao nao for registrada, o comportamento "reload = relogin" pode continuar parecendo bug para usuario final.
2. Se o redirect 401 continuar via `window.location.href`, o comportamento segue funcional, mas com full reload desnecessario.

---

## Proximo passo recomendado

1. Commits atomicos da Phase 12.
2. `/gsd:verify-work 12`
3. `/gsd:ship 12`
4. `/gsd:complete-milestone v1.1`
