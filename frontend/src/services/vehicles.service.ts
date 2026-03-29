import api from '@/lib/api';
import { Vehicle } from '@/types';
import { PageResponse } from './leads.service';

export interface VehicleFilters {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  categoria?: string;
}

export const vehiclesService = {
  async list(filters?: VehicleFilters): Promise<PageResponse<Vehicle>> {
    const response = await api.get<PageResponse<Vehicle>>('/vehicles', { params: filters });
    return response.data;
  },

  async getById(id: string): Promise<Vehicle> {
    const response = await api.get<Vehicle>(`/vehicles/${id}`);
    return response.data;
  },

  async create(data: Partial<Vehicle>): Promise<Vehicle> {
    const response = await api.post<Vehicle>('/vehicles', data);
    return response.data;
  },

  async update(id: string, data: Partial<Vehicle>): Promise<Vehicle> {
    const response = await api.put<Vehicle>(`/vehicles/${id}`, data);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await api.delete(`/vehicles/${id}`);
  },
};
