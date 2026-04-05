import { ShieldAlert } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

interface RoleGateProps {
  allowedRoles: string[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

function DefaultFallback() {
  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center gap-4 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10">
        <ShieldAlert className="h-8 w-8 text-destructive" />
      </div>
      <div>
        <h2 className="text-xl font-semibold text-foreground">Acesso restrito</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Voce nao tem permissao para acessar esta pagina.
        </p>
      </div>
    </div>
  );
}

export function RoleGate({ allowedRoles, children, fallback }: RoleGateProps) {
  const { user } = useAuth();

  if (!user || !allowedRoles.includes(user.role)) {
    return fallback ?? <DefaultFallback />;
  }

  return <>{children}</>;
}
