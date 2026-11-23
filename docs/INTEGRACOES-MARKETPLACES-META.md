# Integrações: Webmotors, Mercado Livre e WhatsApp (Meta)

> Pesquisa de 2026-11-16, escopo MVP. Cada fato tem a fonte. **[NC]** = não consegui confirmar nas fontes acessíveis (muita documentação fica atrás de login ou desatualizada). OLX: fora do MVP; o enum `MarketplaceType.OLX` existe, mas não há fetcher. Ficou para depois da pesquisa.

## 1. Webmotors

| Tema | O que encontrei | Fonte |
|---|---|---|
| Quem acessa | O app se registra no Portal do Desenvolvedor (Sensedia) e recebe Client ID/Secret. O **lojista** cria no Cockpit CRM um usuário com perfil "Integrador de API" (um por loja) e entrega e-mail e senha ao parceiro. | [Autenticação](https://portal-webmotors.sensedia.com/api-portal/documentacao/autenticacao), [Inclusão de Leads](https://portal-webmotors.sensedia.com/api-portal/documentacao/insercao-de-leads) |
| Autenticação | Um nível de autenticação de aplicação: Basic (base64 de client_id:secret), depois usuário/senha do integrador para obter o access token. | [Autenticação](https://portal-webmotors.sensedia.com/api-portal/documentacao/autenticacao) |
| APIs de lead | Existem a **Webmotors Leads API** (consulta, Rev 41) e a API de Inclusão de Leads (sentido contrário, Rev 27). A consulta devolve só leads do Grupo Webmotors, não os de outros canais. | [API Browser](https://portal-webmotors.sensedia.com/api-portal/api-browser) |
| CRM Terceiro | O lojista ativa a integração em Cockpit > CRM > Integrações, com autorização LGPD. Exige plano Controle, Performance ou Assinatura de Motos e anúncios ativos. Não exige produto adicional. | [Cockpit: produtos](https://ajuda.cockpit.com.br/hc/pt-br/articles/5028037000084-%C3%89-preciso-contratar-algum-produto-Webmotors-para-ter-acesso-aos-canais-integrados-via-APIs), [CRM Terceiro](https://ajuda.cockpit.com.br/hc/pt-br/articles/5027739995412-Quais-s%C3%A3o-as-APIs-dispon%C3%ADveis-no-Portal-do-Desenvolvedor-e-para-que-servem) |
| Webhook x polling | **[NC]** Não achei documentação pública de webhook de leads. O que aparece é uma API de **consulta** (pull). | idem |
| `leadTypeId` | **[NC]** A lista de tipos está atrás do login do portal. O código assume 1, 2, 3, 4, 11 e 12. | — |
| API de chat/mensagens | **[NC]** Nada encontrado. | — |
| Prazo de aprovação | **[NC]** Não há processo público. Integradores existentes (Revenda Mais, Zenvia) usam o fluxo acima. | [Revenda Mais](https://ajuda.revendamais.com.br/hc/rmais-ajuda/articles/1755104836-configuracao-crm-_-webmotors) |

**Leitura prática:** o acesso passa pelo **lojista** (usuário integrador + plano elegível), e o Astro precisa ser aceito como CRM no Portal. O contato comercial com a Webmotors é o caminho mais provável. Isso precisa ser confirmado com eles.

## 2. Mercado Livre

| Tema | O que encontrei | Fonte |
|---|---|---|
| App | Cria-se uma aplicação em developers.mercadolivre.com.br e obtém App ID e Client Secret. | [Criar aplicação](https://developers.mercadolivre.com.br/en_us/register-your-application) |
| OAuth | Authorization code. O scope `offline_access` dá refresh token. Access token dura 6 h (o exemplo em inglês mostra 10800 s, o em português 21600 s; validar `expires_in` real). O **refresh token é de uso único e rotativo** (cada renovação devolve um novo; só o último vale) e expira em 6 meses. `invalid_grant` indica token vencido ou já usado. | [Auth](https://developers.mercadolivre.com.br/en_us/authentication-and-authorization), [PT](https://developers.mercadolivre.com.br/pt_br/autenticacao-e-autorizacao) |
| Leads de veículos | Tópico de notificação `vis_leads`; a notificação traz `resource` (`/vis/leads/{id}`) e depois se faz GET. `/vis/leads/{id}` vale para `whatsapp`, `call`, `question`, `visit_request`; `contact_request` e reserva usam `/leads/{id}/details`. Só categoria Motors (veículos). Listagem: `/vis/users/{user_id}/leads/buyers` com `contact_types`, `date_from`, `date_to`. Campos: id, item_id, name, email, phone, `leads[]` (contact_type, created_at, status). | [Pessoas interessadas](https://developers.mercadolivre.com.br/pt_br/pessoas-interessadas), [Notificações](https://developers.mercadolivre.com.br/pt_br/produto-receba-notificacoes) |
| Recomendação VIS | A doc recomenda ativar **somente** o tópico VIS Leads nas integrações VIS, por causa de um problema conhecido. | [Notificações](https://developers.mercadolivre.com.br/pt_br/produto-receba-notificacoes) |
| Entrega da notificação | POST na callback URL. Responder HTTP 200 em até **500 ms**, senão o tópico pode ser desativado. Reentregas por até 1 h; depois a mensagem é descartada. Recomenda-se responder 200 e usar fila. | [Notificações (EN)](https://developers.mercadolivre.com.br/en_us/products-receive-notifications) |
| Autenticidade do webhook | A página de segurança menciona validar assinatura HMAC-SHA256 com chave secreta. **[NC]** nome do header e composição da assinatura. **[NC]** lista de IPs de origem. | [Segurança](https://developers.mercadolivre.com.br/pt_br/seguranca-desenvolvimento-seguro) |
| Perguntas | Existe API de perguntas e respostas (limite de 2.000 caracteres) com notificações em tempo real. | [Perguntas](https://developers.mercadolivre.com.br/pt_br/perguntas-e-respostas), [Veículos: contatos](https://developers.mercadolivre.com.br/pt_ar/automovel-gerenciamento-de-contatos) |
| Mensagens pós-venda | O vendedor **não** pode iniciar a conversa; sempre parte do comprador. Isso é pós-venda, não se aplica a lead de anúncio. | [Pós-venda](https://developers.mercadolivre.com.br/pt_br/mensagens-post-venda) |
| Rate limit | Documentado 500 rpm para GET e 500 rpm para escrita em pós-venda; outros recursos têm tetos menores. **[NC]** limite específico de `/vis/*`. | [Pós-venda](https://developers.mercadolivre.com.br/pt_br/mensagens-post-venda) |
| Certificação / exigências Veículos | **[NC]** Nada encontrado. | — |

## 3. WhatsApp Cloud API (Meta)

| Tema | O que encontrei | Fonte |
|---|---|---|
| Token permanente | Criar um System User no Business Settings, atribuir o app e a WABA e gerar token com `business_management`, `whatsapp_business_messaging`, `whatsapp_business_management`. O token temporário não serve para produção. | [Get started](https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started) |
| Limite de envio inicial | Portfólio novo: **250** destinatários únicos fora da janela de atendimento por 24 h. Sobe para 2.000, 10.000, 100.000 e ilimitado. A verificação de negócio ou 2.000 mensagens de qualidade em 30 dias destrava 2.000. Subidas automáticas exigem uso de pelo menos metade do limite em 7 dias. | [Messaging limits](https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits) |
| Templates | Único tipo de mensagem permitido fora da janela de atendimento. Portfólio não verificado: 250 templates por conta; verificado: até 6.000. | [Messaging limits](https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits) |
| Categoria | Se você marca UTILITY e a Meta entende que é MARKETING, o template é aprovado como MARKETING. Uso indevido de UTILITY pode gerar limite. | [Categorização](https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/template-categorization) |
| Janela de 24 h | Mensagens livres só com janela aberta; cada mensagem do usuário reabre a janela. | [Preços](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing) |
| Preço (Brasil) | Cobrança **por mensagem** (a por conversa está deprecada). Tabela em BRL desde 2026-07-01, faturada pela Facebook Brasil. Utility/authentication/service: ~US$ 0,0068 por mensagem. Desde **2026-10-01**, respostas livres e templates utility dentro da janela passam a ser cobrados. Entrada por anúncio Click-to-WhatsApp: conversa gratuita por 72 h. **[NC]** preço de marketing em BRL (está no CSV/PDF da rate card; blogs de terceiros divergem). | [Preços](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing), [não-template](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing/non-template-messages) |
| Assinatura do webhook | Header `X-Hub-Signature-256: sha256=<HMAC-SHA256 do corpo bruto com o app secret>`. Existe rotação do app secret com janela em que os dois valem. | [Webhooks](https://developers.facebook.com/documentation/business-messaging/messenger-platform/webhooks) |
| Throughput | 80 msg/s por número, até 1.000 por upgrade automático. | [Throughput](https://developers.facebook.com/documentation/business-messaging/whatsapp/throughput/) |
| Versão da Graph API | O changelog cita versão 24.0. O código usa **v18.0**. **[NC]** se a v18 já foi aposentada; atualizar de qualquer forma. | [Changelog](https://developers.facebook.com/documentation/business-messaging/whatsapp/changelog) |
| Business verification, exigências de display name, aprovação de template (prazo) | **[NC]** Passo a passo oficial não retornou; consultar o WhatsApp Manager. | — |

**`welcome_lead` é contato frio** (o lead nunca escreveu para a loja no WhatsApp), então é um template de abertura. Planeje que a Meta pode reclassificar como MARKETING, que é a categoria mais cara.

## 4. Gaps no código atual

| # | Gap | Evidência | Gravidade |
|---|---|---|---|
| 1 | **Com `APP_AWS_ENABLED=false` (padrão), webhooks de marketplace são gravados e nunca processados.** O producer retorna `null` e o `MarketplaceLeadConsumer` só existe com `app.aws.enabled=true`. A ingestão de marketplace só funciona com SQS. | `MarketplaceSqsProducer.publishMarketplaceLead`, `MarketplaceLeadConsumer` (`@ConditionalOnProperty`) | Alta |
| 2 | **Autenticação do webhook por `X-Webhook-Token`.** Não achei na doc do ML como configurar header customizado na callback URL (só a URL). Com o token configurado, notificações reais do ML podem receber 401. Com o token vazio, o endpoint fica aberto (`return true`). Webmotors: **[NC]**. A doc de segurança do ML cita HMAC. | `MarketplaceWebhookController.validateToken` | Alta |
| 3 | **Webmotors assumido como webhook, mas só achei API de consulta.** O fetcher diz "exclusivamente webhook-driven" e `fetchLeadsSince` retorna lista vazia. Se não houver push, nenhum lead Webmotors entra. | `WebmotorsLeadFetcher`, `MarketplaceReconciliationJob` (só ML) | Alta |
| 4 | **Refresh token do ML é rotativo e de uso único.** Dois fluxos simultâneos (consumer e job das 3h) podem refrescar a mesma credencial e invalidar o token. Não há lock nem transação por credencial. Se o refresh falhar com `invalid_grant`, não há reautorização guiada. | `MercadoLivreAuthService.refreshToken` | Média |
| 5 | **Lead sem telefone é descartado.** `normalize` lança exceção; o consumer faz `throw e` e o SQS reentrega até a DLQ. Contatos `question` do ML podem vir sem telefone **[NC]**. Não há caminho "sem WhatsApp" (e-mail, ou criar lead com status pendente). | `LeadNormalizationService.normalize`, `MarketplaceLeadConsumer` | Média |
| 6 | **ML: marca, modelo e ano ficam `null`.** O `vehicleInterest` vira só o `contactType`, o que deixa o lead pouco acionável. Os dados ficam no anúncio (`item_id`); buscar o item é provável solução, **[NC]** nesta pesquisa. | `MercadoLivreLeadFetcher` | Média |
| 7 | **Webhook ML engole exceções e responde 200.** Se o SQS falhar, o evento fica sem fila e só a reconciliação do ML o recupera (e ela vem desligada por padrão). Para Webmotors não há recuperação. | `receiveMercadoLivre`, `marketplace.reconciliation.enabled` | Média |
| 8 | **Resolução da organização.** Webmotors depende de `Organization.cnpj` casando com o `Cnpj` do webhook (formatação com ou sem máscara pode divergir). ML depende de `mlUserId` salvo no OAuth. | `resolveOrganization` | Média |
| 9 | **Deduplicação só por telefone** na organização, independente do marketplace ou do veículo. | `LeadIngestionService` | Baixa |
| 10 | **Meta: versão v18.0 fixa** em dois blocos de config e no cliente. | `application.yaml`, `RealMetaApiClient` | Média |
| 11 | **Meta: `sendTemplate` é chamado só com o nome**; sem `components`, o `welcome_lead` precisa ser um template sem variáveis. Se o template aprovado tiver variáveis (nome, veículo), o envio falha. | `MessageSenderService.sendTemplate` | Média |
| 12 | **Meta: filtro de assinatura.** Lê o corpo com `ContentCachingRequestWrapper` e depois repassa o mesmo wrapper adiante; confirmar com um POST assinado real que o `@RequestBody` ainda chega. A chave de replay usa `X-Hub-Delivery`, que **não encontrei** na doc da Meta (cai no fallback por assinatura; uma reentrega idêntica em menos de 5 min vira 403). | `MetaWebhookSignatureFilter` | Média |
| 13 | **Credenciais em colunas simples** (`wm_client_secret`, `wm_access_token`, tokens ML). Não vi criptografia em repouso. | `MarketplaceCredential` | Média |
| 14 | **Meta mock por padrão** (`META_API_MODE=mock`): nada foi validado contra a Meta real. | `application.yaml` | Esperado |

## 5. Por onde começar

### Ações suas (fora do código)

| Ordem | Ação | Prazo estimado (incerto) |
|---|---|---|
| 1 | **Meta:** criar Business Portfolio, iniciar a verificação de negócio, criar app com o caso de uso WhatsApp, registrar um número dedicado e criar o System User com token permanente. | Dias; a verificação pode levar semanas |
| 2 | **Meta:** submeter o template `welcome_lead` (definir texto e se terá variáveis) e tentar a categoria UTILITY. | Horas até dias |
| 3 | **Webmotors:** contato comercial/técnico pedindo (a) cadastro do Astro como CRM no Portal, (b) confirmação se há push ou só consulta, (c) lista de `idTipoLead`, (d) ambiente de testes. Em paralelo, registrar um app no Portal para obter Client ID/Secret e ler "Consulta de Leads" logado. | Semanas **[NC]** |
| 4 | **Mercado Livre:** criar o app em developers, definir o redirect URI, assinar só o tópico `vis_leads`, e testar com uma conta vendedora de verdade na categoria Motors. | Dias |
| 5 | Ter ao menos **uma concessionária piloto** disposta a criar o usuário integrador (Webmotors) e autorizar o OAuth (ML). | Depende do piloto |

### Ações de código (sem depender das de cima)

| Ordem | Ação |
|---|---|
| 1 | Resolver o gap 1: processar inline quando AWS estiver desligado (ou exigir SQS no deploy). |
| 2 | Resolver o gap 2: validar a notificação do ML por assinatura HMAC (assim que confirmado o formato) ou por consulta ao recurso; remover o `return true` quando o token estiver vazio em produção. |
| 3 | Gap 4: serializar o refresh por credencial. |
| 4 | Gap 5 e 6: aceitar lead sem telefone e buscar os dados do anúncio no ML. |
| 5 | Gap 10 a 12: subir a versão da Graph API, parametrizar `components` e testar o webhook com um POST assinado real. |
| 6 | Gap 3: depende da resposta da Webmotors (push ou polling). Se for pull, criar um job de polling análogo ao do ML. |
| 7 | Gap 13: criptografar credenciais em repouso. |

## 6. Perguntas abertas

1. A Webmotors entrega leads por push (qual formato e autenticação) ou só por consulta?
2. Qual é a lista de `idTipoLead` e os campos da resposta da Leads API Rev 41?
3. O ML assina as notificações? Qual header e como se compõe a assinatura? Há IPs fixos?
4. Contatos `question` do ML trazem telefone? Como buscar marca, modelo e ano de um `item_id`?
5. O app do ML precisa de alguma certificação ou aprovação para a categoria Motors?
6. Qual é o preço de marketing em BRL no Brasil (rate card oficial) e a Meta aceitará `welcome_lead` como UTILITY?
7. A v18.0 da Graph API ainda responde?
8. Quanto tempo leva hoje a verificação de negócio e a aprovação de display name na Meta?
