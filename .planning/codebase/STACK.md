# Technology Stack

**Analysis Date:** 2026-03-26

## Languages

**Primary:**
- TypeScript 5.8.3 - All frontend source code (`frontend/src/**/*.{ts,tsx}`)
  - `strict: false` in `frontend/tsconfig.app.json` (line 19) - all strict checks disabled
  - `noImplicitAny: false`, `noUnusedLocals: false`, `noUnusedParameters: false`
  - Node config (`frontend/tsconfig.node.json`) has `strict: true` separately

**Secondary:**
- CSS (Tailwind utility classes + CSS custom properties in `frontend/src/index.css`)

## Runtime

**Environment:**
- Node.js - version not pinned (no `.nvmrc` or `.node-version` file)
- Browser target: ES2020 (configured in `frontend/tsconfig.app.json` line 4)

**Package Manager:**
- npm (inferred from `frontend/package-lock.json`)
- Lockfile: present (`frontend/package-lock.json`)
- No `.npmrc` file detected

## Frameworks

**Core:**
- React 18.3.1 - UI framework (`frontend/package.json` line 54)
- React DOM 18.3.1 - DOM rendering (`frontend/package.json` line 56)
- React Router DOM 6.30.1 - Client-side routing (`frontend/package.json` line 59)

**Testing:**
- Vitest 3.2.4 - Test runner (`frontend/package.json` line 87)
- @testing-library/react 16.0.0 - Component testing utilities (`frontend/package.json` line 71)
- @testing-library/jest-dom 6.6.0 - DOM assertion matchers (`frontend/package.json` line 70)
- jsdom 20.0.3 - Browser environment for tests (`frontend/package.json` line 80)

**Build/Dev:**
- Vite 5.4.19 - Bundler and dev server (`frontend/package.json` line 86)
- @vitejs/plugin-react-swc 3.11.0 - React Fast Refresh via SWC (`frontend/package.json` line 75)
- PostCSS 8.5.6 - CSS processing (`frontend/package.json` line 82)
- Autoprefixer 10.4.21 - CSS vendor prefixing (`frontend/package.json` line 76)

## Key Dependencies

### UI Component Library

**shadcn/ui (63 components installed):**
- Config: `frontend/components.json`
- Style: `default`
- Base color: `slate`
- CSS Variables: enabled
- RSC: disabled (`"rsc": false`)
- Aliases: `@/components/ui`, `@/lib/utils`, `@/hooks`

**Installed shadcn/ui components** (48 files in `frontend/src/components/ui/`):
`accordion`, `alert-dialog`, `alert`, `aspect-ratio`, `avatar`, `badge`, `breadcrumb`, `button`, `calendar`, `card`, `carousel`, `chart`, `checkbox`, `collapsible`, `command`, `context-menu`, `dialog`, `drawer`, `dropdown-menu`, `form`, `hover-card`, `input-otp`, `input`, `label`, `menubar`, `navigation-menu`, `pagination`, `popover`, `progress`, `radio-group`, `resizable`, `scroll-area`, `select`, `separator`, `sheet`, `sidebar`, `skeleton`, `slider`, `sonner`, `switch`, `table`, `tabs`, `textarea`, `toast`, `toaster`, `toggle-group`, `toggle`, `tooltip`

Plus: `use-toast.ts` (shadcn hook in ui/ directory)

### Radix UI Primitives (underlying shadcn/ui)

All `@radix-ui/react-*` packages - 27 packages total, used exclusively by shadcn/ui components. Key ones:
- `@radix-ui/react-dialog` ^1.1.14
- `@radix-ui/react-dropdown-menu` ^2.1.15
- `@radix-ui/react-popover` ^1.1.14
- `@radix-ui/react-select` ^2.2.5
- `@radix-ui/react-tabs` ^1.1.12
- `@radix-ui/react-toast` ^1.2.14

### Critical Application Dependencies

- **@tanstack/react-query** 5.83.0 - Server state management
  - Status: **Provider configured but NOT actively used**
  - `QueryClientProvider` wraps app in `frontend/src/App.tsx` (line 18)
  - No `useQuery` or `useMutation` calls anywhere in application code
  - Only reference: `frontend/src/App.tsx` lines 15, 18

