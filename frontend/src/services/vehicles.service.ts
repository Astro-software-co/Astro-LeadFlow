import api from '@/lib/api';
import {
  VehicleCategoryDTO,
  VehicleCreateRequestDTO,
  VehiclePhotoResponseDTO,
  VehicleResponseDTO,
  VehicleStatusDTO,
  VehicleUpdateRequestDTO,
} from '@/types/api/vehicles';

export interface VehicleFilters {
  search?: string;
  status?: VehicleStatusDTO;
  category?: VehicleCategoryDTO;
  year?: number;
  minPrice?: number;
  maxPrice?: number;
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

function isInlinePhoto(photo: string) {
  return photo.startsWith('data:');
}

function stablePhotoKey(photo: string) {
  const normalizedPhoto = photo.trim();

  if (normalizedPhoto.length === 0 || isInlinePhoto(normalizedPhoto)) {
    return normalizedPhoto;
  }

  return normalizedPhoto.split('#', 1)[0]?.split('?', 1)[0] ?? normalizedPhoto;
}

function normalizePhotos(photos?: string[]) {
  return (photos ?? [])
    .map((photo) => photo.trim())
    .filter((photo) => photo.length > 0);
}

function buildPhotoQueues(photos: VehiclePhotoResponseDTO[]) {
  return photos.reduce<Map<string, VehiclePhotoResponseDTO[]>>((queues, photo) => {
    const queueKey = stablePhotoKey(photo.s3Url);
    const queue = queues.get(queueKey) ?? [];
    queue.push(photo);
    queues.set(queueKey, queue);
    return queues;
  }, new Map());
}

function extensionForMimeType(mimeType: string) {
  switch (mimeType) {
    case 'image/jpeg':
      return 'jpg';
    case 'image/png':
      return 'png';
    case 'image/webp':
      return 'webp';
    default:
      return 'bin';
  }
}

function dataUrlToFile(dataUrl: string, fileNameBase: string) {
  const match = dataUrl.match(/^data:([^;,]+)?(;base64)?,(.*)$/);

  if (!match) {
    throw new Error('Formato de imagem invalido para upload.');
  }

  const mimeType = match[1] ?? 'application/octet-stream';
  const isBase64Payload = Boolean(match[2]);
  const payload = match[3] ?? '';
  const byteString = isBase64Payload ? atob(payload) : decodeURIComponent(payload);
  const bytes = Uint8Array.from(byteString, (char) => char.charCodeAt(0));

  return new File([bytes], `${fileNameBase}.${extensionForMimeType(mimeType)}`, {
    type: mimeType,
  });
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

  async listPhotos(vehicleId: string): Promise<VehiclePhotoResponseDTO[]> {
    const response = await api.get<VehiclePhotoResponseDTO[]>(`/vehicles/${vehicleId}/photos`);
    return response.data;
  },

  async uploadPhoto(vehicleId: string, photoDataUrl: string, position: number): Promise<VehiclePhotoResponseDTO> {
    const formData = new FormData();
    formData.append('file', dataUrlToFile(photoDataUrl, `vehicle-photo-${position + 1}`));
    formData.append('position', String(position));

    const response = await api.post<VehiclePhotoResponseDTO>(`/vehicles/${vehicleId}/photos`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  },

  async deletePhoto(vehicleId: string, photoId: string): Promise<void> {
    await api.delete(`/vehicles/${vehicleId}/photos/${photoId}`);
  },

  async reorderPhotos(vehicleId: string, photoIds: string[]): Promise<VehiclePhotoResponseDTO[]> {
    const response = await api.patch<VehiclePhotoResponseDTO[]>(`/vehicles/${vehicleId}/photos/reorder`, {
      photoIds,
    });
    return response.data;
  },

  async syncPhotos(vehicleId: string, desiredPhotos?: string[]): Promise<void> {
    if (desiredPhotos === undefined) {
      return;
    }

    const normalizedDesiredPhotos = normalizePhotos(desiredPhotos);
    const existingPhotos = await this.listPhotos(vehicleId);
    const existingPhotoQueues = buildPhotoQueues(existingPhotos);
    const retainedExistingPhotos = new Set<string>();

    normalizedDesiredPhotos.forEach((photo) => {
      if (isInlinePhoto(photo)) {
        return;
      }

      const matchedPhoto = existingPhotoQueues.get(stablePhotoKey(photo))?.shift();
      if (matchedPhoto) {
        retainedExistingPhotos.add(matchedPhoto.id);
      }
    });

    const photosToDelete = existingPhotos.filter((photo) => !retainedExistingPhotos.has(photo.id));
    await Promise.all(photosToDelete.map((photo) => this.deletePhoto(vehicleId, photo.id)));

    const uploadedPhotos: VehiclePhotoResponseDTO[] = [];
    for (const [index, photo] of normalizedDesiredPhotos.entries()) {
      if (!isInlinePhoto(photo)) {
        continue;
      }

      uploadedPhotos.push(await this.uploadPhoto(vehicleId, photo, index));
    }

    if (normalizedDesiredPhotos.length === 0) {
      return;
    }

    const retainedPhotos = existingPhotos.filter((photo) => retainedExistingPhotos.has(photo.id));
    const retainedPhotoQueues = buildPhotoQueues(retainedPhotos);
    const uploadedPhotoQueue = [...uploadedPhotos];

    const finalPhotoIds = normalizedDesiredPhotos.flatMap((photo) => {
      if (isInlinePhoto(photo)) {
        const uploadedPhoto = uploadedPhotoQueue.shift();
        return uploadedPhoto ? [uploadedPhoto.id] : [];
      }

      const retainedPhoto = retainedPhotoQueues.get(stablePhotoKey(photo))?.shift();
      return retainedPhoto ? [retainedPhoto.id] : [];
    });

    if (finalPhotoIds.length > 0) {
      await this.reorderPhotos(vehicleId, finalPhotoIds);
    }
  },
};
