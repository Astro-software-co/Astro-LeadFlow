import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vehiclesService, VehicleFilters } from '@/services/vehicles.service';
import { Vehicle } from '@/types';
import { mockVehicles } from '@/data/mockData';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

export function useVehicles(filters?: VehicleFilters) {
  return useQuery({
    queryKey: ['vehicles', filters],
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
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['vehicles'] }),
  });
}

export function useUpdateVehicle() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Vehicle> }) => vehiclesService.update(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['vehicles'] }),
  });
}
