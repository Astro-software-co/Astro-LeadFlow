export interface SubscriptionDTO {
  planId: string;
  planName: string;
  monthlyPriceBrl: number;
  messageLimit: number;
  marketplaceLimit: number;
  status: 'ACTIVE' | 'SUSPENDED' | 'TRIAL' | 'CANCELLED' | 'PAST_DUE';
  renewsAt: string | null; // ISO 8601
  paymentProvider?: 'ABACATEPAY' | string | null;
  checkoutUrl?: string | null;
  canCancel?: boolean;
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

export interface CreateSubscriptionCheckoutDTO {
  returnUrl?: string;
  completionUrl?: string;
}

export interface SubscriptionCheckoutDTO {
  planId: string;
  planName: string;
  checkoutId: string;
  checkoutUrl: string;
}
