# Codebase Structure

**Analysis Date:** 2026-03-26

## Directory Layout

```
frontend/
├── public/                    # Static assets served as-is
│   ├── favicon.ico            # Browser tab icon
│   ├── placeholder.svg        # Placeholder image
│   └── robots.txt             # Search engine crawl rules
├── src/                       # Application source code
│   ├── components/            # React components (65 files)
│   │   ├── ui/                # shadcn/ui primitives (49 files)
│   │   ├── dashboard/         # Dashboard widgets (5 files)
│   │   ├── kanban/            # Kanban board components (3 files)
│   │   ├── layout/            # Page layout components (2 files)
│   │   ├── modals/            # Dialog/modal components (4 files)
│   │   └── NavLink.tsx        # Custom NavLink wrapper (1 file, orphaned at root)
│   ├── contexts/              # React Context providers (2 files)
│   ├── data/                  # Mock data (1 file)
│   ├── hooks/                 # Custom hooks (2 files)
│   ├── lib/                   # Utility functions (1 file)
│   ├── pages/                 # Route-level page components (6 files)
│   ├── test/                  # Test setup and tests (2 files)
│   ├── types/                 # TypeScript type definitions (1 file)
│   ├── App.tsx                # Root component with providers + routes
│   ├── main.tsx               # Application entry point
│   ├── index.css              # Global styles + CSS variables + Tailwind
│   └── vite-env.d.ts          # Vite type declarations
├── components.json            # shadcn/ui configuration
├── eslint.config.js           # ESLint configuration
├── index.html                 # HTML shell (entry point for Vite)
├── package.json               # Dependencies and scripts
├── package-lock.json          # Dependency lockfile
├── postcss.config.js          # PostCSS config (Tailwind plugin)
├── tailwind.config.ts         # Tailwind CSS configuration + design tokens
├── tsconfig.json              # TypeScript project references
├── tsconfig.app.json          # TypeScript app config (strict: false)
├── tsconfig.node.json         # TypeScript config for Node tooling
├── vite.config.ts             # Vite bundler configuration
└── vitest.config.ts           # Vitest test runner configuration
```

## Directory Purposes

**`src/components/ui/` (49 files):**
- Purpose: shadcn/ui component library -- generated primitives, do NOT edit manually
- Contains: Radix-based UI components (accordion, alert, badge, button, card, calendar, carousel, chart, checkbox, collapsible, command, context-menu, dialog, drawer, dropdown-menu, form, hover-card, input, input-otp, label, menubar, navigation-menu, pagination, popover, progress, radio-group, resizable, scroll-area, select, separator, sheet, sidebar, skeleton, slider, sonner, switch, table, tabs, textarea, toast, toaster, toggle, toggle-group, tooltip, use-toast)
- Key files: `button.tsx`, `dialog.tsx`, `input.tsx`, `select.tsx`, `card.tsx`, `form.tsx`
- Add new: Run `npx shadcn@latest add [component-name]`

**`src/components/dashboard/` (5 files):**
- Purpose: Dashboard page widgets (KPI cards, charts, tables)
- Contains:
  - `KPICard.tsx` -- Reusable metric card with icon, value, trend indicator
  - `SalesLineChart.tsx` -- Recharts line chart for monthly sales
  - `CategoryBarChart.tsx` -- Recharts bar chart for category distribution
  - `StockTable.tsx` -- Vehicle stock summary table
  - `RecentSalesTable.tsx` -- Recent sales transaction table

**`src/components/kanban/` (3 files):**
- Purpose: Kanban board for lead pipeline management
- Contains:
  - `KanbanBoard.tsx` -- Board container with DragDropContext, owns card state
  - `KanbanColumn.tsx` -- Droppable column with header and card list
  - `KanbanCard.tsx` -- Draggable card with lead info, tags, action buttons

**`src/components/layout/` (2 files):**
- Purpose: Page-level layout components
- Contains:
  - `MainLayout.tsx` -- Shell wrapper (Header + main content area)
  - `Header.tsx` -- Top navigation bar with nav links, notifications popover, profile dropdown

**`src/components/modals/` (4 files):**
- Purpose: Modal dialog forms for CRUD operations
- Contains:
  - `CardDetailModal.tsx` -- Lead detail editor (React Hook Form + Zod, collapsible sections)
  - `AddVehicleModal.tsx` -- Vehicle creation form (with photo upload preview)
  - `AddUserModal.tsx` -- User creation form (with password validation)
  - `EditUserModal.tsx` -- User edit form (name, email, role)