- **@hello-pangea/dnd** 18.0.1 - Drag-and-drop for Kanban board
  - Used in: `frontend/src/components/kanban/KanbanBoard.tsx`

- **react-hook-form** 7.61.1 - Form management
  - **@hookform/resolvers** 3.10.0 - Zod/Yup integration
  - Status: installed, ready for use, shadcn/ui `form.tsx` component uses it

- **zod** 3.25.76 - Schema validation
  - Status: installed, ready for use via `@hookform/resolvers`

- **recharts** 2.15.4 - Dashboard charts
  - Used in: `frontend/src/components/dashboard/SalesLineChart.tsx`, `CategoryBarChart.tsx`

- **lucide-react** 0.462.0 - Icon library
  - Used across all pages and components

- **sonner** 1.7.4 - Toast notifications (primary)
  - Component: `frontend/src/components/ui/sonner.tsx`
  - Used in: `frontend/src/pages/Login.tsx`, `frontend/src/components/layout/Header.tsx`

- **date-fns** 3.6.0 - Date manipulation
  - Used by `react-day-picker` / calendar component

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
  - **Issue:** Only used inside `frontend/src/components/ui/sonner.tsx` (line 1)
  - App uses custom `ThemeProvider` in `frontend/src/contexts/ThemeContext.tsx`
  - This creates a conflict: Sonner reads theme from `next-themes` but app uses custom context
- **react-day-picker** 8.10.1 - Date picker (shadcn calendar component)
- **react-resizable-panels** 2.1.9 - Resizable panels (shadcn resizable component)
- **vaul** 0.9.9 - Drawer component (shadcn drawer component)

## State Management

**Approach:** React Context + useState (no global store library)

**Contexts:**
- `frontend/src/contexts/ThemeContext.tsx` - Theme mode (light/dark/system) + accent color
  - Persists to `localStorage` keys: `theme`, `accentColor`
  - Default theme: `dark`
  - Default accent: `purple`
  - Exports: `ThemeProvider`, `useTheme()`

- `frontend/src/contexts/NotificationContext.tsx` - In-app notifications
  - Uses hardcoded initial notifications (mock data, lines 16-41)
  - No backend integration
  - Exports: `NotificationProvider`, `useNotifications()`

**Data layer:** All data comes from `frontend/src/data/mockData.ts` - hardcoded static arrays.
No HTTP client, no service layer, no API calls anywhere in the codebase.

**React Query:** Provider present but completely unused. No queries, mutations, or cache invalidation.

## Routing

**Library:** React Router DOM 6.30.1

**Configuration:** `frontend/src/App.tsx` (lines 24-32)

**Routes (6 total, all public - no auth guards):**
| Path | Component | Purpose |
|------|-----------|---------|
| `/` | `Kanban` | Kanban board (default page) |
| `/dashboard` | `Dashboard` | KPI dashboard |
| `/estoque` | `Estoque` | Vehicle inventory |
| `/configuracoes` | `Configuracoes` | Settings |
| `/login` | `Login` | Login/Register form |
| `*` | `NotFound` | 404 fallback |

**Issues:**
- No route protection (all routes publicly accessible)
- No lazy loading (`React.lazy` not used)
- No nested routes or layout routes
- Login page is purely visual - no real authentication

## CSS / Design System

**Framework:** Tailwind CSS 3.4.17

**Configuration:** `frontend/tailwind.config.ts`
- Dark mode: `class` strategy (line 4)
- Content paths: `./pages/**`, `./components/**`, `./app/**`, `./src/**` (line 5)
- Container: centered, 2rem padding, max 1400px at 2xl (lines 8-13)
- Font: Inter sans-serif (line 17)
- Border radius: CSS variable `--radius` = 0.75rem

**CSS Variables:** Defined in `frontend/src/index.css` (lines 7-108)
- Full light + dark theme via HSL CSS custom properties
- Custom semantic colors: `kanban-*`, `status-*`, `chart-*`, `trend-*`, `sidebar-*`
- Font loaded via Google Fonts CDN: `@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap')`

