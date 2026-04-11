import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Header } from '@/components/layout/Header';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    logout: vi.fn(),
    user: null,
    isAuthenticated: true,
    isLoading: false,
  }),
}));

vi.mock('@/contexts/NotificationContext', () => ({
  useNotifications: () => ({
    notifications: [],
    unreadCount: 0,
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
    removeNotification: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

function renderHeader() {
  return render(
    <MemoryRouter>
      <Header />
    </MemoryRouter>,
  );
}

describe('Header', () => {
  it('renders hamburger button with aria-label', () => {
    renderHeader();
    const hamburger = screen.getByLabelText('Abrir menu de navegacao');
    expect(hamburger).toBeInTheDocument();
  });

  it('renders desktop nav with hidden md:flex class', () => {
    renderHeader();
    const nav = document.querySelector('nav');
    expect(nav).not.toBeNull();
    expect(nav!.className).toContain('hidden');
    expect(nav!.className).toContain('md:flex');
  });

  it('renders Bell button with focus-visible ring', () => {
    renderHeader();
    const bellButton = document.querySelector('button[class*="focus-visible:ring-2"]');
    expect(bellButton).not.toBeNull();
  });

  it('renders Profile button with focus-visible ring', () => {
    renderHeader();
    const allButtons = document.querySelectorAll('button');
    const profileButton = Array.from(allButtons).find(
      (btn) => btn.className.includes('focus-visible:ring-2') && btn.querySelector('svg'),
    );
    expect(profileButton).not.toBeNull();
  });
});
