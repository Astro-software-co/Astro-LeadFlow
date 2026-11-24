import { useState } from 'react';
import { AlertTriangle, Plus, ShieldCheck } from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Textarea } from '@/components/ui/textarea';
import {
  useCreatePrivacyRequest,
  useExecutePrivacyRequest,
  usePrivacyRequests,
  useRejectPrivacyRequest,
} from '@/hooks/api/usePrivacy';
import { formatDateTime } from '@/lib/formatters';
import type { DataSubjectRequestDTO, DataSubjectRequestStatus, DataSubjectRequestType } from '@/types/api/privacy';

const requestTypeLabels: Record<DataSubjectRequestType, string> = {
  ANONYMIZATION: 'Anonimização',
  ACCESS: 'Acesso aos dados',
  CORRECTION: 'Correção',
  DELETION: 'Exclusão',
};

const statusLabels: Record<DataSubjectRequestStatus, string> = {
  RECEIVED: 'Recebida',
  IN_PROGRESS: 'Em andamento',
  COMPLETED: 'Concluída',
  REJECTED: 'Rejeitada',
};

const IRREVERSIBLE: DataSubjectRequestType[] = ['ANONYMIZATION', 'DELETION'];

function isOpen(r: DataSubjectRequestDTO) {
  return r.status === 'RECEIVED' || r.status === 'IN_PROGRESS';
}

function errorMessage(error: unknown, fallback: string): string {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  return message ?? fallback;
}

