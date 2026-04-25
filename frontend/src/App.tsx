import { Suspense, lazy } from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { Toaster } from '@/components/ui/sonner';
import { TooltipProvider } from '@/components/ui/tooltip';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './contexts/ThemeContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { AuthProvider } from './contexts/AuthContext';
import { AuthGuard } from '@/components/layout/AuthGuard';
import { RoleGate } from '@/components/layout/RoleGate';
import { LandingLayout } from '@/components/landing/LandingLayout';
import { AppLayout } from '@/components/layout/AppLayout';
import LandingPage from '@/pages/LandingPage';
import NotFound from './pages/NotFound';
import { ErrorBoundary } from '@/components/layout/ErrorBoundary';
import { Loader2 } from 'lucide-react';

const Onboarding = lazy(() => import('@/pages/Onboarding'));
const AdminOnboarding = lazy(() => import('@/pages/admin/AdminOnboarding'));
const Dashboard = lazy(() => import('@/pages/Dashboard'));
const Leads = lazy(() => import('@/pages/Leads'));
const Kanban = lazy(() => import('@/pages/Kanban'));
const Estoque = lazy(() => import('@/pages/Estoque'));
const VehicleDetail = lazy(() => import('@/pages/VehicleDetail'));
const Configuracoes = lazy(() => import('@/pages/Configuracoes'));
const Comunicacao = lazy(() => import('@/pages/Comunicacao'));
const Login = lazy(() => import('@/pages/Login'));

const queryClient = new QueryClient();

const PageLoader = () => (
  <div className="flex min-h-[50vh] items-center justify-center">
    <Loader2 className="h-8 w-8 animate-spin text-primary" />
  </div>
);

const App = () => (
  <HelmetProvider>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <TooltipProvider>
              <Toaster />
              <ErrorBoundary>
                <BrowserRouter>
                  <Routes>
                    <Route element={<LandingLayout />}>
                      <Route path="/" element={<LandingPage />} />
                    </Route>
                    <Route
                      path="/onboarding"
                      element={
                        <Suspense fallback={<PageLoader />}>
                          <Onboarding />
                        </Suspense>
                      }
                    />
                    <Route element={<AuthGuard />}>
                      <Route path="/app" element={<AppLayout />}>
                        <Route index element={<Navigate to="/app/dashboard" replace />} />
                        <Route
                          path="dashboard"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <Dashboard />
                            </Suspense>
                          }
                        />
                        <Route
                          path="leads"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <Leads />
                            </Suspense>
                          }
                        />
                        <Route
                          path="kanban"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <Kanban />
                            </Suspense>
                          }
                        />
                        <Route
                          path="estoque"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <Estoque />
                            </Suspense>
                          }
                        />
                        <Route
                          path="estoque/:id"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <VehicleDetail />
                            </Suspense>
                          }
                        />
                        <Route
                          path="comunicacao"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <Comunicacao />
                            </Suspense>
                          }
                        />
                        <Route
                          path="configuracoes"
                          element={
                            <Suspense fallback={<PageLoader />}>
                              <RoleGate allowedRoles={['ADMIN', 'MANAGER']}>
                                <Configuracoes />
                              </RoleGate>
                            </Suspense>
                          }
                        />
                      </Route>
                    </Route>
                    <Route element={<AuthGuard />}>
                      <Route
                        path="/admin/onboarding"
                        element={
                          <Suspense fallback={<PageLoader />}>
                            <RoleGate allowedRoles={['ASTRO_ADMIN']}>
                              <AdminOnboarding />
                            </RoleGate>
                          </Suspense>
                        }
                      />
                    </Route>
                    <Route
                      path="/app/login"
                      element={
                        <Suspense fallback={<PageLoader />}>
                          <Login />
                        </Suspense>
                      }
                    />
                    <Route path="*" element={<NotFound />} />
                  </Routes>
                </BrowserRouter>
              </ErrorBoundary>
            </TooltipProvider>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  </HelmetProvider>
);

export default App;
