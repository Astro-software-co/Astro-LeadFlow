export interface LoginRequestDTO {
  email: string;
  password: string;
}

export interface RegisterOrganizationRequestDTO {
  organizationName: string;
  slug: string;
  adminName: string;
  adminEmail: string;
  password: string;
}

export interface AuthResponseDTO {
  accessToken: string | null;
  refreshToken?: string | null;
  userId?: string | null;
  role?: string | null;
  organizationId?: string | null;
  pendingTwoFactor: boolean;
}

export interface RefreshTokenRequestDTO {
  refreshToken?: string;
}

export interface UserProfileResponseDTO {
  id: string;
  name: string;
  email: string;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  role: string;
  status: string;
  organizationId?: string | null;
  organizationName?: string | null;
  twoFactorEmailEnabled: boolean;
  twoFactorSmsEnabled: boolean;
  notifyNewLead: boolean;
  notifyLeadWaiting: boolean;
  notifySaleCompleted: boolean;
  notifyCardMoved: boolean;
  notifyViaEmail: boolean;
  notifyViaPush: boolean;
  createdAt: string;
}
