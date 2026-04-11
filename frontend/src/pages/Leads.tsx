import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { Controller, useForm } from 'react-hook-form';
import type { DateRange } from 'react-day-picker';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Calendar as CalendarIcon, Loader2, Plus, Search, Filter, Users, Pencil, Archive, History } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Calendar } from '@/components/ui/calendar';
import { EmptyState } from '@/components/shared/EmptyState';
import { useArchiveLead, useCreateLead, useLeadActivities, useLeads, useUpdateLead } from '@/hooks/api/useLeads';
import { Lead, KanbanColumn } from '@/types';
import { cn } from '@/lib/utils';
import { mapKanbanColumnToPipelineStage } from '@/mappers/leads.mapper';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { formatDateTime } from '@/lib/formatters';
import { Skeleton } from '@/components/ui/skeleton';

const leadSchema = z.object({
  nome: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres'),
  telefone: z.string().min(10, 'Telefone invalido'),
  email: z.string().email('E-mail invalido').optional().or(z.literal('')),
  origem: z.enum(['OLX', 'Mercado Livre', 'Instagram', 'WebMotors', 'Facebook', 'Mobiauto', 'Napista', 'Direto']),
  interesse: z.string().optional(),
  orcamento: z
    .string()
    .optional()
    .refine((value) => !value || !Number.isNaN(Number(value)), 'Orcamento invalido'),
  urgencia: z.enum(['baixa', 'media', 'alta']).optional().or(z.literal('')),
  observacoes: z.string().optional(),
});

type LeadFormValues = z.infer<typeof leadSchema>;

const stageOptions: Array<{ value: 'all' | KanbanColumn; label: string }> = [
  { value: 'all', label: 'Todos os estagios' },
  { value: 'chatbot', label: 'Falando com chatbot' },
  { value: 'aguardando', label: 'Aguardando atendimento' },
  { value: 'aguardando_documentacao', label: 'Aguardando documentacao' },
  { value: 'negociacao', label: 'Em negociacao' },
  { value: 'concluido', label: 'Concluido' },
];

const originOptions: Array<{ value: 'all' | Lead['origem']; label: string }> = [
  { value: 'all', label: 'Todas as origens' },
  { value: 'Direto', label: 'Direto' },
  { value: 'WebMotors', label: 'WebMotors' },
  { value: 'Mercado Livre', label: 'Mercado Livre' },
  { value: 'OLX', label: 'OLX' },
  { value: 'Instagram', label: 'Instagram' },
  { value: 'Facebook', label: 'Facebook' },
  { value: 'Mobiauto', label: 'Mobiauto' },
  { value: 'Napista', label: 'Napista' },
];

function stageLabel(stage: KanbanColumn | null | undefined): string {
  if (!stage) {
    return 'Sem estagio';
  }

  return stageOptions.find((option) => option.value === stage)?.label ?? 'Aguardando atendimento';
}

