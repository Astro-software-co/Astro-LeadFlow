import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { MonthlySalesData } from '@/types';

interface SalesLineChartProps {
  data: MonthlySalesData[];
}

export function SalesLineChart({ data }: SalesLineChartProps) {
  return (
    <div className="rounded-xl border bg-card p-6 shadow-sm animate-fade-in">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-foreground">Vendas Mensais</h3>
        <p className="text-sm text-muted-foreground">Evolução de vendas nos últimos 6 meses</p>
      </div>
      <div className="h-[300px]">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
            <XAxis 
              dataKey="month" 
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
            <Line 
              type="monotone" 
              dataKey="vendas" 
              name="Vendas"
              stroke="hsl(var(--chart-primary))" 
              strokeWidth={2}
              dot={{ fill: 'hsl(var(--chart-primary))', strokeWidth: 2 }}
              activeDot={{ r: 6 }}
            />
            <Line 
              type="monotone" 
              dataKey="receita" 
              name="Receita (K)"
              stroke="hsl(var(--chart-secondary))" 
              strokeWidth={2}
              dot={{ fill: 'hsl(var(--chart-secondary))', strokeWidth: 2 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
