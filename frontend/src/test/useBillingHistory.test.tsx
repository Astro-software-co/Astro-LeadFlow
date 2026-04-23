import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useBillingHistory } from '@/hooks/api/useBillingHistory';
import { billingHistoryService } from '@/services/billing-history.service';

vi.mock('@/services/billing-history.service', () => ({
  billingHistoryService: {
    listHistory: vi.fn(),
  },
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'user-1',
      name: 'Ana Admin',
      email: 'ana@astro.com',
      role: 'ADMIN',
      organizationId: 'org-1',
    },
  }),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return { wrapper, queryClient };
}

describe('useBillingHistory', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps billing entries and stores them under org-scoped key', async () => {
    vi.mocked(billingHistoryService.listHistory).mockResolvedValueOnce([
      {
        id: 'bill-1',
        issuedAt: '2026-02-10T00:00:00.000Z',
        description: 'Plano Pro Fev/2026',
        amount: 299,
        status: 'PAGO',
        invoicePdfUrl: 'https://example.com/nf/bill-1.pdf',
      },
    ]);

    const { wrapper, queryClient } = createWrapper();
    const { result } = renderHook(() => useBillingHistory(), { wrapper });

    await waitFor(() => expect(result.current.isReady).toBe(true));

    expect(billingHistoryService.listHistory).toHaveBeenCalledTimes(1);
    expect(result.current.history[0]).toMatchObject({
      id: 'bill-1',
      amountBrl: 299,
      status: 'PAID',
      invoiceUrl: 'https://example.com/nf/bill-1.pdf',
    });
    expect(queryClient.getQueryData(['org-1', 'settings', 'billing-history'])).toBeTruthy();
  });

  it('returns explicit error state when billing history fails', async () => {
    vi.mocked(billingHistoryService.listHistory).mockRejectedValueOnce(new Error('billing failed'));

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useBillingHistory(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.state).toBe('error');
    expect(result.current.errorMessage).toContain('billing failed');
  });

  it('normalizes failed and pending statuses with fallback fields', async () => {
    vi.mocked(billingHistoryService.listHistory).mockResolvedValueOnce([
      {
        status: 'RECUSADO',
      },
      {
        id: 'bill-2',
        status: 'desconhecido',
        amount: 0,
      },
    ]);

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useBillingHistory(), { wrapper });

    await waitFor(() => expect(result.current.isReady).toBe(true));

    expect(result.current.history[0]).toMatchObject({
      status: 'FAILED',
      id: 'billing-0',
      description: 'Cobranca #1',
      invoiceUrl: null,
    });
    expect(result.current.history[1]).toMatchObject({
      status: 'PENDING',
      id: 'bill-2',
      amountBrl: 0,
    });
  });

  it('returns generic message when rejection is not an Error instance', async () => {
    vi.mocked(billingHistoryService.listHistory).mockRejectedValueOnce('boom');

    const { wrapper } = createWrapper();
    const { result } = renderHook(() => useBillingHistory(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.errorMessage).toBe('Nao foi possivel carregar o historico de cobrancas.');
  });
});
