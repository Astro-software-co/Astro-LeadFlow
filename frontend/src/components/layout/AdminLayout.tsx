import { BarChart3, ClipboardCheck, LogOut, Sparkles } from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { cn } from '@/lib/utils';

const adminNavItems = [
  { label: 'Solicitacoes', path: '/admin/onboarding', icon: ClipboardCheck },
  { label: 'Dashboard', path: '/admin/dashboard', icon: BarChart3 },
];

export function AdminLayout() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();

  const handleSignOut = async () => {
    await logout();
    toast.success('Você foi desconectado com sucesso!');
    navigate('/app/login');
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 border-b border-border/50 bg-card/95 backdrop-blur supports-[backdrop-filter]:bg-card/60">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary">
              <Sparkles className="h-5 w-5 text-primary-foreground" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-foreground">Astro LeadFlow</p>
              <p className="truncate text-xs text-muted-foreground">{user?.email ?? 'Admin Astro'}</p>
            </div>
          </div>

          <nav className="flex items-center gap-1">
            {adminNavItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex h-9 items-center gap-2 rounded-md px-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                    isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                <span className="hidden sm:inline">{item.label}</span>
              </NavLink>
            ))}
          </nav>

          <Button
            variant="ghost"
            size="icon"
            onClick={() => {
              void handleSignOut();
            }}
            aria-label="Sair"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </header>
      <Outlet />
    </div>
  );
}
