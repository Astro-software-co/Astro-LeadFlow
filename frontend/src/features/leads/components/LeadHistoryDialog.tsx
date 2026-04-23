import { Loader2 } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { formatDateTime } from '@/lib/formatters';
import type { LeadActivity } from '@/hooks/api/useLeads';
import type { Lead } from '@/types';
import { stageLabel } from '../utils';

interface LeadHistoryDialogProps {
  lead: Lead | null;
  activities?: LeadActivity[];
  isLoadingActivities: boolean;
  onOpenChange: (open: boolean) => void;
}

export function LeadHistoryDialog({ lead, activities, isLoadingActivities, onOpenChange }: LeadHistoryDialogProps) {
  return (
    <Dialog open={Boolean(lead)} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Historico do lead</DialogTitle>
        </DialogHeader>
        {lead ? (
          <div className="space-y-4">
            <div>
              <p className="font-medium text-foreground">{lead.nome}</p>
              <p className="text-sm text-muted-foreground">{lead.telefone}</p>
            </div>

            <div className="space-y-3 rounded-lg border bg-muted/20 p-4">
              {isLoadingActivities ? (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Carregando atividades...
                </div>
              ) : activities && activities.length > 0 ? (
                activities.map((activity, index) => (
                  <div key={`${activity.movedAt.toISOString()}-${index}`} className="rounded-md border bg-background p-3">
                    <p className="text-sm font-medium text-foreground">
                      {activity.fromStage
                        ? `Movido de ${stageLabel(activity.fromStage)} para ${stageLabel(activity.toStage)}`
                        : `Entrada no estagio ${stageLabel(activity.toStage)}`}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatDateTime(activity.movedAt)} · {activity.movedByName ?? 'Sistema'}
                    </p>
                  </div>
                ))
              ) : (
                <div className="space-y-1">
                  <p className="text-sm font-medium text-foreground">Sem movimentacoes registradas</p>
                  <p className="text-xs text-muted-foreground">
                    Lead criado em {formatDateTime(lead.dataCriacao)}. Estagio atual: {stageLabel(lead.estagio)}.
                  </p>
                </div>
              )}
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}
