import { CardTag, KanbanCard, KanbanColumn, Lead, LeadOrigin } from '@/types';
import { LeadCreateRequestDTO, LeadResponseDTO, LeadUpdateRequestDTO, PipelineStageDTO } from '@/types/api/leads';

const originMap: Record<string, LeadOrigin> = {
  OLX: 'OLX',
  INSTAGRAM: 'Instagram',
  FACEBOOK: 'Facebook',
  MOBIAUTO: 'Mobiauto',
  NAPISTA: 'Napista',
  DIRETO: 'Direto',
  DIRECT: 'Direto',
  WEBMOTORS: 'WebMotors',
  MERCADO_LIVRE: 'Mercado Livre',
  MERCADOLIVRE: 'Mercado Livre',
};

function mapLeadOrigin(origin: string): LeadOrigin {
  const normalized = origin.trim().toUpperCase();
  return originMap[normalized] ?? 'Direto';
}

function mapStatusTag(column: KanbanColumn): CardTag | null {
  switch (column) {
    case 'negociacao':
      return { type: 'status', label: 'Em negociacao', variant: 'negotiation' };
    case 'aguardando_documentacao':
      return { type: 'status', label: 'Aguardando docs', variant: 'waiting' };
    case 'concluido':
      return { type: 'status', label: 'Concluido', variant: 'completed' };
    default:
      return null;
  }
}

export function mapPipelineStageToKanbanColumn(stage: PipelineStageDTO): KanbanColumn {
  switch (stage) {
    case 'TALKING_TO_BOT':
      return 'chatbot';
    case 'WAITING_ATTENDANCE':
      return 'aguardando';
    case 'IN_ATTENDANCE':
      return 'aguardando_documentacao';
    case 'IN_NEGOTIATION':
      return 'negociacao';
    case 'COMPLETED':
    case 'ARCHIVED':
      return 'concluido';
    default:
      return 'chatbot';
  }
}

export function mapKanbanColumnToPipelineStage(column: KanbanColumn): PipelineStageDTO {
  switch (column) {
    case 'chatbot':
      return 'TALKING_TO_BOT';
    case 'aguardando':
      return 'WAITING_ATTENDANCE';
    case 'aguardando_documentacao':
      return 'IN_ATTENDANCE';
    case 'negociacao':
      return 'IN_NEGOTIATION';
    case 'concluido':
      return 'COMPLETED';
    default:
      return 'WAITING_ATTENDANCE';
  }
}

export function mapLeadResponseToLead(dto: LeadResponseDTO): Lead {
  return {
    id: dto.id,
    nome: dto.name ?? 'Lead sem nome',
    telefone: dto.phoneNumber,
    email: dto.email ?? undefined,
    origem: mapLeadOrigin(dto.origin),
    dataCriacao: new Date(dto.createdAt),
    ultimaInteracao: new Date(dto.updatedAt),
    interesse: dto.vehicleInterest ?? undefined,
    orcamento: dto.budget ?? undefined,
    urgencia: (dto.urgency?.toLowerCase() as Lead['urgencia']) ?? undefined,
    tempoCompra: dto.purchaseForecast ?? undefined,
    observacoes: dto.notes ?? undefined,
    estagio: mapPipelineStageToKanbanColumn(dto.pipelineStage),
    assignedToId: dto.assignedTo?.id,
    assignedToName: dto.assignedTo?.name,
    organizationId: dto.organizationId,
    isDuplicado: dto.isDuplicate,
  };
}

export function mapLeadToKanbanCard(lead: Lead): KanbanCard {
  const column = lead.estagio ?? 'aguardando';
  const tags: CardTag[] = [];

  if (lead.interesse) {
    tags.push({ type: 'vehicle', label: lead.interesse });
  }

  if (lead.assignedToName) {
    tags.push({ type: 'seller', label: lead.assignedToName });
  }

  const statusTag = mapStatusTag(column);
  if (statusTag) {
    tags.push(statusTag);
  }

  return {
    id: lead.id,
    leadId: lead.id,
    lead,
    vendedorId: lead.assignedToId,
    vendedorNome: lead.assignedToName,
    coluna: column,
    tags,
    interesse: lead.interesse,
    orcamento: lead.orcamento,
  };
}

export function mapLeadResponseToKanbanCard(dto: LeadResponseDTO): KanbanCard {
  const lead = mapLeadResponseToLead(dto);
  const column = mapPipelineStageToKanbanColumn(dto.pipelineStage);

  const tags: CardTag[] = [];

  if (dto.vehicle) {
    tags.push({
      type: 'vehicle',
      label: `${dto.vehicle.brand} ${dto.vehicle.model} ${dto.vehicle.year}`,
    });
  }

  if (dto.assignedTo) {
    tags.push({
      type: 'seller',
      label: dto.assignedTo.name,
    });
  }

  const statusTag = mapStatusTag(column);
  if (statusTag) {
    tags.push(statusTag);
  }

  return {
    id: dto.id,
    leadId: dto.id,
    lead,
    vehicleId: dto.vehicle?.id,
    vehicle: dto.vehicle
      ? {
          id: dto.vehicle.id,
          marca: dto.vehicle.brand,
          modelo: dto.vehicle.model,
          ano: dto.vehicle.year,
          preco: dto.vehicle.salePrice,
          categoria: 'Outros',
          status: 'disponivel',
        }
      : undefined,
    vendedorId: dto.assignedTo?.id,
    vendedorNome: dto.assignedTo?.name,
    coluna: column,
    tags,
    interesse: dto.vehicleInterest ?? undefined,
    orcamento: dto.budget ?? undefined,
  };
}

export function mapLeadToCreateRequest(data: Partial<Lead>): LeadCreateRequestDTO {
  if (!data.telefone) {
    throw new Error('Telefone do lead e obrigatorio para criacao');
  }

  return {
    name: data.nome,
    phoneNumber: data.telefone,
    email: data.email,
    origin: data.origem ?? 'Direto',
    vehicleInterest: data.interesse,
    budget: data.orcamento,
    urgency: data.urgencia,
    purchaseForecast: data.tempoCompra,
    notes: data.observacoes,
  };
}

export function mapLeadToUpdateRequest(data: Partial<Lead>): LeadUpdateRequestDTO {
  return {
    name: data.nome,
    email: data.email,
    vehicleInterest: data.interesse,
    budget: data.orcamento,
    urgency: data.urgencia,
    purchaseForecast: data.tempoCompra,
    notes: data.observacoes,
  };
}
