import api from '@/lib/api';
import {
  ChangePasswordRequestDTO,
  InviteUserRequestDTO,
  UpdateNotificationsRequestDTO,
  UpdateTwoFactorRequestDTO,
  AssignableUserResponseDTO,
  UserProfileResponseDTO,
  UserSummaryResponseDTO,
  UserTeamListDataDTO,
} from '@/types/api/users';

function normalizeTeamList(data: UserTeamListDataDTO): UserSummaryResponseDTO[] {
  if (Array.isArray(data)) {
    return data;
  }

  return data.content ?? data.data ?? data.items ?? [];
}

export const usersService = {
  async getProfile(): Promise<UserProfileResponseDTO> {
    const response = await api.get<UserProfileResponseDTO>('/users/me');
    return response.data;
  },

  async listTeam(): Promise<UserSummaryResponseDTO[]> {
    const response = await api.get<UserTeamListDataDTO>('/users');
    return normalizeTeamList(response.data);
  },

  async listAssignable(): Promise<AssignableUserResponseDTO[]> {
    const response = await api.get<AssignableUserResponseDTO[]>('/users/assignable');
    return response.data;
  },

  async changePassword(payload: ChangePasswordRequestDTO): Promise<UserProfileResponseDTO> {
    const response = await api.patch<UserProfileResponseDTO>('/users/me/password', payload);
    return response.data;
  },

  async updateNotifications(payload: UpdateNotificationsRequestDTO): Promise<UserProfileResponseDTO> {
    const response = await api.patch<UserProfileResponseDTO>('/users/me/notifications', payload);
    return response.data;
  },

  async updateTwoFactor(payload: UpdateTwoFactorRequestDTO): Promise<UserProfileResponseDTO> {
    const response = await api.patch<UserProfileResponseDTO>('/users/me/two-factor', payload);
    return response.data;
  },

  async inviteUser(payload: InviteUserRequestDTO): Promise<UserProfileResponseDTO> {
    const normalizedRole = payload.role === 'VENDEDOR' ? 'SELLER' : payload.role;
    const response = await api.post<UserProfileResponseDTO>('/users/invite', {
      ...payload,
      role: normalizedRole,
    });
    return response.data;
  },

  async deleteUser(userId: string): Promise<void> {
    await api.delete(`/users/${userId}`);
  },
};
