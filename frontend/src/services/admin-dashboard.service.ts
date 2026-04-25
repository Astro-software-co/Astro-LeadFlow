import api from '@/lib/api';
import type { AdminDashboardDTO } from '@/types/api/admin-dashboard';

export const adminDashboardService = {
  async getDashboard(): Promise<AdminDashboardDTO> {
    const response = await api.get<AdminDashboardDTO>('/admin/dashboard');
    return response.data;
  },
};
