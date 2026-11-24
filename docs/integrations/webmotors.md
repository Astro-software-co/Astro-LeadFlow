# Integração Webmotors — consolidação e gaps

> Status: **pré-solicitação de acesso**. O lado de leads está implementado e testado contra o contrato provisório da collection (ver seção 7). Falta homologar com a Webmotors e ajustar o que o contrato real divergir.
> Fontes: artigo do Cockpit (CRM Terceiro), formulário de liberação da "API de Consulta de Leads" (Portal do Desenvolvedor, Sensedia) e a collection Postman "API Lead" (ambiente de homologação `hlg-webmotors.sensedia.com`).
> Itens marcados com **(hipótese)** são inferências a confirmar com a Webmotors antes de implementar.

## 1. O que a collection Postman revela

### 1.1 Modelo é *pull*, não webhook
O endpoint principal é `GET /lead/v1/leads`, descrito como "listagem de propostas Pendentes de Aprovação". Não há registro de callback/webhook na collection. Logo, o Astro LeadFlow precisa **consultar** a Webmotors periodicamente (polling) e não apenas expor uma URL. Isso muda a arquitetura que eu havia sugerido antes (endpoint de webhook).

Hoje o backend não tem nenhum `@Scheduled` nem `@EnableScheduling`.

### 1.2 Autenticação
- `POST https://hlg-webmotors.sensedia.com/oauth/v1/access-token`
- Header `Authorization: Basic base64(client_id:client_secret)`
- Body: `grant_type=password` com `username` e `password` de um **usuário**.
- Resposta esperada: HTTP **201** com `access_token`, `token_type`, `expires_in`.
- Chamadas seguintes enviam os headers `client_id` e `access_token` (não é `Authorization: Bearer`).

Implicações: o fluxo é *password grant*, então precisamos saber **de quem é esse usuário** (conta nossa, conta do lojista, ou uma por loja). Sem refresh token na collection: ao expirar, refazer o login.

### 1.3 Endpoints (todos em `hlg-webmotors.sensedia.com`)

| Método | Caminho | Função |
|---|---|---|
| POST | `/oauth/v1/access-token` | Login (https) |
| GET | `/lead/v1/leads` | Lista propostas pendentes (paginada) |
| GET | `/lead/v1/leads/{id}?id_tipo_lead=1` | Detalhe do lead |
| PATCH | `/lead/v1/leads/{id}/aceite` | Aceitar ou recusar |
| PATCH | `/lead/v1/leads/{id}/status` | Alterar status / finalizar com motivo |
| GET | `/lead/v1/motivos-recusa?tipolead=1` | Domínio de motivos |
| GET | `/lead/v1/tipos` | Domínio de tipos de lead |
| GET | `/lead/v1/canais` | Domínio de canais |
| POST | `/inserir-lead/v1/criar` | **Inserir** lead na Webmotors (sentido inverso) |

Os endpoints de lead aparecem com `http://` (sem TLS) na collection de homologação. Em produção deve ser https; **confirmar**. A URL de produção não consta.

### 1.4 Formato do lead (listagem)
Envelope: `paginaAtual`, `totalItens`, `totalPaginas`, `mensagens[]`. Cada item:
- Lead: `id`, `dataMensagem`, `dataAceite`, `aceitoPor`, `dataAgendamento`, `status`, `statusLead`, `codigoMotivoFinalizacao`, `midia`, `tipoLead`, `subTipoLead`, `tipoLeadDesc`, `mensagem`
- `loja`: `id`, `nomeFantasia`
- `anuncio`: `id`, `marca`, `modelo`, `versao`, `anoModelo`, `anoFabricacao`, `valor`, `placa`, `tipoVeiculo`, `blindado`
- `vendedor`: `id`, `nome`

Detalhe do lead acrescenta `nome`, `email`, `telefone`. O teste da collection espera o telefone em formato `"(11) ..."` e nome `"Comprador"`. **(hipótese)** os dados de contato só vêm completos depois do aceite.

Não há parâmetros de paginação, filtro por data ou por status documentados na collection.

