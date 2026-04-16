import api from '@/lib/api';
import { BillingHistoryEntry } from '@/types/settings';

export const billingHistoryService = {
  async listHistory(): Promise<BillingHistoryEntry[]> {
    const response = await api.get<BillingHistoryEntry[]>('/saas/billing-history');
    return response.data;
  },
};
