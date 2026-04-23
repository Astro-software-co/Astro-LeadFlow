import { Helmet } from 'react-helmet-async';
import { TrendingUp, DollarSign, Car, Receipt, Users, Clock, Activity } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { formatCurrency } from '@/lib/formatters';
import { CategoryBarChart, KPICard, RecentSalesTable, SalesLineChart, StockTable, useDashboard } from '@/modules/crm';
import { useVehicles } from '@/modules/inventory';

function DashboardSkeleton() {
  return (
    <div data-testid="dashboard-skeleton" className="space-y-6">
      {/* KPI Row 1: 4 cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} data-testid="skeleton-kpi-card" className="rounded-xl border bg-card p-6 shadow-sm">
            <div className="flex items-start justify-between">
              <div className="space-y-3">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-4 w-40" />
              </div>
              <Skeleton className="h-12 w-12 rounded-xl" />
            </div>
          </div>
        ))}
      </div>

      {/* KPI Row 2: 3 cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} data-testid="skeleton-kpi-card" className="rounded-xl border bg-card p-6 shadow-sm">
            <div className="flex items-start justify-between">
              <div className="space-y-3">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-4 w-40" />
              </div>
              <Skeleton className="h-12 w-12 rounded-xl" />
            </div>
          </div>
        ))}
      </div>

      {/* Chart placeholders */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Skeleton className="h-64 w-full rounded-xl" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>

      {/* Table placeholders */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Skeleton className="h-64 w-full rounded-xl" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    </div>
  );
}

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
    return <DashboardSkeleton />;
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
