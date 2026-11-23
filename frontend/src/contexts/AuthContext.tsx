import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { authService, type AuthResponse, type UserProfile } from '@/services/auth.service';
import { tokenStore } from '@/lib/api';
import { onAuthSessionEnded } from '@/lib/auth-events';

interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<UserProfile>;
  register: (name: string, email: string, password: string, phone?: string) => Promise<UserProfile>;
  refreshUser: () => Promise<UserProfile | null>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const SESSION_KEEPALIVE_MIN_INTERVAL_MS = 4 * 60 * 1000;
const RECENT_ACTIVITY_WINDOW_MS = 75 * 1000;
const ACTIVITY_EVENTS: Array<keyof WindowEventMap> = ['click', 'focus', 'keydown', 'mousemove', 'scroll', 'touchstart'];

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const queryClient = useQueryClient();

  const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

  const clearSession = useCallback(() => {
    tokenStore.clear();
    queryClient.clear();
    setUser(null);
  }, [queryClient]);

  const refreshUser = useCallback(async () => {
    if (useMocks) {
      return null;
    }

    const profile = await authService.getProfile();
    setUser(profile);
    return profile;
  }, [useMocks]);

  useEffect(() => {
    if (useMocks) {
      return;
    }

    return onAuthSessionEnded(() => {
      clearSession();
    });
  }, [clearSession, useMocks]);

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
        role: 'MANAGER',
        organizationId: 'org-1',
        avatarUrl: null,
      });
      setIsLoading(false);
      return;
    }

    const hydrateSession = async () => {
      const token = tokenStore.accessToken;
      if (token) {
        tokenStore.set(token);
        await refreshUser();
        return;
      }

      const auth = await authService.restoreSession();
      tokenStore.set(auth.accessToken);
      await refreshUser();
    };

    hydrateSession()
      .catch(() => {
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setIsLoading(false));
  }, [refreshUser, useMocks]);

  useEffect(() => {
    if (useMocks || !user) {
      return;
    }

    let keepAliveInFlight = false;
    let lastActivityAt = Date.now();
    let lastKeepAliveAt = Date.now();

    const markActivity = () => {
      lastActivityAt = Date.now();
    };

    const maybeKeepAlive = async () => {
      if (keepAliveInFlight) {
        return;
      }

      if (typeof document !== 'undefined' && document.visibilityState === 'hidden') {
        return;
      }

      const now = Date.now();
      if (now - lastActivityAt > RECENT_ACTIVITY_WINDOW_MS) {
        return;
      }

      if (now - lastKeepAliveAt < SESSION_KEEPALIVE_MIN_INTERVAL_MS) {
        return;
      }

      keepAliveInFlight = true;
      try {
        const auth = await authService.restoreSession();
        tokenStore.set(auth.accessToken);
        lastKeepAliveAt = Date.now();
      } finally {
        keepAliveInFlight = false;
      }
    };

    ACTIVITY_EVENTS.forEach((eventName) => {
      window.addEventListener(eventName, markActivity, { passive: true });
    });

    const intervalId = window.setInterval(() => {
      void maybeKeepAlive();
    }, 60_000);

    return () => {
      ACTIVITY_EVENTS.forEach((eventName) => {
        window.removeEventListener(eventName, markActivity);
      });
      window.clearInterval(intervalId);
    };
  }, [user, useMocks]);

  const login = useCallback(
    async (email: string, password: string) => {
      if (useMocks) {
        tokenStore.set('mock-token');
        const mockUser = {
          id: 'mock-user-1',
          name: 'Usuario Demo',
          email,
          role: 'MANAGER',
          organizationId: 'org-1',
          avatarUrl: null,
        };
        setUser(mockUser);
        return mockUser;
      }

      const auth: AuthResponse = await authService.login({ email, password });
      tokenStore.set(auth.accessToken);
      const profile = await authService.getProfile();
      setUser(profile);
      return profile;
    },
    [useMocks],
  );

  const register = useCallback(
    async (name: string, email: string, password: string, _phone?: string) => {
      if (useMocks) {
        tokenStore.set('mock-token');
        const mockUser = {
          id: 'mock-user-1',
          name,
          email,
          role: 'MANAGER',
          organizationId: 'org-1',
          avatarUrl: null,
        };
        setUser(mockUser);
        return mockUser;
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
      return profile;
    },
    [useMocks],
  );

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
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, register, refreshUser, logout }}>
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
