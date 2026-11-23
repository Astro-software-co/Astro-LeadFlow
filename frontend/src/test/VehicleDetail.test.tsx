import type { ReactNode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import VehicleDetail from '@/pages/VehicleDetail';
import type { Vehicle } from '@/types';

const useVehicleMock = vi.fn();
const updateVehicleMutation = {
  mutateAsync: vi.fn(),
  isPending: false,
};

vi.mock('@/hooks/api/useVehicles', () => ({
  useVehicle: (id?: string) => useVehicleMock(id),
  useVehiclePhotos: () => ({ data: [], refetch: vi.fn() }),
  useUpdateVehicle: () => updateVehicleMutation,
  useDeleteVehicle: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useReorderVehiclePhotos: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

vi.mock('@/components/modals/AddVehicleModal', () => ({
  AddVehicleModal: () => <div data-testid="add-vehicle-modal" />,
}));

vi.mock('@/components/layout/RoleGate', () => ({
  RoleGate: ({ children }: { children: ReactNode }) => <>{children}</>,
}));

describe('VehicleDetail page', () => {
  it('renders a string title for Helmet and shows the vehicle name', async () => {
    const vehicle: Vehicle = {
      id: 'vehicle-1',
      marca: 'Toyota',
      modelo: 'Corolla',
      ano: 2024,
      preco: 145000,
      precoCusto: 120000,
      status: 'disponivel',
      categoria: 'Sedan',
      cor: 'Preto',
      placa: 'ABC1D23',
      quilometragem: 15000,
      ipvaPago: true,
      fotos: ['https://example.com/corolla.jpg'],
    };

    useVehicleMock.mockReturnValue({
      data: vehicle,
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    });

    render(
      <HelmetProvider>
        <MemoryRouter initialEntries={['/app/estoque/vehicle-1']}>
          <Routes>
            <Route path="/app/estoque/:id" element={<VehicleDetail />} />
          </Routes>
        </MemoryRouter>
      </HelmetProvider>,
    );

    expect(screen.getByRole('heading', { name: 'Toyota Corolla 2024' })).toBeInTheDocument();

    await waitFor(() => {
      expect(document.title).toBe('Toyota Corolla 2024 — Estoque');
    });
  });
});