### 1.5 Fluxo de aceite/recusa
- Aceitar: `PATCH .../aceite` com `{ "aceito": true, "tipo_lead": 1, "codigo_motivo": 3 }`
- Recusar: mesmo endpoint com `"aceito": false`, `codigo_motivo` e `mensagem`
- Motivos de recusa (do script de teste): 1 spam, 2 já vendi o veículo, 3 proposta abaixo do valor de mercado, 4 proposta de troca inviável, 5 dados já estão no anúncio, 6 outros, 7 não atendo o DDD, 10 sobre o financiamento. A lista oficial vem de `GET /motivos-recusa`.
- A descrição fala em "Proposta" (lead de compra do seu veículo ou troca) e há `tipo_lead` 1 e 2 sem legenda. **Confirmar o que cada tipo significa** via `GET /tipos`.

### 1.6 Inserir lead
`POST /inserir-lead/v1/criar` com `cliente {nome, email}`, `lead {id, id_canal, tipo_lead, mensagem, temperatura}` e `anuncio {tipo_anuncio, marca, modelo, versao, ano_fabricacao, ano_modelo, km, placa}`. Na collection é um exemplo de teste com `id_canal: 59`. Serve para empurrar à Webmotors um lead nascido em outro canal; **não é necessário para o primeiro escopo**.

### 1.7 Limitações da collection
- Os scripts de teste de `Tipos` e `Canais` foram copiados de `Motivo Recusa` (validam as chaves de motivos), então **não** servem como documentação do formato dessas respostas.
- Não há endpoints de estoque/anúncios aqui. O Gestor de Estoque Terceiro é outra API, ainda sem documentação em mãos.
- Não há endpoint de anonimização/exclusão de titular.
- A collection traz dados de exemplo de uma pessoa real (e-mail corporativo da Webmotors). Não copiar para fixtures ou testes do projeto.

## 2. Estado atual do Astro LeadFlow (resumo)

- Leads: `Lead` tem `origin`, `phoneNumber` (obrigatório), `name`, `email`, `vehicleInterest`, `vehicle` (FK), `notes`, `pipelineStage`.
- Ingestão: `POST /api/v1/webhooks/scraper` → `LeadIngestionService` (normaliza telefone, dedupe por `organization + telefone`, SQS opcional, envia template `welcome_lead` por WhatsApp).
- Estoque: `Vehicle` (`brand`, `model`, `year`, `licensePlate`, `salePrice`, `mileageKm`, `status`, fotos S3). Sem versão, combustível, câmbio, opcionais.
- Multi-loja: `Organization` (`slug`, credenciais WhatsApp). `MarketplaceConfig` guarda só `enabled` e `monthlyLimit`.
- HTTP cliente: `WebClient` (usado em `MetaClient`).
- LGPD: só `DELETE /leads/{id}` (`ADMIN`/`MANAGER`), com cascade para histórico e chat.

## 3. Gaps e o que implementar

Prioridade: **P0** bloqueia a homologação/primeira integração, **P1** necessário para produção, **P2** melhoria.

### 3.1 Leads (consulta e aceite)

| # | Gap | Prioridade |
|---|---|---|
| L1 | Cliente HTTP Webmotors (`WebmotorsClient`): login OAuth password, cache do token até `expires_in`, relogin em 401, headers `client_id`/`access_token`, timeouts e retry | P0 |
| L2 | Polling agendado (`@EnableScheduling`) por loja habilitada, com paginação até `totalPaginas`, intervalo configurável, lock distribuído (várias instâncias não podem consumir o mesmo lead) | P0 |
| L3 | Armazenar credenciais da integração por organização, **criptografadas** (hoje `whatsappToken` fica em texto na `Organization`; não repetir o padrão): `client_id`, `client_secret`, usuário/senha, ID da loja na Webmotors | P0 |
| L4 | Vínculo `Organization` ↔ `loja.id`/CNPJ da Webmotors (hoje o vínculo é por `slug`) | P0 |
| L5 | Campos novos em `Lead`: `external_id`, `external_source` (único por organização + fonte), `external_ad_id`, `external_type`, `external_status`, `received_at`, `accepted_at`, `channel` (formulário/ligação) | P0 |
| L6 | Dedupe por `external_id` antes do dedupe por telefone. O dedupe só por telefone pode descartar um 2º interesse do mesmo cliente em outro carro | P0 |
| L7 | Fluxo aceitar/recusar: decidir regra (aceite automático ou manual pelo lojista), tela e endpoint no Kanban, tabela local de motivos (sincronizada com `/motivos-recusa`) | P0 |
| L8 | Sincronizar mudanças de etapa do Kanban de volta (`/status` com código de motivo) ao concluir/arquivar. Exige mapa `PipelineStage` → status/motivo Webmotors | P1 |
| L9 | Telefone pode vir mascarado ou ausente antes do aceite. `phone_number` é `NOT NULL` hoje; definir comportamento (guardar como pendente e completar após aceite) | P0 |
| L10 | Ligar `Lead.vehicle` ao `Vehicle` pelo anúncio (match por `anuncio.id` guardado no `Vehicle` ou por placa) | P1 |
| L11 | Mensagem de boas-vindas no WhatsApp: só disparar após aceite e com base legal/consentimento definidos; avaliar se os termos da Webmotors permitem contato por esse canal | P0 |
| L12 | Tratamento de falhas: dead-letter, métricas, alerta quando o polling para, limites de taxa (rate limit desconhecido) | P1 |
| L13 | Config por loja: ativar/desativar integração, última sincronização, erros recentes (reaproveitar `MarketplaceConfig`) | P1 |
| L14 | Contar leads Webmotors no `UsageLimitService` (`monthlyLimit`) | P2 |

