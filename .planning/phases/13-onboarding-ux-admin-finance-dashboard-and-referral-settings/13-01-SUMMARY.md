---
phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings
plan: "01"
subsystem: database, api, auth
tags: [kotlin, spring-boot, flyway, jpa, rbac, onboarding, referrals, security]

requires:
  - phase: 12-plano-de-acao
    provides: Existing IAM domain (Role, User, Organization), SecurityConfig, PackageBoundaryTest

provides:
  - V6 Flyway migration adding onboarding_applications and referral_relationships tables plus organizations.referral_code column
  - ASTRO_ADMIN role with ROLE_ASTRO_ADMIN authority
  - ROLE_DEALERSHIP composite authority for ADMIN/MANAGER/SELLER roles
  - Public POST /api/onboarding endpoint permission in SecurityConfig
  - /api/v1/admin/** protected by ASTRO_ADMIN role
  - Dealership API families protected by ROLE_DEALERSHIP
  - OnboardingApplication JPA entity and repository
  - OnboardingStatus enum (PENDING/APPROVED/REJECTED)
  - SubmitOnboardingRequest with optional referralCode field
  - RejectOnboardingRequest with validation
  - OnboardingApplicationSummaryResponse and OnboardingApplicationDetailResponse
  - ApproveOnboardingResponse returning generated referralCode
  - ReferralRelationship JPA entity and repository
  - ReferralStatus enum (PENDING/APPROVED/REJECTED)

affects:
  - 13-onboarding-ux-admin-finance-dashboard-and-referral-settings (all subsequent plans in this phase)

tech-stack:
  added: []
  patterns:
    - "TDD: RED (failing test) -> GREEN (implementation) -> verify cycle per task"
    - "Composite Spring authority: ROLE_DEALERSHIP granted to ADMIN/MANAGER/SELLER in User.getAuthorities()"
    - "Separate request/response DTO files under api/requests and api/responses (no inline nested classes)"
    - "Sequential uppercase Flyway migration naming: V6__..."

key-files:
  created:
    - backend/src/main/resources/db/migration/V6__onboarding_admin_referrals.sql
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/domain/OnboardingApplication.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/domain/OnboardingStatus.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/domain/OnboardingApplicationRepository.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/requests/SubmitOnboardingRequest.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/requests/RejectOnboardingRequest.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/responses/OnboardingApplicationSummaryResponse.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/responses/OnboardingApplicationDetailResponse.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/responses/ApproveOnboardingResponse.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/domain/ReferralRelationship.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/domain/ReferralStatus.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/domain/ReferralRelationshipRepository.kt
  modified:
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/Role.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/User.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/Organization.kt
    - backend/src/main/kotlin/com/astro/leadflow/shared/config/SecurityConfig.kt

key-decisions:
  - "ASTRO_ADMIN is added to Role enum and returns only ROLE_ASTRO_ADMIN (not ROLE_DEALERSHIP)"
  - "ADMIN/MANAGER/SELLER emit ROLE_DEALERSHIP as composite authority while keeping their individual ROLE_<name>"
  - "POST /api/onboarding is permitted at root /api (not /api/v1) to match backend contract from research"
  - "/api/v1/admin/** is now protected by ROLE_ASTRO_ADMIN (changed from ROLE_SUPER_ADMIN)"
  - "referral_relationships.referred_organization_id has UNIQUE constraint to prevent duplicate referral attribution"
  - "OnboardingApplication stores referralCode as the referrer's code submitted at intake (not the approved org's generated code)"

requirements-completed:
  - P13-BACKEND-CONVENTIONS
  - P13-PUBLIC-ONBOARDING-POST
  - P13-RBAC-ASTRO-ADMIN
  - P13-RBAC-DEALERSHIP

duration: 35min
completed: 2026-04-24
---

# Phase 13 Plan 01: Backend Schema, RBAC, and Onboarding Domain Summary

**Flyway V6 migration with onboarding/referral tables, ASTRO_ADMIN+DEALERSHIP RBAC in Spring Security, and complete onboarding domain model with separate request/response DTO files**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-04-24T17:00:00Z
- **Completed:** 2026-04-24T17:35:00Z
- **Tasks:** 3
- **Files modified:** 15 (4 modified, 11 created + 3 test files)

## Accomplishments

- Created Flyway V6 migration adding `onboarding_applications`, `referral_relationships` tables, and `organizations.referral_code` column with all required indexes
- Added `ASTRO_ADMIN` to Role enum; updated `User.getAuthorities()` to emit `ROLE_DEALERSHIP` for all dealership roles and `ROLE_ASTRO_ADMIN` for the new role
- Updated SecurityConfig to permit `POST /api/onboarding` publicly, protect `/api/v1/admin/**` with `ASTRO_ADMIN`, and protect dealership API families with `DEALERSHIP`
- Created full onboarding module: entity, status enum, repository, and separate request/response DTO files (5 files)
- Created referrals module domain: `ReferralRelationship` entity, `ReferralStatus` enum, `ReferralRelationshipRepository`
- All tasks followed TDD: RED commit (failing test) followed by GREEN commit (implementation), verified by `PackageBoundaryTest`, `RbacAuthorityTest`, and `OnboardingDomainTest`

## Task Commits

1. **Task 1: V6 schema for onboarding and referrals** - `e13b17f` (feat)
2. **Task 2: ASTRO_ADMIN role, DEALERSHIP authority — RED tests** - `0cad1e0` (test)
3. **Task 2: ASTRO_ADMIN role, DEALERSHIP authority — GREEN impl** - `bd5a0f5` (feat)
4. **Task 3: Onboarding domain model — RED tests** - `aa2a31f` (test)
5. **Task 3: Onboarding domain model and referral domain — GREEN impl** - `c0ce2dc` (feat)

## Files Created/Modified

- `backend/src/main/resources/db/migration/V6__onboarding_admin_referrals.sql` - Adds onboarding_applications, referral_relationships, organizations.referral_code
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/Role.kt` - Added ASTRO_ADMIN enum value
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/User.kt` - Composite authority logic in getAuthorities()
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/iam/domain/Organization.kt` - Added referralCode field
- `backend/src/main/kotlin/com/astro/leadflow/shared/config/SecurityConfig.kt` - Public onboarding, ASTRO_ADMIN admin routes, DEALERSHIP dealership routes
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/domain/OnboardingApplication.kt` - JPA entity
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/domain/OnboardingStatus.kt` - Enum PENDING/APPROVED/REJECTED
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/domain/OnboardingApplicationRepository.kt` - JPA repo with search query
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/requests/SubmitOnboardingRequest.kt` - With optional referralCode
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/requests/RejectOnboardingRequest.kt` - With validation
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/responses/OnboardingApplicationSummaryResponse.kt` - List view DTO
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/responses/OnboardingApplicationDetailResponse.kt` - Detail view DTO
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/onboarding/api/responses/ApproveOnboardingResponse.kt` - Approve response with referralCode
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/domain/ReferralRelationship.kt` - JPA entity
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/domain/ReferralStatus.kt` - Enum PENDING/APPROVED/REJECTED
- `backend/src/main/kotlin/com/astro/leadflow/shared/modules/referrals/domain/ReferralRelationshipRepository.kt` - JPA repo with referrer/status queries

## Decisions Made

- `ASTRO_ADMIN` returns only `ROLE_ASTRO_ADMIN` (no `ROLE_DEALERSHIP`) to prevent Astro staff from accessing dealership-scoped data via broad authority
- `/api/v1/admin/**` was previously protected by `ROLE_SUPER_ADMIN`; migrated to `ROLE_ASTRO_ADMIN` as specified by Phase 13 requirements
- The public onboarding endpoint uses path `/api/onboarding` (root `/api`, not `/api/v1`) matching the backend contract established in 13-RESEARCH.md
- `referral_relationships.referred_organization_id` carries a UNIQUE constraint so an org cannot be attributed to multiple referrers
- `OnboardingApplicationRepository.search()` uses a JPQL query supporting nullable `status` and `query` parameters for flexible admin filtering

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## Known Stubs

None - this plan creates database schema and domain objects. No UI stubs.

## Threat Surface Scan

| Flag | File | Description |
|------|------|-------------|
| threat_flag: auth | SecurityConfig.kt | New public endpoint POST /api/onboarding — no auth required, mitigated by Bean validation on SubmitOnboardingRequest |
| threat_flag: auth | SecurityConfig.kt | /api/v1/admin/** now protected by ROLE_ASTRO_ADMIN (changed from ROLE_SUPER_ADMIN) — ASTRO_ADMIN users can access all admin routes |

## Next Phase Readiness

- Backend domain and RBAC foundation is complete for Plan 13-01B (controllers, services, approve/reject flow)
- `OnboardingApplicationRepository`, `ReferralRelationshipRepository`, and all DTO contracts are ready for service layer consumption
- `Organization.referralCode` field and V6 migration are ready for the approval service to generate and persist referral codes
- `SecurityConfig` permits `POST /api/onboarding` — public onboarding controller can be wired in next plan

## Self-Check

Files exist: ✅ all 16 source files confirmed present
Commits exist: ✅ e13b17f, 0cad1e0, bd5a0f5, aa2a31f, c0ce2dc all in git log
Tests pass: ✅ `PackageBoundaryTest`, `RbacAuthorityTest`, `OnboardingDomainTest` all GREEN

## Self-Check: PASSED

---
*Phase: 13-onboarding-ux-admin-finance-dashboard-and-referral-settings*
*Completed: 2026-04-24*
