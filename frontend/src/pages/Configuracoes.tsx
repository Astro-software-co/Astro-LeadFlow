import { useState } from 'react';
import { MainLayout } from '@/components/layout/MainLayout';
import { AddUserModal } from '@/components/modals/AddUserModal';
import { EditUserModal } from '@/components/modals/EditUserModal';
import {
  Settings, Bell, Shield, CreditCard, UserPlus, Users, Trash2,
  Sun, Moon, Monitor, Camera, Mail, Phone, Smartphone, Lock, Eye, EyeOff,
  Check, X, FileText, Download,
} from 'lucide-react';
import { Seller, ThemeMode, AccentColor } from '@/types';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { useTheme } from '@/contexts/ThemeContext';
import { useNotifications } from '@/contexts/NotificationContext';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';

const initialUsers: Seller[] = [
  { id: '1', nome: 'Carlos Mendes', email: 'carlos@v3motors.com', role: 'ADMIN' },
  { id: '2', nome: 'Ana Paula', email: 'ana@v3motors.com', role: 'VENDEDOR' },
  { id: '3', nome: 'Roberto Silva', email: 'roberto@v3motors.com', role: 'VENDEDOR' },
];

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
  const [isUserModalOpen, setIsUserModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<Seller | null>(null);
  const [users, setUsers] = useState<Seller[]>(initialUsers);
  const [activeSection, setActiveSection] = useState<string>('general');
  const { theme, setTheme, accentColor, setAccentColor } = useTheme();
  const { notifications, unreadCount, markAsRead, markAllAsRead, removeNotification } = useNotifications();

  // Notification preferences
  const [notifPrefs, setNotifPrefs] = useState({
    newLead: true, leadWaiting: true, saleCompleted: true, cardMoved: false, email: false, push: true,
  });

  // Security
  const [securitySettings, setSecuritySettings] = useState({
    emailMfa: false, smsMfa: false,
  });

  const handleAddUser = (userData: Omit<Seller, 'id'>) => {
    setUsers((prev) => [...prev, { ...userData, id: crypto.randomUUID() }]);
  };

  const handleEditUser = (user: Seller) => {
    setEditingUser(user);
    setIsEditModalOpen(true);
  };

  const handleUpdateUser = (updatedUser: Seller) => {
    setUsers((prev) => prev.map((u) => (u.id === updatedUser.id ? updatedUser : u)));
  };

  const handleDeleteUser = (userId: string) => {
    setUsers((prev) => prev.filter((u) => u.id !== userId));
    toast.success('Usuário removido com sucesso!');
  };

  const formatTimeAgo = (date: Date) => {
    const now = new Date();
    const diffMs = now.getTime() - new Date(date).getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    if (diffMins < 1) return 'Agora';
    if (diffMins < 60) return `${diffMins}m atrás`;
    if (diffHours < 24) return `${diffHours}h atrás`;
    return `${Math.floor(diffHours / 24)}d atrás`;
  };

  const tabs = [
    { id: 'general', label: 'Geral', icon: Settings },
    { id: 'notifications', label: 'Notificações', icon: Bell },
    { id: 'security', label: 'Segurança', icon: Shield },
    { id: 'payments', label: 'Pagamentos', icon: CreditCard },
  ];

  return (
    <MainLayout>
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
                  : 'border-transparent text-muted-foreground hover:text-foreground'
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
                  <button className="absolute bottom-0 right-0 h-7 w-7 rounded-full bg-primary flex items-center justify-center text-primary-foreground hover:bg-primary/90 transition-colors">
                    <Camera className="h-3.5 w-3.5" />
                  </button>
                </div>
                <div className="space-y-1">
                  <p className="text-sm font-medium text-foreground">Carlos Mendes</p>
                  <p className="text-xs text-muted-foreground">carlos@v3motors.com</p>
                  <Button variant="outline" size="sm" className="mt-2 text-xs">Alterar foto</Button>
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
                    <p className="text-sm text-muted-foreground">carlos@v3motors.com</p>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => toast.info('Código de verificação enviado para o novo e-mail')}>Alterar</Button>
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-kanban-completed/10">
                    <Phone className="h-5 w-5 text-kanban-completed" />
                  </div>
                  <div className="flex-1">
                    <Label className="text-sm font-medium">Telefone</Label>
                    <p className="text-sm text-muted-foreground">(11) 99999-9999</p>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => toast.info('SMS de verificação enviado')}>Alterar</Button>
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
                        onClick={() => { setTheme(option.value); toast.success(`Tema alterado para ${option.label}`); }}
                        className={cn(
                          'flex items-center gap-2 rounded-lg border px-4 py-3 transition-all',
                          theme === option.value
                            ? 'border-primary bg-primary/10 text-primary'
                            : 'border-border/50 hover:border-foreground/20 text-muted-foreground hover:text-foreground'
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
                        onClick={() => { setAccentColor(color.value); toast.success(`Cor alterada para ${color.label}`); }}
                        className={cn(
                          'flex items-center gap-2 rounded-lg border px-4 py-3 transition-all',
                          accentColor === color.value
                            ? 'border-primary bg-primary/10'
                            : 'border-border/50 hover:border-foreground/20'
                        )}
                      >
                        <div className={cn('h-4 w-4 rounded-full', color.color)} />
                        <span className={cn('font-medium', accentColor === color.value ? 'text-foreground' : 'text-muted-foreground')}>
                          {color.label}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div>
                    <Label className="text-base font-medium">Modo Compacto</Label>
                    <p className="text-sm text-muted-foreground">Reduz o espaçamento entre elementos</p>
                  </div>
                  <Switch />
                </div>
              </div>
            </div>

            {/* Users Section */}
            <div className="rounded-xl border border-border/50 bg-card shadow-sm">
              <div className="flex items-center justify-between border-b border-border/50 p-6">
                <div className="flex items-center gap-3">
                  <Users className="h-5 w-5 text-muted-foreground" />
                  <div>
                    <h3 className="text-lg font-semibold text-foreground">Usuários do Sistema</h3>
                    <p className="text-sm text-muted-foreground">Gerencie vendedores e administradores</p>
                  </div>
                </div>
                <button
                  onClick={() => setIsUserModalOpen(true)}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
                >
                  <UserPlus className="h-4 w-4" />
                  Adicionar Usuário
                </button>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border/50 bg-muted/30">
                      <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Nome</th>
                      <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">E-mail</th>
                      <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Permissão</th>
                      <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {users.map((user) => (
                      <tr key={user.id} className="hover:bg-muted/20 transition-colors">
                        <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-foreground">{user.nome}</td>
                        <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{user.email}</td>
                        <td className="whitespace-nowrap px-6 py-4">
                          <span className={cn(
                            'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
                            user.role === 'ADMIN' ? 'bg-kanban-negotiation/10 text-kanban-negotiation' : 'bg-kanban-completed/10 text-kanban-completed'
                          )}>
                            <div className={cn('h-1.5 w-1.5 rounded-full', user.role === 'ADMIN' ? 'bg-kanban-negotiation' : 'bg-kanban-completed')} />
                            {user.role === 'ADMIN' ? 'Administrador' : 'Vendedor'}
                          </span>
                        </td>
                        <td className="whitespace-nowrap px-6 py-4">
                          <div className="flex items-center gap-2">
                            <button onClick={() => handleEditUser(user)} className="text-sm font-medium text-primary hover:text-primary/80 transition-colors">Editar</button>
                            <button onClick={() => handleDeleteUser(user.id)} className="text-sm font-medium text-destructive hover:text-destructive/80 transition-colors">
                              <Trash2 className="h-4 w-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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
                  { key: 'leadWaiting' as const, label: 'Lead aguardando', desc: 'Quando um lead está aguardando atendimento há mais de 10 minutos' },
                  { key: 'saleCompleted' as const, label: 'Venda concluída', desc: 'Quando uma venda é finalizada com sucesso' },
                  { key: 'cardMoved' as const, label: 'Card movido', desc: 'Quando um card é movido entre colunas' },
                ].map((item) => (
                  <div key={item.key} className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                    <div>
                      <Label className="text-sm font-medium">{item.label}</Label>
                      <p className="text-xs text-muted-foreground">{item.desc}</p>
                    </div>
                    <Switch checked={notifPrefs[item.key]} onCheckedChange={(v) => setNotifPrefs((p) => ({ ...p, [item.key]: v }))} />
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
                    <div><Label className="text-sm font-medium">E-mail</Label><p className="text-xs text-muted-foreground">Receber notificações por e-mail</p></div>
                  </div>
                  <Switch checked={notifPrefs.email} onCheckedChange={(v) => setNotifPrefs((p) => ({ ...p, email: v }))} />
                </div>
                <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
                  <div className="flex items-center gap-3">
                    <Bell className="h-5 w-5 text-muted-foreground" />
                    <div><Label className="text-sm font-medium">Push no navegador</Label><p className="text-xs text-muted-foreground">Notificações em tempo real</p></div>
                  </div>
                  <Switch checked={notifPrefs.push} onCheckedChange={(v) => setNotifPrefs((p) => ({ ...p, push: v }))} />
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
                      <div key={n.id} className={cn('flex items-start gap-3 px-6 py-4 hover:bg-muted/30 transition-colors', !n.read && 'bg-primary/5')}>
                        <div className={cn(
                          'flex h-8 w-8 shrink-0 items-center justify-center rounded-full mt-0.5',
                          n.type === 'success' ? 'bg-kanban-completed/10 text-kanban-completed' :
                          n.type === 'warning' ? 'bg-kanban-waiting/10 text-kanban-waiting' :
                          n.type === 'error' ? 'bg-destructive/10 text-destructive' :
                          'bg-primary/10 text-primary'
                        )}>
                          <Bell className="h-4 w-4" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-foreground">{n.title}</p>
                          <p className="text-xs text-muted-foreground">{n.message}</p>
                          <p className="text-[10px] text-muted-foreground mt-1">{formatTimeAgo(n.createdAt)}</p>
                        </div>
                        <div className="flex items-center gap-1 shrink-0">
                          {!n.read && (
                            <button onClick={() => markAsRead(n.id)} className="p-1 rounded hover:bg-muted" title="Marcar como lida">
                              <Check className="h-3.5 w-3.5 text-muted-foreground" />
                            </button>
                          )}
                          <button onClick={() => removeNotification(n.id)} className="p-1 rounded hover:bg-muted" title="Remover">
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
                      <p className="text-xs text-muted-foreground">Receba um código de verificação no seu e-mail ao fazer login</p>
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
                      <p className="text-xs text-muted-foreground">Receba um código via SMS no seu celular ao fazer login</p>
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
                    <p className="text-2xl font-bold text-foreground">R$ 299<span className="text-sm font-normal text-muted-foreground">/mês</span></p>
                    <p className="text-sm text-muted-foreground mt-1">Renovação automática em 15/03/2026</p>
                  </div>
                  <div className="space-y-2">
                    <Button variant="outline" size="sm">Alterar plano</Button>
                    <br />
                    <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive">Cancelar</Button>
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
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Data</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Descrição</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Valor</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Status</th>
                      <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">NF</th>
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
                  <Button variant="outline" size="sm">Alterar</Button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      <AddUserModal open={isUserModalOpen} onOpenChange={setIsUserModalOpen} onSubmit={handleAddUser} />
      <EditUserModal open={isEditModalOpen} onOpenChange={setIsEditModalOpen} user={editingUser} onSubmit={handleUpdateUser} />
    </MainLayout>
  );
}
