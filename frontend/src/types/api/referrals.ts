export type ReferralStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ReferredCompanyDTO {
  organizationId: string;
  companyName: string;
  status: ReferralStatus | string;
  planCode?: string | null;
  commissionBrl?: number | null;
  approvedAt?: string | null;
}

export interface ReferralSummaryDTO {
  code: string;
  referralLink: string;
  approvedCount: number;
  pendingCount: number;
  currentTier: string;
  commissionRate: number;
  nextTierThreshold: number;
  balanceBrl: number;
  referrals: ReferredCompanyDTO[];
}

export interface ReferralBalanceDTO {
  balanceBrl: number;
  pendingBrl: number;
  withdrawalAvailable: boolean;
}

export interface ReferralSettingsDTO {
  code: string;
  referralLink: string;
  approvedCount: number;
  pendingCount: number;
  currentTier: string;
  commissionRate: number;
  nextTierThreshold: number;
  balanceBrl: number;
  pendingBrl: number;
  withdrawalAvailable: boolean;
  referrals: ReferredCompanyDTO[];
}
