import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import type { OnboardingApplicationDTO, OnboardingStatus } from '@/types/api/admin-onboarding';

const STATUS_LABELS: Record<OnboardingStatus, string> = {
  PENDING_APPROVAL: 'Pendente',
  APPROVED: 'Aprovado',
  REJECTED: 'Rejeitado',
};

const STATUS_VARIANTS: Record<OnboardingStatus, 'secondary' | 'default' | 'destructive'> = {
  PENDING_APPROVAL: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
};

interface AdminOnboardingDetailDialogProps {
  application: OnboardingApplicationDTO | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function Row({ label, value }: { label: string; value?: string | number | null }) {
  if (!value && value !== 0) return null;
  return (
    <div>
      <span className="text-xs text-muted-foreground">{label}</span>
      <p className="text-sm font-medium">{value}</p>
    </div>
  );
}

function getSubmittedAt(application: OnboardingApplicationDTO): string {
  return application.submittedAt ?? application.createdAt;
}

export function AdminOnboardingDetailDialog({
  application,
  open,
  onOpenChange,
}: AdminOnboardingDetailDialogProps) {
  if (!application) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {application.companyName}
            <Badge variant={STATUS_VARIANTS[application.status]}>
              {STATUS_LABELS[application.status]}
            </Badge>
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 text-sm">
          <section>
            <h3 className="mb-2 font-semibold text-xs uppercase text-muted-foreground tracking-wide">
              Empresa
            </h3>
            <div className="space-y-1">
              <Row label="CNPJ" value={application.cnpj} />
              <Row label="Nome fantasia" value={application.tradeName} />
              <Row label="Segmento" value={application.dealershipSegment} />
              <Row label="Site" value={application.website} />
            </div>
          </section>

          <section>
            <h3 className="mb-2 font-semibold text-xs uppercase text-muted-foreground tracking-wide">
              Responsável
            </h3>
            <div className="space-y-1">
              <Row label="Nome" value={application.ownerName} />
              <Row label="E-mail" value={application.ownerEmail} />
              <Row label="CPF" value={application.ownerCpf} />
              <Row label="Telefone" value={application.ownerPhone} />
              <Row label="Cargo" value={application.ownerRole} />
            </div>
          </section>

          <section>
            <h3 className="mb-2 font-semibold text-xs uppercase text-muted-foreground tracking-wide">
              Endereço
            </h3>
            <div className="space-y-1">
              <Row label="Logradouro" value={application.street} />
              <Row label="Bairro" value={application.neighborhood} />
              <Row label="Cidade/UF" value={application.city ? `${application.city} – ${application.state}` : undefined} />
              <Row label="CEP" value={application.cep} />
            </div>
          </section>

          {application.referralCode && (
            <section>
              <h3 className="mb-2 font-semibold text-xs uppercase text-muted-foreground tracking-wide">
                Indicação
              </h3>
              <Row label="Código" value={application.referralCode} />
            </section>
          )}

          <section>
            <h3 className="mb-2 font-semibold text-xs uppercase text-muted-foreground tracking-wide">
              Auditoria
            </h3>
            <div className="space-y-1">
              <Row label="Plano solicitado" value={application.planCode} />
              <Row
                label="Enviado em"
                value={new Date(getSubmittedAt(application)).toLocaleDateString('pt-BR')}
              />
              {application.approvedAt && (
                <Row
                  label="Aprovado em"
                  value={new Date(application.approvedAt).toLocaleDateString('pt-BR')}
                />
              )}
              {application.rejectedAt && (
                <Row
                  label="Rejeitado em"
                  value={new Date(application.rejectedAt).toLocaleDateString('pt-BR')}
                />
              )}
              {application.rejectionReason && (
                <Row label="Motivo da rejeição" value={application.rejectionReason} />
              )}
            </div>
          </section>
        </div>
      </DialogContent>
    </Dialog>
  );
}
