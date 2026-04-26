export type OnboardingStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';

export interface OnboardingApplicationDTO {
  id: string;
  companyName: string;
  ownerName: string;
  ownerEmail: string;
  createdAt: string;
  submittedAt?: string;
  status: OnboardingStatus;
  planCode: string;
  referralCode: string | null;
  // Extended fields (may be present)
  cnpj?: string;
  tradeName?: string;
  ownerPhone?: string;
  ownerCpf?: string;
  ownerRole?: string;
  dealershipSegment?: string;
  branchCount?: number;
  employeeCount?: number;
  stockCount?: number;
  monthlyLeadsAvg?: number;
  marketYears?: number;
  website?: string;
  cep?: string;
  street?: string;
  number?: string;
  complement?: string;
  neighborhood?: string;
  city?: string;
  state?: string;
  intendedMarketplaces?: string[];
  otherMarketplaces?: string;
  rejectionReason?: string;
  approvedOrganizationId?: string;
  approvedAt?: string;
  rejectedAt?: string;
  updatedAt?: string;
}

export interface ApproveOnboardingResponseDTO {
  organizationId: string;
  organizationName: string;
  ownerEmail: string;
  referralCode: string;
}

export interface ListOnboardingResponseDTO {
  content: OnboardingApplicationDTO[];
  totalElements: number;
  number?: number;
  page?: number;
  size: number;
}
