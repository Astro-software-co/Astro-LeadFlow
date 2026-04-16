import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { AxiosError } from 'axios';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ImageOff, Pencil, Archive } from 'lucide-react';
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
import { useUpdateVehicle, useVehicle } from '@/hooks/api/useVehicles';
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

  const { data: vehicle, isLoading, isError, error, refetch } = useVehicle(id);
  const updateVehicle = useUpdateVehicle();

  useEffect(() => {
    setSelectedPhotoIndex(0);
  }, [vehicle?.id]);

  const photos = vehicle?.fotos ?? [];
  const selectedPhoto = photos[selectedPhotoIndex] ?? photos[0];
  const status = vehicle ? statusConfig[vehicle.status] : null;

  const marginPercent = useMemo(() => {
    if (!vehicle?.precoCusto || vehicle.precoCusto <= 0) {
      return null;
    }

    return (((vehicle.preco - vehicle.precoCusto) / vehicle.precoCusto) * 100).toFixed(1);
  }, [vehicle]);

  const isNotFound =
    isError &&
    ((error instanceof AxiosError && error.response?.status === 404) ||
      (error instanceof Error && /nao encontrado/i.test(error.message)));

  const handleArchive = async () => {
    if (!vehicle || vehicle.status === 'vendido') {
      setIsArchiveDialogOpen(false);
      return;
    }

    await updateVehicle.mutateAsync({
      id: vehicle.id,
      data: {
        status: 'vendido',
      },
    });

    toast.success('Veiculo arquivado com sucesso.');
    setIsArchiveDialogOpen(false);
    await refetch();
  };

  const handleEditVehicle = async (data: Omit<Vehicle, 'id'>) => {
    if (!vehicle) {
      return;
    }

    await updateVehicle.mutateAsync({ id: vehicle.id, data });
    await refetch();
  };

  if (isLoading) {
    return <VehicleDetailSkeleton />;
  }

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
        <title>
          {vehicle.marca} {vehicle.modelo} {vehicle.ano} — Estoque
        </title>
      </Helmet>

      <div className="space-y-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="space-y-2">
            <Button variant="outline" size="sm" onClick={() => navigate(-1)}>
              <ArrowLeft className="h-4 w-4" />
              Voltar ao Estoque
            </Button>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold text-foreground">
                {vehicle.marca} {vehicle.modelo} {vehicle.ano}
              </h1>
              {status ? <Badge className={cn('text-xs', status.className)}>{status.label}</Badge> : null}
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => setIsEditModalOpen(true)}>
              <Pencil className="h-4 w-4" />
              Editar
            </Button>

            <AlertDialog open={isArchiveDialogOpen} onOpenChange={setIsArchiveDialogOpen}>
              <AlertDialogTrigger asChild>
                <Button
                  variant="outline"
                  disabled={vehicle.status === 'vendido' || updateVehicle.isPending}
                >
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
          </div>
        </div>

        <div className="rounded-xl border bg-card p-4 shadow-sm">
          {photos.length > 0 ? (
            <div className="space-y-3">
              <div className="overflow-hidden rounded-lg border bg-muted/30">
                <img
                  src={selectedPhoto}
                  alt={`${vehicle.marca} ${vehicle.modelo}`}
                  className="h-[320px] w-full object-cover"
                />
              </div>
              <div className="grid grid-cols-4 gap-2 md:grid-cols-6">
                {photos.map((photo, index) => (
                  <button
                    key={`${photo}-${index}`}
                    type="button"
                    onClick={() => setSelectedPhotoIndex(index)}
                    className={cn(
                      'overflow-hidden rounded-md border',
                      selectedPhotoIndex === index ? 'border-primary ring-2 ring-primary/20' : 'border-border/60',
                    )}
                  >
                    <img
                      src={photo}
                      alt={`Miniatura ${index + 1}`}
                      className="h-16 w-full object-cover"
                    />
                  </button>
                ))}
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

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <SpecCard label="Marca" value={vehicle.marca} />
          <SpecCard label="Modelo" value={vehicle.modelo} />
          <SpecCard label="Ano" value={String(vehicle.ano)} />
          <SpecCard label="Categoria" value={vehicle.categoria} />
          <SpecCard label="Cor" value={vehicle.cor || 'Nao informada'} />
          <SpecCard label="Placa" value={vehicle.placa || 'Nao informada'} />
          <SpecCard
            label="Quilometragem"
            value={typeof vehicle.quilometragem === 'number' ? `${vehicle.quilometragem.toLocaleString('pt-BR')} km` : 'Nao informada'}
          />
          <SpecCard label="IPVA pago" value={vehicle.ipvaPago ? 'Sim' : 'Nao'} />
        </div>

        <div className="rounded-xl border bg-card p-6 shadow-sm">
          <p className="text-sm font-medium text-muted-foreground">Preco de venda</p>
          <p className="mt-1 text-3xl font-bold text-foreground">{formatCurrency(vehicle.preco)}</p>

          <RoleGate allowedRoles={['MANAGER', 'ADMIN']} fallback={<></>}>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg border bg-muted/20 p-4">
                <p className="text-xs text-muted-foreground">Custo</p>
                <p className="text-lg font-semibold text-foreground">
                  {typeof vehicle.precoCusto === 'number' ? formatCurrency(vehicle.precoCusto) : 'Nao informado'}
                </p>
              </div>
              <div className="rounded-lg border bg-muted/20 p-4">
                <p className="text-xs text-muted-foreground">Margem</p>
                <p className="text-lg font-semibold text-foreground">{marginPercent ? `${marginPercent}%` : 'Nao calculada'}</p>
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
