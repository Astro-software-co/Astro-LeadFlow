interface CurrencyFormatOptions {
  compact?: boolean;
  minimumFractionDigits?: number;
}

interface DateTimeFormatOptions {
  locale?: string;
}

export function formatCurrency(value: number, options: CurrencyFormatOptions = {}): string {
  const { compact = false, minimumFractionDigits = 0 } = options;

  if (compact && value >= 1000000) {
    return `R$ ${(value / 1000000).toFixed(1)}M`;
  }

  if (compact && value >= 1000) {
    return `R$ ${(value / 1000).toFixed(1)}K`;
  }

  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits,
  }).format(value);
}

export function formatTimeAgo(value: Date | string): string {
  const now = new Date();
  const date = new Date(value);
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) {
    return 'Agora';
  }

  if (diffMins < 60) {
    return `${diffMins}m atras`;
  }

  if (diffHours < 24) {
    return `${diffHours}h atras`;
  }

  return `${diffDays}d atras`;
}

export function formatDateTime(value: Date | string, options: DateTimeFormatOptions = {}): string {
  const { locale = 'pt-BR' } = options;
  return new Intl.DateTimeFormat(locale, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}
