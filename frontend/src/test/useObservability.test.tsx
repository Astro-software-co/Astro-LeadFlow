import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  useChatbotInsights,
  useInfrastructure,
  useObservabilityErrors,
  useObservabilityOverview,
  useObservabilityWebhooks,
} from "@/hooks/api/useObservability";
import { adminObservabilityService } from "@/services/admin-observability.service";

vi.mock("@/services/admin-observability.service", () => ({
  adminObservabilityService: {
    overview: vi.fn(),
    errors: vi.fn(),
    webhooks: vi.fn(),
    chatbotInsights: vi.fn(),
    infrastructure: vi.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe("useObservability hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches overview", async () => {
    vi.mocked(adminObservabilityService.overview).mockResolvedValueOnce({
      uptimeSeconds: 60,
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
      requestsPerMinute: 1,
      errorRatePercent: 0,
      p95LatencyMs: 10,
    });

    const { result } = renderHook(() => useObservabilityOverview(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminObservabilityService.overview).toHaveBeenCalledTimes(1);
  });

  it("fetches errors with params and realtime polling enabled", async () => {
    const params = { correlationId: "corr-1" };
    vi.mocked(adminObservabilityService.errors).mockResolvedValueOnce({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    const { result } = renderHook(() => useObservabilityErrors(params, true), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminObservabilityService.errors).toHaveBeenCalledWith(params);
  });

  it("fetches webhook events", async () => {
    const params = { source: "META" };
    vi.mocked(adminObservabilityService.webhooks).mockResolvedValueOnce({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });

    const { result } = renderHook(() => useObservabilityWebhooks(params), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminObservabilityService.webhooks).toHaveBeenCalledWith(params);
  });

  it("fetches chatbot insights and infrastructure", async () => {
    vi.mocked(adminObservabilityService.chatbotInsights).mockResolvedValueOnce({
      totalSessions: 1,
      globalFallbackRatePercent: 0,
      tokensConsumed: 0,
      topOrganizationsByTokens: [],
      averageLatencyByEngineMs: {},
    });
    vi.mocked(adminObservabilityService.infrastructure).mockResolvedValueOnce({ health: { dbUp: true } });

    const insights = renderHook(() => useChatbotInsights(), { wrapper: createWrapper() });
    const infra = renderHook(() => useInfrastructure(), { wrapper: createWrapper() });

    await waitFor(() => expect(insights.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(infra.result.current.isSuccess).toBe(true));
    expect(adminObservabilityService.chatbotInsights).toHaveBeenCalledTimes(1);
    expect(adminObservabilityService.infrastructure).toHaveBeenCalledTimes(1);
  });
});
