import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ErrorBoundary } from '@/components/layout/ErrorBoundary';

function ThrowingChild(): JSX.Element {
  throw new Error('Test error');
}

describe('ErrorBoundary', () => {
  it('renders children when no error', () => {
    render(
      <ErrorBoundary>
        <p>Content</p>
      </ErrorBoundary>,
    );
    expect(screen.getByText('Content')).toBeInTheDocument();
  });

  it('renders fallback UI when child throws', () => {
    // Suppress console.error from React's error boundary logging
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    render(
      <ErrorBoundary>
        <ThrowingChild />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Algo deu errado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /recarregar/i })).toBeInTheDocument();
    spy.mockRestore();
  });

  it('calls window.location.reload on button click', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const reloadMock = vi.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload: reloadMock },
      writable: true,
    });

    render(
      <ErrorBoundary>
        <ThrowingChild />
      </ErrorBoundary>,
    );
    fireEvent.click(screen.getByRole('button', { name: /recarregar/i }));
    expect(reloadMock).toHaveBeenCalledOnce();
    spy.mockRestore();
  });
});
