import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vehiclesService, VehicleFilters } from '@/services/vehicles.service';
import { Vehicle } from '@/types';
import { mockVehicles } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';
import {
  mapVehicleResponseToVehicle,
  mapVehicleToCreateRequest,
  mapVehicleToUpdateRequest,
} from '@/mappers/vehicles.mapper';

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

  return useQuery({
    queryKey: [orgId, 'vehicles', filters],
    queryFn: async (): Promise<VehiclesQueryResult> => {
      if (useMocks) {
        return {
          content: mockVehicles,
          totalElements: mockVehicles.length,
          totalPages: 1,
          page: 0,
          size: mockVehicles.length,
        };
      }

      const vehicles = await vehiclesService.list(filters);
      const mapped = vehicles.map(mapVehicleResponseToVehicle);

      return {
        content: mapped,
        totalElements: mapped.length,
        totalPages: 1,
        page: 0,
        size: mapped.length,
      };
    },
    staleTime: 30_000,
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
