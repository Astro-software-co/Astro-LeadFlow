# Codebase Concerns

**Analysis Date:** 2026-03-26

## CRITICAL Severity

### No Authentication or Route Protection

- Issue: The entire application has zero authentication. The login page at `frontend/src/pages/Login.tsx` is purely cosmetic -- it accepts any input, shows a success toast, and navigates to `/` without any API call, JWT handling, or session management.
- Files:
  - `frontend/src/pages/Login.tsx` (lines 16-24) -- fake `handleSubmit` just calls `toast.success()` and `navigate('/')`
  - `frontend/src/App.tsx` (lines 25-31) -- all routes are public, no route guards
- Impact: Any user can access all application pages directly. No multi-tenancy enforcement. No role-based access control. The backend has a complete IAM module with JWT auth that is completely unused.
- Fix approach:
  1. Create `frontend/src/services/api.ts` with Axios instance + JWT interceptors
  2. Create `frontend/src/services/auth.service.ts` for login/logout/refresh
  3. Create `frontend/src/hooks/useAuth.ts` with authentication state
  4. Create `frontend/src/components/auth/AuthGuard.tsx` for protected routes
  5. Create `frontend/src/components/auth/RoleGate.tsx` for RBAC rendering
  6. Restructure `frontend/src/App.tsx` routes with auth guards

### No API Integration -- 100% Mock Data

- Issue: Every data-consuming page imports directly from `frontend/src/data/mockData.ts`. There is no HTTP client, no service layer, and no connection to the backend API whatsoever. React Query (`@tanstack/react-query`) is installed and the `QueryClientProvider` is mounted in `frontend/src/App.tsx` (line 18), but it is never used for any actual queries or mutations.
- Files:
  - `frontend/src/data/mockData.ts` -- 165 lines of hardcoded static data
  - `frontend/src/pages/Dashboard.tsx` (lines 8-14) -- imports all mock data directly
  - `frontend/src/pages/Kanban.tsx` (line 3) -- imports `mockKanbanCards`
  - `frontend/src/pages/Estoque.tsx` (line 6) -- imports `mockVehicles`
  - `frontend/src/pages/Configuracoes.tsx` (lines 21-25) -- hardcoded user list
- Impact: The application is a non-functional prototype. No data persists. No CRUD operations reach the server. The backend is complete with full REST API, Swagger docs, and all modules operational.
- Fix approach:
  1. Create `frontend/src/lib/axios.ts` with configured Axios instance
  2. Create service files under `frontend/src/services/` for each domain module
  3. Create React Query hooks under `frontend/src/hooks/` (e.g., `useLeads.ts`, `useVehicles.ts`)
  4. Replace mock imports with React Query hooks in each page
  5. Keep `mockData.ts` as development fallback controlled by `VITE_ENABLE_MOCK` env var

### No Error Boundary

- Issue: The application has no React Error Boundary at any level. An unhandled error in any component crashes the entire application with a white screen.
- Files:
  - `frontend/src/main.tsx` -- renders `<App />` without any error boundary wrapper
  - `frontend/src/App.tsx` -- no error boundary around routes
- Impact: Any runtime error (failed API call, bad data shape, rendering issue) kills the entire app with no recovery path and no user-facing error message.
- Fix approach: Create `frontend/src/components/shared/ErrorBoundary.tsx` with a fallback UI, and wrap the top-level `<App />` and individual route pages.

### TypeScript Strict Mode Disabled

- Issue: All TypeScript strictness checks are explicitly turned off in `frontend/tsconfig.app.json` (lines 19-23):
  ```
  "strict": false,
  "noUnusedLocals": false,
  "noUnusedParameters": false,
  "noImplicitAny": false,
  "noFallthroughCasesInSwitch": false
  ```
