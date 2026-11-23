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
- O adaptador do módulo contradiz a decisão de produto sobre duplicidade (marcar `isDuplicate=true` e ainda enviar mensagem); será corrigido.
- Credenciais de marketplace e o token do WhatsApp estão em **texto puro** no banco.
- Telefones aparecem em logs sem máscara.
- Os commits do módulo já estão no GitHub (branch `claude/marketplace-integrations-ynztxc`).

## 2. O que vamos implementar (9 fases, em `PLANO-IMPLEMENTACAO.md`)

| Fase | Entrega | Esforço* | Bloqueia o formulário? |
|---|---|---|---|
| F0 | Consolidar branch com a `develop`, renumerar migration, suíte verde | 0,5d | Pré-requisito |
| F1 | Alinhar o módulo Webmotors (convenções, RBAC real, duplicidade oficial, máscara de PII, desativar legado) | 2d | Não (credibilidade técnica) |
| **F2** | **LGPD:** registro de solicitações com SLA, anonimização completa, bloqueio de contato, retenção, runbook | 3–4d | **Sim** |
| **F3** | **Páginas legais, dados institucionais, rascunho do formulário** | 1d + insumos | **Sim** |
| F4 | Aba "Integrações" no CRM (conta, propostas, aceitar/recusar) | 2–3d | Não |
| F5 | Credenciais cifradas em repouso + fail-fast em produção | 2d | Não (segurança) |
| F6 | Hardening da ingestão (ML sem AWS, HMAC/validação, lock de token, lead sem telefone) | 3d | Não |
| F7 | Métricas, alertas e runbook da integração | 1d | Não |
| F8 | Pré-deploy: perfil `prod`, variáveis, e-mail SES, smoke test | 2d | Parcial (e-mail de suporte/privacidade) |
| F9 | Homologação Webmotors e remoção do legado | 1–2d | Após o acesso |

*Ordem de grandeza para um desenvolvedor sênior ou agente de alta capacidade; não é compromisso.

**Caminho crítico até enviar o formulário:** F0 → F2 → F3 + insumos do Ruy.

## 3. Em aberto (funcional)

**Premissas assumidas no plano — valide ou corrija (seção 1.3 do plano):**
1. **Webmotors só por polling** (código antigo desativado). *Confirmado por você.*
2. **Aceite automático por padrão**, manual configurável por loja.
3. **Duplicidade:** segue a regra vigente (marca e ainda envia mensagem); **lead sem telefone fica pendente**, nunca é descartado.
4. **Mensagem de boas-vindas ligada por padrão**, desligável por loja — *precisa de validação jurídica da base legal*.
5. **LGPD:** registro + anonimização + página pública + e-mail de privacidade; **sem portal self-service**.
6. **Banco:** Postgres no Compose com backup em S3 para staging; RDS só em produção.

**Regras de negócio que seguem abertas** (`openspec/STATUS.md`): horário comercial por organização (#4); quem paga as mensagens da Meta e classificação do template (#5); `PAST_DUE` e fim do trial (#6, #7); vender planos com IA antes do Lex (#8); cota de IA esgotada (#10).

**Desconhecidos técnicos da Webmotors** (resolvidos só na homologação — H3–H12 do plano): nome da paginação, formato e máscara do telefone antes do aceite, significado de `tipoLead`, rate limit, URLs de produção, e o que "aceitar" significa comercialmente.

**Mercado Livre:** pesquisa em andamento (você); sem módulo novo até lá.

## 4. Próximos passos

**Você (Ruy), em paralelo ao código**
1. Conta AWS: Budget, OIDC, EC2/SSM, SES, DNS (hoje).
2. Definir **domínio** e criar `suporte@` e `privacidade@`.
3. Reunir: razão social, CNPJ, contato do solicitante, regiões, logo PNG, concessionária piloto.
4. Registrar o app no Portal do Desenvolvedor da Webmotors.
5. Responder as premissas da seção 3 (ou aprovar as sugeridas).
6. Passar a política de privacidade e a base legal do WhatsApp pelo jurídico.

**Execução com o CLI** (um comando por fase; cole o *prompt mestre* da seção 0 do plano)
1. F0 imediatamente (30 min–meio dia).
2. F2 + F3 em seguida; ao concluir, **enviar o formulário**.
3. F1, F4, F5 em paralelo/sequência; F6–F8 conforme a AWS ficar pronta.
4. F9 quando a Webmotors liberar o acesso.

## 5. Riscos principais

| Risco | Mitigação |
|---|---|
| Contrato real da Webmotors diferir da collection | Campos anuláveis, testes de contrato na F9, legado só removido depois |
| Anonimização esquecer uma tabela com dado pessoal | Mapa de PII + teste que varre colunas |
| Perda da chave de cifra | Cofre (SSM), rotação documentada |
| Meta, Lex e Abacate Pay seguirem só em mock no deploy | Registrar como "não validado" no STATUS; não vender como pronto |
| Prazo da Webmotors (sem processo público de aprovação) | Enviar o formulário o quanto antes; F1/F4 já deixam o caminho pavimentado |

## 6. Documentos

- Plano executável: `docs/plano/PLANO-IMPLEMENTACAO.md`
- Rascunho do formulário: `docs/plano/FORMULARIO-WEBMOTORS.md`
- Pesquisa e gaps Webmotors: `docs/integrations/webmotors.md`
- Estado do produto e regras abertas: `openspec/STATUS.md`
