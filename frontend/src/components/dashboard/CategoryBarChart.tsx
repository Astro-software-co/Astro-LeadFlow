import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { CategorySalesData } from '@/types';

interface CategoryBarChartProps {
  data: CategorySalesData[];
}

export function CategoryBarChart({ data }: CategoryBarChartProps) {
  return (
    <div className="rounded-xl border bg-card p-6 shadow-sm animate-fade-in">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-foreground">Vendas por Categoria</h3>
        <p className="text-sm text-muted-foreground">Distribuição de vendas e estoque por tipo de veículo</p>
      </div>
      <div className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
            <XAxis 
              dataKey="categoria" 
              stroke="hsl(var(--muted-foreground))"
              fontSize={12}
            />
            <YAxis 
              stroke="hsl(var(--muted-foreground))"
              fontSize={12}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: 'hsl(var(--card))',
                border: '1px solid hsl(var(--border))',
                borderRadius: '8px',
              }}
            />
            <Legend />
            <Bar 
              dataKey="vendidos" 
              name="Vendidos"
              fill="hsl(var(--chart-secondary))" 
              radius={[4, 4, 0, 0]}
            />
            <Bar 
              dataKey="emEstoque" 
              name="Em Estoque"
              fill="hsl(var(--chart-muted))" 
              radius={[4, 4, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
