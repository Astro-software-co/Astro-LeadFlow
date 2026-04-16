import { type Dispatch, type SetStateAction } from 'react';
import { Mail, Monitor, Smartphone } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import type { SecuritySettings } from '@/types/settings';

interface SecurityTabProps {
  securitySettings: SecuritySettings;
  setSecuritySettings: Dispatch<SetStateAction<SecuritySettings>>;
}

export function SecurityTab({ securitySettings, setSecuritySettings }: SecurityTabProps) {
  return (
    <div className="space-y-6 animate-fade-in">
      <div className="rounded-xl border border-border/50 bg-card shadow-sm">
        <div className="border-b border-border/50 p-6">
          <h3 className="text-lg font-semibold text-foreground">Alterar Senha</h3>
          <p className="text-sm text-muted-foreground">Mantenha sua conta segura</p>
        </div>
        <div className="max-w-md space-y-4 p-6">
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
              onCheckedChange={(value) => {
                setSecuritySettings((prev) => ({ ...prev, emailMfa: value }));
                toast.success(value ? '2FA por e-mail ativado' : '2FA por e-mail desativado');
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
              onCheckedChange={(value) => {
                setSecuritySettings((prev) => ({ ...prev, smsMfa: value }));
                toast.success(value ? '2FA por SMS ativado' : '2FA por SMS desativado');
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
