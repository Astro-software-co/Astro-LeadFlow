import api from '@/lib/api';
import { LeadCreateRequestDTO, LeadResponseDTO, LeadUpdateRequestDTO, MoveLeadRequestDTO } from '@/types/api/leads';

export interface LeadFilters {
  search?: string;
  origin?: string;
  stage?: string;
}

export const leadsService = {
  async list(filters?: LeadFilters): Promise<LeadResponseDTO[]> {
    const response = await api.get<LeadResponseDTO[]>('/leads', { params: filters });
    return response.data;
  },

  async getById(id: string): Promise<LeadResponseDTO> {
    const response = await api.get<LeadResponseDTO>(`/leads/${id}`);
    return response.data;
  },

  async create(data: LeadCreateRequestDTO): Promise<LeadResponseDTO> {
    const response = await api.post<LeadResponseDTO>('/leads', data);
    return response.data;
  },

  async update(id: string, data: LeadUpdateRequestDTO): Promise<LeadResponseDTO> {
    const response = await api.put<LeadResponseDTO>(`/leads/${id}`, data);
    return response.data;
  },

  async updateStage(id: string, toStage: MoveLeadRequestDTO['toStage']): Promise<LeadResponseDTO> {
    const response = await api.patch<LeadResponseDTO>(`/leads/${id}/move`, { toStage });
    return response.data;
  },

  async archive(id: string): Promise<void> {
    await api.patch(`/leads/${id}/archive`);
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/leads/${id}`);
  },

  async assume(id: string): Promise<LeadResponseDTO> {
    const response = await api.patch<LeadResponseDTO>(`/leads/${id}/assume`);
    return response.data;
  },
};
