# Phase 4: Responsiveness + UX Polish - Context

**Gathered:** 2026-04-10
**Status:** Ready for planning

<domain>
## Phase Boundary

Every authenticated app screen (Dashboard, Leads, Kanban, Estoque, Comunicação, Configurações + the AppLayout header shell) receives systematic responsive layout at 375px/768px/1024px/1440px and consistent interaction quality — hover states, loading states, empty states, error states, and prefers-reduced-motion support.

**What this phase delivers:**
- Mobile-first responsive fixes on all authenticated pages
- Consistent loading/empty/error state patterns across the app
- Interaction quality baseline: hover states, cursor-pointer, focus-visible on all interactive elements
- prefers-reduced-motion support in app-layer animations (landing page already handles this)

**Out of scope:**
- New features or pages
- WhatsApp or SaaS functionality changes
- Performance optimization (Phase 5)
- E2E / accessibility audit (Phase 5)

</domain>

<decisions>
## Implementation Decisions

### Mobile Navigation
- **D-01:** At mobile (`< md: / < 768px`), the inline header nav is replaced by a hamburger icon (☰ button). Clicking it opens a shadcn/ui `Sheet` drawer from the left with all 6 nav items stacked vertically.
- **D-02:** At `md: (768px)` and above, the full inline nav is shown. Below 768px, the hamburger + Sheet pattern applies.
- **D-03:** Clicking any nav item in the Sheet navigates to the page AND closes the Sheet automatically.
- **D-04:** No notification badge on the hamburger button — the Bell icon (which stays visible on mobile in the header) already shows the unread count. Avoiding redundancy.

### Table Mobile Strategy (Leads + Estoque)
- **D-05:** Below `md: (768px)`, data tables transform into card-per-row view. Above `md:`, the full table is shown.
- **D-06:** Leads mobile card shows: **Name** (primary) + **Status badge** + **Source** + **Vehicle interest** + action button (Detalhes). This prioritizes sales pipeline context over contact details.
- **D-07:** Estoque mobile card follows the same card-per-row pattern with vehicle-appropriate fields (make/model, status, price).

### Loading State Depth
- **D-08:** Loading pattern split by content type:
  - **Dashboard KPI cards** → `Skeleton` placeholder cards (4 shimmer blocks matching KPI card dimensions)
  - **Leads/Estoque tables** → 8 skeleton rows (Skeleton shimmer for each table row cell)
  - **Kanban board** → keep current full-screen `Loader2` spinner (column count/structure varies, skeletons not worth the cost)
  - **Comunicação chat** → keep `Loader2` spinner (bubble structure too variable)
  - **Configurações** → keep `Loader2` spinner (simple form, low data volume)
- **D-09:** 8 skeleton rows for table loading — enough to fill desktop viewport, scrollable on mobile.

### Kanban Mobile Layout
- **D-10:** Keep horizontal scroll as the Kanban mobile pattern. Do not add a single-column view — the `overflow-x-auto` behavior is sufficient.
- **D-11:** Add a subtle right-edge gradient fade on the Kanban scroll container to hint at scrollability on mobile.
- **D-12:** Column widths change from fixed to adaptive: use `min-w-[260px]` so columns shrink slightly on mobile (fitting ~1.4 columns in 375px viewport), encouraging swipe discovery, while maintaining scroll behavior.

### Claude's Discretion
- Error state standardization: Claude can choose between inline error + retry button vs a shared `ErrorCard` component. Keep consistency across pages — pick one approach and apply it everywhere.
- Hover state sweep: Claude decides which elements need `cursor-pointer` / `transition-colors` additions.
- Empty state consistency: `EmptyState` component already exists — Claude decides where it's not yet used and adds it.
- prefers-reduced-motion: In app-layer CSS animations (e.g., `animate-fade-in`, `animate-slide-up` in `index.css`), add `@media (prefers-reduced-motion: reduce)` overrides. Pattern is already applied in landing page components — replicate for the app shell.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

No external specs — requirements fully captured in decisions above.

### Phase requirements (ROADMAP.md)
- `.planning/ROADMAP.md` §Phase 4 — UX-01 through UX-07 requirements and success criteria

