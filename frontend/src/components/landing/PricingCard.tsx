import { Link } from 'react-router-dom';
import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

interface PricingCardProps {
  plan: {
    id: string;
    name: string;
    monthlyPrice: number | null;
    annualPrice: number | null;
    features: readonly string[];
    highlight: boolean;
    ctaLabel: string;
    ctaVariant: 'default' | 'outline';
  };
  billingCycle: 'mensal' | 'anual';
}

export function PricingCard({ plan, billingCycle }: PricingCardProps) {
  const price = billingCycle === 'mensal' ? plan.monthlyPrice : plan.annualPrice;

  return (
    <div
      className={cn(
        'rounded-xl bg-white p-6 flex flex-col hover:ring-2 hover:ring-primary transition-all duration-200',
        plan.highlight ? 'border-2 border-primary shadow-lg ring-2 ring-primary/20' : 'border border-slate-200',
      )}
    >
      {plan.highlight && (
        <span className="bg-primary text-white text-xs font-bold px-3 py-1 rounded-full inline-block mb-2 self-start">
          MAIS POPULAR
        </span>
      )}
      <h3 className="text-xl font-bold text-slate-900">{plan.name}</h3>

      <div className="mt-4">
        {price === null ? (
          <p className="text-4xl font-bold text-slate-900">Gratis</p>
        ) : (
          <>
            <p className="text-4xl font-bold text-slate-900">
              R$ {price}
              <span className="text-sm font-normal text-slate-500">/mes</span>
            </p>
            {billingCycle === 'anual' && <p className="text-xs text-slate-400 mt-1">cobrado anualmente</p>}
          </>
        )}
      </div>

      <ul className="space-y-2 mt-6 border-t border-slate-100 pt-6 flex-1">
        {plan.features.map((feature) => (
          <li key={feature} className="flex items-center gap-2 text-sm text-slate-600">
            <Check className="h-4 w-4 text-primary flex-shrink-0" />
            {feature}
          </li>
        ))}
      </ul>

      <Button variant={plan.ctaVariant} className="w-full mt-6" asChild>
        <Link to="/app/login">{plan.ctaLabel}</Link>
      </Button>
      <p className="text-xs text-slate-400 mt-3 text-center">Sem cartao de credito</p>
    </div>
  );
}
