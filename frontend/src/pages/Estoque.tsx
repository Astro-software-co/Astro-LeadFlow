import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { Link } from 'react-router-dom';
import { Plus, Search, Car, Loader2, Pencil, SlidersHorizontal } from 'lucide-react';
import { AddVehicleModal } from '@/components/modals/AddVehicleModal';
import { Skeleton } from '@/components/ui/skeleton';
import { Vehicle, VehicleStatus } from '@/types';
import { cn } from '@/lib/utils';
import { EmptyState } from '@/components/shared/EmptyState';
import { useCreateVehicle, useUpdateVehicle, useVehicles } from '@/hooks/api/useVehicles';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { formatCurrency } from '@/lib/formatters';

type FacetStatus = Extract<VehicleStatus, 'disponivel' | 'reservado' | 'vendido'>;

type YearFilter = 'all' | number;

const pageSize = 10;

const statusConfig: Record<VehicleStatus, { label: string; className: string }> = {
  disponivel: { label: 'Disponivel', className: 'bg-status-available/10 text-status-available' },
  reservado: { label: 'Reservado', className: 'bg-status-reserved/10 text-status-reserved' },
  manutencao: { label: 'Em manutencao', className: 'bg-status-maintenance/10 text-status-maintenance' },
  vendido: { label: 'Vendido', className: 'bg-status-sold/10 text-status-sold' },
  aguardando_documentacao: { label: 'Aguardando doc.', className: 'bg-blue-100 text-blue-700' },
};

const statusFacetOptions: Array<{ value: FacetStatus; label: string }> = [
  { value: 'disponivel', label: 'Disponivel' },
  { value: 'reservado', label: 'Reservado' },
  { value: 'vendido', label: 'Vendido' },
];

const defaultStatusFilters: FacetStatus[] = ['disponivel'];

function parsePriceInput(value: string): number | undefined {
  if (!value.trim()) {
    return undefined;
  }

  const parsed = Number(value.replace(',', '.'));
  return Number.isFinite(parsed) ? parsed : undefined;
}

interface VehicleFiltersPanelProps {
  searchQuery: string;
  onSearchChange: (value: string) => void;
  brands: string[];
  brandFilter: string;
  onBrandChange: (value: string) => void;
  models: string[];
  modelFilter: string;
  onModelChange: (value: string) => void;
  minPriceDraft: string;
  maxPriceDraft: string;
  onMinPriceDraftChange: (value: string) => void;
  onMaxPriceDraftChange: (value: string) => void;
  onApplyPriceRange: () => void;
  years: number[];
  yearFromFilter: YearFilter;
  yearToFilter: YearFilter;
  onYearFromChange: (value: YearFilter) => void;
  onYearToChange: (value: YearFilter) => void;
  statusFilters: FacetStatus[];
  onToggleStatus: (status: FacetStatus, checked: boolean) => void;
  onClearFilters: () => void;
}

