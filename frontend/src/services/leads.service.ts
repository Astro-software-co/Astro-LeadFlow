import api from '@/lib/api';
import type { DeleteLeadOptions } from '@/types/api/privacy';
import {
  LeadActivityResponseDTO,
  LeadCreateRequestDTO,
  LeadResponseDTO,
  LeadUpdateRequestDTO,
  MoveLeadRequestDTO,
} from '@/types/api/leads';

export interface LeadFilters {
  search?: string;
  origin?: string;
  stage?: string;
  assignedTo?: string;
  dateFrom?: string;
  dateTo?: string;
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

export const leadsService = {
  async list(filters?: LeadFilters): Promise<PaginatedResult<LeadResponseDTO>> {
    const response = await api.get<LeadResponseDTO[] | SpringPageDTO<LeadResponseDTO>>('/leads', {
      params: filters,
    });
    return normalizePaginatedResponse(response.data, filters?.page, filters?.size);
  },

  async getById(id: string): Promise<LeadResponseDTO> {
    const response = await api.get<LeadResponseDTO>(`/leads/${id}`);
    return response.data;
  },

  async activities(id: string): Promise<LeadActivityResponseDTO[]> {
    const response = await api.get<LeadActivityResponseDTO[]>(`/leads/${id}/activities`);
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

  /** Exclusão irreversível (MANAGER). Ver DeleteLeadOptions. */
  async delete(id: string, options: DeleteLeadOptions = {}): Promise<void> {
    const params = {
      ...(options.includeSamePhone ? { includeSamePhone: true } : {}),
      ...(options.reason ? { reason: options.reason } : {}),
    };
    await api.delete(`/leads/${id}`, Object.keys(params).length ? { params } : undefined);
  },

  async assume(id: string): Promise<LeadResponseDTO> {
    const response = await api.patch<LeadResponseDTO>(`/leads/${id}/assume`);
    return response.data;
  },
};
