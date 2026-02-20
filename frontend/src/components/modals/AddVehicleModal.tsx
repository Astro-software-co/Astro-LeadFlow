import { useState, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Car, X, Upload, Image as ImageIcon } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { toast } from 'sonner';
import { Vehicle, VehicleStatus, VehicleCategory } from '@/types';

const vehicleSchema = z.object({
  marca: z.string().min(2, 'Marca deve ter pelo menos 2 caracteres').max(50, 'Marca deve ter no máximo 50 caracteres'),
  modelo: z.string().min(2, 'Modelo deve ter pelo menos 2 caracteres').max(50, 'Modelo deve ter no máximo 50 caracteres'),
  ano: z.number().min(1990, 'Ano mínimo é 1990').max(new Date().getFullYear() + 1, 'Ano inválido'),
  preco: z.number().min(1000, 'Preço mínimo é R$ 1.000').max(10000000, 'Preço máximo é R$ 10.000.000'),
  precoCusto: z.number().min(0, 'Preço de custo inválido').optional(),
  status: z.enum(['disponivel', 'reservado', 'manutencao', 'vendido', 'aguardando_documentacao'] as const),
  categoria: z.enum(['SUV', 'Sedan', 'Hatch', 'Pickup', 'Outros'] as const),
  cor: z.string().optional(),
  placa: z.string().optional(),
  quilometragem: z.number().min(0, 'Quilometragem inválida').optional(),
  ipvaPago: z.boolean().optional(),
});

type VehicleFormData = z.infer<typeof vehicleSchema>;

interface AddVehicleModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (vehicle: Omit<Vehicle, 'id'>) => void;
}

