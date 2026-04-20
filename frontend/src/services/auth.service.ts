import api from '@/lib/api';
import {
  AuthResponseDTO,
  LoginRequestDTO,
  RegisterOrganizationRequestDTO,
  UserProfileResponseDTO,
} from '@/types/api/auth';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  organizationName: string;
  slug: string;
  adminName: string;
  adminEmail: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  pendingTwoFactor: boolean;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: string;
  organizationId: string;
}

function mapTokenResponse(dto: AuthResponseDTO): AuthResponse {
  if (!dto.accessToken) {
    throw new Error('Resposta de autenticacao incompleta: tokens ausentes');
  }

  return {
    accessToken: dto.accessToken,
    pendingTwoFactor: dto.pendingTwoFactor,
  };
}

function mapUserProfile(dto: UserProfileResponseDTO): UserProfile {
  return {
    id: dto.id,
    name: dto.name,
    email: dto.email,
    role: dto.role,
    organizationId: dto.organizationId ?? 'unknown',
  };
}

export const authService = {
  async login(data: LoginRequest): Promise<AuthResponse> {
    const payload: LoginRequestDTO = {
      email: data.email,
      password: data.password,
    };

    const response = await api.post<AuthResponseDTO>('/auth/login', payload);
    return mapTokenResponse(response.data);
  },

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const payload: RegisterOrganizationRequestDTO = {
      organizationName: data.organizationName,
      slug: data.slug,
      adminName: data.adminName,
      adminEmail: data.adminEmail,
      password: data.password,
    };

    const response = await api.post<AuthResponseDTO>('/auth/register', payload);
    return mapTokenResponse(response.data);
  },

  async restoreSession(): Promise<AuthResponse> {
    const response = await api.post<AuthResponseDTO>('/auth/refresh');
    return mapTokenResponse(response.data);
  },

  async logout(): Promise<void> {
    await api.post('/auth/logout');
  },

  async getProfile(): Promise<UserProfile> {
    const response = await api.get<UserProfileResponseDTO>('/users/me');
    return mapUserProfile(response.data);
  },
};
