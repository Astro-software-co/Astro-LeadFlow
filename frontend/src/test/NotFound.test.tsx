import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { HelmetProvider } from 'react-helmet-async';
import NotFound from '@/pages/NotFound';

describe('NotFound', () => {
  it('renders 404 message', () => {
    render(
      <HelmetProvider>
        <NotFound />
      </HelmetProvider>,
    );
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText('Pagina nao encontrada')).toBeInTheDocument();
  });

  it('has a link back to home', () => {
    render(
      <HelmetProvider>
        <NotFound />
      </HelmetProvider>,
    );
    const link = screen.getByRole('link', { name: /voltar ao inicio/i });
    expect(link).toHaveAttribute('href', '/');
  });
});
