-- Ensure the Astro platform administrator is not scoped to any dealership organization.

UPDATE users
SET email = 'admin@astro.dev'
WHERE LOWER(email) = 'astro.admin@astro.dev'
  AND NOT EXISTS (
      SELECT 1
      FROM users existing_admin
      WHERE LOWER(existing_admin.email) = 'admin@astro.dev'
  );

UPDATE users
SET role = 'ASTRO_ADMIN',
    organization_id = NULL,
    status = 'ACTIVE'
WHERE LOWER(email) = 'admin@astro.dev';

UPDATE users
SET organization_id = NULL
WHERE role = 'ASTRO_ADMIN';
