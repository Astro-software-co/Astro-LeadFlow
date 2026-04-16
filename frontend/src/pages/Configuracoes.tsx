import { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import {
  Settings,
  Bell,
  Shield,
  CreditCard,
  Users,
  Sun,
  Moon,
  Monitor,
  Camera,
  Mail,
  Phone,
  Smartphone,
  Check,
  X,
  Download,
  PackageCheck,
} from 'lucide-react';
import { ThemeMode, AccentColor } from '@/types';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { useTheme } from '@/contexts/ThemeContext';
import { useNotifications } from '@/contexts/NotificationContext';
import { useAuth } from '@/contexts/AuthContext';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { formatTimeAgo } from '@/lib/formatters';
import { RoleGate } from '@/components/layout/RoleGate';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { useSubscription, useUsage, usePlans } from '@/hooks/api/useSaas';

const themeOptions: { value: ThemeMode; label: string; icon: React.ReactNode }[] = [
  { value: 'light', label: 'Claro', icon: <Sun className="h-4 w-4" /> },
  { value: 'dark', label: 'Escuro', icon: <Moon className="h-4 w-4" /> },
  { value: 'system', label: 'Sistema', icon: <Monitor className="h-4 w-4" /> },
];

const accentColors: { value: AccentColor; label: string; color: string }[] = [
  { value: 'purple', label: 'Roxo', color: 'bg-violet-500' },
  { value: 'blue', label: 'Azul', color: 'bg-blue-500' },
  { value: 'green', label: 'Verde', color: 'bg-emerald-500' },
  { value: 'orange', label: 'Laranja', color: 'bg-orange-500' },
];

export default function Configuracoes() {
  const [activeSection, setActiveSection] = useState<string>('general');
  const { theme, setTheme, accentColor, setAccentColor } = useTheme();
  const { user } = useAuth();
  const { notifications, unreadCount, markAsRead, markAllAsRead, removeNotification } = useNotifications();

  const profileName = user?.name ?? '';
  const profileEmail = user?.email ?? '';
  const profilePhone = '';

  // Notification preferences
  const [notifPrefs, setNotifPrefs] = useState({
    newLead: true,
    leadWaiting: true,
    saleCompleted: true,
    cardMoved: false,
    email: false,
    push: true,
  });

  // Security
  const [securitySettings, setSecuritySettings] = useState({
    emailMfa: false,
    smsMfa: false,
  });

  const tabs = [
    { id: 'general', label: 'Geral', icon: Settings },
    { id: 'notifications', label: 'Notificações', icon: Bell },
    { id: 'security', label: 'Segurança', icon: Shield },
    { id: 'payments', label: 'Pagamentos', icon: CreditCard },
    { id: 'assinatura', label: 'Assinatura', icon: PackageCheck },
  ];

  return (
    <>
      <Helmet>
        <title>Configuracoes — Astro LeadFlow</title>
        <meta name="description" content="Gerencie perfil, equipe e preferencias do sistema." />
      </Helmet>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Configurações</h1>
          <p className="text-muted-foreground">Gerencie as configurações do sistema</p>
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 border-b border-border/50 pb-0">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveSection(tab.id)}
              className={cn(
                'flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px',
                activeSection === tab.id
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-foreground',
              )}
            >
              <tab.icon className="h-4 w-4" />
              {tab.label}
            </button>
          ))}
        </div>

        {/* ============ GENERAL TAB ============ */}
        {activeSection === 'general' && (
          <div className="space-y-6 animate-fade-in">
            {/* Profile Photo */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Foto de Perfil</h3>
                <p className="text-sm text-muted-foreground">Personalize sua foto de exibição</p>
              </div>
              <div className="p-6 flex items-center gap-6">
                <div className="relative">
                  <div className="h-20 w-20 rounded-full bg-muted flex items-center justify-center">
                    <Camera className="h-8 w-8 text-muted-foreground" />
                  </div>
                  <button
                    aria-label="Alterar foto de perfil"
                    className="absolute bottom-0 right-0 h-7 w-7 rounded-full bg-primary flex items-center justify-center text-primary-foreground hover:bg-primary/90 transition-colors"
                  >
                    <Camera className="h-3.5 w-3.5" />
                  </button>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-foreground">{profileName || 'Nome nao informado'}</p>
                  <p className="text-xs text-muted-foreground">{profileEmail || 'E-mail nao informado'}</p>
                  <Button variant="outline" size="sm" className="mt-2 text-xs">
                    Alterar foto
                  </Button>
                </div>
              </div>
            </div>

            {/* Contact Info */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Informações de Contato</h3>
                <p className="text-sm text-muted-foreground">Altere seu e-mail ou telefone com verificação</p>
              </div>
              <div className="p-6 space-y-4">
                <div className="flex items-center gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                    <Mail className="h-5 w-5 text-primary" />
                  </div>
                  <div className="flex-1">
                    <Label className="text-sm font-medium">E-mail</Label>
                    <p className="text-sm text-muted-foreground">{profileEmail || 'E-mail de acesso'}</p>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => toast.info('Código de verificação enviado para o novo e-mail')}
                  >
                    Alterar
                  </Button>
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-kanban-completed/10">
                    <Phone className="h-5 w-5 text-kanban-completed" />
                  </div>
                  <div className="flex-1">
                    <Label className="text-sm font-medium">Telefone</Label>
                    <p className="text-sm text-muted-foreground">{profilePhone || 'Nao informado'}</p>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => toast.info('SMS de verificação enviado')}>
                    Alterar
                  </Button>
                </div>
              </div>
            </div>

            {/* Appearance Section inside General */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Aparência</h3>
                <p className="text-sm text-muted-foreground">Personalize a interface do sistema</p>
              </div>
              <div className="p-6 space-y-6">
                <div className="space-y-3">
                  <Label className="text-base font-medium">Modo de Exibição</Label>
                  <div className="flex gap-3">
                    {themeOptions.map((option) => (
                      <button
                        key={option.value}
                        onClick={() => {
                          setTheme(option.value);
                          toast.success(`Tema alterado para ${option.label}`);
                        }}
                        className={cn(
                          'flex items-center gap-2 rounded-lg border px-4 py-3 transition-all',
                          theme === option.value
                            ? 'border-primary bg-primary/10 text-primary'
                            : 'border-border/50 hover:border-foreground/20 text-muted-foreground hover:text-foreground',
                        )}
                      >
                        {option.icon}
                        <span className="font-medium">{option.label}</span>
                      </button>
                    ))}
                  </div>
                </div>
                <div className="space-y-3">
                  <Label className="text-base font-medium">Cor de Destaque</Label>
                  <div className="flex gap-3">
                    {accentColors.map((color) => (
                      <button
                        key={color.value}
                        onClick={() => {
                          setAccentColor(color.value);
                          toast.success(`Cor alterada para ${color.label}`);
                        }}
                        className={cn(
                          'flex items-center gap-2 rounded-lg border px-4 py-3 transition-all',
                          accentColor === color.value
                            ? 'border-primary bg-primary/10'
                            : 'border-border/50 hover:border-foreground/20',
                        )}
                      >
                        <div className={cn('h-4 w-4 rounded-full', color.color)} />
                        <span
                          className={cn(
                            'font-medium',
                            accentColor === color.value ? 'text-foreground' : 'text-muted-foreground',
                          )}
                        >
                          {color.label}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div>
                    <Label htmlFor="compact-mode" className="text-base font-medium">
                      Modo Compacto
                    </Label>
                    <p className="text-sm text-muted-foreground">Reduz o espaçamento entre elementos</p>
                  </div>
                  <Switch id="compact-mode" />
                </div>
              </div>
            </div>

            {/* Users Section */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="flex items-center justify-between border-b border-border/50 p-6">
                <div className="flex items-center gap-3">
                  <Users className="h-5 w-5 text-muted-foreground" />
                  <div>
                    <h3 className="text-lg font-semibold text-foreground">Membros da Equipe</h3>
                    <p className="text-sm text-muted-foreground">Gerencie vendedores e administradores</p>
                  </div>
                </div>
                <Button
                  onClick={() => toast.info('Fluxo de convite sera conectado com a API de equipe nas proximas fases.')}
                >
                  Convidar membro
                </Button>
              </div>
              <div className="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
                <Users className="h-10 w-10 text-muted-foreground/40" />
                <p className="text-sm text-muted-foreground">Nenhum membro cadastrado ainda.</p>
                <Button
                  variant="outline"
                  onClick={() => toast.info('Convite de membros disponivel nas proximas fases.')}
                >
                  Convidar membro
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* ============ NOTIFICATIONS TAB ============ */}
        {activeSection === 'notifications' && (
          <div className="space-y-6 animate-fade-in">
            {/* Notification Preferences */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Preferências de Notificação</h3>
                <p className="text-sm text-muted-foreground">Escolha quais notificações deseja receber</p>
              </div>
              <div className="p-6 space-y-4">
                {[
                  { key: 'newLead' as const, label: 'Novo lead', desc: 'Quando um novo lead entra no chatbot' },
                  {
                    key: 'leadWaiting' as const,
                    label: 'Lead aguardando',
                    desc: 'Quando um lead está aguardando atendimento há mais de 10 minutos',
                  },
                  {
                    key: 'saleCompleted' as const,
                    label: 'Venda concluída',
                    desc: 'Quando uma venda é finalizada com sucesso',
                  },
                  { key: 'cardMoved' as const, label: 'Card movido', desc: 'Quando um card é movido entre colunas' },
                ].map((item) => (
                  <div
                    key={item.key}
                    className="flex items-center justify-between rounded-lg border border-border/50 p-4"
                  >
                    <div>
                      <Label className="text-sm font-medium">{item.label}</Label>
                      <p className="text-xs text-muted-foreground">{item.desc}</p>
                    </div>
                    <Switch
                      checked={notifPrefs[item.key]}
                      onCheckedChange={(v) => setNotifPrefs((p) => ({ ...p, [item.key]: v }))}
                    />
                  </div>
                ))}
              </div>
            </div>

            {/* Channels */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Canais de Entrega</h3>
              </div>
              <div className="p-6 space-y-4">
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <Mail className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <Label className="text-sm font-medium">E-mail</Label>
                      <p className="text-xs text-muted-foreground">Receber notificações por e-mail</p>
                    </div>
                  </div>
                  <Switch
                    checked={notifPrefs.email}
                    onCheckedChange={(v) => setNotifPrefs((p) => ({ ...p, email: v }))}
                  />
                </div>
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <Bell className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <Label className="text-sm font-medium">Push no navegador</Label>
                      <p className="text-xs text-muted-foreground">Notificações em tempo real</p>
                    </div>
                  </div>
                  <Switch
                    checked={notifPrefs.push}
                    onCheckedChange={(v) => setNotifPrefs((p) => ({ ...p, push: v }))}
                  />
                </div>
              </div>
            </div>

            {/* Recent Notifications (synced with header) */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="flex items-center justify-between p-6 border-b border-border/50">
                <div>
                  <h3 className="text-lg font-semibold text-foreground">Notificações Recentes</h3>
                  <p className="text-sm text-muted-foreground">{unreadCount} não lidas</p>
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
                    <Bell className="h-12 w-12 text-muted-foreground/30 mb-3" />
                    <p className="text-sm text-muted-foreground">Nenhuma notificação</p>
                  </div>
                ) : (
                  <div className="divide-y divide-border/50">
                    {notifications.map((n) => (
                      <div
                        key={n.id}
                        className={cn(
                          'flex items-start gap-3 px-6 py-4 hover:bg-muted/30 transition-colors',
                          !n.read && 'bg-primary/5',
                        )}
                      >
                        <div
                          className={cn(
                            'flex h-8 w-8 shrink-0 items-center justify-center rounded-full mt-0.5',
                            n.type === 'success'
                              ? 'bg-kanban-completed/10 text-kanban-completed'
                              : n.type === 'warning'
                                ? 'bg-kanban-waiting/10 text-kanban-waiting'
                                : n.type === 'error'
                                  ? 'bg-destructive/10 text-destructive'
                                  : 'bg-primary/10 text-primary',
                          )}
                        >
                          <Bell className="h-4 w-4" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-foreground">{n.title}</p>
                          <p className="text-xs text-muted-foreground">{n.message}</p>
                          <p className="text-[10px] text-muted-foreground mt-1">{formatTimeAgo(n.createdAt)}</p>
                        </div>
                        <div className="flex items-center gap-1 shrink-0">
                          {!n.read && (
                            <button
                              onClick={() => markAsRead(n.id)}
                              className="p-1 rounded hover:bg-muted"
                              title="Marcar como lida"
                            >
                              <Check className="h-3.5 w-3.5 text-muted-foreground" />
                            </button>
                          )}
                          <button
                            onClick={() => removeNotification(n.id)}
                            className="p-1 rounded hover:bg-muted"
                            title="Remover"
                          >
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
        )}

        {/* ============ SECURITY TAB ============ */}
        {activeSection === 'security' && (
          <div className="space-y-6 animate-fade-in">
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Alterar Senha</h3>
                <p className="text-sm text-muted-foreground">Mantenha sua conta segura</p>
              </div>
              <div className="p-6 space-y-4 max-w-md">
                <div className="space-y-2">
                  <Label>Senha Atual</Label>
                  <Input type="password" placeholder="••••••••" />
                </div>
                <div className="space-y-2">
                  <Label>Nova Senha</Label>
                  <Input type="password" placeholder="••••••••" />
                </div>
                <div className="space-y-2">
                  <Label>Confirmar Nova Senha</Label>
                  <Input type="password" placeholder="••••••••" />
                </div>
                <Button onClick={() => toast.success('Senha alterada com sucesso!')}>Alterar Senha</Button>
              </div>
            </div>

            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Autenticação em Dois Fatores (2FA)</h3>
                <p className="text-sm text-muted-foreground">Adicione uma camada extra de segurança</p>
              </div>
              <div className="p-6 space-y-4">
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                      <Mail className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Verificação por E-mail</Label>
                      <p className="text-xs text-muted-foreground">
                        Receba um código de verificação no seu e-mail ao fazer login
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={securitySettings.emailMfa}
                    onCheckedChange={(v) => {
                      setSecuritySettings((p) => ({ ...p, emailMfa: v }));
                      toast.success(v ? '2FA por e-mail ativado' : '2FA por e-mail desativado');
                    }}
                  />
                </div>
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-kanban-completed/10">
                      <Smartphone className="h-5 w-5 text-kanban-completed" />
                    </div>
                    <div>
                      <Label className="text-sm font-medium">Verificação por SMS</Label>
                      <p className="text-xs text-muted-foreground">
                        Receba um código via SMS no seu celular ao fazer login
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={securitySettings.smsMfa}
                    onCheckedChange={(v) => {
                      setSecuritySettings((p) => ({ ...p, smsMfa: v }));
                      toast.success(v ? '2FA por SMS ativado' : '2FA por SMS desativado');
                    }}
                  />
                </div>
              </div>
            </div>

            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Sessões Ativas</h3>
              </div>
              <div className="p-6 space-y-3">
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <Monitor className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <p className="text-sm font-medium text-foreground">Chrome - Windows</p>
                      <p className="text-xs text-muted-foreground">São Paulo, BR • Sessão atual</p>
                    </div>
                  </div>
                  <span className="text-xs text-kanban-completed font-medium">Ativa</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============ PAYMENTS TAB ============ */}
        {activeSection === 'payments' && (
          <div className="space-y-6 animate-fade-in">
            {/* Current Plan */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Plano Atual</h3>
                <p className="text-sm text-muted-foreground">Gerencie sua assinatura e licença de uso</p>
              </div>
              <div className="p-6">
                <div className="flex items-center justify-between rounded-xl border border-primary/30 bg-primary/5 p-6">
                  <div>
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary mb-2">
                      Plano Profissional
                    </span>
                    <p className="text-2xl font-bold text-foreground">
                      R$ 299<span className="text-sm font-normal text-muted-foreground">/mês</span>
                    </p>
                    <p className="text-sm text-muted-foreground mt-1">Renovação automática em 15/03/2026</p>
                  </div>
                  <div className="space-y-2">
                    <Button variant="outline" size="sm">
                      Alterar plano
                    </Button>
                    <br />
                    <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive">
                      Cancelar
                    </Button>
                  </div>
                </div>
              </div>
            </div>

            {/* Billing History */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Histórico de Cobranças</h3>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border/50 bg-muted/30">
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        Data
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        Descrição
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        Valor
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                        NF
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {[
                      { data: '10/02/2026', desc: 'Plano Profissional - Fev/2026', valor: 'R$ 299,00', status: 'Pago' },
                      { data: '10/01/2026', desc: 'Plano Profissional - Jan/2026', valor: 'R$ 299,00', status: 'Pago' },
                      { data: '10/12/2025', desc: 'Plano Profissional - Dez/2025', valor: 'R$ 299,00', status: 'Pago' },
                    ].map((item, i) => (
                      <tr key={i} className="hover:bg-muted/20 transition-colors">
                        <td className="px-6 py-4 text-sm text-foreground">{item.data}</td>
                        <td className="px-6 py-4 text-sm text-muted-foreground">{item.desc}</td>
                        <td className="px-6 py-4 text-sm font-medium text-foreground">{item.valor}</td>
                        <td className="px-6 py-4">
                          <span className="inline-flex items-center rounded-full bg-kanban-completed/10 px-2.5 py-0.5 text-xs font-medium text-kanban-completed">
                            {item.status}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <button className="text-primary hover:text-primary/80 transition-colors">
                            <Download className="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Payment Method */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="p-6 border-b border-border/50">
                <h3 className="text-lg font-semibold text-foreground">Método de Pagamento</h3>
              </div>
              <div className="p-6">
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <CreditCard className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <p className="text-sm font-medium text-foreground">Visa •••• 4242</p>
                      <p className="text-xs text-muted-foreground">Expira em 12/2027</p>
                    </div>
                  </div>
                  <Button variant="outline" size="sm">
                    Alterar
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============ ASSINATURA TAB ============ */}
        {activeSection === 'assinatura' && (
          <RoleGate
            allowedRoles={['ADMIN']}
            fallback={
              <div className="flex flex-col items-center justify-center py-16 gap-3 text-center animate-fade-in">
                <PackageCheck className="h-12 w-12 text-muted-foreground/30" />
                <p className="text-sm text-muted-foreground">
                  Apenas administradores podem ver as informações de assinatura.
                </p>
              </div>
            }
          >
            <AssinaturaContent />
          </RoleGate>
        )}
      </div>

    </>
  );
}

// ─── Assinatura helpers ────────────────────────────────────────────────────

function usageColorClass(used: number, limit: number): string {
  const pct = limit > 0 ? (used / limit) * 100 : 0;
  if (pct < 70) return 'text-emerald-600';
  if (pct < 90) return 'text-amber-600';
  return 'text-red-600';
}

function usageProgressClass(used: number, limit: number): string {
  const pct = limit > 0 ? (used / limit) * 100 : 0;
  if (pct < 70) return '';
  if (pct < 90) return '[&>div]:bg-amber-500';
  return '[&>div]:bg-red-500';
}

function statusBadgeClass(status: string): string {
  switch (status) {
    case 'ACTIVE': return 'bg-emerald-100 text-emerald-700';
    case 'SUSPENDED': return 'bg-red-100 text-red-700';
    case 'TRIAL': return 'bg-amber-100 text-amber-700';
    default: return 'bg-slate-100 text-slate-600';
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE': return 'Ativo';
    case 'SUSPENDED': return 'Suspenso';
    case 'TRIAL': return 'Avaliação';
    default: return status;
  }
}

function AssinaturaContent() {
  const { data: subscription, isLoading: subLoading, error: subError, refetch: subRefetch } = useSubscription();
  const { data: usage, isLoading: usageLoading, error: usageError, refetch: usageRefetch } = useUsage();
  const { data: plans, isLoading: plansLoading } = usePlans();

  const isLoading = subLoading || usageLoading;
  const hasError = subError || usageError;

  if (isLoading) {
    return (
      <div className="space-y-6 animate-fade-in">
        <Skeleton className="h-32 rounded-xl" />
        <Skeleton className="h-32 rounded-xl" />
        <Skeleton className="h-48 rounded-xl" />
      </div>
    );
  }

  if (hasError) {
    return (
      <div className="rounded-xl border bg-card p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar as configuracoes</h2>
        <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
        <Button className="mt-4" onClick={() => { void subRefetch(); void usageRefetch(); }}>
          Tentar novamente
        </Button>
      </div>
    );
  }

  const renewDate = subscription?.renewsAt
    ? new Date(subscription.renewsAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
    : '—';
  const usageResetDate = usage?.renewsAt
    ? new Date(usage.renewsAt).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
    : '—';

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Card 1: Plano Atual */}
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="p-6 border-b border-border/50">
          <h3 className="text-lg font-semibold text-foreground">Plano Atual</h3>
        </div>
        <div className="p-6">
          <div className="flex items-center justify-between rounded-xl border border-primary/30 bg-primary/5 p-6">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="text-lg font-semibold text-foreground">{subscription?.planName || '—'}</span>
                <Badge className={cn('text-xs', statusBadgeClass(subscription?.status || ''))}>
                  {statusLabel(subscription?.status || '')}
                </Badge>
              </div>
              <p className="text-2xl font-bold text-foreground">
                R$ {(subscription?.monthlyPriceBrl ?? 0).toFixed(2).replace('.', ',')}
                <span className="text-sm font-normal text-muted-foreground">/mês</span>
              </p>
              <p className="text-sm text-muted-foreground">Renova em {renewDate}</p>
            </div>
            <Button variant="default" size="sm" asChild>
              <a href="mailto:vendas@astrosoftware.com.br?subject=Upgrade de plano Astro LeadFlow">
                Falar com vendas
              </a>
            </Button>
          </div>
          {subscription?.status === 'SUSPENDED' && (
            <p className="text-sm text-red-600 mt-3">
              Sua assinatura está suspensa. Fale com nossa equipe para reativar.
            </p>
          )}
          {subscription?.status === 'TRIAL' && (
            <p className="text-sm text-amber-600 mt-3">
              Plano de avaliação — expira em {renewDate}
            </p>
          )}
        </div>
      </div>

      {/* Card 2: Uso de Mensagens */}
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="p-6 border-b border-border/50">
          <h3 className="text-lg font-semibold text-foreground">Uso de Mensagens</h3>
        </div>
        <div className="p-6 space-y-3">
          <Progress
            value={usage ? (usage.globalUsed / Math.max(1, usage.globalLimit)) * 100 : 0}
            className={cn('h-3', usageProgressClass(usage?.globalUsed ?? 0, usage?.globalLimit ?? 1))}
            aria-label={`${usage?.globalUsed ?? 0} de ${usage?.globalLimit ?? 0} mensagens usadas`}
          />
          <div className="flex items-center justify-between">
            <p className={cn('text-sm font-medium', usageColorClass(usage?.globalUsed ?? 0, usage?.globalLimit ?? 1))}>
              {usage?.globalUsed ?? 0} de {usage?.globalLimit ?? 0} mensagens usadas
            </p>
            <p className="text-xs text-muted-foreground">Redefine em {usageResetDate}</p>
          </div>
        </div>
      </div>

      {/* Card 3: Integrações de Marketplace */}
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="p-6 border-b border-border/50">
          <h3 className="text-lg font-semibold text-foreground">Integrações de Marketplace</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border/50 bg-muted/30">
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Marketplace</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Usadas</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Limite</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground min-w-[120px]">Uso</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {(usage?.marketplaces ?? []).map((mkt) => (
                <tr key={mkt.marketplaceName} className="hover:bg-muted/20 transition-colors">
                  <td className="px-6 py-4 text-sm font-medium text-foreground">{mkt.marketplaceName}</td>
                  <td className="px-6 py-4">
                    <Badge className={cn('text-xs', mkt.enabled ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500')}>
                      {mkt.enabled ? 'Ativo' : 'Inativo'}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 text-sm text-foreground">{mkt.used}</td>
                  <td className="px-6 py-4 text-sm text-muted-foreground">{mkt.limit}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <Progress
                        value={mkt.limit > 0 ? (mkt.used / mkt.limit) * 100 : 0}
                        className={cn('h-2 flex-1', usageProgressClass(mkt.used, mkt.limit))}
                      />
                      <span className={cn('text-xs font-medium min-w-[32px] text-right', usageColorClass(mkt.used, mkt.limit))}>
                        {mkt.limit > 0 ? Math.round((mkt.used / mkt.limit) * 100) : 0}%
                      </span>
                    </div>
                  </td>
                </tr>
              ))}
              {(usage?.marketplaces ?? []).length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-sm text-muted-foreground">
                    Nenhuma integração de marketplace configurada.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Card 4: Planos Disponíveis */}
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="p-6 border-b border-border/50">
          <h3 className="text-lg font-semibold text-foreground">Planos Disponíveis</h3>
        </div>
        <div className="p-6">
          {plansLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
              {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-40 rounded-xl" />)}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
              {(plans ?? []).map((plan) => {
                const isCurrent = plan.id === subscription?.planId;
                return (
                  <div
                    key={plan.id}
                    className={cn(
                      'rounded-xl border p-4 space-y-3 transition-colors',
                      isCurrent
                        ? 'border-primary ring-2 ring-primary/20 bg-primary/5'
                        : 'border-border/50 hover:border-foreground/20',
                    )}
                  >
                    <div className="flex items-center justify-between">
                      <h4 className="text-sm font-semibold text-foreground">{plan.name}</h4>
                      {isCurrent && <Badge className="bg-primary/10 text-primary text-xs">Atual</Badge>}
                    </div>
                    <p className="text-xl font-bold text-foreground">
                      {plan.monthlyPriceBrl > 0
                        ? `R$ ${plan.monthlyPriceBrl.toFixed(2).replace('.', ',')}`
                        : 'Personalizado'}
                      {plan.monthlyPriceBrl > 0 && (
                        <span className="text-xs font-normal text-muted-foreground">/mês</span>
                      )}
                    </p>
                    <div className="space-y-1 text-xs text-muted-foreground">
                      <p>{plan.messageLimit.toLocaleString('pt-BR')} mensagens/mês</p>
                      <p>{plan.marketplaceLimit} marketplaces</p>
                    </div>
                    {!isCurrent && (
                      <Button variant="outline" size="sm" className="w-full" asChild>
                        <a href={`mailto:vendas@astrosoftware.com.br?subject=Interesse no plano ${plan.name} — Astro LeadFlow`}>
                          Falar com vendas
                        </a>
                      </Button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
