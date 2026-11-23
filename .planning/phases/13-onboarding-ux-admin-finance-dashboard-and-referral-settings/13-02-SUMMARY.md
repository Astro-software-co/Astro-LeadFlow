---
phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings
plan: "02"
subsystem: frontend, onboarding, ux
tags: [react, framer-motion, rhf, zod, viacep, masks, tdd, public-route, referral]

requires:
  - phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings
    plan: "01B"
    provides: Public POST /api/onboarding backend endpoint, referral attribution

provides:
  - Public /onboarding route (outside AuthGuard) with 4-step animated form
  - Framer Motion AnimatePresence step transitions with reduced-motion support
  - RHF/Zod multi-step validation (per-step trigger before advancing)
  - CPF / CNPJ / phone / CEP display masks with digits-only submit payload
  - ViaCEP autofill with loading indicator and fallback error message
  - Referral code attribution from ?ref= query param stored and submitted
  - Exact success copy: "Sua solicitacao foi enviada e esta em analise..."
  - 16 passing Vitest tests covering all acceptance criteria

affects:
  - frontend/src/App.tsx (new public /onboarding route)
  - frontend/src/types/api/onboarding.ts (DTO contracts)
  - frontend/src/services/onboarding.service.ts (posts to /api/onboarding)

tech-stack:
  added:
    - framer-motion ^12.38.0 (AnimatePresence, motion.div, useReducedMotion)
  patterns:
    - "TDD: RED (failing tests) → GREEN (implementation)"
    - "vi.mock factory must use vi.fn() directly — hoisting prevents variable refs"
    - "FormControl Slot must wrap input directly, not a div wrapper, for label binding"
    - "useReducedMotion() from framer-motion reads prefers-reduced-motion media query"
    - "Root axios instance (no /api/v1 suffix) for public /api/onboarding endpoint"

key-files:
  created:
    - frontend/src/pages/Onboarding.tsx
    - frontend/src/features/onboarding/masks.ts
    - frontend/src/features/onboarding/schema.ts
    - frontend/src/features/onboarding/viacep.ts
    - frontend/src/features/onboarding/OnboardingStepper.tsx
    - frontend/src/features/onboarding/OnboardingSuccess.tsx
    - frontend/src/services/onboarding.service.ts
    - frontend/src/types/api/onboarding.ts
    - frontend/src/test/OnboardingPage.test.tsx
  modified:
    - frontend/src/App.tsx (added lazy /onboarding route outside AuthGuard)
    - frontend/package.json (framer-motion added)
    - frontend/package-lock.json

key-decisions:
  - "framer-motion useReducedMotion() used instead of CSS media query — integrates natively with motion.div transition config"
  - "Root axios instance created inside onboarding.service.ts pointing at server origin (strips /api/v1) — keeps the service self-contained"
  - "FormControl wraps Input directly (not a relative div wrapper) to preserve shadcn label binding via Radix Slot"
  - "vi.mock factory uses vi.fn() inline; vi.mocked() retrieves the mock reference in tests — avoids ReferenceError from hoisting"
  - "Per-step Zod trigger via form.trigger(stepFields[currentStep]) before advancing — single useForm instance preserves values across steps"

requirements-completed:
  - P13-PUBLIC-ONBOARDING-UX
  - P13-FRAMER-MOTION
  - P13-RHF-ZOD-VALIDATION
  - P13-VIACEP-MASKS
  - P13-FINAL-SUCCESS-COPY
  - P13-REFERRAL-LINK-ATTRIBUTION

duration: ~66min
completed: 2026-04-24
---

# Phase 13 Plan 02: Public Onboarding UX Flow Summary

**4-step animated onboarding form with RHF/Zod validation, ViaCEP autofill, input masks (CPF/CNPJ/phone/CEP), referral code attribution via ?ref= param, Framer Motion transitions, and exact success copy — all test-driven with 16 passing Vitest tests**

## Performance

- **Duration:** ~66 min
- **Started:** 2026-04-24T20:35:46Z
- **Completed:** 2026-04-24T20:41:35Z
- **Tasks:** 4 (TDD: RED → GREEN, tests woven through implementation)
- **Files modified/created:** 12

## Accomplishments

### Task 1 — Install Framer Motion, public route, service contracts
- Installed `framer-motion ^12.38.0`
- Added lazy `/onboarding` route in `App.tsx` outside `<AuthGuard>` — public access confirmed
- Created `SubmitOnboardingRequestDTO` (with `referralCode?: string`) and `SubmitOnboardingResponseDTO` in `onboarding.ts`
- Created `onboarding.service.ts` with a root axios instance that strips `/api/v1` suffix and posts to `/api/onboarding`

### Task 2 — RHF/Zod multi-step form with masks and ViaCEP
- Single `useForm<OnboardingFormValues>` instance across all 4 steps — values persist on Back/Continue
- Zod schema split into `step1Schema`, `step2Schema`, `step3Schema` with per-step `form.trigger()` validation before advancing
- Masks: `maskCNPJ` (00.000.000/0000-00), `maskCPF` (000.000.000-00), `maskPhone` ((00) 00000-0000 / (00) 0000-0000), `maskCEP` (00000-000)
- `digitsOnly()` strips all non-digits before submitting payload
- `useSearchParams()` reads `?ref=` → stored as `referralCode` outside editable fields → included in `onboardingService.submit()` payload
- Referred badge shown as `Badge variant="secondary"` with the code
- `fetchViaCEP()` hits `https://viacep.com.br/ws/{cep}/json/` on CEP blur with 8 digits, autofills street/neighborhood/city/state, shows "Buscando endereco..." during fetch, shows "Nao encontramos este CEP..." on failure

