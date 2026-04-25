import type { ReactNode } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useReferrals } from '@/hooks/api/useReferrals';
import { referralsService } from '@/services/referrals.service';

vi.mock('@/services/referrals.service', () => ({
  referralsService: {
    getMyCode: vi.fn(),
    getMyReferrals: vi.fn(),
    getMyBalance: vi.fn(),
  },
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user-1',
      name: 'Ana Admin',
      email: 'ana@astro.com',
      role: 'ADMIN',
      organizationId: 'org-1',
    },
  }),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return { wrapper, queryClient };
}

describe('useReferrals', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it('composes referral settings data from prompt endpoints', async () => {
    vi.mocked(referralsService.getMyCode).mockResolvedValueOnce({
      code: 'ASTRO-1234',
      referralLink: 'https://app.astroleadflow.com/onboarding?ref=ASTRO-1234',
      approvedCount: 3,
      pendingCount: 1,
      currentTier: 'BRONZE',
      commissionRate: 0.08,
      nextTierThreshold: 10,
      balanceBrl: 0,
      referrals: [],
    });
    vi.mocked(referralsService.getMyReferrals).mockResolvedValueOnce([
      {
        organizationId: 'org-ref-1',
        companyName: 'Rota Sul Veiculos',
        status: 'APPROVED',
        planCode: 'PROFISSIONAL_MONTHLY',
        approvedAt: '2026-04-12T00:00:00.000Z',
        commissionBrl: 23.92,
      },
    ]);
    vi.mocked(referralsService.getMyBalance).mockResolvedValueOnce({
      balanceBrl: 23.92,
      pendingBrl: 12,
      withdrawalAvailable: false,
    });

    const { wrapper, queryClient } = createWrapper();
    const { result } = renderHook(() => useReferrals(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(referralsService.getMyCode).toHaveBeenCalledTimes(1);
    expect(referralsService.getMyReferrals).toHaveBeenCalledTimes(1);
    expect(referralsService.getMyBalance).toHaveBeenCalledTimes(1);
    expect(result.current.data).toMatchObject({
      code: 'ASTRO-1234',
      approvedCount: 3,
      balanceBrl: 23.92,
      pendingBrl: 12,
    });
    expect(result.current.data?.referrals[0].companyName).toBe('Rota Sul Veiculos');
    expect(queryClient.getQueryData(['org-1', 'settings', 'referrals'])).toBeTruthy();
  });

  it('returns mock referral data without API calls when VITE_USE_MOCKS=true', async () => {
    vi.resetModules();
    vi.stubEnv('VITE_USE_MOCKS', 'true');
    const { useReferrals: useReferralsMock } = await import('@/hooks/api/useReferrals');

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useReferralsMock(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.code).toBe('ASTRO-8F4A');
    expect(result.current.data?.approvedCount).toBe(8);
    expect(result.current.data?.nextTierThreshold).toBe(10);
    expect(referralsService.getMyCode).not.toHaveBeenCalled();
    expect(referralsService.getMyReferrals).not.toHaveBeenCalled();
    expect(referralsService.getMyBalance).not.toHaveBeenCalled();
  });
});
