# Tasks

## 1. Consolidação (F0)
- [x] 1.1 Merge com a `develop` atual, migration renomeada para `V19`, adaptador compilando; suíte completa verde e cadeia `V1…V19` validada em Postgres 16 com `ddl-auto=validate`

## 2. Uma classe por arquivo (T1.1)
- [x] 2.1 Dividir DTOs, contratos, requests/responses, comandos, views e enums do módulo em arquivos próprios; 47 testes `integrations.webmotors` verdes sem mudança além de imports

## 3. RBAC (T1.2)
- [x] 3.1 `hasRole('MANAGER')` no controller e matcher `/api/v1/integrations/**` em `SecurityConfig`; testes: `SELLER` 403, sem token 401, `ASTRO_ADMIN` 403, proposta de outra org 404

## 4. Porta única de entrada no CRM (T1.3)
- [x] 4.1 `LeadIntakeCommand`/`VehicleSnapshot`; `LeadIngestionService.ingestResolved`; `LeadIngestionServiceTest` verde antes e depois
- [x] 4.2 `CrmLeadIntakeAdapter` delega a `ingestResolved` (sem `TransactionTemplate`, sem anexo de notas); testes: novo, repetido (`isDuplicate`+mensagem), cota excedida, falha da Meta

## 5. Padrões da conta (T1.4)
- [x] 5.1 `AUTO` e `welcomeMessageEnabled=true` por padrão (entidade + `V19`); testes e `docs/integrations/webmotors.md` 7.1

## 6. Logs sem PII (T1.5)
- [x] 6.1 `PiiMasker` com testes
- [x] 6.2 Converters Logback no console e redação no `ApplicationErrorWriter`; testes com log capturado
- [x] 6.3 Remover corpo/payload de terceiros dos logs (Meta, marketplace, Abacate Pay)
- [x] 6.4 Mascarar explicitamente os pontos conhecidos e tirar valor cru das exceções
- [x] 6.5 Teste de arquitetura que falha com `log.*` de PII sem `PiiMasker`

## 7. Remover o legado Webmotors (T1.6)
- [x] 7.1 Apagar endpoint, request, fetcher, auth service e testes do legado; `MarketplaceLeadConsumer` sobe só com o fetcher do ML (teste de contexto)
- [x] 7.2 `V20` removendo `wm_*` + entidade; propriedades e `.env.example` sem `marketplace.webmotors.*`
- [x] 7.3 Docs: `INTEGRACOES-MARKETPLACES-META.md`, `STATUS.md` (regra #3), `webmotors.md` seção 8

## 8. Verificação (T1.7)
- [x] 8.1 Suíte completa backend/frontend; greps (sem `hasAnyRole('ADMIN'`, sem import legado, sem legado Webmotors, sem log de PII); cadeia de migrations em Postgres limpo

## Workflow follow-up
- Arquivar a change (`/openspec-archive-change`) após a homologação (phase G11), pois a capability `webmotors-integration` ainda tem suposições H1–H13.
