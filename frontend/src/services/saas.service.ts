import api from '@/lib/api';
import {
  SubscriptionDTO,
  UsageSummaryDTO,
  PlanDTO,
  MarketplaceConfigDTO,
} from '@/types/api/saas';

export const saasService = {
  async getSubscription(): Promise<SubscriptionDTO> {
    const response = await api.get<SubscriptionDTO>('/saas/subscription');
    return response.data;
  },

  async getUsage(): Promise<UsageSummaryDTO> {
    const response = await api.get<UsageSummaryDTO>('/saas/usage');
    return response.data;
  },

  async getPlans(): Promise<PlanDTO[]> {
    const response = await api.get<PlanDTO[]>('/plans');
    return response.data;
  },

  async getMarketplaces(): Promise<MarketplaceConfigDTO[]> {
    const response = await api.get<MarketplaceConfigDTO[]>('/saas/marketplaces');
    return response.data;
  },
};
