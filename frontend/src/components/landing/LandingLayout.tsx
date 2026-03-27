import { Outlet } from 'react-router-dom';
import { LandingNavbar } from './LandingNavbar';
import { LandingFooter } from './LandingFooter';

export function LandingLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <LandingNavbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <LandingFooter />
    </div>
  );
}
