import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/lib/api";
import { saasService } from "@/services/saas.service";

vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("saasService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getSubscription fetches current subscription", async () => {
    const dto = {
      planId: "plan-profissional",
      planName: "Profissional",
      planDescription: "Plano Profissional para operacoes com mais volume.",
      priceBrl: 299,
      billingCycle: "MONTHLY" as const,
      billingCycleLabel: "Mensal",
      messageLimit: 1000,
      marketplaceLimit: 3,
      status: "ACTIVE" as const,
      renewsAt: "2026-05-13T00:00:00.000Z",
    };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await saasService.getSubscription();

    expect(api.get).toHaveBeenCalledWith("/saas/subscription");
    expect(result).toEqual(dto);
  });

  it("getUsage fetches usage summary", async () => {
    const dto = {
      planName: "Profissional",
      globalLimit: 1000,
      globalUsed: 500,
      globalRemaining: 500,
      renewsAt: "2026-05-13T00:00:00.000Z",
      marketplaces: [{ marketplaceName: "Webmotors", enabled: true, used: 300, limit: 500, remaining: 200 }],
    };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await saasService.getUsage();

    expect(api.get).toHaveBeenCalledWith("/saas/usage");
    expect(result).toEqual(dto);
  });

  it("getPlans fetches available plans", async () => {
    const dto = [{
      id: "starter",
      code: "STARTER_MONTHLY",
      name: "Starter",
      description: "Plano Starter.",
      priceBrl: 99,
      billingCycle: "MONTHLY" as const,
      billingCycleLabel: "Mensal",
      messageLimit: 200,
      marketplaceLimit: 1,
    }];
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await saasService.getPlans();

    expect(api.get).toHaveBeenCalledWith("/plans");
    expect(result).toEqual(dto);
  });

  it("getMarketplaces fetches marketplace configs", async () => {
    const dto = [{ id: "mk-1", marketplaceName: "Webmotors", enabled: true, monthlyLimit: 500 }];
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await saasService.getMarketplaces();

    expect(api.get).toHaveBeenCalledWith("/saas/marketplaces");
    expect(result).toEqual(dto);
  });

  it("createSubscriptionCheckout creates a self-service checkout", async () => {
    const dto = {
      planId: "plan-profissional",
      planName: "Profissional",
      checkoutId: "bill_123",
      checkoutUrl: "https://app.abacatepay.com/pay/bill_123",
    };
    const payload = {
      returnUrl: "http://localhost:5173/app/configuracoes",
      completionUrl: "http://localhost:5173/app/configuracoes",
    };
    vi.mocked(api.post).mockResolvedValueOnce({ data: dto });

    const result = await saasService.createSubscriptionCheckout("plan-profissional", payload);

    expect(api.post).toHaveBeenCalledWith("/saas/subscription/checkout/plan-profissional", payload);
    expect(result).toEqual(dto);
  });

  it("cancelSubscription posts cancel request", async () => {
    const dto = {
      planId: "plan-profissional",
      planName: "Profissional",
      planDescription: "Plano Profissional para operacoes com mais volume.",
      priceBrl: 299,
      billingCycle: "MONTHLY" as const,
      billingCycleLabel: "Mensal",
      messageLimit: 1000,
      marketplaceLimit: 3,
      status: "CANCELLED" as const,
      renewsAt: null,
      paymentProvider: "ABACATEPAY" as const,
      checkoutUrl: null,
      canCancel: false,
    };
    vi.mocked(api.post).mockResolvedValueOnce({ data: dto });

    const result = await saasService.cancelSubscription();

    expect(api.post).toHaveBeenCalledWith("/saas/subscription/cancel");
    expect(result).toEqual(dto);
  });
});
