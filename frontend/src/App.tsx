import { Suspense, lazy } from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { ThemeProvider } from "./contexts/ThemeContext";
import { NotificationProvider } from "./contexts/NotificationContext";
import { LandingLayout } from '@/components/landing/LandingLayout';
import { AppLayout } from '@/components/layout/AppLayout';
import LandingPage from '@/pages/LandingPage';
import NotFound from "./pages/NotFound";

const Dashboard = lazy(() => import('@/pages/Dashboard'));
const Kanban = lazy(() => import('@/pages/Kanban'));
const Estoque = lazy(() => import('@/pages/Estoque'));
const Configuracoes = lazy(() => import('@/pages/Configuracoes'));
const Login = lazy(() => import('@/pages/Login'));

const queryClient = new QueryClient();

const App = () => (
  <HelmetProvider>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <NotificationProvider>
          <TooltipProvider>
            <Toaster />
            <Sonner />
            <BrowserRouter>
              <Routes>
                <Route element={<LandingLayout />}>
                  <Route path="/" element={<LandingPage />} />
                </Route>
                <Route path="/app" element={<AppLayout />}>
                  <Route path="dashboard" element={<Suspense fallback={null}><Dashboard /></Suspense>} />
                  <Route path="kanban" element={<Suspense fallback={null}><Kanban /></Suspense>} />
                  <Route path="estoque" element={<Suspense fallback={null}><Estoque /></Suspense>} />
                  <Route path="configuracoes" element={<Suspense fallback={null}><Configuracoes /></Suspense>} />
                </Route>
                <Route path="/app/login" element={<Suspense fallback={null}><Login /></Suspense>} />
                <Route path="*" element={<NotFound />} />
              </Routes>
            </BrowserRouter>
          </TooltipProvider>
        </NotificationProvider>
      </ThemeProvider>
    </QueryClientProvider>
  </HelmetProvider>
);

export default App;
