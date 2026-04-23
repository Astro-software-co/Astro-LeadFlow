import type { DateRange } from 'react-day-picker';
import { mapKanbanColumnToPipelineStage } from '@/mappers/leads.mapper';
import type { LeadFilters } from '@/services/leads.service';
import type { KanbanColumn } from '@/types';
import { stageOptions } from './constants';
import type { BuildLeadsFiltersInput, LeadAssignedFilter, LeadOriginFilter, LeadStageFilter } from './types';

function toISODate(value?: Date): string | undefined {
  if (!value) {
    return undefined;
  }

  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatDateRange(range?: DateRange): string {
  if (!range?.from) {
    return 'Filtrar por periodo';
  }

  const from = range.from.toLocaleDateString('pt-BR');
  if (!range.to) {
    return `${from} - ...`;
  }

  return `${from} - ${range.to.toLocaleDateString('pt-BR')}`;
}

export function stageLabel(stage: KanbanColumn | null | undefined): string {
  if (!stage) {
    return 'Sem estagio';
  }

  return stageOptions.find((option) => option.value === stage)?.label ?? 'Aguardando atendimento';
}

export function hasActiveLeadFilters(
  search: string,
  origin: LeadOriginFilter,
  stage: LeadStageFilter,
  assignedTo: LeadAssignedFilter,
  dateRange?: DateRange,
): boolean {
  return Boolean(search.trim()) || origin !== 'all' || stage !== 'all' || assignedTo !== 'all' || Boolean(dateRange?.from);
}

export function buildLeadsFilters({
  search,
  origin,
  stage,
  assignedTo,
  dateRange,
  page,
  pageSize,
}: BuildLeadsFiltersInput): LeadFilters {
  const trimmed = search.trim();

  return {
    search: trimmed || undefined,
    origin: origin === 'all' ? undefined : origin,
    stage: stage === 'all' ? undefined : mapKanbanColumnToPipelineStage(stage),
    assignedTo: assignedTo === 'all' ? undefined : assignedTo,
    dateFrom: toISODate(dateRange?.from),
    dateTo: toISODate(dateRange?.to),
    page: page - 1,
    size: pageSize,
  };
}
