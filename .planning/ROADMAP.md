# Roadmap: Astro LeadFlow Frontend

**Created:** 2026-03-27
**Milestone:** v1.0 — Full Frontend Integration
**Granularity:** Standard
**Coverage:** 56/56 v1 requirements mapped

---

## Phases

- [x] **Phase 0: Landing Page + Route Restructuring** - Public institutional landing page and `/app/*` route namespace
- [ ] **Phase 1: Auth + API Foundation + Layout Shell** - Real JWT authentication, API client, layout, and infrastructure
- [ ] **Phase 01.1: Bug Fix + Security Hardening (INSERTED)** - Fix audit bugs, security vulnerabilities, API contract divergences
- [ ] **Phase 2: Core CRM** - Dashboard, leads CRUD, Kanban pipeline, and vehicle catalog connected to backend
- [ ] **Phase 3: Communication + SaaS** - WhatsApp templates, subscription management, marketplace usage
- [ ] **Phase 4: Responsiveness + UX Polish** - Systematic responsive design and interaction quality across all screens
- [ ] **Phase 5: Testing + Performance** - Test coverage, E2E flows, and accessibility audit

---

## Phase Details

### Phase 0: Landing Page + Route Restructuring
**Goal**: Visitors can discover the product through a professional public landing page, and all app routes are restructured under `/app/*` to prevent conflicts
**Depends on**: Nothing (first phase, no backend dependency)
**Requirements**: LAND-01, LAND-02, LAND-03, LAND-04, LAND-05, LAND-06, LAND-07, LAND-08, LAND-09, LAND-10, INFR-05
**Success Criteria** (what must be TRUE):
  1. Visitor lands on `/` and sees the full institutional landing page with hero, features, pricing, and testimonials
  2. Visitor can toggle between monthly and annual pricing on the 4-plan pricing section
  3. Visitor can click "Comece gratis" or "Agendar demonstracao" CTAs and reach the login/demo flow
  4. Landing page Lighthouse Performance score is 90 or above when measured
  5. All authenticated app routes are accessible under `/app/*` namespace without route conflicts