### Prior phase patterns
- `.planning/phases/02-core-crm/02-CONTEXT.md` — established patterns for hooks, service layer, error/loading state conventions
- `.planning/phases/03-communication-saas/03-CONTEXT.md` — loading/error state baseline that Phase 4 polishes

### Design system constraints
- `frontend/src/index.css` — CSS custom properties + `animate-fade-in` / `animate-slide-up` animations that need prefers-reduced-motion guards
- `frontend/src/components/shared/EmptyState.tsx` — shared empty state component to use consistently
- `frontend/src/components/ui/skeleton.tsx` — Skeleton component (shadcn/ui) available for use
- `frontend/src/components/ui/sheet.tsx` — Sheet component (shadcn/ui) for hamburger drawer

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EmptyState` (`frontend/src/components/shared/EmptyState.tsx`) — icon + title + description + optional action button. Use this everywhere lists can be empty.
- `Skeleton` (`frontend/src/components/ui/skeleton.tsx`) — shimmer component, compose into skeleton layouts.
- `Sheet` (`frontend/src/components/ui/sheet.tsx`) — slide-in drawer for hamburger nav.
- `ErrorBoundary` (`frontend/src/components/layout/ErrorBoundary.tsx`) — React error boundary wrapping the app.
- `Loader2` from `lucide-react` — current spinner icon for loading states (keep for Kanban, Communication, Configurações).

### Established Patterns
- All pages use `isLoading / error / data` from React Query hooks — loading and error branches are already wired, just need visual upgrade.
- `cn()` from `@/lib/utils` — use for conditional Tailwind classes in responsive code.
- `tailwindcss-animate` already configured — `animate-spin`, `animate-fade-in`, etc. are available.
- `prefers-reduced-motion` pattern from landing: inline `style={{ animation: prefersReducedMotion ? 'none' : ... }}` — replicate or move to CSS media query in `index.css`.

### Integration Points
- `frontend/src/components/layout/Header.tsx` — add hamburger menu with Sheet and `md:hidden` / `hidden md:flex` breakpoint classes.
- `frontend/src/pages/Dashboard.tsx` — replace full-screen spinner with KPI skeleton cards.
- `frontend/src/pages/Leads.tsx` — replace spinner with 8 skeleton rows; add card-per-row view below md:.
- `frontend/src/pages/Estoque.tsx` — same skeleton + card-per-row pattern as Leads.
- `frontend/src/pages/Kanban.tsx` — update KanbanBoard column widths to `min-w-[260px]`, add right-fade gradient hint.
- `frontend/src/index.css` — add `@media (prefers-reduced-motion: reduce)` overrides for app-shell animations.

### Known Gaps
- `Header.tsx` has no mobile nav — 6 inline items will overflow on 375px.
- `Dashboard.tsx` loading state: full-screen spinner (no skeleton).
- `Leads.tsx` loading state: full-screen spinner; no mobile card view.
- `Estoque.tsx` loading state: full-screen spinner; no mobile card view.
- `prefers-reduced-motion` only handled in landing page components — not in app CSS.
- No consistent cursor-pointer / focus-visible audit has been done.

</code_context>

<specifics>
## Specific Ideas

- Kanban right-fade gradient: `<div className="absolute right-0 top-0 h-full w-8 bg-gradient-to-l from-background to-transparent pointer-events-none md:hidden" />` on the Kanban scroll wrapper — subtle scroll hint visible only on mobile.
- Hamburger breakpoint pattern: `<nav className="hidden md:flex ...">` for desktop nav, `<Button className="md:hidden ...">` for hamburger. Sheet uses `SheetContent side="left"`.
- Mobile card view pattern: `<div className="block md:hidden">` for cards, `<table className="hidden md:table">` for table — conditional render within the same component.

</specifics>

<deferred>
## Deferred Ideas

- Swipeable Kanban columns (carousel-style with snap scrolling) — more native mobile feel, but significant DnD library interaction complexity. Deferred to post-v1.
- Bottom tab bar navigation — more mobile-native pattern than hamburger Sheet. Deferred: requires Layout refactor and only shows 4-5 tabs (we have 6 nav items).
- Full accessibility audit (axe-core, screen reader testing) — explicitly scoped to Phase 5.
- E2E test coverage for responsive behavior — Phase 5.

</deferred>

---

*Phase: 04-responsiveness-ux-polish*
*Context gathered: 2026-04-10*
