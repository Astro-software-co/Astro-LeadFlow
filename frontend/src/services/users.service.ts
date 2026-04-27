import api from '@/lib/api';
import {
  ChangePasswordRequestDTO,
  InviteUserRequestDTO,
  UpdateNotificationsRequestDTO,
  UpdateTwoFactorRequestDTO,
  UpdateUserProfileRequestDTO,
  AssignableUserResponseDTO,
  UpdateTeamUserRequestDTO,
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

  async updateProfile(payload: UpdateUserProfileRequestDTO): Promise<UserProfileResponseDTO> {
    const response = await api.patch<UserProfileResponseDTO>('/users/me', payload);
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
    const response = await api.post<UserProfileResponseDTO>('/users/invite', payload);
    return response.data;
  },

  async updateTeamUser(userId: string, payload: UpdateTeamUserRequestDTO): Promise<UserProfileResponseDTO> {
    const response = await api.patch<UserProfileResponseDTO>(`/users/${userId}`, payload);
    return response.data;
  },

  async suspendUser(userId: string): Promise<void> {
    await api.patch(`/users/${userId}/suspend`);
  },

  async reactivateUser(userId: string): Promise<void> {
    await api.patch(`/users/${userId}/reactivate`);
  },

  async deleteUser(userId: string): Promise<void> {
    await api.delete(`/users/${userId}`);
  },
};
