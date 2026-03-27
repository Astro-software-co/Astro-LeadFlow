# Coding Conventions

**Analysis Date:** 2026-03-26

## Naming Patterns

**Files:**
- React components: `PascalCase.tsx` (e.g., `KPICard.tsx`, `MainLayout.tsx`, `AddVehicleModal.tsx`)
- Custom hooks: `kebab-case.tsx` or `.ts` (e.g., `use-mobile.tsx`, `use-toast.ts`) -- follows shadcn/ui convention
- Contexts: `PascalCase.tsx` (e.g., `ThemeContext.tsx`, `NotificationContext.tsx`)
- Types: `index.ts` in `src/types/` -- single barrel file
- Data/mock files: `camelCase.ts` (e.g., `mockData.ts`)
- Utility files: `camelCase.ts` (e.g., `utils.ts`)
- Pages: `PascalCase.tsx` -- Portuguese names (e.g., `Configuracoes.tsx`, `Estoque.tsx`, `Kanban.tsx`)

**Components:**
- Use PascalCase for component names: `KPICard`, `KanbanBoard`, `MainLayout`
- Suffix with purpose: `*Modal` for modals, `*Card` for cards, `*Table` for tables, `*Chart` for charts
- Component and file names match exactly: `KanbanBoard` lives in `KanbanBoard.tsx`

**Functions:**
- camelCase for all functions: `formatCurrency`, `handleDragEnd`, `handleSignOut`
- Event handlers prefixed with `handle`: `handleSubmit`, `handleAddVehicle`, `handleCardClick`
- Formatting utilities prefixed with `format`: `formatCurrency`, `formatTimeAgo`, `formatDate`

**Variables:**
- camelCase for variables: `searchQuery`, `statusFilter`, `isModalOpen`
- Boolean state prefixed with `is`/`has`/`show`: `isLogin`, `isDetailModalOpen`, `showPassword`, `showAssumeButton`
- State setters follow `set[Name]` convention: `setTheme`, `setCards`, `setSearchQuery`

**Types:**
- Use `interface` for object shapes (component props, DTOs): `KPICardProps`, `Vehicle`, `Lead`
- Use `type` for unions and mapped types: `VehicleStatus`, `LeadOrigin`, `KanbanColumn`, `CardTag`
- Props interfaces suffixed with `Props`: `KPICardProps`, `KanbanBoardProps`, `AddVehicleModalProps`
- Form data types suffixed with `FormData`: `VehicleFormData`, `LeadFormData`, `UserFormData`

## Code Style

**Formatting:**
- No Prettier configured -- formatting is manual/inconsistent
- Indentation: 2 spaces (consistent across all files)
- Semicolons: not enforced, mostly present
- Quotes: single quotes for imports and strings in `.tsx` files, double quotes in some config files
- Trailing commas: used in most places

**Linting:**
- ESLint 9 with flat config: `eslint.config.js`
- Extends: `@eslint/js` recommended + `typescript-eslint` recommended
- Plugins: `react-hooks` (recommended rules), `react-refresh` (only-export-components as warning)
- `@typescript-eslint/no-unused-vars`: **disabled** (`"off"`)
- No custom rules beyond defaults

**TypeScript strictness:**
- `strict: false` in `tsconfig.app.json`
- `noImplicitAny: false`
- `noUnusedLocals: false`
- `noUnusedParameters: false`
- `noFallthroughCasesInSwitch: false`
- Target: ES2020, Module: ESNext, JSX: react-jsx

## Import Organization

**Order (observed pattern, not enforced):**
1. React and React ecosystem (`react`, `react-dom`, `react-router-dom`)
2. Third-party libraries (`@hello-pangea/dnd`, `recharts`, `zod`, `react-hook-form`)
3. UI components from shadcn/ui (`@/components/ui/*`)
4. Application components (`@/components/*`)
5. Hooks and contexts (`@/hooks/*`, `@/contexts/*`)
6. Types (`@/types`)
7. Data (`@/data/mockData`)
8. Utilities (`@/lib/utils`)

**Path Aliases:**
- `@/*` maps to `./src/*` (configured in both `tsconfig.json` and `vite.config.ts`)
- Use `@/components/ui/button` not `../../components/ui/button`
- Relative imports used only within same directory (e.g., `./Header` from `MainLayout.tsx`)

