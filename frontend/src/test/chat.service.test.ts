import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/lib/api";
import { chatService } from "@/services/chat.service";

vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("chatService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getHistory fetches lead chat history by lead id", async () => {
    const dto = [
      {
        id: "msg-1",
        leadId: "lead-1",
        direction: "INBOUND" as const,
        content: "Tem disponibilidade?",
        messageType: "TEXT" as const,
        status: "READ" as const,
        sentAt: "2026-04-13T00:00:00.000Z",
      },
    ];
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await chatService.getHistory("lead-1");

    expect(api.get).toHaveBeenCalledWith("/chat/lead/lead-1");
    expect(result).toEqual(dto);
  });

  it("send posts message payload to chat/send", async () => {
    const payload = {
      phoneNumber: "+5511999999999",
      message: "Olá, posso agendar uma visita?",
      leadId: "lead-1",
    };
    const dto = {
      id: "msg-2",
      leadId: "lead-1",
      direction: "OUTBOUND" as const,
      content: payload.message,
      messageType: "TEXT" as const,
      status: "SENT" as const,
      sentAt: "2026-04-13T01:00:00.000Z",
    };
    vi.mocked(api.post).mockResolvedValueOnce({ data: dto });

    const result = await chatService.send(payload);

    expect(api.post).toHaveBeenCalledWith("/chat/send", payload);
    expect(result).toEqual(dto);
  });

  it("getBotSession fetches active or latest bot session for a lead", async () => {
    const dto = {
      session: {
        id: "bot-session-1",
        leadId: "lead-1",
        leadName: "Ana Cliente",
        leadPhone: "+5511999999999",
        status: "ACTIVE" as const,
        currentIntent: "vehicle_inquiry",
        engineUsed: "LEX",
        fallbackCount: 1,
        createdAt: "2026-04-13T00:00:00.000Z",
      },
    };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await chatService.getBotSession("lead-1");

    expect(api.get).toHaveBeenCalledWith("/chat/lead/lead-1/bot-session");
    expect(result).toEqual(dto);
  });
});
