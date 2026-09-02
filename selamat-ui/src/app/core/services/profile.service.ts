import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Profile, UpdateProfileRequest, UUID } from '../../models';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly api = inject(ApiService);

  getProfile(userId: UUID): Observable<Profile> {
    return this.api.get<Profile>(`/api/users/${userId}/profile`);
  }

  updateProfile(userId: UUID, request: UpdateProfileRequest): Observable<Profile> {
    return this.api.patch<Profile, UpdateProfileRequest>(`/api/users/${userId}/profile`, request);
  }
}
