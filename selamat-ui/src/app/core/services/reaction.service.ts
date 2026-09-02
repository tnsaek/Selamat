import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Reaction, ReactionRequest, UUID } from '../../models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class ReactionService {
  private readonly api = inject(ApiService);

  reactToPost(postId: UUID, request: ReactionRequest): Observable<Reaction> {
    return this.api.put<Reaction, ReactionRequest>(`/api/posts/${postId}/reactions`, request);
  }

  removePostReaction(postId: UUID): Observable<void> {
    return this.api.delete<void>(`/api/posts/${postId}/reactions`);
  }
}
