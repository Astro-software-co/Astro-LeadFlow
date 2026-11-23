import { Helmet } from 'react-helmet-async';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { BotSessionStatus } from '@/types/api/chat';
import { useChatbotHandoff, useChatbotMetrics, useChatbotSessions } from '@/hooks/api/useChatbot';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Loader2 } from 'lucide-react';
import { useState } from 'react';

const statusOptions: Array<BotSessionStatus | 'ALL'> = ['ALL', 'ACTIVE', 'HANDOFF', 'COMPLETED', 'ABANDONED'];

export default function Chatbot() {
  const [status, setStatus] = useState<BotSessionStatus | 'ALL'>('ALL');
  const sessions = useChatbotSessions(status === 'ALL' ? undefined : status);
  const metrics = useChatbotMetrics();
  const handoff = useChatbotHandoff();

  const chartData = metrics.data?.topIntents ?? [];

  return (
    <div className="space-y-6">
      <Helmet>
        <title>Chatbot — Astro LeadFlow</title>
      </Helmet>

      <div>
        <h1 className="text-2xl font-semibold text-foreground">Chatbot</h1>
        <p className="text-sm text-muted-foreground">Acompanhe sessões, métricas e configuração do atendimento automatizado.</p>
      </div>

      <Tabs defaultValue="sessions" className="space-y-4">
        <TabsList>
          <TabsTrigger value="sessions">Sessoes</TabsTrigger>
          <TabsTrigger value="metrics">Metricas</TabsTrigger>
          <TabsTrigger value="config">Configuracao</TabsTrigger>
        </TabsList>

        <TabsContent value="sessions" className="space-y-4">
          <div className="w-56">
            <Select value={status} onValueChange={(value) => setStatus(value as BotSessionStatus | 'ALL')}>
              <SelectTrigger>
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                {statusOptions.map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Card>
            <CardContent className="p-0">
              {sessions.isLoading ? (
                <div className="flex h-40 items-center justify-center">
                  <Loader2 className="h-5 w-5 animate-spin text-primary" />
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Lead</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Intent</TableHead>
                      <TableHead>Fallback</TableHead>
                      <TableHead className="text-right">Acoes</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {(sessions.data?.content ?? []).map((session) => (
                      <TableRow key={session.id}>
                        <TableCell>
                          <p className="font-medium">{session.leadName ?? 'Sem nome'}</p>
                          <p className="text-xs text-muted-foreground">{session.leadPhone}</p>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">{session.status}</Badge>
                        </TableCell>
                        <TableCell>{session.currentIntent ?? 'n/a'}</TableCell>
                        <TableCell>{session.fallbackCount}/3</TableCell>
                        <TableCell className="text-right">
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={session.status !== 'ACTIVE' || handoff.isPending}
                            onClick={() => handoff.mutate(session.id)}
                          >
                            Handoff
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="metrics" className="space-y-4">
          <div className="grid gap-4 md:grid-cols-4">
            <Metric title="Sessoes" value={metrics.data?.totalSessions ?? 0} />
            <Metric title="Fallback" value={`${(metrics.data?.fallbackRatePercent ?? 0).toFixed(1)}%`} />
            <Metric title="Handoff" value={`${(metrics.data?.handoffRatePercent ?? 0).toFixed(1)}%`} />
            <Metric title="Tokens" value={metrics.data?.tokensConsumed ?? 0} />
          </div>
          <Card>
            <CardHeader>
              <CardTitle>Top intents</CardTitle>
            </CardHeader>
            <CardContent className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="intent" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="config">
          <Card>
            <CardHeader>
              <CardTitle>Configuracao somente leitura</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 text-sm md:grid-cols-3">
              <ConfigItem label="Idioma" value="pt_BR" />
              <ConfigItem label="Modo dev" value="mock" />
              <ConfigItem label="Fallback" value="Lex FallbackIntent -> Bedrock" />
              <ConfigItem label="Basico" value="Template fixo" />
              <ConfigItem label="Profissional" value="Lex" />
              <ConfigItem label="Enterprise" value="Lex + Bedrock" />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
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

function ConfigItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 font-medium">{value}</p>
    </div>
  );
}
