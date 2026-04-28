import { fireEvent, render, screen } from "@testing-library/react";
import { HelmetProvider } from "react-helmet-async";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Comunicacao from "@/pages/Comunicacao";
import { useLeads } from "@/modules/crm";
import { useBotSession, useChat, useForceBotHandoff, useSendMessage } from "@/modules/communication";

vi.mock("@/modules/crm", () => ({
  useLeads: vi.fn(),
}));

vi.mock("@/modules/communication", () => ({
  useBotSession: vi.fn(),
  useChat: vi.fn(),
  useForceBotHandoff: vi.fn(),
  useSendMessage: vi.fn(),
}));

vi.mock("@/components/ui/tooltip", () => ({
  Tooltip: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  TooltipTrigger: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  TooltipContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

function renderPage() {
  return render(
    <HelmetProvider>
      <Comunicacao />
    </HelmetProvider>,
  );
}

describe("Comunicacao page chatbot integration", () => {
  const sendMutate = vi.fn();
  const handoffMutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useLeads).mockReturnValue({
      isLoading: false,
      data: {
        content: [
          {
            id: "lead-1",
            nome: "Ana Cliente",
            telefone: "+5511999999999",
            origem: "Meta",
          },
        ],
      },
    } as unknown as ReturnType<typeof useLeads>);
    vi.mocked(useChat).mockReturnValue({
      isLoading: false,
      error: null,
      data: [
        {
          id: "msg-1",
          leadId: "lead-1",
          direction: "OUTBOUND",
          content: "Ola, eu sou o bot.",
          messageType: "TEXT",
          status: "SENT",
          sentAt: "2026-04-13T10:00:00.000Z",
          senderType: "BOT",
          intentDetected: "greeting",
          engineUsed: "LEX",
        },
      ],
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useChat>);
    vi.mocked(useBotSession).mockReturnValue({
      data: {
        session: {
          id: "session-1",
          leadId: "lead-1",
          leadName: "Ana Cliente",
          leadPhone: "+5511999999999",
          status: "ACTIVE",
          currentIntent: "greeting",
          engineUsed: "LEX",
          fallbackCount: 2,
          createdAt: "2026-04-13T10:00:00.000Z",
        },
      },
    } as unknown as ReturnType<typeof useBotSession>);
    vi.mocked(useForceBotHandoff).mockReturnValue({ mutate: handoffMutate, isPending: false } as unknown as ReturnType<typeof useForceBotHandoff>);
    vi.mocked(useSendMessage).mockReturnValue({ mutate: sendMutate, isPending: false } as unknown as ReturnType<typeof useSendMessage>);
  });

  it("shows bot badge, active banner, and disables manual input while bot is active", () => {
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Ana Cliente/i }));

    expect(screen.getByText("🤖 Bot")).toBeInTheDocument();
    expect(screen.getByText(/Bot esta atendendo este lead/i)).toBeInTheDocument();
    expect(screen.getByText("Intent: greeting")).toBeInTheDocument();
    expect(screen.getByText("Engine: LEX")).toBeInTheDocument();

    const input = screen.getByPlaceholderText("Bot ativo — assuma o atendimento para responder manualmente");
    expect(input).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: /Forcar handoff/i }));
    expect(handoffMutate).toHaveBeenCalledWith("session-1");
  });

  it("enables sending when bot session is not active", () => {
    vi.mocked(useBotSession).mockReturnValue({ data: { session: null } } as unknown as ReturnType<typeof useBotSession>);
    renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Ana Cliente/i }));
    fireEvent.change(screen.getByPlaceholderText("Digite uma mensagem"), { target: { value: "Oi" } });
    fireEvent.click(screen.getByLabelText("Enviar mensagem"));

    expect(sendMutate).toHaveBeenCalledWith(
      {
        leadId: "lead-1",
        phoneNumber: "+5511999999999",
        message: "Oi",
      },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
  });
});
