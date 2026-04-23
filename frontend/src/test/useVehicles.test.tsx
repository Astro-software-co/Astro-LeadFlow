import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useCreateVehicle, useUpdateVehicle, useVehicles } from "@/hooks/api/useVehicles";
import { vehiclesService } from "@/services/vehicles.service";

vi.mock("@/services/vehicles.service", () => ({
  vehiclesService: {
    list: vi.fn(),
    create: vi.fn(),
    getById: vi.fn(),
    syncPhotos: vi.fn(),
    update: vi.fn(),
  },
}));

vi.mock("@/contexts/AuthContext", () => ({
  useAuth: () => ({
    user: { organizationId: "org-1" },
  }),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

const baseVehicleResponse = {
  id: "veh-1",
  brand: "Toyota",
  model: "Corolla",
  year: 2023,
  color: "Preto",
  licensePlate: "ABC1D23",
  salePrice: 145000,
  costPrice: 120000,
  mileageKm: 42000,
  ipvaPaid: true,
  category: "SEDAN" as const,
  status: "AVAILABLE" as const,
  photos: [],
  organizationId: "org-1",
  createdAt: "2026-04-10T00:00:00.000Z",
  updatedAt: "2026-04-11T00:00:00.000Z",
};

describe("useVehicles hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useVehicles calls vehiclesService.list and maps response content", async () => {
    vi.mocked(vehiclesService.list).mockResolvedValueOnce({
      content: [baseVehicleResponse],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    const { result } = renderHook(() => useVehicles({ page: 0, size: 10, search: "corolla" }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(vehiclesService.list).toHaveBeenCalledWith({ page: 0, size: 10, search: "corolla" });
    expect(result.current.data?.content[0]?.marca).toBe("Toyota");
    expect(result.current.data?.content[0]?.categoria).toBe("Sedan");
  });

  it("useCreateVehicle maps vehicle shape into dto and calls vehiclesService.create", async () => {
    vi.mocked(vehiclesService.create).mockResolvedValueOnce(baseVehicleResponse);

    const { result } = renderHook(() => useCreateVehicle(), { wrapper: createWrapper() });

    await act(async () => {
      const created = await result.current.mutateAsync({
        marca: "Toyota",
        modelo: "Corolla",
        ano: 2023,
        preco: 145000,
        categoria: "Sedan",
        status: "disponivel",
      });
      expect(created.marca).toBe("Toyota");
      expect(created.status).toBe("disponivel");
    });

    expect(vehiclesService.create).toHaveBeenCalledWith({
      brand: "Toyota",
      model: "Corolla",
      year: 2023,
      color: undefined,
      licensePlate: undefined,
      salePrice: 145000,
      costPrice: undefined,
      mileageKm: undefined,
      ipvaPaid: undefined,
      category: "SEDAN",
      status: "AVAILABLE",
    });
  });

  it("useUpdateVehicle maps partial payload and calls vehiclesService.update", async () => {
    vi.mocked(vehiclesService.update).mockResolvedValueOnce({
      ...baseVehicleResponse,
      color: "Prata",
      status: "RESERVED",
    });

    const { result } = renderHook(() => useUpdateVehicle(), { wrapper: createWrapper() });

    await act(async () => {
      const updated = await result.current.mutateAsync({
        id: "veh-1",
        data: { cor: "Prata", status: "reservado" },
      });
      expect(updated.cor).toBe("Prata");
      expect(updated.status).toBe("reservado");
    });

    expect(vehiclesService.update).toHaveBeenCalledWith("veh-1", {
      brand: undefined,
      model: undefined,
      year: undefined,
      color: "Prata",
      licensePlate: undefined,
      salePrice: undefined,
      costPrice: undefined,
      mileageKm: undefined,
      ipvaPaid: undefined,
      category: undefined,
      status: "RESERVED",
    });
  });

  it("useCreateVehicle syncs photos and reloads the vehicle when gallery data is provided", async () => {
    vi.mocked(vehiclesService.create).mockResolvedValueOnce(baseVehicleResponse);
    vi.mocked(vehiclesService.syncPhotos).mockResolvedValueOnce(undefined);
    vi.mocked(vehiclesService.getById).mockResolvedValueOnce({
      ...baseVehicleResponse,
      photos: ["https://cdn.test/vehicle.png"],
    });

    const { result } = renderHook(() => useCreateVehicle(), { wrapper: createWrapper() });

    await act(async () => {
      const created = await result.current.mutateAsync({
        marca: "Toyota",
        modelo: "Corolla",
        ano: 2023,
        preco: 145000,
        categoria: "Sedan",
        fotos: ["data:image/png;base64,AAA"],
      });

      expect(created.fotos).toEqual(["https://cdn.test/vehicle.png"]);
    });

    expect(vehiclesService.syncPhotos).toHaveBeenCalledWith("veh-1", ["data:image/png;base64,AAA"]);
    expect(vehiclesService.getById).toHaveBeenCalledWith("veh-1");
  });

  it("useUpdateVehicle syncs photos and reloads the vehicle when gallery data changes", async () => {
    vi.mocked(vehiclesService.update).mockResolvedValueOnce(baseVehicleResponse);
    vi.mocked(vehiclesService.syncPhotos).mockResolvedValueOnce(undefined);
    vi.mocked(vehiclesService.getById).mockResolvedValueOnce({
      ...baseVehicleResponse,
      photos: ["https://cdn.test/updated.png"],
    });

    const { result } = renderHook(() => useUpdateVehicle(), { wrapper: createWrapper() });

    await act(async () => {
      const updated = await result.current.mutateAsync({
        id: "veh-1",
        data: {
          fotos: ["https://cdn.test/existing.png", "data:image/png;base64,BBB"],
        },
      });

      expect(updated.fotos).toEqual(["https://cdn.test/updated.png"]);
    });

    expect(vehiclesService.syncPhotos).toHaveBeenCalledWith("veh-1", [
      "https://cdn.test/existing.png",
      "data:image/png;base64,BBB",
    ]);
    expect(vehiclesService.getById).toHaveBeenCalledWith("veh-1");
  });
});

describe("useVehicles hooks (mock-mode branches)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it("filters mock vehicles and normalizes page/size when VITE_USE_MOCKS=true", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const { useVehicles: useVehiclesMock } = await import("@/hooks/api/useVehicles");

    const { result } = renderHook(
      () =>
        useVehiclesMock({
          search: "toyota",
          status: "AVAILABLE",
          category: "SEDAN",
          minPrice: 100000,
          maxPrice: 200000,
          page: -5,
          size: 0,
        }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(vehiclesService.list).not.toHaveBeenCalled();
    expect(result.current.data?.page).toBe(0);
    expect(result.current.data?.size).toBe(1);
    expect(result.current.data?.content[0]?.marca).toBe("Toyota");
  });
});
