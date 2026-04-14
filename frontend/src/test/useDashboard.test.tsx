import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useCategorySales, useDashboard, useKPIs, useMonthlySales, useRecentSales } from "@/hooks/api/useDashboard";
import { dashboardService } from "@/services/dashboard.service";

vi.mock("@/services/dashboard.service", () => ({
  dashboardService: {
    getKPIs: vi.fn(),
    getMonthlySales: vi.fn(),
    getCategorySales: vi.fn(),
  },
}));

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({
    user: { organizationId: "org-1" },
  }),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe("useDashboard hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useKPIs maps dashboard KPI dto to view model", async () => {
    vi.mocked(dashboardService.getKPIs).mockResolvedValueOnce({
      salesThisMonth: { value: 18, previousValue: 14, unit: "NUMBER", percentageChange: 28.6 },
      totalRevenue: { value: 920000, previousValue: 850000, unit: "CURRENCY", percentageChange: 8.2 },
      vehiclesInStock: { value: 77, previousValue: 79, unit: "NUMBER", percentageChange: -2.5 },
      averageTicket: { value: 51000, previousValue: 49000, unit: "CURRENCY", percentageChange: 4.1 },
      leadsConverted: { value: 35, previousValue: 33, unit: "PERCENTAGE", percentageChange: 6.1 },
      avgResponseTime: { value: 2.3, previousValue: 2.8, unit: "HOURS", percentageChange: -17.9 },
      totalLeads: { value: 230, previousValue: 200, unit: "NUMBER", percentageChange: 15 },
    });

    const { result } = renderHook(() => useKPIs(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(dashboardService.getKPIs).toHaveBeenCalledTimes(1);
    expect(result.current.data?.vendasMes).toBe(18);
    expect(result.current.data?.receitaTotal).toBe(920000);
    expect(result.current.data?.leadsConvertidosTrend).toBe(6.1);
  });

  it("useMonthlySales maps sales points into chart model", async () => {
    vi.mocked(dashboardService.getMonthlySales).mockResolvedValueOnce([
      { month: "2026-01", sales: 8, revenue: 420000 },
      { month: "2026-02", sales: 11, revenue: 580000 },
    ]);

    const { result } = renderHook(() => useMonthlySales(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(dashboardService.getMonthlySales).toHaveBeenCalledTimes(1);
    expect(result.current.data).toEqual([
      { month: "2026-01", vendas: 8, receita: 420000 },
      { month: "2026-02", vendas: 11, receita: 580000 },
    ]);
  });

  it("useCategorySales maps category points into chart model", async () => {
    vi.mocked(dashboardService.getCategorySales).mockResolvedValueOnce([
      { category: "SUV", sold: 13, inStock: 20 },
      { category: "Sedan", sold: 7, inStock: 9 },
    ]);

    const { result } = renderHook(() => useCategorySales(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(dashboardService.getCategorySales).toHaveBeenCalledTimes(1);
    expect(result.current.data).toEqual([
      { categoria: "SUV", vendidos: 13, emEstoque: 20 },
      { categoria: "Sedan", vendidos: 7, emEstoque: 9 },
    ]);
  });

  it("useRecentSales returns empty fallback array in real mode", async () => {
    const { result } = renderHook(() => useRecentSales(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it("useDashboard aggregates child queries and exposes safe defaults/refetch", async () => {
    vi.mocked(dashboardService.getKPIs).mockResolvedValueOnce({
      salesThisMonth: { value: 10, previousValue: 8, unit: "NUMBER", percentageChange: 25 },
      totalRevenue: { value: 450000, previousValue: 400000, unit: "CURRENCY", percentageChange: 12.5 },
      vehiclesInStock: { value: 50, previousValue: 52, unit: "NUMBER", percentageChange: -3.8 },
      averageTicket: { value: 45000, previousValue: 43000, unit: "CURRENCY", percentageChange: 4.6 },
      leadsConverted: { value: 20, previousValue: 18, unit: "PERCENTAGE", percentageChange: 11.1 },
      avgResponseTime: { value: 1.9, previousValue: 2.2, unit: "HOURS", percentageChange: -13.6 },
      totalLeads: { value: 120, previousValue: 100, unit: "NUMBER", percentageChange: 20 },
    });
    vi.mocked(dashboardService.getMonthlySales).mockResolvedValueOnce([{ month: "2026-01", sales: 10, revenue: 450000 }]);
    vi.mocked(dashboardService.getCategorySales).mockResolvedValueOnce([{ category: "SUV", sold: 5, inStock: 9 }]);

    const { result } = renderHook(() => useDashboard(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.kpis.vendasMes).toBe(10);
    expect(result.current.monthlySales).toHaveLength(1);
    expect(result.current.categorySales).toHaveLength(1);
    expect(result.current.recentSales).toEqual([]);
    await result.current.refetch();
  });
});

describe("useDashboard hooks (mock-mode branches)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it("returns mock recent sales list when VITE_USE_MOCKS=true", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const { useRecentSales: useRecentSalesMock } = await import("@/hooks/api/useDashboard");

    const { result } = renderHook(() => useRecentSalesMock(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.length).toBeGreaterThan(0);
  });
});
