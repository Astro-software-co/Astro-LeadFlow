import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/lib/api";
import { chatbotService } from "@/services/chatbot.service";

vi.mock("@/lib/api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe("chatbotService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getSessions requests paginated manager sessions with filters", async () => {
    const dto = { content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    const result = await chatbotService.getSessions("ACTIVE", 2);

    expect(api.get).toHaveBeenCalledWith("/chatbot/sessions", { params: { status: "ACTIVE", page: 2 } });
    expect(result).toEqual(dto);
  });

  it("getMetrics fetches manager KPIs", async () => {
    const dto = { totalSessions: 10, fallbackRatePercent: 12.5, handoffRatePercent: 25, tokensConsumed: 1000, topIntents: [] };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    await expect(chatbotService.getMetrics()).resolves.toEqual(dto);

    expect(api.get).toHaveBeenCalledWith("/chatbot/metrics");
  });

  it("getSession fetches a session detail by id", async () => {
    const dto = { session: { id: "s1" }, slotValues: {}, messages: [] };
    vi.mocked(api.get).mockResolvedValueOnce({ data: dto });

    await expect(chatbotService.getSession("s1")).resolves.toEqual(dto);

    expect(api.get).toHaveBeenCalledWith("/chatbot/sessions/s1");
  });

  it("forceHandoff posts to the handoff endpoint", async () => {
    const dto = { session: { id: "s1", status: "HANDOFF" }, slotValues: {}, messages: [] };
    vi.mocked(api.post).mockResolvedValueOnce({ data: dto });

    await expect(chatbotService.forceHandoff("s1")).resolves.toEqual(dto);

    expect(api.post).toHaveBeenCalledWith("/chatbot/sessions/s1/handoff");
  });
});
