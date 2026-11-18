export type KpiUnitDTO = 'NUMBER' | 'CURRENCY' | 'PERCENTAGE' | 'HOURS';

export interface KpiCardDTO {
  value: number;
  previousValue: number;
  unit: KpiUnitDTO;
  percentageChange: number;
}

export interface KpiResponseDTO {
  salesThisMonth: KpiCardDTO;
  totalRevenue: KpiCardDTO;
  vehiclesInStock: KpiCardDTO;
  averageTicket: KpiCardDTO;
  leadsConverted: KpiCardDTO;
  avgResponseTime: KpiCardDTO;
  totalLeads: KpiCardDTO;
}

export interface MonthlySalesPointDTO {
  month: string;
  sales: number;
  revenue: number;
}

export interface CategorySalesPointDTO {
  category: string;
  sold: number;
  inStock: number;
}
