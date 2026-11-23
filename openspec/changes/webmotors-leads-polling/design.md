## Context
Ver proposal.md. Estado verificado no código (2026-11-23, após o merge com a `develop`):
- `User.getAuthorities()` concede `ROLE_MANAGER`/`ROLE_SELLER` **mais** `ROLE_DEALERSHIP`; `ASTRO_ADMIN` recebe só `ROLE_ASTRO_ADMIN`. `SecurityUtils.getCurrentOrganizationId()` lança `AccessDeniedException` (subclasse de `BusinessException` com 403).
- `LeadIngestionService` tem três caminhos (duplicado, SQS, local); `processFromQueue` descarta silenciosamente telefone já existente.
- `MessageSenderService.sendTemplate` já chama `usageLimitService.incrementUsage` após envio bem-sucedido.
- `DatabaseErrorAppender` grava `event.formattedMessage` e o stack trace via `ApplicationErrorWriter`, sem layout/encoder.
- `V19__webmotors_leads_polling.sql` nunca foi mergeada na `develop`.

## Goals / Non-Goals
**Goals:** porta única de entrada no CRM já com o comando completo da Fase 4; legado Webmotors removido; logs sem PII.
**Non-Goals:** regra Card + Interesses (phase G4); UI (G7); cifra compartilhada `FieldCipher` (G8); hardening do Mercado Livre (G9).

## Decisions
1. **`LeadIntakeCommand` e `VehicleSnapshot` em `crm/intake/`.** O comando é usado por `ingestion` e por `integrations/webmotors`; ficar em `crm` evita que `ingestion` dependa do módulo Webmotors. Inclui um campo `vehicleInterest` (texto livre) além do snapshot, porque o scraper só envia texto e o comportamento atual precisa ser preservado. `externalEventId` é anulável para o scraper (não tem id de evento); a Webmotors sempre preenche.
2. **`LeadIngestionService.ingestResolved(organization, command)`** contém a decisão duplicado/novo, a criação e o envio com o tratamento de erros atual de cada ramo. `ingestFromWebhook` mantém a ordem atual (duplicado → `ingestResolved` antes do SQS; novo → SQS ou `processFromQueue`); `processFromQueue` mantém o descarte silencioso de telefone já existente e delega o ramo novo. Quando `sendWelcome=false` o lead nasce em `WAITING_ATTENDANCE` (sem bot), como o adaptador fazia.
3. **Contagem de uso (T1.3.3):** no ramo de duplicado há incremento explícito **e** o incremento interno de `sendTemplate` → dupla contagem quando o envio dá certo; no ramo novo a contagem vem só de `sendTemplate`. O incremento explícito é fixado por testes existentes (`LeadIngestionServiceTest`: "deve incrementar usage ... ANTES de enviar"), portanto não é tratado como bug evidente: **documentado, não alterado**. A phase G4 elimina o ramo de duplicado (R14) e resolve a questão.
4. **Máscara de log em três camadas:** `PiiMasker` (funções puras + `redact(texto)` por regex); no console, `conversionRule` do Logback com `PiiMaskingConverter` (mensagem) e `PiiMaskingThrowableConverter` (stack trace); no banco, `ApplicationErrorWriter` aplica `PiiMasker.redact` à mensagem e ao stack trace (o appender não tem layout, então um converter não o alcança). Alternativa descartada: `TurboFilter` — não reescreve a mensagem.
5. **Teste de arquitetura** varre `src/main/kotlin` por chamadas `log.*(` cujos argumentos referenciem identificadores `phone|email|token|secret|password` sem `PiiMasker`.
6. **Migrations:** defaults (`AUTO`, `welcome_message_enabled=true`) editados direto na `V19` (não publicada na `develop`); remoção das colunas `wm_*` numa nova `V20`.
7. **RBAC:** `.requestMatchers("/api/v1/integrations/**").hasRole("MANAGER")` antes do bloco `DEALERSHIP`, e `@PreAuthorize("hasRole('MANAGER')")` no controller (dupla barreira, padrão de `ChatbotManagerController`).

## Risks / Trade-offs
- [Regex de telefone mascarar números que não são telefone (ids longos)] → regex exige formato BR (E.164 `+55` ou DDD + 8/9 dígitos com separadores típicos); UUIDs não casam.
- [Desenvolvedor local com a antiga `V5__webmotors_leads_polling` aplicada] → Flyway acusa divergência; recriar o banco local (sem dados reais, D6).
- [`com.astro.leadflow` tem `additivity=false` e só aponta para o console] → logs de ERROR da aplicação hoje não chegam ao `application_error` fora do `prod`. Fora do escopo; registrado como pendência.
