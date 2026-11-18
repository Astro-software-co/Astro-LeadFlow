import { useRef, useState, useEffect } from 'react';
import { cn } from '@/lib/utils';
import { TestimonialCard } from './TestimonialCard';

const TESTIMONIALS = [
  {
    quote:
      'Reduzimos o tempo de resposta de 4 horas para 15 minutos. Os leads do Webmotors agora sao atendidos antes da concorrencia.',
    name: 'Joao Silva',
    role: 'Gerente Comercial',
    company: 'AutoPrime Veiculos',
    initials: 'JS',
  },
  {
    quote:
      'Antes, metade dos leads do Mercado Livre se perdia em planilhas. Com o LeadFlow, temos visibilidade total do pipeline.',
    name: 'Maria Santos',
    role: 'Diretora',
    company: 'Grupo Nacional Motors',
    initials: 'MS',
  },
  {
    quote:
      'O Kanban mudou nossa operacao. A equipe sabe exatamente o que fazer a cada momento. Vendemos 30% mais no primeiro mes.',
    name: 'Carlos Oliveira',
    role: 'Proprietario',
    company: 'CO Automoveis',
    initials: 'CO',
  },
];

export function TestimonialsSection() {
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
      ref={sectionRef}
      className={cn(
        'bg-white py-16 md:py-24 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <p className="text-sm font-bold text-primary uppercase tracking-widest">Depoimentos</p>
          <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mt-3">O que nossos clientes dizem</h2>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-10">
          {TESTIMONIALS.map((t) => (
            <TestimonialCard key={t.name} {...t} />
          ))}
        </div>
      </div>
    </section>
  );
}
