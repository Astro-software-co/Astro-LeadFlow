# Testing Patterns

**Analysis Date:** 2026-03-26

## Test Framework

**Runner:**
- Vitest 3.2.4
- Config: `frontend/vitest.config.ts`
- Environment: `jsdom`
- Globals: `true` (no need to import `describe`, `it`, `expect`)
- Setup file: `frontend/src/test/setup.ts`

**Assertion Library:**
- Vitest built-in assertions (`expect`, `toBe`, etc.)
- `@testing-library/jest-dom` 6.6.0 -- extends matchers with DOM assertions (`toBeInTheDocument`, `toHaveClass`, etc.)

**Rendering Library:**
- `@testing-library/react` 16.0.0 -- installed but not used in any test

**Run Commands:**
```bash
npm run test               # Run all tests once (vitest run)
npm run test:watch         # Watch mode (vitest)
```

No coverage command is configured in `package.json` scripts.

## Test File Organization

**Location:**
- Test files go in `frontend/src/test/` directory (separate from source)
- No co-located tests exist (no `*.test.tsx` alongside components)

**Naming:**
- Pattern: `*.test.ts` or `*.test.tsx`
- Configured glob: `src/**/*.{test,spec}.{ts,tsx}`

**Current test files:**
```
frontend/src/test/
├── setup.ts              # Test setup (jest-dom matchers + matchMedia mock)
└── example.test.ts       # Single placeholder test
```

## Test Setup

**Setup file (`frontend/src/test/setup.ts`):**
```typescript
import "@testing-library/jest-dom";

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {},
  }),
});
```

This setup:
1. Imports `@testing-library/jest-dom` to add DOM matchers to Vitest's `expect`
2. Mocks `window.matchMedia` for components that use media queries (e.g., `useIsMobile` hook, theme detection)

**TypeScript config for tests:**
- `tsconfig.app.json` includes `"types": ["vitest/globals"]` for global test APIs
- Path alias `@/*` is configured in `vitest.config.ts` to match Vite

## Test Structure

**Only test (`frontend/src/test/example.test.ts`):**
```typescript
import { describe, it, expect } from "vitest";

describe("example", () => {
  it("should pass", () => {
    expect(true).toBe(true);
  });
});
```

This is a placeholder test that validates the test runner works. No actual application logic is tested.

## Mocking

**Framework:** Vitest built-in (`vi.mock`, `vi.fn`, `vi.spyOn`)

**Current mocks:**
- Only `window.matchMedia` is mocked globally in setup
- No module mocks, no API mocks, no component mocks exist

**Recommended mock patterns for this codebase:**

Mock React Router navigation:
```typescript
import { vi } from 'vitest';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => vi.fn(),
    useLocation: () => ({ pathname: '/test' }),
  };
});
```

Mock Sonner toast:
```typescript
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
}));
```

Mock contexts:
```typescript
// Wrap component in providers for testing
function renderWithProviders(ui: React.ReactElement) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <ThemeProvider>
        <NotificationProvider>
          <TooltipProvider>
            <BrowserRouter>
              {ui}
            </BrowserRouter>
          </TooltipProvider>
        </NotificationProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
```

**What to Mock:**
- `react-router-dom` navigation (`useNavigate`)
- Toast notifications (`sonner`)
- `window.matchMedia` (already handled in setup)
- Future: API calls via service layer when implemented
- `@hello-pangea/dnd` for Kanban drag-and-drop testing
- `localStorage` for theme persistence

**What NOT to Mock:**
- React contexts -- render with real providers
- shadcn/ui components -- render real components
- Zod schemas -- test real validation
- `cn()` utility -- it's pure logic

## Fixtures and Factories

**Test Data:**
- Mock data exists in `frontend/src/data/mockData.ts` -- can be imported in tests
- Provides: `mockVehicles`, `mockLeads`, `mockKanbanCards`, `mockKPIData`, `mockMonthlySales`, `mockCategorySales`, `mockRecentSales`
- No dedicated test fixtures or factory functions exist

**Example fixture from `frontend/src/data/mockData.ts`:**
```typescript
export const mockVehicles: Vehicle[] = [
  { id: '1', marca: 'Toyota', modelo: 'Corolla', ano: 2024, preco: 145000, status: 'disponivel', categoria: 'Sedan' },
  // ...
];

export const mockLeads: Lead[] = [
  { id: '1', nome: 'João Silva', telefone: '(11) 98765-4321', origem: 'OLX', dataCriacao: new Date(), ultimaInteracao: new Date() },
  // ...
];
```

**Recommended factory pattern for new tests:**
```typescript
// src/test/factories.ts
function createVehicle(overrides: Partial<Vehicle> = {}): Vehicle {
  return {
    id: crypto.randomUUID(),
    marca: 'Toyota',
    modelo: 'Corolla',
    ano: 2024,
    preco: 145000,
    status: 'disponivel',
    categoria: 'Sedan',
    ...overrides,
  };
}

function createLead(overrides: Partial<Lead> = {}): Lead {
  return {
    id: crypto.randomUUID(),
    nome: 'Test Lead',
    telefone: '(11) 99999-9999',
    origem: 'Direto',
    dataCriacao: new Date(),
    ultimaInteracao: new Date(),
    ...overrides,
  };
}
```

