import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { Plus, Search, Filter, Car, Loader2, Pencil } from 'lucide-react';
import { AddVehicleModal } from '@/components/modals/AddVehicleModal';
import { Skeleton } from '@/components/ui/skeleton';
import { Vehicle, VehicleStatus, VehicleCategory } from '@/types';
import { cn } from '@/lib/utils';
import { EmptyState } from '@/components/shared/EmptyState';
import { useCreateVehicle, useUpdateVehicle, useVehicles } from '@/hooks/api/useVehicles';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { formatCurrency } from '@/lib/formatters';
import { VehicleCategoryDTO, VehicleStatusDTO } from '@/types/api/vehicles';

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

const categoryToDTOMap: Record<VehicleCategory, VehicleCategoryDTO> = {
  SUV: 'SUV',
  Sedan: 'SEDAN',
  Hatch: 'HATCH',
  Pickup: 'PICKUP',
  Outros: 'COUPE',
};

type PriceRangeFilter = 'all' | 'up-to-100k' | '100k-200k' | '200k-300k' | 'above-300k';

const categoryOptions: Array<{ value: 'all' | VehicleCategory; label: string }> = [
  { value: 'all', label: 'Todas as categorias' },
  { value: 'SUV', label: 'SUV' },
  { value: 'Sedan', label: 'Sedan' },
  { value: 'Hatch', label: 'Hatch' },
  { value: 'Pickup', label: 'Pickup' },
  { value: 'Outros', label: 'Outros' },
];

const priceRangeOptions: Array<{ value: PriceRangeFilter; label: string }> = [
  { value: 'all', label: 'Todas as faixas de preco' },
  { value: 'up-to-100k', label: 'Ate R$ 100.000' },
  { value: '100k-200k', label: 'R$ 100.000 a R$ 200.000' },
  { value: '200k-300k', label: 'R$ 200.000 a R$ 300.000' },
  { value: 'above-300k', label: 'Acima de R$ 300.000' },
];

function getPriceRangeParams(range: PriceRangeFilter): { minPrice?: number; maxPrice?: number } {
  switch (range) {
    case 'up-to-100k':
      return { maxPrice: 100_000 };
    case '100k-200k':
      return { minPrice: 100_000, maxPrice: 200_000 };
    case '200k-300k':
      return { minPrice: 200_000, maxPrice: 300_000 };
    case 'above-300k':
      return { minPrice: 300_000 };
    case 'all':
    default:
      return {};
  }
}

