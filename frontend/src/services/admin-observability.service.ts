import api from '@/lib/api';

export interface SystemHealthDTO {
  abacatePayConfigured: boolean;
  metaWebhookSecretConfigured: boolean;
  metaApiTokenConfigured: boolean;
  redisUp: boolean;
  dbUp: boolean;
  chatbotEngineMode: string;
  awsLexConfigured: boolean;
  awsBedrockConfigured: boolean;
  publicApiUrlConfigured: boolean;
}

export interface OverviewDTO {
  uptimeSeconds: number;
  health: SystemHealthDTO;
  requestsPerMinute: number;
  errorRatePercent: number;
  p95LatencyMs: number;
}

export interface ApplicationErrorDTO {
  id: number;
  occurredAt: string;
  level: 'WARN' | 'ERROR';
  loggerName: string;
  correlationId?: string | null;
  message: string;
  stackTrace?: string | null;
  requestPath?: string | null;
  requestMethod?: string | null;
}

export interface PageDTO<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface WebhookEventDTO {
  id: number;
  receivedAt: string;
  source: string;
  outcome: string;
  httpStatus: number;
  correlationId?: string | null;
  durationMs?: number | null;
  errorMessage?: string | null;
}

export interface ChatbotInsightsDTO {
  totalSessions: number;
  globalFallbackRatePercent: number;
  tokensConsumed: number;
  topOrganizationsByTokens: Array<{ organizationId: string; tokens: number }>;
  averageLatencyByEngineMs: Record<string, number>;
}

export const adminObservabilityService = {
  async overview(): Promise<OverviewDTO> {
    const response = await api.get<OverviewDTO>('/admin/observability/overview');
    return response.data;
  },

  async errors(params?: Record<string, unknown>): Promise<PageDTO<ApplicationErrorDTO>> {
    const response = await api.get<PageDTO<ApplicationErrorDTO>>('/admin/observability/errors', { params });
    return response.data;
  },

  async webhooks(params?: Record<string, unknown>): Promise<PageDTO<WebhookEventDTO>> {
    const response = await api.get<PageDTO<WebhookEventDTO>>('/admin/observability/webhooks', { params });
    return response.data;
  },

  async chatbotInsights(): Promise<ChatbotInsightsDTO> {
    const response = await api.get<ChatbotInsightsDTO>('/admin/observability/chatbot-insights');
    return response.data;
  },

  async infrastructure(): Promise<Record<string, unknown>> {
    const response = await api.get<Record<string, unknown>>('/admin/observability/infrastructure');
    return response.data;
  },
};
