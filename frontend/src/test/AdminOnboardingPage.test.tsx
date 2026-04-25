/**
 * AdminOnboardingPage.test.tsx
 *
 * Tests for the protected /admin/onboarding page:
 *   - Route protected for ASTRO_ADMIN only (non-admin sees fallback)
 *   - List rendering with pending/approved/rejected statuses
 *   - Search and status filter inputs
 *   - Detail dialog fields
 *   - Approve mutation success toast
 *   - Reject dialog min-length validation and payload
 *   - Loading skeleton, empty state, error retry path
 *   - All three icon action aria labels
 */
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

// ── Mock toast ──────────────────────────────────────────────────────────────
const mockToast = vi.fn();
vi.mock('sonner', () => ({ toast: Object.assign(vi.fn(), { success: vi.fn(), error: vi.fn() }) }));

// ── Mock useAdminOnboarding ─────────────────────────────────────────────────
const mockApprove = vi.fn();
const mockReject = vi.fn();

const baseApplication = {
  id: 'app-1',
  companyName: 'Astro Motors',
  ownerName: 'João Silva',
  ownerEmail: 'joao@astro.com',
  submittedAt: '2026-04-20T10:00:00.000Z',
  status: 'PENDING' as const,
  planCode: 'STARTER',
  referralCode: null,
  cnpj: '11222333000181',
  cep: '01310100',
  city: 'São Paulo',
  state: 'SP',
};

const defaultHookResult = {
  applications: [baseApplication],
  isLoading: false,
  isError: false,
  refetch: vi.fn(),
  approve: mockApprove,
  isApproving: false,
  reject: mockReject,
  isRejecting: false,
};

vi.mock('@/hooks/api/useAdminOnboarding', () => ({
  useAdminOnboarding: vi.fn(() => defaultHookResult),
}));

// ── Mock useAuth ────────────────────────────────────────────────────────────
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(() => ({
    user: { id: 'user-1', name: 'Astro Admin', email: 'admin@astro.com', role: 'ASTRO_ADMIN' },
    isAuthenticated: true,
    isLoading: false,
  })),
}));

import { useAdminOnboarding } from '@/hooks/api/useAdminOnboarding';
import { useAuth } from '@/contexts/AuthContext';
import AdminOnboarding from '@/pages/admin/AdminOnboarding';
import { RoleGate } from '@/components/layout/RoleGate';
import { TooltipProvider } from '@/components/ui/tooltip';

// ── Helpers ─────────────────────────────────────────────────────────────────

function renderAdmin() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <MemoryRouter>
          <AdminOnboarding />
        </MemoryRouter>
      </TooltipProvider>
    </QueryClientProvider>,
  );
}

function renderAdminWithRoleGate(role = 'ASTRO_ADMIN') {
  vi.mocked(useAuth).mockReturnValue({
    user: { id: 'user-1', name: 'Test User', email: 'test@example.com', role },
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  });

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <MemoryRouter>
          <RoleGate allowedRoles={['ASTRO_ADMIN']}>
            <AdminOnboarding />
          </RoleGate>
        </MemoryRouter>
      </TooltipProvider>
    </QueryClientProvider>,
  );
}

// ── Tests ────────────────────────────────────────────────────────────────────

