<!-- GSD:project-start source:PROJECT.md -->
## Project

**Astro LeadFlow — Frontend**

Astro LeadFlow is a multi-tenant CRM SaaS for automotive dealerships, built to centralize leads from marketplaces (Webmotors, Mercado Livre), automate first-contact via WhatsApp (Meta API), and provide a visual Kanban pipeline for sales management. The backend (Kotlin + Spring Boot 3.2.5) is complete; this project covers the React frontend that must integrate with it and deliver the full user experience — starting with the public-facing landing page through the authenticated CRM application.

**Core Value:** **Every lead that enters the system must be visible, traceable, and actionable within seconds** — the frontend must connect to the real backend APIs and present a responsive, professional interface that dealership managers and salespeople can trust for their daily workflow.

### Constraints

- **Tech Stack**: React 18 + Vite + Tailwind + shadcn/ui — already established, no framework migration
- **Backend**: API contracts are fixed — frontend must adapt to existing DTOs and endpoints
- **Security**: JWT in httpOnly cookie or memory (never localStorage), RBAC enforcement
- **Performance**: Lighthouse ≥90 for landing page, lazy loading for app routes
- **Responsiveness**: Every screen must work at 375px, 768px, 1024px, 1440px
- **Design**: Soft UI Evolution style, blue accent (#2563EB), no emoji icons (Lucide SVG only)
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- TypeScript 5.8.3 - All frontend source code (`frontend/src/**/*.{ts,tsx}`)
- CSS (Tailwind utility classes + CSS custom properties in `frontend/src/index.css`)
## Runtime
- Node.js - version not pinned (no `.nvmrc` or `.node-version` file)
- Browser target: ES2020 (configured in `frontend/tsconfig.app.json` line 4)
- npm (inferred from `frontend/package-lock.json`)
- Lockfile: present (`frontend/package-lock.json`)
- No `.npmrc` file detected
## Frameworks
- React 18.3.1 - UI framework (`frontend/package.json` line 54)
- React DOM 18.3.1 - DOM rendering (`frontend/package.json` line 56)
- React Router DOM 6.30.1 - Client-side routing (`frontend/package.json` line 59)
- Vitest 3.2.4 - Test runner (`frontend/package.json` line 87)
- @testing-library/react 16.0.0 - Component testing utilities (`frontend/package.json` line 71)
- @testing-library/jest-dom 6.6.0 - DOM assertion matchers (`frontend/package.json` line 70)
- jsdom 20.0.3 - Browser environment for tests (`frontend/package.json` line 80)
- Vite 5.4.19 - Bundler and dev server (`frontend/package.json` line 86)
- @vitejs/plugin-react-swc 3.11.0 - React Fast Refresh via SWC (`frontend/package.json` line 75)
- PostCSS 8.5.6 - CSS processing (`frontend/package.json` line 82)
- Autoprefixer 10.4.21 - CSS vendor prefixing (`frontend/package.json` line 76)
## Key Dependencies
### UI Component Library
- Config: `frontend/components.json`
- Style: `default`
- Base color: `slate`
- CSS Variables: enabled
- RSC: disabled (`"rsc": false`)
- Aliases: `@/components/ui`, `@/lib/utils`, `@/hooks`
### Radix UI Primitives (underlying shadcn/ui)
- `@radix-ui/react-dialog` ^1.1.14
- `@radix-ui/react-dropdown-menu` ^2.1.15
- `@radix-ui/react-popover` ^1.1.14
- `@radix-ui/react-select` ^2.2.5
- `@radix-ui/react-tabs` ^1.1.12
- `@radix-ui/react-toast` ^1.2.14
### Critical Application Dependencies
- **@tanstack/react-query** 5.83.0 - Server state management
- **@hello-pangea/dnd** 18.0.1 - Drag-and-drop for Kanban board
- **react-hook-form** 7.61.1 - Form management
- **zod** 3.25.76 - Schema validation
- **recharts** 2.15.4 - Dashboard charts
- **lucide-react** 0.462.0 - Icon library
- **sonner** 1.7.4 - Toast notifications (primary)
- **date-fns** 3.6.0 - Date manipulation
### Styling Dependencies
- **tailwindcss** 3.4.17 - Utility-first CSS framework
- **tailwind-merge** 2.6.0 - Merge Tailwind classes without conflicts
- **tailwindcss-animate** 1.0.7 - Animation utilities for Tailwind
- **@tailwindcss/typography** 0.5.16 - Typography plugin (dev dependency)
- **class-variance-authority** 0.7.1 - Component variant management (used by shadcn/ui)
- **clsx** 2.1.1 - Conditional classname utility
### Utility Dependencies
- **cmdk** 1.1.1 - Command palette (shadcn command component)
- **embla-carousel-react** 8.6.0 - Carousel (shadcn carousel component)
- **input-otp** 1.4.2 - OTP input (shadcn input-otp component)
- **next-themes** 0.3.0 - Theme switching
- **react-day-picker** 8.10.1 - Date picker (shadcn calendar component)
- **react-resizable-panels** 2.1.9 - Resizable panels (shadcn resizable component)
- **vaul** 0.9.9 - Drawer component (shadcn drawer component)
## State Management
- `frontend/src/contexts/ThemeContext.tsx` - Theme mode (light/dark/system) + accent color
- `frontend/src/contexts/NotificationContext.tsx` - In-app notifications
## Routing
| Path | Component | Purpose |
|------|-----------|---------|
| `/` | `Kanban` | Kanban board (default page) |
| `/dashboard` | `Dashboard` | KPI dashboard |
| `/estoque` | `Estoque` | Vehicle inventory |
| `/configuracoes` | `Configuracoes` | Settings |
| `/login` | `Login` | Login/Register form |
| `*` | `NotFound` | 404 fallback |
- No route protection (all routes publicly accessible)
- No lazy loading (`React.lazy` not used)
- No nested routes or layout routes
- Login page is purely visual - no real authentication
## CSS / Design System
- Dark mode: `class` strategy (line 4)
- Content paths: `./pages/**`, `./components/**`, `./app/**`, `./src/**` (line 5)
- Container: centered, 2rem padding, max 1400px at 2xl (lines 8-13)
- Font: Inter sans-serif (line 17)
- Border radius: CSS variable `--radius` = 0.75rem
- Full light + dark theme via HSL CSS custom properties
- Custom semantic colors: `kanban-*`, `status-*`, `chart-*`, `trend-*`, `sidebar-*`
- Font loaded via Google Fonts CDN: `@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap')`
- `animate-fade-in` - 0.3s ease-out opacity
- `animate-slide-up` - 0.3s ease-out opacity + translateY
- `tailwindcss-animate` - animation utilities
- `@tailwindcss/typography` - typography prose classes (installed as devDep)
## Testing
- Environment: jsdom
- Globals: enabled (no need to import `describe`, `it`, `expect`)
- Setup file: `frontend/src/test/setup.ts`
- Pattern: `src/**/*.{test,spec}.{ts,tsx}`
- Path alias: `@` -> `./src`
- Imports `@testing-library/jest-dom` for DOM matchers
- Mocks `window.matchMedia` (required for responsive hooks)
- Only 1 test file exists: `frontend/src/test/example.test.ts`
- Contains a single trivial assertion: `expect(true).toBe(true)`
- No component tests, no integration tests, no hook tests
- No coverage script configured
- No E2E testing framework installed
## Linting
- Extends: `@eslint/js` recommended + `typescript-eslint` recommended
- Plugins: `react-hooks`, `react-refresh`
- `@typescript-eslint/no-unused-vars`: **disabled** (line 24)
- `react-refresh/only-export-components`: warn with `allowConstantExport: true`
- Ignores: `dist/`
- **No Prettier configured** - no `.prettierrc`, no prettier package
- No automated formatting tool
## Build Configuration
- Plugin: `@vitejs/plugin-react-swc` (SWC-based React transform)
- Dev server: `localhost:3000`
- Path alias: `@` -> `./src`
- No proxy configured for backend API
- No environment-specific configs
## Platform Requirements
- Node.js (version not pinned - recommend adding `.nvmrc`)
- npm (lockfile present)
- Backend services via Docker Compose (`frontend/../backend/compose.yaml`):
- Backend: Kotlin/Spring Boot 3.2.5 on JDK 21 (port 8080)
- Static SPA - requires any static file server
- No SSR/SSG requirements
- No deployment configuration detected
## Known Issues
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming Patterns
- React components: `PascalCase.tsx` (e.g., `KPICard.tsx`, `MainLayout.tsx`, `AddVehicleModal.tsx`)
- Custom hooks: `kebab-case.tsx` or `.ts` (e.g., `use-mobile.tsx`, `use-toast.ts`) -- follows shadcn/ui convention
- Contexts: `PascalCase.tsx` (e.g., `ThemeContext.tsx`, `NotificationContext.tsx`)
- Types: `index.ts` in `src/types/` -- single barrel file
- Data/mock files: `camelCase.ts` (e.g., `mockData.ts`)
- Utility files: `camelCase.ts` (e.g., `utils.ts`)
- Pages: `PascalCase.tsx` -- Portuguese names (e.g., `Configuracoes.tsx`, `Estoque.tsx`, `Kanban.tsx`)
- Use PascalCase for component names: `KPICard`, `KanbanBoard`, `MainLayout`
- Suffix with purpose: `*Modal` for modals, `*Card` for cards, `*Table` for tables, `*Chart` for charts
- Component and file names match exactly: `KanbanBoard` lives in `KanbanBoard.tsx`
- camelCase for all functions: `formatCurrency`, `handleDragEnd`, `handleSignOut`
- Event handlers prefixed with `handle`: `handleSubmit`, `handleAddVehicle`, `handleCardClick`
- Formatting utilities prefixed with `format`: `formatCurrency`, `formatTimeAgo`, `formatDate`
- camelCase for variables: `searchQuery`, `statusFilter`, `isModalOpen`
- Boolean state prefixed with `is`/`has`/`show`: `isLogin`, `isDetailModalOpen`, `showPassword`, `showAssumeButton`
- State setters follow `set[Name]` convention: `setTheme`, `setCards`, `setSearchQuery`
- Use `interface` for object shapes (component props, DTOs): `KPICardProps`, `Vehicle`, `Lead`
- Use `type` for unions and mapped types: `VehicleStatus`, `LeadOrigin`, `KanbanColumn`, `CardTag`
- Props interfaces suffixed with `Props`: `KPICardProps`, `KanbanBoardProps`, `AddVehicleModalProps`
- Form data types suffixed with `FormData`: `VehicleFormData`, `LeadFormData`, `UserFormData`
## Code Style
- No Prettier configured -- formatting is manual/inconsistent
- Indentation: 2 spaces (consistent across all files)
- Semicolons: not enforced, mostly present
- Quotes: single quotes for imports and strings in `.tsx` files, double quotes in some config files
- Trailing commas: used in most places
- ESLint 9 with flat config: `eslint.config.js`
- Extends: `@eslint/js` recommended + `typescript-eslint` recommended
- Plugins: `react-hooks` (recommended rules), `react-refresh` (only-export-components as warning)
- `@typescript-eslint/no-unused-vars`: **disabled** (`"off"`)
- No custom rules beyond defaults
- `strict: false` in `tsconfig.app.json`
- `noImplicitAny: false`
- `noUnusedLocals: false`
- `noUnusedParameters: false`
- `noFallthroughCasesInSwitch: false`
- Target: ES2020, Module: ESNext, JSX: react-jsx
## Import Organization
- `@/*` maps to `./src/*` (configured in both `tsconfig.json` and `vite.config.ts`)
- Use `@/components/ui/button` not `../../components/ui/button`
- Relative imports used only within same directory (e.g., `./Header` from `MainLayout.tsx`)
- Named imports preferred: `import { Button } from '@/components/ui/button'`
- Default exports used for pages: `export default function Dashboard()`
- Named exports used for components: `export function KPICard()`
- Icon imports are destructured: `import { Car, User, Bell } from 'lucide-react'`
- Type imports use inline `type` keyword in some places, not consistently enforced
## Component Patterns
- All components are function components (no class components)
- Named function declarations for exported components: `export function KPICard() {}`
- Arrow functions for the root `App` component: `const App = () => (`
- `forwardRef` used where needed (e.g., `NavLink.tsx`, shadcn/ui components)
## Tailwind CSS Patterns
- All colors use HSL CSS variables: `hsl(var(--primary))`, `hsl(var(--background))`
- Tailwind maps semantic names: `text-foreground`, `bg-card`, `border-border`
- Domain-specific tokens: `bg-kanban-chatbot`, `text-status-available`, `text-chart-primary`, `text-trend-positive`
- Card containers: `rounded-xl border bg-card p-6 shadow-sm animate-fade-in`
- Section headers: `text-lg font-semibold text-foreground` with `text-sm text-muted-foreground` subtitle
- Table headers: `px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground`
- Table rows: `hover:bg-muted/20 transition-colors`
- Status badges: `inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium`
- Icon containers: `flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10`
- Page layout spacing: `space-y-6` for vertical sections, `gap-4` for grid items
- Background tints: `bg-primary/10`, `bg-kanban-completed/10`, `bg-muted/30`
- Hover states: `hover:bg-primary/90`, `hover:bg-muted/50`
- Border opacity: `border-border/50`
- `animate-fade-in`: 0.3s fade-in, applied to cards and sections
- `animate-slide-up`: 0.3s slide-up with translateY
- Class-based: `darkMode: ["class"]` in `tailwind.config.ts`
- CSS variables swap in `.dark` class (defined in `src/index.css`)
- Theme toggle managed by `ThemeContext` -- sets/removes `.dark` on `<html>`
## shadcn/ui Usage Patterns
- Style: `default` (not `new-york`)
- Base color: `slate`
- CSS variables: enabled
- RSC: disabled (`rsc: false`)
- Prefix: none
- `Button` with variants: `default`, `outline`, `ghost`, `destructive`
- `Dialog` + `DialogContent` + `DialogHeader` + `DialogTitle` for modals
- `Input`, `Label`, `Textarea` for form fields
- `Select` + `SelectContent` + `SelectItem` + `SelectTrigger` + `SelectValue`
- `DropdownMenu` for profile/action menus
- `Popover` for notification panel
- `ScrollArea` for scrollable content
- `Tooltip` for help text
- `Switch` for toggle settings
- `Collapsible` for expandable sections
- `Toaster` (Radix) + `Sonner` -- both mounted simultaneously in `App.tsx`
## State Management Patterns
- All state is currently local -- no global state management library
- Forms use `react-hook-form` with `zodResolver` for validation
- Mock data initialized via `useState(mockData)` at page level
- `ThemeContext` (`src/contexts/ThemeContext.tsx`): theme mode (light/dark/system) + accent color, persisted to `localStorage`
- `NotificationContext` (`src/contexts/NotificationContext.tsx`): in-memory notification list with CRUD operations
- `QueryClientProvider` wrapping the app in `App.tsx`
- `QueryClient` instantiated at module level with default config
- **Not actively used anywhere** -- all data comes from `src/data/mockData.ts`
## Form Handling
## Error Handling
- No try/catch blocks (no async operations exist yet)
- No error boundaries
- Form validation via Zod is the only structured error handling
- `toast.success()` from Sonner for success feedback -- no `toast.error()` used for API failures
- No loading states (no async data fetching)
- No empty states for lists (data always present via mock)
- Use `import { toast } from 'sonner'` for all toast notifications
- Pattern: `toast.success('Message')`, `toast.error('Message')`, `toast.info('Message')`
- Do NOT use the Radix Toast system (`useToast` from `@/hooks/use-toast.ts`) -- it exists but is unused and redundant
## Logging
- Single `console.error` in `NotFound.tsx` for 404 tracking
- No structured logging pattern
## Comments
- Section dividers in JSX: `{/* Header */}`, `{/* KPI Cards */}`, `{/* Charts */}`
- Tab section markers: `{/* ============ GENERAL TAB ============ */}`
- No JSDoc or TSDoc on any function or component
- No inline code comments explaining logic
## Module Design
- Pages use `export default function PageName()` -- one per file
- Components use named exports: `export function ComponentName()` -- one per file
- Contexts export both `Provider` and `useHook` from same file
- Types barrel-exported from `src/types/index.ts`
- Only `src/types/index.ts` acts as a barrel
- No barrel files in `components/`, `hooks/`, `contexts/`, or `pages/`
- Each component imported individually from its file path
## Internationalization
- All labels, headings, button text, toast messages in Portuguese
- Type names and code-level identifiers in Portuguese: `marca`, `modelo`, `preco`, `coluna`, `vendedorNome`
- Some mixed: `Lead`, `Vehicle`, `KPIData` (interface names in English), but fields in Portuguese
- Currency formatting uses `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`
- Date formatting uses `Intl.DateTimeFormat('pt-BR', ...)`
## Routing
- Flat route structure in `src/App.tsx` -- no nested routes
- Routes defined inline (not in separate config file)
- No route guards or authentication checks
- No lazy loading of route components
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- React 18 SPA with Vite as bundler (SWC plugin for fast transforms)
- Flat routing via React Router DOM v6 (no nested layouts via router)
- All data sourced from `src/data/mockData.ts` -- zero API calls to backend
- Layout composition via `MainLayout` wrapper component inside each page
- State management through React Context (Theme, Notifications) and local `useState`
- React Query (`QueryClientProvider`) is configured but unused -- no queries or mutations exist
- Component hierarchy: App (providers) -> BrowserRouter -> Pages -> MainLayout -> Feature Components
## Provider Tree
```
```
- Provider setup: `src/App.tsx`
- Theme context: `src/contexts/ThemeContext.tsx`
- Notification context: `src/contexts/NotificationContext.tsx`
## Routing
| Path | Component | Layout | Auth Required |
|------|-----------|--------|---------------|
| `/` | `Kanban` | `MainLayout` | No (should be) |
| `/dashboard` | `Dashboard` | `MainLayout` | No (should be) |
| `/estoque` | `Estoque` | `MainLayout` | No (should be) |
| `/configuracoes` | `Configuracoes` | `MainLayout` | No (should be) |
| `/login` | `Login` | None (standalone) | No |
| `*` | `NotFound` | None (standalone) | No |
- No route guards or authentication checks
- No lazy loading (`React.lazy`) -- all pages eagerly imported
- No nested route layouts via React Router `<Outlet>`
- Login page navigates to `/` on submit with no actual auth
- Sign-out in Header navigates to `/login` with no token clearing (no tokens exist)
- No `/app/*` prefix separation between public and authenticated routes
## Layers
- Purpose: Route-level components that compose the full page view
- Location: `src/pages/`
- Contains: 6 page components (Dashboard, Kanban, Estoque, Configuracoes, Login, NotFound)
- Depends on: MainLayout, feature components, mockData
- Pattern: Each page wraps content in `<MainLayout>` (except Login and NotFound)
- State: Local `useState` for UI state (filters, modals, form data)
- Purpose: Shared page chrome (header navigation)
- Location: `src/components/layout/`
- Contains: `MainLayout.tsx` (shell), `Header.tsx` (top navbar with nav, notifications, profile)
- Pattern: `MainLayout` renders `Header` + `<main>` with children. No sidebar component.
- Navigation items defined as array in `Header.tsx` (lines 23-28)
- Purpose: Domain-specific UI components grouped by feature
- Location: `src/components/dashboard/`, `src/components/kanban/`, `src/components/modals/`
- Contains: KPI cards, charts, tables, kanban board/column/card, modal dialogs
- Depends on: shadcn/ui primitives, types, mockData (via props from pages), contexts
- Pattern: Named exports, typed props interfaces, data received via props from parent pages
- Purpose: shadcn/ui component library (generated, not hand-written)
- Location: `src/components/ui/`
- Contains: 49 component files (accordion, alert, badge, button, card, dialog, form, input, select, etc.)
- Config: `components.json` at project root (style: default, baseColor: slate, cssVariables: true)
- Usage: Import from `@/components/ui/[component]`
- Purpose: Global state via React Context API
- Location: `src/contexts/`
- Contains: ThemeContext (theme mode + accent color), NotificationContext (in-memory notifications)
- Pattern: createContext + Provider component + custom hook (`useTheme`, `useNotifications`)
- Persistence: Theme stored in `localStorage`, notifications are ephemeral (in-memory with seed data)
- Purpose: Mock data that simulates backend responses
- Location: `src/data/mockData.ts`
- Contains: All mock data (vehicles, leads, kanban cards, KPIs, sales, charts)
- Pattern: Exported constants consumed directly by pages
- **Critical gap:** No `src/services/` directory exists. No API client. No HTTP calls.
- Purpose: TypeScript type definitions for the domain model
- Location: `src/types/index.ts`
- Contains: Vehicle, Lead, KanbanCard, KanbanColumn, Seller, KPIData, Notification, ThemeMode, AccentColor
- Pattern: Single barrel file with all types. Mix of `interface` and `type` declarations.
- **Note:** Types use Portuguese field names (e.g., `nome`, `telefone`, `preco`) and do NOT align with backend DTOs
- Purpose: Shared utility functions
- Location: `src/lib/utils.ts`
- Contains: Single `cn()` function (clsx + tailwind-merge)
- No other utility files exist
- Purpose: Custom React hooks
- Location: `src/hooks/`
- Contains: `use-mobile.tsx` (responsive breakpoint detection), `use-toast.ts` (Radix toast state machine)
- **Note:** `use-toast.ts` is shadcn/ui generated code for the Radix-based toast system (duplicate with Sonner)
## Data Flow
- **Global state:** React Context only (Theme + Notifications)
- **Page state:** Local `useState` in each page component
- **Form state:** React Hook Form per modal/form
- **Server state:** None (React Query configured but unused)
- **URL state:** Not used (no query params for filters, pagination, etc.)
## Key Abstractions
- Purpose: Wraps authenticated page content with Header and main area
- File: `src/components/layout/MainLayout.tsx`
- Pattern: Simple children wrapper (`<Header /> + <main>{children}</main>`)
- Used by: All pages except Login and NotFound
- Purpose: Full Kanban board with drag-and-drop
- Files: `src/components/kanban/KanbanBoard.tsx`, `KanbanColumn.tsx`, `KanbanCard.tsx`
- Pattern: Board manages state + DragDropContext -> Column wraps Droppable -> Card wraps Draggable
- State: Board owns all card state; columns and cards are presentational with callbacks
- Purpose: CRUD forms for leads, vehicles, users
- Files: `src/components/modals/CardDetailModal.tsx`, `AddVehicleModal.tsx`, `AddUserModal.tsx`, `EditUserModal.tsx`
- Pattern: shadcn/ui `Dialog` + React Hook Form + Zod schema + `onSubmit` callback to parent
- Validation: Zod schemas defined inline in each modal file
- Purpose: Reusable metric display card
- File: `src/components/dashboard/KPICard.tsx`
- Pattern: Accepts `title`, `value`, `trend`, `icon` (Lucide component), `iconBgColor` as props
## Entry Points
- Location: `src/main.tsx`
- Triggers: Vite dev server or production build
- Responsibilities: Mounts `<App />` to `#root` DOM element, imports global CSS
- Location: `src/App.tsx`
- Triggers: React render
- Responsibilities: Provider tree setup, route definitions, toast renderers
- Location: `index.html`
- Note: Title is "V3 Motors Hub" (not "Astro LeadFlow" -- needs updating)
- Lang: `pt-BR`
## Error Handling
- Form validation: Zod schemas with inline error messages displayed below inputs
- No `ErrorBoundary` component exists
- No API error handling (no API calls exist)
- `NotFound` page logs 404 to console.error
- No loading states for async operations (no async operations exist)
- Toast notifications for user feedback (success only, via Sonner `toast.success()`)
## Cross-Cutting Concerns
## Design Token System
- **Kanban colors:** `--kanban-chatbot`, `--kanban-waiting`, `--kanban-negotiation`, `--kanban-completed`
- **Vehicle status colors:** `--status-available`, `--status-reserved`, `--status-maintenance`, `--status-sold`
- **Chart colors:** `--chart-primary`, `--chart-secondary`, `--chart-tertiary`, `--chart-muted`
- **Trend colors:** `--trend-positive`, `--trend-negative`
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