- Files: `frontend/tsconfig.app.json` (lines 19-23)
- Impact: The codebase accepts `any` types silently, allows unused imports/variables to accumulate, and provides no compile-time safety for null/undefined access. This directly leads to runtime errors that TypeScript strict mode would catch.
- Fix approach: Enable `strict: true` incrementally. Start by enabling `noImplicitAny` and `noUnusedLocals`, then progress to full strict mode.

---

## HIGH Severity

### Dual Toast System -- Sonner + Radix Toast

- Issue: Two independent toast notification systems are installed and mounted simultaneously in `frontend/src/App.tsx` (lines 22-23):
  ```tsx
  <Toaster />   {/* Radix Toast via @radix-ui/react-toast */}
  <Sonner />    {/* Sonner toast library */}
  ```
  All application code uses Sonner (`import { toast } from 'sonner'`), while the Radix Toast system (`@radix-ui/react-toast`, `frontend/src/hooks/use-toast.ts`, `frontend/src/components/ui/toast.tsx`, `frontend/src/components/ui/toaster.tsx`, `frontend/src/components/ui/use-toast.ts`) sits completely unused but still renders a `<ToastProvider>` in the DOM.
- Files:
  - `frontend/src/App.tsx` (lines 1, 22) -- imports and renders both Toasters
  - `frontend/src/hooks/use-toast.ts` -- 186 lines of unused toast reducer logic
  - `frontend/src/components/ui/toast.tsx` -- 111 lines of unused Radix Toast components
  - `frontend/src/components/ui/toaster.tsx` -- 24 lines, unused Radix Toaster
  - `frontend/src/components/ui/use-toast.ts` -- re-export wrapper, unused
  - `@radix-ui/react-toast` in `package.json` (line 41) -- unused dependency
- Impact: Unnecessary bundle size, potential visual overlap if Radix Toast is accidentally triggered, confusing developer experience with two toast APIs available.
- Fix approach: Remove Radix Toast entirely -- delete `frontend/src/hooks/use-toast.ts`, `frontend/src/components/ui/toast.tsx`, `frontend/src/components/ui/toaster.tsx`, `frontend/src/components/ui/use-toast.ts`. Remove `<Toaster />` from `App.tsx`. Remove `@radix-ui/react-toast` from `package.json`. Keep Sonner as the sole toast system.

### Sonner Uses `next-themes` Instead of Custom ThemeContext

- Issue: `frontend/src/components/ui/sonner.tsx` (line 1) imports `useTheme` from `next-themes`, which is a Next.js-specific package. The application uses a custom `ThemeContext` at `frontend/src/contexts/ThemeContext.tsx`, not `next-themes`. The `next-themes` package is listed in `package.json` (line 53) but is only used by this single shadcn/ui boilerplate file.
- Files:
  - `frontend/src/components/ui/sonner.tsx` (line 1) -- `import { useTheme } from "next-themes"`
  - `frontend/src/contexts/ThemeContext.tsx` -- the actual theme provider used by the app
  - `package.json` (line 53) -- `"next-themes": "^0.3.0"` as unnecessary dependency
- Impact: The Sonner toast may not correctly pick up theme changes (light/dark) since it reads from `next-themes` which is not the active theme provider. This can cause toast styling mismatches in dark mode.
- Fix approach: Modify `frontend/src/components/ui/sonner.tsx` to import `useTheme` from `@/contexts/ThemeContext` instead. Remove `next-themes` from `package.json`.

### Branding Mismatch -- "V3 Motors" vs "Astro LeadFlow"

