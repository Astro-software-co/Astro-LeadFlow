import { TrendingUp } from 'lucide-react';
import { RecentSale } from '@/types';
import { formatCurrency } from '@/lib/formatters';

interface RecentSalesTableProps {
  sales: RecentSale[];
}

export function RecentSalesTable({ sales }: RecentSalesTableProps) {
  return (
    <div className="rounded-xl border bg-card shadow-sm animate-fade-in">
      <div className="flex items-center justify-between border-b p-6">
        <div>
          <h3 className="text-lg font-semibold text-foreground">Vendas Recentes</h3>
          <p className="text-sm text-muted-foreground">Últimas transações realizadas</p>
        </div>
        <TrendingUp className="h-5 w-5 text-muted-foreground" />
      </div>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b bg-muted/30">
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Cliente
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Veículo
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Valor
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Data
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Vendedor
              </th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {sales.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-sm text-muted-foreground">
                  Nenhuma venda registrada ainda.
                </td>
              </tr>
            ) : (
              sales.map((sale, index) => (
                <tr key={index} className="hover:bg-muted/20 transition-colors">
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-foreground">{sale.cliente}</td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{sale.veiculo}</td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm font-medium text-chart-primary">
                    {typeof sale.valor === 'number' ? formatCurrency(sale.valor) : '—'}
                  </td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{sale.data}</td>
                  <td className="whitespace-nowrap px-6 py-4 text-sm text-muted-foreground">{sale.vendedor}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
