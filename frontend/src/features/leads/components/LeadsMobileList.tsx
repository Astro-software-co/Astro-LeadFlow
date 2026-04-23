import { Search, Users } from 'lucide-react';
import { EmptyState } from '@/components/shared/EmptyState';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { Lead } from '@/types';
import { stageLabel } from '../utils';

interface LeadsMobileListProps {
  leads: Lead[];
  hasAnyFilter: boolean;
  onCreateLead: () => void;
  onOpenLead: (lead: Lead) => void;
}

export function LeadsMobileList({ leads, hasAnyFilter, onCreateLead, onOpenLead }: LeadsMobileListProps) {
  return (
    <div className="block md:hidden divide-y">
      {leads.length === 0 ? (
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
            onAction={!hasAnyFilter ? onCreateLead : undefined}
          />
        </div>
      ) : (
        leads.map((lead) => (
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
            <p className="text-sm text-muted-foreground">{lead.interesse ?? '-'}</p>
            <div className="flex justify-end pt-2">
              <Button variant="ghost" size="sm" className="py-2" onClick={() => onOpenLead(lead)}>
                Ver lead
              </Button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
