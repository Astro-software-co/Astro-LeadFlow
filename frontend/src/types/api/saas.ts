export interface SubscriptionDTO {
  planId: string;
  planName: string;
  monthlyPriceBrl: number;
  messageLimit: number;
  marketplaceLimit: number;
  status: 'ACTIVE' | 'SUSPENDED' | 'TRIAL';
  renewsAt: string; // ISO 8601
}

export interface MarketplaceUsageDTO {
  marketplaceName: string;
  enabled: boolean;
  used: number;
  limit: number;
  remaining: number;
}

export interface UsageSummaryDTO {
  planName: string;
  globalLimit: number;
  globalUsed: number;
  globalRemaining: number;
  renewsAt: string; // ISO 8601
  marketplaces: MarketplaceUsageDTO[];
}

export interface PlanDTO {
  id: string;
  name: string;
  monthlyPriceBrl: number;
  messageLimit: number;
  marketplaceLimit: number;
}

export interface MarketplaceConfigDTO {
  id: string;
  marketplaceName: string;
  enabled: boolean;
  monthlyLimit: number;
}
