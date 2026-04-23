import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useProfile } from '@/hooks/api/useProfile';
import { usersService } from '@/services/users.service';

vi.mock('@/services/users.service', () => ({
  usersService: {
    getProfile: vi.fn(),
    listTeam: vi.fn(),
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

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return { wrapper, queryClient };
}

describe('useProfile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads and maps profile with org-scoped query key', async () => {
    vi.mocked(usersService.getProfile).mockResolvedValueOnce({
      id: 'user-1',
      name: 'Ana Admin',
      email: 'ana@astro.com',
      phoneNumber: '11999999999',
      avatarUrl: null,
      role: 'ADMIN',
      status: 'ACTIVE',
      organizationId: 'org-1',
      organizationName: 'Astro Motors',
      twoFactorEmailEnabled: false,
      twoFactorSmsEnabled: false,
      notifyNewLead: true,
      notifyLeadWaiting: true,
      notifySaleCompleted: true,
      notifyCardMoved: false,
      notifyViaEmail: false,
      notifyViaPush: true,
      createdAt: '2026-04-14T00:00:00.000Z',
    });

    const { wrapper, queryClient } = createWrapper();
    const { result } = renderHook(() => useProfile(), { wrapper });

    await waitFor(() => expect(result.current.isReady).toBe(true));

    expect(usersService.getProfile).toHaveBeenCalledTimes(1);
    expect(result.current.profile).toMatchObject({
      id: 'user-1',
      name: 'Ana Admin',
      organizationName: 'Astro Motors',
    });
    expect(queryClient.getQueryData(['org-1', 'settings', 'profile'])).toBeTruthy();
  });

  it('returns explicit error state when request fails', async () => {
    vi.mocked(usersService.getProfile).mockRejectedValueOnce(new Error('profile failed'));

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useProfile(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.state).toBe('error');
    expect(result.current.errorMessage).toContain('profile failed');
  });

  it('uses auth org fallback when profile organizationId is missing', async () => {
    vi.mocked(usersService.getProfile).mockResolvedValueOnce({
      id: 'user-1',
      name: 'Ana Admin',
      email: 'ana@astro.com',
      phoneNumber: null,
      avatarUrl: null,
      role: 'ADMIN',
      status: 'ACTIVE',
      organizationId: null,
      organizationName: null,
      twoFactorEmailEnabled: false,
      twoFactorSmsEnabled: false,
      notifyNewLead: true,
      notifyLeadWaiting: true,
      notifySaleCompleted: true,
      notifyCardMoved: false,
      notifyViaEmail: false,
      notifyViaPush: true,
      createdAt: '2026-04-14T00:00:00.000Z',
    });

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useProfile(), { wrapper });

    await waitFor(() => expect(result.current.isReady).toBe(true));

    expect(result.current.profile).toMatchObject({
      organizationId: 'org-1',
      organizationName: null,
      phoneNumber: null,
      avatarUrl: null,
    });
  });

  it('returns generic error message when rejection is not an Error instance', async () => {
    vi.mocked(usersService.getProfile).mockRejectedValueOnce('boom');

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useProfile(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.errorMessage).toBe('Nao foi possivel carregar o perfil.');
  });
});