### 3.2 Estoque (Gestor de Estoque Terceiro)
API ainda sem documentação. Pontos já previsíveis:

| # | Gap | Prioridade |
|---|---|---|
| E1 | Obter a documentação/collection da API de Gestor de Estoque | P0 |
| E2 | `Vehicle` sem versão, combustível, câmbio, carroceria, opcionais, cidade; `year` único (a Webmotors usa fabricação e modelo) | P1 |
| E3 | Mapeamento marca/modelo/versão para o catálogo (códigos) da Webmotors | P1 |
| E4 | Campo `external_ad_id` no `Vehicle` e serviço de publicar/atualizar/despublicar acionado por mudança de `VehicleStatus` | P1 |
| E5 | Envio de fotos: hoje URLs do S3 (`VehiclePhoto.s3Url`); verificar se a API aceita URL ou upload | P1 |

### 3.3 LGPD (últimos dois pontos do formulário)

| # | Gap | Prioridade |
|---|---|---|
| G1 | Endpoint de anonimização de lead (sobrescrever `name`, `phone_number`, `email`, `notes`, conteúdo de `chat_messages`; manter `pipeline_stage` e métricas) em vez de só `DELETE` | P0 |
| G2 | Registro de solicitações de titulares (quem pediu, quando, prazo, quem atendeu, status) — base para o SLA | P0 |
| G3 | Canal oficial: e-mail de privacidade/DPO e página de política de privacidade (frontend não tem nada hoje) | P0 |
| G4 | Política de retenção e rotina de expurgo de leads antigos | P1 |
| G5 | Abrangência da anonimização: mensagens já enviadas via WhatsApp (Meta), arquivos no S3, backups, logs | P1 |
| G6 | Propagar à Webmotors/lojista quando um titular pedir exclusão, se o contrato exigir | P2 |
| G7 | Logs: não registrar telefone/e-mail em claro (`LeadIngestionService` loga o telefone hoje) | P1 |

### 3.4 Segurança
- S1 (P0): webhook atual é `permitAll` e confia no API Gateway; endpoints novos devem ter autenticação própria. Se o modelo for só *pull*, **não expor** endpoint público para a Webmotors.
- S2 (P0): segredos (`client_secret`, senha) em cofre (AWS Secrets Manager) ou criptografados em coluna; nunca em log.
- S3 (P1): garantir TLS em todas as chamadas a produção.

## 4. Perguntas para a Webmotors / decisões internas

**Para a Webmotors (levar no chamado ou na reunião de homologação):**
1. A "API de Consulta de Leads" é o mesmo mecanismo do "CRM Terceiro" do Cockpit, ou o CRM Terceiro é um envio ativo (push) para a nossa URL?
2. O usuário do *password grant* é por loja ou um usuário técnico nosso? Como a loja autoriza o Astro no Cockpit?
3. O telefone do cliente é mascarado antes do aceite?
4. Qual o rate limit, o intervalo de polling recomendado e como funciona a paginação/filtros (por data, por status)?
5. Significado de `tipo_lead` (1, 2 e `subTipoLead`) e de `id_canal`. O lead de ligação aparece nessa API?
6. URL, credenciais e janela de homologação; critérios de aprovação; existe ambiente de produção separado?
7. Existe webhook ou notificação de novos leads?
8. Documentação da API de Gestor de Estoque Terceiro.
9. Termos de uso do dado do lead (contato por WhatsApp, retenção, subprocessadores) e canal/SLA de anonimização do lado deles.

