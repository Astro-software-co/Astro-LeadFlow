import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LeadPrivacyActions } from '@/components/leads/LeadPrivacyActions';

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
const deleteMutate = vi.fn().mockResolvedValue(undefined);
const anonymizeMutate = vi.fn().mockResolvedValue({});
vi.mock('@/hooks/api/usePrivacy', () => ({
  useDeleteLead: () => ({ mutateAsync: deleteMutate, isPending: false }),
  useAnonymizeLead: () => ({ mutateAsync: anonymizeMutate, isPending: false }),
}));
let role = 'MANAGER';
vi.mock('@/contexts/AuthContext', () => ({ useAuth: () => ({ user: { role, organizationId: 'org-1' } }) }));

describe('LeadPrivacyActions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    role = 'MANAGER';
  });

  it('SELLER nao ve as acoes', () => {
    role = 'SELLER';
    render(<LeadPrivacyActions leadId="l1" phone="+5511987654321" />);
    expect(screen.queryByRole('button', { name: /Excluir lead/ })).not.toBeInTheDocument();
  });

  it('exclusao avisa que e irreversivel e envia includeSamePhone', async () => {
    const onDone = vi.fn();
    render(<LeadPrivacyActions leadId="l1" leadName="Maria" phone="+5511987654321" onDone={onDone} />);
    fireEvent.click(screen.getByRole('button', { name: /Excluir lead/ }));
    expect(screen.getByText('Esta ação é irreversível.')).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('Excluir também os outros atendimentos deste telefone'));
    fireEvent.click(screen.getByRole('button', { name: 'Excluir definitivamente' }));
    await vi.waitFor(() => expect(onDone).toHaveBeenCalled());
    expect(deleteMutate).toHaveBeenCalledWith({ id: 'l1', options: { includeSamePhone: true } });
  });

  it('anonimizar usa o telefone do lead; no card so aparece excluir', async () => {
    const { unmount } = render(<LeadPrivacyActions leadId="l1" phone="+5511987654321" />);
    fireEvent.click(screen.getByRole('button', { name: /Anonimizar/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Anonimizar definitivamente' }));
    await vi.waitFor(() => expect(anonymizeMutate).toHaveBeenCalledWith('+5511987654321'));
    unmount();
    render(<LeadPrivacyActions leadId="l1" phone="+5511987654321" deleteOnly />);
    expect(screen.queryByRole('button', { name: /Anonimizar/ })).not.toBeInTheDocument();
  });
});
