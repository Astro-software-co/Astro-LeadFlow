import { Helmet } from 'react-helmet-async';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useChatbotInsights, useInfrastructure, useObservabilityErrors, useObservabilityOverview, useObservabilityWebhooks } from '@/hooks/api/useObservability';
import { useState } from 'react';

export default function AdminSystem() {
  return (
    <main className="mx-auto max-w-7xl space-y-6 px-4 py-6">
      <Helmet>
        <title>Sistema — Astro LeadFlow</title>
      </Helmet>
      <div>
        <h1 className="text-2xl font-semibold text-foreground">Sistema</h1>
        <p className="text-sm text-muted-foreground">Observability, webhooks, chatbot e infraestrutura da plataforma.</p>
      </div>
      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList className="flex flex-wrap">
          <TabsTrigger value="overview">Visao Geral</TabsTrigger>
          <TabsTrigger value="observability">Observability</TabsTrigger>
          <TabsTrigger value="webhooks">Webhooks</TabsTrigger>
          <TabsTrigger value="chatbot">Chatbot Insights</TabsTrigger>
          <TabsTrigger value="infra">Infraestrutura</TabsTrigger>
        </TabsList>
        <TabsContent value="overview">
          <OverviewTab />
        </TabsContent>
        <TabsContent value="observability">
          <ObservabilityTab />
        </TabsContent>
        <TabsContent value="webhooks">
          <WebhooksTab />
        </TabsContent>
        <TabsContent value="chatbot">
          <ChatbotTab />
        </TabsContent>
        <TabsContent value="infra">
          <InfraTab />
        </TabsContent>
      </Tabs>
    </main>
  );
}

function OverviewTab() {
  const overview = useObservabilityOverview();
  const errors = useObservabilityErrors({ size: 5 });
  const data = overview.data;
  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-4">
        <Metric title="Uptime" value={`${Math.round((data?.uptimeSeconds ?? 0) / 60)} min`} />
        <Metric title="Error rate" value={`${(data?.errorRatePercent ?? 0).toFixed(2)}%`} />
        <Metric title="Req/min" value={(data?.requestsPerMinute ?? 0).toFixed(0)} />
        <Metric title="p95 latency" value={`${(data?.p95LatencyMs ?? 0).toFixed(0)} ms`} />
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Top 5 erros recentes</CardTitle>
        </CardHeader>
        <CardContent>
          <ErrorTable rows={errors.data?.content ?? []} />
        </CardContent>
      </Card>
    </div>
  );
}

function ObservabilityTab() {
  const [correlationId, setCorrelationId] = useState('');
  const [realtime, setRealtime] = useState(true);
  const errors = useObservabilityErrors({ correlationId: correlationId || undefined, size: 20 }, realtime);
  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-3">
        <Metric title="Tempo desde ultimo incidente" value="n/a" />
        <Metric title="Erros 24h" value={errors.data?.totalElements ?? 0} />
        <Metric title="Erros 1h" value={errors.data?.content?.filter((e) => Date.now() - new Date(e.occurredAt).getTime() < 3600000).length ?? 0} />
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Erros recentes</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-col gap-2 sm:flex-row">
            <Input placeholder="correlation_id" value={correlationId} onChange={(event) => setCorrelationId(event.target.value)} />
            <Button variant="outline" onClick={() => setRealtime((value) => !value)}>
              {realtime ? 'Pausar' : 'Retomar'}
            </Button>
          </div>
          <ErrorTable rows={errors.data?.content ?? []} />
        </CardContent>
      </Card>
    </div>
  );
}

function WebhooksTab() {
  const webhooks = useObservabilityWebhooks({ size: 20 });
  return (
    <Card>
      <CardHeader>
        <CardTitle>Eventos de webhook</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Source</TableHead>
              <TableHead>Outcome</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Duracao</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(webhooks.data?.content ?? []).map((row) => (
              <TableRow key={row.id}>
                <TableCell>{row.source}</TableCell>
                <TableCell><Badge variant="outline">{row.outcome}</Badge></TableCell>
                <TableCell>{row.httpStatus}</TableCell>
                <TableCell>{row.durationMs ?? 0} ms</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}

function ChatbotTab() {
  const insights = useChatbotInsights();
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Metric title="Sessoes totais" value={insights.data?.totalSessions ?? 0} />
      <Metric title="Fallback global" value={`${(insights.data?.globalFallbackRatePercent ?? 0).toFixed(1)}%`} />
      <Metric title="Tokens consumidos" value={insights.data?.tokensConsumed ?? 0} />
    </div>
  );
}

function InfraTab() {
  const infra = useInfrastructure();
  const health = (infra.data?.health ?? {}) as Record<string, unknown>;
  return (
    <Card>
      <CardHeader>
        <CardTitle>Servicos</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3 md:grid-cols-3">
        {Object.entries(health).map(([key, value]) => (
          <div key={key} className="rounded-md border p-3">
            <p className="text-xs text-muted-foreground">{key}</p>
            <p className="mt-1 font-medium">{String(value)}</p>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

function ErrorTable({ rows }: { rows: Array<{ id: number; level: string; loggerName: string; message: string; occurredAt: string; correlationId?: string | null }> }) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Level</TableHead>
          <TableHead>Logger</TableHead>
          <TableHead>Mensagem</TableHead>
          <TableHead>Correlation</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((row) => (
          <TableRow key={row.id}>
            <TableCell><Badge variant={row.level === 'ERROR' ? 'destructive' : 'outline'}>{row.level}</Badge></TableCell>
            <TableCell className="max-w-56 truncate">{row.loggerName}</TableCell>
            <TableCell className="max-w-96 truncate">{row.message}</TableCell>
            <TableCell>{row.correlationId ?? 'n/a'}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function Metric({ title, value }: { title: string; value: string | number }) {
  return (
    <Card>
      <CardContent className="p-4">
        <p className="text-sm text-muted-foreground">{title}</p>
        <p className="mt-2 text-2xl font-semibold">{value}</p>
      </CardContent>
    </Card>
  );
}
