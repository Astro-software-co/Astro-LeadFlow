import { Button } from '@/components/ui/button';

interface LeadsPaginationProps {
  totalElements: number;
  totalPages: number;
  safePage: number;
  pageSize: number;
  currentCount: number;
  onPrevious: () => void;
  onNext: () => void;
}

export function LeadsPagination({
  totalElements,
  totalPages,
  safePage,
  pageSize,
  currentCount,
  onPrevious,
  onNext,
}: LeadsPaginationProps) {
  return (
    <div className="flex items-center justify-between border-t px-4 py-3">
      <p className="text-xs text-muted-foreground">
        Mostrando {totalElements === 0 ? 0 : (safePage - 1) * pageSize + 1} a{' '}
        {totalElements === 0 ? 0 : (safePage - 1) * pageSize + currentCount} de {totalElements} leads
      </p>
      <div className="flex items-center gap-2">
        <Button variant="outline" size="sm" disabled={safePage <= 1} onClick={onPrevious}>
          Anterior
        </Button>
        <span className="text-xs text-muted-foreground">
          Pagina {safePage} de {totalPages}
        </span>
        <Button variant="outline" size="sm" disabled={safePage >= totalPages} onClick={onNext}>
          Proxima
        </Button>
      </div>
    </div>
  );
}
