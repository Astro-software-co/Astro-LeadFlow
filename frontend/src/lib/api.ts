import axios from 'axios';
import type { AuthResponseDTO, RefreshTokenRequestDTO } from '@/types/api/auth';

// In-memory token store — tokens are kept in module-level closure only.
// This prevents XSS attacks from accessing tokens via JavaScript.
export const tokenStore = {
  _accessToken: null as string | null,
  _refreshToken: null as string | null,
  get accessToken() {
    return this._accessToken;
  },
  get refreshToken() {
    return this._refreshToken;
  },
  set(access: string, refresh: string) {
    this._accessToken = access;
    this._refreshToken = refresh;
  },
  clear() {
    this._accessToken = null;
    this._refreshToken = null;
  },
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

// Dedicated client for refresh requests to avoid interceptor recursion/cycles.
const refreshApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

async function refreshAuthToken(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }> {
  const payload: RefreshTokenRequestDTO = { refreshToken };
  const response = await refreshApi.post<AuthResponseDTO>('/auth/refresh', payload);

  const nextAccessToken = response.data.accessToken;
  const nextRefreshToken = response.data.refreshToken;

  if (!nextAccessToken || !nextRefreshToken) {
    throw new Error('Resposta de autenticacao incompleta: tokens ausentes');
  }

  return {
    accessToken: nextAccessToken,
    refreshToken: nextRefreshToken,
  };
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
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refresh = tokenStore.refreshToken;

      if (!refresh) {
        tokenStore.clear();
        window.location.href = '/app/login';
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // Queue subsequent 401s while refresh is in progress
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
        const newAuth = await refreshAuthToken(refresh);
        tokenStore.set(newAuth.accessToken, newAuth.refreshToken);
        processQueue(null, newAuth.accessToken);
        originalRequest.headers.Authorization = `Bearer ${newAuth.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        tokenStore.clear();
        window.location.href = '/app/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export default api;
