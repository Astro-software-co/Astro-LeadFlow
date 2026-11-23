import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { adminDashboardService } from '@/services/admin-dashboard.service';
import { useAdminDashboard } from '@/hooks/api/useAdminDashboard';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock('@/services/admin-dashboard.service', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/admin-dashboard.service')>();
  return {
    ...actual,
    adminDashboardService: {
      getDashboard: vi.fn(actual.adminDashboardService.getDashboard),
    },
  };
});

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('adminDashboardService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it('requests /admin/dashboard and returns response data', async () => {
    const dto = { metrics: { mrrAtual: 100 }, subscriptions: [] };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await adminDashboardService.getDashboard();

    expect(api.get).toHaveBeenCalledWith('/admin/dashboard');
    expect(result).toEqual(dto);
  });

  it('returns mock dashboard data without API calls when VITE_USE_MOCKS=true', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'true');

    const { result } = renderHook(() => useAdminDashboard(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminDashboardService.getDashboard).not.toHaveBeenCalled();
    expect(result.current.data?.metrics.mrrAtual).toBeGreaterThan(0);
  });

  it('calls service in API mode when VITE_USE_MOCKS is not true', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'false');
    vi.mocked(adminDashboardService.getDashboard).mockResolvedValueOnce({
      metrics: {
        mrrAtual: 100,
        receitaDoMes: 100,
        assinaturasAtivas: 1,
        churnMensal: 0,
        arpa: 100,
        projecao12m: 1200,
        arr: 1200,
        novasAssinaturasNoMes: 1,
        cancelamentosNoMes: 0,
        cadastrosPendentesDeAprovacao: 0,
        ticketMedio: 100,
      },
      revenueLast12Months: [],
      subscriberGrowth: [],
      newVsCancellations: [],
      revenueByPlan: [],
      projections: [],
      subscriptions: [],
    });

    const { result } = renderHook(() => useAdminDashboard(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminDashboardService.getDashboard).toHaveBeenCalledTimes(1);
  });
});
