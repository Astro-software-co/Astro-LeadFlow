export type UserRole = 'ASTRO_ADMIN' | 'MANAGER' | 'SELLER';

export type AppPermission =
  | 'finance:view'
  | 'organizations:view'
  | 'organizations:approve'
  | 'users:manage_all'
  | 'team:view'
  | 'team:create_seller'
  | 'team:update_seller'
  | 'team:delete_seller'
  | 'billing:view'
  | 'plans:view'
  | 'plans:change'
  | 'settings:security_update'
  | 'leads:view'
  | 'leads:update'
  | 'leads:delete'
  | 'privacy:manage'
  | 'stock:view';

export const ROLE_PERMISSIONS: Record<UserRole, AppPermission[]> = {
  ASTRO_ADMIN: ['finance:view', 'organizations:view', 'organizations:approve', 'users:manage_all'],
  MANAGER: [
    'team:view',
    'team:create_seller',
    'team:update_seller',
    'team:delete_seller',
    'billing:view',
    'plans:view',
    'plans:change',
    'settings:security_update',
    'leads:view',
    'leads:update',
    'leads:delete',
    'privacy:manage',
    'stock:view',
  ],
  SELLER: ['settings:security_update', 'leads:view', 'leads:update', 'stock:view'],
};

export function isUserRole(value: unknown): value is UserRole {
  return value === 'ASTRO_ADMIN' || value === 'MANAGER' || value === 'SELLER';
}

export function hasPermission(role: UserRole | string | null | undefined, permission: AppPermission): boolean {
  if (!isUserRole(role)) {
    return false;
  }

  return ROLE_PERMISSIONS[role].includes(permission);
}
