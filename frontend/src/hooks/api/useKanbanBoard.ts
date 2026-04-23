import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { mockLeads } from '@/data/mockData';
import { mapLeadToKanbanCard } from '@/mappers/leads.mapper';
import { kanbanService } from '@/services/kanban.service';
import type { KanbanCard } from '@/types';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useKanbanBoard() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'kanban', 'board'],
    staleTime: 30_000,
    queryFn: async (): Promise<KanbanCard[]> => {
      if (useMocks) {
        return mockLeads.map(mapLeadToKanbanCard);
      }

      return kanbanService.getBoard();
    },
  });
}
