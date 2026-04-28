import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { chatService } from '@/services/chat.service';
import { chatbotService } from '@/services/chatbot.service';
import { useAuth } from '@/contexts/AuthContext';
import { toast } from 'sonner';

export function useBotSession(leadId?: string) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'bot-session', leadId],
    enabled: Boolean(leadId),
    staleTime: 5_000,
    queryFn: () => chatService.getBotSession(leadId!),
  });
}

export function useForceBotHandoff(leadId?: string) {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useMutation({
    mutationFn: (sessionId: string) => chatbotService.forceHandoff(sessionId),
    onSuccess: () => {
      toast.success('Atendimento assumido com sucesso');
      queryClient.invalidateQueries({ queryKey: [orgId, 'bot-session', leadId] });
      queryClient.invalidateQueries({ queryKey: [orgId, 'chatbot-sessions'] });
    },
    onError: () => toast.error('Nao foi possivel forcar o handoff'),
  });
}
