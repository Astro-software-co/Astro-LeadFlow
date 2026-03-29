import { Helmet } from 'react-helmet-async';
import { MainLayout } from '@/components/layout/MainLayout';
import { KanbanBoard } from '@/components/kanban/KanbanBoard';
import { mockKanbanCards } from '@/data/mockData';

export default function Kanban() {
  return (
    <MainLayout>
      <Helmet>
        <title>Kanban — Astro LeadFlow</title>
        <meta name="description" content="Pipeline visual de vendas. Arraste leads entre etapas." />
      </Helmet>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Painel de Controle</h1>
          <p className="text-muted-foreground">Gerencie seus leads e atendimentos</p>
        </div>
        <KanbanBoard initialCards={mockKanbanCards} />
      </div>
    </MainLayout>
  );
}
