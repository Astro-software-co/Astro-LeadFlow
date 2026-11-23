import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, fireEvent } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import type { ReactNode } from 'react';
import AdminDashboard from '@/pages/admin/AdminDashboard';
import { mockAdminDashboardData, useAdminDashboard } from '@/hooks/api/useAdminDashboard';

vi.mock('recharts', () => {
  const Chart = ({ children }: { children?: ReactNode }) => <div data-testid="chart">{children}</div>;
  return {
    ResponsiveContainer: Chart,
    LineChart: Chart,
    AreaChart: Chart,
    BarChart: Chart,
    PieChart: Chart,
    Line: () => <div />,
    Area: () => <div />,
    Bar: () => <div />,
    Pie: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    Cell: () => <div />,
    CartesianGrid: () => <div />,
    XAxis: () => <div />,
    YAxis: () => <div />,
    Tooltip: () => <div />,
    Legend: () => <div />,
  };
});

vi.mock('@/hooks/api/useAdminDashboard', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/hooks/api/useAdminDashboard')>();
  return {
    ...actual,
    useAdminDashboard: vi.fn(),
  };
});

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AdminDashboard />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AdminDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubEnv('VITE_USE_MOCKS', 'true');
    vi.mocked(useAdminDashboard).mockReturnValue({
      data: mockAdminDashboardData,
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAdminDashboard>);
  });

  it('renders exact finance metric labels and source badge', () => {
    renderPage();

    expect(screen.getByText('Dashboard financeiro')).toBeInTheDocument();
    expect(screen.getByText('Modo mock')).toBeInTheDocument();
    expect(screen.getByText('MRR atual')).toBeInTheDocument();
    expect(screen.getByText('Receita do mes')).toBeInTheDocument();
    expect(screen.getByText('Assinaturas ativas')).toBeInTheDocument();
    expect(screen.getByText('Churn mensal')).toBeInTheDocument();
    expect(screen.getByText('ARPA')).toBeInTheDocument();
    expect(screen.getByText('Projecao 12m')).toBeInTheDocument();
  }, 10000);

  it('renders supplemental roadmap metrics', () => {
    renderPage();

    expect(screen.getByText(/ARR/i)).toBeInTheDocument();
    expect(screen.getByText(/Novas assinaturas no mes/i)).toBeInTheDocument();
    expect(screen.getByText(/Cancelamentos no mes/i)).toBeInTheDocument();
    expect(screen.getByText(/Cadastros pendentes de aprovacao/i)).toBeInTheDocument();
    expect(screen.getByText(/Ticket medio/i)).toBeInTheDocument();
  });

  it('renders chart panels and projection labels', () => {
    renderPage();

    expect(screen.getByText('Receita mensal')).toBeInTheDocument();
    expect(screen.getByText('Crescimento de assinantes e projecao')).toBeInTheDocument();
    expect(screen.getByText('Novas vs cancelamentos')).toBeInTheDocument();
    expect(screen.getByText('Receita por plano')).toBeInTheDocument();
    expect(screen.getByText('3 meses')).toBeInTheDocument();
    expect(screen.getByText('6 meses')).toBeInTheDocument();
    expect(screen.getByText('12 meses')).toBeInTheDocument();
  });

  it('renders visible subscription columns and cancellation detail fields', () => {
    renderPage();

    expect(screen.getByText('Empresa')).toBeInTheDocument();
    expect(screen.getByText('Plano')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('MRR')).toBeInTheDocument();
    expect(screen.getByText('Proxima cobranca')).toBeInTheDocument();
    expect(screen.getByText('Origem')).toBeInTheDocument();
    expect(screen.getAllByText(/Inicio:/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Cancelamento:/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Motivo cancelamento:/i).length).toBeGreaterThan(0);
  });

  it('renders empty subscriptions copy', () => {
    vi.mocked(useAdminDashboard).mockReturnValue({
      data: { ...mockAdminDashboardData, subscriptions: [] },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAdminDashboard>);

    renderPage();

    expect(screen.getByText('Nenhuma assinatura encontrada.')).toBeInTheDocument();
  });

  it('renders loading skeletons', () => {
    vi.mocked(useAdminDashboard).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useAdminDashboard>);

    renderPage();

    expect(document.querySelector('.animate-pulse')).toBeTruthy();
  });

  it('renders error retry path', () => {
    const refetch = vi.fn();
    vi.mocked(useAdminDashboard).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      refetch,
    } as unknown as ReturnType<typeof useAdminDashboard>);

    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /tentar novamente/i }));

    expect(refetch).toHaveBeenCalled();
  });
});
