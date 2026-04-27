import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { AxiosError } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ChevronLeft, ChevronRight, GripVertical, ImageOff, Pencil, Archive, Trash2 } from 'lucide-react';
import { DragDropContext, Droppable, Draggable, DropResult } from '@hello-pangea/dnd';
import { toast } from 'sonner';
import { AddVehicleModal } from '@/components/modals/AddVehicleModal';
import { RoleGate } from '@/components/layout/RoleGate';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import {
  useDeleteVehicle,
  useReorderVehiclePhotos,
  useUpdateVehicle,
  useVehicle,
  useVehiclePhotos,
} from '@/hooks/api/useVehicles';
import { formatCurrency } from '@/lib/formatters';
import { Vehicle } from '@/types';
import { cn } from '@/lib/utils';

const statusConfig: Record<Vehicle['status'], { label: string; className: string }> = {
  disponivel: { label: 'Disponivel', className: 'bg-emerald-100 text-emerald-700' },
  reservado: { label: 'Reservado', className: 'bg-amber-100 text-amber-700' },
  manutencao: { label: 'Em manutencao', className: 'bg-slate-100 text-slate-700' },
  vendido: { label: 'Vendido', className: 'bg-slate-200 text-slate-700' },
  aguardando_documentacao: { label: 'Aguardando doc.', className: 'bg-blue-100 text-blue-700' },
};

function VehicleDetailSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="space-y-2">
          <Skeleton className="h-9 w-40" />
          <Skeleton className="h-8 w-72" />
        </div>
        <div className="flex gap-2">
          <Skeleton className="h-10 w-24" />
          <Skeleton className="h-10 w-24" />
        </div>
      </div>
      <Skeleton className="h-72 w-full rounded-xl" />
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {Array.from({ length: 8 }).map((_, index) => (
          <Skeleton key={index} className="h-20 w-full rounded-xl" />
        ))}
      </div>
      <Skeleton className="h-36 w-full rounded-xl" />
    </div>
  );
}

