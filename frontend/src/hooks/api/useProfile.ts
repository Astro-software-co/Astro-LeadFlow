import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { usersService } from '@/services/users.service';
import type { UserProfileResponseDTO } from '@/types/api/users';
import type { SettingsLoadState, SettingsProfile } from '@/types/settings';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

function profileFromAuth(
  user: ReturnType<typeof useAuth>['user'],
  fallbackOrgId: string,
): UserProfileResponseDTO {
  return {
    id: user?.id ?? 'mock-user-1',
    name: user?.name ?? 'Usuario Demo',
    email: user?.email ?? 'demo@astro.com',
    phoneNumber: null,
    avatarUrl: null,
    role: (user?.role as UserProfileResponseDTO['role']) ?? 'ADMIN',
    status: 'ACTIVE',
    organizationId: user?.organizationId ?? fallbackOrgId,
    organizationName: null,
    twoFactorEmailEnabled: false,
    twoFactorSmsEnabled: false,
    notifyNewLead: true,
    notifyLeadWaiting: true,
    notifySaleCompleted: true,
    notifyCardMoved: false,
    notifyViaEmail: false,
    notifyViaPush: true,
    createdAt: new Date().toISOString(),
  };
}

function mapProfile(dto: UserProfileResponseDTO, fallbackOrgId: string): SettingsProfile {
  return {
    id: dto.id,
    name: dto.name,
    email: dto.email,
    phoneNumber: dto.phoneNumber ?? null,
    avatarUrl: dto.avatarUrl ?? null,
    role: dto.role,
    organizationId: dto.organizationId ?? fallbackOrgId,
    organizationName: dto.organizationName ?? null,
  };
}

function resolveState(isPending: boolean, isError: boolean): SettingsLoadState {
  if (isPending) {
    return 'loading';
  }

  if (isError) {
    return 'error';
  }

  return 'ready';
}

export interface UseProfileResult {
  profile: SettingsProfile | null;
  state: SettingsLoadState;
  isLoading: boolean;
  isError: boolean;
  isReady: boolean;
  errorMessage: string | null;
  refetch: () => Promise<unknown>;
}

export function useProfile(): UseProfileResult {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  const query = useQuery({
    queryKey: [orgId, 'settings', 'profile'],
    staleTime: 30_000,
    queryFn: async (): Promise<UserProfileResponseDTO> => {
      if (useMocks) {
        return profileFromAuth(user, orgId);
      }

      return usersService.getProfile();
    },
  });

  const state = resolveState(query.isPending, query.isError);
  const profile = query.data ? mapProfile(query.data, orgId) : null;

  return {
    profile,
    state,
    isLoading: state === 'loading',
    isError: state === 'error',
    isReady: state === 'ready',
    errorMessage: query.error instanceof Error ? query.error.message : query.isError ? 'Nao foi possivel carregar o perfil.' : null,
    refetch: query.refetch,
  };
}
