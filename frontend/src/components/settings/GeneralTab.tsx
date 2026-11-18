import { useState } from 'react';
import { Camera, Mail, Phone, Users } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { AddUserModal } from '@/components/modals/AddUserModal';
import type { AccentColor, ThemeMode } from '@/types';
import type { SettingsLoadState, SettingsProfile, SettingsTeamMember } from '@/types/settings';
import { accentColorOptions, themeOptions } from './settings.helpers';

interface GeneralTabProps {
  profile: SettingsProfile | null;
  team: SettingsTeamMember[];
  teamState: SettingsLoadState;
  theme: ThemeMode;
  accentColor: AccentColor;
  onThemeChange: (value: ThemeMode) => void;
  onAccentColorChange: (value: AccentColor) => void;
  onRetryTeam: () => void;
  onInviteMember: (member: { nome: string; email: string; role: 'ADMIN' | 'SELLER' }) => Promise<void>;
  onDeleteMember: (memberId: string) => Promise<void>;
  isInvitingMember: boolean;
  removingMemberId: string | null;
}

export function GeneralTab({
  profile,
  team,
  teamState,
  theme,
  accentColor,
  onThemeChange,
  onAccentColorChange,
  onRetryTeam,
  onInviteMember,
  onDeleteMember,
  isInvitingMember,
  removingMemberId,
}: GeneralTabProps) {
  const [isAddUserModalOpen, setIsAddUserModalOpen] = useState(false);

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Foto de Perfil</h3>
          <p className="text-sm text-muted-foreground">Personalize sua foto de exibicao</p>
        </div>
        <div className="flex items-center gap-6 p-6">
          <div className="relative">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-muted">
              <Camera className="h-8 w-8 text-muted-foreground" />
            </div>
            <button
              aria-label="Alterar foto de perfil"
              className="absolute bottom-0 right-0 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-primary-foreground transition-colors hover:bg-primary/90"
            >
              <Camera className="h-3.5 w-3.5" />
            </button>
          </div>
          <div className="space-y-1">
            <p className="text-sm font-medium text-foreground">{profile?.name || 'Nome nao informado'}</p>
            <p className="text-xs text-muted-foreground">{profile?.email || 'E-mail nao informado'}</p>
            <Button variant="outline" size="sm" className="mt-2 text-xs">
              Alterar foto
            </Button>
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Informacoes de Contato</h3>
          <p className="text-sm text-muted-foreground">Altere seu e-mail ou telefone com verificacao</p>
        </div>
        <div className="space-y-4 p-6">
          <div className="flex items-center gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
              <Mail className="h-5 w-5 text-primary" />
            </div>
            <div className="flex-1">
              <Label className="text-sm font-medium">E-mail</Label>
              <p className="text-sm text-muted-foreground">{profile?.email || 'E-mail de acesso'}</p>
            </div>
            <Button variant="outline" size="sm" onClick={() => toast.info('Codigo de verificacao enviado para o novo e-mail')}>
              Alterar
            </Button>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-kanban-completed/10">
              <Phone className="h-5 w-5 text-kanban-completed" />
            </div>
            <div className="flex-1">
              <Label className="text-sm font-medium">Telefone</Label>
              <p className="text-sm text-muted-foreground">{profile?.phoneNumber || 'Nao informado'}</p>
            </div>
            <Button variant="outline" size="sm" onClick={() => toast.info('SMS de verificacao enviado')}>
              Alterar
            </Button>
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Aparencia</h3>
          <p className="text-sm text-muted-foreground">Personalize a interface do sistema</p>
        </div>
        <div className="space-y-6 p-6">
          <div className="space-y-3">
            <Label className="text-base font-medium">Modo de Exibicao</Label>
            <div className="flex flex-wrap gap-3">
              {themeOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => onThemeChange(option.value)}
                  className={cn(
                    'flex items-center gap-2 rounded-lg border px-4 py-3 transition-all',
                    theme === option.value
                      ? 'border-primary bg-primary/10 text-primary'
                      : 'border-border/50 text-muted-foreground hover:border-foreground/20 hover:text-foreground',
                  )}
                >
                  <option.icon className="h-4 w-4" />
                  <span className="font-medium">{option.label}</span>
                </button>
              ))}
            </div>
          </div>
          <div className="space-y-3">
            <Label className="text-base font-medium">Cor de Destaque</Label>
            <div className="flex flex-wrap gap-3">
              {accentColorOptions.map((color) => (
                <button
                  key={color.value}
                  onClick={() => onAccentColorChange(color.value)}
                  className={cn(
                    'flex items-center gap-2 rounded-lg border px-4 py-3 transition-all',
                    accentColor === color.value ? 'border-primary bg-primary/10' : 'border-border/50 hover:border-foreground/20',
                  )}
                >
                  <div className={cn('h-4 w-4 rounded-full', color.colorClass)} />
                  <span className={cn('font-medium', accentColor === color.value ? 'text-foreground' : 'text-muted-foreground')}>
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
              <p className="text-sm text-muted-foreground">Reduz o espacamento entre elementos</p>
            </div>
            <Switch id="compact-mode" />
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="flex items-center justify-between border-b border-border/50 p-6">
          <div className="flex items-center gap-3">
            <Users className="h-5 w-5 text-muted-foreground" />
            <div>
              <h3 className="text-lg font-semibold text-foreground">Membros da Equipe</h3>
              <p className="text-sm text-muted-foreground">Gerencie vendedores e administradores</p>
            </div>
          </div>
          <Button onClick={() => setIsAddUserModalOpen(true)} disabled={isInvitingMember}>
            {isInvitingMember ? 'Adicionando...' : 'Adicionar usuario'}
          </Button>
        </div>

        {teamState === 'loading' && <p className="px-6 py-12 text-center text-sm text-muted-foreground">Carregando equipe...</p>}

        {teamState === 'error' && (
          <div className="flex flex-col items-center gap-3 px-6 py-12 text-center">
            <p className="text-sm text-muted-foreground">Nao foi possivel carregar a equipe.</p>
            <Button variant="outline" onClick={onRetryTeam}>Tentar novamente</Button>
          </div>
        )}

        {teamState === 'ready' && team.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-3 px-6 py-12 text-center">
            <Users className="h-10 w-10 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground">Nenhum membro cadastrado ainda.</p>
            <Button variant="outline" onClick={() => setIsAddUserModalOpen(true)} disabled={isInvitingMember}>
              {isInvitingMember ? 'Adicionando...' : 'Adicionar usuario'}
            </Button>
          </div>
        )}

        {teamState === 'ready' && team.length > 0 && (
          <div className="overflow-x-auto px-6 py-4">
            <table className="w-full min-w-[620px] text-sm">
              <thead>
                <tr className="border-b border-border/50 text-left text-xs uppercase tracking-wide text-muted-foreground">
                  <th className="pb-3 font-medium">Nome</th>
                  <th className="pb-3 font-medium">E-mail</th>
                  <th className="pb-3 font-medium">Role</th>
                  <th className="pb-3 text-right font-medium">Acoes</th>
                </tr>
              </thead>
              <tbody>
                {team.map((member) => (
                  <tr key={member.id} className="border-b border-border/30 last:border-none">
                    <td className="py-3 text-foreground">{member.name}</td>
                    <td className="py-3 text-muted-foreground">{member.email}</td>
                    <td className="py-3">
                      <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">{member.role}</span>
                    </td>
                    <td className="py-3">
                      <div className="flex justify-end gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => toast.info('Edicao de usuario sera conectada em breve.')}
                        >
                          Editar
                        </Button>
                        <Button
                          type="button"
                          variant="destructive"
                          size="sm"
                          disabled={removingMemberId === member.id}
                          onClick={() => {
                            void onDeleteMember(member.id);
                          }}
                        >
                          {removingMemberId === member.id ? 'Removendo...' : 'Remover'}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <AddUserModal
        open={isAddUserModalOpen}
        onOpenChange={setIsAddUserModalOpen}
        onSubmit={onInviteMember}
      />
    </div>
  );
}
