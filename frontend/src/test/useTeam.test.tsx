import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useTeam } from '@/hooks/api/useTeam';
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
      role: 'MANAGER',
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

describe('useTeam', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads team with org-scoped query key and maps members', async () => {
    vi.mocked(usersService.listTeam).mockResolvedValueOnce([
      {
        id: 'seller-1',
        name: 'Carlos Vendedor',
        email: 'carlos@astro.com',
        avatarUrl: null,
        role: 'SELLER',
        status: 'ACTIVE',
      },
    ]);

    const { wrapper, queryClient } = createWrapper();
    const { result } = renderHook(() => useTeam(), { wrapper });

    await waitFor(() => expect(result.current.isReady).toBe(true));

    expect(usersService.listTeam).toHaveBeenCalledTimes(1);
    expect(result.current.team[0]).toMatchObject({
      id: 'seller-1',
      name: 'Carlos Vendedor',
      role: 'SELLER',
    });
    expect(queryClient.getQueryData(['org-1', 'settings', 'team'])).toBeTruthy();
  });

  it('returns explicit error state when team request fails', async () => {
    vi.mocked(usersService.listTeam).mockRejectedValueOnce(new Error('team failed'));

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useTeam(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.state).toBe('error');
    expect(result.current.errorMessage).toContain('team failed');
  });
});
