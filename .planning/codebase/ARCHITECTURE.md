# Architecture

**Analysis Date:** 2026-03-26

## Pattern Overview

**Overall:** Single-Page Application (SPA) with React, flat routing, mock-data-driven

**Key Characteristics:**
- React 18 SPA with Vite as bundler (SWC plugin for fast transforms)
- Flat routing via React Router DOM v6 (no nested layouts via router)
- All data sourced from `src/data/mockData.ts` -- zero API calls to backend
- Layout composition via `MainLayout` wrapper component inside each page
- State management through React Context (Theme, Notifications) and local `useState`
- React Query (`QueryClientProvider`) is configured but unused -- no queries or mutations exist
- Component hierarchy: App (providers) -> BrowserRouter -> Pages -> MainLayout -> Feature Components

## Provider Tree

The application wraps all routes in a provider stack defined in `src/App.tsx`:

```
QueryClientProvider          # TanStack React Query (configured, NOT used)
  └── ThemeProvider          # Theme mode (light/dark/system) + accent color
       └── NotificationProvider  # In-memory notification state (hardcoded seed data)
            └── TooltipProvider  # shadcn/ui tooltip context
                 ├── Toaster     # Radix-based toast (shadcn/ui)
                 ├── Sonner      # Sonner-based toast (DUPLICATE)
                 └── BrowserRouter
                      └── Routes
```

**Files:**
- Provider setup: `src/App.tsx`
- Theme context: `src/contexts/ThemeContext.tsx`
- Notification context: `src/contexts/NotificationContext.tsx`

## Routing

**Router:** React Router DOM v6 with `BrowserRouter` and flat `<Routes>`

**Route Table:**

| Path | Component | Layout | Auth Required |
|------|-----------|--------|---------------|
| `/` | `Kanban` | `MainLayout` | No (should be) |
| `/dashboard` | `Dashboard` | `MainLayout` | No (should be) |
| `/estoque` | `Estoque` | `MainLayout` | No (should be) |
| `/configuracoes` | `Configuracoes` | `MainLayout` | No (should be) |
| `/login` | `Login` | None (standalone) | No |
| `*` | `NotFound` | None (standalone) | No |

**Routing Gaps:**
- No route guards or authentication checks
- No lazy loading (`React.lazy`) -- all pages eagerly imported
- No nested route layouts via React Router `<Outlet>`
- Login page navigates to `/` on submit with no actual auth
- Sign-out in Header navigates to `/login` with no token clearing (no tokens exist)
- No `/app/*` prefix separation between public and authenticated routes

**Route definition:** `src/App.tsx` (lines 26-31)

## Layers

**Pages Layer:**
- Purpose: Route-level components that compose the full page view
- Location: `src/pages/`
- Contains: 6 page components (Dashboard, Kanban, Estoque, Configuracoes, Login, NotFound)
- Depends on: MainLayout, feature components, mockData
- Pattern: Each page wraps content in `<MainLayout>` (except Login and NotFound)
- State: Local `useState` for UI state (filters, modals, form data)

**Layout Layer:**
- Purpose: Shared page chrome (header navigation)
- Location: `src/components/layout/`
- Contains: `MainLayout.tsx` (shell), `Header.tsx` (top navbar with nav, notifications, profile)
- Pattern: `MainLayout` renders `Header` + `<main>` with children. No sidebar component.
- Navigation items defined as array in `Header.tsx` (lines 23-28)

**Feature Components Layer:**
- Purpose: Domain-specific UI components grouped by feature
- Location: `src/components/dashboard/`, `src/components/kanban/`, `src/components/modals/`
- Contains: KPI cards, charts, tables, kanban board/column/card, modal dialogs
- Depends on: shadcn/ui primitives, types, mockData (via props from pages), contexts
- Pattern: Named exports, typed props interfaces, data received via props from parent pages

