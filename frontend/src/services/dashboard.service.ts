import api from '@/lib/api';
import { CategorySalesPointDTO, KpiResponseDTO, MonthlySalesPointDTO } from '@/types/api/analytics';

export interface DashboardData {
  kpis: KpiResponseDTO;
  monthlySales: MonthlySalesPointDTO[];
  categorySales: CategorySalesPointDTO[];
}

export const dashboardService = {
  async getKPIs(): Promise<KpiResponseDTO> {
    const response = await api.get<KpiResponseDTO>('/analytics/kpis');
    return response.data;
  },

  async getMonthlySales(): Promise<MonthlySalesPointDTO[]> {
    const response = await api.get<MonthlySalesPointDTO[]>('/analytics/charts/monthly-sales');
    return response.data;
  },

  async getCategorySales(): Promise<CategorySalesPointDTO[]> {
    const response = await api.get<CategorySalesPointDTO[]>('/analytics/charts/sales-by-category');
    return response.data;
  },
};
