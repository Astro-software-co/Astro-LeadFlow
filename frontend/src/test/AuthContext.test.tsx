import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';

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
    localStorage.clear();
    vi.stubEnv('VITE_USE_MOCKS', 'true');
  });

  it('starts as anonymous when no token stored', () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );
    expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous');
  });

  it('login sets user and stores token (mock mode)', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    fireEvent.click(screen.getByText('Login'));
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });
    expect(screen.getByTestId('user-name')).toHaveTextContent('Usuário Demo');
    expect(localStorage.getItem('access_token')).toBe('mock-token');
  });

  it('logout clears user and tokens', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );

    fireEvent.click(screen.getByText('Login'));
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });

    fireEvent.click(screen.getByText('Logout'));
    expect(screen.getByTestId('auth-status')).toHaveTextContent('anonymous');
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('restores auth from stored token on mount (mock mode)', () => {
    localStorage.setItem('access_token', 'mock-token');
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>,
    );
    expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
  });

  it('throws when useAuth is used outside provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow('useAuth must be used within an AuthProvider');
    spy.mockRestore();
  });
});
