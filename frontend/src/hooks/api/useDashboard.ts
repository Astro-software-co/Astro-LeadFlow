import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '@/services/dashboard.service';
import { mockKPIData, mockMonthlySales, mockCategorySales, mockRecentSales } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';
import { CategorySalesData, KPIData, MonthlySalesData, RecentSale } from '@/types';
import { mapCategorySalesPoint, mapKpiResponseToStats, mapMonthlySalesPoint } from '@/mappers/analytics.mapper';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

const emptyKpis: KPIData = {
  vendasMes: 0,
  vendasMesTrend: 0,
  receitaTotal: 0,
  receitaTotalTrend: 0,
  carrosEstoque: 0,
  carrosEstoqueTrend: 0,
  ticketMedio: 0,
  ticketMedioTrend: 0,
  leadsConvertidos: 0,
  leadsConvertidosTrend: 0,
  tempoMedioResposta: 0,
  tempoMedioRespostaTrend: 0,
  totalLeads: 0,
  totalLeadsTrend: 0,
};

const emptyMonthlySales: MonthlySalesData[] = [];
const emptyCategorySales: CategorySalesData[] = [];
const emptyRecentSales: RecentSale[] = [];

export function useKPIs() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'dashboard', 'kpis'],
    queryFn: async () => {
      if (useMocks) {
        return mockKPIData;
      }

      const kpis = await dashboardService.getKPIs();
      return mapKpiResponseToStats(kpis);
    },
    staleTime: 60_000,
  });
}

export function useMonthlySales() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'dashboard', 'monthly-sales'],
    queryFn: async () => {
      if (useMocks) {
        return mockMonthlySales;
      }

      const points = await dashboardService.getMonthlySales();
      return points.map(mapMonthlySalesPoint);
    },
    staleTime: 60_000,
  });
}

export function useCategorySales() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'dashboard', 'category-sales'],
    queryFn: async () => {
      if (useMocks) {
        return mockCategorySales;
      }

      const points = await dashboardService.getCategorySales();
      return points.map(mapCategorySalesPoint);
    },
    staleTime: 60_000,
  });
}

export function useRecentSales() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'dashboard', 'recent-sales'],
    queryFn: async () => (useMocks ? mockRecentSales : []),
    staleTime: 60_000,
  });
}

export function useDashboard() {
  const kpisQuery = useKPIs();
  const monthlySalesQuery = useMonthlySales();
  const categorySalesQuery = useCategorySales();
  const recentSalesQuery = useRecentSales();

  return {
    kpis: kpisQuery.data ?? emptyKpis,
    monthlySales: monthlySalesQuery.data ?? emptyMonthlySales,
    categorySales: categorySalesQuery.data ?? emptyCategorySales,
    recentSales: recentSalesQuery.data ?? emptyRecentSales,
    isLoading:
      kpisQuery.isLoading || monthlySalesQuery.isLoading || categorySalesQuery.isLoading || recentSalesQuery.isLoading,
    error: kpisQuery.error || monthlySalesQuery.error || categorySalesQuery.error || recentSalesQuery.error || null,
    refetch: async () => {
      await Promise.all([
        kpisQuery.refetch(),
        monthlySalesQuery.refetch(),
        categorySalesQuery.refetch(),
        recentSalesQuery.refetch(),
      ]);
    },
  };
}