**Decisões nossas:**
- Aceite automático ou manual?
- E-mail de privacidade oficial e SLA de atendimento a titulares (LGPD permite 15 dias; prometer só o que o processo cumprir).
- Quem cadastra: dados do formulário (razão social, CNPJ, logo PNG, nome da APP no Portal do Desenvolvedor, CNPJs de clientes em comum, regiões de atuação, objetivo da integração).

## 5. Respostas sugeridas aos dois últimos pontos do formulário

**Como os dados dos leads serão processados e disponibilizados aos lojistas?**
Os leads são consultados via API, associados à loja correta (isolamento por organização), deduplicados e exibidos no funil (Kanban) do lojista com atribuição a vendedores. Acesso restrito por perfil. *(Reconfirmar o texto após decidir aceite automático/manual e o uso de WhatsApp.)*

**Canal oficial para anonimização e SLA:**
*Preencher com o e-mail de privacidade e o SLA que decidirmos (itens G1–G3 precisam existir antes de declarar).*

## 6. Ordem sugerida de implementação

1. Decidir pendências da seção 4 e enviar o formulário (não depende de código).
2. G1–G3 (anonimização, registro de solicitações, página/e-mail de privacidade) para sustentar o que for declarado.
3. L3–L5 e L9 (modelo de dados: credenciais criptografadas, vínculo de loja, campos externos no `Lead`, telefone opcional).
4. L1–L2 e L6 (cliente HTTP, polling, dedupe por ID externo) contra o ambiente de homologação.
5. L7, L8, L11 (aceite/recusa, sincronização de status, mensagem inicial).
6. L10, L12–L14 (vínculo com veículo, observabilidade, config por loja, limites).
7. Estoque (E1–E5) quando a documentação chegar.

## 7. Estado da implementação (módulo `integrations/webmotors`)

Tudo fica em `backend/src/main/kotlin/com/astro/leadflow/shared/modules/integrations/webmotors/`, desacoplado do resto. O Mercado Livre pode ganhar um módulo irmão com outra porta, sem reaproveitar nada daqui.

### 7.1 Decisões de design (diferem do plano das seções 2–3)
- **Caixa de entrada própria em vez de campos novos em `Lead`** (L5 mudou). A proposta pendente vive em `webmotors_lead_inbox` e só vira `Lead` depois do aceite e de existir telefone válido. Isso resolve L9 sem tornar `phone_number` anulável, evita disparar WhatsApp antes do aceite (L11) e mantém o `Lead` intacto. O vínculo é `inbox.lead_id`.
- **Uma única porta para o CRM:** `CrmLeadIntake` recebe um `LeadIntakeCommand` (contato, origem, evento, anúncio e snapshot do veículo). O `CrmLeadIntakeAdapter` delega a `LeadIngestionService.ingestResolved`, a mesma regra do scraper e do Mercado Livre: telefone já existente na organização gera lead com `isDuplicate=true`, que ainda recebe a mensagem (regra atual; o modelo Card + Interesses da phase G4 substitui isso neste mesmo ponto). Falha de cota ou da Meta não desfaz o lead.
- **Aceite automático por padrão** (`AcceptMode.AUTO`, decisão D11); `MANUAL` é opção por loja. **Mensagem de abordagem (M1) ligada por padrão** (`welcomeMessageEnabled=true`, decisão D12); o pedido de consentimento (M2) não é desligável por loja. Sem abordagem, o lead entra em `WAITING_ATTENDANCE` (não há bot conversando).
- **Credenciais por loja cifradas** (AES-256-GCM) em `webmotors_accounts`; chave em `WEBMOTORS_SECRETS_KEY`.
- **Sem webhook público.** O módulo só faz chamadas de saída (S1).
- Transações curtas: nenhuma chamada HTTP acontece dentro de transação de banco.

