# API

## Base local

- `http://localhost:8080/api/v1`

Swagger:

- `http://localhost:8080/swagger-ui.html`

## Auth

| Metodo | Rota | Observacao |
|---|---|---|
| `POST` | `/auth/register` | cria organizacao e admin |
| `POST` | `/auth/login` | cria sessao e seta cookie de refresh |
| `POST` | `/auth/2fa/verify` | conclui fluxo com 2FA |
| `POST` | `/auth/refresh` | renova access token e gira refresh token |
| `POST` | `/auth/logout` | revoga refresh token e encerra sessao |

Contrato de sessao:

- `accessToken` retorna no body
- `refreshToken` sai do body e vai para cookie `httpOnly`
- `401` em endpoint protegido pode disparar refresh automatico no frontend

## Users

| Metodo | Rota | Observacao |
|---|---|---|
| `GET` | `/users/me` | perfil do usuario autenticado |
| `PATCH` | `/users/me` | nome, telefone, avatar |
| `PATCH` | `/users/me/password` | troca de senha |
| `PATCH` | `/users/me/notifications` | preferencias |
| `PATCH` | `/users/me/two-factor` | liga/desliga 2FA |
| `GET` | `/users` | lista equipe da organizacao, `ADMIN/MANAGER` |
| `GET` | `/users/assignable` | diretorio minimo para filtro de leads |
| `GET` | `/users/{id}` | detalhe de usuario |
| `POST` | `/users/invite` | cria convite/usuario |
| `PATCH` | `/users/{id}/role` | altera role |
| `PATCH` | `/users/{id}/suspend` | suspende usuario |
| `PATCH` | `/users/{id}/reactivate` | reativa usuario |
| `DELETE` | `/users/{id}` | remove usuario |

`GET /users/assignable` retorna apenas usuarios ativos com papel que pode receber leads.

## Leads e Kanban

| Metodo | Rota | Observacao |
|---|---|---|
| `GET` | `/leads` | lista com filtros e paginacao |
| `GET` | `/leads/{id}` | detalhe do lead |
| `GET` | `/leads/{id}/activities` | historico de etapas |
| `POST` | `/leads` | cria lead manual |
| `PUT` | `/leads/{id}` | atualiza lead |
| `PATCH` | `/leads/{id}/move` | move etapa |
| `PATCH` | `/leads/{id}/archive` | arquiva |
| `PATCH` | `/leads/{id}/assume` | usuario assume o lead |
| `PATCH` | `/leads/{id}/assign` | admin/manager atribui lead |
| `DELETE` | `/leads/{id}?includeSamePhone=&reason=` | exclusão manual irreversível (`MANAGER`): card, mensagens, sessões do bot e propostas vinculadas; eventos de marketplace ficam sem vínculo. Para manter métricas, use a anonimização |
| `GET` | `/kanban/board` | board agrupado por coluna |

Filtros relevantes de `GET /leads`:

- `search`
- `origin`
- `stage`
- `assignedTo`
- `dateFrom`
- `dateTo`
- `page`
- `size`

## Inventory

| Metodo | Rota | Observacao |
|---|---|---|
| `GET` | `/vehicles` | lista estoque |
| `GET` | `/vehicles/{id}` | detalhe |
| `PUT` | `/vehicles/{id}` | atualiza |
| `DELETE` | `/vehicles/{id}` | remove |
| `GET` | `/vehicles/summary` | resumo |
| `POST` | `/vehicles/{vehicleId}/photos` | upload de foto |
| `DELETE` | `/vehicles/{vehicleId}/photos/{photoId}` | remove foto |
| `PATCH` | `/vehicles/{vehicleId}/photos/reorder` | reordena fotos |

## Analytics

| Metodo | Rota |
|---|---|
| `GET` | `/analytics/kpis` |
| `GET` | `/analytics/charts/monthly-sales` |
| `GET` | `/analytics/charts/sales-by-category` |

## Communication

| Metodo | Rota |
|---|---|
| `GET` | `/chat/lead/{leadId}` |
| `POST` | `/chat/send` |
| `POST` | `/webhooks/meta` |

## SaaS

| Metodo | Rota | Observacao |
|---|---|---|
| `GET` | `/plans` | publico |
| `GET` | `/saas/subscription` | `ADMIN` |
| `PATCH` | `/saas/subscription/plan/{planId}` | `ADMIN` |
| `GET` | `/saas/usage` | `ADMIN` |
| `GET` | `/saas/billing-history` | `ADMIN` |
| `GET` | `/saas/marketplaces` | `ADMIN` |
| `POST` | `/saas/marketplaces` | `ADMIN` |
| `PATCH` | `/saas/marketplaces/{id}` | `ADMIN` |
| `DELETE` | `/saas/marketplaces/{id}` | `ADMIN` |

## Privacidade (LGPD) — somente `MANAGER`

| Metodo | Rota | Observacao |
|---|---|---|
| `POST` | `/privacy/requests` | `{type: ANONYMIZATION\|ACCESS\|CORRECTION\|DELETION, identifier, channel?, receivedAt?}`; guarda só HMAC e dica; prazo `privacy.sla-days` (15) |
| `GET` | `/privacy/requests?status=` | lista com `overdue` |
| `POST` | `/privacy/requests/{id}/execute` | `{identifier, resultSummary?}`; identificador divergente → 422; anonimização e exclusão são irreversíveis |
| `POST` | `/privacy/requests/{id}/reject` | `{reason}` |

## Integracoes

| Metodo | Rota |
|---|---|
| `PATCH` | `/organization/whatsapp` |
| `GET` | `/auth/mercadolivre/connect` |
| `GET` | `/auth/mercadolivre/callback` |
| `POST` | `/webhooks/mercadolivre` |
| `POST` | `/webhooks/scraper` |
