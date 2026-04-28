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
  senderType?: 'USER' | 'BOT' | 'SELLER' | 'SYSTEM';
  botSessionId?: string | null;
  intentDetected?: string | null;
  engineUsed?: string | null;
}

export interface SendMessageRequestDTO {
  phoneNumber: string;
  message: string;
  leadId?: string;
}

export type BotSessionStatus = 'ACTIVE' | 'HANDOFF' | 'COMPLETED' | 'ABANDONED';

export interface BotSessionSummaryDTO {
  id: string;
  leadId: string;
  leadName?: string | null;
  leadPhone: string;
  status: BotSessionStatus;
  currentIntent?: string | null;
  engineUsed: string;
  fallbackCount: number;
  handoffReason?: string | null;
  lastMessageAt?: string | null;
  createdAt: string;
}

export interface BotSessionStatusDTO {
  session?: BotSessionSummaryDTO | null;
}
