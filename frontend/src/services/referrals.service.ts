import api from '@/lib/api';
import type {
  ReferralBalanceDTO,
  ReferralSummaryDTO,
  ReferredCompanyDTO,
} from '@/types/api/referrals';

export const referralsService = {
  async getMyCode(): Promise<ReferralSummaryDTO> {
    const response = await api.get<ReferralSummaryDTO>('/referral/my-code');
    return response.data;
  },

  async getMyReferrals(): Promise<ReferredCompanyDTO[]> {
    const response = await api.get<ReferredCompanyDTO[]>('/referral/my-referrals');
    return response.data;
  },

  async getMyBalance(): Promise<ReferralBalanceDTO> {
    const response = await api.get<ReferralBalanceDTO>('/referral/my-balance');
    return response.data;
  },
};
