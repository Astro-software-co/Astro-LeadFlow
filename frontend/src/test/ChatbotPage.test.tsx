import { fireEvent, render, screen } from "@testing-library/react";
import { HelmetProvider } from "react-helmet-async";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Chatbot from "@/pages/Chatbot";
import { useChatbotHandoff, useChatbotMetrics, useChatbotSessions } from "@/hooks/api/useChatbot";

vi.mock("recharts", () => {
  const Chart = ({ children }: { children?: ReactNode }) => <div data-testid="chart">{children}</div>;
  return {
    ResponsiveContainer: Chart,
    BarChart: Chart,
    Bar: () => <div />,
    CartesianGrid: () => <div />,
    XAxis: () => <div />,
    YAxis: () => <div />,
    Tooltip: () => <div />,
  };
});

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

vi.mock("@/hooks/api/useChatbot", () => ({
  useChatbotSessions: vi.fn(),
  useChatbotMetrics: vi.fn(),
  useChatbotHandoff: vi.fn(),
}));

function renderPage() {
  return render(
    <HelmetProvider>
      <Chatbot />
    </HelmetProvider>,
  );
}

describe("Chatbot page", () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useChatbotSessions).mockReturnValue({
      isLoading: false,
      data: {
        content: [
          {
            id: "s1",
            leadId: "lead-1",
            leadName: "Ana Cliente",
            leadPhone: "+5511999999999",
            status: "ACTIVE",
            currentIntent: "vehicle_inquiry",
            engineUsed: "LEX",
            fallbackCount: 1,
            createdAt: "2026-04-13T00:00:00.000Z",
          },
        ],
      },
    } as unknown as ReturnType<typeof useChatbotSessions>);
    vi.mocked(useChatbotMetrics).mockReturnValue({
      data: {
        totalSessions: 12,
        fallbackRatePercent: 8.5,
        handoffRatePercent: 15,
        tokensConsumed: 300,
        topIntents: [{ intent: "greeting", count: 5 }],
      },
    } as unknown as ReturnType<typeof useChatbotMetrics>);
    vi.mocked(useChatbotHandoff).mockReturnValue({ mutate, isPending: false } as unknown as ReturnType<typeof useChatbotHandoff>);
  });

  it("renders sessions and triggers manual handoff", () => {
    renderPage();

    expect(screen.getByText("Chatbot")).toBeInTheDocument();
    expect(screen.getByText("Ana Cliente")).toBeInTheDocument();
    expect(screen.getByText("vehicle_inquiry")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Handoff" }));

    expect(mutate).toHaveBeenCalledWith("s1");
  });

  it("renders metrics and readonly configuration tabs", async () => {
    renderPage();

    expect((await screen.findAllByText("Sessoes")).length).toBeGreaterThan(0);
    expect(screen.getByText("300")).toBeInTheDocument();
    expect(screen.getAllByTestId("chart").length).toBeGreaterThan(0);

    expect(await screen.findByText("pt_BR")).toBeInTheDocument();
    expect(screen.getByText("Lex FallbackIntent -> Bedrock")).toBeInTheDocument();
  });

  it("renders loading and zero-value metric fallbacks", () => {
    vi.mocked(useChatbotSessions).mockReturnValue({
      isLoading: true,
      data: undefined,
    } as unknown as ReturnType<typeof useChatbotSessions>);
    vi.mocked(useChatbotMetrics).mockReturnValue({ data: undefined } as unknown as ReturnType<typeof useChatbotMetrics>);

    renderPage();

    expect(document.querySelector(".animate-spin")).toBeTruthy();
    expect(screen.getAllByText("0").length).toBeGreaterThan(0);
    expect(screen.getAllByText("0.0%").length).toBeGreaterThan(0);
  });

  it("renders missing session fields and disables handoff when unavailable", () => {
    vi.mocked(useChatbotSessions).mockReturnValue({
      isLoading: false,
      data: {
        content: [
          {
            id: "s2",
            leadId: "lead-2",
            leadName: null,
            leadPhone: "+5511888888888",
            status: "HANDOFF",
            currentIntent: null,
            engineUsed: "LEX",
            fallbackCount: 3,
            createdAt: "2026-04-13T00:00:00.000Z",
          },
          {
            id: "s3",
            leadId: "lead-3",
            leadName: null,
            leadPhone: "+5511777777777",
            status: "ACTIVE",
            currentIntent: "fallback",
            engineUsed: "LEX",
            fallbackCount: 2,
            createdAt: "2026-04-13T00:00:00.000Z",
          },
        ],
      },
    } as unknown as ReturnType<typeof useChatbotSessions>);
    vi.mocked(useChatbotHandoff).mockReturnValue({ mutate, isPending: true } as unknown as ReturnType<typeof useChatbotHandoff>);

    renderPage();

    expect(screen.getAllByText("Sem nome").length).toBe(2);
    expect(screen.getByText("n/a")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Handoff" }).every((button) => button.hasAttribute("disabled"))).toBe(true);
  });
});
