import { Button } from '@/components/ui/button';

interface LeadsErrorStateProps {
  onRetry: () => void;
}

export function LeadsErrorState({ onRetry }: LeadsErrorStateProps) {
  return (
    <div className="rounded-xl border bg-card p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-foreground">Nao foi possivel carregar os leads</h2>
      <p className="mt-2 text-sm text-muted-foreground">Verifique sua conexao e tente novamente.</p>
      <Button className="mt-4" onClick={onRetry}>
        Tentar novamente
      </Button>
    </div>
  );
}
