# Status do projeto — 2026-11-23

Fotografia mantida junto das specs. O comportamento implementado está em `openspec/specs/`; o trabalho em aberto está em `openspec/changes/` (uma change por frente). Diagramas e gaps detalhados: `docs/ESTADO-ATUAL-E-FLUXOS-2026-11-16.md`.

## Implementação

| Área | Estado | Onde |
|---|---|---|
| IAM / RBAC (Fase 14) | Implementado, testes verdes. Pendente: conferir no navegador o overflow da barra de abas | `specs/iam-rbac` |
| CRM, Kanban, estoque, dashboard | Implementado | — |
| Onboarding, referral, painel Astro Admin | Implementado | — |
| Billing (Abacate Pay) | Implementado, só testado com mocks | `specs/billing-abacatepay`, change `abacatepay-sandbox-validation` |
| Chatbot | Funcional em mock; slots gravam no Lead, handoff com histórico, atendimento 24h | `specs/chatbot-qualification`, change `lex-real-engine` |
| WhatsApp (Meta) | Mock; nunca validado contra a Meta real | `specs/whatsapp-communication`, change `meta-whatsapp-real` |
| Ingestão Webmotors / Mercado Livre | Código existe com falhas conhecidas | `specs/lead-ingestion`, change `ingestion-hardening` |
| E-mail em produção | Só MailHog | change `email-ses-prod` |
| CI/CD e deploy AWS | Scaffold em standby, nada executado | change `aws-deploy-activation` |
| Perfil `prod` | Criado, boot não testado | `backend/src/main/resources/application-prod.yml` |

Última verificação (2026-11-16): backend `./gradlew test` verde com JDK 21; frontend 213 testes, lint sem erros, typecheck limpo. Playwright E2E e `docker build` não foram rodados.

## Bloqueios (dependem de ação externa)

| Bloqueio | Destrava | Change |
|---|---|---|
| Meta real: verificação de negócio, número, token permanente, template `welcome_lead` aprovado | Ruy | `meta-whatsapp-real` |
| Webmotors: cadastro como CRM no Portal, push ou consulta, `idTipoLead`, ambiente de testes | Ruy (contato comercial) | `ingestion-hardening` |
| Mercado Livre: app em developers, redirect URI, tópico `vis_leads`, conta vendedora Motors | Ruy | `ingestion-hardening` |
| Concessionária piloto (usuário integrador Webmotors, OAuth do ML) | Ruy | `ingestion-hardening` |
| Conta e chave sandbox do Abacate Pay | Ruy | `abacatepay-sandbox-validation` |
| Conta AWS (Budget, OIDC, EC2, SES, Lex) | Ruy | `aws-deploy-activation`, `email-ses-prod`, `lex-real-engine` |

## Regras de negócio em aberto

Decidir antes de implementar a change indicada.

| # | Regra | Hoje | Change |
|---|---|---|---|
| 1 | Lead sem telefone: criar como pendente? Quem trata? | Descartado (vai à DLQ) | `ingestion-hardening` |
| 2 | Chave de duplicidade: só telefone ou telefone mais veículo? | Só telefone | `ingestion-hardening` |
| 3 | Webmotors entrega por push ou só por consulta? | Código assume push | `ingestion-hardening` |
| 4 | Horário comercial por organização e mensagem de handoff fora do horário | Fixo 9h–18h, sem aviso ao cliente | `lex-real-engine` |
| 5 | Quem absorve o custo das mensagens da Meta (limite do plano ou repasse)? `welcome_lead` como UTILITY ou MARKETING? | Indefinido | `meta-whatsapp-real` |
| 6 | O que acontece com a organização em `PAST_DUE` (manter, limitar, bloquear)? | Só muda o status | `abacatepay-sandbox-validation` |
| 7 | Fim do trial de 14 dias sem plano pago; o plano `free` deve existir? | `DEFAULT_PLAN=free` não está nos seeds | `abacatepay-sandbox-validation` |
| 8 | Vender planos Profissional e Enterprise antes de o Lex e a IA estarem ativos? | Descrições prometem chatbot e IA | `abacatepay-sandbox-validation` |
| 9 | Vendedor possui código de indicação, mas a API é só do MANAGER: ele deve ver o próprio código? | Não vê | — |
| 10 | Cota de IA do Enterprise (500 mil tokens/mês) esgotada | Cai para o template | `lex-real-engine` |
| 11 | Responder dentro do chat de cada marketplace | Fora do MVP; a conversa vai para o WhatsApp | — |
| 12 | OLX | Fora do MVP (existe só o enum) | — |

## Decisões já tomadas (não reabrir sem motivo)

- Comunicação é log somente leitura com "Abrir no WhatsApp".
- Cadastro público removido; onboarding por convite ou administrador.
- Lead duplicado é marcado, mas ainda recebe a mensagem.
- Reset de uso segue `renewsAt`, não o mês-calendário.
- Plano muda só depois do webhook de pagamento confirmado.
- Referral restrito a `MANAGER` (2026-11-16).
- O bot qualifica 24 horas por dia; horário não dispara handoff por si só (2026-11-16).
- Sonner é o único sistema de toast.

## Como usar o OpenSpec aqui

- Nova frente: `/openspec-propose "ideia"` cria a change com proposta, design, specs e tarefas.
- Implementar: `/openspec-apply-change`. Ao concluir: `/openspec-archive-change`, que funde os deltas em `openspec/specs/`.
- Validar: `npx @fission-ai/openspec validate --all`.
- O GSD continua sendo o fluxo de fases; o OpenSpec registra specs e mudanças. Ao fechar uma change, atualize esta tabela.
