import { Skeleton } from '@/components/ui/skeleton';

export function LeadsLoadingState() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <Skeleton className="h-8 w-32" />
          <Skeleton className="mt-2 h-4 w-48" />
        </div>
        <Skeleton className="h-10 w-32" />
      </div>
      <div data-testid="leads-skeleton" className="rounded-xl border bg-card shadow-sm overflow-hidden">
        <table className="hidden md:table w-full min-w-[880px]">
          <thead>
            <tr className="border-b bg-muted/30">
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Lead</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Contato</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Origem</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Estagio</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Interesse</th>
              <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">Atualizado</th>
              <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-muted-foreground">Acoes</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {Array.from({ length: 8 }).map((_, index) => (
              <tr key={index} data-testid="skeleton-row">
                <td className="px-4 py-4"><Skeleton className="h-4 w-40" /></td>
                <td className="px-4 py-4"><Skeleton className="h-4 w-24" /></td>
                <td className="px-4 py-4"><Skeleton className="h-4 w-24" /></td>
                <td className="px-4 py-4"><Skeleton className="h-4 w-32" /></td>
                <td className="px-4 py-4"><Skeleton className="h-4 w-20" /></td>
                <td className="px-4 py-4"><Skeleton className="h-4 w-20" /></td>
                <td className="px-4 py-4"><Skeleton className="h-4 w-16 ml-auto" /></td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="block md:hidden space-y-4 p-4">
          {Array.from({ length: 8 }).map((_, index) => (
            <div key={index} className="rounded-xl border bg-card p-4 shadow-sm space-y-3">
              <div className="flex items-start justify-between">
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-5 w-20 rounded-full" />
              </div>
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-40" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