**Custom animations** (in `frontend/src/index.css` lines 122-150):
- `animate-fade-in` - 0.3s ease-out opacity
- `animate-slide-up` - 0.3s ease-out opacity + translateY

**Plugins:**
- `tailwindcss-animate` - animation utilities
- `@tailwindcss/typography` - typography prose classes (installed as devDep)

## Testing

**Runner:** Vitest 3.2.4

**Configuration:** `frontend/vitest.config.ts`
- Environment: jsdom
- Globals: enabled (no need to import `describe`, `it`, `expect`)
- Setup file: `frontend/src/test/setup.ts`
- Pattern: `src/**/*.{test,spec}.{ts,tsx}`
- Path alias: `@` -> `./src`

**Setup file** (`frontend/src/test/setup.ts`):
- Imports `@testing-library/jest-dom` for DOM matchers
- Mocks `window.matchMedia` (required for responsive hooks)

**Test coverage:** Essentially 0%
- Only 1 test file exists: `frontend/src/test/example.test.ts`
- Contains a single trivial assertion: `expect(true).toBe(true)`
- No component tests, no integration tests, no hook tests

**Scripts:**
```bash
npm run test          # vitest run (single run)
npm run test:watch    # vitest (watch mode)
```
- No coverage script configured
- No E2E testing framework installed

## Linting

**ESLint:** 9.32.0 (flat config format)

**Configuration:** `frontend/eslint.config.js`
- Extends: `@eslint/js` recommended + `typescript-eslint` recommended
- Plugins: `react-hooks`, `react-refresh`
- `@typescript-eslint/no-unused-vars`: **disabled** (line 24)
- `react-refresh/only-export-components`: warn with `allowConstantExport: true`
- Ignores: `dist/`

**Formatting:**
- **No Prettier configured** - no `.prettierrc`, no prettier package
- No automated formatting tool

## Build Configuration

**Vite config** (`frontend/vite.config.ts`):
- Plugin: `@vitejs/plugin-react-swc` (SWC-based React transform)
- Dev server: `localhost:3000`
- Path alias: `@` -> `./src`
- No proxy configured for backend API
- No environment-specific configs

**Build scripts:**
```bash
npm run dev           # vite (dev server on localhost:3000)
npm run build         # vite build (production)
npm run build:dev     # vite build --mode development
npm run preview       # vite preview
npm run lint          # eslint .
```

## Platform Requirements

**Development:**
- Node.js (version not pinned - recommend adding `.nvmrc`)
- npm (lockfile present)
- Backend services via Docker Compose (`frontend/../backend/compose.yaml`):
  - PostgreSQL 16 (port 5432)
  - Redis 7 (port 6379)
  - MailHog (SMTP port 1025, UI port 8025)
  - LocalStack 3.0 (port 4566 - S3, SQS, SNS)
- Backend: Kotlin/Spring Boot 3.2.5 on JDK 21 (port 8080)

**Production:**
- Static SPA - requires any static file server
- No SSR/SSG requirements
- No deployment configuration detected

## Known Issues

1. **Dual toast system:** Both `@radix-ui/react-toast` (via shadcn `toast.tsx` + `toaster.tsx`) and `sonner` are active simultaneously in `frontend/src/App.tsx` (lines 1-2). Application code primarily uses Sonner (`toast` from `sonner`).

2. **next-themes conflict:** `next-themes` is installed and used only by `frontend/src/components/ui/sonner.tsx`, but the app has its own `ThemeContext`. The Sonner component reads theme from `next-themes` (which has no provider), while the rest of the app uses the custom `ThemeContext`.

3. **Package name mismatch:** `frontend/package.json` names the project `v3-motors-hub` (line 2), but the branding in code references both "V3 Motors" and "Astro LeadFlow".

4. **TypeScript strict mode disabled:** All strict checks are off in `frontend/tsconfig.app.json`, including `noImplicitAny`. This allows silent type errors.

5. **No HTTP client installed:** Neither `axios` nor any other HTTP client is in dependencies. No `fetch` calls exist. The entire app runs on mock data.

6. **No environment variables:** No `.env` files exist. No `import.meta.env` usage detected anywhere in the codebase.

---

*Stack analysis: 2026-03-26*
