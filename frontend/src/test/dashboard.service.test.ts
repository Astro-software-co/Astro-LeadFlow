import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/lib/api";
import { dashboardService } from "@/services/dashboard.service";

vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("dashboardService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getKPIs requests analytics/kpis and returns response data", async () => {
    const dto = {
      salesThisMonth: { value: 21, previousValue: 19, unit: "NUMBER", percentageChange: 10.5 },
      totalRevenue: { value: 1_250_000, previousValue: 1_100_000, unit: "CURRENCY", percentageChange: 13.6 },
      vehiclesInStock: { value: 87, previousValue: 82, unit: "NUMBER", percentageChange: 6.1 },
      averageTicket: { value: 59_000, previousValue: 57_500, unit: "CURRENCY", percentageChange: 2.6 },
      leadsConverted: { value: 32, previousValue: 29, unit: "PERCENTAGE", percentageChange: 10.3 },
      avgResponseTime: { value: 1.4, previousValue: 1.6, unit: "HOURS", percentageChange: -12.5 },
      totalLeads: { value: 310, previousValue: 280, unit: "NUMBER", percentageChange: 10.7 },
    } as const;

    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await dashboardService.getKPIs();

    expect(api.get).toHaveBeenCalledWith("/analytics/kpis");
    expect(result).toEqual(dto);
  });

  it("getMonthlySales requests monthly sales chart endpoint", async () => {
    const dto = [
      { month: "2026-01", sales: 12, revenue: 720_000 },
      { month: "2026-02", sales: 15, revenue: 830_000 },
    ];

    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await dashboardService.getMonthlySales();

    expect(api.get).toHaveBeenCalledWith("/analytics/charts/monthly-sales");
    expect(result).toEqual(dto);
  });

  it("getCategorySales requests sales-by-category endpoint", async () => {
    const dto = [
      { category: "SUV", sold: 18, inStock: 24 },
      { category: "Sedan", sold: 11, inStock: 16 },
    ];

    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await dashboardService.getCategorySales();

    expect(api.get).toHaveBeenCalledWith("/analytics/charts/sales-by-category");
    expect(result).toEqual(dto);
  });
});
