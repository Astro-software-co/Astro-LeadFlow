import { type Dispatch, type SetStateAction } from 'react';
import { Bell, Check, Mail, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { formatTimeAgo } from '@/lib/formatters';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Switch } from '@/components/ui/switch';
import type { Notification } from '@/types';
import type { NotificationPreferences } from '@/types/settings';

const preferenceItems: Array<{
  key: keyof NotificationPreferences;
  label: string;
  desc: string;
}> = [
  { key: 'newLead', label: 'Novo lead', desc: 'Quando um novo lead entra no chatbot' },
  {
    key: 'leadWaiting',
    label: 'Lead aguardando',
    desc: 'Quando um lead esta aguardando atendimento ha mais de 10 minutos',
  },
  { key: 'saleCompleted', label: 'Venda concluida', desc: 'Quando uma venda e finalizada com sucesso' },
  { key: 'cardMoved', label: 'Card movido', desc: 'Quando um card e movido entre colunas' },
];

interface NotificationsTabProps {
  notifPrefs: NotificationPreferences;
  setNotifPrefs: Dispatch<SetStateAction<NotificationPreferences>>;
  notifications: Notification[];
  unreadCount: number;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  removeNotification: (id: string) => void;
}

export function NotificationsTab({
  notifPrefs,
  setNotifPrefs,
  notifications,
  unreadCount,
  markAsRead,
  markAllAsRead,
  removeNotification,
}: NotificationsTabProps) {
  return (
    <div className="space-y-6 animate-fade-in">
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Preferencias de Notificacao</h3>
          <p className="text-sm text-muted-foreground">Escolha quais notificacoes deseja receber</p>
        </div>
        <div className="space-y-4 p-6">
          {preferenceItems.map((item) => (
            <div key={item.key} className="flex items-center justify-between rounded-lg border border-border/50 p-4">
              <div>
                <Label className="text-sm font-medium">{item.label}</Label>
                <p className="text-xs text-muted-foreground">{item.desc}</p>
              </div>
              <Switch checked={notifPrefs[item.key]} onCheckedChange={(value) => setNotifPrefs((prev) => ({ ...prev, [item.key]: value }))} />
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Canais de Entrega</h3>
        </div>
        <div className="space-y-4 p-6">
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <Mail className="h-5 w-5 text-muted-foreground" />
              <div>
                <Label className="text-sm font-medium">E-mail</Label>
                <p className="text-xs text-muted-foreground">Receber notificacoes por e-mail</p>
              </div>
            </div>
            <Switch checked={notifPrefs.email} onCheckedChange={(value) => setNotifPrefs((prev) => ({ ...prev, email: value }))} />
          </div>
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <Bell className="h-5 w-5 text-muted-foreground" />
              <div>
                <Label className="text-sm font-medium">Push no navegador</Label>
                <p className="text-xs text-muted-foreground">Notificacoes em tempo real</p>
              </div>
            </div>
            <Switch checked={notifPrefs.push} onCheckedChange={(value) => setNotifPrefs((prev) => ({ ...prev, push: value }))} />
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="flex items-center justify-between border-b border-border/50 p-6">
          <div>
            <h3 className="text-lg font-semibold text-foreground">Notificacoes Recentes</h3>
            <p className="text-sm text-muted-foreground">{unreadCount} nao lidas</p>
          </div>
          {unreadCount > 0 && (
            <Button variant="outline" size="sm" onClick={markAllAsRead}>
              <Check className="mr-1 h-3 w-3" /> Marcar todas como lidas
            </Button>
          )}
        </div>
        <ScrollArea className="h-[400px]">
          {notifications.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12">
              <Bell className="mb-3 h-12 w-12 text-muted-foreground/30" />
              <p className="text-sm text-muted-foreground">Nenhuma notificacao</p>
            </div>
          ) : (
            <div className="divide-y divide-border/50">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  className={cn(
                    'flex items-start gap-3 px-6 py-4 transition-colors hover:bg-muted/30',
                    !notification.read && 'bg-primary/5',
                  )}
                >
                  <div
                    className={cn(
                      'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
                      notification.type === 'success'
                        ? 'bg-kanban-completed/10 text-kanban-completed'
                        : notification.type === 'warning'
                          ? 'bg-kanban-waiting/10 text-kanban-waiting'
                          : notification.type === 'error'
                            ? 'bg-destructive/10 text-destructive'
                            : 'bg-primary/10 text-primary',
                    )}
                  >
                    <Bell className="h-4 w-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground">{notification.title}</p>
                    <p className="text-xs text-muted-foreground">{notification.message}</p>
                    <p className="mt-1 text-[10px] text-muted-foreground">{formatTimeAgo(notification.createdAt)}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    {!notification.read && (
                      <button onClick={() => markAsRead(notification.id)} className="rounded p-1 hover:bg-muted" title="Marcar como lida">
                        <Check className="h-3.5 w-3.5 text-muted-foreground" />
                      </button>
                    )}
                    <button onClick={() => removeNotification(notification.id)} className="rounded p-1 hover:bg-muted" title="Remover">
                      <X className="h-3.5 w-3.5 text-muted-foreground" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </div>
    </div>
  );
}
