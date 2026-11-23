import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { ReactNode } from 'react';
import { AlertCircle, Info, RefreshCw, TrendingUp } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useAdminDashboard } from '@/hooks/api/useAdminDashboard';
import type { AdminDashboardDTO, AdminSubscriptionRowDTO } from '@/types/api/admin-dashboard';

const PLAN_COLORS = ['#2563eb', '#16a34a', '#f97316', '#7c3aed'];

function money(value: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    maximumFractionDigits: 0,
  }).format(value);
}

function percent(value: number): string {
  return `${value.toFixed(1).replace('.', ',')}%`;
}

function date(value: string | null): string {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('pt-BR');
}

function MetricCard({ label, value, description }: { label: string; value: string; description: string }) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <div className="rounded-lg border bg-card p-4 outline-none transition-colors hover:border-primary/40 focus-visible:ring-2 focus-visible:ring-ring" tabIndex={0}>
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-medium text-muted-foreground">{label}</p>
            <Info className="h-3.5 w-3.5 shrink-0 text-muted-foreground" aria-hidden="true" />
          </div>
          <p className="mt-2 text-2xl font-semibold">{value}</p>
        </div>
      </TooltipTrigger>
      <TooltipContent className="max-w-64 text-xs leading-relaxed">
        {description}
      </TooltipContent>
    </Tooltip>
  );
}

function LoadingState() {
  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-3">
        {[1, 2, 3, 4, 5, 6].map((item) => (
          <Skeleton key={item} className="h-24 rounded-lg" />
        ))}
      </div>
      <Skeleton className="h-72 rounded-lg" />
    </div>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border py-12 text-center">
      <AlertCircle className="h-6 w-6 text-destructive" />
      <p className="text-sm text-muted-foreground">Nao foi possivel carregar o dashboard financeiro.</p>
      <Button variant="outline" onClick={onRetry}>
        <RefreshCw className="mr-2 h-4 w-4" />
        Tentar novamente
      </Button>
    </div>
  );
}

function ChartPanel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="text-sm font-semibold">{title}</h2>
      <div className="mt-4 h-72">{children}</div>
    </section>
  );
}

function SubscriptionMobileCard({ subscription }: { subscription: AdminSubscriptionRowDTO }) {
  return (
    <div className="space-y-2 rounded-lg border bg-card p-4 md:hidden">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-medium">{subscription.empresa}</p>
          <p className="text-sm text-muted-foreground">{subscription.plano}</p>
        </div>
        <Badge variant={subscription.status === 'ACTIVE' ? 'default' : 'secondary'}>{subscription.status}</Badge>
      </div>
      <p className="text-sm">MRR: {money(subscription.mrr)}</p>
      <p className="text-sm">Proxima cobranca: {date(subscription.proximaCobranca)}</p>
      <p className="text-sm">Origem: {subscription.origem}</p>
      <p className="text-sm">Inicio: {date(subscription.inicio)}</p>
      <p className="text-sm">Cancelamento: {date(subscription.cancelamento)}</p>
      <p className="text-sm">Motivo cancelamento: {subscription.motivoCancelamento ?? '-'}</p>
    </div>
  );
}

function SubscriptionsTable({ rows }: { rows: AdminSubscriptionRowDTO[] }) {
  if (rows.length === 0) {
    return (
      <div className="rounded-lg border py-10 text-center text-sm text-muted-foreground">
        Nenhuma assinatura encontrada.
      </div>
    );
  }

  return (
    <section className="rounded-lg border bg-card">
      <div className="border-b p-4">
        <h2 className="text-sm font-semibold">Assinaturas</h2>
      </div>
      <div className="hidden md:block">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Empresa</TableHead>
              <TableHead>Plano</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>MRR</TableHead>
              <TableHead>Proxima cobranca</TableHead>
              <TableHead>Origem</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={`${row.empresa}-${row.plano}`}>
                <TableCell>
                  <div className="font-medium">{row.empresa}</div>
                  <div className="text-xs text-muted-foreground">
                    Inicio: {date(row.inicio)}
                    {row.cancelamento ? ` · Cancelamento: ${date(row.cancelamento)}` : ''}
                    {row.motivoCancelamento ? ` · Motivo cancelamento: ${row.motivoCancelamento}` : ''}
                  </div>
                </TableCell>
                <TableCell>{row.plano}</TableCell>
                <TableCell><Badge variant={row.status === 'ACTIVE' ? 'default' : 'secondary'}>{row.status}</Badge></TableCell>
                <TableCell>{money(row.mrr)}</TableCell>
                <TableCell>{date(row.proximaCobranca)}</TableCell>
                <TableCell>{row.origem}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      <div className="space-y-3 p-3 md:hidden">
        {rows.map((row) => (
          <SubscriptionMobileCard key={`${row.empresa}-${row.plano}`} subscription={row} />
        ))}
      </div>
    </section>
  );
}

