import { Droppable, Draggable } from '@hello-pangea/dnd';
import { HelpCircle } from 'lucide-react';
import { KanbanCard as KanbanCardType, KanbanColumn as KanbanColumnType } from '@/types';
import { KanbanCardComponent } from './KanbanCard';
import { cn } from '@/lib/utils';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

interface KanbanColumnProps {
  id: KanbanColumnType;
  title: string;
  description: string;
  cards: KanbanCardType[];
  onAssume?: (cardId: string) => void;
  onAdvance?: (cardId: string) => void;
  onArchive?: (cardId: string) => void;
  onCardClick?: (card: KanbanCardType) => void;
}

const columnHintTitle: Record<KanbanColumnType, string> = {
  chatbot: 'Qual a funcao desta coluna?',
  aguardando: 'Quando um lead entra aqui?',
  aguardando_documentacao: 'O que validar nesta etapa?',
  negociacao: 'O que acompanhar nesta fase?',
  concluido: 'Quando marcar como concluido?',
};

const columnStyles: Record<KanbanColumnType, string> = {
  chatbot: 'bg-kanban-chatbot',
  aguardando: 'bg-kanban-waiting',
  aguardando_documentacao: 'bg-amber-600',
  negociacao: 'bg-kanban-negotiation',
  concluido: 'bg-kanban-completed',
};

export function KanbanColumnComponent({
  id,
  title,
  description,
  cards,
  onAssume,
  onAdvance,
  onArchive,
  onCardClick,
}: KanbanColumnProps) {
  return (
    <div className="flex min-w-[260px] flex-shrink-0 flex-col">
      {/* Column Header */}
      <div
        className={cn(
          'rounded-xl px-4 py-3 font-semibold text-white mb-4 flex items-center justify-between',
          columnStyles[id],
        )}
      >
        <span className="text-sm">{title}</span>
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              aria-label={`Ajuda sobre a coluna ${title}`}
              className="rounded-full p-0.5 transition-colors hover:bg-white/20"
            >
              <HelpCircle className="h-4 w-4 text-white/80" />
            </button>
          </TooltipTrigger>
          <TooltipContent side="top" className="max-w-[280px] space-y-1 text-xs">
            <p className="font-semibold text-foreground">{columnHintTitle[id]}</p>
            <p className="text-muted-foreground">{description}</p>
          </TooltipContent>
        </Tooltip>
      </div>

      {/* Droppable Area */}
      <Droppable droppableId={id}>
        {(provided, snapshot) => (
          <div
            ref={provided.innerRef}
            {...provided.droppableProps}
            className={cn(
              'flex-1 space-y-3 rounded-xl p-2 transition-colors min-h-[200px]',
              snapshot.isDraggingOver ? 'bg-muted/50' : 'bg-transparent',
            )}
          >
            {cards.length === 0 && !snapshot.isDraggingOver && (
              <p className="py-8 text-center text-xs text-muted-foreground">Nenhum lead nesta etapa</p>
            )}
            {cards.map((card, index) => (
              <Draggable key={card.id} draggableId={card.id} index={index}>
                {(provided, snapshot) => (
                  <div
                    ref={provided.innerRef}
                    {...provided.draggableProps}
                    {...provided.dragHandleProps}
                    onClick={() => onCardClick?.(card)}
                  >
                    <KanbanCardComponent
                      card={card}
                      isDragging={snapshot.isDragging}
                      onAssume={() => onAssume?.(card.id)}
                      onAdvance={() => onAdvance?.(card.id)}
                      onArchive={() => onArchive?.(card.id)}
                    />
                  </div>
                )}
              </Draggable>
            ))}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </div>
  );
}
