import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import { tokenStore } from '@/lib/api';
import { authService } from '@/services/auth.service';

vi.mock('@/services/auth.service', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    getProfile: vi.fn(),
    logout: vi.fn(),
  },
}));

function makeWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

function TestConsumer() {
  const { isAuthenticated, isLoading, user, login, register, logout } = useAuth();

  if (isLoading) return <p>Loading...</p>;

  return (
    <div>
      <p data-testid="auth-status">{isAuthenticated ? 'authenticated' : 'anonymous'}</p>
      {user && <p data-testid="user-name">{user.name}</p>}
      <button onClick={() => login('test@test.com', 'pass')}>Login</button>
      <button onClick={() => register('Novo Admin', 'novo@astro.test', 'SenhaForte123')}>Register</button>
      <button onClick={() => logout()}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    tokenStore.clear();
    vi.resetAllMocks();
    vi.stubEnv('VITE_USE_MOCKS', 'true');
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('starts as anonymous when no token stored', () => {
    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );
    expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous');
  });

  it('login sets user and stores token (mock mode)', async () => {
    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    fireEvent.click(screen.getByText('Login'));
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });
    expect(screen.getByTestId('user-name')).toHaveTextContent('Usuário Demo');
    expect(tokenStore.accessToken).toBe('mock-token');
  });

  it('register sets VENDEDOR user in mock mode', async () => {
    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    fireEvent.click(screen.getByText('Register'));
    await waitFor(() => expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated'));
    expect(screen.getByTestId('user-name')).toHaveTextContent('Novo Admin');
    expect(tokenStore.refreshToken).toBe('mock-refresh');
  });

  it('logout clears user and tokens', async () => {
    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    fireEvent.click(screen.getByText('Login'));
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });

    fireEvent.click(screen.getByText('Logout'));
    expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous');
    expect(tokenStore.accessToken).toBeNull();
  });

  it('loads profile from API when a token exists and mock mode is disabled', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'false');
    tokenStore.set('persisted-access', 'persisted-refresh');
    vi.mocked(authService.getProfile).mockResolvedValueOnce({
      id: 'u-1',
      name: 'Real User',
      email: 'real@astro.test',
      role: 'ADMIN',
      organizationId: 'org-real',
    });

    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    await waitFor(() => expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated'));
    expect(authService.getProfile).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('user-name')).toHaveTextContent('Real User');
  });

  it('clears stale token when profile fetch fails during bootstrap', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'false');
    tokenStore.set('stale-access', 'stale-refresh');
    vi.mocked(authService.getProfile).mockRejectedValueOnce(new Error('token-invalid'));

    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    await waitFor(() => expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous'));
    expect(tokenStore.accessToken).toBeNull();
    expect(tokenStore.refreshToken).toBeNull();
  });

  it('calls authService login/profile flow when mock mode is disabled', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'false');
    vi.mocked(authService.login).mockResolvedValueOnce({
      accessToken: 'api-access',
      refreshToken: 'api-refresh',
      pendingTwoFactor: false,
    });
    vi.mocked(authService.getProfile).mockResolvedValueOnce({
      id: 'u-2',
      name: 'API User',
      email: 'api@astro.test',
      role: 'ADMIN',
      organizationId: 'org-api',
    });

    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    fireEvent.click(screen.getByText('Login'));

    await waitFor(() => expect(authService.login).toHaveBeenCalledWith({ email: 'test@test.com', password: 'pass' }));
    await waitFor(() => expect(authService.getProfile).toHaveBeenCalled());
    expect(tokenStore.accessToken).toBe('api-access');
  });

  it('calls authService register flow with normalized slug when mock mode is disabled', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'false');
    vi.mocked(authService.register).mockResolvedValueOnce({
      accessToken: 'registered-access',
      refreshToken: 'registered-refresh',
      pendingTwoFactor: false,
    });
    vi.mocked(authService.getProfile).mockResolvedValueOnce({
      id: 'u-3',
      name: 'Novo Admin',
      email: 'novo@astro.test',
      role: 'ADMIN',
      organizationId: 'org-new',
    });

    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    fireEvent.click(screen.getByText('Register'));

    await waitFor(() =>
      expect(authService.register).toHaveBeenCalledWith({
        organizationName: 'Novo Admin Organizacao',
        slug: 'novo',
        adminName: 'Novo Admin',
        adminEmail: 'novo@astro.test',
        password: 'SenhaForte123',
      }),
    );
    expect(tokenStore.accessToken).toBe('registered-access');
  });

  it('calls backend logout in non-mock mode and still clears local state', async () => {
    vi.stubEnv('VITE_USE_MOCKS', 'false');
    vi.mocked(authService.login).mockResolvedValueOnce({
      accessToken: 'a-1',
      refreshToken: 'r-1',
      pendingTwoFactor: false,
    });
    vi.mocked(authService.getProfile).mockResolvedValueOnce({
      id: 'u-4',
      name: 'Logout User',
      email: 'logout@astro.test',
      role: 'ADMIN',
      organizationId: 'org-logout',
    });
    vi.mocked(authService.logout).mockResolvedValueOnce(undefined);

    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );

    fireEvent.click(screen.getByText('Login'));
    await waitFor(() => expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated'));

    fireEvent.click(screen.getByText('Logout'));
    await waitFor(() => expect(authService.logout).toHaveBeenCalledTimes(1));
    expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous');
  });

  it('throws when useAuth is used outside provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });
});
