import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { saasService } from '@/services/saas.service';
import { SubscriptionDTO, UsageSummaryDTO, PlanDTO } from '@/types/api/saas';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

const mockRenewsAt = new Date(Date.now() + 25 * 86400000).toISOString();

const mockSubscription: SubscriptionDTO = {
  planId: 'plan-profissional',
  planName: 'Profissional',
  monthlyPriceBrl: 299,
  messageLimit: 1000,
  marketplaceLimit: 3,
  status: 'ACTIVE',
  renewsAt: mockRenewsAt,
};

const mockUsage: UsageSummaryDTO = {
  planName: 'Profissional',
  globalLimit: 1000,
  globalUsed: 720,
  globalRemaining: 280,
  renewsAt: mockRenewsAt,
  marketplaces: [
    {
      marketplaceName: 'Webmotors',
      enabled: true,
      used: 450,
      limit: 500,
      remaining: 50,
    },
    {
      marketplaceName: 'Mercado Livre',
      enabled: true,
      used: 270,
      limit: 500,
      remaining: 230,
    },
  ],
};

const mockPlans: PlanDTO[] = [
  {
    id: 'plan-starter',
    name: 'Starter',
    monthlyPriceBrl: 99,
    messageLimit: 200,
    marketplaceLimit: 1,
  },
  {
    id: 'plan-profissional',
    name: 'Profissional',
    monthlyPriceBrl: 299,
    messageLimit: 1000,
    marketplaceLimit: 3,
  },
  {
    id: 'plan-empresarial',
    name: 'Empresarial',
    monthlyPriceBrl: 599,
    messageLimit: 5000,
    marketplaceLimit: 5,
  },
  {
    id: 'plan-enterprise',
    name: 'Enterprise',
    monthlyPriceBrl: 0,
    messageLimit: 99999,
    marketplaceLimit: 99,
  },
];

export function useSubscription() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'subscription'],
    staleTime: 30_000,
    queryFn: async (): Promise<SubscriptionDTO> => {
      if (useMocks) {
        return mockSubscription;
      }
      return saasService.getSubscription();
    },
  });
}

export function useUsage() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'saas-usage'],
    staleTime: 30_000,
    queryFn: async (): Promise<UsageSummaryDTO> => {
      if (useMocks) {
        return mockUsage;
      }
      return saasService.getUsage();
    },
  });
}

export function usePlans() {
  return useQuery({
    queryKey: ['plans'],
    staleTime: 300_000,
    queryFn: async (): Promise<PlanDTO[]> => {
      if (useMocks) {
        return mockPlans;
      }
      return saasService.getPlans();
    },
  });
}
