import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { useChatbotHandoff, useChatbotMetrics, useChatbotSessions } from "@/hooks/api/useChatbot";
import { chatbotService } from "@/services/chatbot.service";

vi.mock("@/services/chatbot.service", () => ({
  chatbotService: {
    getSessions: vi.fn(),
    getMetrics: vi.fn(),
    forceHandoff: vi.fn(),
  },
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
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

describe("useChatbot hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches sessions with status and page", async () => {
    vi.mocked(chatbotService.getSessions).mockResolvedValueOnce({ content: [], page: 1, size: 20, totalElements: 0, totalPages: 0 });

    const { result } = renderHook(() => useChatbotSessions("ACTIVE", 1), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(chatbotService.getSessions).toHaveBeenCalledWith("ACTIVE", 1);
  });

  it("fetches chatbot metrics", async () => {
    vi.mocked(chatbotService.getMetrics).mockResolvedValueOnce({
      totalSessions: 1,
      fallbackRatePercent: 0,
      handoffRatePercent: 0,
      tokensConsumed: 0,
      topIntents: [],
    });

    const { result } = renderHook(() => useChatbotMetrics(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.totalSessions).toBe(1);
  });

  it("forces handoff and reports success", async () => {
    vi.mocked(chatbotService.forceHandoff).mockResolvedValueOnce({ session: { id: "s1" }, slotValues: {}, messages: [] } as never);

    const { result } = renderHook(() => useChatbotHandoff(), { wrapper: createWrapper() });

    await act(async () => {
      await result.current.mutateAsync("s1");
    });

    expect(chatbotService.forceHandoff).toHaveBeenCalledWith("s1", expect.any(Object));
    expect(toast.success).toHaveBeenCalledWith("Handoff aplicado");
  });

  it("reports handoff errors", async () => {
    vi.mocked(chatbotService.forceHandoff).mockRejectedValueOnce(new Error("handoff failed"));

    const { result } = renderHook(() => useChatbotHandoff(), { wrapper: createWrapper() });

    await act(async () => {
      await expect(result.current.mutateAsync("s1")).rejects.toThrow("handoff failed");
    });

    expect(toast.error).toHaveBeenCalledWith("Nao foi possivel aplicar handoff");
  });
});
