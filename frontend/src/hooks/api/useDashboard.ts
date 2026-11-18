import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '@/services/dashboard.service';
import { mockKPIData, mockMonthlySales, mockCategorySales, mockRecentSales } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';
import { CategorySalesData, KPIData, Lead, MonthlySalesData, RecentSale } from '@/types';
import { mapCategorySalesPoint, mapKpiResponseToStats, mapMonthlySalesPoint } from '@/mappers/analytics.mapper';
import { useLeads } from '@/hooks/api/useLeads';

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

function mapLeadToRecentSale(lead: Lead): RecentSale {
  return {
    cliente: lead.nome,
    veiculo: lead.interesse || 'Veiculo nao informado',
    valor: lead.orcamento,
    data: lead.ultimaInteracao.toLocaleDateString('pt-BR'),
    vendedor: lead.assignedToName || 'Sem responsavel',
  };
}

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

export function useDashboard() {
  const kpisQuery = useKPIs();
  const monthlySalesQuery = useMonthlySales();
  const categorySalesQuery = useCategorySales();
  const wonLeadsQuery = useLeads({ stage: 'COMPLETED', page: 0, size: 50 });

  const recentSales = useMocks
    ? mockRecentSales
    : [...(wonLeadsQuery.data?.content ?? [])]
        .sort((a, b) => b.ultimaInteracao.getTime() - a.ultimaInteracao.getTime())
        .slice(0, 5)
        .map(mapLeadToRecentSale);

  return {
    kpis: kpisQuery.data ?? emptyKpis,
    monthlySales: monthlySalesQuery.data ?? emptyMonthlySales,
    categorySales: categorySalesQuery.data ?? emptyCategorySales,
    recentSales: recentSales ?? emptyRecentSales,
    isLoading:
      kpisQuery.isLoading || monthlySalesQuery.isLoading || categorySalesQuery.isLoading || wonLeadsQuery.isLoading,
    error: kpisQuery.error || monthlySalesQuery.error || categorySalesQuery.error || wonLeadsQuery.error || null,
    refetch: async () => {
      await Promise.all([
        kpisQuery.refetch(),
        monthlySalesQuery.refetch(),
        categorySalesQuery.refetch(),
        wonLeadsQuery.refetch(),
      ]);
    },
  };
}
