import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Shield } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { Seller } from '@/types';

const userSchema = z
  .object({
    nome: z
      .string()
      .min(3, 'Nome deve ter pelo menos 3 caracteres')
      .max(100, 'Nome deve ter no máximo 100 caracteres')
      .regex(/^[a-zA-ZÀ-ÿ\s]+$/, 'Nome deve conter apenas letras'),
    email: z.string().email('E-mail inválido').max(255, 'E-mail deve ter no máximo 255 caracteres'),
    senha: z
      .string()
      .min(8, 'Senha deve ter pelo menos 8 caracteres')
      .max(50, 'Senha deve ter no máximo 50 caracteres')
      .regex(/[A-Z]/, 'Senha deve conter pelo menos uma letra maiúscula')
      .regex(/[a-z]/, 'Senha deve conter pelo menos uma letra minúscula')
      .regex(/[0-9]/, 'Senha deve conter pelo menos um número'),
    confirmarSenha: z.string(),
    role: z.enum(['ADMIN', 'VENDEDOR'] as const),
  })
  .refine((data) => data.senha === data.confirmarSenha, {
    message: 'As senhas não coincidem',
    path: ['confirmarSenha'],
  });

type UserFormData = z.infer<typeof userSchema>;

interface AddUserModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (user: Omit<Seller, 'id'>) => void;
}

export function AddUserModal({ open, onOpenChange, onSubmit }: AddUserModalProps) {
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UserFormData>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      nome: '',
      email: '',
      senha: '',
      confirmarSenha: '',
      role: 'VENDEDOR',
    },
  });

  const role = watch('role');

  const handleFormSubmit = (data: UserFormData) => {
    onSubmit({
      nome: data.nome,
      email: data.email,
      role: data.role,
    });
    toast.success('Usuário cadastrado com sucesso!');
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
            Cadastrar Usuário
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 mt-4">
          {/* Nome */}
          <div className="space-y-2">
            <Label htmlFor="nome">Nome Completo *</Label>
            <Input
              id="nome"
              placeholder="Ex: João Silva"
              {...register('nome')}
              className={errors.nome ? 'border-destructive' : ''}
            />
            {errors.nome && <p className="text-xs text-destructive">{errors.nome.message}</p>}
          </div>

          {/* Email */}
          <div className="space-y-2">
            <Label htmlFor="email">E-mail *</Label>
            <Input
              id="email"
              type="email"
              placeholder="Ex: joao@v3motors.com"
              {...register('email')}
              className={errors.email ? 'border-destructive' : ''}
            />
            {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* Senha */}
            <div className="space-y-2">
              <Label htmlFor="senha">Senha *</Label>
              <Input
                id="senha"
                type="password"
                placeholder="••••••••"
                {...register('senha')}
                className={errors.senha ? 'border-destructive' : ''}
              />
              {errors.senha && <p className="text-xs text-destructive">{errors.senha.message}</p>}
            </div>

            {/* Confirmar Senha */}
            <div className="space-y-2">
              <Label htmlFor="confirmarSenha">Confirmar Senha *</Label>
              <Input
                id="confirmarSenha"
                type="password"
                placeholder="••••••••"
                {...register('confirmarSenha')}
                className={errors.confirmarSenha ? 'border-destructive' : ''}
              />
              {errors.confirmarSenha && <p className="text-xs text-destructive">{errors.confirmarSenha.message}</p>}
            </div>
          </div>

          {/* Role/Permissão */}
          <div className="space-y-2">
            <Label className="flex items-center gap-2">
              <Shield className="h-4 w-4" />
              Permissão *
            </Label>
            <Select value={role} onValueChange={(value: 'ADMIN' | 'VENDEDOR') => setValue('role', value)}>
              <SelectTrigger className={errors.role ? 'border-destructive' : ''}>
                <SelectValue placeholder="Selecione a permissão" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="VENDEDOR">
                  <div className="flex items-center gap-2">
                    <div className="h-2 w-2 rounded-full bg-kanban-completed" />
                    Vendedor
                  </div>
                </SelectItem>
                <SelectItem value="ADMIN">
                  <div className="flex items-center gap-2">
                    <div className="h-2 w-2 rounded-full bg-kanban-negotiation" />
                    Administrador
                  </div>
                </SelectItem>
              </SelectContent>
            </Select>
            {errors.role && <p className="text-xs text-destructive">{errors.role.message}</p>}
          </div>

          {/* Permission Description */}
          <div className="rounded-lg bg-muted/50 p-4 text-sm">
            {role === 'ADMIN' ? (
              <div className="space-y-2">
                <p className="font-medium text-foreground">Permissões de Administrador:</p>
                <ul className="list-disc list-inside text-muted-foreground space-y-1">
                  <li>Gerenciar todos os leads e vendas</li>
                  <li>Editar cards de qualquer vendedor</li>
                  <li>Gerenciar estoque completo</li>
                  <li>Adicionar/remover usuários</li>
                  <li>Visualizar relatórios completos</li>
                </ul>
              </div>
            ) : (
              <div className="space-y-2">
                <p className="font-medium text-foreground">Permissões de Vendedor:</p>
                <ul className="list-disc list-inside text-muted-foreground space-y-1">
                  <li>Assumir e gerenciar seus próprios leads</li>
                  <li>Mover cards apenas dos seus atendimentos</li>
                  <li>Visualizar estoque (sem edição)</li>
                  <li>Visualizar suas próprias métricas</li>
                </ul>
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Salvando...' : 'Cadastrar Usuário'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
