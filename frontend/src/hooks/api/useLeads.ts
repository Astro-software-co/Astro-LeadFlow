import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { leadsService, LeadFilters } from '@/services/leads.service';
import { Lead } from '@/types';
import { mockLeads } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useLeads(filters?: LeadFilters) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  return useQuery({
    queryKey: [orgId, 'leads', filters],
    queryFn: () => {
      if (useMocks) {
        return {
          content: mockLeads,
          totalElements: mockLeads.length,
          totalPages: 1,
          page: 0,
          size: mockLeads.length,
        };
      }
      return leadsService.list(filters);
    },
    staleTime: 30_000,
  });
}

export function useCreateLead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Lead>) => leadsService.create(data),
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') }),
  });
}

export function useUpdateLead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Lead> }) => leadsService.update(id, data),
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') }),
  });
}

export function useUpdateLeadStage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, stage }: { id: string; stage: string }) => leadsService.updateStage(id, stage),
    onSuccess: () => {
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') });
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('kanban') });
    },
  });
}
