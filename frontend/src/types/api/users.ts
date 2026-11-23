export type UserRoleDTO = 'ASTRO_ADMIN' | 'MANAGER' | 'SELLER';

export type UserStatusDTO = 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';

export interface UserResponseDTO {
  id: string;
  name: string;
  email: string;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  role: UserRoleDTO;
  referralCode?: string | null;
  status: UserStatusDTO;
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

export type UserProfileResponseDTO = UserResponseDTO;

export interface UserSummaryResponseDTO {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string | null;
  role: UserRoleDTO;
  status: UserStatusDTO;
}

export interface AssignableUserResponseDTO {
  id: string;
  name: string;
  avatarUrl?: string | null;
  role: UserRoleDTO;
}

export interface UserTeamListResponseDTO {
  content?: UserSummaryResponseDTO[];
  data?: UserSummaryResponseDTO[];
  items?: UserSummaryResponseDTO[];
  totalElements?: number;
  totalPages?: number;
  page?: number;
  size?: number;
}

export type UserTeamListDataDTO = UserSummaryResponseDTO[] | UserTeamListResponseDTO;

export interface ChangePasswordRequestDTO {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export interface UpdateNotificationsRequestDTO {
  notifyNewLead: boolean;
  notifyLeadWaiting: boolean;
  notifySaleCompleted: boolean;
  notifyCardMoved: boolean;
  notifyViaEmail: boolean;
  notifyViaPush: boolean;
}

export interface UpdateTwoFactorRequestDTO {
  emailEnabled: boolean;
  smsEnabled: boolean;
}

export interface UpdateUserProfileRequestDTO {
  name?: string;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
}

export interface InviteUserRequestDTO {
  name: string;
  email: string;
  role: 'SELLER';
  phoneNumber?: string | null;
}

export interface UpdateTeamUserRequestDTO {
  name: string;
  email: string;
  phoneNumber?: string | null;
}
