import { CategorySalesData, KPIData, MonthlySalesData } from '@/types';
import { CategorySalesPointDTO, KpiCardDTO, KpiResponseDTO, MonthlySalesPointDTO } from '@/types/api/analytics';

function mapValue(card: KpiCardDTO): number {
  return Number(card.value ?? 0);
}

function mapTrend(card: KpiCardDTO): number {
  return Number(card.percentageChange ?? 0);
}

export function mapKpiResponseToStats(dto: KpiResponseDTO): KPIData {
  return {
    vendasMes: mapValue(dto.salesThisMonth),
    vendasMesTrend: mapTrend(dto.salesThisMonth),
    receitaTotal: mapValue(dto.totalRevenue),
    receitaTotalTrend: mapTrend(dto.totalRevenue),
    carrosEstoque: mapValue(dto.vehiclesInStock),
    carrosEstoqueTrend: mapTrend(dto.vehiclesInStock),
    ticketMedio: mapValue(dto.averageTicket),
    ticketMedioTrend: mapTrend(dto.averageTicket),
    leadsConvertidos: mapValue(dto.leadsConverted),
    leadsConvertidosTrend: mapTrend(dto.leadsConverted),
    tempoMedioResposta: mapValue(dto.avgResponseTime),
    tempoMedioRespostaTrend: mapTrend(dto.avgResponseTime),
    totalLeads: mapValue(dto.totalLeads),
    totalLeadsTrend: mapTrend(dto.totalLeads),
  };
}

export function mapMonthlySalesPoint(dto: MonthlySalesPointDTO): MonthlySalesData {
  return {
    month: dto.month,
    vendas: dto.sales,
    receita: dto.revenue,
  };
}

export function mapCategorySalesPoint(dto: CategorySalesPointDTO): CategorySalesData {
  return {
    categoria: dto.category,
    vendidos: dto.sold,
    emEstoque: dto.inStock,
  };
}
