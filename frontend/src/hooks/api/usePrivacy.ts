import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { leadsService } from '@/services/leads.service';
import { privacyService } from '@/services/privacy.service';
import type {
  CreateDataSubjectRequestDTO,
  DataSubjectRequestStatus,
  DeleteLeadOptions,
  ExecuteDataSubjectRequestDTO,
} from '@/types/api/privacy';

function useOrgId() {
  const { user } = useAuth();
  return user?.organizationId ?? 'unknown';
}

export function usePrivacyRequests(status?: DataSubjectRequestStatus) {
  const orgId = useOrgId();
  return useQuery({
    queryKey: [orgId, 'privacy', 'requests', status ?? 'all'],
    queryFn: () => privacyService.list(status),
    staleTime: 30_000,
  });
}

function useInvalidatePrivacy() {
  const orgId = useOrgId();
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: [orgId, 'privacy'] });
}

function useInvalidateLeads() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('leads') });
    queryClient.invalidateQueries({ predicate: (query) => query.queryKey.includes('kanban') });
  };
}

export function useCreatePrivacyRequest() {
  const invalidate = useInvalidatePrivacy();
  return useMutation({
    mutationFn: (data: CreateDataSubjectRequestDTO) => privacyService.create(data),
    onSuccess: () => invalidate(),
  });
}

export function useExecutePrivacyRequest() {
  const invalidatePrivacy = useInvalidatePrivacy();
  const invalidateLeads = useInvalidateLeads();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ExecuteDataSubjectRequestDTO }) => privacyService.execute(id, data),
    onSuccess: () => {
      invalidatePrivacy();
      invalidateLeads();
    },
  });
}

export function useRejectPrivacyRequest() {
  const invalidate = useInvalidatePrivacy();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => privacyService.reject(id, reason),
    onSuccess: () => invalidate(),
  });
}

/** "Anonimizar" a partir do lead: registra a solicitação e a executa com o telefone do lead. */
export function useAnonymizeLead() {
  const invalidatePrivacy = useInvalidatePrivacy();
  const invalidateLeads = useInvalidateLeads();
  return useMutation({
    mutationFn: async (phone: string) => {
      const request = await privacyService.create({ type: 'ANONYMIZATION', identifier: phone, channel: 'MANUAL' });
      return privacyService.execute(request.id, { identifier: phone });
    },
    onSuccess: () => {
      invalidatePrivacy();
      invalidateLeads();
    },
  });
}

export function useDeleteLead() {
  const invalidateLeads = useInvalidateLeads();
  return useMutation({
    mutationFn: ({ id, options }: { id: string; options?: DeleteLeadOptions }) => leadsService.delete(id, options),
    onSuccess: () => invalidateLeads(),
  });
}
