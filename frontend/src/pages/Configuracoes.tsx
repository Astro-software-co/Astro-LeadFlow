import { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { toast } from 'sonner';
import { useAuth } from '@/contexts/AuthContext';
import { cn } from '@/lib/utils';
import { hasPermission } from '@/lib/rbac';
import { useTheme } from '@/contexts/ThemeContext';
import { useNotifications } from '@/contexts/NotificationContext';
import { usersService } from '@/services/users.service';
import {
  AssinaturaTab,
  GeneralTab,
  NotificationsTab,
  PaymentsTab,
  PrivacyTab,
  ReferralTab,
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
import type {
  NotificationPreferences,
  SecuritySettings,
  SettingsLoadState,
  SettingsSectionId,
  SettingsTeamMember,
} from '@/types/settings';

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
  const [isUpdatingAvatar, setIsUpdatingAvatar] = useState(false);
  const [isInvitingMember, setIsInvitingMember] = useState(false);
  const [removingMemberId, setRemovingMemberId] = useState<string | null>(null);
  const [mutatingMemberId, setMutatingMemberId] = useState<string | null>(null);

  const { user, refreshUser } = useAuth();
  const { theme, setTheme, accentColor, setAccentColor } = useTheme();
  const { notifications, unreadCount, markAsRead, markAllAsRead, removeNotification } = useNotifications();
  const canManageTeam = hasPermission(user?.role, 'team:view');
  const canManageBilling = hasPermission(user?.role, 'billing:view');
  const visibleTabs = useMemo(
    () => settingsTabs.filter((tab) => (user?.role === 'SELLER' ? tab.id === 'general' || tab.id === 'security' : true)),
    [user?.role],
  );
  const {
    profile,
    notificationPreferences,
    securitySettings: profileSecuritySettings,
    refetch: refetchProfile,
  } = useProfile();
  const { team, state: teamState, refetch: refetchTeam } = useTeam({ enabled: canManageTeam });
  const { history, state: historyState, refetch: refetchHistory } = useBillingHistory({ enabled: canManageBilling });
  const {
    data: subscription,
    isPending: isSubscriptionPending,
    isError: isSubscriptionError,
    refetch: refetchSubscription,
  } = useSubscription({ enabled: canManageBilling });

  useEffect(() => {
    if (!visibleTabs.some((tab) => tab.id === activeSection)) {
      setActiveSection(visibleTabs[0]?.id ?? 'general');
    }
  }, [activeSection, visibleTabs]);

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

  const handleUpdateAvatar = async (avatarUrl: string) => {
    if (useMocks) {
      toast.success('Foto de perfil atualizada.');
      return;
    }

    setIsUpdatingAvatar(true);
    try {
      await usersService.updateProfile({ avatarUrl });
      await Promise.all([refetchProfile(), refreshUser()]);
      toast.success('Foto de perfil atualizada.');
    } catch {
      toast.error('Nao foi possivel atualizar a foto de perfil.');
    } finally {
      setIsUpdatingAvatar(false);
    }
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

  const handleInviteMember = async (member: { nome: string; email: string; role: 'SELLER' }) => {
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

  const handleUpdateMember = async (memberId: string, values: { name: string; email: string; phoneNumber?: string | null }) => {
    if (useMocks) {
      toast.success('Usuario atualizado com sucesso!');
      return;
    }

    setMutatingMemberId(memberId);
    try {
      await usersService.updateTeamUser(memberId, values);
      await refetchTeam();
      toast.success('Usuario atualizado com sucesso!');
    } catch {
      toast.error('Nao foi possivel atualizar o usuario.');
      throw new Error('update-member-failed');
    } finally {
      setMutatingMemberId(null);
    }
  };

  const handleSuspendMember = async (member: SettingsTeamMember) => {
    if (useMocks) {
      toast.success('Usuario suspenso com sucesso!');
      return;
    }

    setMutatingMemberId(member.id);
    try {
      await usersService.suspendUser(member.id);
      await refetchTeam();
      toast.success('Usuario suspenso com sucesso!');
    } catch {
      toast.error('Nao foi possivel suspender o usuario.');
      throw new Error('suspend-member-failed');
    } finally {
      setMutatingMemberId(null);
    }
  };

  const handleReactivateMember = async (member: SettingsTeamMember) => {
    if (useMocks) {
      toast.success('Usuario reativado com sucesso!');
      return;
    }

    setMutatingMemberId(member.id);
    try {
      await usersService.reactivateUser(member.id);
      await refetchTeam();
      toast.success('Usuario reativado com sucesso!');
    } catch {
      toast.error('Nao foi possivel reativar o usuario.');
      throw new Error('reactivate-member-failed');
    } finally {
      setMutatingMemberId(null);
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

        <div className="flex gap-2 overflow-x-auto border-b border-border/50 pb-0">
          {visibleTabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveSection(tab.id)}
              className={cn(
                'flex shrink-0 items-center gap-2 border-b-2 -mb-px px-4 py-2.5 text-sm font-medium transition-colors',
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
            onUpdateAvatar={handleUpdateAvatar}
            onRetryTeam={() => {
              void refetchTeam();
            }}
            onInviteMember={handleInviteMember}
            onUpdateMember={handleUpdateMember}
            onSuspendMember={handleSuspendMember}
            onReactivateMember={handleReactivateMember}
            onDeleteMember={handleDeleteMember}
            canManageTeam={canManageTeam}
            isUpdatingAvatar={isUpdatingAvatar}
            isInvitingMember={isInvitingMember}
            removingMemberId={removingMemberId}
            mutatingMemberId={mutatingMemberId}
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

        {activeSection === 'referrals' && <ReferralTab />}

        {activeSection === 'privacy' && <PrivacyTab />}
      </div>
    </>
  );
}
