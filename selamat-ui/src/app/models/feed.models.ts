import { PageResponse } from './common.models';
import { Post } from './post.models';

export type FeedResponse = PageResponse<Post>;
