import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Shield } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { SettingsTeamMember } from '@/types/settings';

const editUserSchema = z.object({
  name: z
    .string()
    .min(3, 'Nome deve ter pelo menos 3 caracteres')
    .regex(/^[a-zA-ZÀ-ÿ\s]+$/, 'Nome deve conter apenas letras'),
  email: z.string().email('E-mail invalido'),
});

type EditUserFormData = z.infer<typeof editUserSchema>;

interface EditUserModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user: SettingsTeamMember | null;
  onSubmit: (memberId: string, values: { name: string; email: string; phoneNumber?: string | null }) => Promise<void> | void;
}

export function EditUserModal({ open, onOpenChange, user, onSubmit }: EditUserModalProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<EditUserFormData>({
    resolver: zodResolver(editUserSchema),
  });

  useEffect(() => {
    if (user) {
      reset({ name: user.name, email: user.email });
    }
  }, [user, reset]);

  const handleFormSubmit = async (data: EditUserFormData) => {
    if (!user) return;
    await onSubmit(user.id, { ...data, phoneNumber: null });
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <User className="h-5 w-5 text-primary" />
            </div>
            Editar Usuario
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 mt-4">
          <div className="space-y-2">
            <Label htmlFor="edit-nome">Nome Completo *</Label>
            <Input id="edit-nome" {...register('name')} className={errors.name ? 'border-destructive' : ''} />
            {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-email">E-mail *</Label>
            <Input
              id="edit-email"
              type="email"
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
            <div className="inline-flex items-center gap-2 rounded-md border border-border/50 px-3 py-2 text-sm text-foreground">
              <div className="h-2 w-2 rounded-full bg-kanban-completed" />
              Seller
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Salvando...' : 'Salvar Alteracoes'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
