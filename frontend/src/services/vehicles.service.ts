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
  page?: number;
  size?: number;
}

interface SpringPageDTO<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PaginatedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

function isSpringPageDTO<T>(value: unknown): value is SpringPageDTO<T> {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const page = value as Partial<SpringPageDTO<T>>;
  return (
    Array.isArray(page.content) &&
    typeof page.totalElements === 'number' &&
    typeof page.totalPages === 'number' &&
    typeof page.number === 'number'
  );
}

function normalizePaginatedResponse<T>(
  data: T[] | SpringPageDTO<T>,
  requestedPage = 0,
  requestedSize?: number,
): PaginatedResult<T> {
  if (isSpringPageDTO<T>(data)) {
    return {
      content: data.content,
      totalElements: data.totalElements,
      totalPages: Math.max(1, data.totalPages),
      page: data.number,
      size: data.size,
    };
  }

  const safeSize = Math.max(1, requestedSize ?? (data.length || 1));
  const safePage = Math.max(0, requestedPage);
  const start = safePage * safeSize;
  const content = data.slice(start, start + safeSize);

  return {
    content,
    totalElements: data.length,
    totalPages: Math.max(1, Math.ceil(data.length / safeSize)),
    page: safePage,
    size: safeSize,
  };
}

export const vehiclesService = {
  async list(filters?: VehicleFilters): Promise<PaginatedResult<VehicleResponseDTO>> {
    const response = await api.get<VehicleResponseDTO[] | SpringPageDTO<VehicleResponseDTO>>('/vehicles', {
      params: filters,
    });
    return normalizePaginatedResponse(response.data, filters?.page, filters?.size);
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
