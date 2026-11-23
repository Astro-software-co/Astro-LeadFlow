# Mensagens de WhatsApp — abordagem, consentimento, reabertura e descadastro

> Especificação funcional para a Fase 4 (blocos 4.C) de `PLANO-IMPLEMENTACAO.md`. Redigida em 2026-11-23 a partir das decisões do Ruy.
> **Textos sujeitos a revisão jurídica** (consentimento, base legal, finalidades) e à **aprovação da Meta** (templates). Onde dependem de validação externa há `[VALIDAR]`.

## 1. Visão do fluxo

```
Marketplace (lead) ──► [M1 Abordagem] ──► cliente responde? ──┬─ não: nada mais é enviado
   (card novo ou reaberto)                                     └─ sim ─► [M2 Consentimento]
                                                                          ├─ Aceito ───► [M3] ─► bot qualifica
                                                                          ├─ Não aceito ─► [M4] ─► bloqueio de contato
                                                                          └─ texto livre ─► [M6] (até 2x) ─► atendimento humano
Cliente escreve num card arquivado (≤ 3 meses) ──► reabre o card ──► [M7 Confirmação do veículo]
Em qualquer momento: "SAIR" ──► [M5] ──► bloqueio de contato
```

Regras que valem para todo o fluxo:
- **A mensagem de abordagem (M1) só sai em card novo ou reaberto**, nunca em novo interesse num card ativo (R3/R11 do plano).
- **O consentimento é por `(organização, telefone)`**, não por card: se o cliente já aceitou, não pedimos de novo em cards seguintes.
- **Telefone com status `DECLINED` ou `REVOKED` nunca recebe mensagem enviada pelo sistema** daquela organização (ver 5, "Limite do bloqueio").
- A resposta do cliente abre a **janela de 24 h** do WhatsApp: dentro dela M2, M3, M4, M5, M6 e M7 são mensagens **livres/interativas** e **não precisam de template aprovado**. Só M1 (fora da janela) é template.

## 2. M1 — Mensagem de abordagem (template)

| Campo | Valor |
|---|---|
| Nome | `lead_first_contact` (versão 1) |
| Idioma | `pt_BR` |
| Categoria solicitada | `UTILITY` — **[VALIDAR]** esperar que a Meta reclassifique como `MARKETING` (mais cara) |
| Corpo | `Olá, {{1}}! Aqui é o atendimento da {{2}}. Vimos que você se interessou pelo {{3}} anunciado no {{4}} e gostaríamos de ajudar com mais informações. Posso continuar por aqui?` |
| Rodapé | `Para não receber mais mensagens, responda SAIR.` |
| Botões (resposta rápida) | `Sim, pode continuar` · `Não tenho interesse` |

### Variáveis

| Var | Significado | Origem no sistema | Regra de montagem | Fallback | Exemplo (envio à Meta) |
|---|---|---|---|---|---|
| `{{1}}` | Primeiro nome do cliente | `lead.name` | primeiro token, capitalizado (`maria` → `Maria`) | `cliente` | `Carlos` |
| `{{2}}` | Nome da concessionária | `organization.name` | como cadastrado; máx. 60 caracteres | — (obrigatório) | `Auto Center Silva` |
| `{{3}}` | Veículo de interesse | snapshot do **interesse atual** (R5) | `"{marca} {modelo} {ano}"`; sem versão (muito longa); máx. 60 caracteres | `veículo anunciado` | `Hyundai HB20 2018` |
| `{{4}}` | Marketplace de origem | `marketplace_type` do interesse | `WEBMOTORS`→`Webmotors`, `MERCADO_LIVRE`→`Mercado Livre`, `OLX`→`OLX` | `portal de veículos` | `Webmotors` |

Restrições da Meta a respeitar na montagem **[VALIDAR]**: variável não pode ser o primeiro nem o último elemento do corpo; sem quebras de linha, tabulações nem 4+ espaços consecutivos dentro de uma variável; todos os parâmetros preenchidos; amostra de cada variável obrigatória na submissão.

