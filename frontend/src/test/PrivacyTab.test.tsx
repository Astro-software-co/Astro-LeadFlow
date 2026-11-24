import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PrivacyTab } from '@/components/settings/PrivacyTab';
import { useExecutePrivacyRequest, usePrivacyRequests } from '@/hooks/api/usePrivacy';
import type { DataSubjectRequestDTO } from '@/types/api/privacy';

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
const executeMutate = vi.fn();
vi.mock('@/hooks/api/usePrivacy', () => ({
  usePrivacyRequests: vi.fn(),
  useCreatePrivacyRequest: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useExecutePrivacyRequest: vi.fn(),
  useRejectPrivacyRequest: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

const open: DataSubjectRequestDTO = {
  id: 'r1', type: 'ANONYMIZATION', status: 'RECEIVED', subjectHint: '+55******4321', channel: 'EMAIL',
  receivedAt: '2026-11-01T10:00:00', dueAt: '2026-11-16T10:00:00', completedAt: null, overdue: true, resultSummary: null,
};
const done: DataSubjectRequestDTO = { ...open, id: 'r2', status: 'COMPLETED', overdue: false, resultSummary: 'Leads anonimizados: 1' };

describe('PrivacyTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useExecutePrivacyRequest).mockReturnValue({ mutateAsync: executeMutate, isPending: false } as never);
  });

  it('lista com dica mascarada, selo Atrasada e acoes so nas abertas', () => {
    vi.mocked(usePrivacyRequests).mockReturnValue({ data: [open, done], isLoading: false, isError: false, refetch: vi.fn() } as never);
    render(<PrivacyTab />);
    expect(screen.getAllByText('+55******4321')).toHaveLength(2);
    expect(screen.getByText('Atrasada')).toBeInTheDocument();
    expect(screen.getByText('Leads anonimizados: 1')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Executar' })).toHaveLength(1);
  });

  it('execucao irreversivel avisa e exige o identificador antes de liberar', () => {
    vi.mocked(usePrivacyRequests).mockReturnValue({ data: [open], isLoading: false, isError: false, refetch: vi.fn() } as never);
    render(<PrivacyTab />);
    fireEvent.click(screen.getByRole('button', { name: 'Executar' }));
    expect(screen.getByRole('alert')).toHaveTextContent('irreversível');
    const confirm = screen.getByRole('button', { name: 'Executar (irreversível)' });
    expect(confirm).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Telefone ou e-mail do titular'), { target: { value: '11987654321' } });
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);
    expect(executeMutate).toHaveBeenCalledWith({ id: 'r1', data: { identifier: '11987654321', resultSummary: undefined } });
  });

  it('mostra estado vazio e de erro', () => {
    vi.mocked(usePrivacyRequests).mockReturnValue({ data: [], isLoading: false, isError: false, refetch: vi.fn() } as never);
    const { unmount } = render(<PrivacyTab />);
    expect(screen.getByText('Nenhuma solicitação registrada.')).toBeInTheDocument();
    unmount();
    vi.mocked(usePrivacyRequests).mockReturnValue({ data: undefined, isLoading: false, isError: true, refetch: vi.fn() } as never);
    render(<PrivacyTab />);
    expect(screen.getByText(/Não foi possível carregar/)).toBeInTheDocument();
  });
});
