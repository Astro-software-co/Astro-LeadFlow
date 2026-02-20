import { TrendingUp, DollarSign, Car, Receipt, Users, Clock, Activity } from 'lucide-react';
import { MainLayout } from '@/components/layout/MainLayout';
import { KPICard } from '@/components/dashboard/KPICard';
import { SalesLineChart } from '@/components/dashboard/SalesLineChart';
import { CategoryBarChart } from '@/components/dashboard/CategoryBarChart';
import { StockTable } from '@/components/dashboard/StockTable';
import { RecentSalesTable } from '@/components/dashboard/RecentSalesTable';
import {
  mockKPIData,
  mockMonthlySales,
  mockCategorySales,
  mockVehicles,
  mockRecentSales,
} from '@/data/mockData';

function formatCurrency(value: number): string {
  if (value >= 1000000) {
    return `R$ ${(value / 1000000).toFixed(1)}M`;
  }
  if (value >= 1000) {
    return `R$ ${(value / 1000).toFixed(1)}K`;
  }
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 0,
  }).format(value);
}

export default function Dashboard() {
  return (
    <MainLayout>
      <div className="space-y-6">
        {/* KPI Cards */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <KPICard
            title="Vendas do Mês"
            value={mockKPIData.vendasMes.toString()}
            trend={mockKPIData.vendasMesTrend}
            icon={TrendingUp}
            iconBgColor="bg-primary/10"
          />
          <KPICard
            title="Receita Total"
            value={formatCurrency(mockKPIData.receitaTotal)}
            trend={mockKPIData.receitaTotalTrend}
            icon={DollarSign}
            iconBgColor="bg-kanban-negotiation/10"
          />
          <KPICard
            title="Carros em Estoque"
            value={mockKPIData.carrosEstoque.toString()}
            trend={mockKPIData.carrosEstoqueTrend}
            icon={Car}
            iconBgColor="bg-chart-primary/10"
          />
          <KPICard
            title="Ticket Médio"
            value={formatCurrency(mockKPIData.ticketMedio)}
            trend={mockKPIData.ticketMedioTrend}
            icon={Receipt}
            iconBgColor="bg-kanban-completed/10"
          />
        </div>

        {/* Secondary KPIs */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <KPICard
            title="Leads Convertidos"
            value={`${mockKPIData.leadsConvertidos}%`}
            trend={mockKPIData.leadsConvertidosTrend}
            icon={Users}
            iconBgColor="bg-kanban-negotiation/10"
          />
          <KPICard
            title="Tempo Médio de Resposta"
            value={`${mockKPIData.tempoMedioResposta}h`}
            trend={mockKPIData.tempoMedioRespostaTrend}
            icon={Clock}
            iconBgColor="bg-primary/10"
          />
          <KPICard
            title="Total de Leads"
            value={mockKPIData.totalLeads.toString()}
            trend={mockKPIData.totalLeadsTrend}
            icon={Activity}
            iconBgColor="bg-chart-primary/10"
          />
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <SalesLineChart data={mockMonthlySales} />
          <CategoryBarChart data={mockCategorySales} />
        </div>

        {/* Tables */}
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <StockTable vehicles={mockVehicles} />
          <RecentSalesTable sales={mockRecentSales} />
        </div>
      </div>
    </MainLayout>
  );
}
