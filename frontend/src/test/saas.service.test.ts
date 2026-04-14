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
      monthlyPriceBrl: 299,
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
    const dto = [{ id: "starter", name: "Starter", monthlyPriceBrl: 99, messageLimit: 200, marketplaceLimit: 1 }];
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
});
