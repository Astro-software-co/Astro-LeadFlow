import { fireEvent, render, screen } from '@testing-library/react';
import { HelmetProvider } from 'react-helmet-async';
import { describe, expect, it, vi } from 'vitest';
import Configuracoes from '@/pages/Configuracoes';

const profileHookResult = {
  profile: {
    id: 'user-1',
    name: 'Ana Admin',
    email: 'ana@astro.com',
    phoneNumber: null,
    avatarUrl: null,
    role: 'ADMIN',
    organizationId: 'org-1',
    organizationName: 'Astro Motors',
  },
  notificationPreferences: {
    newLead: true,
    leadWaiting: true,
    saleCompleted: true,
    cardMoved: false,
    email: false,
    push: true,
  },
  securitySettings: {
    emailMfa: false,
    smsMfa: false,
  },
  state: 'ready' as const,
  isLoading: false,
  isError: false,
  isReady: true,
  errorMessage: null,
  refetch: vi.fn(),
};

const teamHookResult = {
  team: [],
  state: 'ready' as const,
  isLoading: false,
  isError: false,
  isReady: true,
  errorMessage: null,
  refetch: vi.fn(),
};

const billingHookResult = {
  history: [],
  state: 'ready' as const,
  isLoading: false,
  isError: false,
  isReady: true,
  errorMessage: null,
  refetch: vi.fn(),
};

const subscriptionHookResult = {
  data: {
    planId: 'plan-pro',
    planName: 'Profissional',
    planDescription: 'Plano Profissional para operacoes com mais volume.',
    priceBrl: 299,
    billingCycle: 'MONTHLY' as const,
    billingCycleLabel: 'Mensal',
    messageLimit: 1000,
    marketplaceLimit: 3,
    status: 'ACTIVE' as const,
    renewsAt: '2026-05-13T00:00:00.000Z',
  },
  isPending: false,
  isError: false,
  refetch: vi.fn(),
};

vi.mock('@/contexts/ThemeContext', () => ({
  useTheme: () => ({
    theme: 'light',
    setTheme: vi.fn(),
    accentColor: 'blue',
    setAccentColor: vi.fn(),
  }),
}));

vi.mock('@/contexts/NotificationContext', () => ({
  useNotifications: () => ({
    notifications: [],
    unreadCount: 0,
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
    removeNotification: vi.fn(),
  }),
}));

vi.mock('@/hooks/api/useProfile', () => ({
  useProfile: () => profileHookResult,
}));

vi.mock('@/hooks/api/useTeam', () => ({
  useTeam: () => teamHookResult,
}));

vi.mock('@/hooks/api/useBillingHistory', () => ({
  useBillingHistory: () => billingHookResult,
}));

vi.mock('@/hooks/api/useSaas', () => ({
  useSubscription: () => subscriptionHookResult,
}));

vi.mock('@/components/settings/GeneralTab', () => ({
  GeneralTab: () => <div data-testid="general-tab">General tab</div>,
}));

vi.mock('@/components/settings/NotificationsTab', () => ({
  NotificationsTab: () => <div data-testid="notifications-tab">Notifications tab</div>,
}));

vi.mock('@/components/settings/SecurityTab', () => ({
  SecurityTab: () => <div data-testid="security-tab">Security tab</div>,
}));

vi.mock('@/components/settings/PaymentsTab', () => ({
  PaymentsTab: () => <div data-testid="payments-tab">Payments tab</div>,
}));

vi.mock('@/components/settings/AssinaturaTab', () => ({
  AssinaturaTab: () => <div data-testid="assinatura-tab">Assinatura tab</div>,
}));

describe('Configuracoes page container', () => {
  it('renders and switches tab composition from container state', async () => {
    render(
      <HelmetProvider>
        <Configuracoes />
      </HelmetProvider>,
    );

    expect(screen.getByText('Configuracoes')).toBeInTheDocument();
    expect(screen.getByTestId('general-tab')).toBeInTheDocument();

    const notifBtn = screen.getByRole('button', { name: /Notificacoes/i });
    fireEvent.click(notifBtn);
    expect(await screen.findByTestId('notifications-tab')).toBeInTheDocument();

    const securityBtn = screen.getByRole('button', { name: /Seguranca/i });
    fireEvent.click(securityBtn);
    expect(await screen.findByTestId('security-tab')).toBeInTheDocument();

    const paymentsBtn = screen.getByRole('button', { name: /Pagamentos/i });
    fireEvent.click(paymentsBtn);
    expect(await screen.findByTestId('payments-tab')).toBeInTheDocument();

    const subBtn = screen.getByRole('button', { name: /Assinatura/i });
    fireEvent.click(subBtn);
    expect(await screen.findByTestId('assinatura-tab')).toBeInTheDocument();
  }, 10000);
});
