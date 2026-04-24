export interface SubmitOnboardingRequestDTO {
  // Step 1 — Dados da empresa
  companyName: string;
  cnpj: string;
  // Step 2 — Responsavel
  ownerName: string;
  ownerEmail: string;
  ownerCpf: string;
  ownerPhone: string;
  // Step 3 — Endereco
  cep: string;
  street: string;
  neighborhood: string;
  city: string;
  state: string;
  // Referral attribution (optional — comes from ?ref= query param)
  referralCode?: string;
}

export interface SubmitOnboardingResponseDTO {
  id: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  message?: string;
}
