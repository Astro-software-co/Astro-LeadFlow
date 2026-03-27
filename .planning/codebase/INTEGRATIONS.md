# External Integrations

**Analysis Date:** 2026-03-26

## Current State: No Active Integrations

The frontend has **zero external integrations**. There is no HTTP client, no API calls, no service layer, and no environment variables. All data is served from a single hardcoded mock file at `frontend/src/data/mockData.ts`.

The backend (Kotlin/Spring Boot) is fully built with complete REST APIs. The frontend has not begun API integration.

## Backend API Contract (Ready for Integration)

**Base URL:** `http://localhost:8080/api/v1`
**OpenAPI/Swagger:** `http://localhost:8080/swagger-ui.html`
**Auth:** JWT Bearer tokens (access + refresh)

### IAM Module

**Auth Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/api/AuthController.kt`):
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/auth/register` | Register new organization + admin user, returns JWT tokens |
| POST | `/api/v1/auth/login` | Login, returns JWT tokens (supports 2FA) |
| POST | `/api/v1/auth/2fa/verify` | Verify 2FA code, returns JWT tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token using refresh token |

**User Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/api/UserController.kt`):
| Method | Endpoint | Purpose | Roles |
|--------|----------|---------|-------|
| GET | `/api/v1/users/me` | Get current user | Any |
| PATCH | `/api/v1/users/me` | Update profile (name, phone, avatar) | Any |
| PATCH | `/api/v1/users/me/password` | Change password | Any |
| PATCH | `/api/v1/users/me/notifications` | Update notification preferences | Any |
| PATCH | `/api/v1/users/me/two-factor` | Toggle 2FA | Any |
| GET | `/api/v1/users` | List org users | ADMIN, MANAGER |
| GET | `/api/v1/users/{id}` | Get user by ID | ADMIN, MANAGER |
| POST | `/api/v1/users/invite` | Invite new user | ADMIN, MANAGER |
| PATCH | `/api/v1/users/{id}/role` | Change user role | ADMIN |
| PATCH | `/api/v1/users/{id}/suspend` | Suspend user | ADMIN |
| PATCH | `/api/v1/users/{id}/reactivate` | Reactivate user | ADMIN |
| DELETE | `/api/v1/users/{id}` | Remove user | ADMIN |

**Organization Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/api/OrganizationController.kt`):
| Method | Endpoint | Purpose | Roles |
|--------|----------|---------|-------|
| GET | `/api/v1/organization` | Get current org data | Any |
| PUT | `/api/v1/organization` | Update org data | ADMIN |
| PATCH | `/api/v1/organization/whatsapp` | Update WhatsApp credentials | ADMIN |

### CRM Module

**Lead Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/crm/api/LeadController.kt`):
| Method | Endpoint | Purpose | Roles |
|--------|----------|---------|-------|
| POST | `/api/v1/leads` | Create lead manually | Any |
| GET | `/api/v1/leads` | List all org leads | Any |
| GET | `/api/v1/leads/{id}` | Get lead by ID | Any |
| PUT | `/api/v1/leads/{id}` | Update lead data | Any |
| PATCH | `/api/v1/leads/{id}/move` | Move lead to Kanban stage | Any |
| PATCH | `/api/v1/leads/{id}/archive` | Archive lead | Any |
| PATCH | `/api/v1/leads/{id}/assume` | Seller assumes lead | Any |
| PATCH | `/api/v1/leads/{id}/assign` | Assign lead to seller | ADMIN, MANAGER |
| DELETE | `/api/v1/leads/{id}` | Delete lead permanently | ADMIN, MANAGER |

**Kanban Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/crm/api/KanbanController.kt`):
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/kanban/board` | Get full Kanban board for org |

### Inventory Module

**Vehicle Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/inventory/api/VehicleController.kt`):
| Method | Endpoint | Purpose | Roles |
|--------|----------|---------|-------|
| POST | `/api/v1/vehicles` | Create vehicle | ADMIN, MANAGER |
| GET | `/api/v1/vehicles` | List vehicles (supports `?search=` and `?status=`) | Any |
| GET | `/api/v1/vehicles/{id}` | Get vehicle by ID | Any |
| PUT | `/api/v1/vehicles/{id}` | Update vehicle | ADMIN, MANAGER |
| DELETE | `/api/v1/vehicles/{id}` | Delete vehicle | ADMIN |
| GET | `/api/v1/vehicles/summary` | Stock summary by status | Any |

