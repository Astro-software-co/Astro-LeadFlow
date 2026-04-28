import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/lib/api";
import { adminObservabilityService } from "@/services/admin-observability.service";

vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("adminObservabilityService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("loads overview snapshot", async () => {
    const dto = {
      uptimeSeconds: 120,
      health: {
        abacatePayConfigured: true,
        metaWebhookSecretConfigured: true,
        metaApiTokenConfigured: true,
        redisUp: true,
        dbUp: true,
        chatbotEngineMode: "mock",
        awsLexConfigured: false,
        awsBedrockConfigured: false,
        publicApiUrlConfigured: true,
      },
      requestsPerMinute: 3,
      errorRatePercent: 0,
      p95LatencyMs: 55,
    };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    await expect(adminObservabilityService.overview()).resolves.toEqual(dto);

    expect(api.get).toHaveBeenCalledWith("/admin/observability/overview");
  });

  it("loads filtered errors", async () => {
    const dto = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    const params = { level: "ERROR", correlationId: "corr-1" };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    await expect(adminObservabilityService.errors(params)).resolves.toEqual(dto);

    expect(api.get).toHaveBeenCalledWith("/admin/observability/errors", { params });
  });

  it("loads filtered webhook events", async () => {
    const dto = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    const params = { source: "META" };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    await expect(adminObservabilityService.webhooks(params)).resolves.toEqual(dto);

    expect(api.get).toHaveBeenCalledWith("/admin/observability/webhooks", { params });
  });

  it("loads chatbot insights and infrastructure", async () => {
    const insights = {
      totalSessions: 7,
      globalFallbackRatePercent: 20,
      tokensConsumed: 500,
      topOrganizationsByTokens: [],
      averageLatencyByEngineMs: {},
    };
    const infrastructure = { health: { dbUp: true } };
    vi.mocked(api.get).mockResolvedValueOnce({ data: insights }).mockResolvedValueOnce({ data: infrastructure });

    await expect(adminObservabilityService.chatbotInsights()).resolves.toEqual(insights);
    await expect(adminObservabilityService.infrastructure()).resolves.toEqual(infrastructure);

    expect(api.get).toHaveBeenNthCalledWith(1, "/admin/observability/chatbot-insights");
    expect(api.get).toHaveBeenNthCalledWith(2, "/admin/observability/infrastructure");
  });
});
