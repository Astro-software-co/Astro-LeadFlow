import { useEffect, useMemo, useState } from 'react';
import type { DateRange } from 'react-day-picker';
import { Helmet } from 'react-helmet-async';
import { Plus } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  LeadArchiveConfirmDialog,
  LeadFormDialog,
  LeadHistoryDialog,
  LeadsErrorState,
  LeadsFiltersBar,
  LeadsLoadingState,
  LeadsMobileList,
  LeadsPagination,
  LeadsTable,
  buildLeadsFilters,
  formatDateRange,
  hasActiveLeadFilters,
  useArchiveLead,
  useAssignableUsers,
  useCreateLead,
  useLeadActivities,
  useLeads,
  useUpdateLead,
} from '@/modules/crm';
import type { LeadAssignedFilter, LeadFormValues, LeadOriginFilter, LeadStageFilter } from '@/modules/crm';
import type { Lead } from '@/types';

const PAGE_SIZE = 10;

export default function Leads() {
  const [search, setSearch] = useState('');
  const [origin, setOrigin] = useState<LeadOriginFilter>('all');
  const [stage, setStage] = useState<LeadStageFilter>('all');
  const [assignedTo, setAssignedTo] = useState<LeadAssignedFilter>('all');
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined);
  const [isDateRangeOpen, setIsDateRangeOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedLead, setSelectedLead] = useState<Lead | null>(null);
  const [historyLead, setHistoryLead] = useState<Lead | null>(null);
  const [pendingArchiveLead, setPendingArchiveLead] = useState<Lead | null>(null);

  const filters = useMemo(
    () =>
      buildLeadsFilters({
        search,
        origin,
        stage,
        assignedTo,
        dateRange,
        page,
        pageSize: PAGE_SIZE,
      }),
    [search, origin, stage, assignedTo, dateRange, page],
  );

  const { data, isLoading, isFetching, error, refetch } = useLeads(filters);
  const { data: assignableUsersData } = useAssignableUsers();
  const { data: activities, isLoading: isLoadingActivities } = useLeadActivities(historyLead?.id);
  const createLead = useCreateLead();
  const updateLead = useUpdateLead();
  const archiveLead = useArchiveLead();

  const leads = useMemo(() => data?.content ?? [], [data]);
  const assignedUsersFromLeads = useMemo(() => {
    const unique = new Map<string, string>();
    leads.forEach((lead) => {
      if (lead.assignedToId && lead.assignedToName) unique.set(lead.assignedToId, lead.assignedToName);
    });
    return Array.from(unique.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((left, right) => left.name.localeCompare(right.name));
  }, [leads]);
  const assignedUsers = assignableUsersData?.length ? assignableUsersData : assignedUsersFromLeads;

  const totalElements = data?.totalElements ?? 0;
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const safePage = Math.min(page, totalPages);
  const dateRangeLabel = useMemo(() => formatDateRange(dateRange), [dateRange]);
  const hasAnyFilter = hasActiveLeadFilters(search, origin, stage, assignedTo, dateRange);

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

  const handleSearchChange = (value: string) => {
    setSearch(value);
    setPage(1);
  };

  const handleOriginChange = (value: LeadOriginFilter) => {
    setOrigin(value);
    setPage(1);
  };

  const handleStageChange = (value: LeadStageFilter) => {
    setStage(value);
    setPage(1);
  };

  const handleAssignedToChange = (value: LeadAssignedFilter) => {
    setAssignedTo(value);
    setPage(1);
  };

  const handleDateRangeChange = (nextRange?: DateRange) => {
    setDateRange(nextRange);
    setPage(1);
    if (nextRange?.from && nextRange.to) setIsDateRangeOpen(false);
  };

  const clearDateRange = () => {
    setDateRange(undefined);
    setPage(1);
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

  const handleArchiveConfirm = async () => {
    if (!pendingArchiveLead) {
      return;
    }

    try {
      await archiveLead.mutateAsync(pendingArchiveLead.id);
      toast.success('Lead arquivado com sucesso');
      setPendingArchiveLead(null);
    } catch {
      toast.error('Nao foi possivel arquivar o lead.');
    }
  };

  if (isLoading && !data) return <LeadsLoadingState />;
  if (error) return <LeadsErrorState onRetry={() => void refetch()} />;

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

        <LeadsFiltersBar
          search={search}
          origin={origin}
          stage={stage}
          assignedTo={assignedTo}
          assignedUsers={assignedUsers}
          dateRange={dateRange}
          dateRangeLabel={dateRangeLabel}
          isDateRangeOpen={isDateRangeOpen}
          isFetching={isFetching}
          onSearchChange={handleSearchChange}
          onOriginChange={handleOriginChange}
          onStageChange={handleStageChange}
          onAssignedToChange={handleAssignedToChange}
          onDateRangeOpenChange={setIsDateRangeOpen}
          onDateRangeChange={handleDateRangeChange}
          onClearDateRange={clearDateRange}
        />

        <div className="rounded-xl border bg-card shadow-sm">
          <LeadsTable
            leads={leads}
            hasAnyFilter={hasAnyFilter}
            onCreateLead={openCreateModal}
            onOpenHistory={setHistoryLead}
            onOpenEdit={openEditModal}
            onArchiveLead={(leadId) => {
              const lead = leads.find((item) => item.id === leadId) ?? null;
              setPendingArchiveLead(lead);
            }}
          />

          <LeadsMobileList
            leads={leads}
            hasAnyFilter={hasAnyFilter}
            onCreateLead={openCreateModal}
            onOpenLead={openEditModal}
          />

          <LeadsPagination
            totalElements={totalElements}
            totalPages={totalPages}
            safePage={safePage}
            pageSize={PAGE_SIZE}
            currentCount={leads.length}
            onPrevious={() => setPage((current) => current - 1)}
            onNext={() => setPage((current) => current + 1)}
          />
        </div>
      </div>

      <LeadFormDialog
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        lead={selectedLead}
        onSubmit={handleSubmit}
        isSubmitting={createLead.isPending || updateLead.isPending}
      />

      <LeadHistoryDialog
        lead={historyLead}
        activities={activities}
        isLoadingActivities={isLoadingActivities}
        onOpenChange={(open) => !open && setHistoryLead(null)}
      />

      <LeadArchiveConfirmDialog
        open={Boolean(pendingArchiveLead)}
        leadName={pendingArchiveLead?.nome}
        isArchiving={archiveLead.isPending}
        onOpenChange={(open) => {
          if (!open) {
            setPendingArchiveLead(null);
          }
        }}
        onConfirm={() => {
          void handleArchiveConfirm();
        }}
      />
    </>
  );
}
