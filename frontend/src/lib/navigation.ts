import { isUserRole } from '@/lib/rbac';

export const DEALERSHIP_HOME_PATH = '/app/dashboard';
export const ASTRO_ADMIN_HOME_PATH = '/admin/onboarding';

export function getHomePathForRole(role: unknown): string {
  return isUserRole(role) && role === 'ASTRO_ADMIN'
    ? ASTRO_ADMIN_HOME_PATH
    : DEALERSHIP_HOME_PATH;
}
