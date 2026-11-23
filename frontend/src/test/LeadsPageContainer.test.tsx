import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { HelmetProvider } from 'react-helmet-async';
import { describe, expect, it, vi } from 'vitest';
import Leads from '@/pages/Leads';
import type { Lead } from '@/types';

const useLeadsMock = vi.fn();
const archiveMutateAsync = vi.fn().mockResolvedValue(undefined);

vi.mock('@/hooks/api/useLeads', () => ({
  useLeads: (filters: unknown) => useLeadsMock(filters),
  useCreateLead: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateLead: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useArchiveLead: () => ({ mutateAsync: archiveMutateAsync, isPending: false }),
  useLeadActivities: () => ({ data: [], isLoading: false }),
}));

vi.mock('@/hooks/api/useAssignableUsers', () => ({
  useAssignableUsers: () => ({ data: [], isLoading: false }),
}));

vi.mock('@/features/leads/components', () => ({
  LeadsLoadingState: () => <div data-testid="leads-loading">Loading</div>,
  LeadsErrorState: ({ onRetry }: { onRetry: () => void }) => (
    <button data-testid="leads-error-retry" onClick={onRetry}>
      Retry
    </button>
  ),
  LeadsFiltersBar: ({ onSearchChange }: { onSearchChange: (value: string) => void }) => (
    <button data-testid="filters-search" onClick={() => onSearchChange('Ana')}>
      Set search
    </button>
  ),
  LeadsTable: ({ leads, onArchiveLead }: { leads: Lead[]; onArchiveLead: (leadId: string) => void }) => (
    <div>
      <div data-testid="leads-table">Rows:{leads.length}</div>
      <button data-testid="archive-trigger" onClick={() => onArchiveLead(leads[0].id)}>
        Open archive dialog
      </button>
    </div>
  ),
  LeadsMobileList: ({ leads }: { leads: Lead[] }) => <div data-testid="leads-mobile">Mobile:{leads.length}</div>,
  LeadsPagination: ({ safePage }: { safePage: number }) => <div data-testid="leads-pagination">Page:{safePage}</div>,
  LeadFormDialog: ({ open }: { open: boolean }) => <div data-testid="lead-form-open">{String(open)}</div>,
  LeadHistoryDialog: () => <div data-testid="lead-history" />,
  LeadArchiveConfirmDialog: ({ open, onConfirm }: { open: boolean; onConfirm: () => void }) => (
    <div data-testid="archive-dialog">
      <span>{String(open)}</span>
      {open && (
        <button data-testid="confirm-archive" onClick={onConfirm}>
          Confirm archive
        </button>
      )}
    </div>
  ),
}));

describe('Leads page container orchestration', () => {
  it('wires slices, updates filters and confirms archive through app dialog', async () => {
    useLeadsMock.mockReturnValue({
      data: {
        content: [
          {
            id: 'lead-1',
            nome: 'Ana Cliente',
            telefone: '11999999999',
            origem: 'WebMotors',
            dataCriacao: new Date('2026-01-10T12:00:00Z'),
            ultimaInteracao: new Date('2026-01-11T12:00:00Z'),
            estagio: 'negociacao',
          },
        ],
        totalElements: 1,
        totalPages: 1,
      },
      isLoading: false,
      isFetching: false,
      error: null,
      refetch: vi.fn(),
    });

    render(
      <HelmetProvider>
        <Leads />
      </HelmetProvider>,
    );

    expect(screen.getByText('Leads')).toBeInTheDocument();
    expect(screen.getByTestId('leads-table')).toHaveTextContent('Rows:1');
    expect(screen.getByTestId('leads-mobile')).toHaveTextContent('Mobile:1');
    expect(screen.getByTestId('lead-form-open')).toHaveTextContent('false');

    fireEvent.click(screen.getByRole('button', { name: /novo lead/i }));
    expect(screen.getByTestId('lead-form-open')).toHaveTextContent('true');

    fireEvent.click(screen.getByTestId('filters-search'));

    await waitFor(() => {
      expect(useLeadsMock).toHaveBeenLastCalledWith(
        expect.objectContaining({
          search: 'Ana',
          page: 0,
          size: 10,
        }),
      );
    });

    fireEvent.click(screen.getByTestId('archive-trigger'));
    expect(screen.getByTestId('archive-dialog')).toHaveTextContent('true');

    fireEvent.click(screen.getByTestId('confirm-archive'));

    await waitFor(() => {
      expect(archiveMutateAsync).toHaveBeenCalledWith('lead-1');
    });
  });
});
