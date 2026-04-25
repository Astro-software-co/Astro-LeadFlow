import { useEffect, useRef, useState } from 'react';
import { Check, Copy, Link2, Share2, Trophy, WalletCards } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { useReferrals } from '@/hooks/api/useReferrals';
import type { ReferralSettingsDTO, ReferredCompanyDTO } from '@/types/api/referrals';
import { formatCurrencyBrl, formatLocalDateLabel } from './settings.helpers';

const REFERRAL_LINK_BASE = 'https://app.astroleadflow.com/onboarding?ref=';

const tierCards = [
  { key: 'BRONZE', label: 'Bronze', rate: '8%', range: '0 a 9 empresas' },
  { key: 'SILVER', label: 'Prata', rate: '12%', range: '10 a 19 empresas' },
  { key: 'GOLD', label: 'Ouro', rate: '16%', range: '20+ empresas' },
];

type CopyTarget = 'code' | 'link';

function referralStatusLabel(status: string): string {
  switch (status) {
    case 'APPROVED':
      return 'Aprovada';
    case 'REJECTED':
      return 'Rejeitada';
    case 'PENDING_APPROVAL':
    case 'PENDING':
      return 'Pendente';
    default:
      return status;
  }
}

function referralStatusClass(status: string): string {
  switch (status) {
    case 'APPROVED':
      return 'bg-kanban-completed/10 text-kanban-completed';
    case 'REJECTED':
      return 'bg-destructive/10 text-destructive';
    default:
      return 'bg-kanban-waiting/10 text-kanban-waiting';
  }
}

function formatPlanCode(planCode: string | null | undefined): string {
  if (!planCode) {
    return '--';
  }

  return planCode
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ');
}