- Issue: The entire frontend UI still references "V3 Motors" (the old brand) instead of "Astro LeadFlow" (the current brand). Even the package name in `package.json` is `"v3-motors-hub"`.
- Files:
  - `frontend/package.json` (line 2) -- `"name": "v3-motors-hub"`
  - `frontend/index.html` (line 6) -- `<title>V3 Motors Hub</title>`
  - `frontend/index.html` (line 7) -- `"Sistema de gestao V3 Motors"`
  - `frontend/src/pages/Login.tsx` (line 35) -- `"V3 Motors"` heading
  - `frontend/src/pages/Login.tsx` (line 169) -- `"V3 Motors"` copyright
  - `frontend/src/components/layout/Header.tsx` (line 69) -- `"V3 Motors"` in navbar
  - `frontend/src/pages/Configuracoes.tsx` (lines 22-24) -- `v3motors.com` email addresses
  - `frontend/src/pages/Configuracoes.tsx` (line 142) -- `v3motors.com` display
  - `frontend/src/components/modals/AddUserModal.tsx` (line 123) -- `v3motors.com` placeholder
- Impact: Complete brand inconsistency. The product name, logos, and references do not match the actual brand identity.
- Fix approach: Global find-and-replace "V3 Motors" with "Astro LeadFlow", update package name, update email domains, and create a proper Astro logo SVG component.

### Frontend Types Misaligned with Backend DTOs

- Issue: The TypeScript types in `frontend/src/types/index.ts` use Portuguese field names (`nome`, `telefone`, `origem`, `dataCriacao`) and Portuguese enum values (`chatbot`, `aguardando`, `negociacao`, `concluido`, `disponivel`, `reservado`, `manutencao`), while the backend Kotlin API uses English field names and enum values (`NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST` for lead stages; `WEBMOTORS`, `MERCADO_LIVRE`, `WEBSITE`, `PHONE`, `WALK_IN`, `REFERRAL` for lead sources).
- Files:
  - `frontend/src/types/index.ts` -- all types use Portuguese naming
  - `frontend/src/data/mockData.ts` -- mock data uses Portuguese values
  - Backend `LeadStage` enum: `NEW | CONTACTED | QUALIFIED | PROPOSAL | NEGOTIATION | WON | LOST`
  - Backend `LeadSource` enum: `WEBMOTORS | MERCADO_LIVRE | WEBSITE | PHONE | WALK_IN | REFERRAL`
  - Frontend `LeadOrigin` type (line 20): `'OLX' | 'Mercado Livre' | 'Instagram' | 'WebMotors' | 'Facebook' | 'Mobiauto' | 'Napista' | 'Direto'`
  - Frontend `KanbanColumn` type (line 39): `'chatbot' | 'aguardando' | 'aguardando_documentacao' | 'negociacao' | 'concluido'`
- Impact: When API integration happens, every type will need to be rewritten. The current Kanban column model does not match the backend lead stage pipeline at all. This will require significant refactoring of both types and all components that consume them.
- Fix approach: Create `frontend/src/types/api.types.ts` with DTOs that mirror the backend Kotlin DTOs exactly. Keep UI-specific types separate with mapping functions between API DTOs and UI models.

### No Loading States for Data

- Issue: Since all data is currently hardcoded, no pages have loading skeletons, spinners, or any indication of data fetching. When real API integration happens, every page will show unstyled content jumps.
- Files:
  - `frontend/src/pages/Dashboard.tsx` -- renders all cards/charts immediately from mock data
  - `frontend/src/pages/Kanban.tsx` -- renders board immediately from mock data
  - `frontend/src/pages/Estoque.tsx` -- renders table immediately from mock data
  - `frontend/src/pages/Configuracoes.tsx` -- renders all tabs immediately from hardcoded data
  - `frontend/src/components/ui/skeleton.tsx` -- installed but never used anywhere
- Impact: Poor UX when API integration is added. Users will see layout shifts (CLS issues) during data loading.
- Fix approach: Create loading skeleton variants for each page/component. Use React Query's `isLoading`/`isFetching` states to conditionally show skeletons.

### No Empty States for Lists/Tables

