import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/lib/api';
import { privacyService } from '@/services/privacy.service';
import { leadsService } from '@/services/leads.service';

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}));

describe('privacyService', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista com filtro opcional de status', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: [] });
    await privacyService.list();
    await privacyService.list('RECEIVED');
    expect(api.get).toHaveBeenNthCalledWith(1, '/privacy/requests', { params: undefined });
    expect(api.get).toHaveBeenNthCalledWith(2, '/privacy/requests', { params: { status: 'RECEIVED' } });
  });

  it('cria, executa e rejeita nas rotas do backend', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { id: 'r1' } });
    await privacyService.create({ type: 'ANONYMIZATION', identifier: '11987654321' });
    await privacyService.execute('r1', { identifier: '11987654321' });
    await privacyService.reject('r1', 'motivo');
    expect(api.post).toHaveBeenNthCalledWith(1, '/privacy/requests', { type: 'ANONYMIZATION', identifier: '11987654321' });
    expect(api.post).toHaveBeenNthCalledWith(2, '/privacy/requests/r1/execute', { identifier: '11987654321' });
    expect(api.post).toHaveBeenNthCalledWith(3, '/privacy/requests/r1/reject', { reason: 'motivo' });
  });

  it('exclusao de lead envia includeSamePhone e motivo so quando pedidos', async () => {
    vi.mocked(api.delete).mockResolvedValue({ data: undefined });
    await leadsService.delete('l1');
    await leadsService.delete('l1', { includeSamePhone: true, reason: 'duplicado' });
    expect(api.delete).toHaveBeenNthCalledWith(1, '/leads/l1', undefined);
    expect(api.delete).toHaveBeenNthCalledWith(2, '/leads/l1', { params: { includeSamePhone: true, reason: 'duplicado' } });
  });
});
