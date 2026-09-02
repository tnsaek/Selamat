import { IsoDateTime, UUID } from './common.models';
import { Media } from './media.models';
import { ReactionType } from './reaction.models';
import { UserSummary } from './user.models';

export type PostVisibility = 'PUBLIC' | 'FOLLOWERS_ONLY' | 'PRIVATE';
export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'HIDDEN' | 'DELETED';

export interface Post {
  id: UUID;
  author: UserSummary;
  content: string | null;
  visibility: PostVisibility;
  status: PostStatus;
  media: Media[];
  commentCount: number;
  reactionCount: number;
  viewerReaction: ReactionType | null;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
}

export interface CreatePostRequest {
  content?: string | null;
  visibility: PostVisibility;
  mediaIds?: UUID[];
}

export interface UpdatePostRequest {
  content?: string | null;
  visibility?: PostVisibility | null;
}
