import { Linkedin, Instagram } from 'lucide-react';
import { AstroLogo } from './AstroLogo';

const footerLinks = {
  produto: [
    { label: 'Funcionalidades', href: '#funcionalidades' },
    { label: 'Precos', href: '#precos' },
    { label: 'Changelog', href: '#' },
  ],
  empresa: [
    { label: 'Sobre', href: '#' },
    { label: 'Contato', href: '#contato' },
    { label: 'Blog', href: '#' },
  ],
  legal: [
    { label: 'Termos', href: '#' },
    { label: 'Privacidade', href: '#' },
    { label: 'Status', href: '#' },
  ],
};

export function LandingFooter() {
  return (
    <footer className="bg-slate-950 py-12 md:py-16">
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-12">
          {/* Brand column */}
          <div>
            <AstroLogo variant="light" />
            <p className="text-sm text-slate-400 mt-4 max-w-xs">Sua concessionaria no piloto automatico.</p>
            <div className="flex items-center gap-4 mt-6">
              <a
                href="#"
                className="text-slate-500 hover:text-slate-200 transition-colors cursor-pointer"
                aria-label="LinkedIn"
              >
                <Linkedin className="h-5 w-5" />
              </a>
              <a
                href="#"
                className="text-slate-500 hover:text-slate-200 transition-colors cursor-pointer"
                aria-label="Instagram"
              >
                <Instagram className="h-5 w-5" />
              </a>
            </div>
          </div>

          {/* Produto */}
          <div>
            <h3 className="text-sm font-bold text-slate-200 uppercase tracking-widest mb-4">Produto</h3>
            <ul className="space-y-3">
              {footerLinks.produto.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="text-sm text-slate-400 hover:text-slate-200 transition-colors">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Empresa */}
          <div>
            <h3 className="text-sm font-bold text-slate-200 uppercase tracking-widest mb-4">Empresa</h3>
            <ul className="space-y-3">
              {footerLinks.empresa.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="text-sm text-slate-400 hover:text-slate-200 transition-colors">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h3 className="text-sm font-bold text-slate-200 uppercase tracking-widest mb-4">Legal</h3>
            <ul className="space-y-3">
              {footerLinks.legal.map((link) => (
                <li key={link.label}>
                  <a href={link.href} className="text-sm text-slate-400 hover:text-slate-200 transition-colors">
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="border-t border-slate-800 mt-10 pt-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-xs text-slate-500">&copy; 2026 Astro Software Co. Todos os direitos reservados.</p>
          <div className="flex items-center gap-6">
            <a href="#" className="text-xs text-slate-500 hover:text-slate-300 transition-colors">
              Termos
            </a>
            <a href="#" className="text-xs text-slate-500 hover:text-slate-300 transition-colors">
              Privacidade
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
