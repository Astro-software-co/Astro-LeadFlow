# API Reference

## Base URL

| Ambiente | URL |
|---|---|
| Local | `http://localhost:8080/api/v1` |
| Produção | `https://api.leadflow.astro.com.br/api/v1` |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## Autenticação

Todos os endpoints protegidos exigem `Authorization: Bearer <accessToken>`.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/auth/login` | Login com email + senha → `{ accessToken, expiresIn }` + cookie `httpOnly` |
| `POST` | `/auth/refresh` | Usa cookie de refresh → novo `accessToken` |
| `POST` | `/auth/logout` | Revoga refresh token no backend + limpa cookie |

---

## IAM — Usuários

| Método | Endpoint | Papel mínimo | Descrição |
|---|---|---|---|
| `GET` | `/users/me` | SELLER | Perfil do usuário logado |
| `PATCH` | `/users/me/password` | SELLER | Alterar senha |
| `PATCH` | `/users/me/notifications` | SELLER | Preferências de notificação |
| `PATCH` | `/users/me/two-factor` | SELLER | Ativar / desativar 2FA |
| `GET` | `/users` | MANAGER | Listar membros da organização |
| `POST` | `/users/invite` | ADMIN | Convidar novo usuário |
| `DELETE` | `/users/{id}` | ADMIN | Remover usuário da organização |

---

## CRM — Leads

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/leads` | Listagem paginada com filtros (stage, source, assignedTo, dateRange) |
| `POST` | `/leads` | Criar lead manualmente |
| `GET` | `/leads/{id}` | Detalhe do lead |
| `PUT` | `/leads/{id}` | Atualizar dados do lead |
| `PATCH` | `/leads/{id}/move` | Mover lead para nova etapa do pipeline |
| `PATCH` | `/leads/{id}/assume` | Vendedor assume o lead (IN_ATTENDANCE) |
| `PATCH` | `/leads/{id}/assign` | Admin/Manager atribui lead a vendedor |
| `PATCH` | `/leads/{id}/archive` | Arquivar lead (ARCHIVED) |
| `GET` | `/leads/{id}/activities` | Histórico de atividades do lead |

### Etapas do Lead (LeadStage)

```
TALKING_TO_BOT → WAITING_ATTENDANCE → IN_ATTENDANCE → IN_NEGOTIATION → COMPLETED / ARCHIVED
```

### Fontes do Lead (LeadSource)

`WEBMOTORS` | `MERCADO_LIVRE` | `WEBSITE` | `PHONE` | `WALK_IN` | `REFERRAL`

---

## CRM — Kanban

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/kanban/board` | Board completo com colunas e cards ordenados |

O endpoint retorna as colunas com os leads agrupados por etapa, otimizado para renderização do Kanban sem N+1 queries.

---

## Inventory — Veículos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/vehicles` | Catálogo paginado com filtros (marca, modelo, ano, preço, status, categoria) |
| `GET` | `/vehicles/{id}` | Detalhe do veículo com spec grid e histórico |
| `POST` | `/vehicles` | Cadastrar veículo |
| `PATCH` | `/vehicles/{id}/archive` | Arquivar veículo |

---

## Communication — WhatsApp

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/chat/lead/{leadId}` | Histórico de mensagens do lead |
| `GET` | `/whatsapp/templates` | Templates disponíveis por organização |
| `POST` | `/whatsapp/send` | Enviar mensagem usando template |

---

## Analytics

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/analytics/kpis` | KPIs: total leads, taxa conversão, tempo médio resposta, receita |
| `GET` | `/analytics/charts/monthly-sales` | Série temporal de vendas mensais |
| `GET` | `/analytics/charts/sales-by-category` | Distribuição de vendas por categoria de veículo |

---

## SaaS — Assinatura e Billing

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/saas/subscription` | Assinatura atual com plano, status e datas |
| `GET` | `/saas/usage` | Uso atual por marketplace vs cota |
| `GET` | `/saas/billing-history` | Histórico paginado de cobranças |
| `PATCH` | `/saas/subscription/plan/{planId}` | Trocar plano de assinatura |
| `GET` | `/saas/plans` | Todos os planos disponíveis |

---

## Ingestion — Webhooks

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/webhooks/meta` | Receber eventos do Meta (WhatsApp Business) |
| `POST` | `/webhooks/webmotors` | Receber leads do Webmotors |
| `POST` | `/webhooks/mercadolivre` | Receber leads do Mercado Livre |

**Segurança dos webhooks:**

- Meta: valida `X-Hub-Signature-256` (HMAC-SHA256) + anti-replay Redis.
- Webmotors / Mercado Livre: valida API key no header `X-Api-Key`.
- Em produção: `app.meta.webhook-secret` em branco causa falha na inicialização.

---

## Códigos de Resposta Relevantes

| Código | Significado |
|---|---|
| `200` | Sucesso |
| `201` | Criado |
| `400` | Payload inválido (erros de validação no body) |
| `401` | Token ausente, expirado ou revogado |
| `403` | Papel insuficiente para a operação |
| `404` | Recurso não encontrado |
| `409` | Conflito (ex.: lead duplicado já existe) |
| `429` | Rate limit atingido |
| `500` | Erro interno do servidor |

---

## Páginas Relacionadas

- [Architecture](./02-Architecture.md)
- [Runbooks](./03-Runbooks.md)