**`src/pages/` (6 files):**
- Purpose: Route-level components, one per route
- Contains:
  - `Dashboard.tsx` -- KPI grid + charts + tables (all mock data)
  - `Kanban.tsx` -- Kanban board page wrapper
  - `Estoque.tsx` -- Vehicle inventory with search, filter, table, add modal
  - `Configuracoes.tsx` -- Settings page with tabbed sections (General, Notifications, Security, Payments)
  - `Login.tsx` -- Login/Register form (visual only, no auth)
  - `NotFound.tsx` -- 404 page

**`src/contexts/` (2 files):**
- Purpose: React Context providers for global state
- Contains:
  - `ThemeContext.tsx` -- Theme mode (light/dark/system) + accent color, persisted to localStorage
  - `NotificationContext.tsx` -- In-memory notification state with CRUD operations

**`src/data/` (1 file):**
- Purpose: Mock data that simulates backend API responses
- Contains:
  - `mockData.ts` -- All mock data: vehicles (8), leads (7), kanban cards (7), KPIs, monthly sales, category sales, recent sales

**`src/hooks/` (2 files):**
- Purpose: Custom React hooks
- Contains:
  - `use-mobile.tsx` -- `useIsMobile()` hook using matchMedia (breakpoint: 768px)
  - `use-toast.ts` -- Radix-based toast state machine (shadcn/ui generated, duplicate with Sonner)

**`src/lib/` (1 file):**
- Purpose: Shared utility functions
- Contains:
  - `utils.ts` -- `cn()` function (clsx + tailwind-merge for conditional class merging)

**`src/types/` (1 file):**
- Purpose: TypeScript type definitions
- Contains:
  - `index.ts` -- All domain types: Vehicle, Lead, KanbanCard, KanbanColumn, Seller, KPIData, MonthlySalesData, CategorySalesData, RecentSale, Notification, ThemeMode, AccentColor, VehicleStatus, VehicleCategory, LeadOrigin, CardTag

**`src/test/` (2 files):**
- Purpose: Test infrastructure and test files
- Contains:
  - `setup.ts` -- Jest DOM matchers + matchMedia mock
  - `example.test.ts` -- Single placeholder test (`expect(true).toBe(true)`)

## Key File Locations

**Entry Points:**
- `src/main.tsx`: React DOM render entry point
- `src/App.tsx`: Root component (providers, routing, toasters)
- `index.html`: HTML shell with `<div id="root">`

**Configuration:**
- `vite.config.ts`: Vite config (port 3000, `@` alias to `./src`)
- `tsconfig.app.json`: TypeScript config (`strict: false`, path alias `@/*`)
- `tailwind.config.ts`: Tailwind + design tokens (kanban, status, chart, trend colors)
- `components.json`: shadcn/ui config (style: default, baseColor: slate)
- `src/index.css`: CSS variables (light/dark themes), custom animations

**Core Logic:**
- `src/components/kanban/KanbanBoard.tsx`: Kanban board state management + drag-and-drop
- `src/components/layout/Header.tsx`: Navigation + notifications + profile menu
- `src/pages/Configuracoes.tsx`: Settings with 4 tab sections (588 lines -- largest page)
- `src/data/mockData.ts`: All application data (166 lines)

**Types:**
- `src/types/index.ts`: All domain type definitions (114 lines)

**Testing:**
- `src/test/setup.ts`: Test setup with jest-dom
- `src/test/example.test.ts`: Placeholder test
- `vitest.config.ts`: Vitest configuration

## File Counts Summary

| Directory | Files | Description |
|-----------|-------|-------------|
| `src/components/ui/` | 49 | shadcn/ui primitives |
| `src/pages/` | 6 | Route-level pages |
| `src/components/dashboard/` | 5 | Dashboard widgets |
| `src/components/modals/` | 4 | Modal dialogs |
| `src/components/kanban/` | 3 | Kanban board |
| `src/components/layout/` | 2 | Layout components |
| `src/contexts/` | 2 | Context providers |
| `src/hooks/` | 2 | Custom hooks |
| `src/test/` | 2 | Test files |
| `src/components/` (root) | 1 | NavLink.tsx |
| `src/data/` | 1 | Mock data |
| `src/lib/` | 1 | Utilities |
| `src/types/` | 1 | Type definitions |
| `src/` (root) | 4 | App, main, CSS, env.d.ts |
| **Total** | **83** | |

## Naming Conventions

**Files:**
- Components: `PascalCase.tsx` (e.g., `KPICard.tsx`, `MainLayout.tsx`, `KanbanBoard.tsx`)
- Hooks: `kebab-case.tsx` or `.ts` (e.g., `use-mobile.tsx`, `use-toast.ts`)
- Types: `camelCase.ts` barrel (e.g., `index.ts`)
- Data: `camelCase.ts` (e.g., `mockData.ts`)
- Utilities: `camelCase.ts` (e.g., `utils.ts`)
- CSS: `camelCase.css` (e.g., `index.css`)
- Config files: `kebab-case` at root (e.g., `vite.config.ts`, `tailwind.config.ts`)

