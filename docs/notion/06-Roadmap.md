# Roadmap e Próximos Passos

## Estado Atual (2026-04-20)

O milestone v1.1 está tecnicamente completo. Todos os 65 requisitos mapeados foram entregues. O branch `develop` está verde em lint, testes unitários (92% cobertura) e E2E Playwright.

**Pendências de governança abertas:**

1. Fechar formalmente Phase 12: commit → `/gsd:verify-work 12` → `/gsd:ship 12`
2. Executar `/gsd:complete-milestone v1.1`
3. Publicar `docs/notion/` no workspace Notion da equipe
4. Decidir oficialmente se `reload = relogin` é aceito ou se persistência via cookie já é o comportamento v1.1 (foi implementado em `d269315` — o README já documenta como comportamento atual)

---

## Perspectiva de Negócio

### O que o produto entrega hoje

- **Centralização de leads:** Webmotors e Mercado Livre entram automaticamente via webhook. WhatsApp via Meta API.
- **Pipeline visual:** Kanban com arrastar e soltar, 5 etapas de `TALKING_TO_BOT` a `COMPLETED`.
- **Atendimento rastreável:** Toda movimentação de lead gera histórico. Vendedor assume ou é atribuído.
- **Visibilidade operacional:** Dashboard com KPIs de conversão, tempo de resposta e receita.
- **Gestão de estoque:** Catálogo de veículos associável a leads.
- **Controle de assinatura:** Planos, cotas por marketplace, histórico de cobranças.
- **Gestão de equipe:** Convite por admin, RBAC (ADMIN / MANAGER / SELLER), 2FA.

### O que o produto ainda não entrega

| Lacuna | Impacto | Milestone alvo |
|---|---|---|
| Bot de atendimento automático (Lex + Bedrock) | Alto — reduz tempo de resposta de horas para segundos | v1.2 |
| Notificações em tempo real (WebSocket) | Médio — vendedor precisa recarregar para ver novos leads | v1.2 |
| Envio real de WhatsApp pelo frontend | Médio — hoje só visualiza histórico | v1.2 |
| Integração OLX | Médio — marketplace relevante, enum já existe no backend | v1.3 |
| Relatórios exportáveis (PDF/CSV) | Médio — demanda frequente de gestores | v1.3 |
| Pagamento in-app (Stripe / MercadoPago) | Alto para self-service | v2.0 |
| App mobile nativo | Alto para vendedores em campo | v2.0 |
| i18n (inglês / espanhol) | Baixo para MVP nacional | Futuro |

---

## Perspectiva Técnica — Próximos Passos

### v1.2 — Automação e Tempo Real

#### Chatbot de Atendimento (AWS Lex + Amazon Bedrock)

Este é o próximo grande diferencial competitivo do produto. O fluxo atual coloca o lead em `TALKING_TO_BOT` mas nenhum bot real existe — o avanço é manual.

**Arquitetura proposta:**

```
Lead entra via webhook
       ↓
Backend: cria lead em TALKING_TO_BOT
       ↓
Chatbot Service (novo módulo backend)
       ↓
AWS Lex → gerencia diálogo (intents: saudação, interesse, qualificação)
       ↓
Amazon Bedrock (Claude / Titan) → geração de resposta contextualizada
       ↓
Meta API → envia mensagem WhatsApp ao lead
       ↓
Condição de saída: lead qualificado → WAITING_ATTENDANCE
                   lead desistente → ARCHIVED
```

**Regras de negócio críticas:**

- Bot ativo apenas para organizações com plano que inclui o módulo de automação.
- Configuração por organização: ativar/desativar, tom de voz, modelo Bedrock selecionado.
- Limite de turnos configurável (ex.: máx. 5 interações antes de escalar para vendedor).
- Fallback: se Bedrock falhar, resposta de template fixo ou escalonamento imediato.
- Logs completos de cada interação armazenados como atividades do lead.

**Módulos novos no backend:**

