# Phase 4: Responsiveness + UX Polish - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-10
**Phase:** 04-responsiveness-ux-polish
**Areas discussed:** Mobile navigation, Table mobile strategy, Loading skeleton depth, Kanban mobile layout

---

## Mobile Navigation

| Option | Description | Selected |
|--------|-------------|----------|
| Hamburger + Sheet drawer | Sheet from left with all nav items; uses shadcn/ui Sheet component | ✓ |
| Abbreviate to icons only | Collapse nav to icon-only buttons on small screens | |
| Bottom tab bar | Fixed bottom navigation bar, requires Layout refactor | |

**User's choice:** Hamburger + Sheet drawer
**Notes:** Breakpoint at md: (768px) — below that hamburger shows, above that inline nav shows. Clicking a nav item closes the Sheet and navigates. No notification badge on the hamburger (Bell icon already shows count).

---

## Table Mobile Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Card-per-row view at mobile | Each row becomes a vertical card below md: | ✓ |
| Horizontal scroll container | overflow-x-auto wrapper, user scrolls sideways | |
| Progressive column hiding | Hide secondary columns at smaller breakpoints | |

**User's choice:** Card-per-row view at mobile
**Notes:** Leads card shows Name + Status badge + Source + Vehicle interest + action. Estoque follows the same card pattern with vehicle-appropriate fields. Breakpoint is md: (768px).

---

## Loading Skeleton Depth

| Option | Description | Selected |
|--------|-------------|----------|
| Skeletons for structured, spinners for simple | Dashboard KPI cards + Leads/Estoque tables get skeletons; Kanban + others keep spinner | ✓ |
| Spinners everywhere | Keep current Loader2 full-screen spinner on all pages | |
| Skeletons for everything | Every page gets skeleton screens | |

**User's choice:** Skeletons for structured content, spinners for simple
**Notes:** 8 skeleton rows for tables (not 5 — user chose 8). Dashboard gets 4 KPI skeleton cards. Kanban keeps spinner. Comunicação and Configurações keep spinner.

---

## Kanban Mobile Layout

| Option | Description | Selected |
|--------|-------------|----------|
| Keep horizontal scroll | Current behavior + scroll hint gradient | ✓ |
| Single-column with column switcher | One column shown at a time, dropdown to switch | |

**User's choice:** Keep horizontal scroll
**Notes:** Column widths change from fixed to `min-w-[260px]` (adaptive). Add right-edge gradient fade to hint at scrollability on mobile. No single-column refactor needed.

---
