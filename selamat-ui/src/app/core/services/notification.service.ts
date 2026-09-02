import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { Notification, NotificationCount, PageResponse, UUID } from '../../models';
import { ApiService } from './api.service';

export interface NotificationQuery {
  unreadOnly?: boolean | null;
  cursor?: string | null;
  limit?: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly api = inject(ApiService);
  private readonly unreadCountSignal = signal(0);

  readonly unreadCount = this.unreadCountSignal.asReadonly();

  listNotifications(query: NotificationQuery = {}): Observable<PageResponse<Notification>> {
    return this.api.get<PageResponse<Notification>>('/api/notifications', {
      unreadOnly: query.unreadOnly,
      cursor: query.cursor,
      limit: query.limit,
    });
  }

  markAsRead(notificationId: UUID): Observable<Notification> {
    return this.api.patch<Notification, Record<string, never>>(`/api/notifications/${notificationId}/read`, {}).pipe(
      tap((notification) => {
        if (notification.read) {
          this.unreadCountSignal.update((count) => Math.max(0, count - 1));
        }
      }),
    );
  }

  markAllAsRead(): Observable<NotificationCount> {
    return this.api.patch<NotificationCount, Record<string, never>>('/api/notifications/read-all', {}).pipe(
      tap((response) => this.unreadCountSignal.set(response.unreadCount)),
    );
  }

  loadUnreadCount(): Observable<NotificationCount> {
    return this.api
      .get<NotificationCount>('/api/notifications/unread-count')
      .pipe(tap((response) => this.unreadCountSignal.set(response.unreadCount)));
  }
}