**Directories:**
- Feature groups: `kebab-case` (e.g., `dashboard`, `kanban`, `modals`, `layout`)
- Functional groups: `kebab-case` (e.g., `contexts`, `hooks`, `types`, `data`, `lib`, `pages`, `test`)

**Components:**
- Named exports: `export function ComponentName()` (most feature components)
- Default exports: `export default function PageName()` (page components only)
- shadcn/ui: Follows its own convention with `forwardRef` and `React.ComponentPropsWithoutRef`

**Import Aliases:**
- `@/` maps to `src/` (configured in `vite.config.ts` and `tsconfig.app.json`)
- All internal imports use `@/` prefix (e.g., `@/components/ui/button`, `@/lib/utils`)

## Where to Add New Code

**New Page:**
- Create page component in `src/pages/NewPage.tsx` (default export)
- Add route in `src/App.tsx` inside `<Routes>`
- Wrap page content with `<MainLayout>` for authenticated pages
- Add nav item in `src/components/layout/Header.tsx` `navItems` array

**New Feature Component:**
- Create directory `src/components/[feature-name]/`
- Add component files with named exports
- Import from pages as needed

**New Modal/Dialog:**
- Add in `src/components/modals/NewModal.tsx`
- Use shadcn/ui `Dialog` + React Hook Form + Zod pattern (see `CardDetailModal.tsx` as reference)

**New shadcn/ui Component:**
- Run: `npx shadcn@latest add [component-name]`
- Auto-added to `src/components/ui/`
- Do NOT manually create files in `ui/`

**New API Service (when created):**
- Create `src/services/` directory
- Add service files as `[domain].service.ts` (e.g., `leads.service.ts`)
- Create API client in `src/lib/axios.ts`
- Create React Query hooks in `src/hooks/use[Domain].ts`

**New React Query Hook (when API layer exists):**
- Create in `src/hooks/use[Domain].ts`
- Follow pattern: `useQuery` for reads, `useMutation` for writes
- Invalidate queries on mutation success

**New Context:**
- Create in `src/contexts/NewContext.tsx`
- Follow pattern from `ThemeContext.tsx`: createContext + Provider + custom hook
- Add Provider to the provider tree in `src/App.tsx`

**New Type Definition:**
- Add to `src/types/index.ts` (single barrel file)
- When types grow, consider splitting into `src/types/[domain].types.ts`

**New Utility Function:**
- Add to `src/lib/utils.ts` or create new file in `src/lib/` (e.g., `src/lib/formatters.ts`)

**New Test:**
- Co-locate with source: `src/components/[feature]/Component.test.tsx`
- Or add to `src/test/` for integration tests
- Test setup is in `src/test/setup.ts`

**Landing Page Components (planned):**
- Create `src/components/landing/` directory
- Add landing-specific components (LandingLayout, HeroSection, etc.)
- Landing page at `src/pages/LandingPage.tsx`
- Landing uses its own layout (no MainLayout, no sidebar)

## Special Directories

**`src/components/ui/`:**
- Purpose: shadcn/ui generated components
- Generated: Yes (via `npx shadcn@latest add`)
- Committed: Yes
- Do not edit manually -- updates via shadcn CLI will overwrite changes

**`node_modules/`:**
- Purpose: npm dependencies
- Generated: Yes (`npm install`)
- Committed: No (.gitignore)

**`public/`:**
- Purpose: Static assets served at root URL
- Generated: No
- Committed: Yes
- Contains: favicon.ico, placeholder.svg, robots.txt

## Directories That Do NOT Exist Yet (Planned)

These directories are referenced in CLAUDE.md as planned but do not exist:

| Planned Directory | Purpose | Priority |
|-------------------|---------|----------|
| `src/services/` | API service layer (axios calls) | Phase 1 |
| `src/lib/axios.ts` | Configured axios instance | Phase 1 |
| `src/components/auth/` | LoginForm, AuthGuard, RoleGate | Phase 1 |
| `src/components/shared/` | LoadingSpinner, EmptyState, ErrorBoundary | Phase 1 |
| `src/components/landing/` | Landing page sections | Phase 0 |
| `src/components/vehicles/` | Vehicle detail components | Phase 2 |
| `src/components/communication/` | WhatsApp template components | Phase 3 |
| `src/components/saas/` | Plan/billing components | Phase 3 |

---

*Structure analysis: 2026-03-26*
