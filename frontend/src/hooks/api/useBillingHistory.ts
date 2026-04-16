import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { billingHistoryService } from '@/services/billing-history.service';
import type {
  BillingHistoryEntry,
  BillingHistoryResponseDTO,
  BillingHistoryStatus,
  SettingsLoadState,
} from '@/types/settings';

const useMocks = import.meta.env.VITE_USE_MOCKS === 'true';

const mockBillingHistory: BillingHistoryEntry[] = [
  {
    id: 'bill-2026-02',
    billedAt: '2026-02-10T00:00:00.000Z',
    description: 'Plano Profissional - Fev/2026',
    amountBrl: 299,
    status: 'PAID',
  },
  {
    id: 'bill-2026-01',
    billedAt: '2026-01-10T00:00:00.000Z',
    description: 'Plano Profissional - Jan/2026',
    amountBrl: 299,
    status: 'PAID',
  },
  {
    id: 'bill-2025-12',
    billedAt: '2025-12-10T00:00:00.000Z',
    description: 'Plano Profissional - Dez/2025',
    amountBrl: 299,
    status: 'PAID',
  },
];

function normalizeStatus(status?: string): BillingHistoryStatus {
  const normalized = (status ?? '').toUpperCase();

  if (normalized === 'PAID' || normalized === 'PAGO') {
    return 'PAID';
  }

  if (normalized === 'FAILED' || normalized === 'FALHOU' || normalized === 'RECUSADO') {
    return 'FAILED';
  }

  return 'PENDING';
}

function mapBillingEntry(dto: BillingHistoryResponseDTO, index: number): BillingHistoryEntry {
  return {
    id: dto.id ?? `billing-${index}`,
    billedAt: dto.billedAt ?? dto.issuedAt ?? dto.createdAt ?? new Date().toISOString(),
    description: dto.description ?? `Cobranca #${index + 1}`,
    amountBrl: dto.amountBrl ?? dto.amount ?? 0,
    status: normalizeStatus(dto.status),
    invoiceUrl: dto.invoiceUrl ?? dto.invoicePdfUrl ?? null,
  };
}

function resolveState(isPending: boolean, isError: boolean): SettingsLoadState {
  if (isPending) {
    return 'loading';
  }

  if (isError) {
    return 'error';
  }

  return 'ready';
}

export interface UseBillingHistoryResult {
  history: BillingHistoryEntry[];
  state: SettingsLoadState;
  isLoading: boolean;
  isError: boolean;
  isReady: boolean;
  errorMessage: string | null;
  refetch: () => Promise<unknown>;
}

export function useBillingHistory(): UseBillingHistoryResult {
  const { user } = useAuth();
  const orgId = user?.organizationId ?? 'unknown';

  const query = useQuery({
    queryKey: [orgId, 'settings', 'billing-history'],
    staleTime: 30_000,
    queryFn: async (): Promise<BillingHistoryEntry[]> => {
      if (useMocks) {
        return mockBillingHistory;
      }

      const response = await billingHistoryService.listHistory();
      return response.map(mapBillingEntry);
    },
  });

  const state = resolveState(query.isPending, query.isError);

  return {
    history: query.data ?? [],
    state,
    isLoading: state === 'loading',
    isError: state === 'error',
    isReady: state === 'ready',
    errorMessage:
      query.error instanceof Error
        ? query.error.message
        : query.isError
          ? 'Nao foi possivel carregar o historico de cobrancas.'
          : null,
    refetch: query.refetch,
  };
}
