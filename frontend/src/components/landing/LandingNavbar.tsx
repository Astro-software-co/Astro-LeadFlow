import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Menu } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { AstroLogo } from './AstroLogo';

const navLinks = [
  { label: 'Funcionalidades', href: '#funcionalidades' },
  { label: 'Precos', href: '#precos' },
  { label: 'Contato', href: '#contato' },
];

export function LandingNavbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 10);
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <nav
      className={cn(
        'fixed top-0 left-0 right-0 z-50 h-16 transition-all duration-300',
        isScrolled
          ? 'bg-white/80 backdrop-blur-md border-b border-slate-200'
          : 'bg-transparent'
      )}
    >
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 h-full flex items-center justify-between">
        <AstroLogo variant={isScrolled ? 'dark' : 'light'} />

        {/* Desktop nav links */}
        <div className="hidden lg:flex items-center gap-8">
          {navLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className={cn(
                'text-sm font-medium transition-colors',
                isScrolled
                  ? 'text-slate-600 hover:text-slate-900'
                  : 'text-slate-400 hover:text-slate-100'
              )}
            >
              {link.label}
            </a>
          ))}
        </div>

        {/* Desktop buttons */}
        <div className="hidden lg:flex items-center gap-3">
          <Button
            variant="ghost"
            asChild
            className={cn(
              isScrolled
                ? ''
                : 'text-white hover:text-white hover:bg-white/10'
            )}
          >
            <Link to="/app/login">Entrar</Link>
          </Button>
          <Button variant="default" size="sm" asChild>
            <Link to="/app/login">Comece gratis</Link>
          </Button>
        </div>

        {/* Mobile hamburger */}
        <div className="lg:hidden">
          <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
            <SheetTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className={cn(
                  isScrolled
                    ? 'text-slate-900'
                    : 'text-white hover:bg-white/10'
                )}
              >
                <Menu className="h-6 w-6" />
                <span className="sr-only">Menu</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-72">
              <SheetTitle className="sr-only">Menu de navegacao</SheetTitle>
              <div className="flex flex-col gap-6 mt-8">
                {navLinks.map((link) => (
                  <a
                    key={link.href}
                    href={link.href}
                    onClick={() => setSheetOpen(false)}
                    className="text-base font-medium text-slate-700 hover:text-slate-900 transition-colors"
                  >
                    {link.label}
                  </a>
                ))}
                <div className="border-t border-slate-200 pt-6 flex flex-col gap-3">
                  <Button variant="outline" asChild onClick={() => setSheetOpen(false)}>
                    <Link to="/app/login">Entrar</Link>
                  </Button>
                  <Button variant="default" asChild onClick={() => setSheetOpen(false)}>
                    <Link to="/app/login">Comece gratis</Link>
                  </Button>
                </div>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </nav>
  );
}
