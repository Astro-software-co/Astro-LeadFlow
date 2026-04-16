import type { AccentColor, ThemeMode } from '@/types';

export type SettingsSectionId = 'general' | 'notifications' | 'security' | 'payments' | 'assinatura';

export interface SettingsProfile {
  id: string;
  name: string;
  email: string;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  role: string;
  organizationId: string;
  organizationName?: string | null;
}

export interface SettingsTeamMember {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string | null;
  role: string;
}

export type BillingHistoryStatus = 'PAID' | 'PENDING' | 'FAILED';

export interface BillingHistoryEntry {
  id: string;
  billedAt: string;
  description: string;
  amountBrl: number;
  status: BillingHistoryStatus;
  invoiceUrl?: string | null;
}

export interface NotificationPreferences {
  newLead: boolean;
  leadWaiting: boolean;
  saleCompleted: boolean;
  cardMoved: boolean;
  email: boolean;
  push: boolean;
}

export interface SecuritySettings {
  emailMfa: boolean;
  smsMfa: boolean;
}

export interface ThemeOption {
  value: ThemeMode;
  label: string;
}

export interface AccentColorOption {
  value: AccentColor;
  label: string;
  colorClass: string;
}