export function AddVehicleModal({ open, onOpenChange, onSubmit }: AddVehicleModalProps) {
  const [photos, setPhotos] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<VehicleFormData>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: {
      marca: '',
      modelo: '',
      ano: new Date().getFullYear(),
      preco: 0,
      precoCusto: undefined,
      status: 'disponivel',
      categoria: 'Sedan',
      cor: '',
      placa: '',
      quilometragem: undefined,
      ipvaPago: false,
    },
  });

  const status = watch('status');
  const categoria = watch('categoria');
  const ipvaPago = watch('ipvaPago');

  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files) {
      Array.from(files).forEach((file) => {
        const reader = new FileReader();
        reader.onloadend = () => {
          setPhotos((prev) => [...prev, reader.result as string]);
        };
        reader.readAsDataURL(file);
      });
    }
  };

  const removePhoto = (index: number) => {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  };

  const handleFormSubmit = (data: VehicleFormData) => {
    const vehicleData: Omit<Vehicle, 'id'> = {
      marca: data.marca,
      modelo: data.modelo,
      ano: data.ano,
      preco: data.preco,
      precoCusto: data.precoCusto,
      status: data.status,
      categoria: data.categoria,
      cor: data.cor || undefined,
      placa: data.placa || undefined,
      quilometragem: data.quilometragem,
      ipvaPago: data.ipvaPago,
      fotos: photos.length > 0 ? photos : undefined,
    };
    onSubmit(vehicleData);
    toast.success('Veículo cadastrado com sucesso!');
    reset();
    setPhotos([]);
    onOpenChange(false);
  };

  const handleClose = () => {
    reset();
    setPhotos([]);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <Car className="h-5 w-5 text-primary" />
            </div>
            Cadastrar Veículo
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 mt-4">
          {/* Marca e Modelo */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="marca">Marca *</Label>
              <Input
                id="marca"
                placeholder="Ex: Toyota"
                {...register('marca')}
                className={errors.marca ? 'border-destructive' : ''}
              />
              {errors.marca && (
                <p className="text-xs text-destructive">{errors.marca.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="modelo">Modelo *</Label>
              <Input
                id="modelo"
                placeholder="Ex: Corolla"
                {...register('modelo')}
                className={errors.modelo ? 'border-destructive' : ''}
              />
              {errors.modelo && (
                <p className="text-xs text-destructive">{errors.modelo.message}</p>
              )}
            </div>
          </div>

          {/* Ano, Cor e Placa */}
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="ano">Ano *</Label>
              <Input
                id="ano"
                type="number"
                placeholder="2024"
                {...register('ano', { valueAsNumber: true })}
                className={errors.ano ? 'border-destructive' : ''}
              />
              {errors.ano && (
                <p className="text-xs text-destructive">{errors.ano.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="cor">Cor</Label>
              <Input
                id="cor"
                placeholder="Ex: Preto"
                {...register('cor')}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="placa">Placa (Rodízio)</Label>
              <Input
                id="placa"
                placeholder="Ex: ABC1D23"
                {...register('placa')}
              />
            </div>
          </div>

          {/* Preço e Preço de Custo */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="preco">Preço de Venda (R$) *</Label>
              <Input
                id="preco"
                type="number"
                placeholder="150000"
                {...register('preco', { valueAsNumber: true })}
                className={errors.preco ? 'border-destructive' : ''}
              />
              {errors.preco && (
                <p className="text-xs text-destructive">{errors.preco.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="precoCusto">Preço de Custo (R$)</Label>
              <Input
                id="precoCusto"
                type="number"
                placeholder="120000"
                {...register('precoCusto', { valueAsNumber: true })}
                className={errors.precoCusto ? 'border-destructive' : ''}
              />
              {errors.precoCusto && (
                <p className="text-xs text-destructive">{errors.precoCusto.message}</p>
              )}
            </div>
          </div>

          {/* Quilometragem e IPVA */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="quilometragem">Quilometragem (km)</Label>
              <Input
                id="quilometragem"
                type="number"
                placeholder="45000"
                {...register('quilometragem', { valueAsNumber: true })}
              />
            </div>

            <div className="flex items-center justify-between rounded-lg border p-3">
              <Label htmlFor="ipvaPago" className="cursor-pointer">IPVA Pago</Label>
              <Switch
                id="ipvaPago"
                checked={ipvaPago}
                onCheckedChange={(checked) => setValue('ipvaPago', checked)}
              />
            </div>
          </div>

          {/* Categoria e Status */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Categoria *</Label>
              <Select
                value={categoria}
                onValueChange={(value: VehicleCategory) => setValue('categoria', value)}
              >
                <SelectTrigger className={errors.categoria ? 'border-destructive' : ''}>
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SUV">SUV</SelectItem>
                  <SelectItem value="Sedan">Sedan</SelectItem>
                  <SelectItem value="Hatch">Hatch</SelectItem>
                  <SelectItem value="Pickup">Pickup</SelectItem>
                  <SelectItem value="Outros">Outros</SelectItem>
                </SelectContent>
              </Select>
              {errors.categoria && (
                <p className="text-xs text-destructive">{errors.categoria.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label>Status *</Label>
              <Select
                value={status}
                onValueChange={(value: VehicleStatus) => setValue('status', value)}
              >
                <SelectTrigger className={errors.status ? 'border-destructive' : ''}>
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="disponivel">Disponível</SelectItem>
                  <SelectItem value="reservado">Reservado</SelectItem>
                  <SelectItem value="manutencao">Em Manutenção</SelectItem>
                  <SelectItem value="vendido">Vendido</SelectItem>
                  <SelectItem value="aguardando_documentacao">Aguardando Documentação</SelectItem>
                </SelectContent>
              </Select>
              {errors.status && (
                <p className="text-xs text-destructive">{errors.status.message}</p>
              )}
            </div>
          </div>

          {/* Fotos */}
          <div className="space-y-2">
            <Label>Fotos do Veículo</Label>
            <div
              onClick={() => fileInputRef.current?.click()}
              className="flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-muted-foreground/25 bg-muted/20 p-6 transition-colors hover:border-primary/50 hover:bg-muted/30"
            >
              <Upload className="h-8 w-8 text-muted-foreground mb-2" />
              <p className="text-sm text-muted-foreground">
                Clique para adicionar fotos
              </p>
              <p className="text-xs text-muted-foreground/70">
                PNG, JPG até 5MB cada
              </p>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handlePhotoUpload}
              className="hidden"
            />

            {photos.length > 0 && (
              <div className="grid grid-cols-4 gap-2 mt-3">
                {photos.map((photo, index) => (
                  <div key={index} className="relative group">
                    <img
                      src={photo}
                      alt={`Foto ${index + 1}`}
                      className="h-20 w-full object-cover rounded-lg"
                    />
                    <button
                      type="button"
                      onClick={() => removePhoto(index)}
                      className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-destructive text-destructive-foreground opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Salvando...' : 'Cadastrar Veículo'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