export default function Estoque() {
  const pageSize = 10;
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<VehicleStatus | 'all'>('all');
  const [categoryFilter, setCategoryFilter] = useState<'all' | VehicleCategory>('all');
  const [yearFilter, setYearFilter] = useState<'all' | number>('all');
  const [priceRangeFilter, setPriceRangeFilter] = useState<PriceRangeFilter>('all');
  const [page, setPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);
  const priceRangeParams = useMemo(() => getPriceRangeParams(priceRangeFilter), [priceRangeFilter]);
  const yearOptions = useMemo(() => {
    const currentYear = new Date().getFullYear() + 1;
    const years: number[] = [];

    for (let year = currentYear; year >= 1990; year -= 1) {
      years.push(year);
    }

    return years;
  }, []);

  const filters = useMemo(
    () => ({
      search: searchQuery.trim() || undefined,
      status: statusFilter === 'all' ? undefined : statusToDTOMap[statusFilter],
      category: categoryFilter === 'all' ? undefined : categoryToDTOMap[categoryFilter],
      year: yearFilter === 'all' ? undefined : yearFilter,
      minPrice: priceRangeParams.minPrice,
      maxPrice: priceRangeParams.maxPrice,
      page: page - 1,
      size: pageSize,
    }),
    [searchQuery, statusFilter, categoryFilter, yearFilter, priceRangeParams, page],
  );

  const { data, isLoading, isFetching, error, refetch } = useVehicles(filters);
  const createVehicle = useCreateVehicle();
  const updateVehicle = useUpdateVehicle();

  const vehicles = useMemo(() => data?.content ?? [], [data]);
  const totalElements = data?.totalElements ?? 0;
  const totalPages = Math.max(1, data?.totalPages ?? 1);
  const safePage = Math.min(page, totalPages);
  const hasAnyFilter =
    !!searchQuery.trim() || statusFilter !== 'all' || categoryFilter !== 'all' || yearFilter !== 'all' ||
    priceRangeFilter !== 'all';

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const openCreateModal = () => {
    setSelectedVehicle(null);
    setIsModalOpen(true);
  };

  const openEditModal = (vehicle: Vehicle) => {
    setSelectedVehicle(vehicle);
    setIsModalOpen(true);
  };

  const handleSubmitVehicle = async (vehicleData: Omit<Vehicle, 'id'>) => {
    if (selectedVehicle) {
      await updateVehicle.mutateAsync({ id: selectedVehicle.id, data: vehicleData });
      return;
    }

    await createVehicle.mutateAsync(vehicleData);
  };

  if (isLoading && !data) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <Skeleton className="h-8 w-48" />
            <Skeleton className="mt-2 h-4 w-56" />
          </div>
          <Skeleton className="h-10 w-40" />
        </div>
        <div className="rounded-xl border bg-card shadow-sm overflow-hidden">
          <table className="hidden md:table w-full">
            <thead>
              <tr className="border-b bg-muted/30">
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Marca</th>
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Modelo</th>
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Ano</th>
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Categoria</th>
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Preco</th>
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
                <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Acoes</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-20" /></td>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-28" /></td>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-12" /></td>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-16" /></td>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-24" /></td>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-20 rounded-full" /></td>
                  <td className="px-6 py-4"><Skeleton className="h-4 w-16" /></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="block md:hidden space-y-4 p-4">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="rounded-xl border bg-card p-4 shadow-sm space-y-3">
                <div className="flex items-start justify-between">
                  <Skeleton className="h-5 w-40" />
                  <Skeleton className="h-5 w-20 rounded-full" />
                </div>
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-5 w-28" />
              </div>
            ))}
          </div>
        </div>
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
          <Button onClick={openCreateModal}>
            <Plus className="h-4 w-4" />
            Adicionar Veiculo
          </Button>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Buscar por marca ou modelo..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(1);
              }}
              className="pl-10 pr-10"
            />
            {isFetching ? (
              <Loader2 className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
            ) : null}
          </div>

          <div className="relative">
            <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={statusFilter}
              onValueChange={(value) => {
                setStatusFilter(value as VehicleStatus | 'all');
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todos os status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Todos os status</SelectItem>
                <SelectItem value="disponivel">Disponivel</SelectItem>
                <SelectItem value="reservado">Reservado</SelectItem>
                <SelectItem value="manutencao">Em Manutencao</SelectItem>
                <SelectItem value="vendido">Vendido</SelectItem>
                <SelectItem value="aguardando_documentacao">Aguardando Documentacao</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="relative">
            <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={categoryFilter}
              onValueChange={(value) => {
                setCategoryFilter(value as 'all' | VehicleCategory);
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todas as categorias" />
              </SelectTrigger>
              <SelectContent>
                {categoryOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="relative">
            <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={yearFilter === 'all' ? 'all' : String(yearFilter)}
              onValueChange={(value) => {
                setYearFilter(value === 'all' ? 'all' : Number(value));
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todos os anos" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Todos os anos</SelectItem>
                {yearOptions.map((year) => (
                  <SelectItem key={year} value={String(year)}>
                    {year}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="relative">
            <Filter className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Select
              value={priceRangeFilter}
              onValueChange={(value) => {
                setPriceRangeFilter(value as PriceRangeFilter);
                setPage(1);
              }}
            >
              <SelectTrigger className="pl-10">
                <SelectValue placeholder="Todas as faixas de preco" />
              </SelectTrigger>
              <SelectContent>
                {priceRangeOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="rounded-xl border bg-card shadow-sm animate-fade-in overflow-hidden">
          <div className="hidden md:block overflow-x-auto">
            <table className="hidden md:table w-full">
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
                          hasAnyFilter
                            ? 'Nenhum resultado encontrado'
                            : 'Nenhum veiculo cadastrado'
                        }
                        description={
                          hasAnyFilter
                            ? 'Tente alterar os filtros de busca.'
                            : 'Adicione seu primeiro veiculo ao estoque.'
                        }
                        actionLabel={!hasAnyFilter ? 'Adicionar veiculo' : undefined}
                        onAction={!hasAnyFilter ? openCreateModal : undefined}
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
                          <Button size="sm" variant="outline" onClick={() => openEditModal(vehicle)}>
                            <Pencil className="mr-1 h-3.5 w-3.5" />
                            Editar
                          </Button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Mobile card view */}
          <div className="block md:hidden divide-y">
            {vehicles.length === 0 ? (
              <div className="p-4">
                <EmptyState
                  icon={hasAnyFilter ? Search : Car}
                  title={hasAnyFilter ? 'Nenhum resultado encontrado' : 'Nenhum veiculo cadastrado'}
                  description={
                    hasAnyFilter
                      ? 'Tente alterar os filtros de busca.'
                      : 'Adicione seu primeiro veiculo ao estoque.'
                  }
                  actionLabel={!hasAnyFilter ? 'Adicionar veiculo' : undefined}
                  onAction={!hasAnyFilter ? openCreateModal : undefined}
                />
              </div>
            ) : (
              vehicles.map((vehicle) => {
                const status = statusConfig[vehicle.status];
                return (
                  <div key={vehicle.id} className="p-4 space-y-1">
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-base font-semibold text-foreground truncate">
                        {vehicle.marca} {vehicle.modelo}
                      </p>
                      <span className={cn('inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-medium', status.className)}>
                        {status.label}
                      </span>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {vehicle.ano}{vehicle.cor ? ` \u00B7 ${vehicle.cor}` : ''}
                    </p>
                    <p className="text-lg font-semibold text-foreground">{formatCurrency(vehicle.preco)}</p>
                    <div className="flex justify-end pt-2">
                      <Button variant="ghost" size="sm" className="py-2" onClick={() => openEditModal(vehicle)}>
                        Ver detalhes
                      </Button>
                    </div>
                  </div>
                );
              })
            )}
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
            <p className="text-sm text-muted-foreground">Disponiveis (visiveis)</p>
          </div>
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'reservado').length}
            </p>
            <p className="text-sm text-muted-foreground">Reservados (visiveis)</p>
          </div>
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'manutencao').length}
            </p>
            <p className="text-sm text-muted-foreground">Em Manutencao (visiveis)</p>
          </div>
          <div className="rounded-xl border bg-card p-4 text-center">
            <p className="text-2xl font-bold text-foreground">
              {vehicles.filter((v) => v.status === 'vendido').length}
            </p>
            <p className="text-sm text-muted-foreground">Vendidos (visiveis)</p>
          </div>
        </div>
      </div>

      <AddVehicleModal
        open={isModalOpen}
        onOpenChange={(open) => {
          setIsModalOpen(open);
          if (!open) {
            setSelectedVehicle(null);
          }
        }}
        onSubmit={handleSubmitVehicle}
        vehicle={selectedVehicle}
      />
    </>
  );
}
