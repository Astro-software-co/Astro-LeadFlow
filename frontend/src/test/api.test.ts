import { describe, it, expect, beforeEach } from 'vitest';
import { AxiosRequestHeaders, InternalAxiosRequestConfig } from 'axios';
import api, { tokenStore } from '@/lib/api';

function buildConfig(): InternalAxiosRequestConfig {
  return {
    headers: {} as AxiosRequestHeaders,
  } as InternalAxiosRequestConfig;
}

async function runRequestInterceptor(config: InternalAxiosRequestConfig) {
  const interceptor = api.interceptors.request.handlers[0];
  if (!interceptor?.fulfilled) {
    throw new Error('Request interceptor not configured');
  }

  return interceptor.fulfilled(config);
}

describe('API client', () => {
  beforeEach(() => {
    tokenStore.clear();
  });

  it('has correct baseURL', () => {
    expect(api.defaults.baseURL).toContain('/api/v1');
  });

  it('has Content-Type header', () => {
    expect(api.defaults.headers['Content-Type']).toBe('application/json');
  });

  it('attaches Authorization header when token exists', async () => {
    tokenStore.set('test-jwt-token', 'test-refresh-token');
    const config = buildConfig();

    const result = await runRequestInterceptor(config);
    expect(result.headers.Authorization).toBe('Bearer test-jwt-token');
  });

  it('does not attach Authorization header without token', async () => {
    const config = buildConfig();

    const result = await runRequestInterceptor(config);
    expect(result.headers.Authorization).toBeUndefined();
  });
});
