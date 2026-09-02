import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { User, UserSummary, UUID } from '../../models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly api = inject(ApiService);

  getCurrentUser(): Observable<User> {
    return this.api.get<User>('/api/users/me');
  }

  getUserById(userId: UUID): Observable<User> {
    return this.api.get<User>(`/api/users/${userId}`);
  }

  searchUsers(query: string, limit = 10): Observable<UserSummary[]> {
    return this.api.get<UserSummary[]>('/api/users/search', {
      query,
      limit,
    });
  }
}
