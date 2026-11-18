import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { usersService } from '@/services/users.service';
import type { UserSummaryResponseDTO } from '@/types/api/users';
import type { SettingsLoadState, SettingsTeamMember } from '@/types/settings';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

function mapTeamMember(dto: UserSummaryResponseDTO): SettingsTeamMember {
  return {
    id: dto.id,
    name: dto.name,
    email: dto.email,
    avatarUrl: dto.avatarUrl ?? null,
    role: dto.role,
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

export interface UseTeamResult {
  team: SettingsTeamMember[];
  state: SettingsLoadState;
  isLoading: boolean;
  isError: boolean;
  isReady: boolean;
  errorMessage: string | null;
  refetch: () => Promise<unknown>;
}

export function useTeam(): UseTeamResult {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  const query = useQuery({
    queryKey: [orgId, 'settings', 'team'],
    staleTime: 30_000,
    queryFn: async (): Promise<UserSummaryResponseDTO[]> => {
      if (useMocks) {
        return [];
      }

      return usersService.listTeam();
    },
  });

  const state = resolveState(query.isPending, query.isError);

  return {
    team: (query.data ?? []).map(mapTeamMember),
    state,
    isLoading: state === 'loading',
    isError: state === 'error',
    isReady: state === 'ready',
    errorMessage: query.error instanceof Error ? query.error.message : query.isError ? 'Nao foi possivel carregar a equipe.' : null,
    refetch: query.refetch,
  };
}
