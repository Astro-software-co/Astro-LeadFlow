import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { usersService } from '@/services/users.service';
import type { AssignedUserOption } from '@/features/leads/types';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useAssignableUsers() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'users', 'assignable'],
    enabled: Boolean(user),
    staleTime: 60_000,
    queryFn: async (): Promise<AssignedUserOption[]> => {
      if (useMocks) {
        return [];
      }

      const assignable = await usersService.listAssignable();
      return assignable
        .map((candidate) => ({
          id: candidate.id,
          name: candidate.name,
        }))
        .sort((left, right) => left.name.localeCompare(right.name));
    },
  });
}
