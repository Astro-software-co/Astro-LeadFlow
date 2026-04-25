export type MarketplaceTypeDTO = 'WEBMOTORS' | 'MERCADO_LIVRE' | 'OLX';

export interface SubmitOnboardingRequestDTO {
  // Step 1 — Responsável
  ownerName: string;
  ownerEmail: string;
  ownerCpf: string;
  ownerPhone: string;
  ownerRole?: string;
  // Step 2 — Concessionária
  companyName: string;
  cnpj: string;
  tradeName?: string;
  dealershipSegment?: string;
  branchCount?: number;
  employeeCount?: number;
  stockCount?: number;
  monthlyLeadsAvg?: number;
  marketYears?: number;
  website?: string;
  // Step 3 — Endereço
  cep: string;
  street: string;
  number?: string;
  complement?: string;
  neighborhood: string;
  city: string;
  state: string;
  // Step 4 — Integrações
  intendedMarketplaces?: MarketplaceTypeDTO[];
  otherMarketplaces?: string;
  // Step 5 — Revisão (referral attribution from ?ref= or manual entry)
  referralCode?: string;
}

export interface SubmitOnboardingResponseDTO {
  id: string;
  status: 'PENDING_APPROVAL' | 'PENDING' | 'APPROVED' | 'REJECTED';
  message?: string;
}
