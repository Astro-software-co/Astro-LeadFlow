import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { leadsService, LeadFilters } from '@/services/leads.service';
import { KanbanColumn, Lead } from '@/types';
import { mockLeads } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';
import {
  mapKanbanColumnToPipelineStage,
  mapLeadResponseToLead,
  mapLeadToCreateRequest,
  mapLeadToUpdateRequest,
} from '@/mappers/leads.mapper';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

interface LeadsQueryResult {
  content: Lead[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export function useLeads(filters?: LeadFilters) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'leads', filters],
    queryFn: async (): Promise<LeadsQueryResult> => {
      if (useMocks) {
        return {
          content: mockLeads,
          totalElements: mockLeads.length,
          totalPages: 1,
          page: 0,
          size: mockLeads.length,
        };
      }

      const leads = await leadsService.list(filters);
      const mapped = leads.map(mapLeadResponseToLead);

      return {
        content: mapped,
        totalElements: mapped.length,
        totalPages: 1,
        page: 0,
        size: mapped.length,
      };
    },
    staleTime: 30_000,
  });
}

export function useCreateLead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: Partial<Lead>) => {
      const created = await leadsService.create(mapLeadToCreateRequest(data));
      return mapLeadResponseToLead(created);
    },
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') }),
  });
}

export function useUpdateLead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<Lead> }) => {
      const updated = await leadsService.update(id, mapLeadToUpdateRequest(data));
      return mapLeadResponseToLead(updated);
    },
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') }),
  });
}

export function useUpdateLeadStage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, stage }: { id: string; stage: KanbanColumn }) => {
      const updated = await leadsService.updateStage(id, mapKanbanColumnToPipelineStage(stage));
      return mapLeadResponseToLead(updated);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') });
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('kanban') });
    },
  });
}

export function useArchiveLead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => leadsService.archive(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') });
      queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('kanban') });
    },
  });
}
