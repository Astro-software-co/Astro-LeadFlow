# Runbook — Privacidade e solicitações de titulares (LGPD)

> **Revisar com advogado antes de publicar.** Textos e prazos seguem a LGPD (art. 18 e 19), mas não são parecer jurídico.

## Papéis
- **Concessionária = controladora.** Responde ao titular, valida a identidade dele e decide o que atender.
- **Astro = operadora.** Fornece a ferramenta (Configurações → Privacidade) e executa o que a controladora mandar.

## Fluxo
1. **Receber o pedido.** Por qualquer canal da loja (e-mail de privacidade, telefone, presencial). A controladora confirma a identidade do titular **antes** de registrar (documento, retorno pelo mesmo canal etc.).
2. **Registrar.** Configurações → Privacidade → *Nova solicitação*: tipo, telefone ou e-mail do titular e canal. O sistema **não guarda** o identificador: só um HMAC (`privacy.hash-secret`) e uma dica mascarada (`+55******4321`).
3. **Prazo.** `due_at = recebimento + privacy.sla-days` (padrão **15 dias corridos**). Pedidos abertos e vencidos aparecem com o selo **Atrasada**.
4. **Executar.** *Executar* exige o identificador de novo (conferido contra o hash):
   - **Anonimização:** todos os leads da organização com aquele telefone/e-mail ficam com nome "Titular anonimizado", telefone `ANON-xxxxxxxx`, sem e-mail e sem notas; mensagens viram `[mensagem removida]`; dados coletados pelo bot, payloads de marketplace e da caixa Webmotors são limpos; o identificador é redigido nos erros persistidos (`application_error`). **Estágio, valores e métricas ficam.** O contato não recebe mais mensagens do sistema.
   - **Exclusão:** remove os cards do titular e tudo que depende deles (mensagens, sessões do bot, propostas Webmotors, histórico). Eventos de marketplace ficam sem vínculo e sem payload. Registra auditoria sem PII (`lead_deletion_log`).
   - **Acesso / Correção:** atendidos pela controladora fora do sistema (exportação, edição do lead); ao concluir, descrever o que foi feito.
5. **Rejeitar.** Com motivo (ex.: identidade não confirmada). O motivo fica na solicitação.

Atalhos no detalhe do lead (só `MANAGER`): **Anonimizar** (cria e executa a solicitação com o telefone do lead) e **Excluir lead** (com a opção de excluir os outros atendimentos do mesmo telefone).

## O que não é apagado
- Agregados e métricas (contagens, receita, estágios).
- O registro da solicitação (hash, dica, datas, resumo) e o log de exclusão (hash, contagens, motivo) — prova de atendimento, sem dado pessoal.
- Consentimentos de WhatsApp (phase G6) ficam **só com hash** para que um telefone descadastrado continue bloqueado (R20).

## Retenção automática (`privacy.retention.enabled`, ligado em `prod`)
| Dado | Prazo | Propriedade |
|---|---|---|
| `raw_payload` de eventos de marketplace e da caixa Webmotors | 90 dias | `privacy.retention.webhook-payload-days` |
| Erros persistidos (`application_error`) | 90 dias | `app.monitoring.error-retention-days` |
| Conteúdo de conversa de card fechado | 90 dias após o fechamento | phase G5 (`ConversationRetentionJob`) |

O job roda diariamente às 03:30 (`privacy.retention.cron`).

## Backups
A anonimização e a exclusão **não retroagem** a cópias de segurança. Os dados continuam nos backups até eles expirarem. Janela de retenção dos backups do Postgres: **[PREENCHER — definir com o Ruy na change `aws-deploy-activation`]**. Se um backup for restaurado, reexecutar as solicitações concluídas depois da data do backup (a lista fica em Configurações → Privacidade).

## Operação
- `PRIVACY_HASH_SECRET` é obrigatório em `prod` (sem ele a anonimização falha). Trocar o segredo invalida a conferência de solicitações abertas: só trocar sem pedidos em aberto.
- Endpoints: `docs/API.md` (seção Privacidade).
