import { useQuery } from '@tanstack/react-query';
import { adminDashboardService } from '@/services/admin-dashboard.service';
import type { AdminDashboardDTO } from '@/types/api/admin-dashboard';

export const mockAdminDashboardData: AdminDashboardDTO = {
  metrics: {
    mrrAtual: 42850,
    receitaDoMes: 51720,
    assinaturasAtivas: 143,
    churnMensal: 2.1,
    arpa: 299.65,
    projecao12m: 684900,
    arr: 514200,
    novasAssinaturasNoMes: 18,
    cancelamentosNoMes: 3,
    cadastrosPendentesDeAprovacao: 7,
    ticketMedio: 361.67,
  },
  revenueLast12Months: [
    { month: '05/2025', revenueBrl: 28100 },
    { month: '06/2025', revenueBrl: 29400 },
    { month: '07/2025', revenueBrl: 31800 },
    { month: '08/2025', revenueBrl: 33750 },
    { month: '09/2025', revenueBrl: 35120 },
    { month: '10/2025', revenueBrl: 36980 },
    { month: '11/2025', revenueBrl: 38440 },
    { month: '12/2025', revenueBrl: 40110 },
    { month: '01/2026', revenueBrl: 41820 },
    { month: '02/2026', revenueBrl: 43900 },
    { month: '03/2026', revenueBrl: 46240 },
    { month: '04/2026', revenueBrl: 51720 },
  ],
  subscriberGrowth: [
    { month: '05/2025', activeSubscribers: 94, projectedMrr: 28100 },
    { month: '06/2025', activeSubscribers: 99, projectedMrr: 29400 },
    { month: '07/2025', activeSubscribers: 105, projectedMrr: 31800 },
    { month: '08/2025', activeSubscribers: 111, projectedMrr: 33750 },
    { month: '09/2025', activeSubscribers: 116, projectedMrr: 35120 },
    { month: '10/2025', activeSubscribers: 122, projectedMrr: 36980 },
    { month: '11/2025', activeSubscribers: 127, projectedMrr: 38440 },
    { month: '12/2025', activeSubscribers: 132, projectedMrr: 40110 },
    { month: '01/2026', activeSubscribers: 136, projectedMrr: 41820 },
    { month: '02/2026', activeSubscribers: 139, projectedMrr: 43900 },
    { month: '03/2026', activeSubscribers: 141, projectedMrr: 46240 },
    { month: '04/2026', activeSubscribers: 143, projectedMrr: 51720 },
  ],
  newVsCancellations: [
    { month: '01/2026', newSubscriptions: 11, cancellations: 2 },
    { month: '02/2026', newSubscriptions: 13, cancellations: 4 },
    { month: '03/2026', newSubscriptions: 15, cancellations: 3 },
    { month: '04/2026', newSubscriptions: 18, cancellations: 3 },
  ],
  revenueByPlan: [
    { planName: 'Starter Mensal', mrrBrl: 14900, subscriptions: 100 },
    { planName: 'Pro Mensal', mrrBrl: 19136, subscriptions: 64 },
    { planName: 'Scale Mensal', mrrBrl: 8814, subscriptions: 18 },
  ],
  projections: [
    { months: 3, projectedMrrBrl: 48900, averageMonthlyGrowthBrl: 2016 },
    { months: 6, projectedMrrBrl: 54950, averageMonthlyGrowthBrl: 2016 },
    { months: 12, projectedMrrBrl: 67040, averageMonthlyGrowthBrl: 2016 },
  ],
  subscriptions: [
    {
      empresa: 'Astro Motors',
      plano: 'Pro Mensal',
      status: 'ACTIVE',
      mrr: 299,
      proximaCobranca: '2026-05-20',
      origem: 'Onboarding',
      inicio: '2026-01-20',
      cancelamento: null,
      motivoCancelamento: null,
    },
    {
      empresa: 'Rota Sul Veiculos',
      plano: 'Starter Mensal',
      status: 'CANCELLED',
      mrr: 0,
      proximaCobranca: null,
      origem: 'Indicacao',
      inicio: '2025-11-12',
      cancelamento: '2026-04-02',
      motivoCancelamento: 'Troca de DMS',
    },
  ],
};

export function useAdminDashboard() {
  return useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: async () => {
      const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';
      if (useMocks) {
        return mockAdminDashboardData;
      }
      return adminDashboardService.getDashboard();
    },
    staleTime: 60_000,
  });
}
