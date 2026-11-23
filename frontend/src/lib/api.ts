import axios from 'axios';
import type { AuthResponseDTO } from '@/types/api/auth';
import { emitAuthSessionEnded } from '@/lib/auth-events';

function resolveApiBaseUrl() {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }

  if (typeof window !== 'undefined') {
    return `${window.location.protocol}//${window.location.hostname}:8080/api/v1`;
  }

  return 'http://localhost:8080/api/v1';
}

// In-memory access token store — access tokens never persist across reloads.
// The refresh token is managed as a secure httpOnly cookie by the backend.
export const tokenStore = {
  _accessToken: null as string | null,
  get accessToken() {
    return this._accessToken;
  },
  set(access: string) {
    this._accessToken = access;
  },
  clear() {
    this._accessToken = null;
  },
};

const api = axios.create({
  baseURL: resolveApiBaseUrl(),
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

// Dedicated client for refresh requests to avoid interceptor recursion/cycles.
const refreshApi = axios.create({
  baseURL: resolveApiBaseUrl(),
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

async function refreshAuthToken(): Promise<{ accessToken: string }> {
  const response = await refreshApi.post<AuthResponseDTO>('/auth/refresh');

  const nextAccessToken = response.data.accessToken;

  if (!nextAccessToken) {
    throw new Error('Resposta de autenticacao incompleta: access token ausente');
  }

  return {
    accessToken: nextAccessToken,
  };
}

function isRefreshRequest(url?: string) {
  return typeof url === 'string' && url.includes('/auth/refresh');
}

function isAuthRequest(url?: string) {
  return typeof url === 'string' && url.includes('/auth/');
}

function shouldAttemptSessionRefresh(status: number | undefined, url?: string): boolean {
  if (status === 401) {
    return true;
  }

  // Spring Security returns 403 for role-protected endpoints when an expired
  // access token is ignored by the JWT filter and the request becomes anonymous.
  return status === 403 && Boolean(tokenStore.accessToken) && !isAuthRequest(url);
}

// Refresh queue mechanism — prevents concurrent refresh attempts
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: string) => void;
  reject: (reason: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token!);
    }
  });
  failedQueue = [];
}

api.interceptors.request.use((config) => {
  const token = tokenStore.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as (typeof error.config & { _retry?: boolean }) | undefined;

    if ((error.response?.status === 401 || error.response?.status === 403) && isRefreshRequest(originalRequest?.url)) {
      tokenStore.clear();
      emitAuthSessionEnded();
      return Promise.reject(error);
    }

    if (shouldAttemptSessionRefresh(error.response?.status, originalRequest?.url) && originalRequest && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue subsequent auth failures while refresh is in progress
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const newAuth = await refreshAuthToken();
        tokenStore.set(newAuth.accessToken);
        processQueue(null, newAuth.accessToken);
        originalRequest.headers.Authorization = `Bearer ${newAuth.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        tokenStore.clear();
        emitAuthSessionEnded();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export default api;