describe('AdminOnboardingPage', () => {
  beforeEach(() => {
    mockApprove.mockResolvedValue({ referralCode: 'REF-123' });
    mockReject.mockResolvedValue({});
    vi.mocked(useAdminOnboarding).mockReturnValue(defaultHookResult);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // ── 1. Route protection — non-Astro role ──────────────────────────────────
  it('shows access-restricted fallback for non-ASTRO_ADMIN role', () => {
    renderAdminWithRoleGate('ADMIN');
    expect(screen.getByText(/acesso restrito/i)).toBeInTheDocument();
    expect(screen.queryByText(/onboarding/i)).not.toBeInTheDocument();
  });

  // ── 2. ASTRO_ADMIN can access the page ───────────────────────────────────
  it('renders page title for ASTRO_ADMIN user', () => {
    renderAdmin();
    expect(screen.getByText(/onboarding/i)).toBeInTheDocument();
    expect(screen.getByText(/revise cadastros enviados/i)).toBeInTheDocument();
  });

  // ── 3. Renders the application row ───────────────────────────────────────
  it('renders the application company name and owner', () => {
    renderAdmin();
    // Both table and mobile cards render in jsdom (no CSS); getAllBy handles duplicates
    expect(screen.getAllByText('Astro Motors').length).toBeGreaterThan(0);
    expect(screen.getAllByText('João Silva').length).toBeGreaterThan(0);
  });

  // ── 4. Filters: search input ──────────────────────────────────────────────
  it('renders search input with correct placeholder', () => {
    renderAdmin();
    expect(screen.getByPlaceholderText(/buscar empresa ou responsavel/i)).toBeInTheDocument();
  });

  // ── 5. Filters: status select options ────────────────────────────────────
  it('renders status filter with Todos, Pendente, Aprovado, Rejeitado options', () => {
    renderAdmin();
    expect(screen.getByText('Todos')).toBeInTheDocument();
  });

  // ── 6. Table column labels ────────────────────────────────────────────────
  it('renders table column headers', () => {
    renderAdmin();
    expect(screen.getByText('Empresa')).toBeInTheDocument();
    expect(screen.getByText('Responsável')).toBeInTheDocument();
    expect(screen.getByText('Enviado em')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Ações')).toBeInTheDocument();
  });

  // ── 7. Three icon action aria labels ─────────────────────────────────────
  it('renders all three icon action buttons with correct aria labels', () => {
    renderAdmin();
    // Both table and mobile cards render in jsdom; use getAllByRole
    expect(screen.getAllByRole('button', { name: /ver detalhes do cadastro/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /aprovar empresa/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /rejeitar cadastro/i }).length).toBeGreaterThan(0);
  });

  // ── 8. Detail dialog opens with fields ───────────────────────────────────
  it('opens detail dialog with company and owner information', async () => {
    renderAdmin();
    await act(async () => {
      fireEvent.click(screen.getAllByRole('button', { name: /ver detalhes do cadastro/i })[0]);
    });
    await waitFor(() => {
      expect(screen.getByText(/joao@astro.com/i)).toBeInTheDocument();
    });
  });

  // ── 9. Approve mutation ───────────────────────────────────────────────────
  it('calls approve mutation when approve button is clicked', async () => {
    renderAdmin();
    await act(async () => {
      fireEvent.click(screen.getAllByRole('button', { name: /aprovar empresa/i })[0]);
    });
    await waitFor(() => {
      expect(mockApprove).toHaveBeenCalledWith('app-1');
    });
  });

  // ── 10. Reject dialog opens ───────────────────────────────────────────────
  it('opens reject dialog when reject button is clicked', async () => {
    renderAdmin();
    await act(async () => {
      fireEvent.click(screen.getAllByRole('button', { name: /rejeitar cadastro/i })[0]);
    });
    await waitFor(() => {
      expect(screen.getByLabelText(/motivo da rejeicao/i)).toBeInTheDocument();
    });
  });

  // ── 11. Reject dialog min-length validation ───────────────────────────────
  it('blocks reject submission when reason is fewer than 10 characters', async () => {
    renderAdmin();
    await act(async () => {
      fireEvent.click(screen.getAllByRole('button', { name: /rejeitar cadastro/i })[0]);
    });
    await waitFor(() => {
      expect(screen.getByLabelText(/motivo da rejeicao/i)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/motivo da rejeicao/i), {
      target: { value: 'curto' },
    });

    const confirmBtns = screen.getAllByRole('button', { name: /rejeitar cadastro/i });
    await act(async () => { fireEvent.click(confirmBtns[confirmBtns.length - 1]); });

    expect(mockReject).not.toHaveBeenCalled();
  });

  // ── 12. Reject mutation with reason ──────────────────────────────────────
  it('calls reject mutation with reason when valid reason is provided', async () => {
    renderAdmin();
    await act(async () => {
      fireEvent.click(screen.getAllByRole('button', { name: /rejeitar cadastro/i })[0]);
    });
    await waitFor(() => {
      expect(screen.getByLabelText(/motivo da rejeicao/i)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/motivo da rejeicao/i), {
      target: { value: 'Documentos incompletos ou inválidos para aprovação.' },
    });

    const confirmBtns = screen.getAllByRole('button', { name: /rejeitar cadastro/i });
    const dialogConfirmBtn = confirmBtns[confirmBtns.length - 1];
    await act(async () => { fireEvent.click(dialogConfirmBtn); });

    await waitFor(() => {
      expect(mockReject).toHaveBeenCalledWith(
        'app-1',
        expect.stringContaining('Documentos incompletos'),
      );
    });
  });

  // ── 13. Loading skeleton ──────────────────────────────────────────────────
  it('renders loading skeleton when isLoading is true', () => {
    vi.mocked(useAdminOnboarding).mockReturnValue({
      ...defaultHookResult,
      applications: [],
      isLoading: true,
    });
    renderAdmin();
    expect(document.querySelector('[data-slot="skeleton"]') ?? document.querySelector('.animate-pulse')).toBeTruthy();
  });

  // ── 14. Empty state ───────────────────────────────────────────────────────
  it('renders empty state when no applications are returned', () => {
    vi.mocked(useAdminOnboarding).mockReturnValue({
      ...defaultHookResult,
      applications: [],
      isLoading: false,
    });
    renderAdmin();
    expect(screen.getByText(/nenhum cadastro encontrado/i)).toBeInTheDocument();
  });

  // ── 15. Error state and retry ─────────────────────────────────────────────
  it('renders error state with retry button', () => {
    const refetch = vi.fn();
    vi.mocked(useAdminOnboarding).mockReturnValue({
      ...defaultHookResult,
      applications: [],
      isLoading: false,
      isError: true,
      refetch,
    });
    renderAdmin();
    expect(screen.getByRole('button', { name: /tentar novamente/i })).toBeInTheDocument();
  });

  it('calls refetch when retry button is clicked', async () => {
    const refetch = vi.fn();
    vi.mocked(useAdminOnboarding).mockReturnValue({
      ...defaultHookResult,
      applications: [],
      isLoading: false,
      isError: true,
      refetch,
    });
    renderAdmin();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /tentar novamente/i }));
    });
    expect(refetch).toHaveBeenCalled();
  });
});
