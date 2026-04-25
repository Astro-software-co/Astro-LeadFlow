import { useState } from 'react';
import { Eye, Check, X, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { AdminOnboardingDetailDialog } from '@/components/admin/AdminOnboardingDetailDialog';
import { RejectOnboardingDialog } from '@/components/admin/RejectOnboardingDialog';
import { useAdminOnboarding } from '@/hooks/api/useAdminOnboarding';
import type { OnboardingApplicationDTO, OnboardingStatus } from '@/types/api/admin-onboarding';

// ── Constants ────────────────────────────────────────────────────────────────

const STATUS_LABELS: Record<OnboardingStatus, string> = {
  PENDING: 'Pendente',
  APPROVED: 'Aprovado',
  REJECTED: 'Rejeitado',
};

const STATUS_VARIANTS: Record<OnboardingStatus, 'secondary' | 'default' | 'destructive'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
};

// ── Sub-components ────────────────────────────────────────────────────────────

function LoadingSkeletons() {
  return (
    <div className="space-y-3">
      {[1, 2, 3].map((i) => (
        <Skeleton key={i} className="h-14 w-full rounded-lg" />
      ))}
    </div>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-4 py-12 text-center">
      <p className="text-sm text-muted-foreground">
        Nao foi possivel carregar os cadastros. Verifique sua conexao.
      </p>
      <Button variant="outline" onClick={onRetry}>
        Tentar novamente
      </Button>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center gap-2 py-12 text-center">
      <p className="text-sm text-muted-foreground">Nenhum cadastro encontrado</p>
    </div>
  );
}

interface ActionButtonsProps {
  application: OnboardingApplicationDTO;
  onView: () => void;
  onApprove: () => void;
  onReject: () => void;
  isApproving: boolean;
}

function ActionButtons({ application, onView, onApprove, onReject, isApproving }: ActionButtonsProps) {
  const isPending = application.status === 'PENDING';
  return (
    <div className="flex items-center gap-1">
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={onView}
            aria-label="Ver detalhes do cadastro"
          >
            <Eye className="h-4 w-4" />
          </Button>
        </TooltipTrigger>
        <TooltipContent>Ver detalhes do cadastro</TooltipContent>
      </Tooltip>

      {isPending && (
        <>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-green-600 hover:text-green-700"
                onClick={onApprove}
                disabled={isApproving}
                aria-label="Aprovar empresa"
              >
                {isApproving ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Check className="h-4 w-4" />
                )}
              </Button>
            </TooltipTrigger>
            <TooltipContent>{isApproving ? 'Aprovando...' : 'Aprovar empresa'}</TooltipContent>
          </Tooltip>

          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-destructive hover:text-destructive/80"
                onClick={onReject}
                aria-label="Rejeitar cadastro"
              >
                <X className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Rejeitar cadastro</TooltipContent>
          </Tooltip>
        </>
      )}
    </div>
  );
}

// ── Mobile card ───────────────────────────────────────────────────────────────

interface MobileCardProps {
  application: OnboardingApplicationDTO;
  onView: () => void;
  onApprove: () => void;
  onReject: () => void;
  isApproving: boolean;
}

function MobileCard({ application, onView, onApprove, onReject, isApproving }: MobileCardProps) {
  return (
    <div className="rounded-lg border bg-card p-4 space-y-2 md:hidden">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-medium text-sm">{application.companyName}</p>
          <p className="text-xs text-muted-foreground">{application.ownerName}</p>
        </div>
        <Badge variant={STATUS_VARIANTS[application.status]}>
          {STATUS_LABELS[application.status]}
        </Badge>
      </div>
      <p className="text-xs text-muted-foreground">
        Enviado em: {new Date(application.submittedAt).toLocaleDateString('pt-BR')}
      </p>
      <ActionButtons
        application={application}
        onView={onView}
        onApprove={onApprove}
        onReject={onReject}
        isApproving={isApproving}
      />
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function AdminOnboarding() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [detailApp, setDetailApp] = useState<OnboardingApplicationDTO | null>(null);
  const [rejectApp, setRejectApp] = useState<OnboardingApplicationDTO | null>(null);

  const { applications, isLoading, isError, refetch, approve, isApproving, reject, isRejecting } =
    useAdminOnboarding({ search: search || undefined, status: statusFilter });

  async function handleApprove(app: OnboardingApplicationDTO) {
    await approve(app.id);
  }

  async function handleRejectConfirm(reason: string) {
    if (!rejectApp) return;
    await reject(rejectApp.id, reason);
    setRejectApp(null);
  }

  const filtered = applications.filter((app) => {
    const matchesSearch =
      !search ||
      app.companyName.toLowerCase().includes(search.toLowerCase()) ||
      app.ownerName.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || app.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="container mx-auto max-w-6xl px-4 py-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">Onboarding</h1>
        <p className="text-sm text-muted-foreground">
          Revise cadastros enviados por empresas e aprove acessos.
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Input
          placeholder="Buscar empresa ou responsavel"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="sm:max-w-xs"
        />
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="sm:w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todos</SelectItem>
            <SelectItem value="PENDING">Pendente</SelectItem>
            <SelectItem value="APPROVED">Aprovado</SelectItem>
            <SelectItem value="REJECTED">Rejeitado</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Content */}
      {isLoading ? (
        <LoadingSkeletons />
      ) : isError ? (
        <ErrorState onRetry={() => refetch()} />
      ) : filtered.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          {/* Desktop table — hidden below md */}
          <div className="hidden md:block rounded-lg border overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Empresa</TableHead>
                  <TableHead>Responsável</TableHead>
                  <TableHead>Enviado em</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filtered.map((app) => (
                  <TableRow key={app.id}>
                    <TableCell className="font-medium">{app.companyName}</TableCell>
                    <TableCell>{app.ownerName}</TableCell>
                    <TableCell>
                      {new Date(app.submittedAt).toLocaleDateString('pt-BR')}
                    </TableCell>
                    <TableCell>
                      <Badge variant={STATUS_VARIANTS[app.status]}>
                        {STATUS_LABELS[app.status]}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <ActionButtons
                        application={app}
                        onView={() => setDetailApp(app)}
                        onApprove={() => handleApprove(app)}
                        onReject={() => setRejectApp(app)}
                        isApproving={isApproving}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Mobile cards — visible below md */}
          <div className="space-y-3 md:hidden">
            {filtered.map((app) => (
              <MobileCard
                key={app.id}
                application={app}
                onView={() => setDetailApp(app)}
                onApprove={() => handleApprove(app)}
                onReject={() => setRejectApp(app)}
                isApproving={isApproving}
              />
            ))}
          </div>
        </>
      )}

      {/* Detail dialog */}
      <AdminOnboardingDetailDialog
        application={detailApp}
        open={detailApp !== null}
        onOpenChange={(open) => { if (!open) setDetailApp(null); }}
      />

      {/* Reject dialog */}
      <RejectOnboardingDialog
        open={rejectApp !== null}
        onOpenChange={(open) => { if (!open) setRejectApp(null); }}
        onConfirm={handleRejectConfirm}
        isSubmitting={isRejecting}
      />
    </div>
  );
}