- Issue: Tables and lists have no empty state handling. If filtered results return zero items, or if the API returns an empty array, the UI renders an empty table body with no feedback.
- Files:
  - `frontend/src/pages/Estoque.tsx` (lines 123-161) -- table body renders nothing when `filteredVehicles` is empty
  - `frontend/src/components/dashboard/StockTable.tsx` (lines 56-86) -- no empty check
  - `frontend/src/components/dashboard/RecentSalesTable.tsx` (lines 47-66) -- no empty check
  - `frontend/src/components/kanban/KanbanColumn.tsx` (line 74) -- empty column shows only blank space
- Impact: Users see confusing blank areas with no guidance on what to do.
- Fix approach: Create a reusable `frontend/src/components/shared/EmptyState.tsx` component. Add empty state checks to all list/table renders.

---

## MEDIUM Severity

### Code Duplication -- `formatCurrency` Function

- Issue: The `formatCurrency` function is duplicated identically in 5 different files (with a slight variation in `Dashboard.tsx` that handles K/M abbreviation).
- Files:
  - `frontend/src/pages/Dashboard.tsx` (lines 16-28) -- variant with K/M formatting
  - `frontend/src/pages/Estoque.tsx` (lines 17-23) -- basic version
  - `frontend/src/components/kanban/KanbanCard.tsx` (lines 26-32) -- basic version
  - `frontend/src/components/dashboard/RecentSalesTable.tsx` (lines 8-14) -- basic version
  - `frontend/src/components/dashboard/StockTable.tsx` (lines 17-23) -- basic version
- Impact: Maintenance burden. If currency formatting needs to change, it must be updated in 5 places.
- Fix approach: Extract to `frontend/src/lib/formatters.ts` with both `formatCurrency` and `formatCurrencyShort` functions.

### Code Duplication -- `formatTimeAgo` Function

- Issue: The `formatTimeAgo` function is duplicated in 2 files with nearly identical logic.
- Files:
  - `frontend/src/components/layout/Header.tsx` (lines 40-51)
  - `frontend/src/pages/Configuracoes.tsx` (lines 77-86)
- Impact: Same logic maintained in two places.
- Fix approach: Extract to `frontend/src/lib/formatters.ts`.

### Code Duplication -- `statusConfig` Object

- Issue: The `statusConfig` record mapping vehicle statuses to labels and CSS classes is duplicated in 2 files.
- Files:
  - `frontend/src/pages/Estoque.tsx` (lines 9-15)
  - `frontend/src/components/dashboard/StockTable.tsx` (lines 9-15)
- Impact: If status labels or colors change, both files must be updated.
- Fix approach: Extract to a shared constants file, e.g., `frontend/src/lib/constants.ts`.

### No `React.StrictMode` Wrapper

- Issue: `frontend/src/main.tsx` renders `<App />` without `<React.StrictMode>`, which disables React's development-mode double-rendering checks for detecting side effects and deprecated APIs.
- Files: `frontend/src/main.tsx` (line 5)
- Impact: Potential side-effect bugs that would be caught by StrictMode go undetected during development.
- Fix approach: Wrap `<App />` in `<React.StrictMode>` in `frontend/src/main.tsx`.

### No Lazy Loading of Routes

- Issue: All page components are eagerly imported in `frontend/src/App.tsx` (lines 8-13). No `React.lazy()` or `Suspense` is used.
- Files: `frontend/src/App.tsx` (lines 8-13)
- Impact: The entire application loads as a single bundle. As the app grows (especially with landing page, communication, SaaS modules), initial load time will degrade significantly.
- Fix approach: Use `React.lazy()` for page-level components and wrap routes in `<Suspense>` with a loading fallback.

### ESLint `no-unused-vars` Disabled

- Issue: The ESLint rule `@typescript-eslint/no-unused-vars` is explicitly set to `"off"` in `frontend/eslint.config.js` (line 23).
- Files: `frontend/eslint.config.js` (line 23)
- Impact: Unused imports, variables, and function parameters accumulate silently, increasing noise and potential confusion.
- Fix approach: Re-enable the rule with `"warn"` or `"error"` severity and clean up existing violations.

