import { IsoDateTime, UUID } from './common.models';
import { UserSummary } from './user.models';

export type NotificationType =
  | 'FOLLOW'
  | 'COMMENT'
  | 'REACTION'
  | 'MESSAGE'
  | 'REPORT_UPDATE'
  | 'SYSTEM';

export interface Notification {
  id: UUID;
  recipientId: UUID;
  actor: UserSummary;
  notificationType: NotificationType;
  title: string;
  body: string;
  targetType: string | null;
  targetId: UUID | null;
  read: boolean;
  createdAt: IsoDateTime;
  readAt: IsoDateTime | null;
}

export interface NotificationCount {
  unreadCount: number;
}
