import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from '@/pages/Dashboard';

vi.mock('@/hooks/api/useDashboard', () => ({
  useDashboard: () => ({
    kpis: null,
    monthlySales: [],
    categorySales: [],
    recentSales: [],
    isLoading: true,
    error: null,
    refetch: vi.fn(),
  }),
}));

vi.mock('@/hooks/api/useVehicles', () => ({
  useVehicles: () => ({
    data: null,
    isLoading: true,
    error: null,
    refetch: vi.fn(),
  }),
}));

vi.mock('react-helmet-async', () => ({
  Helmet: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  HelmetProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

describe('DashboardSkeleton', () => {
  it('renders dashboard skeleton when loading', () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>,
    );

    expect(screen.getByTestId('dashboard-skeleton')).toBeInTheDocument();
  });

  it('skeleton contains 7 KPI placeholder cards', () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>,
    );

    const skeletonCards = screen.getAllByTestId('skeleton-kpi-card');
    expect(skeletonCards.length).toBeGreaterThanOrEqual(7);
  });
});