### No Prettier Configuration

- Issue: No Prettier config file exists (`.prettierrc`, `.prettierrc.js`, etc.). No `prettier` package in `package.json`. Code formatting is inconsistent across files.
- Files: `frontend/package.json` -- no prettier dependency
- Impact: Inconsistent formatting across files, higher friction in code reviews, and formatting conflicts between developers.
- Fix approach: Add `prettier` and `eslint-config-prettier` as devDependencies. Create `.prettierrc` with project standards. Add `format` script to `package.json`.

### 34 Unused shadcn/ui Components Installed

- Issue: Out of 49 files in `frontend/src/components/ui/`, only 13 are imported by application code. The remaining 34 components are boilerplate from shadcn/ui initialization that add no value and increase the mental overhead of the codebase.
- Files: All files in `frontend/src/components/ui/` that are never imported by application code:
  `accordion.tsx`, `alert-dialog.tsx`, `alert.tsx`, `aspect-ratio.tsx`, `avatar.tsx`, `badge.tsx`, `breadcrumb.tsx`, `calendar.tsx`, `card.tsx`, `carousel.tsx`, `chart.tsx`, `checkbox.tsx`, `command.tsx`, `context-menu.tsx`, `drawer.tsx`, `form.tsx`, `hover-card.tsx`, `input-otp.tsx`, `menubar.tsx`, `navigation-menu.tsx`, `pagination.tsx`, `progress.tsx`, `radio-group.tsx`, `resizable.tsx`, `separator.tsx`, `sheet.tsx`, `sidebar.tsx`, `skeleton.tsx`, `slider.tsx`, `table.tsx`, `tabs.tsx`, `toggle-group.tsx`, `toggle.tsx`
- Impact: Codebase noise. Some of these components (e.g., `card.tsx`, `badge.tsx`, `skeleton.tsx`, `table.tsx`, `tabs.tsx`) will likely be needed in future phases, but most (e.g., `input-otp.tsx`, `menubar.tsx`, `resizable.tsx`, `carousel.tsx`) are unlikely to be used. Unused components also pull in unused Radix dependencies.
- Fix approach: Keep components that will be needed soon (`card`, `badge`, `skeleton`, `table`, `tabs`, `separator`, `progress`, `avatar`, `sheet`, `checkbox`, `form`). Remove clearly unneeded ones (`input-otp`, `menubar`, `resizable`, `carousel`, `navigation-menu`, `hover-card`, `context-menu`, `aspect-ratio`). Reinstall from shadcn/ui CLI when needed.

### Navbar Not Responsive -- No Mobile Hamburger Menu

- Issue: The header navigation in `frontend/src/components/layout/Header.tsx` renders all nav items in a horizontal flex row (line 72) with no hamburger menu toggle for mobile viewports. On screens narrower than ~768px, nav items will overflow or wrap awkwardly.
- Files:
  - `frontend/src/components/layout/Header.tsx` (lines 72-89) -- `<nav className="flex items-center gap-1">` with no mobile breakpoint logic
  - `frontend/src/hooks/use-mobile.tsx` -- `useIsMobile()` hook exists but is never used anywhere
- Impact: Navigation is broken on mobile devices. Users cannot access all pages.
- Fix approach: Use the existing `useIsMobile()` hook to conditionally render a hamburger menu (Sheet/Drawer) on mobile. Add `hidden md:flex` to desktop nav, `md:hidden` to mobile menu trigger.

### `console.error` in Production Code

- Issue: `frontend/src/pages/NotFound.tsx` (line 8) has a `console.error` call that will log to production console.
- Files: `frontend/src/pages/NotFound.tsx` (line 8)
- Impact: Information leakage in production. The route path is logged to the browser console.
- Fix approach: Remove the `console.error` or wrap it in a development-only check. Use proper error tracking (Sentry) in production.

