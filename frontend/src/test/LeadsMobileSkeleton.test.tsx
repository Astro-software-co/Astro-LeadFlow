import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import Leads from '@/pages/Leads';

vi.mock('react-helmet-async', () => ({
  Helmet: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  HelmetProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock('@/hooks/api/useLeads', () => ({
  useLeads: () => ({ data: undefined, isLoading: true, isFetching: false, error: null, refetch: vi.fn() }),
  useCreateLead: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateLead: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useArchiveLead: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useLeadActivities: () => ({ data: undefined, isLoading: false }),
}));

vi.mock('@/hooks/api/useAssignableUsers', () => ({
  useAssignableUsers: () => ({ data: [], isLoading: false }),
}));

vi.mock('@/mappers/leads.mapper', () => ({
  mapKanbanColumnToPipelineStage: vi.fn(),
}));

function renderLeads() {
  return render(
    <MemoryRouter>
      <Leads />
    </MemoryRouter>,
  );
}

describe('Leads skeleton loading state', () => {
  it('renders skeleton container when loading', () => {
    renderLeads();
    expect(screen.getByTestId('leads-skeleton')).toBeInTheDocument();
  });

  it('skeleton has 8 placeholder rows', () => {
    renderLeads();
    const rows = screen.getAllByTestId('skeleton-row');
    expect(rows.length).toBe(8);
  });
});
