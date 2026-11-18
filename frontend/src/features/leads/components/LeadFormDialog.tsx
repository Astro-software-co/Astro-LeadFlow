import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import type { Lead } from '@/types';
import { originOptions } from '../constants';

const leadSchema = z.object({
  nome: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres'),
  telefone: z.string().min(10, 'Telefone invalido'),
  email: z.string().email('E-mail invalido').optional().or(z.literal('')),
  origem: z.enum(['OLX', 'Mercado Livre', 'Instagram', 'WebMotors', 'Facebook', 'Mobiauto', 'Napista', 'Direto']),
  interesse: z.string().optional(),
  orcamento: z
    .string()
    .optional()
    .refine((value) => !value || !Number.isNaN(Number(value)), 'Orcamento invalido'),
  urgencia: z.enum(['baixa', 'media', 'alta']).optional().or(z.literal('')),
  observacoes: z.string().optional(),
});

export type LeadFormValues = z.infer<typeof leadSchema>;

interface LeadFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lead: Lead | null;
  onSubmit: (values: LeadFormValues) => Promise<void>;
  isSubmitting: boolean;
}

export function LeadFormDialog({ open, onOpenChange, lead, onSubmit, isSubmitting }: LeadFormDialogProps) {
  const form = useForm<LeadFormValues>({
    resolver: zodResolver(leadSchema),
    values: lead
      ? {
          nome: lead.nome,
          telefone: lead.telefone,
          email: lead.email ?? '',
          origem: lead.origem,
          interesse: lead.interesse ?? '',
          orcamento: lead.orcamento ? String(lead.orcamento) : '',
          urgencia: lead.urgencia ?? '',
          observacoes: lead.observacoes ?? '',
        }
      : {
          nome: '',
          telefone: '',
          email: '',
          origem: 'Direto',
          interesse: '',
          orcamento: '',
          urgencia: '',
          observacoes: '',
        },
  });

  const urgencyEmptyValue = 'none';

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{lead ? 'Editar lead' : 'Novo lead'}</DialogTitle>
        </DialogHeader>

        <form
          className="mt-4 space-y-4"
          onSubmit={form.handleSubmit(async (values) => {
            await onSubmit(values);
          })}
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="nome">Nome</Label>
              <Input id="nome" {...form.register('nome')} />
              {form.formState.errors.nome ? (
                <p className="text-xs text-destructive">{form.formState.errors.nome.message}</p>
              ) : null}
            </div>

            <div className="space-y-2">
              <Label htmlFor="telefone">Telefone</Label>
              <Input id="telefone" {...form.register('telefone')} />
              {form.formState.errors.telefone ? (
                <p className="text-xs text-destructive">{form.formState.errors.telefone.message}</p>
              ) : null}
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <Input id="email" type="email" {...form.register('email')} />
              {form.formState.errors.email ? (
                <p className="text-xs text-destructive">{form.formState.errors.email.message}</p>
              ) : null}
            </div>

            <div className="space-y-2">
              <Label htmlFor="origem">Origem</Label>
              <Controller
                control={form.control}
                name="origem"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="origem">
                      <SelectValue placeholder="Selecione a origem" />
                    </SelectTrigger>
                    <SelectContent>
                      {originOptions
                        .filter((option) => option.value !== 'all')
                        .map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="interesse">Interesse</Label>
              <Input id="interesse" {...form.register('interesse')} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="orcamento">Orcamento (R$)</Label>
              <Input id="orcamento" {...form.register('orcamento')} />
              {form.formState.errors.orcamento ? (
                <p className="text-xs text-destructive">{form.formState.errors.orcamento.message}</p>
              ) : null}
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="urgencia">Urgencia</Label>
              <Controller
                control={form.control}
                name="urgencia"
                render={({ field }) => (
                  <Select
                    value={field.value || urgencyEmptyValue}
                    onValueChange={(value) => field.onChange(value === urgencyEmptyValue ? '' : value)}
                  >
                    <SelectTrigger id="urgencia">
                      <SelectValue placeholder="Nao informada" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={urgencyEmptyValue}>Nao informada</SelectItem>
                      <SelectItem value="baixa">Baixa</SelectItem>
                      <SelectItem value="media">Media</SelectItem>
                      <SelectItem value="alta">Alta</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="observacoes">Observacoes</Label>
              <Textarea id="observacoes" rows={3} {...form.register('observacoes')} />
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isSubmitting}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
              {lead ? 'Salvar' : 'Criar lead'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
