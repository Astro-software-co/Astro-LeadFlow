import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '@/services/dashboard.service';
import { mockKPIData, mockMonthlySales, mockCategorySales, mockRecentSales } from '@/data/mockData';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useKPIs() {
  return useQuery({
    queryKey: ['dashboard', 'kpis'],
    queryFn: () => useMocks ? mockKPIData : dashboardService.getKPIs(),
    staleTime: 60_000,
  });
}

export function useMonthlySales() {
  return useQuery({
    queryKey: ['dashboard', 'monthly-sales'],
    queryFn: () => useMocks ? mockMonthlySales : dashboardService.getMonthlySales(),
    staleTime: 60_000,
  });
}

export function useCategorySales() {
  return useQuery({
    queryKey: ['dashboard', 'category-sales'],
    queryFn: () => useMocks ? mockCategorySales : dashboardService.getCategorySales(),
    staleTime: 60_000,
  });
}

export function useRecentSales() {
  return useQuery({
    queryKey: ['dashboard', 'recent-sales'],
    queryFn: () => useMocks ? mockRecentSales : dashboardService.getRecentSales(),
    staleTime: 60_000,
  });
}
