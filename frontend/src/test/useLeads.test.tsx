import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  useArchiveLead,
  useCreateLead,
  useLeadActivities,
  useLeads,
  useUpdateLead,
  useUpdateLeadStage,
} from "@/hooks/api/useLeads";
import { leadsService } from "@/services/leads.service";

vi.mock("@/services/leads.service", () => ({
  leadsService: {
    list: vi.fn(),
    activities: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    updateStage: vi.fn(),
    archive: vi.fn(),
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

const baseLeadResponse = {
  id: "lead-1",
  name: "Joao Silva",
  phoneNumber: "11999999999",
  email: "joao@lead.com",
  origin: "WEBMOTORS",
  vehicleInterest: "Corolla",
  budget: 120000,
  urgency: "alta",
  purchaseForecast: "30 dias",
  notes: "cliente quente",
  pipelineStage: "WAITING_ATTENDANCE" as const,
  assignedTo: { id: "seller-1", name: "Ana Vendedora" },
  vehicle: null,
  isDuplicate: false,
  organizationId: "org-1",
  createdAt: "2026-04-10T00:00:00.000Z",
  updatedAt: "2026-04-11T00:00:00.000Z",
};

describe("useLeads hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useLeads calls leadsService.list and maps dto list into lead view model", async () => {
    vi.mocked(leadsService.list).mockResolvedValueOnce({
      content: [baseLeadResponse],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    });

    const { result } = renderHook(() => useLeads({ search: "joao", page: 0, size: 10 }), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(leadsService.list).toHaveBeenCalledWith({ search: "joao", page: 0, size: 10 });
    expect(result.current.data?.content[0]?.nome).toBe("Joao Silva");
    expect(result.current.data?.content[0]?.origem).toBe("WebMotors");
    expect(result.current.data?.content[0]?.estagio).toBe("aguardando");
  });

  it("useCreateLead maps input payload and calls leadsService.create", async () => {
    vi.mocked(leadsService.create).mockResolvedValueOnce(baseLeadResponse);

    const { result } = renderHook(() => useCreateLead(), { wrapper: createWrapper() });

    await act(async () => {
      const created = await result.current.mutateAsync({
        nome: "Joao Silva",
        telefone: "11999999999",
        origem: "WebMotors",
        interesse: "Corolla",
      });

      expect(created.nome).toBe("Joao Silva");
      expect(created.interesse).toBe("Corolla");
    });

    expect(leadsService.create).toHaveBeenCalledWith({
      name: "Joao Silva",
      phoneNumber: "11999999999",
      email: undefined,
      origin: "WebMotors",
      vehicleInterest: "Corolla",
      budget: undefined,
      urgency: undefined,
      purchaseForecast: undefined,
      notes: undefined,
    });
  });

  it("useUpdateLead maps update payload and calls leadsService.update", async () => {
    vi.mocked(leadsService.update).mockResolvedValueOnce({
      ...baseLeadResponse,
      name: "Joao Atualizado",
    });

    const { result } = renderHook(() => useUpdateLead(), { wrapper: createWrapper() });

    await act(async () => {
      const updated = await result.current.mutateAsync({
        id: "lead-1",
        data: { nome: "Joao Atualizado", interesse: "Civic" },
      });
      expect(updated.nome).toBe("Joao Atualizado");
    });

    expect(leadsService.update).toHaveBeenCalledWith("lead-1", {
      name: "Joao Atualizado",
      email: undefined,
      vehicleInterest: "Civic",
      budget: undefined,
      urgency: undefined,
      purchaseForecast: undefined,
      notes: undefined,
    });
  });

  it("useUpdateLeadStage maps kanban column to pipeline stage and calls leadsService.updateStage", async () => {
    vi.mocked(leadsService.updateStage).mockResolvedValueOnce({
      ...baseLeadResponse,
      pipelineStage: "IN_NEGOTIATION",
    });

    const { result } = renderHook(() => useUpdateLeadStage(), { wrapper: createWrapper() });

    await act(async () => {
      const updated = await result.current.mutateAsync({
        id: "lead-1",
        stage: "negociacao",
      });
      expect(updated.estagio).toBe("negociacao");
    });

    expect(leadsService.updateStage).toHaveBeenCalledWith("lead-1", "IN_NEGOTIATION");
  });

  it("useLeadActivities maps API timeline DTO into UI activity shape", async () => {
    vi.mocked(leadsService.activities).mockResolvedValueOnce([
      {
        fromStage: "WAITING_ATTENDANCE",
        toStage: "IN_NEGOTIATION",
        movedBy: { id: "seller-1", name: "Ana Vendedora", avatarUrl: null },
        movedAt: "2026-04-11T15:00:00.000Z",
      },
    ]);

    const { result } = renderHook(() => useLeadActivities("lead-1"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(leadsService.activities).toHaveBeenCalledWith("lead-1");
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0]).toMatchObject({
      fromStage: "aguardando",
      toStage: "negociacao",
      movedByName: "Ana Vendedora",
    });
    expect(result.current.data?.[0]?.movedAt).toBeInstanceOf(Date);
  });

  it("useArchiveLead calls archive service and resolves", async () => {
    vi.mocked(leadsService.archive).mockResolvedValueOnce(undefined);

    const { result } = renderHook(() => useArchiveLead(), { wrapper: createWrapper() });

    await act(async () => {
      await result.current.mutateAsync("lead-1");
    });

    expect(leadsService.archive).toHaveBeenCalledWith("lead-1");
  });
});

describe("useLeads hooks (mock-mode branches)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it("uses in-memory mock filtering and page normalization when VITE_USE_MOCKS=true", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const { useLeads: useLeadsMock } = await import("@/hooks/api/useLeads");

    const { result } = renderHook(() => useLeadsMock({ search: "jo", page: -3, size: 0 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(leadsService.list).not.toHaveBeenCalled();
    expect(result.current.data?.page).toBe(0);
    expect(result.current.data?.size).toBe(1);
    expect((result.current.data?.content.length ?? 0) > 0).toBe(true);
  });

  it("returns synthetic timeline for a known mock lead when VITE_USE_MOCKS=true", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const { useLeadActivities: useLeadActivitiesMock } = await import("@/hooks/api/useLeads");

    const { result } = renderHook(() => useLeadActivitiesMock("1"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(leadsService.activities).not.toHaveBeenCalled();
    expect(result.current.data?.[0]?.toStage).toBe("aguardando");
  });

  it("returns an empty timeline for unknown mock lead id", async () => {
    vi.resetModules();
    vi.stubEnv("VITE_USE_MOCKS", "true");
    const { useLeadActivities: useLeadActivitiesMock } = await import("@/hooks/api/useLeads");

    const { result } = renderHook(() => useLeadActivitiesMock("unknown-lead"), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });
});