**Plans**: 5 plans
Plans:
- [x] 00-01-PLAN.md — Install react-helmet-async, restructure routes to /app/*, create AppLayout
- [x] 00-02-PLAN.md — LandingLayout, LandingNavbar (scroll-aware + mobile Sheet), LandingFooter, AstroLogo
- [x] 00-03-PLAN.md — HeroSection, SocialProofBar, ProblemSolutionSection (top sections)
- [x] 00-04-PLAN.md — FeaturesGrid, FeatureHighlight, IntegrationsSection (middle sections)
- [x] 00-05-PLAN.md — PricingSection, Testimonials, CTASection, LandingPage assembly, SEO Helmet, index.html
**UI hint**: yes
**Status**: COMPLETE (completed outside GSD workflow — no SUMMARY files; evidence: git commit 905038d, all 15 components exist)

### Phase 1: Auth + API Foundation + Layout Shell
**Goal**: Users can securely log in, the app connects to real backend APIs, and the authenticated layout shell is in place
**Depends on**: Phase 0 (route structure)
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, INFR-01, INFR-02, INFR-03, INFR-04, INFR-06, INFR-07, INFR-08
**Success Criteria** (what must be TRUE):
  1. User can log in with email and password and reach the dashboard; session survives browser refresh
  2. User can log out and is redirected to login; all tokens and React Query cache are cleared
  3. Unauthenticated user visiting `/app/dashboard` is redirected to login; authenticated user visiting `/app/login` is redirected to dashboard
  4. The sidebar collapses to a hamburger menu on mobile and expands correctly on larger screens
  5. A network request to any protected endpoint automatically attaches the JWT Authorization header
**Plans**: TBD
**UI hint**: yes
**Status**: ~90% COMPLETE (outside GSD workflow — AUTH-06 role-gating partial; React Query cache clear on logout unverified)

### Phase 01.1: Bug Fix + Security Hardening (INSERTED)

**Goal:** Fix all audit bugs (BUG-001 through BUG-008), resolve security vulnerabilities (SEC-001 token→httpOnly/memory, SEC-002 RBAC RoleGate, SEC-005 CSP), correct API contract divergences (dashboard/leads/profile/logout/register endpoints), and verify mock flag functionality — ensuring Phase 2 wiring builds on correct foundations
**Requirements**: AUTH-06 (partial from Phase 1), SEC-001, SEC-002, SEC-005, BUG-001–BUG-008
**Depends on:** Phase 1
**Plans:** 2 plans

Plans:
- [ ] 01.1-01-PLAN.md — Token security: migrate localStorage to in-memory, refresh interceptor, cache clear on logout, orgId query keys
- [ ] 01.1-02-PLAN.md — Auth UI fixes: remove public registration, fix landing CTAs, create RoleGate, add CSP

### Phase 2: Core CRM
**Goal**: Users can manage the full lead lifecycle — from dashboard overview through lead CRUD, Kanban pipeline, and vehicle catalog — all powered by real backend data
**Depends on**: Phase 1 (auth + API)
**Requirements**: DASH-01, DASH-02, DASH-03, DASH-04, LEAD-01, LEAD-02, LEAD-03, LEAD-04, LEAD-05, KANB-01, KANB-02, KANB-03, KANB-04, VEHI-01, VEHI-02, VEHI-03
**Success Criteria** (what must be TRUE):
  1. Dashboard shows live KPI cards (lead count, conversion rate, response time, revenue) fetched from the backend
  2. User can create a new lead via validated form and see it appear in the leads list without page refresh
  3. User can drag a lead card from one Kanban column to another and the stage change is persisted to the backend
  4. User can search and filter the leads list by stage, source, date range, and assigned user
  5. User can browse the vehicle catalog with search and filter, and view a vehicle detail page
**Plans**: TBD
**UI hint**: yes
**Status**: Infrastructure ready (service layer + React Query hooks exist), but pages still import mockData directly. Next work: wire pages.

### Phase 3: Communication + SaaS
**Goal**: Users can send WhatsApp messages using templates, and admins can view subscription plans, current usage, and marketplace quota
**Depends on**: Phase 2 (leads page needed for communication context)
**Requirements**: COMM-01, COMM-02, COMM-03, SAAS-01, SAAS-02, SAAS-03
**Success Criteria** (what must be TRUE):
  1. User can view the list of WhatsApp message templates fetched from the backend
  2. User can select a template, compose a message, send it, and see the delivery status result
  3. User can view their current subscription plan with usage metrics (leads used vs limit)
  4. Admin can view the marketplace quota dashboard showing consumption vs available quota per marketplace
**Plans**: TBD
**UI hint**: yes

### Phase 4: Responsiveness + UX Polish
**Goal**: Every screen in the application is consistently responsive across all four breakpoints and meets the interaction quality bar (hover states, loading states, empty states, error states, accessibility)
**Depends on**: Phase 2 (core screens must exist before systematic polish)
**Requirements**: UX-01, UX-02, UX-03, UX-04, UX-05, UX-06, UX-07
**Success Criteria** (what must be TRUE):
  1. Every page renders correctly at 375px, 768px, 1024px, and 1440px without overflow or broken layouts
  2. Every clickable element shows a pointer cursor and a visible hover transition within 150-300ms
  3. Every async data fetch shows a loading skeleton or spinner, and every API failure shows a user-friendly error message
  4. Every list and table shows a descriptive empty state when no data is returned
  5. All animations and transitions are suppressed when `prefers-reduced-motion: reduce` is set
**Plans**: TBD
**UI hint**: yes

### Phase 5: Testing + Performance
**Goal**: The application has reliable automated test coverage, critical user flows are validated end-to-end, and no accessibility critical violations exist
**Depends on**: Phase 4 (stable, polished UI to test against)
**Requirements**: TEST-01, TEST-02, TEST-03
**Success Criteria** (what must be TRUE):
  1. Running `npm run test:coverage` reports Vitest unit test coverage at 80% or above
  2. E2E tests for login flow, lead creation/edit, and Kanban drag-and-drop pass consistently in Playwright
  3. Running axe-core accessibility audit against all main pages returns zero critical violations
**Plans**: TBD
**UI hint**: no

---

## Progress Table

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 0. Landing Page + Route Restructuring | 5/5 | Done (outside GSD) | 2026-03-28 |
| 1. Auth + API Foundation + Layout Shell | 0/? | ~90% done (outside GSD, AUTH-06 partial) | - |
| 01.1. Bug Fix + Security Hardening (INSERTED) | 0/2 | Planned | - |
| 2. Core CRM | 0/? | Infra ready, pages not wired | - |
| 3. Communication + SaaS | 0/? | Not started | - |
| 4. Responsiveness + UX Polish | 0/? | Not started | - |
| 5. Testing + Performance | 0/? | Not started | - |

---

## Coverage Map

| Requirement | Phase | Status |
|-------------|-------|--------|
| LAND-01 | Phase 0 | Done |
| LAND-02 | Phase 0 | Done |
| LAND-03 | Phase 0 | Done |
| LAND-04 | Phase 0 | Done |
| LAND-05 | Phase 0 | Done |
| LAND-06 | Phase 0 | Done |
| LAND-07 | Phase 0 | Done |
| LAND-08 | Phase 0 | Done |
| LAND-09 | Phase 0 | Done |
| LAND-10 | Phase 0 | Done |
| INFR-05 | Phase 0 | Done |
| AUTH-01 | Phase 1 | Done |
| AUTH-02 | Phase 1 | Done |
| AUTH-03 | Phase 1 | Done |
| AUTH-04 | Phase 1 | Done |
| AUTH-05 | Phase 1 | Done |
| AUTH-06 | Phase 1 | Partial |
| INFR-01 | Phase 1 | Done |
| INFR-02 | Phase 1 | Done |
| INFR-03 | Phase 1 | Done |
| INFR-04 | Phase 1 | Done |
| INFR-06 | Phase 1 | Done |
| INFR-07 | Phase 1 | Done |
| INFR-08 | Phase 1 | Done |
| DASH-01 | Phase 2 | Pending |
| DASH-02 | Phase 2 | Pending |
| DASH-03 | Phase 2 | Pending |
| DASH-04 | Phase 2 | Pending |
| LEAD-01 | Phase 2 | Pending |
| LEAD-02 | Phase 2 | Pending |
| LEAD-03 | Phase 2 | Pending |
| LEAD-04 | Phase 2 | Pending |
| LEAD-05 | Phase 2 | Pending |
| KANB-01 | Phase 2 | Pending |
| KANB-02 | Phase 2 | Pending |
| KANB-03 | Phase 2 | Pending |
| KANB-04 | Phase 2 | Pending |
| VEHI-01 | Phase 2 | Pending |
| VEHI-02 | Phase 2 | Pending |
| VEHI-03 | Phase 2 | Pending |
| COMM-01 | Phase 3 | Pending |
| COMM-02 | Phase 3 | Pending |
| COMM-03 | Phase 3 | Pending |
| SAAS-01 | Phase 3 | Pending |
| SAAS-02 | Phase 3 | Pending |
| SAAS-03 | Phase 3 | Pending |
| UX-01 | Phase 4 | Pending |
| UX-02 | Phase 4 | Pending |
| UX-03 | Phase 4 | Pending |
| UX-04 | Phase 4 | Pending |
| UX-05 | Phase 4 | Pending |
| UX-06 | Phase 4 | Pending |
| UX-07 | Phase 4 | Pending |
| TEST-01 | Phase 5 | Pending |
| TEST-02 | Phase 5 | Pending |
| TEST-03 | Phase 5 | Pending |

**Total mapped: 56/56**

---
*Created: 2026-03-27*
*Last updated: 2026-04-05 — Phase 01.1 planned: 2 plans in 1 wave*
