import { useRef, useState, useEffect } from 'react';
import { cn } from '@/lib/utils';

const integrations = ['Webmotors', 'Mercado Livre', 'WhatsApp'];

export function IntegrationsSection() {
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
        'bg-white py-16 md:py-20 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="text-center">
          <h2 className="text-2xl md:text-3xl font-bold text-slate-900">Conecte suas fontes de leads</h2>
          <p className="text-slate-500 mt-3">
            Integracoes nativas com os principais marketplaces automotivos do Brasil
          </p>
        </div>
        <div className="flex flex-wrap items-center justify-center gap-8 md:gap-16 mt-10">
          {integrations.map((name) => (
            <div
              key={name}
              className="h-10 flex items-center text-xl font-bold text-slate-400 hover:text-slate-600 transition-colors cursor-default"
            >
              {name}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
