import api from '@/lib/api';
import { BillingHistoryCollectionResponseDTO, BillingHistoryResponseDTO } from '@/types/settings';

type BillingHistoryDataDTO = BillingHistoryResponseDTO[] | BillingHistoryCollectionResponseDTO;

function normalizeBillingHistory(data: BillingHistoryDataDTO): BillingHistoryResponseDTO[] {
  if (Array.isArray(data)) {
    return data;
  }

  return data.content ?? data.data ?? data.items ?? [];
}

export const billingHistoryService = {
  async listHistory(): Promise<BillingHistoryResponseDTO[]> {
    const response = await api.get<BillingHistoryDataDTO>('/saas/billing-history');
    return normalizeBillingHistory(response.data);
  },
};
