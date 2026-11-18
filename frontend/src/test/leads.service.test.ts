import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { leadsService } from '@/services/leads.service';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('leadsService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends server-side advanced filters to leads endpoint', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 10,
      },
    });

    await leadsService.list({
      search: 'joao',
      origin: 'WebMotors',
      stage: 'WAITING_ATTENDANCE',
      assignedTo: '9fcd99cb-52e8-4db4-a77e-8ab0ddcf1841',
      dateFrom: '2026-04-01',
      dateTo: '2026-04-05',
      page: 0,
      size: 10,
    });

    expect(api.get).toHaveBeenCalledWith('/leads', {
      params: {
        search: 'joao',
        origin: 'WebMotors',
        stage: 'WAITING_ATTENDANCE',
        assignedTo: '9fcd99cb-52e8-4db4-a77e-8ab0ddcf1841',
        dateFrom: '2026-04-01',
        dateTo: '2026-04-05',
        page: 0,
        size: 10,
      },
    });
  });

  it('normalizes spring page payload into paginated result', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        content: [{ id: 'lead-1' }],
        totalElements: 5,
        totalPages: 1,
        number: 0,
        size: 10,
      },
    });

    const result = await leadsService.list({ page: 0, size: 10 });

    expect(result.content).toHaveLength(1);
    expect(result.totalElements).toBe(5);
    expect(result.totalPages).toBe(1);
    expect(result.page).toBe(0);
    expect(result.size).toBe(10);
  });

  it('calls lead activities endpoint for timeline', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: [] });

    await leadsService.activities('lead-123');

    expect(api.get).toHaveBeenCalledWith('/leads/lead-123/activities');
  });
});
