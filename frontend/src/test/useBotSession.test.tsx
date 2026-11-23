import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { useBotSession, useForceBotHandoff } from "@/hooks/api/useBotSession";
import { chatService } from "@/services/chat.service";
import { chatbotService } from "@/services/chatbot.service";

vi.mock("@/services/chat.service", () => ({
  chatService: {
    getBotSession: vi.fn(),
  },
}));

vi.mock("@/services/chatbot.service", () => ({
  chatbotService: {
    forceHandoff: vi.fn(),
  },
}));

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({ user: { organizationId: "org-1" } }),
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

describe("useBotSession hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("fetches bot session for a selected lead", async () => {
    vi.mocked(chatService.getBotSession).mockResolvedValueOnce({ session: null });

    const { result } = renderHook(() => useBotSession("lead-1"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(chatService.getBotSession).toHaveBeenCalledWith("lead-1");
  });

  it("does not fetch without a lead id", () => {
    renderHook(() => useBotSession(undefined), { wrapper: createWrapper() });

    expect(chatService.getBotSession).not.toHaveBeenCalled();
  });

  it("forces bot handoff and shows success", async () => {
    vi.mocked(chatbotService.forceHandoff).mockResolvedValueOnce({ session: { id: "s1" }, slotValues: {}, messages: [] } as never);

    const { result } = renderHook(() => useForceBotHandoff("lead-1"), { wrapper: createWrapper() });

    await act(async () => {
      await result.current.mutateAsync("s1");
    });

    expect(chatbotService.forceHandoff).toHaveBeenCalledWith("s1");
    expect(toast.success).toHaveBeenCalledWith("Atendimento assumido com sucesso");
  });

  it("reports force handoff failures", async () => {
    vi.mocked(chatbotService.forceHandoff).mockRejectedValueOnce(new Error("failed"));

    const { result } = renderHook(() => useForceBotHandoff("lead-1"), { wrapper: createWrapper() });

    await act(async () => {
      await expect(result.current.mutateAsync("s1")).rejects.toThrow("failed");
    });

    expect(toast.error).toHaveBeenCalledWith("Nao foi possivel forcar o handoff");
  });
});