export function PrivacyTab() {
  const [status, setStatus] = useState<DataSubjectRequestStatus | 'all'>('all');
  const { data, isLoading, isError, refetch } = usePrivacyRequests(status === 'all' ? undefined : status);
  const [creating, setCreating] = useState(false);
  const [executing, setExecuting] = useState<DataSubjectRequestDTO | null>(null);
  const [rejecting, setRejecting] = useState<DataSubjectRequestDTO | null>(null);

  return (
    <section className="space-y-6" aria-labelledby="privacy-title">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 id="privacy-title" className="text-lg font-semibold">Privacidade (LGPD)</h2>
          <p className="text-sm text-muted-foreground">
            Solicitações de titulares de dados. A concessionária é a controladora e confirma a identidade do titular antes de executar.
          </p>
        </div>
        <Button onClick={() => setCreating(true)} className="w-full cursor-pointer sm:w-auto">
          <Plus className="mr-2 h-4 w-4" />
          Nova solicitação
        </Button>
      </div>

      <div className="max-w-xs">
        <Label htmlFor="privacy-status">Status</Label>
        <Select value={status} onValueChange={(v) => setStatus(v as DataSubjectRequestStatus | 'all')}>
          <SelectTrigger id="privacy-status"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todas</SelectItem>
            {Object.entries(statusLabels).map(([value, label]) => (
              <SelectItem key={value} value={value}>{label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading && (
        <div className="space-y-3" data-testid="privacy-loading">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      )}

      {isError && (
        <div className="rounded-lg border border-destructive/30 p-4 text-sm">
          Não foi possível carregar as solicitações.{' '}
          <Button variant="link" className="h-auto p-0 cursor-pointer" onClick={() => void refetch()}>Tentar de novo</Button>
        </div>
      )}

      {!isLoading && !isError && data?.length === 0 && (
        <div className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
          <ShieldCheck className="mx-auto mb-2 h-6 w-6" />
          Nenhuma solicitação registrada.
        </div>
      )}

      <ul className="grid gap-3 md:grid-cols-2">
        {data?.map((r) => (
          <li key={r.id} className="rounded-lg border p-4 space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-medium">{requestTypeLabels[r.type]}</span>
              <Badge variant="secondary">{statusLabels[r.status]}</Badge>
              {r.overdue && <Badge variant="destructive">Atrasada</Badge>}
            </div>
            <p className="text-sm text-muted-foreground">
              Titular: <span className="font-mono">{r.subjectHint ?? '—'}</span>
              {r.channel ? ` · Canal: ${r.channel}` : ''}
            </p>
            <p className="text-sm">
              Recebida em {formatDateTime(r.receivedAt)} · Prazo {formatDateTime(r.dueAt)}
            </p>
            {r.resultSummary && <p className="text-sm text-muted-foreground">{r.resultSummary}</p>}
            {isOpen(r) && (
              <div className="flex flex-col gap-2 pt-1 sm:flex-row">
                <Button size="sm" className="cursor-pointer" onClick={() => setExecuting(r)}>Executar</Button>
                <Button size="sm" variant="outline" className="cursor-pointer" onClick={() => setRejecting(r)}>Rejeitar</Button>
              </div>
            )}
          </li>
        ))}
      </ul>

      <CreateRequestDialog open={creating} onOpenChange={setCreating} />
      {executing && <ExecuteRequestDialog request={executing} onClose={() => setExecuting(null)} />}
      {rejecting && <RejectRequestDialog request={rejecting} onClose={() => setRejecting(null)} />}
    </section>
  );
}

function CreateRequestDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const create = useCreatePrivacyRequest();
  const [type, setType] = useState<DataSubjectRequestType>('ANONYMIZATION');
  const [identifier, setIdentifier] = useState('');
  const [channel, setChannel] = useState('');

  const submit = async () => {
    try {
      await create.mutateAsync({ type, identifier: identifier.trim(), channel: channel.trim() || undefined });
      toast.success('Solicitação registrada.');
      setIdentifier('');
      setChannel('');
      onOpenChange(false);
    } catch (error) {
      toast.error(errorMessage(error, 'Não foi possível registrar a solicitação.'));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[100dvh] w-full max-w-full sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Nova solicitação de titular</DialogTitle>
          <DialogDescription>O telefone ou e-mail não é guardado: só um código de verificação e uma dica mascarada.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label htmlFor="dsr-type">Tipo</Label>
            <Select value={type} onValueChange={(v) => setType(v as DataSubjectRequestType)}>
              <SelectTrigger id="dsr-type"><SelectValue /></SelectTrigger>
              <SelectContent>
                {Object.entries(requestTypeLabels).map(([value, label]) => (
                  <SelectItem key={value} value={value}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label htmlFor="dsr-identifier">Telefone ou e-mail do titular</Label>
            <Input id="dsr-identifier" value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="off" />
          </div>
          <div>
            <Label htmlFor="dsr-channel">Canal de recebimento (opcional)</Label>
            <Input id="dsr-channel" value={channel} onChange={(e) => setChannel(e.target.value)} placeholder="E-mail, telefone, presencial..." maxLength={30} />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" className="cursor-pointer" onClick={() => onOpenChange(false)}>Cancelar</Button>
          <Button className="cursor-pointer" onClick={() => void submit()} disabled={!identifier.trim() || create.isPending}>
            Registrar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ExecuteRequestDialog({ request, onClose }: { request: DataSubjectRequestDTO; onClose: () => void }) {
  const execute = useExecutePrivacyRequest();
  const [identifier, setIdentifier] = useState('');
  const [summary, setSummary] = useState('');
  const irreversible = IRREVERSIBLE.includes(request.type);
  const needsSummary = !irreversible;

  const submit = async () => {
    try {
      const result = await execute.mutateAsync({
        id: request.id,
        data: { identifier: identifier.trim(), resultSummary: needsSummary ? summary.trim() : undefined },
      });
      toast.success(result.resultSummary ?? 'Solicitação concluída.');
      onClose();
    } catch (error) {
      toast.error(errorMessage(error, 'Não foi possível executar a solicitação.'));
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[100dvh] w-full max-w-full sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Executar: {requestTypeLabels[request.type]}</DialogTitle>
          <DialogDescription>Informe de novo o telefone ou e-mail do titular ({request.subjectHint}) para confirmar.</DialogDescription>
        </DialogHeader>
        {irreversible && (
          <div role="alert" className="flex gap-2 rounded-md border border-destructive/40 bg-destructive/5 p-3 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0 text-destructive" />
            <span>
              Ação <strong>irreversível</strong>.{' '}
              {request.type === 'ANONYMIZATION'
                ? 'Nome, telefone, e-mail, notas, mensagens e dados coletados pelo bot serão apagados; o card e as métricas ficam.'
                : 'Todos os cards do titular serão excluídos, com mensagens, sessões do bot e propostas vinculadas.'}
            </span>
          </div>
        )}
        <div className="space-y-4">
          <div>
            <Label htmlFor="dsr-confirm">Telefone ou e-mail do titular</Label>
            <Input id="dsr-confirm" value={identifier} onChange={(e) => setIdentifier(e.target.value)} autoComplete="off" />
          </div>
          {needsSummary && (
            <div>
              <Label htmlFor="dsr-summary">Como a solicitação foi atendida</Label>
              <Textarea id="dsr-summary" value={summary} onChange={(e) => setSummary(e.target.value)} maxLength={2000} />
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" className="cursor-pointer" onClick={onClose}>Cancelar</Button>
          <Button
            variant={irreversible ? 'destructive' : 'default'}
            className="cursor-pointer"
            onClick={() => void submit()}
            disabled={!identifier.trim() || (needsSummary && !summary.trim()) || execute.isPending}
          >
            {irreversible ? 'Executar (irreversível)' : 'Concluir'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function RejectRequestDialog({ request, onClose }: { request: DataSubjectRequestDTO; onClose: () => void }) {
  const reject = useRejectPrivacyRequest();
  const [reason, setReason] = useState('');

  const submit = async () => {
    try {
      await reject.mutateAsync({ id: request.id, reason: reason.trim() });
      toast.success('Solicitação rejeitada.');
      onClose();
    } catch (error) {
      toast.error(errorMessage(error, 'Não foi possível rejeitar a solicitação.'));
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[100dvh] w-full max-w-full sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Rejeitar solicitação</DialogTitle>
          <DialogDescription>O motivo fica registrado na solicitação.</DialogDescription>
        </DialogHeader>
        <Label htmlFor="dsr-reason">Motivo</Label>
        <Textarea id="dsr-reason" value={reason} onChange={(e) => setReason(e.target.value)} maxLength={2000} />
        <DialogFooter>
          <Button variant="outline" className="cursor-pointer" onClick={onClose}>Cancelar</Button>
          <Button className="cursor-pointer" onClick={() => void submit()} disabled={!reason.trim() || reject.isPending}>Rejeitar</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
