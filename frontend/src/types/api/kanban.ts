import type { LeadResponseDTO } from '@/types/api/leads';

export interface KanbanBoardResponseDTO {
  stages: Record<string, LeadResponseDTO[] | undefined>;
}
