import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { chatbotService } from '@/services/chatbot.service';
import { BotSessionStatus } from '@/types/api/chat';
import { toast } from 'sonner';

export function useChatbotSessions(status?: BotSessionStatus, page = 0) {
  return useQuery({
    queryKey: ['chatbot-sessions', status ?? 'ALL', page],
    queryFn: () => chatbotService.getSessions(status, page),
    staleTime: 10_000,
  });
}

export function useChatbotMetrics() {
  return useQuery({
    queryKey: ['chatbot-metrics'],
    queryFn: chatbotService.getMetrics,
    staleTime: 30_000,
  });
}

export function useChatbotHandoff() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: chatbotService.forceHandoff,
    onSuccess: () => {
      toast.success('Handoff aplicado');
      queryClient.invalidateQueries({ queryKey: ['chatbot-sessions'] });
      queryClient.invalidateQueries({ queryKey: ['chatbot-metrics'] });
    },
    onError: () => toast.error('Nao foi possivel aplicar handoff'),
  });
}
