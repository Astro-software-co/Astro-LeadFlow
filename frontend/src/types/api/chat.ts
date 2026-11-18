export type MessageDirection = 'INBOUND' | 'OUTBOUND';
export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';
export type MessageType = 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';

export interface ChatMessageDTO {
  id: string;
  leadId: string;
  direction: MessageDirection;
  content: string;
  messageType: MessageType;
  status: MessageStatus;
  sentAt: string; // ISO 8601
}

export interface SendMessageRequestDTO {
  phoneNumber: string;
  message: string;
  leadId?: string;
}
