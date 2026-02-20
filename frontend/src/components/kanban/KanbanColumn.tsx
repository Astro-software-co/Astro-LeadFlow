import { Droppable, Draggable } from '@hello-pangea/dnd';
import { HelpCircle } from 'lucide-react';
import { KanbanCard as KanbanCardType, KanbanColumn as KanbanColumnType } from '@/types';
import { KanbanCardComponent } from './KanbanCard';
import { cn } from '@/lib/utils';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';

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
    <div className="flex w-[300px] flex-shrink-0 flex-col">
      {/* Column Header */}
      <div
        className={cn(
          'rounded-xl px-4 py-3 font-semibold text-white mb-4 flex items-center justify-between',
          columnStyles[id]
        )}
      >
        <span className="text-sm">{title}</span>
        <Tooltip>
          <TooltipTrigger asChild>
            <button className="p-0.5 rounded-full hover:bg-white/20 transition-colors">
              <HelpCircle className="h-4 w-4 text-white/80" />
            </button>
          </TooltipTrigger>
          <TooltipContent side="top" className="max-w-[250px] text-xs">
            {description}
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
              snapshot.isDraggingOver ? 'bg-muted/50' : 'bg-transparent'
            )}
          >
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
