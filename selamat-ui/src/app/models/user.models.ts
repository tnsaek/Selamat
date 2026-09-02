import { IsoDateTime, UUID } from './common.models';

export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED';

export interface User {
  id: UUID;
  username: string;
  email: string;
  status: UserStatus;
  roles: string[];
  createdAt: IsoDateTime;
}

export interface UserSummary {
  id: UUID;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
}
