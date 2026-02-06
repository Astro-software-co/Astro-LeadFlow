import { Droppable, Draggable } from '@hello-pangea/dnd';
import { KanbanCard as KanbanCardType, KanbanColumn as KanbanColumnType } from '@/types';
import { KanbanCardComponent } from './KanbanCard';
import { cn } from '@/lib/utils';

interface KanbanColumnProps {
  id: KanbanColumnType;
  title: string;
  cards: KanbanCardType[];
  onAssume?: (cardId: string) => void;
  onAdvance?: (cardId: string) => void;
  onArchive?: (cardId: string) => void;
  onCardClick?: (card: KanbanCardType) => void;
}

const columnStyles: Record<KanbanColumnType, string> = {
  chatbot: 'bg-kanban-chatbot',
  aguardando: 'bg-kanban-waiting',
  negociacao: 'bg-kanban-negotiation',
  concluido: 'bg-kanban-completed',
};

export function KanbanColumnComponent({
  id,
  title,
  cards,
  onAssume,
  onAdvance,
  onArchive,
  onCardClick,
}: KanbanColumnProps) {
  return (
    <div className="flex w-[340px] flex-shrink-0 flex-col">
      {/* Column Header */}
      <div
        className={cn(
          'rounded-xl px-4 py-3 text-center font-semibold text-white mb-4',
          columnStyles[id]
        )}
      >
        {title}
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
