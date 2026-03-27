import { Link } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function HeroSection() {
  return (
    <section className="bg-gradient-to-br from-slate-900 to-slate-800 pt-32 pb-20 md:pt-40 md:pb-28 lg:pt-44 lg:pb-36">
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Copy column */}
          <div>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl xl:text-7xl font-bold tracking-tight text-slate-50">
              Pare de perder leads.{' '}
              <span className="text-primary">Comece a vender.</span>
            </h1>
            <p className="text-lg md:text-xl text-slate-400 max-w-xl mt-6">
              O Astro LeadFlow centraliza leads de Webmotors, Mercado Livre e
              WhatsApp, organiza seu pipeline de vendas e automatiza o primeiro
              contato.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 mt-8">
              <Button variant="default" size="lg" asChild>
                <Link to="/app/login">Comece gratis</Link>
              </Button>
              <Button
                variant="outline"
                size="lg"
                asChild
                className="border-white/30 text-white hover:bg-white/10"
              >
                <a href="#contato">Agendar demonstracao</a>
              </Button>
            </div>
            <div className="flex items-center gap-2 mt-8 text-sm text-slate-400">
              <CheckCircle2 className="h-4 w-4 text-green-400 flex-shrink-0" />
              <span>Integrado com Webmotors e Mercado Livre</span>
            </div>
          </div>

          {/* CSS Dashboard Mockup — desktop */}
          <div className="hidden lg:block">
            <div
              className="rounded-xl border border-white/10 shadow-2xl overflow-hidden max-w-lg ml-auto"
              style={{
                transform: 'perspective(1000px) rotateY(-8deg) rotateX(4deg)',
              }}
            >
              {/* Window chrome */}
              <div className="bg-slate-700 px-4 py-3 flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-red-400" />
                <div className="w-3 h-3 rounded-full bg-yellow-400" />
                <div className="w-3 h-3 rounded-full bg-green-400" />
              </div>
              {/* Content */}
              <div className="bg-slate-800/80 p-4">
                <div className="grid grid-cols-4 gap-2 mb-4">
                  <div className="h-16 rounded-lg bg-blue-500/40" />
                  <div className="h-16 rounded-lg bg-green-500/40" />
                  <div className="h-16 rounded-lg bg-purple-500/40" />
                  <div className="h-16 rounded-lg bg-orange-500/40" />
                </div>
                <div className="h-28 bg-slate-700/60 rounded-lg mb-4 bg-gradient-to-t from-blue-500/20 to-transparent" />
                <div className="space-y-2">
                  <div className="h-8 bg-slate-700/40 rounded" />
                  <div className="h-8 bg-slate-700/40 rounded" />
                  <div className="h-8 bg-slate-700/40 rounded" />
                </div>
              </div>
            </div>
          </div>

          {/* CSS Dashboard Mockup — mobile */}
          <div className="lg:hidden mt-12">
            <div className="rounded-xl border border-white/10 shadow-2xl overflow-hidden max-w-sm mx-auto">
              <div className="bg-slate-700 px-4 py-3 flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-red-400" />
                <div className="w-3 h-3 rounded-full bg-yellow-400" />
                <div className="w-3 h-3 rounded-full bg-green-400" />
              </div>
              <div className="bg-slate-800/80 p-4">
                <div className="grid grid-cols-4 gap-2 mb-4">
                  <div className="h-12 rounded-lg bg-blue-500/40" />
                  <div className="h-12 rounded-lg bg-green-500/40" />
                  <div className="h-12 rounded-lg bg-purple-500/40" />
                  <div className="h-12 rounded-lg bg-orange-500/40" />
                </div>
                <div className="h-20 bg-slate-700/60 rounded-lg mb-3 bg-gradient-to-t from-blue-500/20 to-transparent" />
                <div className="space-y-2">
                  <div className="h-6 bg-slate-700/40 rounded" />
                  <div className="h-6 bg-slate-700/40 rounded" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