### 7.2 Gaps atendidos
L1 cliente HTTP (login, cache de token, relogin em 401) · L2 polling agendado com lease no banco (várias instâncias não sincronizam a mesma conta) · L3 credenciais cifradas · L4 vínculo org↔loja (`store_id` filtra leads de outra loja) · L5/L9 inbox · L6 dedupe por `(org, id externo, tipo)` + por telefone · L7 aceite/recusa via API (sem tela) · L11 boas-vindas só após aceite e opcional · L12 parcial (erro por conta registrado em `last_sync_error`; sem métricas/alerta) · L13 parcial (config por loja via API; sem tela).

Endpoints (somente `MANAGER`; sem token → 401), base `/api/v1/integrations/webmotors`: `GET|PUT /account`, `POST /account/test`, `POST /sync`, `GET /proposals?status=`, `POST /proposals/{id}/accept`, `POST /proposals/{id}/refuse`, `GET /refusal-reasons`.

Configuração (`application.properties`): `WEBMOTORS_ENABLED` (liga o polling), `WEBMOTORS_AUTH_URL`, `WEBMOTORS_API_URL` (defaults = **homologação**), `WEBMOTORS_POLL_INTERVAL_MS`, `WEBMOTORS_PAGE_PARAM`, `WEBMOTORS_SECRETS_KEY` (`openssl rand -base64 32`).

### 7.3 O que ainda é suposição (ajustar na homologação)
Cada item abaixo já está isolado num ponto único:
- Nome do query param de paginação (`WEBMOTORS_PAGE_PARAM`, default `pagina`) e se `totalPaginas`/`paginaAtual` funcionam como esperado.
- Formato do contato no detalhe: `WebmotorsContactExtractor` procura `nome`/`email`/`telefone` em qualquer nível, ignorando vendedor/loja/anúncio. Se o telefone vier mascarado até o aceite, a proposta fica `AWAITING_CONTACT` e é retentada a cada ciclo (até `maxContactAttempts`, depois `FAILED`).
- Aceitar não envia `codigo_motivo` (a collection envia, mas parece cópia do recusar).
- IDs tratados como texto; datas guardadas como texto cru (`remote_created_at`).
- Significado de `tipoLead` (usado só como parte da chave de unicidade e repassado nas chamadas).
- Hosts de produção e uso de `https` (a collection de homologação usa `http` nos endpoints de dados).

### 7.4 Ainda não feito
L8 (devolver mudança de etapa do Kanban à Webmotors com `/status`; o cliente já tem `changeStatus`) · L10 (vínculo com `Vehicle`) · L14 (cota mensal) · telas no frontend (conta, fila de propostas) · métricas/alertas · estoque (E1–E5) · LGPD (G1–G7), que **continua bloqueando** a resposta ao formulário · migrar `LeadIngestionService`/scraper não é necessário.

### 7.5 Como validar
Testes em `backend/src/test/kotlin/.../integrations/webmotors/` (47): cifra, extrator, cliente HTTP (sem rede), sync, importador, decisões, lease, adaptador e repositórios em H2.

## 8. Fluxo legado por webhook (removido)

A `develop` trazia uma integração Webmotors **por webhook** no módulo `ingestion` (`POST /api/v1/webhooks/webmotors`, `WebmotorsLeadFetcher`, `WebmotorsAuthService`, colunas `wm_*` em `marketplace_credentials`, propriedades `marketplace.webmotors.*`). Ela assumia outro contrato (notificação `IdLead/Cnpj/IdTipoLead`, auth em dois passos, detalhe em `/leads/v1/{id}`) que não aparece na collection e nunca rodou contra a Webmotors real.

**Decisão D1 (2026-11-23): polling é o único caminho.** O fluxo legado foi removido (T1.6): endpoint, request, fetcher, auth service e seus testes apagados; colunas `wm_*` removidas na `V20__drop_legacy_webmotors_credentials.sql`; propriedades e variáveis `WEBMOTORS_BASE_URL/OAUTH_URL/CLIENT_ID/CLIENT_SECRET` retiradas. Continuam: `MarketplaceType.WEBMOTORS` (cota, `marketplace_configs`, origem do lead) e `Organization.cnpj`.

A migration deste módulo é `V19__webmotors_leads_polling.sql` (renomeada de `V5` no merge com a `develop`, que já ocupa `V1`–`V18`).
