import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vehiclesService, VehicleFilters } from '@/services/vehicles.service';
import { Vehicle } from '@/types';
import { mockVehicles } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';
import {
  mapVehicleToCreateRequest,
  mapVehicleToUpdateRequest,
  mapVehicleResponseToVehicle,
} from '@/mappers/vehicles.mapper';
import { VehicleCategoryDTO, VehicleStatusDTO } from '@/types/api/vehicles';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

interface VehiclesQueryResult {
  content: Vehicle[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export function useVehicles(filters?: VehicleFilters) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  const page = Math.max(0, filters?.page ?? 0);
  const size = Math.max(1, filters?.size ?? 10);

  return useQuery({
    queryKey: [orgId, 'vehicles', filters],
    queryFn: async (): Promise<VehiclesQueryResult> => {
      if (useMocks) {
        const normalizedSearch = filters?.search?.trim().toLowerCase();
        const statusMap: Record<VehicleStatusDTO, Vehicle['status']> = {
          AVAILABLE: 'disponivel',
          RESERVED: 'reservado',
          MAINTENANCE: 'manutencao',
          SOLD: 'vendido',
        };
        const categoryMap: Record<VehicleCategoryDTO, Vehicle['categoria']> = {
          HATCH: 'Hatch',
          SEDAN: 'Sedan',
          SUV: 'SUV',
          PICKUP: 'Pickup',
          MINIVAN: 'Outros',
          CONVERTIBLE: 'Outros',
          COUPE: 'Outros',
        };

        const filtered = mockVehicles.filter((vehicle) => {
          const matchesSearch =
            !normalizedSearch ||
            vehicle.marca.toLowerCase().includes(normalizedSearch) ||
            vehicle.modelo.toLowerCase().includes(normalizedSearch);
          const matchesStatus = !filters?.status || vehicle.status === statusMap[filters.status];
          const matchesCategory = !filters?.category || vehicle.categoria === categoryMap[filters.category];
          const matchesYear = !filters?.year || vehicle.ano === filters.year;
          const matchesMinPrice = filters?.minPrice === undefined || vehicle.preco >= filters.minPrice;
          const matchesMaxPrice = filters?.maxPrice === undefined || vehicle.preco <= filters.maxPrice;

          return matchesSearch && matchesStatus && matchesCategory && matchesYear && matchesMinPrice && matchesMaxPrice;
        });

        const start = page * size;
        const content = filtered.slice(start, start + size);
        return {
          content,
          totalElements: filtered.length,
          totalPages: Math.max(1, Math.ceil(filtered.length / size)),
          page,
          size,
        };
      }

      const response = await vehiclesService.list(filters);
      const mapped = response.content.map(mapVehicleResponseToVehicle);

      return {
        content: mapped,
        totalElements: response.totalElements,
        totalPages: response.totalPages,
        page: response.page,
        size: response.size,
      };
    },
    staleTime: 30_000,
    placeholderData: (previous) => previous,
  });
}

export function useCreateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: Partial<Vehicle>) => {
      const created = await vehiclesService.create(mapVehicleToCreateRequest(data));
      return mapVehicleResponseToVehicle(created);
    },
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('vehicles') }),
  });
}

export function useUpdateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, data }: { id: string; data: Partial<Vehicle> }) => {
      const updated = await vehiclesService.update(id, mapVehicleToUpdateRequest(data));
      return mapVehicleResponseToVehicle(updated);
    },
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('vehicles') }),
  });
}
