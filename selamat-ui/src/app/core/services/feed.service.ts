import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { FeedResponse } from '../../models';
import { ApiService } from './api.service';

export interface FeedQuery {
  cursor?: string | null;
  limit?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class FeedService {
  private readonly api = inject(ApiService);

  getFeed(query: FeedQuery = {}): Observable<FeedResponse> {
    return this.api.get<FeedResponse>('/api/feed', {
      cursor: query.cursor,
      limit: query.limit,
    });
  }
}
