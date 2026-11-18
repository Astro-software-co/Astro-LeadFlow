import api from '@/lib/api';
import { ChatMessageDTO, SendMessageRequestDTO } from '@/types/api/chat';

export const chatService = {
  async getHistory(leadId: string): Promise<ChatMessageDTO[]> {
    const response = await api.get<ChatMessageDTO[]>(`/chat/lead/${leadId}`);
    return response.data;
  },

  async send(payload: SendMessageRequestDTO): Promise<ChatMessageDTO> {
    const response = await api.post<ChatMessageDTO>('/chat/send', payload);
    return response.data;
  },
};
