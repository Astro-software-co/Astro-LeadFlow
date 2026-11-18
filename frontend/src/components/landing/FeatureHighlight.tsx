import { useRef, useState, useEffect } from 'react';
import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

const bullets = [
  'Visualize todo o pipeline em uma so tela',
  'Mova leads com drag-and-drop entre etapas de venda',
  'Saiba exatamente quem precisa de atencao agora',
];

const kanbanColumns = [
  {
    title: 'Novo',
    cards: [{ variant: 'blue' }, { variant: 'default' }, { variant: 'default' }],
  },
  {
    title: 'Qualificado',
    cards: [{ variant: 'default' }, { variant: 'blue' }],
  },
  {
    title: 'Proposta',
    cards: [{ variant: 'default' }, { variant: 'blue' }, { variant: 'default' }],
  },
];

export function FeatureHighlight() {
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
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          {/* Copy */}
          <div>
            <p className="text-sm font-bold text-primary uppercase tracking-widest">Pipeline Kanban</p>
            <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mt-3">
              Arraste leads entre etapas. Sem planilhas. Sem confusao.
            </h2>
            <ul className="mt-6 space-y-4">
              {bullets.map((text) => (
                <li key={text} className="flex items-start gap-3">
                  <Check className="h-5 w-5 text-primary mt-0.5 flex-shrink-0" />
                  <span className="text-slate-600">{text}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* CSS Kanban mockup */}
          <div className="rounded-xl border border-slate-200 shadow-lg overflow-hidden bg-white">
            <div className="bg-slate-100 px-4 py-3 flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-red-400" />
              <div className="w-3 h-3 rounded-full bg-yellow-400" />
              <div className="w-3 h-3 rounded-full bg-green-400" />
            </div>
            <div className="p-4 bg-white">
              <div className="grid grid-cols-3 gap-3">
                {kanbanColumns.map((col) => (
                  <div key={col.title} className="flex flex-col gap-2">
                    <p className="text-xs font-bold text-slate-500 uppercase mb-2 text-center">{col.title}</p>
                    {col.cards.map((card, i) => (
                      <div
                        key={i}
                        className={cn(
                          'h-12 rounded-lg border',
                          card.variant === 'blue' ? 'bg-blue-50 border-blue-200' : 'bg-slate-100 border-slate-200',
                        )}
                      />
                    ))}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