### Mapeamento no código
- Propriedades: `whatsapp.templates.first-contact.name=lead_first_contact`, `…language=pt_BR`, e `…fallback-name=welcome_lead` (usado enquanto o novo template não estiver aprovado, ou em modo mock).
- `FirstContactMessageBuilder.build(lead, organization, interest): TemplateMessage(name, language, components)` produz os 4 parâmetros do corpo na ordem acima e aplica sanitização e fallbacks. Testes: nome ausente → `cliente`; veículo sem dados → `veículo anunciado`; marketplace desconhecido → `portal de veículos`; textos longos truncados em 60 caracteres sem quebrar palavra; remoção de quebras de linha.
- Tratamento do botão **"Não tenho interesse"**: interesse do card → `DESCARTADO`; envia cortesia (M8) e para a automação; o card segue o fluxo normal para o atendente decidir.

## 3. Mensagens livres/interativas (dentro da janela de 24 h)

| # | Quando | Tipo | Texto |
|---|---|---|---|
| **M2** | 1ª resposta do cliente e ainda **sem** consentimento `ACCEPTED` para o telefone | Interativa, botões `Aceito` / `Não aceito` | `Obrigado pelo retorno! Para continuarmos o seu atendimento, a {{loja}} precisa do seu aceite aos Termos e Condições e à Política de Privacidade: {{link}}. Você concorda?` |
| **M3** | Cliente tocou em `Aceito` | Texto | `Perfeito, obrigado! Vamos continuar o seu atendimento.` |
| **M4** | Cliente tocou em `Não aceito` | Texto | `Tudo bem, respeitamos a sua decisão. Não enviaremos mais mensagens. Se mudar de ideia, é só nos chamar.` |
| **M5** | Cliente enviou `SAIR` (ou sinônimo) | Texto | `Pronto, você não receberá mais mensagens da {{loja}}. Se quiser voltar a conversar, é só nos chamar.` |
| **M6** | Resposta ambígua à M2 | Interativa (repete M2 curto) | `Não entendi a sua resposta. Para continuar, toque em Aceito ou Não aceito.` |
| **M7** | Cliente escreve num card **arquivado** (≤ 3 meses) e o card é reaberto | Interativa, botões `Sim, esse mesmo` / `Outro veículo` | `Olá, {{nome}}! Que bom ter você de volta. Você quer retomar o atendimento sobre o {{veículo da etiqueta}}?` |
| **M8** | Botão `Não tenho interesse` da M1 | Texto | `Tudo bem! Se precisar de algo, é só chamar.` |
| **M9** | `Outro veículo` na M7 | Texto | `Sem problemas! Qual veículo você procura?` |

Variáveis das mensagens livres: `{{loja}}` = `organization.name`; `{{link}}` = política configurada pela loja (`privacy_policy_url`, opcional) ou, por padrão, `{SITE_URL}/privacidade` (Fase 3) **[VALIDAR com o jurídico se é necessário um termo por concessionária]**; `{{nome}}` = primeiro nome (fallback: omitir o nome e a vírgula); `{{veículo da etiqueta}}` = `"{marca} {modelo} {ano}"` do `closing_interest` do card arquivado.

Finalidades no texto do consentimento: o texto da M2 cobre **atendimento**. O Ruy quer manter dados de cards para **suporte pós-venda e futuras campanhas comerciais**; campanhas comerciais exigem **consentimento específico de marketing** e templates `MARKETING` — ver backlog B3 do plano. Até lá, o termo apontado pelo link deve **descrever** o uso pós-venda **[VALIDAR com o jurídico]**.

## 4. Regras do consentimento e do descadastro

