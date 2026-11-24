import type { ReactNode } from 'react';
import { act, renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAnonymizeLead, useDeleteLead, useExecutePrivacyRequest, usePrivacyRequests } from '@/hooks/api/usePrivacy';
import { privacyService } from '@/services/privacy.service';
import { leadsService } from '@/services/leads.service';

vi.mock('@/services/privacy.service', () => ({
  privacyService: { list: vi.fn(), create: vi.fn(), execute: vi.fn(), reject: vi.fn() },
}));
vi.mock('@/services/leads.service', () => ({ leadsService: { delete: vi.fn() } }));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'u1', role: 'MANAGER', organizationId: 'org-1' } }),
}));

function setup() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  return { queryClient, wrapper };
}

describe('usePrivacy', () => {
  beforeEach(() => vi.clearAllMocks());

  it('usa chave iniciada pelo orgId', async () => {
    vi.mocked(privacyService.list).mockResolvedValue([]);
    const { queryClient, wrapper } = setup();
    const { result } = renderHook(() => usePrivacyRequests('RECEIVED'), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryCache().find({ queryKey: ['org-1', 'privacy', 'requests', 'RECEIVED'] })).toBeDefined();
  });

  it('anonimizar a partir do lead cria e executa a solicitacao com o telefone', async () => {
    vi.mocked(privacyService.create).mockResolvedValue({ id: 'r1' } as never);
    vi.mocked(privacyService.execute).mockResolvedValue({ id: 'r1', status: 'COMPLETED' } as never);
    const { wrapper } = setup();
    const { result } = renderHook(() => useAnonymizeLead(), { wrapper });
    await act(() => result.current.mutateAsync('+5511987654321'));
    expect(privacyService.create).toHaveBeenCalledWith({ type: 'ANONYMIZATION', identifier: '+5511987654321', channel: 'MANUAL' });
    expect(privacyService.execute).toHaveBeenCalledWith('r1', { identifier: '+5511987654321' });
  });

  it('executar e excluir invalidam privacidade, leads e kanban', async () => {
    vi.mocked(privacyService.execute).mockResolvedValue({ id: 'r1' } as never);
    vi.mocked(leadsService.delete).mockResolvedValue(undefined);
    const { queryClient, wrapper } = setup();
    const spy = vi.spyOn(queryClient, 'invalidateQueries');
    const exec = renderHook(() => useExecutePrivacyRequest(), { wrapper });
    await act(() => exec.result.current.mutateAsync({ id: 'r1', data: { identifier: 'x' } }));
    expect(spy).toHaveBeenCalledWith({ queryKey: ['org-1', 'privacy'] });

    const del = renderHook(() => useDeleteLead(), { wrapper });
    await act(() => del.result.current.mutateAsync({ id: 'l1', options: { includeSamePhone: true } }));
    expect(leadsService.delete).toHaveBeenCalledWith('l1', { includeSamePhone: true });
    expect(spy.mock.calls.length).toBeGreaterThanOrEqual(5);
  });
});
