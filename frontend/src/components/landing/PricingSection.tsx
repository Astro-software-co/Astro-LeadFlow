import { useRef, useState, useEffect } from 'react';
import { cn } from '@/lib/utils';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PricingCard } from './PricingCard';

const PLANS = [
  {
    id: 'starter',
    name: 'Starter',
    monthlyPrice: null,
    annualPrice: null,
    messageLimit: 100,
    marketplaceLimit: 1,
    userLimit: 2,
    features: [
      '100 mensagens/mes',
      '1 marketplace',
      '2 usuarios',
      'Dashboard basico',
    ],
    highlight: false,
    ctaLabel: 'Comecar gratis',
    ctaVariant: 'outline' as const,
  },
  {
    id: 'basico',
    name: 'Basico',
    monthlyPrice: 149,
    annualPrice: 119,
    messageLimit: 1000,
    marketplaceLimit: 1,
    userLimit: 5,
    features: [
      '1.000 mensagens/mes',
      '1 marketplace',
      '5 usuarios',
      'Relatorios basicos',
    ],
    highlight: false,
    ctaLabel: 'Assinar agora',
    ctaVariant: 'outline' as const,
  },
  {
    id: 'profissional',
    name: 'Profissional',
    monthlyPrice: 299,
    annualPrice: 239,
    messageLimit: 3000,
    marketplaceLimit: 3,
    userLimit: 15,
    features: [
      '3.000 mensagens/mes',
      '3 marketplaces',
      '15 usuarios',
      'Analytics avancado',
      'Suporte prioritario',
    ],
    highlight: true,
    ctaLabel: 'Assinar agora',
    ctaVariant: 'default' as const,
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    monthlyPrice: 599,
    annualPrice: 479,
    messageLimit: 10000,
    marketplaceLimit: 10,
    userLimit: 999,
    features: [
      '10.000 mensagens/mes',
      '10 marketplaces',
      'Usuarios ilimitados',
      'API dedicada',
      'SLA garantido',
    ],
    highlight: false,
    ctaLabel: 'Assinar agora',
    ctaVariant: 'outline' as const,
  },
] as const;

export function PricingSection() {
  const sectionRef = useRef<HTMLElement>(null);
  const [isVisible, setIsVisible] = useState(false);
  const [billingCycle, setBillingCycle] = useState<'mensal' | 'anual'>(
    'mensal'
  );

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)'
    ).matches;
    if (prefersReducedMotion) {
      setIsVisible(true);
      return;
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -64px 0px' }
    );
    if (sectionRef.current) observer.observe(sectionRef.current);
    return () => observer.disconnect();
  }, []);

  return (
    <section
      id="precos"
      ref={sectionRef}
      className={cn(
        'bg-slate-50 py-16 md:py-24 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <p className="text-sm font-bold text-primary uppercase tracking-widest">
            Precos
          </p>
          <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mt-3">
            Planos para toda concessionaria
          </h2>
          <p className="text-lg text-slate-500 mt-4">
            Comece gratis. Sem cartao de credito.
          </p>
        </div>

        <div className="flex justify-center mt-8">
          <Tabs
            value={billingCycle}
            onValueChange={(v) => setBillingCycle(v as 'mensal' | 'anual')}
          >
            <TabsList>
              <TabsTrigger value="mensal">Mensal</TabsTrigger>
              <TabsTrigger value="anual">
                Anual
                <span className="ml-2 bg-primary/10 text-primary text-xs rounded-full px-2 py-0.5">
                  Economize 20%
                </span>
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-10">
          {PLANS.map((plan) => (
            <PricingCard
              key={plan.id}
              plan={plan}
              billingCycle={billingCycle}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
