import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { authService, AuthResponse, UserProfile } from '@/services/auth.service';

interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string, phone?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function storeTokens(auth: AuthResponse) {
  localStorage.setItem('access_token', auth.accessToken);
  localStorage.setItem('refresh_token', auth.refreshToken);
}

function clearTokens() {
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

  useEffect(() => {
    const token = localStorage.getItem('access_token');
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
        clearTokens();
      })
      .finally(() => setIsLoading(false));
  }, [useMocks]);

  const login = useCallback(async (email: string, password: string) => {
    if (useMocks) {
      storeTokens({ accessToken: 'mock-token', refreshToken: 'mock-refresh', expiresIn: 3600 });
      setUser({
        id: 'mock-user-1',
        name: 'Usuário Demo',
        email,
        role: 'ADMIN',
        organizationId: 'org-1',
      });
      return;
    }

    const auth = await authService.login({ email, password });
    storeTokens(auth);
    const profile = await authService.getProfile();
    setUser(profile);
  }, [useMocks]);

  const register = useCallback(async (name: string, email: string, password: string, phone?: string) => {
    if (useMocks) {
      storeTokens({ accessToken: 'mock-token', refreshToken: 'mock-refresh', expiresIn: 3600 });
      setUser({
        id: 'mock-user-1',
        name,
        email,
        role: 'VENDEDOR',
        organizationId: 'org-1',
      });
      return;
    }

    const auth = await authService.register({ name, email, password, phone });
    storeTokens(auth);
    const profile = await authService.getProfile();
    setUser(profile);
  }, [useMocks]);

  const logout = useCallback(() => {
    if (!useMocks) {
      authService.logout().catch(() => {});
    }
    clearTokens();
    setUser(null);
  }, [useMocks]);

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
