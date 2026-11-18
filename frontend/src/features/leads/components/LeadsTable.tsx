import { Archive, History, Pencil, Users } from 'lucide-react';
import { EmptyState } from '@/components/shared/EmptyState';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { Lead } from '@/types';
import { stageOptions } from '../constants';

interface LeadsTableProps {
  leads: Lead[];
  hasAnyFilter: boolean;
  onCreateLead: () => void;
  onOpenHistory: (lead: Lead) => void;
  onOpenEdit: (lead: Lead) => void;
  onArchiveLead: (leadId: string) => void;
}

export function LeadsTable({
  leads,
  hasAnyFilter,
  onCreateLead,
  onOpenHistory,
  onOpenEdit,
  onArchiveLead,
}: LeadsTableProps) {
  return (
    <div className="hidden md:block overflow-x-auto">
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
          {leads.length === 0 ? (
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
                  onAction={!hasAnyFilter ? onCreateLead : undefined}
                />
              </td>
            </tr>
          ) : (
            leads.map((lead) => (
              <tr key={lead.id} className="hover:bg-muted/20 transition-colors cursor-pointer" onClick={() => onOpenEdit(lead)}>
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
                <td className="px-4 py-3 text-sm text-muted-foreground">{lead.ultimaInteracao.toLocaleDateString('pt-BR')}</td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={(event) => {
                        event.stopPropagation();
                        onOpenHistory(lead);
                      }}
                    >
                      <History className="mr-1 h-3.5 w-3.5" />
                      Historico
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={(event) => {
                        event.stopPropagation();
                        onOpenEdit(lead);
                      }}
                    >
                      <Pencil className="mr-1 h-3.5 w-3.5" />
                      Editar
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={(event) => {
                        event.stopPropagation();
                        onArchiveLead(lead.id);
                      }}
                    >
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
  );
}
