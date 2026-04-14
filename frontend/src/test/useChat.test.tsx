import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useChat, useSendMessage } from "@/hooks/api/useChat";
import { chatService } from "@/services/chat.service";
import { toast } from "sonner";

vi.mock("@/services/chat.service", () => ({
  chatService: {
    getHistory: vi.fn(),
    send: vi.fn(),
  },
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
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

describe("useChat hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useChat fetches chat history for the selected lead", async () => {
    vi.mocked(chatService.getHistory).mockResolvedValueOnce([
      {
        id: "msg-1",
        leadId: "lead-1",
        direction: "INBOUND",
        content: "Tem disponível?",
        messageType: "TEXT",
        status: "READ",
        sentAt: "2026-04-13T00:00:00.000Z",
      },
    ]);

    const { result } = renderHook(() => useChat("lead-1"), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(chatService.getHistory).toHaveBeenCalledWith("lead-1");
    expect(result.current.data?.[0]?.content).toContain("dispon");
  });

  it("useSendMessage posts payload and shows success toast", async () => {
    vi.mocked(chatService.send).mockResolvedValueOnce({
      id: "msg-2",
      leadId: "lead-1",
      direction: "OUTBOUND",
      content: "Podemos agendar para amanhã",
      messageType: "TEXT",
      status: "SENT",
      sentAt: "2026-04-13T00:30:00.000Z",
    });

    const { result } = renderHook(() => useSendMessage("lead-1"), { wrapper: createWrapper() });

    await act(async () => {
      await result.current.mutateAsync({
        phoneNumber: "+5511999999999",
        message: "Podemos agendar para amanhã",
        leadId: "lead-1",
      });
    });

    expect(chatService.send).toHaveBeenCalledWith({
      phoneNumber: "+5511999999999",
      message: "Podemos agendar para amanhã",
      leadId: "lead-1",
    });
    expect(toast.success).toHaveBeenCalledWith("Mensagem enviada com sucesso");
  });

  it("useSendMessage shows error toast when API call fails", async () => {
    vi.mocked(chatService.send).mockRejectedValueOnce(new Error("failed-send"));

    const { result } = renderHook(() => useSendMessage("lead-1"), { wrapper: createWrapper() });

    await act(async () => {
      await expect(
        result.current.mutateAsync({
          phoneNumber: "+5511999999999",
          message: "mensagem",
          leadId: "lead-1",
        }),
      ).rejects.toThrow("failed-send");
    });

    expect(toast.error).toHaveBeenCalledWith("Falha ao enviar mensagem. Verifique o numero e tente novamente.");
  });
});

describe("useChat hooks (mock-mode branches)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it("returns in-memory conversation for given lead when VITE_USE_MOCKS=true", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const { useChat: useChatMock } = await import("@/hooks/api/useChat");

    const { result } = renderHook(() => useChatMock("lead-x"), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(chatService.getHistory).not.toHaveBeenCalled();
    expect(result.current.data?.every((item) => item.leadId === "lead-x")).toBe(true);
  });
});
