# Resumo executivo — Astro LeadFlow · 2026-11-23

**Objetivo imediato:** ficar apto a solicitar acesso às APIs da Webmotors e do Mercado Livre e colocar o produto no ar na AWS, sem retrabalho depois da homologação.

## 1. Onde estamos

| Frente | Situação | Evidência |
|---|---|---|
| CRM, Kanban, estoque, dashboard, onboarding, referral, RBAC | **Implementado** e testado | `openspec/STATUS.md`; backend verde, frontend 213 testes |
| Webmotors (API de Consulta de Leads) | **Módulo novo implementado**: login, polling, caixa de entrada, aceite/recusa, credenciais cifradas, 47 testes, migration validada em Postgres 16. **Não testado contra a Webmotors real** (sem acesso) | branch `claude/marketplace-integrations-ynztxc` |
| Webmotors (código antigo no `ingestion`) | Assume push/webhook e um contrato que **não** bate com a collection; será desativado | pesquisa 16/11 (gap #3) + collection |
| Mercado Livre | Código existe com falhas conhecidas (webhook sem fila, token rotativo sem lock, lead sem telefone descartado) | change `ingestion-hardening` |
| WhatsApp (Meta), Lex (chatbot), Abacate Pay | Funcionam em **mock**; nunca validados contra o serviço real | `openspec/STATUS.md` |
| CI/CD e AWS | Workflows e compose prontos em standby; **conta AWS sendo criada hoje** (Ruy) | `.github/`, `infra/` |
| LGPD / privacidade | **Quase inexistente**: só `DELETE /leads/{id}` e link "Privacidade" vazio no rodapé | varredura do código |
| Hexagonal | **Fora de escopo** por decisão: pouco ganho diante das pendências | decisão do usuário |

**Pontos de atenção descobertos agora**
- A branch estava defasada: a `develop` já avançou 35 commits e tem migrations até **V18**; a do módulo Webmotors precisa virar **V19**.
- A regra `isDuplicate` (duplicado vira outro card e recebe outra mensagem) será **substituída** pelo modelo Card + Interesses (F4); até lá o módulo usa o caminho oficial de ingestão (F1/T1.3).
- Credenciais de marketplace, token do WhatsApp e **API keys das organizações** estão em texto puro no banco (F6).
- Logs de **ERROR são gravados em tabela**: PII que vaze em log vira dado persistido (F1/T1.5).
- O `DELETE /leads/{id}` provavelmente **falha** para leads vindos de marketplace (FK sem cascata) — corrigido na F2/T2.0.
- Telefones aparecem em logs sem máscara, e alguns clientes HTTP logam o corpo das respostas.
- Os commits do módulo já estão no GitHub (branch `claude/marketplace-integrations-ynztxc`).

## 2. O que vamos implementar (11 fases, em `PLANO-IMPLEMENTACAO.md` v2)

| Fase | Entrega | Esforço* | Bloqueia o formulário? |
|---|---|---|---|
| F0 | Consolidar branch com a `develop`, renumerar migration, suíte verde | 0,5d | Pré-requisito |
| F1 | Alinhar o módulo Webmotors (convenções, RBAC real), **remover o código legado Webmotors**, **máscara de PII/segredos em log** (inclui logs gravados no banco) | 2,5d | Não (credibilidade técnica) |
| **F2** | **LGPD:** registro de solicitações com SLA, anonimização completa, bloqueio de contato, retenção de payloads, correção do `DELETE` de lead | 3–4d | **Sim** |
| **F3** | **Páginas legais, dados institucionais, rascunho do formulário** | 1d + insumos | **Sim** |
| F4 | **Modelo Card + Interesses**: um card ativo por cliente, vários anúncios por card, reabertura em 3 meses, etiquetagem no fechamento, retenção da conversa, notificações | 5–7d | Não |
| F5 | UI: aba Integrações (Webmotors) + painel de interesses, chip no Kanban, notificações | 3–4d | Não |
| F6 | **Segredos em repouso:** tokens/credenciais cifrados, API keys hasheadas, fail-fast em produção | 3d | Não (segurança) |
| F7 | Hardening da ingestão (ML sem AWS, validação de webhook, lock de token, lead sem telefone) | 3d | Não |
| F8 | Métricas, alertas e runbook das integrações | 1d | Não |
| F9 | Pré-deploy: perfil `prod`, variáveis, e-mail SES, smoke test | 2d | Parcial (e-mail de suporte/privacidade) |
| F10 | Homologação Webmotors | 1–2d | Após o acesso |

*Ordem de grandeza para um desenvolvedor sênior ou agente de alta capacidade; não é compromisso.

**Caminho crítico até enviar o formulário:** F0 → F2 → F3 + insumos do Ruy.

## 3. Decisões e pendências

**Decididas (2026-11-23):**
- Webmotors só por polling; **código legado removido**.
- **Card + Interesses:** um card ativo por telefone; interesses por anúncio com status `CONSIDERANDO | DESCARTADO | ESCOLHIDO`; conversa salva de forma generalista e **etiquetada no fechamento** com o interesse mais recente; **reabre** em até 3 meses se for o mesmo anúncio, senão card novo; conteúdo da conversa removido 3 meses após o fechamento; novo interesse em card ativo notifica o vendedor (se assumido) ou o dono da loja (se em aberto/com bot); um vendedor por card.
- Hexagonal fora de escopo; Mercado Livre sem módulo novo agora.

**Em discussão (padrão assumido no plano até fechar):**
| # | Tema | Padrão assumido |
|---|---|---|
| P1 | Lead sem telefone | Fica pendente, nunca descartado |
| P2 | Aceite na Webmotors | Automático por padrão; manual configurável por loja |
| P3 | Mensagem de boas-vindas | Ligada por padrão, desligável por loja; validar base legal |
| P4 | LGPD | Registro + anonimização + página pública + e-mail; sem portal self-service |
| P5 | Dados pessoais de card fechado após 3 meses | Só o conteúdo da conversa é apagado; nome/telefone/e-mail ficam (revisar com jurídico) |
| P6 | Cliente responde no WhatsApp a card arquivado | Reabre o card fechado mais recente (≤ 3 meses) |
| P7 | Concluir card com vários interesses | O interesse atual vira `ESCOLHIDO` automaticamente; gerente pode trocar |
| P8 | Estágio ao reabrir | `TALKING_TO_BOT` (bot revalida os dados) |

**Regras de negócio que seguem abertas** (`openspec/STATUS.md`): horário comercial por organização (#4); quem paga as mensagens da Meta e classificação do template (#5); `PAST_DUE` e fim do trial (#6, #7); vender planos com IA antes do Lex (#8); cota de IA esgotada (#10). As regras #1 e #2 (sem telefone e duplicidade) passam a ser tratadas pela Fase 4 e por P1.

**Desconhecidos técnicos da Webmotors** (resolvidos só na homologação — H3–H13): nome da paginação, formato e máscara do telefone antes do aceite, significado de `tipoLead`, rate limit, URLs de produção, significado comercial de "aceitar" e estabilidade do `anuncio.id`.

**Mercado Livre:** pesquisa em andamento (Ruy); sem módulo novo até lá.

## 4. Próximos passos

**Você (Ruy), em paralelo ao código**
1. Conta AWS: Budget, OIDC, EC2/SSM, SES, DNS, **SSM Parameter Store** (chaves de cifra).
2. Definir **domínio** e criar `suporte@` e `privacidade@`.
3. Reunir: razão social, CNPJ, contato do solicitante, regiões, logo PNG, concessionária piloto.
4. Registrar o app no Portal do Desenvolvedor da Webmotors.
5. **Fechar P1–P8** (ou aprovar os padrões).
6. Jurídico: política de privacidade, base legal do WhatsApp, retenção de dados de cards fechados.
7. Meta: template `welcome_lead` **com variável do veículo**.

**Execução com o CLI** (um comando por fase; cole o *prompt mestre* da seção 0 do plano)
1. F0 imediatamente. 2. F2 + F3; ao concluir, **enviar o formulário**. 3. F1, F4, F5, F6 em sequência. 4. F7–F9 conforme a AWS ficar pronta. 5. F10 quando a Webmotors liberar o acesso.

## 5. Riscos principais

| Risco | Mitigação |
|---|---|
| Contrato real da Webmotors diferir da collection | Campos anuláveis, testes de contrato na F9, legado só removido depois |
| Anonimização esquecer uma tabela com dado pessoal (inclui logs de erro gravados no banco e tabelas novas) | Mapa de PII + máscara global de log (F1) + teste que varre colunas |
| Regra Card+Interesses mexer em fluxo central e gerar 2 cards ativos por corrida | Índice parcial único + teste de concorrência + flag para ligar/desligar |
| Perda da chave de cifra | Cofre (SSM), `keyId` no prefixo, rotação documentada |
| Meta, Lex e Abacate Pay seguirem só em mock no deploy | Registrar como "não validado" no STATUS; não vender como pronto |
| Prazo da Webmotors (sem processo público de aprovação) | Enviar o formulário o quanto antes; F1/F4 já deixam o caminho pavimentado |

## 6. Documentos

- Plano executável: `docs/plano/PLANO-IMPLEMENTACAO.md`
- Rascunho do formulário: `docs/plano/FORMULARIO-WEBMOTORS.md`
- Pesquisa e gaps Webmotors: `docs/integrations/webmotors.md`
- Estado do produto e regras abertas: `openspec/STATUS.md`
