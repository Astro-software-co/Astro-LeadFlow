import api from '@/lib/api';
import {
  CreateSubscriptionCheckoutDTO,
  SubscriptionCheckoutDTO,
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

  async createSubscriptionCheckout(
    planId: string,
    payload: CreateSubscriptionCheckoutDTO,
  ): Promise<SubscriptionCheckoutDTO> {
    const response = await api.post<SubscriptionCheckoutDTO>(`/saas/subscription/checkout/${planId}`, payload);
    return response.data;
  },

  async cancelSubscription(): Promise<SubscriptionDTO> {
    const response = await api.post<SubscriptionDTO>('/saas/subscription/cancel');
    return response.data;
  },
};
