import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '@/services/dashboard.service';
import { mockKPIData, mockMonthlySales, mockCategorySales, mockRecentSales } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useKPIs() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  return useQuery({
    queryKey: [orgId, 'dashboard', 'kpis'],
    queryFn: () => useMocks ? mockKPIData : dashboardService.getKPIs(),
    staleTime: 60_000,
  });
}

export function useMonthlySales() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  return useQuery({
    queryKey: [orgId, 'dashboard', 'monthly-sales'],
    queryFn: () => useMocks ? mockMonthlySales : dashboardService.getMonthlySales(),
    staleTime: 60_000,
  });
}

export function useCategorySales() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  return useQuery({
    queryKey: [orgId, 'dashboard', 'category-sales'],
    queryFn: () => useMocks ? mockCategorySales : dashboardService.getCategorySales(),
    staleTime: 60_000,
  });
}

export function useRecentSales() {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  return useQuery({
    queryKey: [orgId, 'dashboard', 'recent-sales'],
    queryFn: () => useMocks ? mockRecentSales : dashboardService.getRecentSales(),
    staleTime: 60_000,
  });
}
