import { useState } from 'react';
import { DragDropContext, DropResult } from '@hello-pangea/dnd';
import { KanbanCard, KanbanColumn } from '@/types';
import { KanbanColumnComponent } from './KanbanColumn';
import { CardDetailModal } from '@/components/modals/CardDetailModal';
import { toast } from 'sonner';
import { useNotifications } from '@/contexts/NotificationContext';

interface KanbanBoardProps {
  initialCards: KanbanCard[];
}

const columns: { id: KanbanColumn; title: string }[] = [
  { id: 'chatbot', title: 'Falando com Chatbot' },
  { id: 'aguardando', title: 'Aguardando atendimento' },
  { id: 'negociacao', title: 'Em negociação' },
  { id: 'concluido', title: 'Concluído' },
];

export function KanbanBoard({ initialCards }: KanbanBoardProps) {
  const [cards, setCards] = useState<KanbanCard[]>(initialCards);
  const [selectedCard, setSelectedCard] = useState<KanbanCard | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const { addNotification } = useNotifications();

  const getCardsByColumn = (column: KanbanColumn) => {
    return cards.filter((card) => card.coluna === column);
  };

  const handleDragEnd = (result: DropResult) => {
    const { destination, source, draggableId } = result;

    if (!destination) return;

    if (destination.droppableId === source.droppableId && destination.index === source.index) {
      return;
    }

    const newColumn = destination.droppableId as KanbanColumn;
    const card = cards.find((c) => c.id === draggableId);
    
    setCards((prevCards) =>
      prevCards.map((card) =>
        card.id === draggableId ? { ...card, coluna: newColumn } : card
      )
    );

    if (card) {
      addNotification({
        title: 'Card movido',
        message: `${card.lead.nome} foi movido para ${columns.find(c => c.id === newColumn)?.title}`,
        type: 'info',
      });
    }

    toast.success('Card movido com sucesso!');
  };

  const handleCardClick = (card: KanbanCard) => {
    setSelectedCard(card);
    setIsDetailModalOpen(true);
  };

  const handleCardUpdate = (updatedCard: KanbanCard) => {
    setCards((prevCards) =>
      prevCards.map((card) =>
        card.id === updatedCard.id ? updatedCard : card
      )
    );
  };

  const handleAssume = (cardId: string) => {
    const card = cards.find((c) => c.id === cardId);
    setCards((prevCards) =>
      prevCards.map((card) =>
        card.id === cardId
          ? {
              ...card,
              coluna: 'negociacao',
              vendedorId: 'current-user',
              vendedorNome: 'Você',
              tags: [
                ...card.tags,
                { type: 'seller' as const, label: 'Você' },
              ],
            }
          : card
      )
    );
    
    if (card) {
      addNotification({
        title: 'Lead assumido',
        message: `Você assumiu o lead de ${card.lead.nome}`,
        type: 'success',
      });
    }
    
    toast.success('Você assumiu este lead!');
  };

  const handleAdvance = (cardId: string) => {
    const card = cards.find((c) => c.id === cardId);
    if (!card) return;

    const columnOrder: KanbanColumn[] = ['chatbot', 'aguardando', 'negociacao', 'concluido'];
    const currentIndex = columnOrder.indexOf(card.coluna);
    const nextColumn = columnOrder[currentIndex + 1];

    if (nextColumn) {
      setCards((prevCards) =>
        prevCards.map((c) =>
          c.id === cardId ? { ...c, coluna: nextColumn } : c
        )
      );
      
      if (nextColumn === 'concluido') {
        addNotification({
          title: 'Venda concluída!',
          message: `Lead de ${card.lead.nome} foi finalizado com sucesso`,
          type: 'success',
        });
      }
      
      toast.success('Card avançado!');
    }
  };

  const handleArchive = (cardId: string) => {
    const card = cards.find((c) => c.id === cardId);
    setCards((prevCards) => prevCards.filter((card) => card.id !== cardId));
    
    if (card) {
      addNotification({
        title: 'Card arquivado',
        message: `Lead de ${card.lead.nome} foi arquivado`,
        type: 'info',
      });
    }
    
    toast.success('Card arquivado!');
  };

  return (
    <>
      <DragDropContext onDragEnd={handleDragEnd}>
        <div className="flex gap-6 overflow-x-auto pb-4">
          {columns.map((column) => (
            <KanbanColumnComponent
              key={column.id}
              id={column.id}
              title={column.title}
              cards={getCardsByColumn(column.id)}
              onAssume={handleAssume}
              onAdvance={handleAdvance}
              onArchive={handleArchive}
              onCardClick={handleCardClick}
            />
          ))}
        </div>
      </DragDropContext>

      {selectedCard && (
        <CardDetailModal
          open={isDetailModalOpen}
          onOpenChange={setIsDetailModalOpen}
          card={selectedCard}
          onUpdate={handleCardUpdate}
        />
      )}
    </>
  );
}
