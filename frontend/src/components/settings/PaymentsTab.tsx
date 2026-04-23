import { CreditCard, Download } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import type { SubscriptionDTO } from '@/types/api/saas';
import type { BillingHistoryEntry, SettingsLoadState } from '@/types/settings';
import {
  billingStatusClass,
  billingStatusLabel,
  formatBillingDate,
  formatCurrencyBrl,
  statusBadgeClass,
  statusLabel,
} from './settings.helpers';

interface PaymentsTabProps {
  history: BillingHistoryEntry[];
  historyState: SettingsLoadState;
  onRetryHistory: () => void;
  subscription: SubscriptionDTO | null;
  subscriptionState: SettingsLoadState;
  onRetrySubscription: () => void;
}

function renderSubscriptionCard(
  subscription: SubscriptionDTO | null,
  subscriptionState: SettingsLoadState,
  onRetrySubscription: () => void,
) {
  if (subscriptionState === 'loading') {
    return (
      <div className="space-y-3 p-6">
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-8 w-32" />
        <Skeleton className="h-4 w-52" />
      </div>
    );
  }

  if (subscriptionState === 'error') {
    return (
      <div className="p-6">
        <p className="mb-3 text-sm text-muted-foreground">Nao foi possivel carregar os dados da assinatura.</p>
        <Button variant="outline" size="sm" onClick={onRetrySubscription}>
          Tentar novamente
        </Button>
      </div>
    );
  }

  if (!subscription) {
    return (
      <div className="p-6">
        <p className="text-sm text-muted-foreground">Nenhum plano ativo encontrado para esta organizacao.</p>
      </div>
    );
  }

  const renewDate = subscription.renewsAt
    ? new Date(subscription.renewsAt).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })
    : '--';

  return (
    <div className="p-6">
      <div className="flex items-center justify-between rounded-xl border border-primary/30 bg-primary/5 p-6">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <span className="inline-flex items-center rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
              {subscription.planName}
            </span>
            <span className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium', statusBadgeClass(subscription.status))}>
              {statusLabel(subscription.status)}
            </span>
          </div>
          <p className="text-2xl font-bold text-foreground">
            {formatCurrencyBrl(subscription.monthlyPriceBrl)}
            <span className="text-sm font-normal text-muted-foreground">/mes</span>
          </p>
          <p className="mt-1 text-sm text-muted-foreground">Renovacao em {renewDate}</p>
        </div>
        <div className="space-y-2">
          <Button variant="outline" size="sm" disabled>
            Alterar plano
          </Button>
          <br />
          <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" disabled>
            Cancelar
          </Button>
        </div>
      </div>
    </div>
  );
}

export function PaymentsTab({
  history,
  historyState,
  onRetryHistory,
  subscription,
  subscriptionState,
  onRetrySubscription,
}: PaymentsTabProps) {
  return (
    <div className="space-y-6 animate-fade-in">
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Plano Atual</h3>
          <p className="text-sm text-muted-foreground">Gerencie sua assinatura e licenca de uso</p>
        </div>
        {renderSubscriptionCard(subscription, subscriptionState, onRetrySubscription)}
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Historico de Cobrancas</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border/50 bg-muted/30">
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Data</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Descricao</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Valor</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">NF</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {historyState === 'loading' && (
                <tr>
                  <td colSpan={5} className="space-y-2 px-6 py-4">
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-full" />
                  </td>
                </tr>
              )}
              {historyState === 'error' && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center">
                    <p className="mb-3 text-sm text-muted-foreground">Nao foi possivel carregar o historico.</p>
                    <Button variant="outline" size="sm" onClick={onRetryHistory}>
                      Tentar novamente
                    </Button>
                  </td>
                </tr>
              )}
              {historyState === 'ready' &&
                history.map((entry) => (
                  <tr key={entry.id} className="transition-colors hover:bg-muted/20">
                    <td className="px-6 py-4 text-sm text-foreground">{formatBillingDate(entry.billedAt)}</td>
                    <td className="px-6 py-4 text-sm text-muted-foreground">{entry.description}</td>
                    <td className="px-6 py-4 text-sm font-medium text-foreground">{formatCurrencyBrl(entry.amountBrl)}</td>
                    <td className="px-6 py-4">
                      <span className={cn('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium', billingStatusClass(entry.status))}>
                        {billingStatusLabel(entry.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <button
                        className="text-primary transition-colors hover:text-primary/80"
                        onClick={() => {
                          if (!entry.invoiceUrl) {
                            toast.info('Nota fiscal indisponivel no momento.');
                            return;
                          }
                          window.open(entry.invoiceUrl, '_blank', 'noopener,noreferrer');
                        }}
                      >
                        <Download className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              {historyState === 'ready' && history.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-sm text-muted-foreground">
                    Nenhuma cobranca encontrada.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Metodo de Pagamento</h3>
        </div>
        <div className="p-6">
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <CreditCard className="h-5 w-5 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium text-foreground">Cartao cadastrado</p>
                <p className="text-xs text-muted-foreground">Gerencie o metodo de pagamento com o suporte financeiro.</p>
              </div>
            </div>
            <Button variant="outline" size="sm" disabled>
              Alterar
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