**UI Primitives Layer:**
- Purpose: shadcn/ui component library (generated, not hand-written)
- Location: `src/components/ui/`
- Contains: 49 component files (accordion, alert, badge, button, card, dialog, form, input, select, etc.)
- Config: `components.json` at project root (style: default, baseColor: slate, cssVariables: true)
- Usage: Import from `@/components/ui/[component]`

**Contexts Layer:**
- Purpose: Global state via React Context API
- Location: `src/contexts/`
- Contains: ThemeContext (theme mode + accent color), NotificationContext (in-memory notifications)
- Pattern: createContext + Provider component + custom hook (`useTheme`, `useNotifications`)
- Persistence: Theme stored in `localStorage`, notifications are ephemeral (in-memory with seed data)

**Data Layer:**
- Purpose: Mock data that simulates backend responses
- Location: `src/data/mockData.ts`
- Contains: All mock data (vehicles, leads, kanban cards, KPIs, sales, charts)
- Pattern: Exported constants consumed directly by pages
- **Critical gap:** No `src/services/` directory exists. No API client. No HTTP calls.

**Types Layer:**
- Purpose: TypeScript type definitions for the domain model
- Location: `src/types/index.ts`
- Contains: Vehicle, Lead, KanbanCard, KanbanColumn, Seller, KPIData, Notification, ThemeMode, AccentColor
- Pattern: Single barrel file with all types. Mix of `interface` and `type` declarations.
- **Note:** Types use Portuguese field names (e.g., `nome`, `telefone`, `preco`) and do NOT align with backend DTOs

**Utilities Layer:**
- Purpose: Shared utility functions
- Location: `src/lib/utils.ts`
- Contains: Single `cn()` function (clsx + tailwind-merge)
- No other utility files exist

**Hooks Layer:**
- Purpose: Custom React hooks
- Location: `src/hooks/`
- Contains: `use-mobile.tsx` (responsive breakpoint detection), `use-toast.ts` (Radix toast state machine)
- **Note:** `use-toast.ts` is shadcn/ui generated code for the Radix-based toast system (duplicate with Sonner)

## Data Flow

**Page Data Loading (current -- mock only):**

1. Page component imports mock data directly from `src/data/mockData.ts`
2. Mock data stored in local `useState` (e.g., `const [vehicles, setVehicles] = useState(mockVehicles)`)
3. User actions mutate local state (add vehicle, move kanban card, etc.)
4. Changes are lost on page refresh (no persistence)

**Kanban Card Drag Flow:**

1. User drags card in `KanbanBoard` (`src/components/kanban/KanbanBoard.tsx`)
2. `@hello-pangea/dnd` `DragDropContext.onDragEnd` fires with source/destination
3. `handleDragEnd` updates `cards` state by changing `coluna` property
4. Notification added via `NotificationContext.addNotification`
5. Sonner toast displayed with success message

**Form Submission Flow (modals):**

1. Form uses React Hook Form + Zod resolver for validation
2. On submit, validated data passed to parent callback (e.g., `onSubmit`, `onUpdate`)
3. Parent page updates local state with new/modified data
4. Sonner toast displayed with success message
5. Modal closes

**Theme Change Flow:**

1. User selects theme in `Configuracoes` page or uses context directly
2. `ThemeContext.setTheme()` updates state and persists to `localStorage`
3. `useEffect` in ThemeProvider toggles `dark` class on `document.documentElement`
4. CSS variables in `src/index.css` `:root` vs `.dark` provide themed values
5. Accent color stored as `data-accent` attribute on root element

**State Management:**
- **Global state:** React Context only (Theme + Notifications)
- **Page state:** Local `useState` in each page component
- **Form state:** React Hook Form per modal/form
- **Server state:** None (React Query configured but unused)
- **URL state:** Not used (no query params for filters, pagination, etc.)

## Key Abstractions

**MainLayout:**
- Purpose: Wraps authenticated page content with Header and main area
- File: `src/components/layout/MainLayout.tsx`
- Pattern: Simple children wrapper (`<Header /> + <main>{children}</main>`)
- Used by: All pages except Login and NotFound

