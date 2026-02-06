import { useState } from 'react';
import { MainLayout } from '@/components/layout/MainLayout';
import { AddUserModal } from '@/components/modals/AddUserModal';
import { Settings, Bell, Shield, Palette, UserPlus, Users, Trash2, Sun, Moon, Monitor } from 'lucide-react';
import { Seller, ThemeMode, AccentColor } from '@/types';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { useTheme } from '@/contexts/ThemeContext';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';

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
  const [users, setUsers] = useState<Seller[]>(initialUsers);
  const [activeSection, setActiveSection] = useState<string | null>(null);
  const { theme, setTheme, accentColor, setAccentColor } = useTheme();

  const handleAddUser = (userData: Omit<Seller, 'id'>) => {
    const newUser: Seller = {
      ...userData,
      id: crypto.randomUUID(),
    };
    setUsers((prev) => [...prev, newUser]);
  };

  const handleDeleteUser = (userId: string) => {
    setUsers((prev) => prev.filter((u) => u.id !== userId));
    toast.success('Usuário removido com sucesso!');
  };

  const handleThemeChange = (newTheme: ThemeMode) => {
    setTheme(newTheme);
    toast.success(`Tema alterado para ${themeOptions.find(t => t.value === newTheme)?.label}`);
  };

  const handleAccentColorChange = (newColor: AccentColor) => {
    setAccentColor(newColor);
    toast.success(`Cor de destaque alterada para ${accentColors.find(c => c.value === newColor)?.label}`);
  };

  return (
    <MainLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Configurações</h1>
          <p className="text-muted-foreground">Gerencie as configurações do sistema</p>
        </div>

        {/* Settings Cards */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div 
            onClick={() => setActiveSection(activeSection === 'general' ? null : 'general')}
            className={cn(
              "rounded-xl border bg-card p-6 hover:shadow-md transition-shadow cursor-pointer",
              activeSection === 'general' && "ring-2 ring-primary"
            )}
          >
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                <Settings className="h-6 w-6 text-primary" />
              </div>
              <div>
                <h3 className="font-semibold text-foreground">Geral</h3>
                <p className="text-sm text-muted-foreground">Configurações gerais</p>
              </div>
            </div>
          </div>

          <div 
            onClick={() => setActiveSection(activeSection === 'notifications' ? null : 'notifications')}
            className={cn(
              "rounded-xl border bg-card p-6 hover:shadow-md transition-shadow cursor-pointer",
              activeSection === 'notifications' && "ring-2 ring-primary"
            )}
          >
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-kanban-waiting/10">
                <Bell className="h-6 w-6 text-kanban-waiting" />
              </div>
              <div>
                <h3 className="font-semibold text-foreground">Notificações</h3>
                <p className="text-sm text-muted-foreground">Alertas e avisos</p>
              </div>
            </div>
          </div>

          <div 
            onClick={() => setActiveSection(activeSection === 'security' ? null : 'security')}
            className={cn(
              "rounded-xl border bg-card p-6 hover:shadow-md transition-shadow cursor-pointer",
              activeSection === 'security' && "ring-2 ring-primary"
            )}
          >
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-kanban-negotiation/10">
                <Shield className="h-6 w-6 text-kanban-negotiation" />
              </div>
              <div>
                <h3 className="font-semibold text-foreground">Segurança</h3>
                <p className="text-sm text-muted-foreground">Permissões</p>
              </div>
            </div>
          </div>

          <div 
            onClick={() => setActiveSection(activeSection === 'appearance' ? null : 'appearance')}
            className={cn(
              "rounded-xl border bg-card p-6 hover:shadow-md transition-shadow cursor-pointer",
              activeSection === 'appearance' && "ring-2 ring-primary"
            )}
          >
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-kanban-completed/10">
                <Palette className="h-6 w-6 text-kanban-completed" />
              </div>
              <div>
                <h3 className="font-semibold text-foreground">Aparência</h3>
                <p className="text-sm text-muted-foreground">Tema e interface</p>
              </div>
            </div>
          </div>
        </div>

        {/* Appearance Settings Panel */}
        {activeSection === 'appearance' && (
          <div className="rounded-xl border bg-card shadow-sm animate-fade-in">
            <div className="flex items-center gap-3 border-b p-6">
              <Palette className="h-5 w-5 text-muted-foreground" />
              <div>
                <h3 className="text-lg font-semibold text-foreground">Configurações de Aparência</h3>
                <p className="text-sm text-muted-foreground">Personalize a interface do sistema</p>
              </div>
            </div>
            <div className="p-6 space-y-6">
              {/* Theme Mode */}
              <div className="space-y-3">
                <Label className="text-base font-medium">Modo de Exibição</Label>
                <div className="flex gap-3">
                  {themeOptions.map((option) => (
                    <button
                      key={option.value}
                      onClick={() => handleThemeChange(option.value)}
                      className={cn(
                        "flex items-center gap-2 rounded-lg border px-4 py-3 transition-all",
                        theme === option.value
                          ? "border-primary bg-primary/10 text-primary"
                          : "border-muted hover:border-foreground/20 text-muted-foreground hover:text-foreground"
                      )}
                    >
                      {option.icon}
                      <span className="font-medium">{option.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Accent Color */}
              <div className="space-y-3">
                <Label className="text-base font-medium">Cor de Destaque</Label>
                <div className="flex gap-3">
                  {accentColors.map((color) => (
                    <button
                      key={color.value}
                      onClick={() => handleAccentColorChange(color.value)}
                      className={cn(
                        "flex items-center gap-2 rounded-lg border px-4 py-3 transition-all",
                        accentColor === color.value
                          ? "border-primary bg-primary/10"
                          : "border-muted hover:border-foreground/20"
                      )}
                    >
                      <div className={cn("h-4 w-4 rounded-full", color.color)} />
                      <span className={cn(
                        "font-medium",
                        accentColor === color.value ? "text-foreground" : "text-muted-foreground"
                      )}>
                        {color.label}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Compact Mode */}
              <div className="flex items-center justify-between rounded-lg border p-4">
                <div>
                  <Label className="text-base font-medium">Modo Compacto</Label>
                  <p className="text-sm text-muted-foreground">Reduz o espaçamento entre elementos</p>
                </div>
                <Switch />
              </div>
            </div>
          </div>
        )}

        {/* Users Section */}
        <div className="rounded-xl border bg-card shadow-sm">
          <div className="flex items-center justify-between border-b p-6">
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
                <tr className="border-b bg-muted/30">
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Nome
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    E-mail
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Permissão
                  </th>
                  <th className="px-6 py-4 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-muted/20 transition-colors">
                    <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-foreground">
                      {user.nome}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">
                      {user.email}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span
                        className={cn(
                          'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
                          user.role === 'ADMIN'
                            ? 'bg-kanban-negotiation/10 text-kanban-negotiation'
                            : 'bg-kanban-completed/10 text-kanban-completed'
                        )}
                      >
                        <div
                          className={cn(
                            'h-1.5 w-1.5 rounded-full',
                            user.role === 'ADMIN' ? 'bg-kanban-negotiation' : 'bg-kanban-completed'
                          )}
                        />
                        {user.role === 'ADMIN' ? 'Administrador' : 'Vendedor'}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button className="text-sm font-medium text-primary hover:text-primary/80 transition-colors">
                          Editar
                        </button>
                        <button
                          onClick={() => handleDeleteUser(user.id)}
                          className="text-sm font-medium text-destructive hover:text-destructive/80 transition-colors"
                        >
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

      {/* Add User Modal */}
      <AddUserModal
        open={isUserModalOpen}
        onOpenChange={setIsUserModalOpen}
        onSubmit={handleAddUser}
      />
    </MainLayout>
  );
}
