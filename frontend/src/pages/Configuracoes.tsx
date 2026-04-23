import { useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { useTheme } from '@/contexts/ThemeContext';
import { useNotifications } from '@/contexts/NotificationContext';
import { usersService } from '@/services/users.service';
import {
  AssinaturaTab,
  GeneralTab,
  NotificationsTab,
  PaymentsTab,
  SecurityTab,
  accentColorOptions,
  settingsTabs,
  themeOptions,
  useBillingHistory,
  useProfile,
  useSubscription,
  useTeam,
} from '@/modules/saas';
import type { UserProfileResponseDTO } from '@/types/api/users';
import type { NotificationPreferences, SecuritySettings, SettingsLoadState, SettingsSectionId } from '@/types/settings';

const defaultNotifPrefs: NotificationPreferences = {
  newLead: true,
  leadWaiting: true,
  saleCompleted: true,
  cardMoved: false,
  email: false,
  push: true,
};

const defaultSecuritySettings: SecuritySettings = {
  emailMfa: false,
  smsMfa: false,
};

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

function labelForTheme(value: string): string {
  return themeOptions.find((option) => option.value === value)?.label ?? value;
}

function labelForAccent(value: string): string {
  return accentColorOptions.find((option) => option.value === value)?.label ?? value;
}

function mapNotificationPreferences(dto: UserProfileResponseDTO): NotificationPreferences {
  return {
    newLead: dto.notifyNewLead,
    leadWaiting: dto.notifyLeadWaiting,
    saleCompleted: dto.notifySaleCompleted,
    cardMoved: dto.notifyCardMoved,
    email: dto.notifyViaEmail,
    push: dto.notifyViaPush,
  };
}

function mapSecuritySettings(dto: UserProfileResponseDTO): SecuritySettings {
  return {
    emailMfa: dto.twoFactorEmailEnabled,
    smsMfa: dto.twoFactorSmsEnabled,
  };
}

function sameNotificationPreferences(
  a: NotificationPreferences,
  b: NotificationPreferences,
): boolean {
  return (
    a.newLead === b.newLead
    && a.leadWaiting === b.leadWaiting
    && a.saleCompleted === b.saleCompleted
    && a.cardMoved === b.cardMoved
    && a.email === b.email
    && a.push === b.push
  );
}

function sameSecuritySettings(a: SecuritySettings, b: SecuritySettings): boolean {
  return a.emailMfa === b.emailMfa && a.smsMfa === b.smsMfa;
}

function toNotificationsPayload(preferences: NotificationPreferences) {
  return {
    notifyNewLead: preferences.newLead,
    notifyLeadWaiting: preferences.leadWaiting,
    notifySaleCompleted: preferences.saleCompleted,
    notifyCardMoved: preferences.cardMoved,
    notifyViaEmail: preferences.email,
    notifyViaPush: preferences.push,
  };
}

export default function Configuracoes() {
  const [activeSection, setActiveSection] = useState<SettingsSectionId>('general');
  const [notifPrefs, setNotifPrefs] = useState<NotificationPreferences>(defaultNotifPrefs);
  const [securitySettings, setSecuritySettings] = useState<SecuritySettings>(defaultSecuritySettings);
  const [isSavingNotifications, setIsSavingNotifications] = useState(false);
  const [isSavingTwoFactor, setIsSavingTwoFactor] = useState(false);
  const [isSavingPassword, setIsSavingPassword] = useState(false);
  const [isInvitingMember, setIsInvitingMember] = useState(false);
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null);

  const { theme, setTheme, accentColor, setAccentColor } = useTheme();
  const { notifications, unreadCount, markAsRead, markAllAsRead, removeNotification } = useNotifications();
  const {
    profile,
    notificationPreferences,
    securitySettings: profileSecuritySettings,
  } = useProfile();
  const { team, state: teamState, refetch: refetchTeam } = useTeam();
  const { history, state: historyState, refetch: refetchHistory } = useBillingHistory();
  const {
    data: subscription,
    isPending: isSubscriptionPending,
    isError: isSubscriptionError,
    refetch: refetchSubscription,
  } = useSubscription();

  useEffect(() => {
    if (notificationPreferences) {
      setNotifPrefs((current) => (
        sameNotificationPreferences(current, notificationPreferences)
          ? current
          : notificationPreferences
      ));
    }
  }, [notificationPreferences]);

  useEffect(() => {
    if (profileSecuritySettings) {
      setSecuritySettings((current) => (
        sameSecuritySettings(current, profileSecuritySettings)
          ? current
          : profileSecuritySettings
      ));
    }
  }, [profileSecuritySettings]);

  const applyProfileSettings = (userProfile: UserProfileResponseDTO) => {
    setNotifPrefs(mapNotificationPreferences(userProfile));
    setSecuritySettings(mapSecuritySettings(userProfile));
  };

  const handleToggleNotificationPreference = async (
    key: keyof NotificationPreferences,
    value: boolean,
  ) => {
    const previous = notifPrefs;
    const next = { ...notifPrefs, [key]: value };
    setNotifPrefs(next);

    if (useMocks) {
      return;
    }

    setIsSavingNotifications(true);
    try {
      const response = await usersService.updateNotifications(toNotificationsPayload(next));
      applyProfileSettings(response);
      toast.success('Preferencias de notificacao atualizadas.');
    } catch {
      setNotifPrefs(previous);
      toast.error('Nao foi possivel atualizar as preferencias de notificacao.');
      throw new Error('notification-update-failed');
    } finally {
      setIsSavingNotifications(false);
    }
  };

  const handleToggleTwoFactor = async (
    channel: 'emailMfa' | 'smsMfa',
    enabled: boolean,
  ) => {
    const previous = securitySettings;
    const next = { ...securitySettings, [channel]: enabled };
    setSecuritySettings(next);

    if (useMocks) {
      return;
    }

    setIsSavingTwoFactor(true);
    try {
      const response = await usersService.updateTwoFactor({
        emailEnabled: next.emailMfa,
        smsEnabled: next.smsMfa,
      });
      applyProfileSettings(response);
      toast.success('Configuracao de 2FA atualizada.');
    } catch {
      setSecuritySettings(previous);
      toast.error('Nao foi possivel atualizar a configuracao de 2FA.');
      throw new Error('two-factor-update-failed');
    } finally {
      setIsSavingTwoFactor(false);
    }
  };

  const handleChangePassword = async (values: {
    currentPassword: string;
    newPassword: string;
    confirmNewPassword: string;
  }) => {
    if (useMocks) {
      toast.success('Senha alterada com sucesso!');
      return;
    }

    setIsSavingPassword(true);
    try {
      await usersService.changePassword(values);
      toast.success('Senha alterada com sucesso!');
    } catch {
      toast.error('Nao foi possivel alterar a senha. Verifique os dados informados.');
      throw new Error('password-update-failed');
    } finally {
      setIsSavingPassword(false);
    }
  };

  const handleInviteMember = async (member: { nome: string; email: string; role: 'ADMIN' | 'SELLER' }) => {
    if (useMocks) {
      toast.success('Usuario cadastrado com sucesso!');
      return;
    }

    setIsInvitingMember(true);
    try {
      await usersService.inviteUser({
        name: member.nome,
        email: member.email,
        role: member.role,
      });
      await refetchTeam();
      toast.success('Usuario cadastrado com sucesso!');
    } catch {
      toast.error('Nao foi possivel cadastrar o usuario.');
      throw new Error('invite-member-failed');
    } finally {
      setIsInvitingMember(false);
    }
  };

  const handleDeleteMember = async (memberId: string) => {
    if (useMocks) {
      toast.success('Usuario removido com sucesso!');
      return;
    }

    setRemovingMemberId(memberId);
    try {
      await usersService.deleteUser(memberId);
      await refetchTeam();
      toast.success('Usuario removido com sucesso!');
    } catch {
      toast.error('Nao foi possivel remover o usuario.');
      throw new Error('delete-member-failed');
    } finally {
      setRemovingMemberId(null);
    }
  };

  const subscriptionState: SettingsLoadState = isSubscriptionPending
    ? 'loading'
    : isSubscriptionError
      ? 'error'
      : 'ready';

  return (
    <>
      <Helmet>
        <title>Configuracoes — Astro LeadFlow</title>
        <meta name="description" content="Gerencie perfil, equipe e preferencias do sistema." />
      </Helmet>

      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Configuracoes</h1>
          <p className="text-muted-foreground">Gerencie as configuracoes do sistema</p>
        </div>

        <div className="flex gap-2 border-b border-border/50 pb-0">
          {settingsTabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveSection(tab.id)}
              className={cn(
                'flex items-center gap-2 border-b-2 -mb-px px-4 py-2.5 text-sm font-medium transition-colors',
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

        {activeSection === 'general' && (
          <GeneralTab
            profile={profile}
            team={team}
            teamState={teamState}
            theme={theme}
            accentColor={accentColor}
            onThemeChange={(value) => {
              setTheme(value);
              toast.success(`Tema alterado para ${labelForTheme(value)}`);
            }}
            onAccentColorChange={(value) => {
              setAccentColor(value);
              toast.success(`Cor alterada para ${labelForAccent(value)}`);
            }}
            onRetryTeam={() => {
              void refetchTeam();
            }}
            onInviteMember={handleInviteMember}
            onDeleteMember={handleDeleteMember}
            isInvitingMember={isInvitingMember}
            removingMemberId={removingMemberId}
          />
        )}

        {activeSection === 'notifications' && (
          <NotificationsTab
            notifPrefs={notifPrefs}
            notifications={notifications}
            unreadCount={unreadCount}
            markAsRead={markAsRead}
            markAllAsRead={markAllAsRead}
            removeNotification={removeNotification}
            onTogglePreference={handleToggleNotificationPreference}
            isSavingPreferences={isSavingNotifications}
          />
        )}

        {activeSection === 'security' && (
          <SecurityTab
            securitySettings={securitySettings}
            onChangePassword={handleChangePassword}
            onToggleEmailMfa={async (enabled) => handleToggleTwoFactor('emailMfa', enabled)}
            onToggleSmsMfa={async (enabled) => handleToggleTwoFactor('smsMfa', enabled)}
            isSavingPassword={isSavingPassword}
            isSavingTwoFactor={isSavingTwoFactor}
          />
        )}

        {activeSection === 'payments' && (
          <PaymentsTab
            history={history}
            historyState={historyState}
            onRetryHistory={() => {
              void refetchHistory();
            }}
            subscription={subscription ?? null}
            subscriptionState={subscriptionState}
            onRetrySubscription={() => {
              void refetchSubscription();
            }}
          />
        )}

        {activeSection === 'assinatura' && <AssinaturaTab />}
      </div>
    </>
  );
}
