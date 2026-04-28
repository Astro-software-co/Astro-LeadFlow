import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { chatService } from '@/services/chat.service';
import { ChatMessageDTO, SendMessageRequestDTO } from '@/types/api/chat';
import { toast } from 'sonner';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

const mockChatMessages: ChatMessageDTO[] = [
  {
    id: 'mock-msg-1',
    leadId: 'placeholder',
    direction: 'INBOUND',
    content: 'Boa tarde! Vi o anuncio do Honda Civic 2023 no Webmotors. Ainda esta disponivel?',
    messageType: 'TEXT',
    status: 'READ',
    sentAt: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 'mock-msg-2',
    leadId: 'placeholder',
    direction: 'OUTBOUND',
    content: 'Boa tarde! Sim, o Honda Civic 2023 ainda esta disponivel. Gostaria de agendar uma visita ou prefere mais informacoes?',
    messageType: 'TEXT',
    status: 'DELIVERED',
    sentAt: new Date(Date.now() - 3300000).toISOString(),
    senderType: 'BOT',
    intentDetected: 'VEHICLE_INQUIRY',
    engineUsed: 'LEX',
  },
  {
    id: 'mock-msg-3',
    leadId: 'placeholder',
    direction: 'OUTBOUND',
    content: 'Temos condicoes especiais de financiamento ate 60x. Posso te enviar uma simulacao?',
    messageType: 'TEXT',
    status: 'SENT',
    sentAt: new Date(Date.now() - 1800000).toISOString(),
  },
];

export function useChat(leadId?: string) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useQuery({
    queryKey: [orgId, 'chat-history', leadId],
    enabled: Boolean(leadId),
    staleTime: 10_000,
    queryFn: async (): Promise<ChatMessageDTO[]> => {
      if (useMocks) {
        return mockChatMessages.map((msg) => ({ ...msg, leadId: leadId ?? msg.leadId }));
      }
      return chatService.getHistory(leadId!);
    },
  });
}

export function useSendMessage(leadId?: string) {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  return useMutation({
    mutationFn: (payload: SendMessageRequestDTO) => chatService.send(payload),
    onSuccess: () => {
      toast.success('Mensagem enviada com sucesso');
      queryClient.invalidateQueries({ queryKey: [orgId, 'chat-history', leadId] });
    },
    onError: () => {
      toast.error('Falha ao enviar mensagem. Verifique o numero e tente novamente.');
    },
  });
}
