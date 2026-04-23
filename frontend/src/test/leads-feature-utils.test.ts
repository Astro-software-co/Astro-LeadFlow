import { describe, expect, it } from 'vitest';
import { buildLeadsFilters, formatDateRange, hasActiveLeadFilters, stageLabel } from '@/features/leads/utils';

describe('leads feature utils', () => {
  it('builds query filters with serialized values', () => {
    const filters = buildLeadsFilters({
      search: '  ana  ',
      origin: 'WebMotors',
      stage: 'negociacao',
      assignedTo: 'seller-1',
      dateRange: {
        from: new Date(2026, 0, 5),
        to: new Date(2026, 0, 10),
      },
      page: 2,
      pageSize: 10,
    });

    expect(filters).toEqual({
      search: 'ana',
      origin: 'WebMotors',
      stage: 'IN_NEGOTIATION',
      assignedTo: 'seller-1',
      dateFrom: '2026-01-05',
      dateTo: '2026-01-10',
      page: 1,
      size: 10,
    });
  });

  it('returns readable date range labels', () => {
    expect(formatDateRange()).toBe('Filtrar por periodo');
    expect(formatDateRange({ from: new Date(2026, 0, 5), to: undefined })).toBe('05/01/2026 - ...');
    expect(formatDateRange({ from: new Date(2026, 0, 5), to: new Date(2026, 0, 10) })).toBe('05/01/2026 - 10/01/2026');
  });

  it('detects active filters and stage labels', () => {
    expect(hasActiveLeadFilters('', 'all', 'all', 'all')).toBe(false);
    expect(hasActiveLeadFilters('ana', 'all', 'all', 'all')).toBe(true);
    expect(stageLabel('negociacao')).toBe('Em negociacao');
    expect(stageLabel(undefined)).toBe('Sem estagio');
  });
});
