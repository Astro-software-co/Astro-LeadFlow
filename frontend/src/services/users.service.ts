import api from '@/lib/api';
import { UserProfileResponseDTO, UserSummaryResponseDTO, UserTeamListDataDTO } from '@/types/api/users';

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
};
