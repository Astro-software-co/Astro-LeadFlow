import { useEffect, useState } from 'react';
import { DragDropContext, DropResult } from '@hello-pangea/dnd';
import { KanbanCard, KanbanColumn } from '@/types';
import { KanbanColumnComponent } from './KanbanColumn';
import { CardDetailModal } from '@/components/modals/CardDetailModal';
import { toast } from 'sonner';
import { useNotifications } from '@/contexts/NotificationContext';
import { TooltipProvider } from '@/components/ui/tooltip';

interface KanbanBoardProps {
  initialCards: KanbanCard[];
  onMoveLead?: (leadId: string, stage: KanbanColumn) => Promise<void> | void;
  onArchiveLead?: (leadId: string) => Promise<void> | void;
}

const columns: { id: KanbanColumn; title: string; description: string }[] = [
  {
    id: 'chatbot',
    title: 'Falando com Chatbot',
    description:
      'Leads que estão interagindo com o chatbot automatizado. Os dados estão sendo coletados antes do atendimento humano.',
  },
  {
    id: 'aguardando',
    title: 'Aguardando atendimento',
    description: 'Leads qualificados aguardando um vendedor assumir o atendimento. Clique em "Assumir" para iniciar.',
  },
  {
    id: 'aguardando_documentacao',
    title: 'Aguardando documentação',
    description: 'Leads que já foram atendidos mas aguardam envio ou verificação de documentos para prosseguir.',
  },
  {
    id: 'negociacao',
    title: 'Em negociação',
    description:
      'Leads em processo ativo de negociação com um vendedor. Inclui propostas, contrapropostas e financiamento.',
  },
  {
    id: 'concluido',
    title: 'Concluído',
    description: 'Vendas finalizadas com sucesso. O veículo foi entregue e a documentação concluída.',
  },
];

export function KanbanBoard({ initialCards, onMoveLead, onArchiveLead }: KanbanBoardProps) {
  const [cards, setCards] = useState<KanbanCard[]>(initialCards);
  const [selectedCard, setSelectedCard] = useState<KanbanCard | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const { addNotification } = useNotifications();

  useEffect(() => {
    setCards(initialCards);
  }, [initialCards]);

  const getCardsByColumn = (column: KanbanColumn) => cards.filter((card) => card.coluna === column);

  const handleDragEnd = (result: DropResult) => {
    const { destination, source, draggableId } = result;
    if (!destination) return;
    if (destination.droppableId === source.droppableId && destination.index === source.index) return;

    const newColumn = destination.droppableId as KanbanColumn;
    const card = cards.find((c) => c.id === draggableId);

    const previousCards = cards;
    setCards((prevCards) => prevCards.map((item) => (item.id === draggableId ? { ...item, coluna: newColumn } : item)));

    if (card && onMoveLead) {
      void Promise.resolve(onMoveLead(card.leadId, newColumn)).catch(() => {
        setCards(previousCards);
        toast.error('Nao foi possivel mover o lead. Atualize a pagina e tente novamente.');
      });
    }

    if (card) {
      addNotification({
        title: 'Card movido',
        message: `${card.lead.nome} foi movido para ${columns.find((c) => c.id === newColumn)?.title}`,
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
    setCards((prevCards) => prevCards.map((card) => (card.id === updatedCard.id ? updatedCard : card)));
  };

  const handleAssume = (cardId: string) => {
    const card = cards.find((c) => c.id === cardId);
    const previousCards = cards;
    setCards((prevCards) =>
      prevCards.map((item) =>
        item.id === cardId
          ? {
              ...item,
              coluna: 'negociacao',
              vendedorId: 'current-user',
              vendedorNome: 'Você',
              tags: [...item.tags, { type: 'seller' as const, label: 'Você' }],
            }
          : item,
      ),
    );

    if (card && onMoveLead) {
      void Promise.resolve(onMoveLead(card.leadId, 'negociacao')).catch(() => {
        setCards(previousCards);
        toast.error('Nao foi possivel assumir o lead agora.');
      });
    }

    if (card) {
      addNotification({ title: 'Lead assumido', message: `Você assumiu o lead de ${card.lead.nome}`, type: 'success' });
    }
    toast.success('Você assumiu este lead!');
  };

  const handleAdvance = (cardId: string) => {
    const card = cards.find((c) => c.id === cardId);
    if (!card) return;
    const columnOrder: KanbanColumn[] = ['chatbot', 'aguardando', 'aguardando_documentacao', 'negociacao', 'concluido'];
    const currentIndex = columnOrder.indexOf(card.coluna);
    const nextColumn = columnOrder[currentIndex + 1];
    if (nextColumn) {
      const previousCards = cards;
      setCards((prevCards) => prevCards.map((item) => (item.id === cardId ? { ...item, coluna: nextColumn } : item)));

      if (onMoveLead) {
        void Promise.resolve(onMoveLead(card.leadId, nextColumn)).catch(() => {
          setCards(previousCards);
          toast.error('Nao foi possivel avancar o lead agora.');
        });
      }

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
    const previousCards = cards;

    if (card && onArchiveLead) {
      void Promise.resolve(onArchiveLead(card.leadId)).catch(() => {
        setCards(previousCards);
        toast.error('Nao foi possivel arquivar o lead agora.');
      });
    }

    setCards((prevCards) => prevCards.filter((card) => card.id !== cardId));
    if (card) {
      addNotification({ title: 'Card arquivado', message: `Lead de ${card.lead.nome} foi arquivado`, type: 'info' });
    }
    toast.success('Card arquivado!');
  };

  return (
    <>
      <DragDropContext onDragEnd={handleDragEnd}>
        <TooltipProvider delayDuration={150}>
          <div className="relative">
            <div className="flex gap-4 overflow-x-auto pb-4">
            {columns.map((column) => (
              <KanbanColumnComponent
                key={column.id}
                id={column.id}
                title={column.title}
                description={column.description}
                cards={getCardsByColumn(column.id)}
                onAssume={handleAssume}
                onAdvance={handleAdvance}
                onArchive={handleArchive}
                onCardClick={handleCardClick}
              />
            ))}
            </div>
            <div className="pointer-events-none absolute right-0 top-0 h-full w-8 bg-gradient-to-l from-background to-transparent md:hidden" />
          </div>
        </TooltipProvider>
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
