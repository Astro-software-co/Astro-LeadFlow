import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import { tokenStore } from '@/lib/api';

function makeWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}

function TestConsumer() {
  const { isAuthenticated, isLoading, user, login, logout } = useAuth();

  if (isLoading) return <p>Loading...</p>;

  return (
    <div>
      <p data-testid="auth-status">{isAuthenticated ? 'authenticated' : 'anonymous'}</p>
      {user && <p data-testid="user-name">{user.name}</p>}
      <button onClick={() => login('test@test.com', 'pass')}>Login</button>
      <button onClick={() => logout()}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  beforeEach(() => {
    tokenStore.clear();
    vi.stubEnv('VITE_USE_MOCKS', 'true');
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
    // Tokens are now in-memory (not localStorage) per SEC-001
    expect(tokenStore.accessToken).toBe('mock-token');
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
    // Tokens cleared from in-memory store per SEC-001
    expect(tokenStore.accessToken).toBeNull();
  });

  it('stays anonymous after page refresh (in-memory tokens are not persisted)', () => {
    // SEC-001: tokens are in-memory only — lost on page reload, user must re-login
    // Previously this test checked localStorage restoration; that behavior is removed.
    const Wrapper = makeWrapper();
    render(
      <Wrapper>
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      </Wrapper>,
    );
    // Without calling login(), user is always anonymous (no persistent storage)
    expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous');
  });

  it('throws when useAuth is used outside provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });
});
