import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { UUID } from '../../models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class ModerationService {
  private readonly api = inject(ApiService);

  hidePost(postId: UUID): Observable<void> {
    return this.api.patch<void, Record<string, never>>(`/api/admin/moderation/posts/${postId}/hide`, {});
  }

  hideComment(commentId: UUID): Observable<void> {
    return this.api.patch<void, Record<string, never>>(`/api/admin/moderation/comments/${commentId}/hide`, {});
  }

  deleteMessage(messageId: UUID): Observable<void> {
    return this.api.patch<void, Record<string, never>>(`/api/admin/moderation/messages/${messageId}/delete`, {});
  }

  hideMedia(mediaId: UUID): Observable<void> {
    return this.api.patch<void, Record<string, never>>(`/api/admin/moderation/media/${mediaId}/hide`, {});
  }

  suspendUser(userId: UUID): Observable<void> {
    return this.api.patch<void, Record<string, never>>(`/api/admin/moderation/users/${userId}/suspend`, {});
  }
}