## Coverage

**Requirements:** None enforced -- no coverage thresholds configured
**Current coverage:** Effectively 0% (only a `true === true` test exists)

**To run coverage (not yet scripted):**
```bash
npx vitest run --coverage
```

**Recommended vitest.config.ts coverage configuration:**
```typescript
test: {
  coverage: {
    provider: 'v8',
    reporter: ['text', 'html', 'lcov'],
    include: ['src/**/*.{ts,tsx}'],
    exclude: [
      'src/components/ui/**',   // shadcn/ui generated components
      'src/test/**',            // test files themselves
      'src/data/**',            // mock data
      'src/vite-env.d.ts',
    ],
    thresholds: {
      lines: 80,
      branches: 80,
      functions: 80,
      statements: 80,
    },
  },
}
```

## Test Types

**Unit Tests:**
- Not implemented
- Candidates: `formatCurrency` (duplicated 5 times), `formatTimeAgo`, `cn()`, Zod schemas, context hooks

**Component Tests:**
- Not implemented
- Framework ready: `@testing-library/react` is installed
- Candidates: `KPICard`, `KanbanCard`, `AddVehicleModal` form validation, `Login` form

**Integration Tests:**
- Not implemented
- Candidates: `KanbanBoard` drag-and-drop flow, page-level rendering with providers

**E2E Tests:**
- No framework installed (Playwright not configured)
- Not applicable at current stage

## Test Gaps (Critical)

**Zero functional test coverage.** Every component, hook, context, and utility is untested.

**Priority areas to test first:**

1. **Utility functions** (easiest wins):
   - `formatCurrency` in `src/pages/Dashboard.tsx` (and its 4 duplicates)
   - `cn()` in `src/lib/utils.ts`
   - Files: `src/lib/utils.ts`

2. **Zod validation schemas** (business logic):
   - `vehicleSchema` in `src/components/modals/AddVehicleModal.tsx`
   - `leadSchema` in `src/components/modals/CardDetailModal.tsx`
   - `userSchema` in `src/components/modals/AddUserModal.tsx`
   - `editUserSchema` in `src/components/modals/EditUserModal.tsx`

3. **Context hooks** (state management):
   - `useTheme` in `src/contexts/ThemeContext.tsx`
   - `useNotifications` in `src/contexts/NotificationContext.tsx`
   - `useIsMobile` in `src/hooks/use-mobile.tsx`

4. **Form components** (user interaction):
   - `AddVehicleModal` -- form submission, validation errors, photo upload
   - `AddUserModal` -- password confirmation, role selection
   - `CardDetailModal` -- lead data editing, section collapsing
   - `Login` -- login/register toggle, form submission

5. **Kanban board** (core business feature):
   - `KanbanBoard` -- drag-and-drop, card movement between columns
   - `KanbanCard` -- button actions (assume, advance, archive), tag rendering

6. **Dashboard components** (data visualization):
   - `KPICard` -- trend direction, value formatting
   - `SalesLineChart` / `CategoryBarChart` -- render without errors
   - `StockTable` / `RecentSalesTable` -- data mapping

7. **Page components** (integration):
   - `Dashboard` -- renders all KPI cards and charts
   - `Estoque` -- search filtering, status filtering, add vehicle flow
   - `Configuracoes` -- tab navigation, theme toggle, user management

## Recommended Test File Structure

```
frontend/src/
├── test/
│   ├── setup.ts                        # Global test setup (exists)
│   ├── helpers.tsx                      # renderWithProviders, shared utilities
│   └── factories.ts                    # Test data factories
├── lib/
│   └── utils.test.ts                   # cn() tests
├── contexts/
│   ├── ThemeContext.test.tsx            # Theme provider tests
│   └── NotificationContext.test.tsx     # Notification CRUD tests
├── hooks/
│   └── use-mobile.test.tsx             # Mobile breakpoint tests
├── components/
│   ├── dashboard/
│   │   ├── KPICard.test.tsx
│   │   └── StockTable.test.tsx
│   ├── kanban/
│   │   ├── KanbanBoard.test.tsx
│   │   └── KanbanCard.test.tsx
│   └── modals/
│       ├── AddVehicleModal.test.tsx
│       └── AddUserModal.test.tsx
└── pages/
    ├── Dashboard.test.tsx
    ├── Estoque.test.tsx
    └── Login.test.tsx
```

## Testing Checklist for New Features

When adding new code, follow this testing pattern:

1. **Pure functions/utilities:** Unit test with various inputs/edge cases
2. **Zod schemas:** Test valid inputs pass, invalid inputs fail with correct messages
3. **Custom hooks:** Test with `renderHook` from `@testing-library/react`
4. **Components:** Test rendering, user interaction (clicks, typing), conditional display
5. **Forms:** Test validation errors, successful submission, form reset
6. **Pages:** Test composition renders without errors, key interactions work

---

*Testing analysis: 2026-03-26*
