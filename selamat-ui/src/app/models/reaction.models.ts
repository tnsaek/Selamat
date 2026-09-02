import { IsoDateTime, UUID } from './common.models';

export type ReactionType = 'LIKE' | 'LOVE' | 'LAUGH' | 'SAD' | 'ANGRY';

export interface Reaction {
  id: UUID;
  userId: UUID;
  postId: UUID | null;
  commentId: UUID | null;
  reactionType: ReactionType;
  createdAt: IsoDateTime;
}

export interface ReactionRequest {
  reactionType: ReactionType;
}
