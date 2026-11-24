import { useState } from 'react';
import { AlertTriangle, EyeOff, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/contexts/AuthContext';
import { useAnonymizeLead, useDeleteLead } from '@/hooks/api/usePrivacy';
import { hasPermission } from '@/lib/rbac';

interface LeadPrivacyActionsProps {
  leadId: string;
  leadName?: string;
  phone: string;
  /** Chamado depois de excluir ou anonimizar (ex.: fechar o detalhe). */
  onDone?: () => void;
  compact?: boolean;
  /** No card do Kanban só cabe a exclusão; o detalhe mostra as duas ações. */
  deleteOnly?: boolean;
}

type Pending = 'delete' | 'anonymize' | null;

/** "Excluir lead" e "Anonimizar" (D13). Só aparece para MANAGER. */
export function LeadPrivacyActions({ leadId, leadName, phone, onDone, compact = false, deleteOnly = false }: LeadPrivacyActionsProps) {
  const { user } = useAuth();
  const [pending, setPending] = useState<Pending>(null);
  const [includeSamePhone, setIncludeSamePhone] = useState(false);
  const deleteLead = useDeleteLead();
  const anonymize = useAnonymizeLead();

  if (!hasPermission(user?.role, 'leads:delete')) return null;

  const confirm = async () => {
    try {
      if (pending === 'delete') {
        await deleteLead.mutateAsync({ id: leadId, options: { includeSamePhone } });
        toast.success(includeSamePhone ? 'Lead e atendimentos do mesmo telefone excluídos.' : 'Lead excluído.');
      } else {
        await anonymize.mutateAsync(phone);
        toast.success('Dados pessoais do lead anonimizados.');
      }
      setPending(null);
      onDone?.();
    } catch {
      toast.error(pending === 'delete' ? 'Não foi possível excluir o lead.' : 'Não foi possível anonimizar o lead.');
    }
  };

  const busy = deleteLead.isPending || anonymize.isPending;
  const size = compact ? 'sm' : 'default';

  return (
    <>
      <div className={deleteOnly ? 'inline-flex' : 'flex flex-col gap-2 sm:flex-row'} onClick={(e) => e.stopPropagation()}>
        {!deleteOnly && (
          <Button type="button" variant="outline" size={size} className="cursor-pointer" onClick={() => setPending('anonymize')}>
            <EyeOff className="mr-1 h-4 w-4" />
            Anonimizar
          </Button>
        )}
        <Button type="button" variant="destructive" size={size} className="cursor-pointer" onClick={() => setPending('delete')}>
          <Trash2 className="mr-1 h-4 w-4" />
          Excluir lead
        </Button>
      </div>

      <Dialog open={pending !== null} onOpenChange={(open) => !open && setPending(null)}>
        <DialogContent className="max-h-[100dvh] w-full max-w-full sm:max-w-lg" onClick={(e) => e.stopPropagation()}>
          <DialogHeader>
            <DialogTitle>{pending === 'delete' ? 'Excluir lead' : 'Anonimizar lead'}{leadName ? `: ${leadName}` : ''}</DialogTitle>
            <DialogDescription>Esta ação é irreversível.</DialogDescription>
          </DialogHeader>
          <div role="alert" className="flex gap-2 rounded-md border border-destructive/40 bg-destructive/5 p-3 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0 text-destructive" />
            {pending === 'delete' ? (
              <span>
                Serão apagados o card, o histórico de etapas, as mensagens, as sessões do bot e as propostas da Webmotors
                vinculadas. Para manter as métricas, use <strong>Anonimizar</strong>.
              </span>
            ) : (
              <span>
                Nome, telefone, e-mail, notas, mensagens e dados coletados pelo bot serão apagados. O card e as métricas
                ficam, e o contato não recebe mais mensagens do sistema.
              </span>
            )}
          </div>
          {pending === 'delete' && (
            <div className="flex items-center gap-2">
              <Checkbox
                id={`same-phone-${leadId}`}
                checked={includeSamePhone}
                onCheckedChange={(checked) => setIncludeSamePhone(checked === true)}
              />
              <Label htmlFor={`same-phone-${leadId}`} className="cursor-pointer">
                Excluir também os outros atendimentos deste telefone
              </Label>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" className="cursor-pointer" onClick={() => setPending(null)}>Cancelar</Button>
            <Button variant="destructive" className="cursor-pointer" onClick={() => void confirm()} disabled={busy}>
              {pending === 'delete' ? 'Excluir definitivamente' : 'Anonimizar definitivamente'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
