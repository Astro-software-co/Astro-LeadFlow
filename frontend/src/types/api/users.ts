export type UserRoleDTO = 'SUPER_ADMIN' | 'ADMIN' | 'MANAGER' | 'SELLER' | 'VENDEDOR';

export type UserStatusDTO = 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';

export interface UserResponseDTO {
  id: string;
  name: string;
  email: string;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  role: UserRoleDTO;
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

export interface InviteUserRequestDTO {
  name: string;
  email: string;
  role: Exclude<UserRoleDTO, 'SUPER_ADMIN'>;
  phoneNumber?: string | null;
}