### No `.env` Files

- Issue: No `.env`, `.env.development`, or `.env.example` files exist in `frontend/`. The application references no environment variables.
- Files: `frontend/` -- no `.env*` files present
- Impact: When API integration is added, there is no documented or configured way to set `VITE_API_URL`, `VITE_ENABLE_MOCK`, or other environment variables.
- Fix approach: Create `frontend/.env.example` with documented variables. Create `frontend/.env.development` with local development defaults. Add `.env` to `.gitignore`.

### `any` Type Usage in CardDetailModal

- Issue: Two `onValueChange` handlers use explicit `any` type casts.
- Files:
  - `frontend/src/components/modals/CardDetailModal.tsx` (line 211) -- `(value: any) => setValue('origem', value)`
  - `frontend/src/components/modals/CardDetailModal.tsx` (line 263) -- `(value: any) => setValue('urgencia', value || undefined)`
- Impact: Type safety hole. The correct types (`LeadOrigin`, urgency enum) should be used.
- Fix approach: Cast to the correct union type: `(value: LeadOrigin) => setValue('origem', value)`.

---

## LOW Severity

### Missing `cursor-pointer` on Interactive Elements

- Issue: Most buttons and clickable elements throughout the application do not have `cursor-pointer` applied. Only 4 instances use it across the entire codebase.
- Files:
  - `frontend/src/pages/Estoque.tsx` (lines 56-62) -- "Adicionar Veiculo" button, no cursor-pointer
  - `frontend/src/pages/Estoque.tsx` (line 154) -- "Editar" button, no cursor-pointer
  - `frontend/src/pages/Configuracoes.tsx` (lines 247-253) -- "Adicionar Usuario" button
  - `frontend/src/components/kanban/KanbanCard.tsx` (lines 93-99) -- action buttons
  - `frontend/src/components/layout/Header.tsx` (line 96) -- notification bell button
- Impact: UX degradation. Users do not get visual feedback that elements are clickable.
- Fix approach: Ensure all `<button>` elements and clickable `<div>`s include `cursor-pointer`. Consider adding it to the global button styles.

### No `prefers-reduced-motion` Support

- Issue: Custom animations in `frontend/src/index.css` (lines 122-149) define `animate-fade-in` and `animate-slide-up` without respecting the `prefers-reduced-motion` media query. These animations are used extensively across dashboard and settings pages.
- Files:
  - `frontend/src/index.css` (lines 122-149) -- animation definitions without motion preference check
  - Used in: `KPICard.tsx`, `SalesLineChart.tsx`, `CategoryBarChart.tsx`, `StockTable.tsx`, `RecentSalesTable.tsx`, `Estoque.tsx`, `Configuracoes.tsx`
- Impact: Accessibility violation for users with vestibular disorders who have motion sensitivity.
- Fix approach: Add `@media (prefers-reduced-motion: reduce) { .animate-fade-in, .animate-slide-up { animation: none; } }` to `index.css`.

### No Accessibility Attributes in Custom Components

- Issue: Custom components (non-shadcn/ui) lack ARIA attributes. All ARIA usage comes exclusively from shadcn/ui boilerplate components that are mostly unused.
- Files:
  - `frontend/src/components/kanban/KanbanBoard.tsx` -- no ARIA roles for drag-and-drop regions
  - `frontend/src/components/layout/Header.tsx` -- notification bell button lacks `aria-label`
  - `frontend/src/pages/Estoque.tsx` -- filter select lacks proper labeling
  - `frontend/src/pages/Login.tsx` -- tab buttons lack `role="tab"` and `aria-selected`
- Impact: Screen reader users cannot navigate custom interactive components.
- Fix approach: Audit all interactive elements and add appropriate ARIA attributes.

### 588-Line Configuracoes Page -- Too Large

