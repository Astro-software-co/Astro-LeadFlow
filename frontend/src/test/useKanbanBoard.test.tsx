import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useKanbanBoard } from '@/hooks/api/useKanbanBoard';
import { kanbanService } from '@/services/kanban.service';

vi.mock('@/services/kanban.service', () => ({
  kanbanService: {
    getBoard: vi.fn(),
  },
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { organizationId: 'org-1' },
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

describe('useKanbanBoard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unstubAllEnvs();
  });

  it('loads board cards from kanban service in api mode', async () => {
    vi.mocked(kanbanService.getBoard).mockResolvedValueOnce([
      {
        id: 'lead-1',
        leadId: 'lead-1',
        coluna: 'aguardando',
        lead: {
          id: 'lead-1',
          nome: 'Ana Cliente',
          telefone: '11999999999',
          origem: 'Direto',
          dataCriacao: new Date('2026-01-10T12:00:00Z'),
          ultimaInteracao: new Date('2026-01-11T12:00:00Z'),
        },
        tags: [],
      },
    ]);

    const { result } = renderHook(() => useKanbanBoard(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(kanbanService.getBoard).toHaveBeenCalledTimes(1);
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].lead.nome).toBe('Ana Cliente');
  });

  it('uses local mock cards when VITE_USE_MOCKS=true', async () => {
    vi.resetModules();
    vi.stubEnv('VITE_USE_MOCKS', 'true');

    const { useKanbanBoard: useKanbanBoardMock } = await import('@/hooks/api/useKanbanBoard');
    const { result } = renderHook(() => useKanbanBoardMock(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(kanbanService.getBoard).not.toHaveBeenCalled();
    expect((result.current.data?.length ?? 0) > 0).toBe(true);
  });
});
