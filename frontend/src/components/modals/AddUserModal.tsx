import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Shield } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const userSchema = z.object({
  nome: z
    .string()
    .min(3, 'Nome deve ter pelo menos 3 caracteres')
    .max(100, 'Nome deve ter no maximo 100 caracteres')
    .regex(/^[a-zA-ZÀ-ÿ\s]+$/, 'Nome deve conter apenas letras'),
  email: z.string().email('E-mail invalido').max(255, 'E-mail deve ter no maximo 255 caracteres'),
  role: z.literal('SELLER'),
});

type UserFormData = z.infer<typeof userSchema>;

interface AddUserModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (user: { nome: string; email: string; role: 'SELLER' }) => Promise<void> | void;
}

export function AddUserModal({ open, onOpenChange, onSubmit }: AddUserModalProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UserFormData>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      nome: '',
      email: '',
      role: 'SELLER',
    },
  });

  const handleFormSubmit = async (data: UserFormData) => {
    await onSubmit({
      nome: data.nome,
      email: data.email,
      role: data.role,
    });

    reset();
    onOpenChange(false);
  };

  const handleClose = () => {
    reset();
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-kanban-negotiation/10">
              <User className="h-5 w-5 text-kanban-negotiation" />
            </div>
            Cadastrar Usuario
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="mt-4 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="nome">Nome Completo *</Label>
            <Input
              id="nome"
              placeholder="Ex: Joao Silva"
              {...register('nome')}
              className={errors.nome ? 'border-destructive' : ''}
            />
            {errors.nome && <p className="text-xs text-destructive">{errors.nome.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">E-mail *</Label>
            <Input
              id="email"
              type="email"
              placeholder="Ex: joao@astroleadflow.com.br"
              {...register('email')}
              className={errors.email ? 'border-destructive' : ''}
            />
            {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
          </div>

          <div className="space-y-2">
            <Label className="flex items-center gap-2">
              <Shield className="h-4 w-4" />
              Perfil
            </Label>
            <input type="hidden" {...register('role')} />
            <div className="inline-flex items-center gap-2 rounded-md border border-border/50 px-3 py-2 text-sm text-foreground">
              <div className="h-2 w-2 rounded-full bg-kanban-completed" />
              Seller
            </div>
            {errors.role && <p className="text-xs text-destructive">{errors.role.message}</p>}
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Salvando...' : 'Cadastrar Usuario'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
