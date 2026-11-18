# Varredura Geral do Projeto - Consolidacao Tecnica

Historical note: este arquivo registra uma consolidacao anterior. Para o estado atual do projeto, use a documentacao canonica na raiz de `docs/`, comecando por `docs/README.md`.

Data original da analise: 2026-04-17  
Estado revalidado em: 2026-04-20  
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

## O que continua aberto apos a varredura

1. **Persistencia de sessao apos refresh**
   - estado atual: comportamento intencional de relogin
   - pendencia: registrar se isso fica como regra oficial do produto ou sobe para v1.2

2. **Diretorio dedicado para `assignedTo`**
   - estado atual: filtro depende do dataset carregado
   - pendencia: criar endpoint dedicado ou aceitar a restricao atual

3. **Redirect router-aware em 401**
   - estado atual: `window.location.href`
   - pendencia: substituir por evento consumido pelo React Router

4. **Cleanup tecnico**
   - `hooks/use-toast.ts` ainda existe embora Sonner seja o caminho canonico

## Validacao objetiva da baseline atual

- `cd frontend && npm run lint` -> PASS com warnings conhecidos
- `cd frontend && npm run test:coverage` -> PASS
- `cd frontend && npm run test:e2e` -> PASS (15 passed)
- `cd backend && ./gradlew test` -> BUILD SUCCESSFUL

## Conclusao

A varredura de 2026-04-17 esta tecnicamente fechada pela Phase 12. O que resta nao e mais uma pendencia critica de integracao ou seguranca da baseline; agora sao itens de governanca, produto e cleanup que devem ser tratados no fechamento do milestone ou promovidos para v1.2.
