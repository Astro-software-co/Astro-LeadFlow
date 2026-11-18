import { FormEvent, useState } from 'react';
import { Mail, Monitor, Smartphone } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import type { SecuritySettings } from '@/types/settings';

interface ChangePasswordValues {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

interface SecurityTabProps {
  securitySettings: SecuritySettings;
  onChangePassword: (values: ChangePasswordValues) => Promise<void>;
  onToggleEmailMfa: (enabled: boolean) => Promise<void>;
  onToggleSmsMfa: (enabled: boolean) => Promise<void>;
  isSavingPassword: boolean;
  isSavingTwoFactor: boolean;
}

export function SecurityTab({
  securitySettings,
  onChangePassword,
  onToggleEmailMfa,
  onToggleSmsMfa,
  isSavingPassword,
  isSavingTwoFactor,
}: SecurityTabProps) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');

  const handleChangePassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!currentPassword || !newPassword || !confirmNewPassword) {
      toast.error('Preencha todos os campos de senha.');
      return;
    }

    if (newPassword.length < 8) {
      toast.error('A nova senha deve ter pelo menos 8 caracteres.');
      return;
    }

    if (newPassword !== confirmNewPassword) {
      toast.error('A confirmacao da senha deve ser igual a nova senha.');
      return;
    }

    try {
      await onChangePassword({
        currentPassword,
        newPassword,
        confirmNewPassword,
      });

      setCurrentPassword('');
      setNewPassword('');
      setConfirmNewPassword('');
    } catch {
      // Container already handles error messaging.
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Alterar Senha</h3>
          <p className="text-sm text-muted-foreground">Mantenha sua conta segura</p>
        </div>
        <form className="max-w-md space-y-4 p-6" onSubmit={(event) => void handleChangePassword(event)}>
          <div className="space-y-2">
            <Label htmlFor="current-password">Senha Atual</Label>
            <Input
              id="current-password"
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              placeholder="••••••••"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="new-password">Nova Senha</Label>
            <Input
              id="new-password"
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder="••••••••"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirm-password">Confirmar Nova Senha</Label>
            <Input
              id="confirm-password"
              type="password"
              value={confirmNewPassword}
              onChange={(event) => setConfirmNewPassword(event.target.value)}
              placeholder="••••••••"
            />
          </div>
          <Button type="submit" disabled={isSavingPassword}>
            {isSavingPassword ? 'Salvando...' : 'Alterar Senha'}
          </Button>
        </form>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Autenticacao em Dois Fatores (2FA)</h3>
          <p className="text-sm text-muted-foreground">Adicione uma camada extra de seguranca</p>
        </div>
        <div className="space-y-4 p-6">
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                <Mail className="h-5 w-5 text-primary" />
              </div>
              <div>
                <Label className="text-sm font-medium">Verificacao por E-mail</Label>
                <p className="text-xs text-muted-foreground">Receba um codigo de verificacao no login</p>
              </div>
            </div>
            <Switch
              checked={securitySettings.emailMfa}
              disabled={isSavingTwoFactor}
              onCheckedChange={(value) => {
                void onToggleEmailMfa(value);
              }}
            />
          </div>
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-kanban-completed/10">
                <Smartphone className="h-5 w-5 text-kanban-completed" />
              </div>
              <div>
                <Label className="text-sm font-medium">Verificacao por SMS</Label>
                <p className="text-xs text-muted-foreground">Receba um codigo via SMS ao fazer login</p>
              </div>
            </div>
            <Switch
              checked={securitySettings.smsMfa}
              disabled={isSavingTwoFactor}
              onCheckedChange={(value) => {
                void onToggleSmsMfa(value);
              }}
            />
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Sessoes Ativas</h3>
        </div>
        <div className="space-y-3 p-6">
          <div className="flex items-center justify-between rounded-lg border border-border/50 p-4">
            <div className="flex items-center gap-3">
              <Monitor className="h-5 w-5 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium text-foreground">Chrome - Windows</p>
                <p className="text-xs text-muted-foreground">Sao Paulo, BR • Sessao atual</p>
              </div>
            </div>
            <span className="text-xs font-medium text-kanban-completed">Ativa</span>
          </div>
        </div>
      </div>
    </div>
  );
}
