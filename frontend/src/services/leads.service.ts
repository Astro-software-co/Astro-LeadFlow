import api from '@/lib/api';
import { Lead } from '@/types';

export interface LeadFilters {
  page?: number;
  size?: number;
  search?: string;
  origem?: string;
  urgencia?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export const leadsService = {
  async list(filters?: LeadFilters): Promise<PageResponse<Lead>> {
    const response = await api.get<PageResponse<Lead>>('/leads', { params: filters });
    return response.data;
  },

  async getById(id: string): Promise<Lead> {
    const response = await api.get<Lead>(`/leads/${id}`);
    return response.data;
  },

  async create(data: Partial<Lead>): Promise<Lead> {
    const response = await api.post<Lead>('/leads', data);
    return response.data;
  },

  async update(id: string, data: Partial<Lead>): Promise<Lead> {
    const response = await api.put<Lead>(`/leads/${id}`, data);
    return response.data;
  },

  async updateStage(id: string, stage: string): Promise<Lead> {
    const response = await api.patch<Lead>(`/leads/${id}/stage`, { stage });
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/leads/${id}`);
  },
};
