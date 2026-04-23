import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { saasService } from '@/services/saas.service';
import { SubscriptionDTO, UsageSummaryDTO, PlanDTO } from '@/types/api/saas';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

const mockRenewsAt = new Date(Date.now() + 25 * 86400000).toISOString();

const mockSubscription: SubscriptionDTO = {
  planId: 'plan-profissional-monthly',
  planName: 'Profissional',
  planDescription: 'Plano Profissional para operacoes com mais volume.',
  priceBrl: 299,
  billingCycle: 'MONTHLY',
  billingCycleLabel: 'Mensal',
  messageLimit: 3000,
  marketplaceLimit: 3,
  status: 'ACTIVE',
  renewsAt: mockRenewsAt,
};

const mockUsage: UsageSummaryDTO = {
  planName: 'Profissional',
  globalLimit: 3000,
  globalUsed: 720,
  globalRemaining: 2280,
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
    id: 'plan-basico-monthly',
    code: 'BASICO_MONTHLY',
    name: 'Basico',
    description: 'Plano Basico para equipes em crescimento.',
    priceBrl: 149,
    billingCycle: 'MONTHLY',
    billingCycleLabel: 'Mensal',
    messageLimit: 1000,
    marketplaceLimit: 1,
  },
  {
    id: 'plan-basico-semiannually',
    code: 'BASICO_SEMIANNUALLY',
    name: 'Basico',
    description: 'Plano Basico para equipes em crescimento.',
    priceBrl: 804.6,
    billingCycle: 'SEMIANNUALLY',
    billingCycleLabel: 'Semestral',
    messageLimit: 1000,
    marketplaceLimit: 1,
  },
  {
    id: 'plan-basico-annually',
    code: 'BASICO_ANNUALLY',
    name: 'Basico',
    description: 'Plano Basico para equipes em crescimento.',
    priceBrl: 1430.4,
    billingCycle: 'ANNUALLY',
    billingCycleLabel: 'Anual',
    messageLimit: 1000,
    marketplaceLimit: 1,
  },
  {
    id: 'plan-profissional-monthly',
    code: 'PROFISSIONAL_MONTHLY',
    name: 'Profissional',
    description: 'Plano Profissional para operacoes com mais volume.',
    priceBrl: 299,
    billingCycle: 'MONTHLY',
    billingCycleLabel: 'Mensal',
    messageLimit: 3000,
    marketplaceLimit: 3,
  },
  {
    id: 'plan-profissional-semiannually',
    code: 'PROFISSIONAL_SEMIANNUALLY',
    name: 'Profissional',
    description: 'Plano Profissional para operacoes com mais volume.',
    priceBrl: 1614.6,
    billingCycle: 'SEMIANNUALLY',
    billingCycleLabel: 'Semestral',
    messageLimit: 3000,
    marketplaceLimit: 3,
  },
  {
    id: 'plan-profissional-annually',
    code: 'PROFISSIONAL_ANNUALLY',
    name: 'Profissional',
    description: 'Plano Profissional para operacoes com mais volume.',
    priceBrl: 2870.4,
    billingCycle: 'ANNUALLY',
    billingCycleLabel: 'Anual',
    messageLimit: 3000,
    marketplaceLimit: 3,
  },
  {
    id: 'plan-enterprise-monthly',
    code: 'ENTERPRISE_MONTHLY',
    name: 'Enterprise',
    description: 'Plano Enterprise para estruturas com alta escala.',
    priceBrl: 599,
    billingCycle: 'MONTHLY',
    billingCycleLabel: 'Mensal',
    messageLimit: 10000,
    marketplaceLimit: 10,
  },
  {
    id: 'plan-enterprise-semiannually',
    code: 'ENTERPRISE_SEMIANNUALLY',
    name: 'Enterprise',
    description: 'Plano Enterprise para estruturas com alta escala.',
    priceBrl: 3234.6,
    billingCycle: 'SEMIANNUALLY',
    billingCycleLabel: 'Semestral',
    messageLimit: 10000,
    marketplaceLimit: 10,
  },
  {
    id: 'plan-enterprise-annually',
    code: 'ENTERPRISE_ANNUALLY',
    name: 'Enterprise',
    description: 'Plano Enterprise para estruturas com alta escala.',
    priceBrl: 5750.4,
    billingCycle: 'ANNUALLY',
    billingCycleLabel: 'Anual',
    messageLimit: 10000,
    marketplaceLimit: 10,
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