```
com.astro.leadflow.shared.modules.chatbot
├── domain/
│   ├── BotSession.kt           estado da conversa por lead
│   ├── BotConfig.kt            configuração por organização
│   └── BotSessionRepository.kt
├── service/
│   ├── ChatbotOrchestrationService.kt
│   ├── LexDialogService.kt     integração AWS Lex V2
│   └── BedrockGenerationService.kt   integração Amazon Bedrock
└── api/
    └── BotConfigController.kt  CRUD de configuração (ADMIN)
```

**Frontend — novas telas:**

- Aba "Bot" em Configurações (somente se plano inclui automação).
- Indicador visual no card do Kanban quando lead está em atendimento pelo bot.
- Histórico de sessão do bot no timeline de atividades do lead.

**Dependência de assinatura:**

O módulo de bot deve ser verificado no `SubscriptionService` antes de cada execução. Organizações sem o módulo habilitado não iniciam sessão de bot — o lead avança diretamente para `WAITING_ATTENDANCE`.

#### Notificações em Tempo Real (WebSocket / SSE)

- Backend: Spring WebSocket ou Server-Sent Events para push de eventos de lead.
- Frontend: hook `useLeadNotifications` com toast ao receber novo lead.
- Prioridade: novos leads e leads sem atendimento por X minutos.

#### Envio de WhatsApp pelo Frontend

- Hoje: `POST /whatsapp/send` existe no backend mas o frontend só exibe histórico.
- Próximo: interface de composição com seleção de template + variáveis preenchíveis.
- Deve respeitar cota de uso da assinatura antes de permitir envio.

---

### v1.3 — Expansão de Canais e Relatórios

#### Integração OLX

- O enum `OLX` já existe no backend (fora do MVP por decisão explícita).
- Requer: webhook endpoint + mapeamento de payload OLX → LeadDTO + testes.
- Frontend: sem mudança estrutural — lead aparece com `source: OLX`.

#### Relatórios Exportáveis

- Dashboard com filtro de período e exportação para PDF / CSV.
- Relatório de funil por marketplace, por vendedor, por período.
- Backend: endpoint de agregação `/analytics/reports` com parâmetros de filtro.

#### Melhorias de UX Pendentes

| Item | Localização | Esforço |
|---|---|---|
| Redirect 401 via React Router (sem `window.location.href`) | `frontend/src/lib/api.ts` | Baixo |
| Endpoint dedicado para filtro `assignedTo` | Backend | Baixo |
| Remover `use-toast.ts` legado | Frontend | Baixo |
| Persistência de filtros de leads na URL (query params) | Frontend | Médio |
| Paginação infinita no Kanban (muitos leads por coluna) | Frontend | Médio |

---

### v2.0 — Produto Self-Service

#### Pagamento In-App

- Integração com Stripe (internacional) ou MercadoPago (Brasil).
- Fluxo: seleção de plano → checkout → ativação automática da assinatura.
- Backend: webhooks de pagamento + atualização automática de `SubscriptionStatus`.

#### App Mobile

- React Native com compartilhamento de types e serviços com o frontend web.
- Foco inicial: lista de leads + kanban + assumir lead + enviar WhatsApp.
- Push notifications nativas para novos leads.

---

## Débitos Técnicos Priorizados

| Item | Impacto | Esforço |
|---|---|---|
| TypeScript `strict: true` no frontend | Qualidade / segurança de tipo | Médio |
| Tailwind CSS v4 (quando shadcn/ui suportar) | Performance de build | Baixo |
| Lazy loading de imagens no catálogo de veículos | Performance | Baixo |
| Expandir cobertura E2E: fluxo de settings, vehicles detail | Confiabilidade | Médio |
| Adicionar Husky + lint-staged no pre-commit | DX | Baixo |

---

## Páginas Relacionadas

- [Overview](./01-Overview.md)
- [Architecture](./02-Architecture.md)
- [API Reference](./05-API-Reference.md)
