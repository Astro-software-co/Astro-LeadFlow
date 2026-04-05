import api from '@/lib/api';
import {
  VehicleCreateRequestDTO,
  VehicleResponseDTO,
  VehicleStatusDTO,
  VehicleUpdateRequestDTO,
} from '@/types/api/vehicles';

export interface VehicleFilters {
  search?: string;
  status?: VehicleStatusDTO;
}

export const vehiclesService = {
  async list(filters?: VehicleFilters): Promise<VehicleResponseDTO[]> {
    const response = await api.get<VehicleResponseDTO[]>('/vehicles', { params: filters });
    return response.data;
  },

  async getById(id: string): Promise<VehicleResponseDTO> {
    const response = await api.get<VehicleResponseDTO>(`/vehicles/${id}`);
    return response.data;
  },

  async create(data: VehicleCreateRequestDTO): Promise<VehicleResponseDTO> {
    const response = await api.post<VehicleResponseDTO>('/vehicles', data);
    return response.data;
  },

  async update(id: string, data: VehicleUpdateRequestDTO): Promise<VehicleResponseDTO> {
    const response = await api.put<VehicleResponseDTO>(`/vehicles/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/vehicles/${id}`);
  },
};