**Vehicle Photo Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/inventory/api/VehiclePhotoController.kt`):
- Manages vehicle photo uploads (likely to S3/LocalStack)

### Analytics Module

**Dashboard Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/analytics/api/DashboardController.kt`):
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/analytics/kpis` | All dashboard KPIs |
| GET | `/api/v1/analytics/charts/monthly-sales` | Monthly sales chart data (6 months) |
| GET | `/api/v1/analytics/charts/sales-by-category` | Sales by vehicle category |

### Communication Module

**Chat Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/communication/api/ChatController.kt`):
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/chat/lead/{leadId}` | Get message history for lead |
| POST | `/api/v1/chat/send` | Send WhatsApp message to lead |

**Meta Webhook Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/communication/api/MetaWebhookController.kt`):
- Receives incoming WhatsApp messages via Meta webhook

### SaaS Module

**Plan Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/saas/api/PlanController.kt`):
| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| GET | `/api/v1/plans` | List all available plans | Public |

**Subscription Controller** (`backend/src/main/kotlin/com/astro/leadflow/shared/modules/saas/api/SubscriptionController.kt`):
| Method | Endpoint | Purpose | Roles |
|--------|----------|---------|-------|
| GET | `/api/v1/saas/subscription` | Get current org subscription | ADMIN |
| PATCH | `/api/v1/saas/subscription/plan/{planId}` | Change plan | ADMIN |
| GET | `/api/v1/saas/usage` | Get monthly usage summary | ADMIN |
| GET | `/api/v1/saas/marketplaces` | List configured marketplaces | ADMIN |
| POST | `/api/v1/saas/marketplaces` | Add marketplace | ADMIN |
| PATCH | `/api/v1/saas/marketplaces/{id}` | Update marketplace config | ADMIN |
| DELETE | `/api/v1/saas/marketplaces/{id}` | Remove marketplace | ADMIN |

### Ingestion Module

**Webhook Controllers:**
- `ScraperWebhookController.kt` - Receives scraped lead data
- `MarketplaceWebhookController.kt` - Marketplace-specific webhooks
- `MercadoLivreAuthController.kt` - Mercado Livre OAuth flow

## Data Storage (Backend)

**Databases:**
- PostgreSQL 16 (via Docker: `backend/compose.yaml`)
  - Database: `leadflow`
  - ORM: Spring Data JPA + Hibernate
  - Migrations: Flyway

**Caching:**
- Redis 7 (via Docker: `backend/compose.yaml`)
  - Used for: rate limiting, session data, caching

**File Storage:**
- LocalStack S3 (via Docker: `backend/compose.yaml`)
  - Services: S3, SQS, SNS
  - Region: us-east-1
  - Used for: vehicle photos, file uploads

**Email:**
- MailHog (dev environment)
  - SMTP: port 1025
  - Web UI: port 8025

## Authentication & Identity

**Current Frontend State:**
- Login page at `frontend/src/pages/Login.tsx` is **purely visual**
- Form submits with `toast.success()` and `navigate('/')` - no actual auth
- No JWT storage, no auth headers, no token refresh
- No route guards - all routes are publicly accessible
- No role-based rendering

**Backend Auth System (ready):**
- JWT-based authentication (access + refresh tokens)
- RBAC with roles: ADMIN, MANAGER, SELLER (via `@PreAuthorize`)
- Multi-tenant: every request scoped to `organizationId` from JWT claims
- 2FA support via email/SMS verification codes
- Spring Security with password hashing

**Integration requirements for frontend:**
1. Install HTTP client (axios recommended)
2. Create auth service calling `/api/v1/auth/login`, `/register`, `/refresh`
3. Store tokens securely (httpOnly cookie preferred, memory fallback)
4. Add request interceptor to attach `Authorization: Bearer <token>`
5. Add response interceptor for 401 handling + token refresh
6. Create `AuthGuard` component for route protection
7. Create `RoleGate` component for role-based rendering

## Mock Data vs Real API

**Current data source:** `frontend/src/data/mockData.ts`

**Mock data exports:**
| Export | Type | Used by | Backend Equivalent |
|--------|------|---------|-------------------|
| `mockVehicles` | `Vehicle[]` (8 items) | `Estoque.tsx`, `Dashboard.tsx` | `GET /api/v1/vehicles` |
| `mockLeads` | `Lead[]` (7 items) | Referenced by Kanban cards | `GET /api/v1/leads` |
| `mockKanbanCards` | `KanbanCard[]` (7 items) | `Kanban.tsx` | `GET /api/v1/kanban/board` |
| `mockKPIData` | `KPIData` | `Dashboard.tsx` | `GET /api/v1/analytics/kpis` |
| `mockMonthlySales` | `MonthlySalesData[]` (6 items) | `Dashboard.tsx` | `GET /api/v1/analytics/charts/monthly-sales` |
| `mockCategorySales` | `CategorySalesData[]` (5 items) | `Dashboard.tsx` | `GET /api/v1/analytics/charts/sales-by-category` |
| `mockRecentSales` | `RecentSale[]` (8 items) | `Dashboard.tsx` | No direct endpoint (derive from leads) |

**Type mismatches between frontend and backend:**

The frontend types in `frontend/src/types/index.ts` use Portuguese field names while the backend uses English:

| Frontend Type | Frontend Field | Backend Equivalent |
|--------------|---------------|-------------------|
| `Vehicle` | `marca`, `modelo`, `preco` | `brand`, `model`, `price` |
| `Lead` | `nome`, `telefone`, `origem` | `name`, `phone`, `source` |
| `KanbanCard` | `coluna`, `vendedorNome` | `stage`, `assignedUserName` |
| `VehicleStatus` | `'disponivel'`, `'reservado'` | `AVAILABLE`, `RESERVED` |
| `LeadOrigin` | `'OLX'`, `'Instagram'` | `WEBMOTORS`, `MERCADO_LIVRE`, etc. |
| `KanbanColumn` | `'chatbot'`, `'aguardando'` | Stage enum from backend |

These types must be reconciled before API integration. Two approaches:
1. Change frontend types to match backend DTOs (recommended)
2. Create a mapper/adapter layer between API responses and frontend types

## Environment Configuration

**Required env vars (to be created):**

No `.env` files exist. No `import.meta.env` usage in the codebase. The following must be created:

```
VITE_API_URL=http://localhost:8080/api/v1     # Backend API base URL
VITE_APP_NAME=Astro LeadFlow                  # Application display name
VITE_ENABLE_MOCK=false                        # Toggle mock vs real API data
```

**Vite env type declaration** (`frontend/src/vite-env.d.ts`):
- Only contains default Vite client types reference
- Must be extended with `ImportMetaEnv` interface for custom env vars

## Third-Party Services (Backend-Side, Frontend Awareness Needed)

**WhatsApp Business API (Meta):**
- Backend handles all WhatsApp communication
- Frontend needs: chat UI component, message history display, send message form
- Credentials managed via: `PATCH /api/v1/organization/whatsapp`

**Marketplaces:**
- Webmotors - lead ingestion via webhooks (backend)
- Mercado Livre - lead ingestion + OAuth integration (backend)
- Frontend needs: marketplace configuration UI, usage tracking display

**AWS Services (via LocalStack in dev):**
- S3 - vehicle photo storage
- SQS - message queues
- SNS - notifications
- Frontend needs: photo upload UI, possibly pre-signed URL handling

## CI/CD & Deployment

**Hosting:** Not configured
**CI Pipeline:** Not configured
**Docker:** Backend has `Dockerfile`, frontend does not

## Monitoring & Observability

**Error Tracking:** None (Sentry DSN env var planned but not implemented)
**Logs:** `console.*` only (no structured logging)
**Analytics:** None (GA ID env var planned but not implemented)
**Backend:** Spring Boot Actuator enabled, OpenAPI/Swagger available

## Webhooks & Callbacks

**Incoming (backend handles):**
- Meta WhatsApp webhook (`MetaWebhookController.kt`)
- Marketplace webhooks (`MarketplaceWebhookController.kt`, `ScraperWebhookController.kt`)
- Mercado Livre OAuth callback (`MercadoLivreAuthController.kt`)

**Outgoing (backend handles):**
- WhatsApp messages via Meta API
- Email via SMTP (MailHog in dev)
- AWS SQS/SNS events

**Frontend impact:** None direct, but frontend must display real-time data from these sources (consider WebSocket or polling for live updates).

## Integration Roadmap (Priority Order)

**Phase 1 - Foundation:**
1. Install axios, create API client at `src/lib/axios.ts`
2. Create `.env.development` with `VITE_API_URL`
3. Create auth service + hooks (`src/services/auth.service.ts`, `src/hooks/useAuth.ts`)
4. Add JWT token handling (interceptors)
5. Add route guards (`AuthGuard` component)

**Phase 2 - Core CRM:**
1. Create lead service + hooks (replace `mockLeads`, `mockKanbanCards`)
2. Create vehicle service + hooks (replace `mockVehicles`)
3. Create dashboard service + hooks (replace `mockKPIData`, `mockMonthlySales`, etc.)
4. Reconcile type definitions with backend DTOs

**Phase 3 - Communication + SaaS:**
1. Create chat service (WhatsApp message history + send)
2. Create subscription/plan service
3. Create marketplace config service

---

*Integration audit: 2026-03-26*