| # | Regra |
|---|---|
| C1 | **Registro por `(organização, telefone)`** em `contact_consents`: `status` (`ACCEPTED`, `DECLINED`, `REVOKED`), `terms_version`, `terms_url`, `source` (`BUTTON` ou `TEXT`), `wa_message_id` da resposta, `responded_at`, `lead_id` de origem (anulável), `phone_hash` (HMAC; **não** guardar o telefone em claro nesta tabela). |
| C2 | **Prova preservada:** o registro de consentimento **não** é apagado pela retenção de 3 meses nem por exclusão manual do lead (mantém só hash e metadados); é removido apenas se o jurídico definir o contrário **[VALIDAR]**. |
| C3 | **Ao receber a 1ª resposta** de um telefone sem `ACCEPTED`: enviar M2 e **suspender o bot** (sem qualificação, sem handoff automático) até a resposta. A mensagem original do cliente permanece salva na conversa. |
| C4 | **Aceito** = toque no botão ou texto exato (sem acento/caixa) em `{aceito, concordo, sim}` → `ACCEPTED`, envia M3, **retoma o bot** do ponto em que parou. |
| C5 | **Não aceito** = botão ou `{nao aceito, não, nao}` → `DECLINED`, envia M4, interrompe toda automação, **arquiva o card** com o motivo "Cliente não aceitou os termos" e bloqueia novos envios (P10). |
| C6 | **Resposta ambígua:** M6 até **2** vezes; depois disso o bot **não** envia mais nada, o card vai para `WAITING_ATTENDANCE` e o responsável/`MANAGER` é notificado. |
| C7 | **Descadastro:** `SAIR`, `PARAR`, `CANCELAR`, `STOP` (sem acento/caixa, mensagem inteira) em **qualquer** momento → `REVOKED`, envia M5, bloqueia; vale para **todos** os cards do telefone na organização. Tem precedência sobre qualquer outro tratamento. |
| C8 | **Reabertura** de card de telefone com `ACCEPTED` **não** pede consentimento de novo (apenas M7). |
| C9 | **Cliente com `DECLINED`/`REVOKED` que volta pelo marketplace:** o card é criado normalmente (o lojista precisa **ver** o lead), com selo **"Não contatar"** e **sem** mensagem automática. |
| C10 | Versionamento: ao publicar nova versão dos termos, `ACCEPTED` de versões antigas continua válido para atendimento em curso; um novo aceite só é exigido para telefones sem `ACCEPTED` (política simples; **[VALIDAR]**). |

### Limite do bloqueio (importante)
O bloqueio vale para **mensagens enviadas pelo sistema** (template, bot, handoff). O vendedor ainda pode abrir o WhatsApp pelo botão **"Abrir no WhatsApp"** (a comunicação é log somente leitura, fora do sistema). Por isso a interface mostra o selo **"Não contatar"** e um **aviso de confirmação** ao clicar em "Abrir no WhatsApp" de um telefone bloqueado. O sistema não consegue impedir um contato feito fora dele.

## 5. Reabertura por mensagem do cliente (decisão P6)

1. Cliente escreve no WhatsApp; **não há card ativo** do telefone, mas há card **fechado há ≤ 90 dias**.
2. O sistema **reabre o card mais recente** (R10), usando como contexto o veículo da **etiqueta** (`closing_interest`) e dispara **M7** pedindo confirmação.
3. `Sim, esse mesmo` → segue o fluxo normal; o bot **revalida** os dados mutáveis (telefone, e-mail, valor de entrada, urgência, tipo de interesse, troca) e consolida com o que já havia.
4. `Outro veículo` → o interesse da etiqueta vira `DESCARTADO`, o bot envia **M9** e o texto livre da resposta cria um **novo interesse** (marketplace `WHATSAPP`, sem `listingId`, veículo descrito pelo cliente). O card **permanece o mesmo** (sem churn de cards).
5. Passados 90 dias do fechamento, não há reabertura: cria-se card novo e a etiqueta/conversa antiga já foi expirada.

## 6. Notas para implementação

- **Cliente da Meta:** hoje só existem `sendText` e `sendTemplate(name)` (sem `components`). É preciso: `sendTemplate` com `components` (parâmetros do corpo), **mensagem interativa com botões de resposta** e o **parsing de entrada** de `interactive.button_reply` e de `button` (resposta de botão de template) no webhook. Implementar **mock e real** (a Meta ainda é mock) e cobrir com testes de contrato quando houver acesso real (change `meta-whatsapp-real`).
- **Roteador de entrada** (ordem de precedência): (1) descadastro (C7) → (2) consentimento pendente (C3–C6) → (3) reabertura/confirmação (seção 5) → (4) bot.
- **Portão de saída:** `MessagingPolicy.canSend(org, phone, kind)` consultado por `MessageSenderService` e por qualquer envio do bot: `NO_RECORD` permite **apenas** M1; `ACCEPTED` permite tudo; `DECLINED`/`REVOKED` e lead anonimizado negam.
- **Feature flag:** `whatsapp.consent.enabled`. Só ligar em produção depois de as páginas `/privacidade` e `/termos` (Fase 3) estarem publicadas.
- **Telefone nos logs:** nenhum texto de mensagem nem telefone em log (T1.5).
