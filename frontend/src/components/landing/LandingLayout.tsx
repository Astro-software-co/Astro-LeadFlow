import { Outlet } from 'react-router-dom';
import { LandingNavbar } from './LandingNavbar';
import { LandingFooter } from './LandingFooter';

export function LandingLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground focus:shadow-lg"
      >
        Pular para o conteudo
      </a>
      <LandingNavbar />
      <main id="main-content" className="flex-1">
        <Outlet />
      </main>
      <LandingFooter />
    </div>
  );
}