export default function VehicleDetail() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [selectedPhotoIndex, setSelectedPhotoIndex] = useState(0);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isArchiveDialogOpen, setIsArchiveDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isReorderMode, setIsReorderMode] = useState(false);
  const [draftPhotoIds, setDraftPhotoIds] = useState<string[]>([]);

  const { data: vehicle, isLoading, isError, error, refetch } = useVehicle(id);
  const { data: photoDetails, refetch: refetchPhotos } = useVehiclePhotos(id);
  const updateVehicle = useUpdateVehicle();
  const deleteVehicle = useDeleteVehicle();
  const reorderMutation = useReorderVehiclePhotos(id ?? '');

  useEffect(() => {
    setSelectedPhotoIndex(0);
    setIsReorderMode(false);
    setDraftPhotoIds([]);
  }, [vehicle?.id]);

  const photos = vehicle?.fotos ?? [];

  // In reorder mode use draft order mapped back to URLs; otherwise use vehicle photos as-is
  const displayPhotos = useMemo(() => {
    if (!isReorderMode || !photoDetails || draftPhotoIds.length === 0) {
      return photos;
    }
    return draftPhotoIds.flatMap((pid) => {
      const found = photoDetails.find((p) => p.id === pid);
      return found ? [found.s3Url] : [];
    });
  }, [isReorderMode, draftPhotoIds, photoDetails, photos]);

  const selectedPhoto = displayPhotos[selectedPhotoIndex] ?? displayPhotos[0];

  const status = vehicle ? statusConfig[vehicle.status] : null;
  const vehicleDisplayName = vehicle
    ? [vehicle.marca, vehicle.modelo, String(vehicle.ano)]
        .map((v) => v.trim())
        .filter(Boolean)
        .join(' ') || 'Veiculo'
    : 'Veiculo';

  const marginPercent = useMemo(() => {
    if (!vehicle?.precoCusto || vehicle.precoCusto <= 0) return null;
    return (((vehicle.preco - vehicle.precoCusto) / vehicle.precoCusto) * 100).toFixed(1);
  }, [vehicle]);

  const isNotFound =
    isError &&
    ((error instanceof AxiosError && error.response?.status === 404) ||
      (error instanceof Error && /nao encontrado/i.test(error.message)));

  const navigate_prev = () =>
    setSelectedPhotoIndex((i) => (i > 0 ? i - 1 : displayPhotos.length - 1));
  const navigate_next = () =>
    setSelectedPhotoIndex((i) => (i < displayPhotos.length - 1 ? i + 1 : 0));

  const enterReorderMode = () => {
    if (!photoDetails || photoDetails.length < 2) return;
    setDraftPhotoIds(photoDetails.map((p) => p.id));
    setIsReorderMode(true);
  };

  const cancelReorderMode = () => {
    setDraftPhotoIds([]);
    setIsReorderMode(false);
  };

  const saveReorderMode = async () => {
    try {
      await reorderMutation.mutateAsync(draftPhotoIds);
      await refetchPhotos();
      await refetch();
      setIsReorderMode(false);
      setDraftPhotoIds([]);
      toast.success('Ordem das fotos atualizada.');
    } catch {
      toast.error('Nao foi possivel salvar a ordem das fotos.');
    }
  };

  const handleDragEnd = (result: DropResult) => {
    if (!result.destination) return;
    const items = [...draftPhotoIds];
    const [moved] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, moved);
    setDraftPhotoIds(items);
    setSelectedPhotoIndex(result.destination.index);
  };

  const handleArchive = async () => {
    if (!vehicle || vehicle.status === 'vendido') {
      setIsArchiveDialogOpen(false);
      return;
    }
    await updateVehicle.mutateAsync({ id: vehicle.id, data: { status: 'vendido' } });
    toast.success('Veiculo arquivado com sucesso.');
    setIsArchiveDialogOpen(false);
    await refetch();
  };

  const handleDelete = async () => {
    if (!vehicle) return;
    try {
      await deleteVehicle.mutateAsync(vehicle.id);
      toast.success('Veiculo excluido com sucesso.');
      navigate('/app/estoque');
    } catch (err) {
      const message =
        err instanceof AxiosError
          ? (err.response?.data as { message?: string } | undefined)?.message ?? 'Nao foi possivel excluir o veiculo.'
          : 'Nao foi possivel excluir o veiculo.';
      toast.error(message);
    }
    setIsDeleteDialogOpen(false);
  };

  const handleEditVehicle = async (data: Omit<Vehicle, 'id'>) => {
    if (!vehicle) return;
    await updateVehicle.mutateAsync({ id: vehicle.id, data });
    await refetch();
  };

  if (isLoading) return <VehicleDetailSkeleton />;

  if (isNotFound || !vehicle) {
    return (
      <div className="rounded-xl border bg-card p-8 text-center shadow-sm">
        <h2 className="text-xl font-semibold text-foreground">Veiculo nao encontrado</h2>
        <p className="mt-2 text-sm text-muted-foreground">O registro solicitado nao existe ou foi removido.</p>
        <Button className="mt-4" variant="outline" onClick={() => navigate('/app/estoque')}>
          Voltar ao Estoque
        </Button>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar o veiculo</h2>
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
        <title>{`${vehicleDisplayName} — Estoque`}</title>
      </Helmet>

      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="space-y-2">
            <Button variant="outline" size="sm" onClick={() => navigate(-1)}>
              <ArrowLeft className="h-4 w-4" />
              Voltar ao Estoque
            </Button>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold text-foreground">{vehicleDisplayName}</h1>
              {status ? <Badge className={cn('text-xs', status.className)}>{status.label}</Badge> : null}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button variant="outline" onClick={() => setIsEditModalOpen(true)}>
              <Pencil className="h-4 w-4" />
              Editar
            </Button>

            <AlertDialog open={isArchiveDialogOpen} onOpenChange={setIsArchiveDialogOpen}>
              <AlertDialogTrigger asChild>
                <Button variant="outline" disabled={vehicle.status === 'vendido' || updateVehicle.isPending}>
                  <Archive className="h-4 w-4" />
                  Arquivar
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Arquivar este veiculo?</AlertDialogTitle>
                  <AlertDialogDescription>
                    Esta acao alterara o status para "Vendido". Voce podera reverter editando o veiculo depois.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancelar</AlertDialogCancel>
                  <AlertDialogAction onClick={() => void handleArchive()}>Confirmar</AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>

            <RoleGate allowedRoles={['MANAGER']} fallback={<></>}>
              <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
                <AlertDialogTrigger asChild>
                  <Button variant="outline" className="text-destructive hover:bg-destructive/10 hover:text-destructive border-destructive/30" disabled={deleteVehicle.isPending}>
                    <Trash2 className="h-4 w-4" />
                    Excluir
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Excluir este veiculo?</AlertDialogTitle>
                    <AlertDialogDescription>
                      Esta acao e permanente e nao pode ser desfeita. O veiculo sera removido do estoque. A exclusao sera bloqueada se houver leads ativos associados.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancelar</AlertDialogCancel>
                    <AlertDialogAction
                      className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                      onClick={() => void handleDelete()}
                    >
                      Excluir permanentemente
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </RoleGate>
          </div>
        </div>

        {/* Photo section */}
        <div className="rounded-xl border bg-card p-4 shadow-sm">
          {displayPhotos.length > 0 ? (
            <div className="space-y-3">
              {/* Main image with nav arrows */}
              <div className="group relative overflow-hidden rounded-lg border bg-muted/30">
                <img
                  src={selectedPhoto}
                  alt={vehicleDisplayName}
                  className="h-[340px] w-full object-cover md:h-[420px]"
                />

                {displayPhotos.length > 1 && !isReorderMode && (
                  <>
                    <button
                      type="button"
                      onClick={navigate_prev}
                      className="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 p-1.5 text-white opacity-0 transition-opacity hover:bg-black/60 group-hover:opacity-100 focus-visible:opacity-100"
                      aria-label="Foto anterior"
                    >
                      <ChevronLeft className="h-5 w-5" />
                    </button>
                    <button
                      type="button"
                      onClick={navigate_next}
                      className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 p-1.5 text-white opacity-0 transition-opacity hover:bg-black/60 group-hover:opacity-100 focus-visible:opacity-100"
                      aria-label="Proxima foto"
                    >
                      <ChevronRight className="h-5 w-5" />
                    </button>
                    <div className="absolute bottom-2 right-3 rounded-full bg-black/50 px-2.5 py-0.5 text-xs font-medium text-white">
                      {selectedPhotoIndex + 1} / {displayPhotos.length}
                    </div>
                  </>
                )}

                {isReorderMode && (
                  <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                    <p className="rounded-lg bg-black/60 px-4 py-2 text-sm font-medium text-white">
                      Arraste as miniaturas abaixo para reordenar
                    </p>
                  </div>
                )}
              </div>

              {/* Thumbnail strip */}
              {isReorderMode ? (
                <DragDropContext onDragEnd={handleDragEnd}>
                  <Droppable droppableId="photo-thumbnails" direction="horizontal">
                    {(provided) => (
                      <div
                        ref={provided.innerRef}
                        {...provided.droppableProps}
                        className="flex gap-2 overflow-x-auto pb-1"
                      >
                        {draftPhotoIds.map((pid, index) => {
                          const photo = photoDetails?.find((p) => p.id === pid);
                          if (!photo) return null;
                          return (
                            <Draggable key={pid} draggableId={pid} index={index}>
                              {(drag, snapshot) => (
                                <div
                                  ref={drag.innerRef}
                                  {...drag.draggableProps}
                                  className={cn(
                                    'relative flex-shrink-0 overflow-hidden rounded-md border',
                                    snapshot.isDragging ? 'border-primary shadow-lg ring-2 ring-primary/30' : 'border-border/60',
                                    selectedPhotoIndex === index ? 'border-primary ring-2 ring-primary/20' : '',
                                  )}
                                >
                                  <div
                                    {...drag.dragHandleProps}
                                    className="absolute left-1 top-1 z-10 cursor-grab rounded bg-black/40 p-0.5 active:cursor-grabbing"
                                    aria-label="Arrastar"
                                  >
                                    <GripVertical className="h-3 w-3 text-white" />
                                  </div>
                                  <div className="absolute bottom-0.5 right-1 z-10 rounded bg-black/50 px-1 text-[10px] font-semibold text-white">
                                    {index + 1}
                                  </div>
                                  <img
                                    src={photo.s3Url}
                                    alt={`Foto ${index + 1}`}
                                    onClick={() => setSelectedPhotoIndex(index)}
                                    className="h-16 w-24 cursor-pointer object-cover"
                                  />
                                </div>
                              )}
                            </Draggable>
                          );
                        })}
                        {provided.placeholder}
                      </div>
                    )}
                  </Droppable>
                </DragDropContext>
              ) : (
                <div className="flex gap-2 overflow-x-auto pb-1">
                  {displayPhotos.map((photo, index) => (
                    <button
                      key={`${photo}-${index}`}
                      type="button"
                      onClick={() => setSelectedPhotoIndex(index)}
                      className={cn(
                        'flex-shrink-0 overflow-hidden rounded-md border transition-all',
                        selectedPhotoIndex === index
                          ? 'border-primary ring-2 ring-primary/20'
                          : 'border-border/60 hover:border-border',
                      )}
                    >
                      <img src={photo} alt={`Miniatura ${index + 1}`} className="h-16 w-24 object-cover" />
                    </button>
                  ))}
                </div>
              )}

              {/* Reorder controls */}
              <div className="flex items-center justify-between">
                {isReorderMode ? (
                  <div className="flex gap-2">
                    <Button size="sm" onClick={() => void saveReorderMode()} disabled={reorderMutation.isPending}>
                      {reorderMutation.isPending ? 'Salvando...' : 'Salvar ordem'}
                    </Button>
                    <Button size="sm" variant="outline" onClick={cancelReorderMode} disabled={reorderMutation.isPending}>
                      Cancelar
                    </Button>
                  </div>
                ) : (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={enterReorderMode}
                    disabled={!photoDetails || photos.length < 2}
                    className="text-muted-foreground"
                  >
                    <GripVertical className="h-4 w-4" />
                    Reordenar fotos
                  </Button>
                )}
                <p className="text-xs text-muted-foreground">{photos.length} foto{photos.length !== 1 ? 's' : ''}</p>
              </div>
            </div>
          ) : (
            <div className="flex h-[260px] flex-col items-center justify-center rounded-lg border border-dashed bg-muted/30 text-center">
              <div className="mb-3 rounded-full bg-muted p-3">
                <ImageOff className="h-6 w-6 text-muted-foreground" />
              </div>
              <p className="text-sm font-medium text-foreground">Sem fotos cadastradas</p>
              <p className="text-xs text-muted-foreground">Adicione imagens para melhorar a apresentacao do veiculo.</p>
            </div>
          )}
        </div>

        {/* Specs grid */}
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <SpecCard label="Marca" value={vehicle.marca} />
          <SpecCard label="Modelo" value={vehicle.modelo} />
          <SpecCard label="Ano" value={String(vehicle.ano)} />
          <SpecCard label="Categoria" value={vehicle.categoria} />
          <SpecCard label="Cor" value={vehicle.cor || 'Nao informada'} />
          <SpecCard label="Placa" value={vehicle.placa || 'Nao informada'} />
          <SpecCard
            label="Quilometragem"
            value={
              typeof vehicle.quilometragem === 'number'
                ? `${vehicle.quilometragem.toLocaleString('pt-BR')} km`
                : 'Nao informada'
            }
          />
          <SpecCard label="IPVA pago" value={vehicle.ipvaPago ? 'Sim' : 'Nao'} />
        </div>

        {/* Pricing */}
        <div className="rounded-xl border bg-card p-6 shadow-sm">
          <p className="text-sm font-medium text-muted-foreground">Preco de venda</p>
          <p className="mt-1 text-3xl font-bold text-foreground">{formatCurrency(vehicle.preco)}</p>

          <RoleGate allowedRoles={['MANAGER']} fallback={<></>}>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg border bg-muted/20 p-4">
                <p className="text-xs text-muted-foreground">Custo</p>
                <p className="text-lg font-semibold text-foreground">
                  {typeof vehicle.precoCusto === 'number' ? formatCurrency(vehicle.precoCusto) : 'Nao informado'}
                </p>
              </div>
              <div className="rounded-lg border bg-muted/20 p-4">
                <p className="text-xs text-muted-foreground">Margem</p>
                <p className="text-lg font-semibold text-foreground">
                  {marginPercent ? `${marginPercent}%` : 'Nao calculada'}
                </p>
              </div>
            </div>
          </RoleGate>
        </div>
      </div>

      <AddVehicleModal
        open={isEditModalOpen}
        onOpenChange={setIsEditModalOpen}
        onSubmit={handleEditVehicle}
        vehicle={vehicle}
      />
    </>
  );
}

function SpecCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border bg-card p-4 shadow-sm">
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-base font-semibold text-foreground">{value}</p>
    </div>
  );
}