- Issue: `frontend/src/pages/Configuracoes.tsx` is 588 lines long with 4 tabs rendered inline via conditional `activeSection` checks. Each tab contains complex UI (tables, forms, switches) that should be separate components.
- Files: `frontend/src/pages/Configuracoes.tsx` (588 lines)
- Impact: Difficult to maintain and test. Changes to one tab risk breaking another.
- Fix approach: Extract each tab into its own component: `GeneralTab.tsx`, `NotificationsTab.tsx`, `SecurityTab.tsx`, `PaymentsTab.tsx`. Place them in `frontend/src/components/settings/`.

### Photo Upload Stores Base64 in State

- Issue: `frontend/src/components/modals/AddVehicleModal.tsx` (lines 80-91) reads uploaded photos as base64 data URLs and stores them in React state. There is no file size validation despite claiming "PNG, JPG ate 5MB cada" (line 314).
- Files: `frontend/src/components/modals/AddVehicleModal.tsx` (lines 80-91, 110, 313-314)
- Impact: Large photos stored as base64 strings can consume significant memory. No actual upload to a backend or file storage occurs. When API integration happens, this needs to change to multipart form upload.
- Fix approach: Add file size validation. When API is integrated, upload files to backend/S3 and store only URLs.

### Hardcoded Data in Configuracoes Page

- Issue: The settings page contains hardcoded user profile data, payment information, billing history, and session data that should come from the API.
- Files:
  - `frontend/src/pages/Configuracoes.tsx` (line 141) -- hardcoded name "Carlos Mendes"
  - `frontend/src/pages/Configuracoes.tsx` (line 142) -- hardcoded email
  - `frontend/src/pages/Configuracoes.tsx` (line 171) -- hardcoded phone
  - `frontend/src/pages/Configuracoes.tsx` (lines 502-516) -- hardcoded subscription plan
  - `frontend/src/pages/Configuracoes.tsx` (lines 536-540) -- hardcoded billing history
  - `frontend/src/pages/Configuracoes.tsx` (lines 568-575) -- hardcoded payment method
  - `frontend/src/pages/Configuracoes.tsx` (lines 477-487) -- hardcoded session data
- Impact: All settings data is static. Changes are not persisted. Will need full rework during API integration.
- Fix approach: Will be resolved as part of the API integration phase. Create `useProfile()`, `useSubscription()`, `useBillingHistory()` hooks.

### No Pre-commit Hooks

- Issue: No Husky, lint-staged, or any pre-commit hook configuration exists.
- Files: No `.husky/` directory, no `lint-staged` in `package.json`
- Impact: Developers can commit code that fails linting or has TypeScript errors without being caught.
- Fix approach: Install `husky` and `lint-staged`. Configure pre-commit to run `eslint` and `tsc --noEmit`.

### Notification Context Uses Only In-Memory Hardcoded Data

- Issue: `frontend/src/contexts/NotificationContext.tsx` initializes with hardcoded notifications (lines 16-41) and stores all state in React state only. Notifications are lost on page refresh.
- Files: `frontend/src/contexts/NotificationContext.tsx` (lines 16-41)
- Impact: No real notification system. All notifications are fake and non-persistent.
- Fix approach: Integrate with backend notification system (WebSocket or polling). Persist notification read state.

---

## Test Coverage Gaps

### Effectively Zero Test Coverage

- What's not tested: The entire application. The only test file is a placeholder.
- Files:
  - `frontend/src/test/example.test.ts` -- single `expect(true).toBe(true)` test
  - `frontend/vitest.config.ts` -- configured but no real tests exist
  - `frontend/src/test/setup.ts` -- test setup with `jest-dom` matchers, ready but unused
- Risk: Any refactoring or feature addition can break existing functionality with zero safety net. No component tests, no hook tests, no integration tests, no E2E tests.
- Priority: High -- should be addressed in parallel with API integration to ensure correctness.

---

## Dependencies at Risk

### `next-themes` -- Wrong Ecosystem