**Import style:**
- Named imports preferred: `import { Button } from '@/components/ui/button'`
- Default exports used for pages: `export default function Dashboard()`
- Named exports used for components: `export function KPICard()`
- Icon imports are destructured: `import { Car, User, Bell } from 'lucide-react'`
- Type imports use inline `type` keyword in some places, not consistently enforced

## Component Patterns

**Functional components only:**
- All components are function components (no class components)
- Named function declarations for exported components: `export function KPICard() {}`
- Arrow functions for the root `App` component: `const App = () => (`
- `forwardRef` used where needed (e.g., `NavLink.tsx`, shadcn/ui components)

**Props pattern:**
```typescript
// Define interface above component
interface KPICardProps {
  title: string;
  value: string;
  trend: number;
  icon: LucideIcon;
  iconBgColor?: string;
}

// Destructure in function signature with defaults
export function KPICard({ title, value, trend, icon: Icon, iconBgColor = 'bg-primary/10' }: KPICardProps) {
```

**Page component pattern:**
```typescript
// Default export, wraps in MainLayout, uses mock data
import { MainLayout } from '@/components/layout/MainLayout';
import { mockData } from '@/data/mockData';

export default function PageName() {
  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Page content */}
      </div>
    </MainLayout>
  );
}
```

**Modal component pattern:**
```typescript
// Uses shadcn/ui Dialog, react-hook-form + zod for validation
interface ModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: DataType) => void;
}

export function SomeModal({ open, onOpenChange, onSubmit }: ModalProps) {
  const { register, handleSubmit, setValue, watch, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { ... },
  });

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>...</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onFormSubmit)}>...</form>
      </DialogContent>
    </Dialog>
  );
}
```

**Context pattern:**
```typescript
// Create context with undefined default
const SomeContext = createContext<ContextType | undefined>(undefined);

// Provider component with state
export function SomeProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState(...);
  return <SomeContext.Provider value={{...}}>{children}</SomeContext.Provider>;
}

// Custom hook with guard
export function useSomething() {
  const context = useContext(SomeContext);
  if (context === undefined) {
    throw new Error('useSomething must be used within a SomeProvider');
  }
  return context;
}
```

## Tailwind CSS Patterns

**Class composition with `cn()`:**
```typescript
import { cn } from '@/lib/utils';

// Conditional classes
className={cn(
  'base-classes here',
  isActive ? 'active-classes' : 'inactive-classes'
)}

// Error state on inputs
className={errors.field ? 'border-destructive' : ''}
```

**Design tokens via CSS custom properties:**
- All colors use HSL CSS variables: `hsl(var(--primary))`, `hsl(var(--background))`
- Tailwind maps semantic names: `text-foreground`, `bg-card`, `border-border`
- Domain-specific tokens: `bg-kanban-chatbot`, `text-status-available`, `text-chart-primary`, `text-trend-positive`

**Common utility patterns:**
- Card containers: `rounded-xl border bg-card p-6 shadow-sm animate-fade-in`
- Section headers: `text-lg font-semibold text-foreground` with `text-sm text-muted-foreground` subtitle
- Table headers: `px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground`
- Table rows: `hover:bg-muted/20 transition-colors`
- Status badges: `inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium`
- Icon containers: `flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10`
- Page layout spacing: `space-y-6` for vertical sections, `gap-4` for grid items

**Opacity modifiers used extensively:**
- Background tints: `bg-primary/10`, `bg-kanban-completed/10`, `bg-muted/30`
- Hover states: `hover:bg-primary/90`, `hover:bg-muted/50`
- Border opacity: `border-border/50`

**Custom animations (defined in `src/index.css`):**
- `animate-fade-in`: 0.3s fade-in, applied to cards and sections
- `animate-slide-up`: 0.3s slide-up with translateY

**Dark mode:**
- Class-based: `darkMode: ["class"]` in `tailwind.config.ts`
- CSS variables swap in `.dark` class (defined in `src/index.css`)
- Theme toggle managed by `ThemeContext` -- sets/removes `.dark` on `<html>`

## shadcn/ui Usage Patterns

**Configuration:**
- Style: `default` (not `new-york`)
- Base color: `slate`
- CSS variables: enabled
- RSC: disabled (`rsc: false`)
- Prefix: none

**49 UI components installed** in `src/components/ui/`

