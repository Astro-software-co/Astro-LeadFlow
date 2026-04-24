-- ── Fix: onboarding_applications.state column type CHAR(2) → VARCHAR(2) ──────
-- Hibernate schema-validation requires VARCHAR for @Column(length=2) mappings.
-- The V6 migration used CHAR(2) which PostgreSQL maps to bpchar, causing a
-- validation mismatch. This migration aligns the column type with the JPA entity.

ALTER TABLE onboarding_applications
    ALTER COLUMN state TYPE VARCHAR(2);
