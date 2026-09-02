import { IsoDateTime, UUID } from './common.models';
import { UserSummary } from './user.models';

export type CommentStatus = 'VISIBLE' | 'HIDDEN' | 'DELETED';

export interface Comment {
  id: UUID;
  postId: UUID;
  parentCommentId: UUID | null;
  author: UserSummary;
  content: string;
  status: CommentStatus;
  reactionCount: number;
  createdAt: IsoDateTime;
}

export interface CreateCommentRequest {
  content: string;
  parentCommentId?: UUID | null;
}
