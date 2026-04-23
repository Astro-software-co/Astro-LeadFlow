import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { usePlans, useSubscription, useUsage } from "@/hooks/api/useSaas";
import { saasService } from "@/services/saas.service";

vi.mock("@/services/saas.service", () => ({
  saasService: {
    getSubscription: vi.fn(),
    getUsage: vi.fn(),
    getPlans: vi.fn(),
    getMarketplaces: vi.fn(),
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

describe("useSaas hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useSubscription fetches active subscription from service", async () => {
    vi.mocked(saasService.getSubscription).mockResolvedValueOnce({
      planId: "plan-pro",
      planName: "Profissional",
      planDescription: "Plano Profissional para operacoes com mais volume.",
      priceBrl: 299,
      billingCycle: "MONTHLY",
      billingCycleLabel: "Mensal",
      messageLimit: 1000,
      marketplaceLimit: 3,
      status: "ACTIVE",
      renewsAt: "2026-05-13T00:00:00.000Z",
    });

    const { result } = renderHook(() => useSubscription(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(saasService.getSubscription).toHaveBeenCalledTimes(1);
    expect(result.current.data?.planName).toBe("Profissional");
  });

  it("useUsage fetches usage summary from service", async () => {
    vi.mocked(saasService.getUsage).mockResolvedValueOnce({
      planName: "Profissional",
      globalLimit: 1000,
      globalUsed: 650,
      globalRemaining: 350,
      renewsAt: "2026-05-13T00:00:00.000Z",
      marketplaces: [{ marketplaceName: "Webmotors", enabled: true, used: 300, limit: 500, remaining: 200 }],
    });

    const { result } = renderHook(() => useUsage(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(saasService.getUsage).toHaveBeenCalledTimes(1);
    expect(result.current.data?.globalUsed).toBe(650);
  });

  it("usePlans fetches plan catalog from service", async () => {
    vi.mocked(saasService.getPlans).mockResolvedValueOnce([
      {
        id: "starter",
        code: "STARTER_MONTHLY",
        name: "Starter",
        description: "Plano Starter.",
        priceBrl: 99,
        billingCycle: "MONTHLY",
        billingCycleLabel: "Mensal",
        messageLimit: 200,
        marketplaceLimit: 1,
      },
      {
        id: "pro",
        code: "PRO_MONTHLY",
        name: "Profissional",
        description: "Plano Profissional.",
        priceBrl: 299,
        billingCycle: "MONTHLY",
        billingCycleLabel: "Mensal",
        messageLimit: 1000,
        marketplaceLimit: 3,
      },
    ]);

    const { result } = renderHook(() => usePlans(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(saasService.getPlans).toHaveBeenCalledTimes(1);
    expect(result.current.data).toHaveLength(2);
  });
});

describe("useSaas hooks (mock-mode branches)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it("returns mocked subscription and usage payloads when VITE_USE_MOCKS=true", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const {
      usePlans: usePlansMock,
      useSubscription: useSubscriptionMock,
      useUsage: useUsageMock,
    } = await import("@/hooks/api/useSaas");

    const subscription = renderHook(() => useSubscriptionMock(), { wrapper: createWrapper() });
    const usage = renderHook(() => useUsageMock(), { wrapper: createWrapper() });
    const plans = renderHook(() => usePlansMock(), { wrapper: createWrapper() });

    await waitFor(() => expect(subscription.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(usage.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(plans.result.current.isSuccess).toBe(true));

    expect(subscription.result.current.data?.planName).toBe("Profissional");
    expect(usage.result.current.data?.marketplaces.length).toBeGreaterThan(0);
    expect(plans.result.current.data?.length).toBeGreaterThan(0);
    expect(saasService.getSubscription).not.toHaveBeenCalled();
    expect(saasService.getUsage).not.toHaveBeenCalled();
    expect(saasService.getPlans).not.toHaveBeenCalled();
  });
});
