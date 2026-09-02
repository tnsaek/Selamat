import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Follow, FollowStatusResponse, UUID } from '../../models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class FollowService {
  private readonly api = inject(ApiService);

  followUser(userId: UUID): Observable<Follow> {
    return this.api.post<Follow, Record<string, never>>(`/api/users/${userId}/follow`, {});
  }

  getFollowStatus(userId: UUID): Observable<FollowStatusResponse> {
    return this.api.get<FollowStatusResponse>(`/api/users/${userId}/follow`);
  }

  unfollowUser(userId: UUID): Observable<void> {
    return this.api.delete<void>(`/api/users/${userId}/follow`);
  }
}
