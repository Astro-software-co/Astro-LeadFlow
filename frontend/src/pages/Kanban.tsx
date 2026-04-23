import { Helmet } from 'react-helmet-async';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { KanbanColumn } from '@/types';
import { KanbanBoard, useArchiveLead, useKanbanBoard, useUpdateLeadStage } from '@/modules/crm';

export default function Kanban() {
  const { data: cards, isLoading, error, refetch } = useKanbanBoard();
  const updateLeadStage = useUpdateLeadStage();
  const archiveLead = useArchiveLead();

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar o kanban</h2>
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
        <title>Kanban — Astro LeadFlow</title>
        <meta name="description" content="Pipeline visual de vendas. Arraste leads entre etapas." />
      </Helmet>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Kanban</h1>
          <p className="text-muted-foreground">Gerencie seus leads e atendimentos</p>
        </div>
        <KanbanBoard
          initialCards={cards ?? []}
          onMoveLead={async (leadId: string, stage: KanbanColumn) => {
            await updateLeadStage.mutateAsync({ id: leadId, stage });
          }}
          onArchiveLead={async (leadId: string) => {
            await archiveLead.mutateAsync(leadId);
          }}
        />
      </div>
    </>
  );
}
