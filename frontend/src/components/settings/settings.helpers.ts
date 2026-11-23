import {
  Bell,
  CreditCard,
  Monitor,
  Moon,
  PackageCheck,
  Settings,
  Share2,
  Shield,
  Sun,
  type LucideIcon,
} from 'lucide-react';
import type { BillingCycle } from '@/types/api/saas';
import type { AccentColor, ThemeMode } from '@/types';
import type { BillingHistoryStatus, SettingsSectionId } from '@/types/settings';

export interface SettingsTabItem {
  id: SettingsSectionId;
  label: string;
  icon: LucideIcon;
}

export interface ThemeOptionItem {
  value: ThemeMode;
  label: string;
  icon: LucideIcon;
}

export interface AccentColorOptionItem {
  value: AccentColor;
  label: string;
  colorClass: string;
}

export const settingsTabs: SettingsTabItem[] = [
  { id: 'general', label: 'Geral', icon: Settings },
  { id: 'notifications', label: 'Notificacoes', icon: Bell },
  { id: 'security', label: 'Seguranca', icon: Shield },
  { id: 'payments', label: 'Pagamentos', icon: CreditCard },
  { id: 'assinatura', label: 'Assinatura', icon: PackageCheck },
  { id: 'referrals', label: 'Indicacoes', icon: Share2 },
];

export const themeOptions: ThemeOptionItem[] = [
  { value: 'light', label: 'Claro', icon: Sun },
  { value: 'dark', label: 'Escuro', icon: Moon },
  { value: 'system', label: 'Sistema', icon: Monitor },
];

export const accentColorOptions: AccentColorOptionItem[] = [
  { value: 'purple', label: 'Roxo', colorClass: 'bg-violet-500' },
  { value: 'blue', label: 'Azul', colorClass: 'bg-blue-500' },
  { value: 'green', label: 'Verde', colorClass: 'bg-emerald-500' },
  { value: 'orange', label: 'Laranja', colorClass: 'bg-orange-500' },
];

export function usageColorClass(used: number, limit: number): string {
  const pct = limit > 0 ? (used / limit) * 100 : 0;
  if (pct < 70) return 'text-emerald-600';
  if (pct < 90) return 'text-amber-600';
  return 'text-red-600';
}

export function usageProgressClass(used: number, limit: number): string {
  const pct = limit > 0 ? (used / limit) * 100 : 0;
  if (pct < 70) return '';
  if (pct < 90) return '[&>div]:bg-amber-500';
  return '[&>div]:bg-red-500';
}

export function statusBadgeClass(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'bg-emerald-100 text-emerald-700';
    case 'SUSPENDED':
      return 'bg-red-100 text-red-700';
    case 'TRIAL':
      return 'bg-amber-100 text-amber-700';
    case 'CANCELLED':
      return 'bg-slate-200 text-slate-700';
    case 'PAST_DUE':
      return 'bg-orange-100 text-orange-700';
    default:
      return 'bg-slate-100 text-slate-600';
  }
}

export function statusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'Ativo';
    case 'SUSPENDED':
      return 'Suspenso';
    case 'TRIAL':
      return 'Avaliacao';
    case 'CANCELLED':
      return 'Cancelado';
    case 'PAST_DUE':
      return 'Pagamento pendente';
    default:
      return status;
  }
}

export function billingStatusLabel(status: BillingHistoryStatus): string {
  if (status === 'PAID') return 'Pago';
  if (status === 'FAILED') return 'Falhou';
  return 'Pendente';
}

export function billingStatusClass(status: BillingHistoryStatus): string {
  if (status === 'PAID') return 'bg-kanban-completed/10 text-kanban-completed';
  if (status === 'FAILED') return 'bg-destructive/10 text-destructive';
  return 'bg-kanban-waiting/10 text-kanban-waiting';
}

export function formatBillingDate(value: string): string {
  return formatLocalDateLabel(value);
}

export function formatCurrencyBrl(value: number): string {
  return `R$ ${value.toFixed(2).replace('.', ',')}`;
}

export function billingCycleSuffix(cycle: BillingCycle): string {
  switch (cycle) {
    case 'WEEKLY':
      return '/semana';
    case 'SEMIANNUALLY':
      return '/semestre';
    case 'ANNUALLY':
      return '/ano';
    default:
      return '/mes';
  }
}

export function formatLocalDateLabel(
  value: string | null | undefined,
  options?: Intl.DateTimeFormatOptions,
): string {
  if (!value) {
    return '--';
  }

  const date = parseDateValue(value);
  if (!date) {
    return '--';
  }

  return date.toLocaleDateString('pt-BR', options);
}

function parseDateValue(value: string): Date | null {
  const dateOnlyMatch = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (dateOnlyMatch) {
    const [, year, month, day] = dateOnlyMatch;
    return new Date(Number(year), Number(month) - 1, Number(day));
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}
