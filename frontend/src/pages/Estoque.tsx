import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { Plus, Search, Filter, Car, Loader2 } from 'lucide-react';
import { AddVehicleModal } from '@/components/modals/AddVehicleModal';
import { Vehicle, VehicleStatus } from '@/types';
import { cn } from '@/lib/utils';
import { EmptyState } from '@/components/shared/EmptyState';
import { useCreateVehicle, useVehicles } from '@/hooks/api/useVehicles';
import { Button } from '@/components/ui/button';
import { formatCurrency } from '@/lib/formatters';
import { VehicleStatusDTO } from '@/types/api/vehicles';

const statusConfig: Record<VehicleStatus, { label: string; className: string }> = {
  disponivel: { label: 'Disponivel', className: 'bg-status-available/10 text-status-available' },
  reservado: { label: 'Reservado', className: 'bg-status-reserved/10 text-status-reserved' },
  manutencao: { label: 'Em Manutencao', className: 'bg-status-maintenance/10 text-status-maintenance' },
  vendido: { label: 'Vendido', className: 'bg-status-sold/10 text-status-sold' },
  aguardando_documentacao: { label: 'Aguardando Doc.', className: 'bg-kanban-waiting/10 text-kanban-waiting' },
};

const statusToDTOMap: Record<VehicleStatus, VehicleStatusDTO> = {
  disponivel: 'AVAILABLE',
  reservado: 'RESERVED',
  manutencao: 'MAINTENANCE',
  vendido: 'SOLD',
  aguardando_documentacao: 'RESERVED',
};

export default function Estoque() {
  const pageSize = 10;
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<VehicleStatus | 'all'>('all');
  const [page, setPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const filters = useMemo(
    () => ({
      search: searchQuery.trim() || undefined,
      status: statusFilter === 'all' ? undefined : statusToDTOMap[statusFilter],
      page: page - 1,
      size: pageSize,
    }),
    [searchQuery, statusFilter, page],
  );

  const { data, isLoading, error, refetch } = useVehicles(filters);
  const createVehicle = useCreateVehicle();

  const vehicles = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const safePage = Math.min(page, totalPages);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const handleAddVehicle = async (vehicleData: Omit<Vehicle, 'id'>) => {
    await createVehicle.mutateAsync(vehicleData);
  };

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
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar o estoque</h2>
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
        <title>Estoque — Astro LeadFlow</title>
        <meta name="description" content="Catalogo de veiculos da concessionaria." />
      </Helmet>
      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Gestao de Estoque</h1>
            <p className="text-muted-foreground">Gerencie os veiculos disponiveis</p>
          </div>
          <button
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            <Plus className="h-4 w-4" />
            Adicionar Veiculo
          </button>
        </div>

        <div className="flex flex-col gap-4 sm:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="text"
              placeholder="Buscar por marca ou modelo..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(1);
              }}
              className="w-full rounded-lg border bg-card pl-10 pr-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as VehicleStatus | 'all');
                setPage(1);
              }}
              className="appearance-none rounded-lg border bg-card pl-10 pr-8 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="all">Todos os status</option>
              <option value="disponivel">Disponivel</option>
              <option value="reservado">Reservado</option>
              <option value="manutencao">Em Manutencao</option>
              <option value="vendido">Vendido</option>
              <option value="aguardando_documentacao">Aguardando Documentacao</option>
            </select>
          </div>
        </div>

        <div className="rounded-xl border bg-card shadow-sm animate-fade-in overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b bg-muted/30">
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Marca
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Modelo
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Ano
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Categoria
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Preco
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Status
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Acoes
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {vehicles.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-6 py-4">
                      <EmptyState
                        icon={Car}
                        title={
                          searchQuery || statusFilter !== 'all'
                            ? 'Nenhum resultado encontrado'
                            : 'Nenhum veiculo cadastrado'
                        }
                        description={
                          searchQuery || statusFilter !== 'all'
                            ? 'Tente alterar os filtros de busca.'
                            : 'Adicione seu primeiro veiculo ao estoque.'
                        }
                        actionLabel={!searchQuery && statusFilter === 'all' ? 'Adicionar veiculo' : undefined}
                        onAction={!searchQuery && statusFilter === 'all' ? () => setIsModalOpen(true) : undefined}
                      />
                    </td>
                  </tr>
                ) : (
                  vehicles.map((vehicle) => {
                    const status = statusConfig[vehicle.status];
                    return (
                      <tr key={vehicle.id} className="hover:bg-muted/20 transition-colors">
                        <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-foreground">
                          {vehicle.marca}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{vehicle.modelo}</td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{vehicle.ano}</td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">
                          {vehicle.categoria}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-foreground">
                          {formatCurrency(vehicle.preco)}
                        </td>
                        <td className="whitespace-nowrap px-6 py-4">
                          <span
                            className={cn(
                              'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
                              status.className,
                            )}
                          >
                            {status.label}
                          </span>
                        </td>
                        <td className="whitespace-nowrap px-6 py-4">
                          <button className="text-sm font-medium text-primary hover:text-primary/80 transition-colors">
                            Editar
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="flex items-center justify-between">
          <p className="text-xs text-muted-foreground">
            Mostrando {totalElements === 0 ? 0 : (safePage - 1) * pageSize + 1} a{' '}
            {totalElements === 0 ? 0 : (safePage - 1) * pageSize + vehicles.length} de {totalElements} veiculos
          </p>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" disabled={safePage <= 1} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </Button>
            <span className="text-xs text-muted-foreground">
              Pagina {safePage} de {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={safePage >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Proxima
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'disponivel').length}
            </p>
            <p className="text-sm text-muted-foreground">Disponiveis (pagina)</p>
          </div>
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'reservado').length}
            </p>
            <p className="text-sm text-muted-foreground">Reservados (pagina)</p>
          </div>
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'manutencao').length}
            </p>
            <p className="text-sm text-muted-foreground">Em Manutencao (pagina)</p>
          </div>
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'vendido').length}
            </p>
            <p className="text-sm text-muted-foreground">Vendidos (pagina)</p>
          </div>
        </div>
      </div>

      <AddVehicleModal open={isModalOpen} onOpenChange={setIsModalOpen} onSubmit={handleAddVehicle} />
    </>
  );
}