- Risk: `next-themes` is a Next.js-specific package being used in a Vite + React app. It may have unexpected behavior or bundle inefficiencies outside the Next.js context.
- Impact: Currently only used by `frontend/src/components/ui/sonner.tsx` (shadcn/ui boilerplate). May cause SSR-related warnings in browser console.
- Migration plan: Replace with the app's own `ThemeContext` import.

### `@radix-ui/react-toast` -- Unused, Conflicts with Sonner

- Risk: Installed but completely unused. Adds bundle weight and has a mounted `<ToastProvider>` that could conflict with Sonner.
- Impact: ~15KB unused bundle weight. Potential z-index or positioning conflicts.
- Migration plan: Remove package and all associated files.

### Bloated Radix UI Dependencies

- Risk: Many `@radix-ui/*` packages are installed (26 packages in `package.json` lines 18-44) to support shadcn/ui components, but only ~13 components are actually used. The unused Radix packages add to `node_modules` size and lock file complexity.
- Impact: Slower `npm install`, larger `node_modules`, more potential CVE surface area.
- Migration plan: Remove unused shadcn/ui components and their corresponding Radix dependencies.

---

## Security Considerations

### Token Storage Not Implemented

- Risk: When auth is implemented, there is a high risk of storing JWT in `localStorage` (the naive approach), which is vulnerable to XSS attacks.
- Files: `frontend/src/contexts/ThemeContext.tsx` (lines 15, 20, 34, 40) already uses `localStorage` for theme preferences, establishing a pattern that could be followed for tokens.
- Current mitigation: None -- auth does not exist yet.
- Recommendations: Store JWT in httpOnly cookies (requires backend CORS + cookie config) or in-memory with refresh token rotation. Never store tokens in `localStorage`.

### No Input Sanitization

- Risk: User inputs in forms (lead details, vehicle data, user data) are validated with Zod for shape/format but never sanitized against XSS. If any input is rendered as HTML (e.g., via `dangerouslySetInnerHTML` in the future), it could enable XSS.
- Files:
  - `frontend/src/components/modals/CardDetailModal.tsx` -- lead form
  - `frontend/src/components/modals/AddVehicleModal.tsx` -- vehicle form
  - `frontend/src/components/modals/AddUserModal.tsx` -- user form
- Current mitigation: React's JSX rendering escapes strings by default, preventing basic XSS.
- Recommendations: Add DOMPurify for any rich text fields. Ensure no `dangerouslySetInnerHTML` is used without sanitization.

### No CSRF Protection

- Risk: When API integration is added with cookie-based authentication, the application will be vulnerable to CSRF attacks unless CSRF tokens are implemented.
- Current mitigation: None -- no API calls exist.
- Recommendations: Implement CSRF token header in the Axios request interceptor when auth is added.

---

## Performance Bottlenecks

### No Code Splitting

- Problem: The entire application is bundled as a single chunk. Every page, component, and library is loaded on initial page load.
- Files: `frontend/src/App.tsx` (lines 8-13) -- all pages imported eagerly
- Cause: No `React.lazy()` or dynamic imports used anywhere.
- Improvement path: Implement route-level code splitting with `React.lazy()`. Split heavy libraries (Recharts, `@hello-pangea/dnd`) into separate chunks via Vite's `manualChunks` config.

### Recharts Imported for Every Dashboard Visit

- Problem: Recharts is a heavy library (~200KB minified) that is imported synchronously by `SalesLineChart.tsx` and `CategoryBarChart.tsx`.
- Files:
  - `frontend/src/components/dashboard/SalesLineChart.tsx` (line 1)
  - `frontend/src/components/dashboard/CategoryBarChart.tsx` (line 1)
- Cause: No lazy loading or dynamic import of chart components.
- Improvement path: Lazy-load chart components so Recharts is only loaded when the Dashboard page is visited.

---

*Concerns audit: 2026-03-26*
