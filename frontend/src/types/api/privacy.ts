export type DataSubjectRequestType = 'ANONYMIZATION' | 'ACCESS' | 'CORRECTION' | 'DELETION';
export type DataSubjectRequestStatus = 'RECEIVED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

/** Espelha DataSubjectRequestResponse do backend (o identificador nunca volta, só a dica mascarada). */
export interface DataSubjectRequestDTO {
  id: string;
  type: DataSubjectRequestType;
  status: DataSubjectRequestStatus;
  subjectHint: string | null;
  channel: string | null;
  receivedAt: string;
  dueAt: string;
  completedAt: string | null;
  overdue: boolean;
  resultSummary: string | null;
}

export interface CreateDataSubjectRequestDTO {
  type: DataSubjectRequestType;
  identifier: string;
  channel?: string;
  receivedAt?: string;
}

export interface ExecuteDataSubjectRequestDTO {
  identifier: string;
  resultSummary?: string;
}

export interface DeleteLeadOptions {
  includeSamePhone?: boolean;
  reason?: string;
}
