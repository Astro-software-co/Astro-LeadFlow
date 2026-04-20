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
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const queryClient = useQueryClient();

  const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

  useEffect(() => {
    if (useMocks) {
      const token = tokenStore.accessToken;
      if (!token) {
        setIsLoading(false);
        return;
      }

      setUser({
        id: 'mock-user-1',
        name: 'Usuario Demo',
        email: 'demo@astro.com',
        role: 'ADMIN',
        organizationId: 'org-1',
      });
      setIsLoading(false);
      return;
    }

    const hydrateSession = async () => {
      const token = tokenStore.accessToken;
      if (token) {
        const profile = await authService.getProfile();
        tokenStore.set(token);
        setUser(profile);
        return;
      }

      const auth = await authService.restoreSession();
      tokenStore.set(auth.accessToken);
      const profile = await authService.getProfile();
      setUser(profile);
    };

    hydrateSession()
      .catch(() => {
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, [useMocks]);

  const login = useCallback(
    async (email: string, password: string) => {
      if (useMocks) {
        tokenStore.set('mock-token');
        setUser({
          id: 'mock-user-1',
          name: 'Usuario Demo',
          email,
          role: 'ADMIN',
          organizationId: 'org-1',
        });
        return;
      }

      const auth: AuthResponse = await authService.login({ email, password });
      tokenStore.set(auth.accessToken);
      const profile = await authService.getProfile();
      setUser(profile);
    },
    [useMocks],
  );

  const register = useCallback(
    async (name: string, email: string, password: string, _phone?: string) => {
      if (useMocks) {
        tokenStore.set('mock-token');
        setUser({
          id: 'mock-user-1',
          name,
          email,
          role: 'VENDEDOR',
          organizationId: 'org-1',
        });
        return;
      }

      const normalizedSlug = email
        .split('@')[0]
        .toLowerCase()
        .replace(/[^a-z0-9-]/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');

      const auth: AuthResponse = await authService.register({
        organizationName: `${name} Organizacao`,
        slug: normalizedSlug || 'organizacao',
        adminName: name,
        adminEmail: email,
        password,
      });
      tokenStore.set(auth.accessToken);
      const profile = await authService.getProfile();
      setUser(profile);
    },
    [useMocks],
  );

  const clearSession = useCallback(() => {
    tokenStore.clear();
    queryClient.clear();
    setUser(null);
  }, [queryClient]);

  const logout = useCallback(async () => {
    if (!useMocks) {
      try {
        await authService.logout();
      } catch {
        // Best-effort revocation: local session must still be cleared.
      }
    }

    clearSession();
  }, [clearSession, useMocks]);

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
