import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { referralsService } from '@/services/referrals.service';
import type { ReferralSettingsDTO } from '@/types/api/referrals';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export const mockReferralSettings: ReferralSettingsDTO = {
  code: 'ASTRO-8F4A',
  referralLink: 'https://app.astroleadflow.com/onboarding?ref=ASTRO-8F4A',
  approvedCount: 8,
  pendingCount: 2,
  currentTier: 'BRONZE',
  commissionRate: 0.08,
  nextTierThreshold: 10,
  balanceBrl: 478.4,
  pendingBrl: 159.2,
  withdrawalAvailable: false,
  referrals: [
    {
      organizationId: 'org-ref-1',
      companyName: 'Rota Sul Veiculos',
      status: 'APPROVED',
      planCode: 'PROFISSIONAL_MONTHLY',
      approvedAt: '2026-04-12T00:00:00.000Z',
      commissionBrl: 23.92,
    },
    {
      organizationId: 'org-ref-2',
      companyName: 'Auto Prime Campinas',
      status: 'PENDING',
      planCode: 'BASICO_MONTHLY',
      approvedAt: null,
      commissionBrl: null,
    },
  ],
};

export function useReferrals() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'settings', 'referrals'],
    staleTime: 30_000,
    queryFn: async (): Promise<ReferralSettingsDTO> => {
      if (useMocks) {
        return mockReferralSettings;
      }

      const [summary, referrals, balance] = await Promise.all([
        referralsService.getMyCode(),
        referralsService.getMyReferrals(),
        referralsService.getMyBalance(),
      ]);

      return {
        ...summary,
        referrals,
        balanceBrl: balance.balanceBrl,
        pendingBrl: balance.pendingBrl,
        withdrawalAvailable: balance.withdrawalAvailable,
      };
    },
  });
}
