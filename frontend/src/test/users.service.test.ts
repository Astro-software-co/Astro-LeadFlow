import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { usersService } from '@/services/users.service';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('usersService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getProfile calls /users/me and returns payload', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        id: 'u-1',
        name: 'Ana',
        email: 'ana@astro.com',
        role: 'ADMIN',
        status: 'ACTIVE',
        organizationId: 'org-1',
        organizationName: 'Astro',
        twoFactorEmailEnabled: false,
        twoFactorSmsEnabled: false,
        notifyNewLead: true,
        notifyLeadWaiting: true,
        notifySaleCompleted: true,
        notifyCardMoved: false,
        notifyViaEmail: false,
        notifyViaPush: true,
        createdAt: '2026-04-14T00:00:00.000Z',
      },
    });

    const profile = await usersService.getProfile();

    expect(api.get).toHaveBeenCalledWith('/users/me');
    expect(profile.id).toBe('u-1');
  });

  it('listTeam normalizes array response', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: [{ id: 'u-1', name: 'Ana', email: 'ana@astro.com', role: 'ADMIN' }],
    });

    const users = await usersService.listTeam();

    expect(api.get).toHaveBeenCalledWith('/users');
    expect(users).toHaveLength(1);
    expect(users[0].id).toBe('u-1');
  });

  it('listTeam normalizes content/data/items response variants', async () => {
    vi.mocked(api.get)
      .mockResolvedValueOnce({ data: { content: [{ id: 'u-1', name: 'Ana', email: 'ana@astro.com', role: 'ADMIN' }] } })
      .mockResolvedValueOnce({ data: { data: [{ id: 'u-2', name: 'Bia', email: 'bia@astro.com', role: 'MANAGER' }] } })
      .mockResolvedValueOnce({ data: { items: [{ id: 'u-3', name: 'Cris', email: 'cris@astro.com', role: 'SELLER' }] } });

    const fromContent = await usersService.listTeam();
    const fromData = await usersService.listTeam();
    const fromItems = await usersService.listTeam();

    expect(fromContent[0].id).toBe('u-1');
    expect(fromData[0].id).toBe('u-2');
    expect(fromItems[0].id).toBe('u-3');
  });

  it('changePassword maps payload to /users/me/password', async () => {
    vi.mocked(api.patch).mockResolvedValueOnce({ data: { id: 'u-1' } });

    await usersService.changePassword({
      currentPassword: 'SenhaAntiga123!',
      newPassword: 'SenhaNova123!',
      confirmNewPassword: 'SenhaNova123!',
    });

    expect(api.patch).toHaveBeenCalledWith('/users/me/password', {
      currentPassword: 'SenhaAntiga123!',
      newPassword: 'SenhaNova123!',
      confirmNewPassword: 'SenhaNova123!',
    });
  });

  it('updateNotifications maps payload to /users/me/notifications', async () => {
    vi.mocked(api.patch).mockResolvedValueOnce({ data: { id: 'u-1' } });

    await usersService.updateNotifications({
      notifyNewLead: true,
      notifyLeadWaiting: false,
      notifySaleCompleted: true,
      notifyCardMoved: false,
      notifyViaEmail: true,
      notifyViaPush: true,
    });

    expect(api.patch).toHaveBeenCalledWith('/users/me/notifications', {
      notifyNewLead: true,
      notifyLeadWaiting: false,
      notifySaleCompleted: true,
      notifyCardMoved: false,
      notifyViaEmail: true,
      notifyViaPush: true,
    });
  });

  it('updateTwoFactor maps payload to /users/me/two-factor', async () => {
    vi.mocked(api.patch).mockResolvedValueOnce({ data: { id: 'u-1' } });

    await usersService.updateTwoFactor({ emailEnabled: true, smsEnabled: false });

    expect(api.patch).toHaveBeenCalledWith('/users/me/two-factor', {
      emailEnabled: true,
      smsEnabled: false,
    });
  });

  it('inviteUser maps payload to /users/invite and normalizes role', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 'u-2' } });

    await usersService.inviteUser({
      name: 'Vendor Test',
      email: 'vendor@astro.test',
      role: 'VENDEDOR',
    });

    expect(api.post).toHaveBeenCalledWith('/users/invite', {
      name: 'Vendor Test',
      email: 'vendor@astro.test',
      role: 'SELLER',
    });
  });

  it('deleteUser calls /users/:id', async () => {
    vi.mocked(api.delete).mockResolvedValueOnce({ data: null });

    await usersService.deleteUser('u-2');

    expect(api.delete).toHaveBeenCalledWith('/users/u-2');
  });
});
