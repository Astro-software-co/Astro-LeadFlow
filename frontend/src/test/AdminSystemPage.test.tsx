import { fireEvent, render, screen } from "@testing-library/react";
import { HelmetProvider } from "react-helmet-async";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminSystem from "@/pages/admin/AdminSystem";
import {
  useChatbotInsights,
  useInfrastructure,
  useObservabilityErrors,
  useObservabilityOverview,
  useObservabilityWebhooks,
} from "@/hooks/api/useObservability";

vi.mock("@/components/ui/tabs", () => ({
  Tabs: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  TabsList: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  TabsTrigger: ({ children }: { children: ReactNode }) => (
    <button type="button" role="tab">
      {children}
    </button>
  ),
  TabsContent: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

vi.mock("@/hooks/api/useObservability", () => ({
  useObservabilityOverview: vi.fn(),
  useObservabilityErrors: vi.fn(),
  useObservabilityWebhooks: vi.fn(),
  useChatbotInsights: vi.fn(),
  useInfrastructure: vi.fn(),
}));

function renderPage() {
  return render(
    <HelmetProvider>
      <AdminSystem />
    </HelmetProvider>,
  );
}

describe("AdminSystem page", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useObservabilityOverview).mockReturnValue({
      data: {
        uptimeSeconds: 600,
        requestsPerMinute: 4,
        errorRatePercent: 0.5,
        p95LatencyMs: 120,
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
      },
    } as unknown as ReturnType<typeof useObservabilityOverview>);
    vi.mocked(useObservabilityErrors).mockReturnValue({
      data: {
        content: [
          {
            id: 1,
            occurredAt: new Date().toISOString(),
            level: "ERROR",
            loggerName: "com.astro.Test",
            correlationId: "corr-1",
            message: "boom",
          },
        ],
        totalElements: 1,
      },
    } as unknown as ReturnType<typeof useObservabilityErrors>);
    vi.mocked(useObservabilityWebhooks).mockReturnValue({
      data: {
        content: [
          { id: 1, receivedAt: new Date().toISOString(), source: "META", outcome: "SUCCESS", httpStatus: 200, durationMs: 25 },
        ],
      },
    } as unknown as ReturnType<typeof useObservabilityWebhooks>);
    vi.mocked(useChatbotInsights).mockReturnValue({
      data: { totalSessions: 5, globalFallbackRatePercent: 10, tokensConsumed: 1500, topOrganizationsByTokens: [], averageLatencyByEngineMs: {} },
    } as unknown as ReturnType<typeof useChatbotInsights>);
    vi.mocked(useInfrastructure).mockReturnValue({
      data: { health: { dbUp: true, redisUp: true, publicApiUrlConfigured: true } },
    } as unknown as ReturnType<typeof useInfrastructure>);
  });

  it("renders overview metrics and recent errors", () => {
    renderPage();

    expect(screen.getByText("Sistema")).toBeInTheDocument();
    expect(screen.getByText("Top 5 erros recentes")).toBeInTheDocument();
    expect(screen.getAllByText("com.astro.Test").length).toBeGreaterThan(0);
    expect(screen.getAllByText("corr-1").length).toBeGreaterThan(0);
  });

  it("renders observability filters and realtime toggle", async () => {
    renderPage();

    expect(await screen.findByPlaceholderText("correlation_id")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Pausar" })).toBeInTheDocument();
  });

  it("renders webhooks, chatbot insights, and infrastructure tabs", async () => {
    renderPage();

    expect(await screen.findByText("Eventos de webhook")).toBeInTheDocument();
    expect(screen.getByText("META")).toBeInTheDocument();

    expect(await screen.findByText("Sessoes totais")).toBeInTheDocument();
    expect(screen.getByText("1500")).toBeInTheDocument();

    expect(await screen.findByText("Servicos")).toBeInTheDocument();
    expect(screen.getByText("dbUp")).toBeInTheDocument();
  });

  it("renders fallback states, warn rows, null fields, and toggles realtime polling", async () => {
    vi.mocked(useObservabilityOverview).mockReturnValue({ data: undefined } as unknown as ReturnType<typeof useObservabilityOverview>);
    vi.mocked(useObservabilityErrors).mockReturnValue({
      data: {
        content: [
          {
            id: 2,
            occurredAt: "2026-04-13T00:00:00.000Z",
            level: "WARN",
            loggerName: "com.astro.Warn",
            correlationId: null,
            message: "careful",
          },
        ],
        totalElements: 1,
      },
    } as unknown as ReturnType<typeof useObservabilityErrors>);
    vi.mocked(useObservabilityWebhooks).mockReturnValue({
      data: {
        content: [
          { id: 2, receivedAt: "2026-04-13T00:00:00.000Z", source: "ABACATEPAY", outcome: "SIGNATURE_FAIL", httpStatus: 401, durationMs: null },
        ],
      },
    } as unknown as ReturnType<typeof useObservabilityWebhooks>);
    vi.mocked(useChatbotInsights).mockReturnValue({ data: undefined } as unknown as ReturnType<typeof useChatbotInsights>);
    vi.mocked(useInfrastructure).mockReturnValue({ data: undefined } as unknown as ReturnType<typeof useInfrastructure>);

    renderPage();

    expect(screen.getByText("0 min")).toBeInTheDocument();
    expect(screen.getAllByText("WARN").length).toBeGreaterThan(0);
    expect(screen.getAllByText("n/a").length).toBeGreaterThan(0);
    expect(screen.getByText("ABACATEPAY")).toBeInTheDocument();
    expect(screen.getAllByText("0 ms").length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
    expect(await screen.findByRole("button", { name: "Retomar" })).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText("correlation_id"), { target: { value: "corr-2" } });
    expect(useObservabilityErrors).toHaveBeenCalledWith(expect.objectContaining({ correlationId: "corr-2", size: 20 }), false);
  });
});
