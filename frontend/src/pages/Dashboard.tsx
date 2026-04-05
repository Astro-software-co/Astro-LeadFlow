import { Helmet } from 'react-helmet-async';
import { TrendingUp, DollarSign, Car, Receipt, Users, Clock, Activity, Loader2 } from 'lucide-react';
import { KPICard } from '@/components/dashboard/KPICard';
import { SalesLineChart } from '@/components/dashboard/SalesLineChart';
import { CategoryBarChart } from '@/components/dashboard/CategoryBarChart';
import { StockTable } from '@/components/dashboard/StockTable';
import { RecentSalesTable } from '@/components/dashboard/RecentSalesTable';
import { Button } from '@/components/ui/button';
import { useDashboard } from '@/hooks/api/useDashboard';
import { useVehicles } from '@/hooks/api/useVehicles';
import { formatCurrency } from '@/lib/formatters';

export default function Dashboard() {
  const {
    kpis,
    monthlySales,
    categorySales,
    recentSales,
    isLoading: isDashboardLoading,
    error: dashboardError,
    refetch,
  } = useDashboard();

  const {
    data: vehiclesData,
    isLoading: isVehiclesLoading,
    error: vehiclesError,
    refetch: refetchVehicles,
  } = useVehicles();

  const isLoading = isDashboardLoading || isVehiclesLoading;
  const error = dashboardError ?? vehiclesError;
  const vehicles = vehiclesData?.content ?? [];

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar o dashboard</h2>
        <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
        <Button
          className="mt-4"
          onClick={() => {
            void refetch();
            void refetchVehicles();
          }}
        >
          Tentar novamente
        </Button>
      </div>
    );
  }

  return (
    <>
      <Helmet>
        <title>Dashboard — Astro LeadFlow</title>
        <meta name="description" content="Painel de KPIs, vendas e performance da sua concessionaria." />
      </Helmet>
      <div className="space-y-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <KPICard
            title="Vendas do Mes"
            value={kpis.vendasMes.toString()}
            trend={kpis.vendasMesTrend}
            icon={TrendingUp}
            iconBgColor="bg-primary/10"
          />
          <KPICard
            title="Receita Total"
            value={formatCurrency(kpis.receitaTotal, { compact: true })}
            trend={kpis.receitaTotalTrend}
            icon={DollarSign}
            iconBgColor="bg-kanban-negotiation/10"
          />
          <KPICard
            title="Carros em Estoque"
            value={kpis.carrosEstoque.toString()}
            trend={kpis.carrosEstoqueTrend}
            icon={Car}
            iconBgColor="bg-chart-primary/10"
          />
          <KPICard
            title="Ticket Medio"
            value={formatCurrency(kpis.ticketMedio)}
            trend={kpis.ticketMedioTrend}
            icon={Receipt}
            iconBgColor="bg-kanban-completed/10"
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <KPICard
            title="Leads Convertidos"
            value={`${kpis.leadsConvertidos}%`}
            trend={kpis.leadsConvertidosTrend}
            icon={Users}
            iconBgColor="bg-kanban-negotiation/10"
          />
          <KPICard
            title="Tempo Medio de Resposta"
            value={`${kpis.tempoMedioResposta}h`}
            trend={kpis.tempoMedioRespostaTrend}
            icon={Clock}
            iconBgColor="bg-primary/10"
          />
          <KPICard
            title="Total de Leads"
            value={kpis.totalLeads.toString()}
            trend={kpis.totalLeadsTrend}
            icon={Activity}
            iconBgColor="bg-chart-primary/10"
          />
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <SalesLineChart data={monthlySales} />
          <CategoryBarChart data={categorySales} />
        </div>

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <StockTable vehicles={vehicles} />
          <RecentSalesTable sales={recentSales} />
        </div>
      </div>
    </>
  );
}
