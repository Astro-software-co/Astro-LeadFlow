import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vehiclesService, VehicleFilters } from '@/services/vehicles.service';
import { Vehicle } from '@/types';
import { mockVehicles } from '@/data/mockData';
import { useAuth } from '@/contexts/AuthContext';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useVehicles(filters?: VehicleFilters) {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';
  return useQuery({
    queryKey: [orgId, 'vehicles', filters],
    queryFn: () => {
      if (useMocks) {
        return {
          content: mockVehicles,
          totalElements: mockVehicles.length,
          totalPages: 1,
          page: 0,
          size: mockVehicles.length,
        };
      }
      return vehiclesService.list(filters);
    },
    staleTime: 30_000,
  });
}

export function useCreateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Vehicle>) => vehiclesService.create(data),
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('vehicles') }),
  });
}

export function useUpdateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Vehicle> }) => vehiclesService.update(id, data),
    onSuccess: () => queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('vehicles') }),
  });
}