function toISODate(value?: Date): string | undefined {
  if (!value) {
    return undefined;
  }

  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateRange(range?: DateRange): string {
  if (!range?.from) {
    return 'Filtrar por periodo';
  }

  const from = range.from.toLocaleDateString('pt-BR');
  if (!range.to) {
    return `${from} - ...`;
  }

  return `${from} - ${range.to.toLocaleDateString('pt-BR')}`;
}

function LeadFormModal({
  open,
  onOpenChange,
  lead,
  onSubmit,
  isSubmitting,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lead: Lead | null;
  onSubmit: (values: LeadFormValues) => Promise<void>;
  isSubmitting: boolean;
}) {
  const form = useForm<LeadFormValues>({
    resolver: zodResolver(leadSchema),
    values: lead
      ? {
          nome: lead.nome,
          telefone: lead.telefone,
          email: lead.email ?? '',
          origem: lead.origem,
          interesse: lead.interesse ?? '',
          orcamento: lead.orcamento ? String(lead.orcamento) : '',
          urgencia: lead.urgencia ?? '',
          observacoes: lead.observacoes ?? '',
        }
      : {
          nome: '',
          telefone: '',
          email: '',
          origem: 'Direto',
          interesse: '',
          orcamento: '',
          urgencia: '',
          observacoes: '',
        },
  });
        const urgencyEmptyValue = 'none';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{lead ? 'Editar lead' : 'Novo lead'}</DialogTitle>
        </DialogHeader>

        <form
          className="mt-4 space-y-4"
          onSubmit={form.handleSubmit(async (values) => {
            await onSubmit(values);
          })}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="nome">Nome</Label>
              <Input id="nome" {...form.register('nome')} />
              {form.formState.errors.nome && <p className="text-xs text-destructive">{form.formState.errors.nome.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="telefone">Telefone</Label>
              <Input id="telefone" {...form.register('telefone')} />
              {form.formState.errors.telefone && (
                <p className="text-xs text-destructive">{form.formState.errors.telefone.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <Input id="email" type="email" {...form.register('email')} />
              {form.formState.errors.email && (
                <p className="text-xs text-destructive">{form.formState.errors.email.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="origem">Origem</Label>
              <Controller
                control={form.control}
                name="origem"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="origem">
                      <SelectValue placeholder="Selecione a origem" />
                    </SelectTrigger>
                    <SelectContent>
                      {originOptions
                        .filter((option) => option.value !== 'all')
                        .map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="interesse">Interesse</Label>
              <Input id="interesse" {...form.register('interesse')} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="orcamento">Orcamento (R$)</Label>
              <Input id="orcamento" {...form.register('orcamento')} />
              {form.formState.errors.orcamento && (
                <p className="text-xs text-destructive">{form.formState.errors.orcamento.message}</p>
              )}
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="urgencia">Urgencia</Label>
              <Controller
                control={form.control}
                name="urgencia"
                render={({ field }) => (
                  <Select
                    value={field.value || urgencyEmptyValue}
                    onValueChange={(value) => field.onChange(value === urgencyEmptyValue ? '' : value)}
                  >
                    <SelectTrigger id="urgencia">
                      <SelectValue placeholder="Nao informada" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={urgencyEmptyValue}>Nao informada</SelectItem>
                      <SelectItem value="baixa">Baixa</SelectItem>
                      <SelectItem value="media">Media</SelectItem>
                      <SelectItem value="alta">Alta</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="observacoes">Observacoes</Label>
              <Textarea id="observacoes" rows={3} {...form.register('observacoes')} />
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
              {lead ? 'Salvar' : 'Criar lead'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function Leads() {
  const pageSize = 10;
  const [search, setSearch] = useState('');
  const [origin, setOrigin] = useState<'all' | Lead['origem']>('all');
  const [stage, setStage] = useState<'all' | KanbanColumn>('all');
  const [assignedTo, setAssignedTo] = useState<'all' | string>('all');
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);
  const [isDateRangeOpen, setIsDateRangeOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedLead, setSelectedLead] = useState<Lead | null>(null);
  const [historyLead, setHistoryLead] = useState<Lead | null>(null);

  const filters = useMemo(() => {
    const trimmed = search.trim();
    const dateFrom = toISODate(dateRange?.from);
    const dateTo = toISODate(dateRange?.to);

    return {
      search: trimmed || undefined,
      origin: origin === 'all' ? undefined : origin,
      stage: stage === 'all' ? undefined : mapKanbanColumnToPipelineStage(stage),
      assignedTo: assignedTo === 'all' ? undefined : assignedTo,
      dateFrom,
      dateTo,
      page: page - 1,
      size: pageSize,
    };
  }, [search, origin, stage, assignedTo, dateRange, page]);

  const { data, isLoading, isFetching, error, refetch } = useLeads(filters);
  const { data: activities, isLoading: isLoadingActivities } = useLeadActivities(historyLead?.id);
  const createLead = useCreateLead();
  const updateLead = useUpdateLead();
  const archiveLead = useArchiveLead();

  const leads = useMemo(() => data?.content ?? [], [data]);
  const assignedUsers = useMemo(() => {
    const unique = new Map<string, string>();
    leads.forEach((lead) => {
      if (lead.assignedToId && lead.assignedToName) {
        unique.set(lead.assignedToId, lead.assignedToName);
      }
    });
    return Array.from(unique.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [leads]);
  const totalElements = data?.totalElements ?? 0;
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const safePage = Math.min(page, totalPages);
  const pagedLeads = leads;
  const dateRangeLabel = useMemo(() => formatDateRange(dateRange), [dateRange]);

  const hasAnyFilter =
    !!search.trim() || origin !== 'all' || stage !== 'all' || assignedTo !== 'all' || !!dateRange?.from;

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const openCreateModal = () => {
    setSelectedLead(null);
    setIsModalOpen(true);
  };

  const openEditModal = (lead: Lead) => {
    setSelectedLead(lead);
    setIsModalOpen(true);
  };

  const handleSubmit = async (values: LeadFormValues) => {
    const payload: Partial<Lead> = {
      nome: values.nome,
      telefone: values.telefone,
      email: values.email || undefined,
      origem: values.origem,
      interesse: values.interesse || undefined,
      orcamento: values.orcamento ? Number(values.orcamento) : undefined,
      urgencia: values.urgencia || undefined,
      observacoes: values.observacoes || undefined,
    };

    if (selectedLead) {
      await updateLead.mutateAsync({ id: selectedLead.id, data: payload });
      toast.success('Lead atualizado com sucesso');
    } else {
      await createLead.mutateAsync(payload);
      toast.success('Lead criado com sucesso');
    }

    setIsModalOpen(false);
    setSelectedLead(null);
  };

  const handleArchive = async (leadId: string) => {
    const confirmed = window.confirm('Deseja arquivar este lead?');
    if (!confirmed) return;
    await archiveLead.mutateAsync(leadId);
    toast.success('Lead arquivado com sucesso');
  };

  if (isLoading && !data) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <Skeleton className="h-8 w-32" />
            <Skeleton className="mt-2 h-4 w-48" />
          </div>
          <Skeleton className="h-10 w-32" />
        </div>
        <div data-testid="leads-skeleton" className="rounded-xl border bg-card shadow-sm overflow-hidden">
          <table className="hidden md:table w-full min-w-[880px]">
            <thead>
              <tr className="border-b bg-muted/30">
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Lead</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Contato</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Origem</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Estagio</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Interesse</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Atualizado</th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-muted-foreground">Acoes</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {Array.from({ length: 8 }).map((_, i) => (
                <tr key={i} data-testid="skeleton-row">
                  <td className="px-4 py-4"><Skeleton className="h-4 w-40" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-4 w-24" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-4 w-24" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-4 w-32" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-4 w-20" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-4 w-20" /></td>
                  <td className="px-4 py-4"><Skeleton className="h-4 w-16 ml-auto" /></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="block md:hidden space-y-4 p-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="rounded-xl border bg-card p-4 shadow-sm space-y-3">
                <div className="flex items-start justify-between">
                  <Skeleton className="h-5 w-32" />
                  <Skeleton className="h-5 w-20 rounded-full" />
                </div>
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-4 w-40" />
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar os leads</h2>
        <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
        <Button className="mt-4" onClick={() => void refetch()}>
          Tentar novamente
        </Button>
      </div>
    );
  }

  return (
    <>
      <Helmet>
        <title>Leads — Astro LeadFlow</title>
        <meta name="description" content="Gestao de leads com busca, filtros e operacoes basicas de CRM." />
      </Helmet>

      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Leads</h1>
            <p className="text-muted-foreground">Gestao de contatos e oportunidades</p>
          </div>
          <Button onClick={openCreateModal}>
            <Plus className="mr-2 h-4 w-4" />
            Novo lead
          </Button>
        </div>

        <div className="grid gap-3 md:grid-cols-5">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Buscar por nome, telefone ou email"
              className="pl-10 pr-10"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(1);
              }}
            />
            {isFetching ? (
              <Loader2 className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
            ) : null}
          </div>

          <div className="relative">
            <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={origin}
              onValueChange={(value) => {
                setOrigin(value as 'all' | Lead['origem']);
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todas as origens" />
              </SelectTrigger>
              <SelectContent>
                {originOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="relative">
            <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={stage}
              onValueChange={(value) => {
                setStage(value as 'all' | KanbanColumn);
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todos os estagios" />
              </SelectTrigger>
              <SelectContent>
                {stageOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="relative">
            <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={assignedTo}
              onValueChange={(value) => {
                setAssignedTo(value);
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todos os responsaveis" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Todos os responsaveis</SelectItem>
                {assignedUsers.map((seller) => (
                  <SelectItem key={seller.id} value={seller.id}>
                    {seller.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Popover open={isDateRangeOpen} onOpenChange={setIsDateRangeOpen}>
            <PopoverTrigger asChild>
              <Button
                type="button"
                variant="outline"
                className={cn('h-10 w-full justify-start text-left font-normal', !dateRange?.from && 'text-muted-foreground')}
              >
                <CalendarIcon className="mr-2 h-4 w-4" />
                <span className="truncate">{dateRangeLabel}</span>
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0" align="start">
              <Calendar
                mode="range"
                selected={dateRange}
                defaultMonth={dateRange?.from}
                onSelect={(nextRange) => {
                  setDateRange(nextRange);
                  setPage(1);

                  if (nextRange?.from && nextRange.to) {
                    setIsDateRangeOpen(false);
                  }
                }}
                numberOfMonths={1}
              />
              {dateRange?.from ? (
                <div className="border-t p-2">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="w-full justify-center"
                    onClick={() => {
                      setDateRange(undefined);
                      setPage(1);
                    }}
                  >
                    Limpar periodo
                  </Button>
                </div>
              ) : null}
            </PopoverContent>
          </Popover>
        </div>

        <div className="rounded-xl border bg-card shadow-sm">
          <div className="hidden md:block overflow-x-auto">
            <table className="hidden md:table w-full min-w-[880px]">
              <thead>
                <tr className="border-b bg-muted/30">
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Lead
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Contato
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Origem
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Estagio
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Interesse
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Atualizado
                  </th>
                  <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Acoes
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {pagedLeads.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-8">
                      <EmptyState
                        icon={Users}
                        title={!hasAnyFilter ? 'Nenhum lead encontrado' : 'Sem resultados para os filtros'}
                        description={
                          !hasAnyFilter
                            ? 'Crie o primeiro lead para iniciar o funil comercial.'
                            : 'Tente ajustar busca, origem, estagio, responsavel ou periodo.'
                        }
                        actionLabel={!hasAnyFilter ? 'Criar lead' : undefined}
                        onAction={!hasAnyFilter ? openCreateModal : undefined}
                      />
                    </td>
                  </tr>
                ) : (
                  pagedLeads.map((lead) => (
                    <tr key={lead.id} className="hover:bg-muted/20">
                      <td className="px-4 py-3">
                        <p className="font-medium text-foreground">{lead.nome}</p>
                        {lead.assignedToName ? (
                          <p className="text-xs text-muted-foreground">Responsavel: {lead.assignedToName}</p>
                        ) : (
                          <p className="text-xs text-muted-foreground">Sem responsavel</p>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">
                        <p>{lead.telefone}</p>
                        {lead.email ? <p>{lead.email}</p> : null}
                      </td>
                      <td className="px-4 py-3 text-sm text-foreground">{lead.origem}</td>
                      <td className="px-4 py-3 text-sm">
                        <span
                          className={cn(
                            'inline-flex rounded-full px-2.5 py-1 text-xs font-medium',
                            lead.estagio === 'concluido'
                              ? 'bg-kanban-completed/10 text-kanban-completed'
                              : lead.estagio === 'negociacao'
                                ? 'bg-kanban-negotiation/10 text-kanban-negotiation'
                                : 'bg-muted text-muted-foreground',
                          )}
                        >
                          {stageOptions.find((option) => option.value === (lead.estagio ?? 'aguardando'))?.label ??
                            'Aguardando atendimento'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">{lead.interesse ?? '-'}</td>
                      <td className="px-4 py-3 text-sm text-muted-foreground">
                        {lead.ultimaInteracao.toLocaleDateString('pt-BR')}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          <Button size="sm" variant="outline" onClick={() => setHistoryLead(lead)}>
                            <History className="mr-1 h-3.5 w-3.5" />
                            Historico
                          </Button>
                          <Button size="sm" variant="outline" onClick={() => openEditModal(lead)}>
                            <Pencil className="mr-1 h-3.5 w-3.5" />
                            Editar
                          </Button>
                          <Button size="sm" variant="outline" onClick={() => void handleArchive(lead.id)}>
                            <Archive className="mr-1 h-3.5 w-3.5" />
                            Arquivar
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile card view */}
          <div className="block md:hidden divide-y">
            {pagedLeads.length === 0 ? (
              <div className="p-4">
                <EmptyState
                  icon={hasAnyFilter ? Search : Users}
                  title={!hasAnyFilter ? 'Nenhum lead encontrado' : 'Nenhum resultado'}
                  description={
                    !hasAnyFilter
                      ? 'Crie o primeiro lead para iniciar o funil comercial.'
                      : 'Nenhum lead corresponde aos filtros aplicados. Tente outras combinacoes.'
                  }
                  actionLabel={!hasAnyFilter ? 'Criar lead' : undefined}
                  onAction={!hasAnyFilter ? openCreateModal : undefined}
                />
              </div>
            ) : (
              pagedLeads.map((lead) => (
                <div key={lead.id} className="p-4 space-y-1">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-base font-semibold text-foreground truncate">{lead.nome}</p>
                    <span
                      className={cn(
                        'inline-flex shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium',
                        lead.estagio === 'concluido'
                          ? 'bg-kanban-completed/10 text-kanban-completed'
                          : lead.estagio === 'negociacao'
                            ? 'bg-kanban-negotiation/10 text-kanban-negotiation'
                            : 'bg-muted text-muted-foreground',
                      )}
                    >
                      {stageLabel(lead.estagio)}
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground">{lead.origem}</p>
                  <p className="text-sm text-muted-foreground">{lead.interesse ?? '\u2014'}</p>
                  <div className="flex justify-end pt-2">
                    <Button variant="ghost" size="sm" className="py-2" onClick={() => openEditModal(lead)}>
                      Ver lead
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="flex items-center justify-between border-t px-4 py-3">
            <p className="text-xs text-muted-foreground">
              Mostrando {totalElements === 0 ? 0 : (safePage - 1) * pageSize + 1} a{' '}
              {totalElements === 0 ? 0 : (safePage - 1) * pageSize + pagedLeads.length}{' '}
              de {totalElements} leads
            </p>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" disabled={safePage <= 1} onClick={() => setPage((p) => p - 1)}>
                Anterior
              </Button>
              <span className="text-xs text-muted-foreground">
                Pagina {safePage} de {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={safePage >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Proxima
              </Button>
            </div>
          </div>
        </div>
      </div>

      <LeadFormModal
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        lead={selectedLead}
        onSubmit={handleSubmit}
        isSubmitting={createLead.isPending || updateLead.isPending}
      />

      <Dialog open={!!historyLead} onOpenChange={(open) => (!open ? setHistoryLead(null) : null)}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>Historico do lead</DialogTitle>
          </DialogHeader>
          {historyLead ? (
            <div className="space-y-4">
              <div>
                <p className="font-medium text-foreground">{historyLead.nome}</p>
                <p className="text-sm text-muted-foreground">{historyLead.telefone}</p>
              </div>

              <div className="space-y-3 rounded-lg border bg-muted/20 p-4">
                {isLoadingActivities ? (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Carregando atividades...
                  </div>
                ) : activities && activities.length > 0 ? (
                  activities.map((activity, index) => (
                    <div key={`${activity.movedAt.toISOString()}-${index}`} className="rounded-md border bg-background p-3">
                      <p className="text-sm font-medium text-foreground">
                        {activity.fromStage
                          ? `Movido de ${stageLabel(activity.fromStage)} para ${stageLabel(activity.toStage)}`
                          : `Entrada no estagio ${stageLabel(activity.toStage)}`}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {formatDateTime(activity.movedAt)} · {activity.movedByName ?? 'Sistema'}
                      </p>
                    </div>
                  ))
                ) : (
                  <div className="space-y-1">
                    <p className="text-sm font-medium text-foreground">Sem movimentacoes registradas</p>
                    <p className="text-xs text-muted-foreground">
                      Lead criado em {formatDateTime(historyLead.dataCriacao)}. Estagio atual: {stageLabel(historyLead.estagio)}.
                    </p>
                  </div>
                )}
              </div>
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}
