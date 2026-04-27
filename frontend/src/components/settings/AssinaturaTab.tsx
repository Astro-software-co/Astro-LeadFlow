import { startTransition, useState } from 'react';
import { PackageCheck } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { RoleGate } from '@/components/layout/RoleGate';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Skeleton } from '@/components/ui/skeleton';
import { saasService } from '@/services/saas.service';
import { usePlans, useSubscription, useUsage } from '@/hooks/api/useSaas';
import {
  billingCycleSuffix,
  formatLocalDateLabel,
  statusBadgeClass,
  statusLabel,
  usageColorClass,
  usageProgressClass,
} from './settings.helpers';

function checkoutErrorMessage(error: unknown): string {
  const responseMessage = (error as { response?: { data?: { message?: string; error?: string } } }).response?.data;
  return responseMessage?.message
    ?? responseMessage?.error
    ?? (error instanceof Error ? error.message : 'Nao foi possivel iniciar o checkout da assinatura.');
}

function AssinaturaContent() {
  const { data: subscription, isLoading: subLoading, error: subError, refetch: subRefetch } = useSubscription();
  const { data: usage, isLoading: usageLoading, error: usageError, refetch: usageRefetch } = useUsage();
  const { data: plans, isLoading: plansLoading } = usePlans();
  const [checkoutPlanId, setCheckoutPlanId] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);

  if (subLoading || usageLoading) {
    return (
      <div className="space-y-6 animate-fade-in">
        <Skeleton className="h-32 rounded-xl" />
        <Skeleton className="h-32 rounded-xl" />
        <Skeleton className="h-48 rounded-xl" />
      </div>
    );
  }

  if (subError || usageError) {
    return (
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar as configuracoes</h2>
        <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
        <Button
          className="mt-4"
          onClick={() => {
            void subRefetch();
            void usageRefetch();
          }}
        >
          Tentar novamente
        </Button>
      </div>
    );
  }

  const renewDate = formatLocalDateLabel(subscription?.renewsAt, { day: '2-digit', month: 'long', year: 'numeric' });
  const usageResetDate = formatLocalDateLabel(usage?.renewsAt, { day: '2-digit', month: 'long', year: 'numeric' });

  const handleCreateCheckout = async (planId: string) => {
    setCheckoutPlanId(planId);
    try {
      const baseUrl = `${window.location.origin}/app/configuracoes`;
      const checkout = await saasService.createSubscriptionCheckout(planId, {
        returnUrl: baseUrl,
        completionUrl: baseUrl,
      });
      window.location.assign(checkout.checkoutUrl);
    } catch (error) {
      toast.error(checkoutErrorMessage(error));
    } finally {
      setCheckoutPlanId(null);
    }
  };

  const handleCancelSubscription = async () => {
    const confirmed = window.confirm('Deseja cancelar a assinatura atual? As proximas cobrancas serao interrompidas.');
    if (!confirmed) {
      return;
    }

    setIsCancelling(true);
    try {
      await saasService.cancelSubscription();
      toast.success('Assinatura cancelada com sucesso.');
      startTransition(() => {
        void subRefetch();
        void usageRefetch();
      });
    } catch {
      toast.error('Nao foi possivel cancelar a assinatura.');
    } finally {
      setIsCancelling(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Plano Atual</h3>
        </div>
        <div className="p-6">
          <div className="flex items-center justify-between rounded-xl border border-primary/30 bg-primary/5 p-6">
            <div className="space-y-2">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-lg font-semibold text-foreground">{subscription?.planName || '—'}</span>
                {subscription?.billingCycleLabel && (
                  <Badge variant="outline" className="text-xs">
                    {subscription.billingCycleLabel}
                  </Badge>
                )}
                <Badge className={cn('text-xs', statusBadgeClass(subscription?.status || ''))}>{statusLabel(subscription?.status || '')}</Badge>
              </div>
              <p className="text-2xl font-bold text-foreground">
                R$ {(subscription?.priceBrl ?? 0).toFixed(2).replace('.', ',')}
                {subscription?.billingCycle && <span className="text-sm font-normal text-muted-foreground">{billingCycleSuffix(subscription.billingCycle)}</span>}
              </p>
              {subscription?.planDescription && <p className="text-sm text-muted-foreground">{subscription.planDescription}</p>}
              <p className="text-sm text-muted-foreground">Renova em {renewDate}</p>
            </div>
            <div className="flex flex-col gap-2">
              {subscription?.checkoutUrl ? (
                <Button variant="default" size="sm" onClick={() => window.location.assign(subscription.checkoutUrl ?? '')}>
                  Concluir pagamento
                </Button>
              ) : (
                <Button variant="outline" size="sm" disabled>
                  Plano atual
                </Button>
              )}
              {subscription?.canCancel && (
                <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" disabled={isCancelling} onClick={handleCancelSubscription}>
                  {isCancelling ? 'Cancelando...' : 'Cancelar assinatura'}
                </Button>
              )}
            </div>
          </div>
          {subscription?.status === 'SUSPENDED' && (
            <p className="mt-3 text-sm text-red-600">Sua assinatura esta suspensa. Fale com nossa equipe para reativar.</p>
          )}
          {subscription?.status === 'TRIAL' && <p className="mt-3 text-sm text-amber-600">Plano de avaliacao — expira em {renewDate}</p>}
        </div>
      </div>
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Uso de Mensagens</h3>
        </div>
        <div className="space-y-3 p-6">
          <Progress
            value={usage ? (usage.globalUsed / Math.max(1, usage.globalLimit)) * 100 : 0}
            className={cn('h-3', usageProgressClass(usage?.globalUsed ?? 0, usage?.globalLimit ?? 1))}
            aria-label={`${usage?.globalUsed ?? 0} de ${usage?.globalLimit ?? 0} mensagens usadas`}
          />
          <div className="flex items-center justify-between">
            <p className={cn('text-sm font-medium', usageColorClass(usage?.globalUsed ?? 0, usage?.globalLimit ?? 1))}>
              {usage?.globalUsed ?? 0} de {usage?.globalLimit ?? 0} mensagens usadas
            </p>
            <p className="text-xs text-muted-foreground">Redefine em {usageResetDate}</p>
          </div>
        </div>
      </div>
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Integracoes de Marketplace</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border/50 bg-muted/30">
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Marketplace</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Usadas</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Limite</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground min-w-[120px]">Uso</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {(usage?.marketplaces ?? []).map((marketplace) => (
                <tr key={marketplace.marketplaceName} className="transition-colors hover:bg-muted/20">
                  <td className="px-6 py-4 text-sm font-medium text-foreground">{marketplace.marketplaceName}</td>
                  <td className="px-6 py-4">
                    <Badge className={cn('text-xs', marketplace.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500')}>
                      {marketplace.enabled ? 'Ativo' : 'Inativo'}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 text-sm text-foreground">{marketplace.used}</td>
                  <td className="px-6 py-4 text-sm text-muted-foreground">{marketplace.limit}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <Progress value={marketplace.limit > 0 ? (marketplace.used / marketplace.limit) * 100 : 0} className={cn('h-2 flex-1', usageProgressClass(marketplace.used, marketplace.limit))} />
                      <span className={cn('min-w-[32px] text-right text-xs font-medium', usageColorClass(marketplace.used, marketplace.limit))}>
                        {marketplace.limit > 0 ? Math.round((marketplace.used / marketplace.limit) * 100) : 0}%
                      </span>
                    </div>
                  </td>
                </tr>
              ))}
              {(usage?.marketplaces ?? []).length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-sm text-muted-foreground">Nenhuma integracao de marketplace configurada.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Planos Disponiveis</h3>
        </div>
        <div className="p-6">
          {plansLoading ? (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, index) => <Skeleton key={index} className="h-48 rounded-xl" />)}
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
              {(plans ?? []).map((plan) => {
                const isCurrent = plan.id === subscription?.planId;
                return (
                  <div key={plan.id} className={cn('space-y-3 rounded-xl border p-4 transition-colors', isCurrent ? 'border-primary bg-primary/5 ring-2 ring-primary/20' : 'border-border/50 hover:border-foreground/20')}>
                    <div className="flex items-center justify-between">
                      <h4 className="text-sm font-semibold text-foreground">{plan.name}</h4>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-xs">
                          {plan.billingCycleLabel}
                        </Badge>
                        {isCurrent && <Badge className="bg-primary/10 text-xs text-primary">Atual</Badge>}
                      </div>
                    </div>
                    <p className="text-xl font-bold text-foreground">
                      {plan.priceBrl > 0 ? `R$ ${plan.priceBrl.toFixed(2).replace('.', ',')}` : 'Personalizado'}
                      {plan.priceBrl > 0 && <span className="text-xs font-normal text-muted-foreground">{billingCycleSuffix(plan.billingCycle)}</span>}
                    </p>
                    <p className="text-xs text-muted-foreground">{plan.description}</p>
                    <div className="space-y-1 text-xs text-muted-foreground">
                      <p>{plan.messageLimit.toLocaleString('pt-BR')} mensagens/mes</p>
                      <p>{plan.marketplaceLimit} marketplaces</p>
                    </div>
                    {isCurrent ? (
                      <Button variant="outline" size="sm" className="w-full" disabled>
                        Plano atual
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        size="sm"
                        className="w-full"
                        disabled={checkoutPlanId === plan.id || plan.priceBrl <= 0}
                        onClick={() => {
                          void handleCreateCheckout(plan.id);
                        }}
                      >
                        {checkoutPlanId === plan.id ? 'Abrindo checkout...' : 'Assinar com AbacatePay'}
                      </Button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function AssinaturaTab() {
  return (
    <RoleGate
      allowedRoles={['MANAGER']}
      fallback={
        <div className="flex flex-col items-center justify-center gap-3 py-16 text-center animate-fade-in">
          <PackageCheck className="h-12 w-12 text-muted-foreground/30" />
          <p className="text-sm text-muted-foreground">Apenas managers podem ver as informacoes de assinatura.</p>
        </div>
      }
    >
      <AssinaturaContent />
    </RoleGate>
  );
}
