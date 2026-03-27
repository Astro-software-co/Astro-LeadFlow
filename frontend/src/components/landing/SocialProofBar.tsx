import { useRef, useState, useEffect } from 'react';
import { cn } from '@/lib/utils';

const metrics = [
  { value: '+500', label: 'concessionarias atendidas' },
  { value: '+50.000', label: 'leads gerenciados' },
  { value: '99.9%', label: 'de disponibilidade' },
];

export function SocialProofBar() {
  const sectionRef = useRef<HTMLElement>(null);
  const [isVisible, setIsVisible] = useState(false);

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
      ref={sectionRef}
      className={cn(
        'bg-white border-y border-slate-200 py-8 md:py-10 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row items-center justify-center gap-8 md:gap-0 divide-y sm:divide-y-0 sm:divide-x divide-slate-200">
          {metrics.map((metric) => (
            <div
              key={metric.label}
              className="flex-1 flex flex-col items-center text-center px-8 py-4 sm:py-0"
            >
              <span className="text-3xl font-bold text-primary">
                {metric.value}
              </span>
              <span className="text-sm text-slate-500 mt-1">
                {metric.label}
              </span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
