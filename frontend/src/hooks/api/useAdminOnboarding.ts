import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { adminOnboardingService } from '@/services/admin-onboarding.service';
import type { OnboardingApplicationDTO } from '@/types/api/admin-onboarding';
import type { AdminOnboardingFilters } from '@/services/admin-onboarding.service';

export function useAdminOnboarding(filters: AdminOnboardingFilters = {}) {
  const queryClient = useQueryClient();
  const [isApproving, setIsApproving] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin-onboarding', filters],
    queryFn: () => adminOnboardingService.list(filters),
    staleTime: 30_000,
  });

  const applications: OnboardingApplicationDTO[] = data?.content ?? [];

  const approve = useCallback(
    async (id: string) => {
      setIsApproving(true);
      try {
        const result = await adminOnboardingService.approve(id);
        toast.success('Cadastro aprovado. Codigo de indicacao gerado.');
        queryClient.invalidateQueries({ queryKey: ['admin-onboarding'] });
        return result;
      } catch {
        toast.error('Nao foi possivel aprovar o cadastro. Tente novamente.');
        throw new Error('Approve failed');
      } finally {
        setIsApproving(false);
      }
    },
    [queryClient],
  );

  const reject = useCallback(
    async (id: string, reason: string) => {
      setIsRejecting(true);
      try {
        await adminOnboardingService.reject(id, reason);
        toast.success('Cadastro rejeitado.');
        queryClient.invalidateQueries({ queryKey: ['admin-onboarding'] });
      } catch {
        toast.error('Nao foi possivel rejeitar o cadastro. Tente novamente.');
        throw new Error('Reject failed');
      } finally {
        setIsRejecting(false);
      }
    },
    [queryClient],
  );

  return {
    applications,
    isLoading,
    isError,
    refetch,
    approve,
    isApproving,
    reject,
    isRejecting,
  };
}
