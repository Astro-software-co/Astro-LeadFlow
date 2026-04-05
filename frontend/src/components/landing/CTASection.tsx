import { useRef, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Mail } from 'lucide-react';

export function CTASection() {
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
      id="contato"
      ref={sectionRef}
      className={cn(
        'bg-gradient-to-br from-slate-900 to-slate-800 py-16 md:py-24 transition-all duration-700 ease-out',
        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center">
        <h2 className="text-3xl md:text-4xl font-bold text-white">
          Pronto para nunca mais perder um lead?
        </h2>
        <p className="text-slate-400 mt-4 text-lg">
          Sem cartao de credito. Setup em 5 minutos.
        </p>
        <div className="flex flex-col sm:flex-row justify-center gap-4 mt-8">
          <Button variant="default" size="lg" asChild>
            <Link to="/app/login">Comece gratis agora</Link>
          </Button>
          <Button
            variant="outline"
            size="lg"
            asChild
            className="border-white/30 text-white hover:bg-white/10"
          >
            <a
              href="mailto:contato@astrosoftware.com.br?subject=Demonstra%C3%A7%C3%A3o%20Astro%20LeadFlow"
              rel="noopener noreferrer"
            >
              <Mail className="mr-2 h-4 w-4" />
              Agendar demonstracao
            </a>
          </Button>
        </div>
      </div>
    </section>
  );
}
