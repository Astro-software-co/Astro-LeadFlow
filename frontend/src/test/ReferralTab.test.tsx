import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { TooltipProvider } from '@/components/ui/tooltip';
import { ReferralTab } from '@/components/settings/ReferralTab';
import { useReferrals, mockReferralSettings } from '@/hooks/api/useReferrals';
import type { ReferralSettingsDTO } from '@/types/api/referrals';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock('@/hooks/api/useReferrals', () => ({
  mockReferralSettings: {
    code: '483920',
    referralLink: 'https://app.astroleadflow.com/onboarding?ref=483920',
    approvedCount: 8,
    pendingCount: 2,
    currentTier: 'BRONZE',
    commissionRate: 0.08,
    nextTierThreshold: 10,
    balanceBrl: 478.4,
    pendingBrl: 159.2,
    withdrawalAvailable: false,
    referrals: [
      {
        organizationId: 'org-ref-1',
        companyName: 'Rota Sul Veiculos',
        status: 'APPROVED',
        planCode: 'PROFISSIONAL_MONTHLY',
        approvedAt: '2026-04-12T00:00:00.000Z',
        commissionBrl: 23.92,
      },
    ],
  },
  useReferrals: vi.fn(),
}));

function renderReferralTab() {
  return render(
    <TooltipProvider delayDuration={0}>
      <ReferralTab />
    </TooltipProvider>,
  );
}

function mockHook(data: ReferralSettingsDTO = mockReferralSettings) {
  vi.mocked(useReferrals).mockReturnValue({
    data,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useReferrals>);
}

function stubClipboard(writeText = vi.fn().mockResolvedValue(undefined)) {
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText },
  });
  return writeText;
}

describe('ReferralTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHook();
    stubClipboard();
  });

  it('renders referral progress, tier cards, balance, table row, and withdrawal tooltip', async () => {
    renderReferralTab();

    expect(screen.getAllByText('Indicacoes')[0]).toBeInTheDocument();
    expect(screen.getByText('https://app.astroleadflow.com/onboarding?ref=483920')).toBeInTheDocument();
    expect(screen.getByText('Progresso para o proximo nivel')).toBeInTheDocument();
    expect(screen.getByText('8 de 10 empresas aprovadas')).toBeInTheDocument();
    expect(screen.getByText('Bronze 8%')).toBeInTheDocument();
    expect(screen.getByText('Prata 12%')).toBeInTheDocument();
    expect(screen.getByText('Ouro 16%')).toBeInTheDocument();
    expect(screen.getByText('Saldo acumulado')).toBeInTheDocument();
    expect(screen.getByText('R$ 478,40')).toBeInTheDocument();
    expect(screen.getAllByText('Rota Sul Veiculos')[0]).toBeInTheDocument();
    expect(screen.getByText('Empresa')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Plano')).toBeInTheDocument();
    expect(screen.getAllByText('Data de aprovacao')[0]).toBeInTheDocument();
    expect(screen.getAllByText('Comissao')[0]).toBeInTheDocument();
    expect(screen.getAllByText('R$ 23,92')[0]).toBeInTheDocument();

    const withdrawalButton = screen.getByRole('button', { name: /Solicitar saque/i });
    expect(withdrawalButton).toBeDisabled();
    const tooltipTrigger = withdrawalButton.closest('span');
    expect(tooltipTrigger).toHaveAttribute('tabIndex', '0');
    fireEvent.focus(tooltipTrigger!);
    expect((await screen.findAllByText('Em breve'))[0]).toBeInTheDocument();
  }, 10000);

  it('copies referral code and link with success toast', async () => {
    const writeText = stubClipboard();
    renderReferralTab();

    fireEvent.click(screen.getByRole('button', { name: /Copiar codigo de indicacao/i }));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('483920'));
    expect(toast.success).toHaveBeenCalledWith('Copiado para a area de transferencia.');

    fireEvent.click(screen.getByRole('button', { name: /Copiar link de indicacao/i }));
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('https://app.astroleadflow.com/onboarding?ref=483920'));
  });

  it('shows clipboard failure toast', async () => {
    stubClipboard(vi.fn().mockRejectedValue(new Error('denied')));
    renderReferralTab();

    fireEvent.click(screen.getByRole('button', { name: /Copiar codigo de indicacao/i }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Nao foi possivel copiar. Selecione e copie manualmente.');
    });
  });

  it('renders empty referred companies state', () => {
    mockHook({
      ...mockReferralSettings,
      referrals: [],
    });

    renderReferralTab();

    expect(screen.getByText('Nenhuma empresa indicada ainda.')).toBeInTheDocument();
  });
});
