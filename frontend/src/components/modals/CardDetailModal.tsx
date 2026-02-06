import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Phone, Mail, MapPin, Briefcase, Car, DollarSign, Clock, AlertCircle, MessageSquare, X } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible';
import { toast } from 'sonner';
import { KanbanCard, Lead } from '@/types';
import { cn } from '@/lib/utils';
import { ChevronDown } from 'lucide-react';

const leadSchema = z.object({
  nome: z.string().min(2, 'Nome deve ter pelo menos 2 caracteres'),
  telefone: z.string().min(10, 'Telefone inválido'),
  email: z.string().email('E-mail inválido').optional().or(z.literal('')),
  origem: z.enum(['OLX', 'Mercado Livre', 'Instagram', 'WebMotors', 'Facebook', 'Mobiauto', 'Napista', 'Direto'] as const),
  interesse: z.string().optional(),
  orcamento: z.number().optional(),
  urgencia: z.enum(['baixa', 'media', 'alta']).optional(),
  tempoCompra: z.string().optional(),
  observacoes: z.string().optional(),
  endereco: z.string().optional(),
  profissao: z.string().optional(),
});

type LeadFormData = z.infer<typeof leadSchema>;

interface CardDetailModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  card: KanbanCard;
  onUpdate: (card: KanbanCard) => void;
}

interface SectionProps {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
  defaultOpen?: boolean;
}

function Section({ title, icon, children, defaultOpen = true }: SectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="flex w-full items-center justify-between rounded-lg bg-muted/50 px-4 py-3 hover:bg-muted transition-colors">
        <div className="flex items-center gap-2">
          {icon}
          <span className="font-medium text-foreground">{title}</span>
        </div>
        <ChevronDown className={cn("h-4 w-4 text-muted-foreground transition-transform", isOpen && "rotate-180")} />
      </CollapsibleTrigger>
      <CollapsibleContent className="px-4 py-4 space-y-4">
        {children}
      </CollapsibleContent>
    </Collapsible>
  );
}

export function CardDetailModal({ open, onOpenChange, card, onUpdate }: CardDetailModalProps) {
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isDirty },
  } = useForm<LeadFormData>({
    resolver: zodResolver(leadSchema),
    defaultValues: {
      nome: card.lead.nome,
      telefone: card.lead.telefone,
      email: card.lead.email || '',
      origem: card.lead.origem,
      interesse: card.lead.interesse || card.interesse || '',
      orcamento: card.lead.orcamento || card.orcamento || undefined,
      urgencia: card.lead.urgencia || undefined,
      tempoCompra: card.lead.tempoCompra || '',
      observacoes: card.lead.observacoes || '',
      endereco: card.lead.endereco || '',
      profissao: card.lead.profissao || '',
    },
  });

  const origem = watch('origem');
  const urgencia = watch('urgencia');

  const handleFormSubmit = (data: LeadFormData) => {
    const updatedLead: Lead = {
      ...card.lead,
      ...data,
      email: data.email || undefined,
    };

    const updatedCard: KanbanCard = {
      ...card,
      lead: updatedLead,
      interesse: data.interesse,
      orcamento: data.orcamento,
    };

    onUpdate(updatedCard);
    toast.success('Lead atualizado com sucesso!');
    onOpenChange(false);
  };

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(date));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
                <User className="h-5 w-5 text-primary" />
              </div>
              <div>
                <span className="block">Detalhes do Lead</span>
                <span className="text-xs font-normal text-muted-foreground">
                  Criado em {formatDate(card.lead.dataCriacao)}
                </span>
              </div>
            </div>
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 mt-4">
          {/* Seção 1: Informações Essenciais */}
          <Section 
            title="Informações Essenciais" 
            icon={<User className="h-4 w-4 text-primary" />}
            defaultOpen={true}
          >
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="nome">Nome *</Label>
                <Input
                  id="nome"
                  placeholder="Nome do cliente"
                  {...register('nome')}
                  className={errors.nome ? 'border-destructive' : ''}
                />
                {errors.nome && (
                  <p className="text-xs text-destructive">{errors.nome.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="telefone">Telefone *</Label>
                <Input
                  id="telefone"
                  placeholder="(11) 99999-9999"
                  {...register('telefone')}
                  className={errors.telefone ? 'border-destructive' : ''}
                />
                {errors.telefone && (
                  <p className="text-xs text-destructive">{errors.telefone.message}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="email">E-mail</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="email@exemplo.com"
                  {...register('email')}
                  className={errors.email ? 'border-destructive' : ''}
                />
                {errors.email && (
                  <p className="text-xs text-destructive">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label>Origem *</Label>
                <Select
                  value={origem}
                  onValueChange={(value: any) => setValue('origem', value)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="OLX">OLX</SelectItem>
                    <SelectItem value="Mercado Livre">Mercado Livre</SelectItem>
                    <SelectItem value="Instagram">Instagram</SelectItem>
                    <SelectItem value="WebMotors">WebMotors</SelectItem>
                    <SelectItem value="Facebook">Facebook</SelectItem>
                    <SelectItem value="Mobiauto">Mobiauto</SelectItem>
                    <SelectItem value="Napista">Napista</SelectItem>
                    <SelectItem value="Direto">Direto</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </Section>

          {/* Seção 2: Informações de Qualificação */}
          <Section 
            title="Informações de Qualificação" 
            icon={<Car className="h-4 w-4 text-kanban-negotiation" />}
            defaultOpen={true}
          >
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="interesse">Veículo de Interesse</Label>
                <Input
                  id="interesse"
                  placeholder="Ex: Toyota Corolla 2023"
                  {...register('interesse')}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="orcamento">Orçamento (R$)</Label>
                <Input
                  id="orcamento"
                  type="number"
                  placeholder="150000"
                  {...register('orcamento', { valueAsNumber: true })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Urgência</Label>
                <Select
                  value={urgencia || ''}
                  onValueChange={(value: any) => setValue('urgencia', value || undefined)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Selecione" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="baixa">Baixa</SelectItem>
                    <SelectItem value="media">Média</SelectItem>
                    <SelectItem value="alta">Alta</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="tempoCompra">Previsão de Compra</Label>
                <Input
                  id="tempoCompra"
                  placeholder="Ex: 30 dias"
                  {...register('tempoCompra')}
                />
              </div>
            </div>
          </Section>

          {/* Seção 3: Informações Complementares */}
          <Section 
            title="Informações Complementares" 
            icon={<MessageSquare className="h-4 w-4 text-kanban-completed" />}
            defaultOpen={false}
          >
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="profissao">Profissão</Label>
                <Input
                  id="profissao"
                  placeholder="Ex: Engenheiro"
                  {...register('profissao')}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endereco">Endereço</Label>
                <Input
                  id="endereco"
                  placeholder="Cidade, Estado"
                  {...register('endereco')}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="observacoes">Observações</Label>
              <Textarea
                id="observacoes"
                placeholder="Notas adicionais sobre o cliente..."
                rows={3}
                {...register('observacoes')}
              />
            </div>
          </Section>

          {/* Card Info */}
          {card.vendedorNome && (
            <div className="rounded-lg bg-muted/30 p-3 text-sm">
              <span className="text-muted-foreground">Vendedor responsável: </span>
              <span className="font-medium text-foreground">{card.vendedorNome}</span>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={!isDirty}>
              Salvar Alterações
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
