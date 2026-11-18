import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { leadsService, LeadFilters } from '@/services/leads.service';
import { KanbanColumn, Lead } from '@/types';
import { mockLeads } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';
import {
  mapKanbanColumnToPipelineStage,
  mapPipelineStageToKanbanColumn,
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

export interface LeadActivity {
  fromStage?: KanbanColumn | null;
  toStage: KanbanColumn;
  movedByName?: string;
  movedAt: Date;
}

export function useLeads(filters?: LeadFilters) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  const page = Math.max(0, filters?.page ?? 0);
  const size = Math.max(1, filters?.size ?? 10);

  return useQuery({
    queryKey: [orgId, 'leads', filters],
    queryFn: async (): Promise<LeadsQueryResult> => {
      if (useMocks) {
        const normalizedSearch = filters?.search?.trim().toLowerCase();
        const startDate = filters?.dateFrom ? new Date(`${filters.dateFrom}T00:00:00`) : null;
        const endDate = filters?.dateTo ? new Date(`${filters.dateTo}T23:59:59.999`) : null;
        const filtered = mockLeads.filter((lead) => {
          const matchesSearch =
            !normalizedSearch ||
            lead.nome.toLowerCase().includes(normalizedSearch) ||
            lead.telefone.toLowerCase().includes(normalizedSearch) ||
            (lead.email?.toLowerCase().includes(normalizedSearch) ?? false);
          const matchesOrigin = !filters?.origin || lead.origem === filters.origin;
          const matchesStage =
            !filters?.stage || (lead.estagio ? mapKanbanColumnToPipelineStage(lead.estagio) === filters.stage : false);
          const matchesAssignedTo = !filters?.assignedTo || lead.assignedToId === filters.assignedTo;
          const matchesFrom = !startDate || lead.dataCriacao >= startDate;
          const matchesTo = !endDate || lead.dataCriacao <= endDate;
          return matchesSearch && matchesOrigin && matchesStage && matchesAssignedTo && matchesFrom && matchesTo;
        });

        const start = page * size;
        const content = filtered.slice(start, start + size);
        return {
          content,
          totalElements: filtered.length,
          totalPages: Math.max(1, Math.ceil(filtered.length / size)),
          page,
          size,
        };
      }

      const response = await leadsService.list(filters);
      const mapped = response.content.map(mapLeadResponseToLead);

      return {
        content: mapped,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        page: response.page,
        size: response.size,
      };
    },
    staleTime: 30_000,
    placeholderData: (previous) => previous,
  });
}

export function useLeadActivities(leadId?: string) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'lead-activities', leadId],
    enabled: Boolean(leadId),
    queryFn: async (): Promise<LeadActivity[]> => {
      if (!leadId) {
        return [];
      }

      if (useMocks) {
        const lead = mockLeads.find((candidate) => candidate.id === leadId);
        if (!lead) {
          return [];
        }

        return [
          {
            fromStage: null,
            toStage: lead.estagio ?? 'aguardando',
            movedByName: lead.assignedToName,
            movedAt: lead.dataCriacao,
          },
        ];
      }

      const activities = await leadsService.activities(leadId);
      return activities.map((activity) => ({
        fromStage: activity.fromStage ? mapPipelineStageToKanbanColumn(activity.fromStage) : null,
        toStage: mapPipelineStageToKanbanColumn(activity.toStage),
        movedByName: activity.movedBy?.name ?? undefined,
        movedAt: new Date(activity.movedAt),
      }));
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
