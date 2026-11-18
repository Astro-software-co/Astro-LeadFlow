import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { EmptyState } from '@/components/shared/EmptyState';
import { Car } from 'lucide-react';

describe('EmptyState', () => {
  it('renders title and description', () => {
    render(<EmptyState icon={Car} title="Sem veiculos" description="Adicione um veiculo." />);
    expect(screen.getByText('Sem veiculos')).toBeInTheDocument();
    expect(screen.getByText('Adicione um veiculo.')).toBeInTheDocument();
  });

  it('renders action button when provided', () => {
    const onAction = vi.fn();
    render(<EmptyState icon={Car} title="Vazio" description="Desc" actionLabel="Adicionar" onAction={onAction} />);
    const button = screen.getByRole('button', { name: 'Adicionar' });
    expect(button).toBeInTheDocument();
    fireEvent.click(button);
    expect(onAction).toHaveBeenCalledOnce();
  });

  it('does not render button without actionLabel', () => {
    render(<EmptyState icon={Car} title="Vazio" description="Desc" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
