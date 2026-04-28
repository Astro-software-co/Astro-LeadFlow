import { useQuery } from '@tanstack/react-query';
import { adminObservabilityService } from '@/services/admin-observability.service';

export function useObservabilityOverview() {
  return useQuery({
    queryKey: ['admin-observability-overview'],
    queryFn: adminObservabilityService.overview,
    refetchInterval: 30_000,
  });
}

export function useObservabilityErrors(params?: Record<string, unknown>, realtime = false) {
  return useQuery({
    queryKey: ['admin-observability-errors', params],
    queryFn: () => adminObservabilityService.errors(params),
    refetchInterval: realtime ? 10_000 : false,
  });
}

export function useObservabilityWebhooks(params?: Record<string, unknown>) {
  return useQuery({
    queryKey: ['admin-observability-webhooks', params],
    queryFn: () => adminObservabilityService.webhooks(params),
    refetchInterval: 30_000,
  });
}

export function useChatbotInsights() {
  return useQuery({
    queryKey: ['admin-chatbot-insights'],
    queryFn: adminObservabilityService.chatbotInsights,
    refetchInterval: 30_000,
  });
}

export function useInfrastructure() {
  return useQuery({
    queryKey: ['admin-infrastructure'],
    queryFn: adminObservabilityService.infrastructure,
    refetchInterval: 30_000,
  });
}