function VehicleFiltersPanel({
  searchQuery,
  onSearchChange,
  brands,
  brandFilter,
  onBrandChange,
  models,
  modelFilter,
  onModelChange,
  minPriceDraft,
  maxPriceDraft,
  onMinPriceDraftChange,
  onMaxPriceDraftChange,
  onApplyPriceRange,
  years,
  yearFromFilter,
  yearToFilter,
  onYearFromChange,
  onYearToChange,
  statusFilters,
  onToggleStatus,
  onClearFilters,
}: VehicleFiltersPanelProps) {
  return (
    <div className="space-y-5 rounded-xl border bg-card p-4 shadow-sm">
      <div className="space-y-2">
        <p className="text-sm font-semibold text-foreground">Busca</p>
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Marca ou modelo"
            className="pl-10"
          />
        </div>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-semibold text-foreground">Marca</p>
        <Select value={brandFilter} onValueChange={onBrandChange}>
          <SelectTrigger>
            <SelectValue placeholder="Todas as marcas" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todas as marcas</SelectItem>
            {brands.map((brand) => (
              <SelectItem key={brand} value={brand}>
                {brand}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-semibold text-foreground">Modelo</p>
        <Select value={modelFilter} onValueChange={onModelChange} disabled={brandFilter === 'all'}>
          <SelectTrigger>
            <SelectValue placeholder={brandFilter === 'all' ? 'Selecione uma marca primeiro' : 'Todos os modelos'} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todos os modelos</SelectItem>
            {models.map((model) => (
              <SelectItem key={model} value={model}>
                {model}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-semibold text-foreground">Faixa de preco</p>
        <div className="grid grid-cols-2 gap-2">
          <div className="relative">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground">
              R$
            </span>
            <Input
              type="number"
              inputMode="decimal"
              min={0}
              placeholder="Preco min."
              value={minPriceDraft}
              onChange={(event) => onMinPriceDraftChange(event.target.value)}
              onBlur={onApplyPriceRange}
              className="pl-9"
            />
          </div>
          <div className="relative">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground">
              R$
            </span>
            <Input
              type="number"
              inputMode="decimal"
              min={0}
              placeholder="Preco max."
              value={maxPriceDraft}
              onChange={(event) => onMaxPriceDraftChange(event.target.value)}
              onBlur={onApplyPriceRange}
              className="pl-9"
            />
          </div>
        </div>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-semibold text-foreground">Ano</p>
        <div className="grid grid-cols-2 gap-2">
          <Select
            value={yearFromFilter === 'all' ? 'all' : String(yearFromFilter)}
            onValueChange={(value) => onYearFromChange(value === 'all' ? 'all' : Number(value))}
          >
            <SelectTrigger>
              <SelectValue placeholder="Ano de" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Ano de</SelectItem>
              {years.map((year) => (
                <SelectItem key={`from-${year}`} value={String(year)}>
                  {year}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select
            value={yearToFilter === 'all' ? 'all' : String(yearToFilter)}
            onValueChange={(value) => onYearToChange(value === 'all' ? 'all' : Number(value))}
          >
            <SelectTrigger>
              <SelectValue placeholder="Ano ate" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Ano ate</SelectItem>
              {years.map((year) => (
                <SelectItem key={`to-${year}`} value={String(year)}>
                  {year}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="space-y-2">
        <p className="text-sm font-semibold text-foreground">Status</p>
        <div className="space-y-2 rounded-lg border p-3">
          {statusFacetOptions.map((option) => (
            <label key={option.value} className="flex cursor-pointer items-center gap-2 text-sm text-foreground">
              <Checkbox
                checked={statusFilters.includes(option.value)}
                onCheckedChange={(checked) => onToggleStatus(option.value, checked === true)}
              />
              {option.label}
            </label>
          ))}
          <p className="text-xs text-muted-foreground">Se nenhum estiver marcado, todos os status serao exibidos.</p>
        </div>
      </div>

      <Button type="button" variant="outline" className="w-full" onClick={onClearFilters}>
        Limpar filtros
      </Button>
    </div>
  );
}

export default function Estoque() {
  const [searchQuery, setSearchQuery] = useState('');
  const [brandFilter, setBrandFilter] = useState<string>('all');
  const [modelFilter, setModelFilter] = useState<string>('all');
  const [yearFromFilter, setYearFromFilter] = useState<YearFilter>('all');
  const [yearToFilter, setYearToFilter] = useState<YearFilter>('all');
  const [statusFilters, setStatusFilters] = useState<FacetStatus[]>(defaultStatusFilters);
  const [minPriceDraft, setMinPriceDraft] = useState('');
  const [maxPriceDraft, setMaxPriceDraft] = useState('');
  const [minPrice, setMinPrice] = useState<number | undefined>(undefined);
  const [maxPrice, setMaxPrice] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(1);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);

  const { data, isLoading, isFetching, error, refetch } = useVehicles({ page: 0, size: 500 });
  const createVehicle = useCreateVehicle();
  const updateVehicle = useUpdateVehicle();

  const allVehicles = data?.content ?? [];

  const brands = useMemo(
    () => [...new Set(allVehicles.map((vehicle) => vehicle.marca))].sort((a, b) => a.localeCompare(b)),
    [allVehicles],
  );

  const models = useMemo(() => {
    if (brandFilter === 'all') {
      return [];
    }

    return [...new Set(allVehicles.filter((vehicle) => vehicle.marca === brandFilter).map((vehicle) => vehicle.modelo))]
      .sort((a, b) => a.localeCompare(b));
  }, [allVehicles, brandFilter]);

  const years = useMemo(
    () => [...new Set(allVehicles.map((vehicle) => vehicle.ano))].sort((a, b) => b - a),
    [allVehicles],
  );

  useEffect(() => {
    if (brandFilter === 'all') {
      setModelFilter('all');
      return;
    }

    if (modelFilter !== 'all' && !models.includes(modelFilter)) {
      setModelFilter('all');
    }
  }, [brandFilter, modelFilter, models]);

  const applyPriceRange = () => {
    setMinPrice(parsePriceInput(minPriceDraft));
    setMaxPrice(parsePriceInput(maxPriceDraft));
  };

  const filteredVehicles = useMemo(() => {
    const normalizedSearch = searchQuery.trim().toLowerCase();

    const parsedYearFrom = yearFromFilter === 'all' ? undefined : yearFromFilter;
    const parsedYearTo = yearToFilter === 'all' ? undefined : yearToFilter;
    const yearMin =
      parsedYearFrom !== undefined && parsedYearTo !== undefined
        ? Math.min(parsedYearFrom, parsedYearTo)
        : parsedYearFrom;
    const yearMax =
      parsedYearFrom !== undefined && parsedYearTo !== undefined
        ? Math.max(parsedYearFrom, parsedYearTo)
        : parsedYearTo;

    const resolvedMinPrice =
      minPrice !== undefined && maxPrice !== undefined ? Math.min(minPrice, maxPrice) : minPrice;
    const resolvedMaxPrice =
      minPrice !== undefined && maxPrice !== undefined ? Math.max(minPrice, maxPrice) : maxPrice;

    return allVehicles.filter((vehicle) => {
      const matchesSearch =
        !normalizedSearch ||
        vehicle.marca.toLowerCase().includes(normalizedSearch) ||
        vehicle.modelo.toLowerCase().includes(normalizedSearch);
      const matchesBrand = brandFilter === 'all' || vehicle.marca === brandFilter;
      const matchesModel = modelFilter === 'all' || vehicle.modelo === modelFilter;
      const matchesPriceMin = resolvedMinPrice === undefined || vehicle.preco >= resolvedMinPrice;
      const matchesPriceMax = resolvedMaxPrice === undefined || vehicle.preco <= resolvedMaxPrice;
      const matchesYearFrom = yearMin === undefined || vehicle.ano >= yearMin;
      const matchesYearTo = yearMax === undefined || vehicle.ano <= yearMax;
      const matchesStatus =
        statusFilters.length === 0 || statusFilters.includes(vehicle.status as FacetStatus);

      return (
        matchesSearch &&
        matchesBrand &&
        matchesModel &&
        matchesPriceMin &&
        matchesPriceMax &&
        matchesYearFrom &&
        matchesYearTo &&
        matchesStatus
      );
    });
  }, [allVehicles, searchQuery, brandFilter, modelFilter, minPrice, maxPrice, yearFromFilter, yearToFilter, statusFilters]);

  const totalElements = filteredVehicles.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  useEffect(() => {
    setPage(1);
  }, [searchQuery, brandFilter, modelFilter, minPrice, maxPrice, yearFromFilter, yearToFilter, statusFilters]);

  const safePage = Math.min(page, totalPages);

  const pagedVehicles = useMemo(() => {
    const start = (safePage - 1) * pageSize;
    return filteredVehicles.slice(start, start + pageSize);
  }, [filteredVehicles, safePage]);

  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (searchQuery.trim()) count += 1;
    if (brandFilter !== 'all') count += 1;
    if (modelFilter !== 'all') count += 1;
    if (minPrice !== undefined) count += 1;
    if (maxPrice !== undefined) count += 1;
    if (yearFromFilter !== 'all') count += 1;
    if (yearToFilter !== 'all') count += 1;

    const isDefaultStatusSelection =
      statusFilters.length === defaultStatusFilters.length &&
      defaultStatusFilters.every((value) => statusFilters.includes(value));

    if (!isDefaultStatusSelection) {
      count += 1;
    }

    return count;
  }, [searchQuery, brandFilter, modelFilter, minPrice, maxPrice, yearFromFilter, yearToFilter, statusFilters]);

  const hasAnyFilter = activeFiltersCount > 0;

  const handleToggleStatus = (status: FacetStatus, checked: boolean) => {
    setStatusFilters((previous) => {
      if (checked) {
        return previous.includes(status) ? previous : [...previous, status];
      }

      return previous.filter((item) => item !== status);
    });
  };

  const clearFilters = () => {
    setSearchQuery('');
    setBrandFilter('all');
    setModelFilter('all');
    setYearFromFilter('all');
    setYearToFilter('all');
    setStatusFilters(defaultStatusFilters);
    setMinPriceDraft('');
    setMaxPriceDraft('');
    setMinPrice(undefined);
    setMaxPrice(undefined);
    setPage(1);
  };

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

        <div className="grid gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
          <Skeleton className="h-[520px] w-full rounded-xl" />
          <div className="space-y-4">
            <Skeleton className="h-10 w-44" />
            <div className="rounded-xl border bg-card p-4 shadow-sm">
              {Array.from({ length: 6 }).map((_, index) => (
                <Skeleton key={index} className="mb-3 h-12 w-full rounded-lg last:mb-0" />
              ))}
            </div>
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

        <div className="flex items-center justify-between rounded-xl border bg-card p-3 shadow-sm lg:hidden">
          <p className="text-sm text-muted-foreground">{totalElements} veiculos encontrados</p>
          <Button variant="outline" onClick={() => setIsFiltersOpen(true)}>
            <SlidersHorizontal className="h-4 w-4" />
            Filtros
            {activeFiltersCount > 0 ? <Badge className="ml-1 h-5 px-1.5">{activeFiltersCount}</Badge> : null}
          </Button>
        </div>

        <div className="grid gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
          <aside className="hidden lg:block">
            <VehicleFiltersPanel
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              brands={brands}
              brandFilter={brandFilter}
              onBrandChange={setBrandFilter}
              models={models}
              modelFilter={modelFilter}
              onModelChange={setModelFilter}
              minPriceDraft={minPriceDraft}
              maxPriceDraft={maxPriceDraft}
              onMinPriceDraftChange={setMinPriceDraft}
              onMaxPriceDraftChange={setMaxPriceDraft}
              onApplyPriceRange={applyPriceRange}
              years={years}
              yearFromFilter={yearFromFilter}
              yearToFilter={yearToFilter}
              onYearFromChange={setYearFromFilter}
              onYearToChange={setYearToFilter}
              statusFilters={statusFilters}
              onToggleStatus={handleToggleStatus}
              onClearFilters={clearFilters}
            />
          </aside>

          <div className="space-y-4">
            <div className="hidden items-center justify-between lg:flex">
              <p className="text-sm text-muted-foreground">{totalElements} veiculos encontrados</p>
              {isFetching ? <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" /> : null}
            </div>

            <div className="rounded-xl border bg-card shadow-sm overflow-hidden">
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b bg-muted/30">
                      <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        Veiculo
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
                    {pagedVehicles.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-6 py-4">
                          <EmptyState
                            icon={hasAnyFilter ? Search : Car}
                            title={hasAnyFilter ? 'Nenhum resultado encontrado' : 'Nenhum veiculo cadastrado'}
                            description={
                              hasAnyFilter ? 'Tente alterar os filtros de busca.' : 'Adicione seu primeiro veiculo ao estoque.'
                            }
                            actionLabel={!hasAnyFilter ? 'Adicionar veiculo' : undefined}
                            onAction={!hasAnyFilter ? openCreateModal : undefined}
                          />
                        </td>
                      </tr>
                    ) : (
                      pagedVehicles.map((vehicle) => {
                        const status = statusConfig[vehicle.status];
                        return (
                          <tr key={vehicle.id} className="hover:bg-muted/20 transition-colors">
                            <td className="whitespace-nowrap px-6 py-4 text-sm">
                              <Link
                                to={`/app/estoque/${vehicle.id}`}
                                className="font-medium text-foreground transition-colors hover:text-primary"
                              >
                                {vehicle.marca} {vehicle.modelo}
                              </Link>
                            </td>
                            <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{vehicle.ano}</td>
                            <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{vehicle.categoria}</td>
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
                              <div className="flex items-center gap-2">
                                <Button size="sm" variant="ghost" asChild>
                                  <Link to={`/app/estoque/${vehicle.id}`}>Detalhes</Link>
                                </Button>
                                <Button size="sm" variant="outline" onClick={() => openEditModal(vehicle)}>
                                  <Pencil className="mr-1 h-3.5 w-3.5" />
                                  Editar
                                </Button>
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>

              <div className="block md:hidden divide-y">
                {pagedVehicles.length === 0 ? (
                  <div className="p-4">
                    <EmptyState
                      icon={hasAnyFilter ? Search : Car}
                      title={hasAnyFilter ? 'Nenhum resultado encontrado' : 'Nenhum veiculo cadastrado'}
                      description={
                        hasAnyFilter ? 'Tente alterar os filtros de busca.' : 'Adicione seu primeiro veiculo ao estoque.'
                      }
                      actionLabel={!hasAnyFilter ? 'Adicionar veiculo' : undefined}
                      onAction={!hasAnyFilter ? openCreateModal : undefined}
                    />
                  </div>
                ) : (
                  pagedVehicles.map((vehicle) => {
                    const status = statusConfig[vehicle.status];
                    return (
                      <div key={vehicle.id} className="space-y-2 p-4">
                        <div className="flex items-start justify-between gap-2">
                          <Link
                            to={`/app/estoque/${vehicle.id}`}
                            className="truncate text-base font-semibold text-foreground transition-colors hover:text-primary"
                          >
                            {vehicle.marca} {vehicle.modelo}
                          </Link>
                          <span
                            className={cn(
                              'inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
                              status.className,
                            )}
                          >
                            {status.label}
                          </span>
                        </div>
                        <p className="text-sm text-muted-foreground">
                          {vehicle.ano}
                          {vehicle.cor ? ` · ${vehicle.cor}` : ''}
                        </p>
                        <p className="text-lg font-semibold text-foreground">{formatCurrency(vehicle.preco)}</p>
                        <div className="flex justify-end gap-2 pt-1">
                          <Button variant="ghost" size="sm" asChild>
                            <Link to={`/app/estoque/${vehicle.id}`}>Ver detalhes</Link>
                          </Button>
                          <Button variant="outline" size="sm" onClick={() => openEditModal(vehicle)}>
                            Editar
                          </Button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs text-muted-foreground">
                Mostrando {totalElements === 0 ? 0 : (safePage - 1) * pageSize + 1} a{' '}
                {totalElements === 0 ? 0 : (safePage - 1) * pageSize + pagedVehicles.length} de {totalElements} veiculos
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" disabled={safePage <= 1} onClick={() => setPage((current) => current - 1)}>
                  Anterior
                </Button>
                <span className="text-xs text-muted-foreground">
                  Pagina {safePage} de {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={safePage >= totalPages}
                  onClick={() => setPage((current) => current + 1)}
                >
                  Proxima
                </Button>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div className="rounded-xl border bg-card p-4 text-center">
                <p className="text-2xl font-bold text-foreground">
                  {filteredVehicles.filter((vehicle) => vehicle.status === 'disponivel').length}
                </p>
                <p className="text-sm text-muted-foreground">Disponiveis</p>
              </div>
              <div className="rounded-xl border bg-card p-4 text-center">
                <p className="text-2xl font-bold text-foreground">
                  {filteredVehicles.filter((vehicle) => vehicle.status === 'reservado').length}
                </p>
                <p className="text-sm text-muted-foreground">Reservados</p>
              </div>
              <div className="rounded-xl border bg-card p-4 text-center">
                <p className="text-2xl font-bold text-foreground">
                  {filteredVehicles.filter((vehicle) => vehicle.status === 'manutencao').length}
                </p>
                <p className="text-sm text-muted-foreground">Em manutencao</p>
              </div>
              <div className="rounded-xl border bg-card p-4 text-center">
                <p className="text-2xl font-bold text-foreground">
                  {filteredVehicles.filter((vehicle) => vehicle.status === 'vendido').length}
                </p>
                <p className="text-sm text-muted-foreground">Vendidos</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <Sheet open={isFiltersOpen} onOpenChange={setIsFiltersOpen}>
        <SheetContent side="left" className="w-[92vw] overflow-y-auto sm:max-w-md">
          <SheetHeader>
            <SheetTitle>Filtros de Estoque</SheetTitle>
            <SheetDescription>Ajuste os facetes para refinar a listagem de veiculos.</SheetDescription>
          </SheetHeader>

          <div className="mt-6 space-y-4">
            <VehicleFiltersPanel
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              brands={brands}
              brandFilter={brandFilter}
              onBrandChange={setBrandFilter}
              models={models}
              modelFilter={modelFilter}
              onModelChange={setModelFilter}
              minPriceDraft={minPriceDraft}
              maxPriceDraft={maxPriceDraft}
              onMinPriceDraftChange={setMinPriceDraft}
              onMaxPriceDraftChange={setMaxPriceDraft}
              onApplyPriceRange={applyPriceRange}
              years={years}
              yearFromFilter={yearFromFilter}
              yearToFilter={yearToFilter}
              onYearFromChange={setYearFromFilter}
              onYearToChange={setYearToFilter}
              statusFilters={statusFilters}
              onToggleStatus={handleToggleStatus}
              onClearFilters={clearFilters}
            />

            <Button
              className="w-full"
              onClick={() => {
                applyPriceRange();
                setIsFiltersOpen(false);
              }}
            >
              Aplicar filtros
            </Button>
          </div>
        </SheetContent>
      </Sheet>

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
