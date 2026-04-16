import api from '@/lib/api';
import { UserProfileResponseDTO, UserSummaryResponseDTO } from '@/types/api/users';

export const usersService = {
  async getProfile(): Promise<UserProfileResponseDTO> {
    const response = await api.get<UserProfileResponseDTO>('/users/me');
    return response.data;
  },

  async listTeam(): Promise<UserSummaryResponseDTO[]> {
    const response = await api.get<UserSummaryResponseDTO[]>('/users');
    return response.data;
  },
};
