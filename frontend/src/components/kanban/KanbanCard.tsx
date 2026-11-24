import { Car, User, Archive, ChevronRight } from 'lucide-react';
import { LeadPrivacyActions } from '@/components/leads/LeadPrivacyActions';
import { KanbanCard as KanbanCardType, CardTag } from '@/types';
import { cn } from '@/lib/utils';
import { formatCurrency } from '@/lib/formatters';

interface KanbanCardProps {
  card: KanbanCardType;
  onAssume?: () => void;
  onAdvance?: () => void;
  onArchive?: () => void;
  isDragging?: boolean;
}

const tagStyles: Record<CardTag['type'], string> = {
  vehicle: 'bg-primary/10 text-primary border-primary/20',
  seller: 'bg-kanban-negotiation/10 text-kanban-negotiation border-kanban-negotiation/20',
  status: '',
};

const statusVariantStyles: Record<string, string> = {
  negotiation: 'bg-kanban-waiting/10 text-kanban-waiting border-kanban-waiting/20',
  reserved: 'bg-kanban-completed/10 text-kanban-completed border-kanban-completed/20',
  completed: 'bg-kanban-completed/10 text-kanban-completed border-kanban-completed/20',
  waiting: 'bg-kanban-waiting/10 text-kanban-waiting border-kanban-waiting/20',
};

export function KanbanCardComponent({ card, onAssume, onAdvance, onArchive, isDragging }: KanbanCardProps) {
  const showAssumeButton = card.coluna === 'aguardando' && !card.vendedorId;
  const showAdvanceButton = card.coluna !== 'concluido';
  const showArchiveButton = true;

  return (
    <div
      className={cn(
        'rounded-xl border bg-card p-4 shadow-sm transition-all cursor-pointer',
        isDragging ? 'rotate-2 scale-105 shadow-lg' : 'hover:shadow-md',
      )}
    >
      <div className="space-y-3">
        {/* Header */}
        <div>
          <h4 className="font-semibold text-foreground">{card.lead.nome}</h4>
          <p className="text-sm text-muted-foreground">Número: {card.lead.telefone}</p>
        </div>

        {/* Tags */}
        <div className="flex flex-wrap gap-2">
          {card.tags.map((tag, index) => {
            let style = tagStyles[tag.type];
            if (tag.type === 'status' && 'variant' in tag) {
              style = statusVariantStyles[tag.variant];
            }
            return (
              <span
                key={index}
                className={cn(
                  'inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium',
                  style,
                )}
              >
                {tag.type === 'vehicle' && <Car className="h-3 w-3" />}
                {tag.type === 'seller' && <User className="h-3 w-3" />}
                {tag.label}
              </span>
            );
          })}
        </div>

        {/* Interest */}
        {card.interesse && <p className="text-sm text-muted-foreground">Interesse: {card.interesse}</p>}

        {/* Budget */}
        {card.orcamento && <p className="text-sm text-muted-foreground">Orçamento: {formatCurrency(card.orcamento)}</p>}

        {/* Actions */}
        <div className="flex flex-wrap gap-2 pt-2">
          {showAssumeButton && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onAssume?.();
              }}
              className="inline-flex items-center gap-1 rounded-lg bg-primary px-2 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              <User className="h-3 w-3" />
              Assumir
            </button>
          )}
          {showArchiveButton && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onArchive?.();
              }}
              className="inline-flex items-center gap-1 rounded-lg border bg-card px-2 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted transition-colors"
            >
              <Archive className="h-3 w-3" />
              Arquivar
            </button>
          )}
          <LeadPrivacyActions leadId={card.leadId} leadName={card.lead.nome} phone={card.lead.telefone} compact deleteOnly />
          {showAdvanceButton && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onAdvance?.();
              }}
              className="inline-flex items-center gap-1 rounded-lg bg-kanban-completed px-2 py-1.5 text-xs font-medium text-white hover:bg-kanban-completed/90 transition-colors"
            >
              Avançar
              <ChevronRight className="h-3 w-3" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
