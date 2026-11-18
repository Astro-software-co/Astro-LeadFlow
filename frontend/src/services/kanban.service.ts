import api from '@/lib/api';
import { mapLeadResponseToKanbanCard } from '@/mappers/leads.mapper';
import type { KanbanCard } from '@/types';
import type { LeadResponseDTO } from '@/types/api/leads';
import type { KanbanBoardResponseDTO } from '@/types/api/kanban';

const orderedStages = [
  'TALKING_TO_BOT',
  'WAITING_ATTENDANCE',
  'IN_ATTENDANCE',
  'IN_NEGOTIATION',
  'COMPLETED',
  'ARCHIVED',
] as const;

function isLeadArray(value: unknown): value is LeadResponseDTO[] {
  return Array.isArray(value);
}

function normalizeBoardResponse(payload: KanbanBoardResponseDTO): LeadResponseDTO[] {
  const knownLeads = orderedStages.flatMap((stage) => {
    const leads = payload.stages[stage];
    return isLeadArray(leads) ? leads : [];
  });

  const unknownLeads = Object.entries(payload.stages)
    .filter(([stage]) => !orderedStages.includes(stage as (typeof orderedStages)[number]))
    .flatMap(([, leads]) => (isLeadArray(leads) ? leads : []));

  return [...knownLeads, ...unknownLeads];
}

export const kanbanService = {
  async getBoard(): Promise<KanbanCard[]> {
    const response = await api.get<KanbanBoardResponseDTO>('/kanban/board');
    const leads = normalizeBoardResponse(response.data);
    return leads.map(mapLeadResponseToKanbanCard);
  },
};