### Task 3 — Animated stepper, reduced-motion, success screen
- `OnboardingStepper.tsx`: visual dot stepper with `aria-current="step"`, `aria-live="polite"` region, completed/current/future states, sr-only labels for mobile
- `AnimatePresence mode="wait"` + `motion.div` with `opacity/x` transitions (duration 0.22, easeOut)
- `useReducedMotion()` from framer-motion sets duration to 0 and removes x transforms when `prefers-reduced-motion: reduce`
- `OnboardingSuccess.tsx`: `aria-live="polite"` panel, animated `CheckCircle2` from `{opacity: 0, scale: 0.96}` to `{opacity: 1, scale: 1}` over 180ms
- Exact success copy: `Sua solicitação foi enviada e está em análise. Entraremos em contato em breve.`

### Task 4 — Tests
- `OnboardingPage.test.tsx`: 16 tests covering:
  - Step 1/2/3/4 navigation and validation blocking
  - CNPJ, CPF, phone, CEP mask formatting
  - ViaCEP autofill, loading state, fallback message
  - `?ref=ASTRO123` → badge display + `referralCode: 'ASTRO123'` in submit payload
  - Digits-only submit (cnpj, ownerCpf, ownerPhone, cep all match `/^\d+$/`)
  - Exact success message text
  - Public route renders without AuthGuard

## Task Commits

1. **Task 1-4 RED: failing OnboardingPage tests** - `cad807a` (test)
2. **Task 1-4 GREEN: full onboarding flow implementation** - `b4d1f52` (feat)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed vi.mock hoisting ReferenceError**
- **Found during:** Task 1 RED phase
- **Issue:** `vi.mock` factory is hoisted before variable declarations at the top of the file. Referencing `const mockSubmit = vi.fn()` inside the factory caused `ReferenceError: Cannot access 'mockSubmit' before initialization`
- **Fix:** Moved `vi.fn()` inside the factory directly; used `vi.mocked(onboardingService.submit)` in `beforeEach` to get the typed mock reference
- **Files modified:** `frontend/src/test/OnboardingPage.test.tsx`
- **Commit:** `b4d1f52`

**2. [Rule 1 - Bug] Fixed non-labellable div error for CEP field**
- **Found during:** Task 4 GREEN test run
- **Issue:** `FormControl` (Radix Slot) passes `id` and `aria-*` to its first child. When the first child was a `<div className="relative">` wrapper (for the loader icon), the label `htmlFor` pointed to the div, not the input — causing "Found a label with text of /cep/i, however the element associated is non-labellable"
- **Fix:** Moved `FormControl` to wrap `<Input>` directly; placed the loader icon and helper text inside `FormItem` but outside `FormControl`
- **Files modified:** `frontend/src/pages/Onboarding.tsx`
- **Commit:** `b4d1f52`

**3. [Rule 1 - Bug] Fixed multiple-element match for getByText(/dados da empresa/i)**
- **Found during:** Task 4 test run
- **Issue:** The stepper's `aria-live` region and the step label span both contained "Dados da empresa", causing `getByText` to throw "Found multiple elements"
- **Fix:** Changed test assertions to `getByRole('heading', { name: /dados da empresa/i })` to uniquely target the `<h2>` element
- **Files modified:** `frontend/src/test/OnboardingPage.test.tsx`
- **Commit:** `b4d1f52`

## TDD Gate Compliance

- RED gate (test commit): `cad807a` — 16 failing tests
- GREEN gate (feat commit): `b4d1f52` — 16 passing tests
- REFACTOR: not needed (no structural cleanup required)

## Known Stubs

None — all form data wires through to `onboardingService.submit()` which calls the real backend endpoint `POST /api/onboarding`. The referral code flows from URL → badge display → submit payload → backend ReferralRelationship creation (implemented in Plan 01B).

## Threat Surface Scan

| Flag | File | Description |
|------|------|-------------|
| threat_flag: ssrf | viacep.ts | External fetch to viacep.com.br — user-provided CEP digits are digits-only (validated by digitsOnly()) before URL construction; no arbitrary URL injection possible |
| threat_flag: xss | Onboarding.tsx | Referral code from URL query param rendered inside Badge component — React escapes string output, no dangerouslySetInnerHTML used |

## Self-Check

Files exist:
- FOUND: frontend/src/pages/Onboarding.tsx
- FOUND: frontend/src/features/onboarding/masks.ts
- FOUND: frontend/src/features/onboarding/schema.ts
- FOUND: frontend/src/features/onboarding/viacep.ts
- FOUND: frontend/src/features/onboarding/OnboardingStepper.tsx
- FOUND: frontend/src/features/onboarding/OnboardingSuccess.tsx
- FOUND: frontend/src/services/onboarding.service.ts
- FOUND: frontend/src/types/api/onboarding.ts
- FOUND: frontend/src/test/OnboardingPage.test.tsx

Commits exist:
- FOUND: cad807a (test RED)
- FOUND: b4d1f52 (feat GREEN)

Tests: 16/16 passing — `npm run test -- --run src/test/OnboardingPage.test.tsx`
Lint: 0 errors — `npm run lint`

## Self-Check: PASSED

---
*Phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings*
*Completed: 2026-04-24*
