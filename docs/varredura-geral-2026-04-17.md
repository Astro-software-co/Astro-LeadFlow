# Varredura Geral do Projeto - Consolidacao Tecnica

Historical note: este arquivo registra uma consolidacao anterior. Para o estado atual do projeto, use a documentacao canonica na raiz de `docs/`, comecando por `docs/README.md`.

Data original da analise: 2026-04-17  
Estado revalidado em: 2026-04-20  
Fechamento cross-phase: 2026-04-29
Escopo: backend, frontend, integracoes, testes, e consolidacao da Phase 12

## Contexto

Esta varredura originou a Phase 12 do roadmap. Em 2026-04-20 o codigo foi cruzado novamente com os artefatos do projeto e o quality gate foi rerodado. O resultado: os itens criticos e medios que motivaram a Phase 12 estao tecnicamente resolvidos no codigo atual.

## Fechamentos confirmados

1. Billing history deixou de ser divergente:
   - o backend agora expoe `GET /api/v1/saas/billing-history`
   - o frontend consome o contrato real via service e hook dedicados
2. Logout agora revoga refresh token server-side.
3. `Configuracoes` persiste senha, 2FA e preferencias de notificacao no backend.
4. Anti-replay do webhook Meta migrou para Redis com TTL e deduplicacao distribuida.
5. `Kanban` adotou o endpoint dedicado de board.
6. Coverage estabilizou acima do threshold de branches.
7. E2E full run saiu verde com 15 testes aprovados.
8. UX de archive e payments foi alinhada ao estado atual da aplicacao.

## O que saiu do caminho critico

- Falso sucesso local em `SecurityTab`
- `window.confirm` no arquivamento
- Pagamentos com dados estaticos
- Cobertura zero em `users.service` e `billing-history.service`
- Falta de execucao E2E consolidada

## Fechamento posterior das pendencias nao criticas

Em 2026-04-29, os itens que ainda apareciam como abertos neste snapshot foram fechados ou reclassificados como externos ao repositorio:

1. **Persistencia de sessao apos refresh**
   - fechamento: sessao restaura pelo refresh cookie httpOnly; access token continua apenas em memoria

2. **Diretorio dedicado para `assignedTo`**
   - fechamento: `GET /api/v1/users/assignable` existe e o frontend consome via `useAssignableUsers`

3. **Redirect router-aware em 401**
   - fechamento: `frontend/src/lib/api.ts` emite evento de sessao encerrada; nao ha `window.location.href` no cliente API

4. **Cleanup tecnico**
   - fechamento: componente/dependencia Radix toast removidos; Sonner permanece canonico

5. **Publicacao Notion**
   - fechamento para repo: `docs/notion/` e pacote exportado/historico pronto; publicar em workspace e distribuicao externa

## Validacao objetiva da baseline atual

- `cd frontend && npm run lint` -> PASS com warnings conhecidos
- `cd frontend && npm run test:coverage` -> PASS
- `cd frontend && npm run test:e2e` -> PASS (15 passed)
- `cd backend && ./gradlew test` -> BUILD SUCCESSFUL

## Conclusao

A varredura de 2026-04-17 esta tecnicamente fechada pela Phase 12. A rechecagem de 2026-04-29 fechou tambem os residuos documentais e de cleanup que ainda apareciam como abertos. O unico trabalho ativo restante no roadmap e a Phase 14.