**KanbanBoard Composition:**
- Purpose: Full Kanban board with drag-and-drop
- Files: `src/components/kanban/KanbanBoard.tsx`, `KanbanColumn.tsx`, `KanbanCard.tsx`
- Pattern: Board manages state + DragDropContext -> Column wraps Droppable -> Card wraps Draggable
- State: Board owns all card state; columns and cards are presentational with callbacks

**Modal Pattern:**
- Purpose: CRUD forms for leads, vehicles, users
- Files: `src/components/modals/CardDetailModal.tsx`, `AddVehicleModal.tsx`, `AddUserModal.tsx`, `EditUserModal.tsx`
- Pattern: shadcn/ui `Dialog` + React Hook Form + Zod schema + `onSubmit` callback to parent
- Validation: Zod schemas defined inline in each modal file

**KPICard:**
- Purpose: Reusable metric display card
- File: `src/components/dashboard/KPICard.tsx`
- Pattern: Accepts `title`, `value`, `trend`, `icon` (Lucide component), `iconBgColor` as props

## Entry Points

**Application Entry:**
- Location: `src/main.tsx`
- Triggers: Vite dev server or production build
- Responsibilities: Mounts `<App />` to `#root` DOM element, imports global CSS

**App Root:**
- Location: `src/App.tsx`
- Triggers: React render
- Responsibilities: Provider tree setup, route definitions, toast renderers

**HTML Shell:**
- Location: `index.html`
- Note: Title is "V3 Motors Hub" (not "Astro LeadFlow" -- needs updating)
- Lang: `pt-BR`

## Error Handling

**Strategy:** Minimal -- no systematic error handling

**Patterns:**
- Form validation: Zod schemas with inline error messages displayed below inputs
- No `ErrorBoundary` component exists
- No API error handling (no API calls exist)
- `NotFound` page logs 404 to console.error
- No loading states for async operations (no async operations exist)
- Toast notifications for user feedback (success only, via Sonner `toast.success()`)

## Cross-Cutting Concerns

**Logging:** No structured logging. Single `console.error` in NotFound page. No error tracking (Sentry not configured).

**Validation:** Zod schemas in modal components only. No validation layer for API responses (no API layer exists).

**Authentication:** Not implemented. Login page is purely visual -- `handleSubmit` shows a toast and navigates to `/`. No JWT, no token storage, no auth guards.

**Theming:** Full light/dark mode support via CSS custom properties in `src/index.css`. Theme toggled via class `dark` on `<html>`. Accent color system exists (purple/blue/green/orange) but only affects theme context state -- actual accent color CSS variable mapping is unclear.

**Internationalization:** Not implemented. All UI text is in Brazilian Portuguese (hardcoded strings).

**Accessibility:** Minimal. shadcn/ui provides base a11y (ARIA attributes from Radix primitives). No custom a11y implementation. Focus management relies on Radix defaults.

**Responsive Design:** Partial. Some pages use Tailwind responsive prefixes (`sm:`, `lg:`). Header navbar has no mobile hamburger menu. Kanban board has horizontal scroll but no mobile-optimized view.

## Design Token System

Custom CSS variables extend shadcn/ui defaults. Defined in `src/index.css`:

- **Kanban colors:** `--kanban-chatbot`, `--kanban-waiting`, `--kanban-negotiation`, `--kanban-completed`
- **Vehicle status colors:** `--status-available`, `--status-reserved`, `--status-maintenance`, `--status-sold`
- **Chart colors:** `--chart-primary`, `--chart-secondary`, `--chart-tertiary`, `--chart-muted`
- **Trend colors:** `--trend-positive`, `--trend-negative`

These are consumed via Tailwind config in `tailwind.config.ts` (lines 53-74) and referenced as Tailwind classes (e.g., `bg-kanban-completed`, `text-status-available`).

---

*Architecture analysis: 2026-03-26*
