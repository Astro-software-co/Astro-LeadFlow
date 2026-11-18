import { useRef, useState, useEffect } from 'react';
import { ArrowRight } from 'lucide-react';
import { cn } from '@/lib/utils';

const channels = ['Webmotors', 'Mercado Livre', 'WhatsApp'];

export function ProblemSolutionSection() {
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
        'bg-slate-50 py-16 md:py-24 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mx-auto text-center">
          <p className="text-sm font-bold text-primary uppercase tracking-widest">O problema</p>
          <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mt-4">
            Leads chegam por WhatsApp, e-mail, marketplaces...
          </h2>
          <p className="text-xl text-slate-600 italic mt-4">...e voce perde metade deles.</p>
          <p className="text-lg text-slate-600 mt-6">O Astro LeadFlow centraliza tudo em um so lugar.</p>

          {/* Before/After diagram */}
          <div className="flex flex-col md:flex-row items-center justify-center gap-6 md:gap-8 mt-12">
            {/* Channel boxes */}
            <div className="flex flex-col gap-2">
              {channels.map((channel) => (
                <div
                  key={channel}
                  className="bg-white border border-slate-200 rounded-lg px-4 py-2 text-sm font-medium text-slate-600"
                >
                  {channel}
                </div>
              ))}
            </div>

            {/* Arrow */}
            <ArrowRight className="text-primary flex-shrink-0 rotate-90 md:rotate-0" size={32} />

            {/* Astro box */}
            <div className="bg-primary/10 border border-primary/30 rounded-xl px-6 py-4 text-center">
              <p className="text-sm font-bold text-primary">Astro LeadFlow</p>
              <p className="text-xs text-slate-500 mt-1">Todos os leads. Um lugar.</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
