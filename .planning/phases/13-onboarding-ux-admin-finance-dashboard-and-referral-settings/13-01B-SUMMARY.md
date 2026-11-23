---
phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings
plan: "01B"
subsystem: backend, api, onboarding, referrals
tags: [kotlin, spring-boot, jpa, tdd, onboarding, referrals, rbac, controllers, services]

requires:
  - phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings
    plan: "01"
    provides: OnboardingApplication entity, ReferralRelationship entity, all DTO files, SecurityConfig RBAC, OrganizationRepository, UserRepository

provides:
  - Public POST /api/onboarding controller (OnboardingController) — no auth required
  - Admin onboarding CRUD: GET/GET/{id}/POST/{id}/approve/POST/{id}/reject at /api/v1/admin/onboarding — ROLE_ASTRO_ADMIN protected
  - OnboardingApplicationService: submit(), list(), getById()
  - OnboardingApprovalService: approve() (creates org + ADMIN user PENDING_VERIFICATION + unique referralCode + optional ReferralRelationship), reject() (reason >= 10 chars)
  - ReferralService: getTier() with BRONZE/SILVER/GOLD/DIAMOND tiers at 10/20/35 thresholds, listReferrals()
  - ReferralTier enum with 4 tiers
  - V7 Flyway migration: onboarding_applications.state CHAR(2) → VARCHAR(2)
  - OrganizationRepository.findByReferralCode() for referral attribution lookup
  - Backend tests: OnboardingControllerTest, AdminOnboardingControllerTest, OnboardingApprovalServiceTest, ReferralServiceTest

affects:
  - 13-onboarding-ux-admin-finance-dashboard-and-referral-settings (subsequent plans consuming these endpoints)

tech-stack:
  added: []
  patterns:
    - "TDD: RED (failing test) → GREEN (implementation) per task"
    - "Service delegates to Repository without SecurityUtils for ASTRO_ADMIN context (no org scope)"
    - "Approval service generates unique referral code via UUID loop with uniqueness check"
    - "ReferralRelationship created only when submitted referralCode resolves to an existing Organization"
    - "Tier thresholds: BRONZE <10, SILVER 10–19, GOLD 20–34, DIAMOND 35+"

key-files:
  created:
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/service/OnboardingApplicationService.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/service/OnboardingApprovalService.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/OnboardingController.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/AdminOnboardingController.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/service/ReferralService.kt
    - backend/src/main/resources/db/migration/V7__fix_onboarding_state_varchar.sql
    - backend/src/test/kotlin/com/astro/leadflow/shared/modules/onboarding/api/OnboardingControllerTest.kt
    - backend/src/test/kotlin/com/astro/leadflow/shared/modules/onboarding/api/AdminOnboardingControllerTest.kt
    - backend/src/test/kotlin/com/astro/leadflow/shared/modules/onboarding/service/OnboardingApprovalServiceTest.kt
    - backend/src/test/kotlin/com/astro/leadflow/shared/modules/referrals/service/ReferralServiceTest.kt
  modified:
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/OrganizationRepository.kt

key-decisions:
  - "AdminOnboardingController uses @PreAuthorize('hasRole(''ASTRO_ADMIN'')') at class level — all methods inherit RBAC"
  - "Referral code uniqueness enforced at service level via UUID loop + findByReferralCode check"
  - "Approval does not fail when submitted referralCode is unknown — silently skips ReferralRelationship creation"
  - "Tier thresholds: BRONZE <10, SILVER 10, GOLD 20, DIAMOND 35 (based on APPROVED referrals only)"
  - "V7 migration added to fix CHAR(2)→VARCHAR(2) type mismatch for onboarding_applications.state"

requirements-completed:
  - P13-PUBLIC-ONBOARDING-POST
  - P13-ADMIN-ONBOARDING-API
  - P13-REFERRAL-CODE-APPROVE

duration: 15min
completed: 2026-04-24
---

# Phase 13 Plan 01B: Onboarding Services, Controllers, and Referral Attribution Summary

**Public and admin onboarding endpoints wired to services with referral attribution, ASTRO_ADMIN RBAC enforcement, and referral tier logic — all test-driven with 4 test classes and 40+ assertions**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-24T20:15:30Z
- **Completed:** 2026-04-24T20:30:46Z
- **Tasks:** 2
- **Files modified/created:** 11 (1 modified, 10 created)

## Accomplishments

- Implemented `OnboardingApplicationService` with `submit()` (persists referralCode), `list()` with nullable status/query filters, and `getById()` with detail DTO mapping
- Implemented `OnboardingApprovalService` with full approval flow: creates Organization, creates ADMIN User with `PENDING_VERIFICATION` status, generates unique referral code (UUID loop with collision check), creates `ReferralRelationship` when submitted referralCode matches an existing organization, rejects with reason length validation (>= 10 chars)
- Implemented `OnboardingController` mapped to `POST /api/onboarding` (public — no auth required, matching SecurityConfig permit)
- Implemented `AdminOnboardingController` mapped to `/api/v1/admin/onboarding` with `@PreAuthorize("hasRole('ASTRO_ADMIN')")` at class level — list, detail, approve, reject operations
- Implemented `ReferralService` with `getTier()` (BRONZE/SILVER/GOLD/DIAMOND at 10/20/35 APPROVED referral thresholds) and `listReferrals()`
- Added `ReferralTier` enum in the referrals service package
- Added `OrganizationRepository.findByReferralCode()` for referral attribution lookup during approval
- All tasks followed TDD: RED commit (failing tests) → GREEN commit (implementation)

