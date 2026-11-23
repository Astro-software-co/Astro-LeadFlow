import api from '@/lib/api';
import { BotSessionSummaryDTO, BotSessionStatus } from '@/types/api/chat';

export interface BotSessionPageDTO {
  content: BotSessionSummaryDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TopIntentDTO {
  intent: string;
  count: number;
}

export interface ChatbotMetricsDTO {
  totalSessions: number;
  fallbackRatePercent: number;
  handoffRatePercent: number;
  tokensConsumed: number;
  topIntents: TopIntentDTO[];
}

export interface BotSessionDetailDTO {
  session: BotSessionSummaryDTO;
  slotValues: Record<string, string>;
  messages: unknown[];
}

export const chatbotService = {
  async getSessions(status?: BotSessionStatus, page = 0): Promise<BotSessionPageDTO> {
    const response = await api.get<BotSessionPageDTO>('/chatbot/sessions', { params: { status, page } });
    return response.data;
  },

  async getMetrics(): Promise<ChatbotMetricsDTO> {
    const response = await api.get<ChatbotMetricsDTO>('/chatbot/metrics');
    return response.data;
  },

  async getSession(id: string): Promise<BotSessionDetailDTO> {
    const response = await api.get<BotSessionDetailDTO>(`/chatbot/sessions/${id}`);
    return response.data;
  },

  async forceHandoff(id: string): Promise<BotSessionDetailDTO> {
    const response = await api.post<BotSessionDetailDTO>(`/chatbot/sessions/${id}/handoff`);
    return response.data;
  },
};
