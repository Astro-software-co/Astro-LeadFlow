import api from '@/lib/api';
import type {
  ApproveOnboardingResponseDTO,
  ListOnboardingResponseDTO,
  OnboardingApplicationDTO,
} from '@/types/api/admin-onboarding';

export interface AdminOnboardingFilters {
  search?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const adminOnboardingService = {
  async list(filters: AdminOnboardingFilters = {}): Promise<ListOnboardingResponseDTO> {
    const params = new URLSearchParams();
    if (filters.search) params.set('search', filters.search);
    if (filters.status && filters.status !== 'ALL') params.set('status', filters.status);
    if (filters.page !== undefined) params.set('page', String(filters.page));
    if (filters.size !== undefined) params.set('size', String(filters.size));
    const response = await api.get<ListOnboardingResponseDTO>(
      `/admin/onboarding?${params.toString()}`,
    );
    return response.data;
  },

  async getById(id: string): Promise<OnboardingApplicationDTO> {
    const response = await api.get<OnboardingApplicationDTO>(`/admin/onboarding/${id}`);
    return response.data;
  },

  async approve(id: string): Promise<ApproveOnboardingResponseDTO> {
    const response = await api.post<ApproveOnboardingResponseDTO>(
      `/admin/onboarding/${id}/approve`,
    );
    return response.data;
  },

  async reject(id: string, reason: string): Promise<void> {
    await api.post(`/admin/onboarding/${id}/reject`, { reason });
  },
};
