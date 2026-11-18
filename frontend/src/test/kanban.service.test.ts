import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { kanbanService } from '@/services/kanban.service';
import type { PipelineStageDTO } from '@/types/api/leads';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

function buildLead(id: string, stage: PipelineStageDTO) {
  return {
    id,
    name: `Lead ${id}`,
    phoneNumber: '11999999999',
    email: `${id}@astro.test`,
    origin: 'DIRETO',
    vehicleInterest: null,
    budget: null,
    urgency: null,
    purchaseForecast: null,
    interestType: null,
    notes: null,
    pipelineStage: stage,
    assignedTo: null,
    vehicle: null,
    isDuplicate: false,
    organizationId: 'org-1',
    createdAt: '2026-04-10T00:00:00.000Z',
    updatedAt: '2026-04-11T00:00:00.000Z',
  };
}

describe('kanbanService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps ordered and unknown stage buckets into kanban cards', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({
      data: {
        stages: {
          TALKING_TO_BOT: [buildLead('lead-1', 'TALKING_TO_BOT')],
          IN_NEGOTIATION: [buildLead('lead-2', 'IN_NEGOTIATION')],
          CUSTOM_STAGE: [buildLead('lead-3', 'WAITING_ATTENDANCE')],
          BROKEN_STAGE: null,
        },
      },
    });

    const result = await kanbanService.getBoard();

    expect(api.get).toHaveBeenCalledWith('/kanban/board');
    expect(result).toHaveLength(3);
    expect(result[0].coluna).toBe('chatbot');
    expect(result[1].coluna).toBe('negociacao');
    expect(result[2].leadId).toBe('lead-3');
  });
});
