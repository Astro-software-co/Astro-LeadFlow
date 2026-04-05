export type PipelineStageDTO =
  | 'TALKING_TO_BOT'
  | 'WAITING_ATTENDANCE'
  | 'IN_ATTENDANCE'
  | 'IN_NEGOTIATION'
  | 'COMPLETED'
  | 'ARCHIVED';

export interface SellerSummaryDTO {
  id: string;
  name: string;
  avatarUrl?: string | null;
}

export interface VehicleSummaryDTO {
  id: string;
  brand: string;
  model: string;
  year: number;
  salePrice: number;
}

export interface LeadResponseDTO {
  id: string;
  name?: string | null;
  phoneNumber: string;
  email?: string | null;
  origin: string;
  vehicleInterest?: string | null;
  budget?: number | null;
  urgency?: string | null;
  purchaseForecast?: string | null;
  interestType?: string | null;
  notes?: string | null;
  pipelineStage: PipelineStageDTO;
  assignedTo?: SellerSummaryDTO | null;
  vehicle?: VehicleSummaryDTO | null;
  isDuplicate: boolean;
  organizationId: string;
  createdAt: string;
  updatedAt: string;
}

export interface LeadCreateRequestDTO {
  name?: string;
  phoneNumber: string;
  email?: string;
  origin: string;
  vehicleInterest?: string;
  budget?: number;
  urgency?: string;
  purchaseForecast?: string;
  interestType?: string;
  notes?: string;
  vehicleId?: string;
}

export interface LeadUpdateRequestDTO {
  name?: string;
  email?: string;
  vehicleInterest?: string;
  budget?: number;
  urgency?: string;
  purchaseForecast?: string;
  interestType?: string;
  notes?: string;
  vehicleId?: string;
}

export interface MoveLeadRequestDTO {
  toStage: PipelineStageDTO;
}
