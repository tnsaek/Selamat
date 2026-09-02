import { IsoDateTime, UUID } from './common.models';

export type FollowStatus = 'PENDING' | 'ACCEPTED' | 'BLOCKED';

export interface Follow {
  id: UUID;
  followerId: UUID;
  followedId: UUID;
  status: FollowStatus;
  createdAt: IsoDateTime;
}

export interface FollowStatusResponse {
  following: boolean;
}
