import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { authService } from '@/services/auth.service';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('login posts credentials and returns mapped auth response', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      data: {
        accessToken: 'token-1',
        pendingTwoFactor: false,
      },
    });

    const result = await authService.login({ email: 'john@astro.com', password: 'secret123' });

    expect(api.post).toHaveBeenCalledWith('/auth/login', {
      email: 'john@astro.com',
      password: 'secret123',
    });
    expect(result).toEqual({
      accessToken: 'token-1',
      pendingTwoFactor: false,
    });
  });

  it('register posts organization payload and returns mapped auth response', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      data: {
        accessToken: 'token-2',
        pendingTwoFactor: true,
      },
    });

    const payload = {
      organizationName: 'Astro Motors',
      slug: 'astro-motors',
      adminName: 'John Admin',
      adminEmail: 'admin@astro.com',
      password: 'secret123',
    };

    const result = await authService.register(payload);

    expect(api.post).toHaveBeenCalledWith('/auth/register', payload);
    expect(result).toEqual({
      accessToken: 'token-2',
      pendingTwoFactor: true,
    });
  });

  it('restoreSession posts empty payload and returns mapped auth response', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      data: {
        accessToken: 'token-3',
        pendingTwoFactor: false,
      },
    });

    const result = await authService.restoreSession();

    expect(api.post).toHaveBeenCalledWith('/auth/refresh');
    expect(result.accessToken).toBe('token-3');
  });

  it('throws when auth response is missing access token', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({
      data: {
        accessToken: null,
        pendingTwoFactor: false,
      },
    });

    await expect(authService.login({ email: 'john@astro.com', password: 'secret123' })).rejects.toThrow(
      'Resposta de autenticacao incompleta: tokens ausentes',
    );
  });

  it('getProfile maps organizationId to unknown when backend returns null', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        id: 'u-1',
        name: 'Jane',
        email: 'jane@astro.com',
        role: 'MANAGER',
        status: 'ACTIVE',
        organizationId: null,
        twoFactorEmailEnabled: false,
        twoFactorSmsEnabled: false,
        notifyNewLead: true,
        notifyLeadWaiting: true,
        notifySaleCompleted: true,
        notifyCardMoved: false,
        notifyViaEmail: false,
        notifyViaPush: true,
        createdAt: '2026-04-13T00:00:00.000Z',
      },
    });

    const result = await authService.getProfile();

    expect(api.get).toHaveBeenCalledWith('/users/me');
    expect(result).toEqual({
      id: 'u-1',
      name: 'Jane',
      email: 'jane@astro.com',
      role: 'MANAGER',
      organizationId: 'unknown',
      avatarUrl: null,
    });
  });

  it('logout posts refresh token payload to backend endpoint', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: null });

    await expect(authService.logout()).resolves.toBeUndefined();

    expect(api.post).toHaveBeenCalledWith('/auth/logout');
  });
});
