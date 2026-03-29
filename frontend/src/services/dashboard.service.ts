import api from '@/lib/api';
import { KPIData, MonthlySalesData, CategorySalesData, RecentSale } from '@/types';

export interface DashboardData {
  kpis: KPIData;
  monthlySales: MonthlySalesData[];
  categorySales: CategorySalesData[];
  recentSales: RecentSale[];
}

export const dashboardService = {
  async getKPIs(): Promise<KPIData> {
    const response = await api.get<KPIData>('/dashboard/kpis');
    return response.data;
  },

  async getMonthlySales(): Promise<MonthlySalesData[]> {
    const response = await api.get<MonthlySalesData[]>('/dashboard/monthly-sales');
    return response.data;
  },

  async getCategorySales(): Promise<CategorySalesData[]> {
    const response = await api.get<CategorySalesData[]>('/dashboard/category-sales');
    return response.data;
  },

  async getRecentSales(): Promise<RecentSale[]> {
    const response = await api.get<RecentSale[]>('/dashboard/recent-sales');
    return response.data;
  },
};
