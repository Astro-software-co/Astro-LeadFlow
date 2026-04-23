import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { billingHistoryService } from '@/services/billing-history.service';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('billingHistoryService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns array payload as-is', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: [
        { id: 'bill-1', amountBrl: 299 },
      ],
    });

    const result = await billingHistoryService.listHistory();

    expect(api.get).toHaveBeenCalledWith('/saas/billing-history');
    expect(result).toHaveLength(1);
    expect(result[0].id).toBe('bill-1');
  });

  it('normalizes content/data/items collections and empty fallback', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: { content: [{ id: 'bill-content' }] } })
      .mockResolvedValueOnce({ data: { data: [{ id: 'bill-data' }] } })
      .mockResolvedValueOnce({ data: { items: [{ id: 'bill-items' }] } })
      .mockResolvedValueOnce({ data: {} });

    const fromContent = await billingHistoryService.listHistory();
    const fromData = await billingHistoryService.listHistory();
    const fromItems = await billingHistoryService.listHistory();
    const emptyFallback = await billingHistoryService.listHistory();

    expect(fromContent[0].id).toBe('bill-content');
    expect(fromData[0].id).toBe('bill-data');
    expect(fromItems[0].id).toBe('bill-items');
    expect(emptyFallback).toEqual([]);
  });
});
