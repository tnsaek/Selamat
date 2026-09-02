import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Comment, CreateCommentRequest, PageResponse, UUID } from '../../models';
import { ApiService } from './api.service';

export interface CommentQuery {
  cursor?: string | null;
  limit?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class CommentService {
  private readonly api = inject(ApiService);

  listPostComments(postId: UUID, query: CommentQuery = {}): Observable<PageResponse<Comment>> {
    return this.api.get<PageResponse<Comment>>(`/api/posts/${postId}/comments`, {
      cursor: query.cursor,
      limit: query.limit,
    });
  }

  createComment(postId: UUID, request: CreateCommentRequest): Observable<Comment> {
    return this.api.post<Comment, CreateCommentRequest>(`/api/posts/${postId}/comments`, request);
  }
}
