import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { authService, AuthResponse, UserProfile } from '@/services/auth.service';
import { tokenStore } from '@/lib/api';

interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string, phone?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const queryClient = useQueryClient();

  const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

  useEffect(() => {
    // In-memory tokens are lost on page refresh — user must re-login.
    // This is expected behavior for SEC-001 (tokens kept in closure, not persisted).
    const token = tokenStore.accessToken;
    if (!token) {
      setIsLoading(false);
      return;
    }

    if (useMocks) {
      setUser({
        id: 'mock-user-1',
        name: 'Usuário Demo',
        email: 'demo@astro.com',
        role: 'ADMIN',
        organizationId: 'org-1',
      });
      setIsLoading(false);
      return;
    }

    authService
      .getProfile()
      .then(setUser)
      .catch(() => {
        tokenStore.clear();
      })
      .finally(() => setIsLoading(false));
  }, [useMocks]);

  const login = useCallback(async (email: string, password: string) => {
    if (useMocks) {
      tokenStore.set('mock-token', 'mock-refresh');
      setUser({
        id: 'mock-user-1',
        name: 'Usuário Demo',
        email,
        role: 'ADMIN',
        organizationId: 'org-1',
      });
      return;
    }

    const auth: AuthResponse = await authService.login({ email, password });
    tokenStore.set(auth.accessToken, auth.refreshToken);
    const profile = await authService.getProfile();
    setUser(profile);
  }, [useMocks]);

  const register = useCallback(async (name: string, email: string, password: string, phone?: string) => {
    if (useMocks) {
      tokenStore.set('mock-token', 'mock-refresh');
      setUser({
        id: 'mock-user-1',
        name,
        email,
        role: 'VENDEDOR',
        organizationId: 'org-1',
      });
      return;
    }

    const auth: AuthResponse = await authService.register({ name, email, password, phone });
    tokenStore.set(auth.accessToken, auth.refreshToken);
    const profile = await authService.getProfile();
    setUser(profile);
  }, [useMocks]);

  const logout = useCallback(() => {
    // Logout sequence: call backend first (needs refreshToken), then clear state
    const refreshToken = tokenStore.refreshToken;
    if (!useMocks && refreshToken) {
      authService.logout(refreshToken).catch(() => {});
    }
    tokenStore.clear();
    queryClient.clear(); // Wipe ALL React Query cache — prevents multi-tenant data leak
    setUser(null);
  }, [useMocks, queryClient]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