## Task Commits

1. **Task 1 RED: onboarding tests (services, controllers, approval)** - `022482f` (test)
2. **Task 1 GREEN: onboarding services and controllers implementation** - `bd4c41d` (feat)
3. **Task 2 RED: ReferralService tier tests** - `9111aa2` (test)
4. **Task 2 GREEN: ReferralService tier logic and V7 schema fix** - `0625539` (feat)

## Files Created/Modified

- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/service/OnboardingApplicationService.kt` — submit, list, getById with DTO mapping
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/service/OnboardingApprovalService.kt` — approve + reject + referral attribution
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/OnboardingController.kt` — POST /api/onboarding (public)
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/AdminOnboardingController.kt` — /api/v1/admin/onboarding with ASTRO_ADMIN RBAC
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/service/ReferralService.kt` — getTier + listReferrals + ReferralTier enum
- `backend/src/main/resources/db/migration/V7__fix_onboarding_state_varchar.sql` — CHAR(2)→VARCHAR(2) for state column
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/OrganizationRepository.kt` — added findByReferralCode()
- `backend/src/test/kotlin/com/astro/leadflow/shared/modules/onboarding/api/OnboardingControllerTest.kt` — public submit tests
- `backend/src/test/kotlin/com/astro/leadflow/shared/modules/onboarding/api/AdminOnboardingControllerTest.kt` — admin list/detail/approve/reject + RBAC annotation
- `backend/src/test/kotlin/com/astro/leadflow/shared/modules/onboarding/service/OnboardingApprovalServiceTest.kt` — approve/reject service tests
- `backend/src/test/kotlin/com/astro/leadflow/shared/modules/referrals/service/ReferralServiceTest.kt` — tier boundary tests

## Decisions Made

- `AdminOnboardingController` uses `@PreAuthorize("hasRole('ASTRO_ADMIN')")` at class level — all methods inherit this check without per-method annotations
- When the submitted `referralCode` does not resolve to an existing organization, approval silently skips ReferralRelationship creation (no error raised — graceful degradation)
- Referral code uniqueness is enforced at service level via a loop checking `findByReferralCode` before persisting; database UNIQUE constraint on `organizations.referral_code` provides a secondary safety net
- Tier thresholds use APPROVED-only count (pending referrals do not contribute to tier advancement)
- `OnboardingApprovalService` does not use `SecurityUtils` — admin operations act on the full dataset without org-scoping

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed CHAR(2) vs VARCHAR(2) schema validation failure**
- **Found during:** Final full test suite run (`LeadflowApplicationTests.contextLoads()` failed)
- **Issue:** V6 migration used `CHAR(2)` for `onboarding_applications.state` column; Hibernate schema-validation expects `VARCHAR(2)` for `@Column(length=2)` mapping — PostgreSQL maps `CHAR(2)` to `bpchar` type which does not match
- **Fix:** Added V7 Flyway migration: `ALTER TABLE onboarding_applications ALTER COLUMN state TYPE VARCHAR(2)`
- **Files modified:** `backend/src/main/resources/db/migration/V7__fix_onboarding_state_varchar.sql` (created)
- **Commit:** `0625539`

## TDD Gate Compliance

- RED gate (test commit) exists for Task 1: `022482f`
- GREEN gate (feat commit) exists for Task 1: `bd4c41d`
- RED gate (test commit) exists for Task 2: `9111aa2`
- GREEN gate (feat commit) exists for Task 2: `0625539`

## Known Stubs

None — this plan creates backend services and controllers. No UI components or data stubs.

## Threat Surface Scan

| Flag | File | Description |
|------|------|-------------|
| threat_flag: spam | OnboardingController.kt | Public POST /api/onboarding — no rate limiting on submit; mitigated by duplicate email/CNPJ check in OnboardingApplicationService |
| threat_flag: auth | AdminOnboardingController.kt | @PreAuthorize at class level covers all methods; method-level RBAC tests verify rejection for non-ASTRO_ADMIN roles |

## Self-Check

Files exist:
- FOUND: OnboardingApplicationService.kt
- FOUND: OnboardingApprovalService.kt
- FOUND: OnboardingController.kt
- FOUND: AdminOnboardingController.kt
- FOUND: ReferralService.kt
- FOUND: V7__fix_onboarding_state_varchar.sql
- FOUND: OnboardingControllerTest.kt
- FOUND: AdminOnboardingControllerTest.kt
- FOUND: OnboardingApprovalServiceTest.kt
- FOUND: ReferralServiceTest.kt

Commits exist:
- FOUND: 022482f (test RED)
- FOUND: bd4c41d (feat GREEN)
- FOUND: 9111aa2 (test RED)
- FOUND: 0625539 (feat GREEN)

Tests: `./gradlew test` → BUILD SUCCESSFUL (all tests pass including full suite with LeadflowApplicationTests)

## Self-Check: PASSED

---
*Phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings*
*Completed: 2026-04-24*
