import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CreatePostRequest, Post, UpdatePostRequest, UUID } from '../../models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class PostService {
  private readonly api = inject(ApiService);

  createPost(request: CreatePostRequest): Observable<Post> {
    return this.api.post<Post, CreatePostRequest>('/api/posts', request);
  }

  getPostById(postId: UUID): Observable<Post> {
    return this.api.get<Post>(`/api/posts/${postId}`);
  }

  updatePost(postId: UUID, request: UpdatePostRequest): Observable<Post> {
    return this.api.patch<Post, UpdatePostRequest>(`/api/posts/${postId}`, request);
  }

  deletePost(postId: UUID): Observable<void> {
    return this.api.delete<void>(`/api/posts/${postId}`);
  }
}
