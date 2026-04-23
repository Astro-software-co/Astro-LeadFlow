import type { DateRange } from 'react-day-picker';
import type { KanbanColumn, Lead } from '@/types';

export type LeadStageFilter = 'all' | KanbanColumn;
export type LeadOriginFilter = 'all' | Lead['origem'];
export type LeadAssignedFilter = 'all' | string;

export interface StageOption {
  value: LeadStageFilter;
  label: string;
}

export interface OriginOption {
  value: LeadOriginFilter;
  label: string;
}

export interface AssignedUserOption {
  id: string;
  name: string;
}

export interface BuildLeadsFiltersInput {
  search: string;
  origin: LeadOriginFilter;
  stage: LeadStageFilter;
  assignedTo: LeadAssignedFilter;
  dateRange?: DateRange;
  page: number;
  pageSize: number;
}
