import api from '@/lib/api';
import type {
  CreateDataSubjectRequestDTO,
  DataSubjectRequestDTO,
  DataSubjectRequestStatus,
  ExecuteDataSubjectRequestDTO,
} from '@/types/api/privacy';

export const privacyService = {
  async list(status?: DataSubjectRequestStatus): Promise<DataSubjectRequestDTO[]> {
    const response = await api.get<DataSubjectRequestDTO[]>('/privacy/requests', { params: status ? { status } : undefined });
    return response.data;
  },

  async create(data: CreateDataSubjectRequestDTO): Promise<DataSubjectRequestDTO> {
    const response = await api.post<DataSubjectRequestDTO>('/privacy/requests', data);
    return response.data;
  },

  async execute(id: string, data: ExecuteDataSubjectRequestDTO): Promise<DataSubjectRequestDTO> {
    const response = await api.post<DataSubjectRequestDTO>(`/privacy/requests/${id}/execute`, data);
    return response.data;
  },

  async reject(id: string, reason: string): Promise<DataSubjectRequestDTO> {
    const response = await api.post<DataSubjectRequestDTO>(`/privacy/requests/${id}/reject`, { reason });
    return response.data;
  },
};