function ReferralTable({ referrals }: { referrals: ReferredCompanyDTO[] }) {
  if (referrals.length === 0) {
    return (
      <div className="px-6 py-8 text-center text-sm text-muted-foreground">
        Nenhuma empresa indicada ainda.
      </div>
    );
  }

  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border/50 bg-muted/30">
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Empresa</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Plano</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Data de aprovacao</th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Comissao</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border/50">
            {referrals.map((referral) => (
              <tr key={referral.organizationId} className="transition-colors hover:bg-muted/20">
                <td className="px-6 py-4 text-sm font-medium text-foreground">{referral.companyName}</td>
                <td className="px-6 py-4">
                  <Badge className={cn('text-xs', referralStatusClass(referral.status))}>
                    {referralStatusLabel(referral.status)}
                  </Badge>
                </td>
                <td className="px-6 py-4 text-sm text-muted-foreground">{formatPlanCode(referral.planCode)}</td>
                <td className="px-6 py-4 text-sm text-muted-foreground">{formatLocalDateLabel(referral.approvedAt)}</td>
                <td className="px-6 py-4 text-sm font-medium text-foreground">
                  {typeof referral.commissionBrl === 'number' ? formatCurrencyBrl(referral.commissionBrl) : '--'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="space-y-3 p-4 md:hidden">
        {referrals.map((referral) => (
          <div key={referral.organizationId} className="rounded-lg border border-border/50 p-4">
            <div className="mb-3 flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-foreground">{referral.companyName}</p>
                <p className="text-xs text-muted-foreground">{formatPlanCode(referral.planCode)}</p>
              </div>
              <Badge className={cn('text-xs', referralStatusClass(referral.status))}>
                {referralStatusLabel(referral.status)}
              </Badge>
            </div>
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <p className="text-muted-foreground">Data de aprovacao</p>
                <p className="mt-1 font-medium text-foreground">{formatLocalDateLabel(referral.approvedAt)}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Comissao</p>
                <p className="mt-1 font-medium text-foreground">
                  {typeof referral.commissionBrl === 'number' ? formatCurrencyBrl(referral.commissionBrl) : '--'}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

function ReferralContent({ data }: { data: ReferralSettingsDTO }) {
  const [copiedTarget, setCopiedTarget] = useState<CopyTarget | null>(null);
  const timeoutRef = useRef<number | null>(null);
  const referralLink = data.referralLink || `${REFERRAL_LINK_BASE}${data.code}`;
  const progressValue = Math.min(100, (data.approvedCount / Math.max(1, data.nextTierThreshold)) * 100);
  const currentRateLabel = `${Math.round(data.commissionRate * 100)}%`;

  useEffect(() => () => {
    if (timeoutRef.current !== null) {
      window.clearTimeout(timeoutRef.current);
    }
  }, []);

  const handleCopy = async (target: CopyTarget, value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopiedTarget(target);
      toast.success('Copiado para a area de transferencia.');

      if (timeoutRef.current !== null) {
        window.clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = window.setTimeout(() => {
        setCopiedTarget((current) => (current === target ? null : current));
      }, 1500);
    } catch {
      toast.error('Nao foi possivel copiar. Selecione e copie manualmente.');
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[0.85fr_1.15fr]">
        <div className="rounded-xl border border-border/50 bg-card shadow-sm">
          <div className="border-b border-border/50 p-6">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <WalletCards className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-foreground">Saldo acumulado</h3>
                <p className="text-sm text-muted-foreground">Valores liberados apos aprovacao da empresa indicada.</p>
              </div>
            </div>
          </div>
          <div className="space-y-5 p-6">
            <p className="text-[30px] font-semibold leading-tight text-foreground">{formatCurrencyBrl(data.balanceBrl)}</p>
            <div className="rounded-lg border border-border/50 bg-muted/20 p-4">
              <p className="text-xs text-muted-foreground">Comissao pendente</p>
              <p className="mt-1 text-sm font-medium text-foreground">{formatCurrencyBrl(data.pendingBrl)}</p>
            </div>
            <Tooltip>
              <TooltipTrigger asChild>
                <span className="inline-flex w-full rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2" tabIndex={0}>
                  <Button className="w-full" disabled>
                    Solicitar saque
                  </Button>
                </span>
              </TooltipTrigger>
              <TooltipContent>
                <p>Em breve</p>
              </TooltipContent>
            </Tooltip>
          </div>
        </div>

        <div className="rounded-xl border border-border/50 bg-card shadow-sm">
          <div className="border-b border-border/50 p-6">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Share2 className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-foreground">Indicacoes</h3>
                <p className="text-sm text-muted-foreground">Compartilhe seu codigo para receber comissao.</p>
              </div>
            </div>
          </div>
          <div className="space-y-4 p-6">
            <div className="rounded-lg border border-border/50 bg-muted/20 p-4">
              <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">Codigo</p>
              <p className="mt-2 break-all text-lg font-semibold tracking-wide text-foreground">{data.code}</p>
            </div>
            <div className="rounded-lg border border-border/50 bg-muted/20 p-4">
              <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">Link</p>
              <p className="mt-2 break-all text-sm font-medium text-foreground">{referralLink}</p>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                variant="outline"
                className="w-full sm:w-auto"
                aria-label="Copiar codigo de indicacao"
                onClick={() => {
                  void handleCopy('code', data.code);
                }}
              >
                {copiedTarget === 'code' ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                Copiar codigo
              </Button>
              <Button
                variant="outline"
                className="w-full sm:w-auto"
                aria-label="Copiar link de indicacao"
                onClick={() => {
                  void handleCopy('link', referralLink);
                }}
              >
                {copiedTarget === 'link' ? <Check className="h-4 w-4" /> : <Link2 className="h-4 w-4" />}
                Copiar link
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="text-lg font-semibold text-foreground">Progresso para o proximo nivel</h3>
              <p className="text-sm text-muted-foreground">
                {data.approvedCount} de {data.nextTierThreshold} empresas aprovadas
              </p>
            </div>
            <Badge variant="outline" className="w-fit text-xs">
              {data.currentTier} {currentRateLabel}
            </Badge>
          </div>
        </div>
        <div className="space-y-6 p-6">
          <Progress
            value={progressValue}
            className="h-3"
            aria-label={`${data.approvedCount} de ${data.nextTierThreshold} empresas aprovadas`}
          />
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            {tierCards.map((tier) => {
              const isCurrent = data.currentTier === tier.key;
              return (
                <div
                  key={tier.key}
                  className={cn(
                    'rounded-lg border p-4 transition-colors',
                    isCurrent
                      ? 'border-primary bg-primary/5 ring-2 ring-primary/20'
                      : 'border-border/50 bg-muted/10',
                  )}
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-foreground">{tier.label} {tier.rate}</p>
                    <Trophy className={cn('h-4 w-4', isCurrent ? 'text-primary' : 'text-muted-foreground')} />
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">{tier.range}</p>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Empresas indicadas</h3>
          <p className="text-sm text-muted-foreground">Acompanhe aprovacao, plano e comissao de cada indicacao.</p>
        </div>
        <ReferralTable referrals={data.referrals} />
      </div>
    </div>
  );
}

export function ReferralTab() {
  const { data, isLoading, isError, refetch } = useReferrals();

  if (isLoading) {
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
          <Skeleton className="h-56 rounded-xl" />
          <Skeleton className="h-56 rounded-xl" />
        </div>
        <Skeleton className="h-52 rounded-xl" />
        <Skeleton className="h-72 rounded-xl" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm animate-fade-in">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar as indicacoes</h2>
        <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
        <Button
          className="mt-4"
          variant="outline"
          onClick={() => {
            void refetch();
          }}
        >
          Tentar novamente
        </Button>
      </div>
    );
  }

  return <ReferralContent data={data} />;
}
