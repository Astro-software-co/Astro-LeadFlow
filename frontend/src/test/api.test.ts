import { describe, it, expect, beforeEach } from 'vitest';
import api from '@/lib/api';

describe('API client', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('has correct baseURL', () => {
    expect(api.defaults.baseURL).toContain('/api/v1');
  });

  it('has Content-Type header', () => {
    expect(api.defaults.headers['Content-Type']).toBe('application/json');
  });

  it('attaches Authorization header when token exists', () => {
    localStorage.setItem('access_token', 'test-jwt-token');
    const config = {
      headers: {} as Record<string, string>,
    };

    // Manually invoke the request interceptor logic
    const interceptor = api.interceptors.request.handlers[0];
    if (interceptor?.fulfilled) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const result = interceptor.fulfilled(config as any);
      expect(result.headers.Authorization).toBe('Bearer test-jwt-token');
    }
  });

  it('does not attach Authorization header without token', () => {
    const config = {
      headers: {} as Record<string, string>,
    };

    const interceptor = api.interceptors.request.handlers[0];
    if (interceptor?.fulfilled) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const result = interceptor.fulfilled(config as any);
      expect(result.headers.Authorization).toBeUndefined();
    }
  });
});