function DashboardContent({ data }: { data: AdminDashboardDTO }) {
  const sourceBadge = import.meta.env.VITE_USE_MOCKS === 'true' ? 'Modo mock' : 'API real';

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 md:flex-row md:items-start">
        <div>
          <h1 className="text-2xl font-bold">Dashboard financeiro</h1>
          <p className="text-sm text-muted-foreground">Visao agregada das assinaturas, receita e projecoes.</p>
        </div>
        <Badge variant="secondary">{sourceBadge}</Badge>
      </div>

      <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
        <MetricCard
          label="MRR atual"
          value={money(data.metrics.mrrAtual)}
          description="Receita recorrente mensal estimada a partir das assinaturas ativas."
        />
        <MetricCard
          label="Receita do mes"
          value={money(data.metrics.receitaDoMes)}
          description="Total faturado no mes corrente considerando pagamentos confirmados."
        />
        <MetricCard
          label="Assinaturas ativas"
          value={String(data.metrics.assinaturasAtivas)}
          description="Quantidade de organizações com assinatura ativa no LeadFlow."
        />
        <MetricCard
          label="Churn mensal"
          value={percent(data.metrics.churnMensal)}
          description="Percentual de assinaturas canceladas no mes em relacao a base ativa."
        />
        <MetricCard
          label="ARPA"
          value={money(data.metrics.arpa)}
          description="Receita media por conta ativa. Ajuda a acompanhar expansao, upgrades e mix de planos."
        />
        <MetricCard
          label="Projecao 12m"
          value={money(data.metrics.projecao12m)}
          description="Estimativa de receita recorrente mensal em 12 meses com base no ritmo atual de crescimento."
        />
      </div>

      <div className="flex flex-wrap gap-2 text-xs">
        <Badge variant="outline">ARR {money(data.metrics.arr)}</Badge>
        <Badge variant="outline">Novas assinaturas no mes {data.metrics.novasAssinaturasNoMes}</Badge>
        <Badge variant="outline">Cancelamentos no mes {data.metrics.cancelamentosNoMes}</Badge>
        <Badge variant="outline">Cadastros pendentes de aprovacao {data.metrics.cadastrosPendentesDeAprovacao}</Badge>
        <Badge variant="outline">Ticket medio {money(data.metrics.ticketMedio)}</Badge>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <ChartPanel title="Receita mensal">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data.revenueLast12Months}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <RechartsTooltip />
              <Line type="monotone" dataKey="revenueBrl" stroke="#2563eb" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </ChartPanel>

        <ChartPanel title="Crescimento de assinantes e projecao">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data.subscriberGrowth}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <RechartsTooltip />
              <Area type="monotone" dataKey="activeSubscribers" stroke="#16a34a" fill="#16a34a33" />
              <Area type="monotone" dataKey="projectedMrr" stroke="#7c3aed" fill="#7c3aed22" />
            </AreaChart>
          </ResponsiveContainer>
        </ChartPanel>

        <ChartPanel title="Novas vs cancelamentos">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.newVsCancellations}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <RechartsTooltip />
              <Legend />
              <Bar dataKey="newSubscriptions" fill="#16a34a" name="Novas" />
              <Bar dataKey="cancellations" fill="#dc2626" name="Cancelamentos" />
            </BarChart>
          </ResponsiveContainer>
        </ChartPanel>

        <ChartPanel title="Receita por plano">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={data.revenueByPlan} dataKey="mrrBrl" nameKey="planName" outerRadius={95} label>
                {data.revenueByPlan.map((item, index) => (
                  <Cell key={item.planName} fill={PLAN_COLORS[index % PLAN_COLORS.length]} />
                ))}
              </Pie>
              <RechartsTooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </ChartPanel>
      </div>

      <section className="grid gap-3 md:grid-cols-3">
        {data.projections.map((projection) => (
          <div key={projection.months} className="rounded-lg border bg-card p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium">{projection.months} meses</p>
              <TrendingUp className="h-4 w-4 text-trend-positive" />
            </div>
            <p className="mt-2 text-xl font-semibold">{money(projection.projectedMrrBrl)}</p>
            <p className="text-xs text-muted-foreground">
              Crescimento medio {money(projection.averageMonthlyGrowthBrl)}
            </p>
          </div>
        ))}
      </section>

      <SubscriptionsTable rows={data.subscriptions} />
    </div>
  );
}

export default function AdminDashboard() {
  const { data, isLoading, isError, refetch } = useAdminDashboard();

  return (
    <TooltipProvider>
      <main className="container mx-auto max-w-7xl px-4 py-6">
        {isLoading ? (
          <LoadingState />
        ) : isError || !data ? (
          <ErrorState onRetry={() => refetch()} />
        ) : (
          <DashboardContent data={data} />
        )}
      </main>
    </TooltipProvider>
  );
}
