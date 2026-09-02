import { IsoDateTime, UUID } from './common.models';
import { UserSummary } from './user.models';

export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ' | 'DELETED';

export interface Message {
  id: UUID;
  sender: UserSummary;
  recipient: UserSummary;
  content: string;
  status: MessageStatus;
  sentAt: IsoDateTime;
  deliveredAt: IsoDateTime | null;
  readAt: IsoDateTime | null;
}

export interface SendMessageRequest {
  recipientId: UUID;
  content: string;
}
