import { LucideIcon, TrendingUp, TrendingDown } from 'lucide-react';
import { cn } from '@/lib/utils';

interface KPICardProps {
  title: string;
  value: string;
  trend: number;
  icon: LucideIcon;
  iconBgColor?: string;
}

export function KPICard({ title, value, trend, icon: Icon, iconBgColor = 'bg-primary/10' }: KPICardProps) {
  const isPositive = trend >= 0;
  const trendText = `${isPositive ? '+' : ''}${trend}%`;

  return (
    <div className="rounded-xl border bg-card p-6 shadow-sm animate-fade-in">
      <div className="flex items-start justify-between">
        <div className="space-y-3">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <p className="text-3xl font-bold text-foreground">{value}</p>
          <div className="flex items-center gap-1.5">
            {isPositive ? (
              <TrendingUp className="h-4 w-4 text-trend-positive" />
            ) : (
              <TrendingDown className="h-4 w-4 text-trend-negative" />
            )}
            <span
              className={cn(
                'text-sm font-medium',
                isPositive ? 'text-trend-positive' : 'text-trend-negative'
              )}
            >
              {trendText}
            </span>
            <span className="text-sm text-muted-foreground">vs mês anterior</span>
          </div>
        </div>
        <div className={cn('flex h-12 w-12 items-center justify-center rounded-xl', iconBgColor)}>
          <Icon className="h-6 w-6 text-primary" />
        </div>
      </div>
    </div>
  );
}
