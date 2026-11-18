import { useRef, useState, useEffect } from 'react';
import { BarChart3, Kanban, MessageCircle, Car, Globe, TrendingUp, type LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

const features: Feature[] = [
  {
    icon: BarChart3,
    title: 'Dashboard em tempo real',
    description: 'Acompanhe KPIs de vendas, conversao e performance da equipe em tempo real.',
  },
  {
    icon: Kanban,
    title: 'Pipeline visual de vendas',
    description: 'Arraste leads entre etapas e nunca perca o timing certo para fechar.',
  },
  {
    icon: MessageCircle,
    title: 'WhatsApp automatico',
    description: 'Resposta automatica em segundos. Templates personalizados por marketplace.',
  },
  {
    icon: Car,
    title: 'Estoque integrado',
    description: 'Catalogo de veiculos integrado. Associe veiculos a leads com um clique.',
  },
  {
    icon: Globe,
    title: 'Marketplaces conectados',
    description: 'Webmotors e Mercado Livre conectados. Leads entram automaticamente.',
  },
  {
    icon: TrendingUp,
    title: 'Analytics de conversao',
    description: 'Saiba quais canais convertem mais e onde sua equipe pode melhorar.',
  },
];

export function FeaturesGrid() {
  const sectionRef = useRef<HTMLElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
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
      { threshold: 0.1, rootMargin: '0px 0px -64px 0px' },
    );
    if (sectionRef.current) observer.observe(sectionRef.current);
    return () => observer.disconnect();
  }, []);

  return (
    <section
      id="funcionalidades"
      ref={sectionRef}
      className={cn(
        'bg-white py-16 md:py-24 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <p className="text-sm font-bold text-primary uppercase tracking-widest">Funcionalidades</p>
          <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mt-3">Tudo que sua concessionaria precisa</h2>
          <p className="text-lg text-slate-500 mt-4 max-w-2xl mx-auto">
            Ferramentas integradas para captar, gerenciar e converter leads em vendas.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 md:gap-8 mt-12">
          {features.map((feature) => (
            <div
              key={feature.title}
              className="rounded-xl border border-slate-200 bg-white p-6 hover:-translate-y-1 hover:shadow-lg transition-all duration-200 cursor-pointer"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 mb-4">
                <feature.icon className="h-6 w-6 text-primary" />
              </div>
              <h3 className="text-lg font-bold text-slate-900">{feature.title}</h3>
              <p className="text-sm text-slate-500 mt-2 leading-relaxed">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