**Commonly used components:**
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

**Do NOT manually edit** files in `src/components/ui/` -- these are generated by the shadcn/ui CLI.

## State Management Patterns

**Local state with `useState`:**
- All state is currently local -- no global state management library
- Forms use `react-hook-form` with `zodResolver` for validation
- Mock data initialized via `useState(mockData)` at page level

**React Context for cross-cutting concerns:**
- `ThemeContext` (`src/contexts/ThemeContext.tsx`): theme mode (light/dark/system) + accent color, persisted to `localStorage`
- `NotificationContext` (`src/contexts/NotificationContext.tsx`): in-memory notification list with CRUD operations

**React Query:**
- `QueryClientProvider` wrapping the app in `App.tsx`
- `QueryClient` instantiated at module level with default config
- **Not actively used anywhere** -- all data comes from `src/data/mockData.ts`

## Form Handling

**Stack: react-hook-form + zod + @hookform/resolvers**

**Pattern:**
```typescript
const schema = z.object({
  field: z.string().min(2, 'Error message'),
});
type FormData = z.infer<typeof schema>;

const { register, handleSubmit, setValue, watch, reset, formState: { errors, isSubmitting, isDirty } } = useForm<FormData>({
  resolver: zodResolver(schema),
  defaultValues: { ... },
});
```

**Validation error display:**
```tsx
<Input {...register('field')} className={errors.field ? 'border-destructive' : ''} />
{errors.field && <p className="text-xs text-destructive">{errors.field.message}</p>}
```

**Select components** use `setValue` + `watch` instead of `register` (since shadcn/ui Select is not a native input):
```tsx
const value = watch('field');
<Select value={value} onValueChange={(v) => setValue('field', v)}>
```

## Error Handling

**Current state: minimal error handling**
- No try/catch blocks (no async operations exist yet)
- No error boundaries
- Form validation via Zod is the only structured error handling
- `toast.success()` from Sonner for success feedback -- no `toast.error()` used for API failures
- No loading states (no async data fetching)
- No empty states for lists (data always present via mock)

**Toast notifications:**
- Use `import { toast } from 'sonner'` for all toast notifications
- Pattern: `toast.success('Message')`, `toast.error('Message')`, `toast.info('Message')`
- Do NOT use the Radix Toast system (`useToast` from `@/hooks/use-toast.ts`) -- it exists but is unused and redundant

## Logging

**Framework:** `console` (no logging library)
- Single `console.error` in `NotFound.tsx` for 404 tracking
- No structured logging pattern

## Comments

**When used:**
- Section dividers in JSX: `{/* Header */}`, `{/* KPI Cards */}`, `{/* Charts */}`
- Tab section markers: `{/* ============ GENERAL TAB ============ */}`
- No JSDoc or TSDoc on any function or component
- No inline code comments explaining logic

## Module Design

**Exports:**
- Pages use `export default function PageName()` -- one per file
- Components use named exports: `export function ComponentName()` -- one per file
- Contexts export both `Provider` and `useHook` from same file
- Types barrel-exported from `src/types/index.ts`

**Barrel files:**
- Only `src/types/index.ts` acts as a barrel
- No barrel files in `components/`, `hooks/`, `contexts/`, or `pages/`
- Each component imported individually from its file path

## Internationalization

**Language: Portuguese (pt-BR) for UI text**
- All labels, headings, button text, toast messages in Portuguese
- Type names and code-level identifiers in Portuguese: `marca`, `modelo`, `preco`, `coluna`, `vendedorNome`
- Some mixed: `Lead`, `Vehicle`, `KPIData` (interface names in English), but fields in Portuguese
- Currency formatting uses `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`
- Date formatting uses `Intl.DateTimeFormat('pt-BR', ...)`

## Routing

**React Router DOM v6:**
- Flat route structure in `src/App.tsx` -- no nested routes
- Routes defined inline (not in separate config file)
- No route guards or authentication checks
- No lazy loading of route components

**Current routes:**
```typescript
<Route path="/" element={<Kanban />} />
<Route path="/dashboard" element={<Dashboard />} />
<Route path="/estoque" element={<Estoque />} />
<Route path="/configuracoes" element={<Configuracoes />} />
<Route path="/login" element={<Login />} />
<Route path="*" element={<NotFound />} />
```

---

*Convention analysis: 2026-03-26*
