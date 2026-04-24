import axios from 'axios';
import type { SubmitOnboardingRequestDTO, SubmitOnboardingResponseDTO } from '@/types/api/onboarding';

/**
 * Resolves the root host of the current API base URL.
 *
 * The main API client uses `/api/v1` as its base.
 * The public onboarding endpoint lives at `/api/onboarding` (no `/v1`),
 * so we build a separate axios instance pointing at the server root.
 */
function resolveRootBaseUrl(): string {
  const configured = import.meta.env.VITE_API_URL as string | undefined;

  if (configured) {
    // e.g. "http://localhost:8080/api/v1" → "http://localhost:8080"
    try {
      const url = new URL(configured);
      return url.origin;
    } catch {
      // fallback: strip known suffix
      return configured.replace(/\/api\/v1\/?$/, '');
    }
  }

  if (typeof window !== 'undefined') {
    return `${window.location.protocol}//${window.location.hostname}:8080`;
  }

  return 'http://localhost:8080';
}

const rootApi = axios.create({
  baseURL: resolveRootBaseUrl(),
  headers: { 'Content-Type': 'application/json' },
});

export const onboardingService = {
  async submit(payload: SubmitOnboardingRequestDTO): Promise<SubmitOnboardingResponseDTO> {
    const response = await rootApi.post<SubmitOnboardingResponseDTO>('/api/onboarding', payload);
    return response.data;
  },
};
