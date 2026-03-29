import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { leadsService, LeadFilters } from '@/services/leads.service';
import { Lead } from '@/types';
import { mockLeads } from '@/data/mockData';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useLeads(filters?: LeadFilters) {
  return useQuery({
    queryKey: ['leads', filters],
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
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['leads'] }),
  });
}

export function useUpdateLead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Lead> }) => leadsService.update(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['leads'] }),
  });
}

export function useUpdateLeadStage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, stage }: { id: string; stage: string }) => leadsService.updateStage(id, stage),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leads'] });
      queryClient.invalidateQueries({ queryKey: ['kanban'] });
    },
  });
}
