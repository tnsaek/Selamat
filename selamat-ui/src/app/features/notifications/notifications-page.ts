import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Notification, UUID } from '../../models';
import { NotificationService } from '../../core/services';

@Component({
  selector: 'app-notifications-page',
  imports: [DatePipe],
  templateUrl: './notifications-page.html',
  styleUrl: './notifications-page.scss',
})
export class NotificationsPage implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly notifications = signal<Notification[]>([]);
  readonly nextCursor = signal<string | null>(null);
  readonly unreadOnly = signal(false);
  readonly isLoading = signal(false);
  readonly isLoadingMore = signal(false);
  readonly isMarkingAllRead = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly markingReadIds = signal<Record<UUID, boolean>>({});

  ngOnInit(): void {
    this.notificationService.loadUnreadCount().subscribe({
      error: () => undefined,
    });
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.notificationService
      .listNotifications({
        unreadOnly: this.unreadOnly(),
        limit: 20,
      })
      .subscribe({
        next: (page) => {
          this.notifications.set(page.items);
          this.nextCursor.set(page.nextCursor);
          this.isLoading.set(false);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errorText(error));
          this.isLoading.set(false);
        },
      });
  }

  loadMore(): void {
    const cursor = this.nextCursor();
    if (!cursor || this.isLoadingMore()) {
      return;
    }

    this.isLoadingMore.set(true);
    this.errorMessage.set(null);

    this.notificationService
      .listNotifications({
        unreadOnly: this.unreadOnly(),
        cursor,
        limit: 20,
      })
      .subscribe({
        next: (page) => {
          this.notifications.update((items) => [...items, ...page.items]);
          this.nextCursor.set(page.nextCursor);
          this.isLoadingMore.set(false);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.errorText(error));
          this.isLoadingMore.set(false);
        },
      });
  }

  toggleUnreadOnly(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.unreadOnly.set(input.checked);
    this.loadNotifications();
  }

  markAsRead(notification: Notification): void {
    if (notification.read || this.markingReadIds()[notification.id]) {
      return;
    }

    this.markingReadIds.update((ids) => ({ ...ids, [notification.id]: true }));
    this.errorMessage.set(null);

    this.notificationService.markAsRead(notification.id).subscribe({
      next: (updatedNotification) => {
        this.applyReadNotification(updatedNotification);
        this.clearMarkingRead(notification.id);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.errorText(error));
        this.clearMarkingRead(notification.id);
      },
    });
  }

  markAllAsRead(): void {
    if (this.isMarkingAllRead() || this.notifications().every((notification) => notification.read)) {
      return;
    }

    this.isMarkingAllRead.set(true);
    this.errorMessage.set(null);

    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        const readAt = new Date().toISOString();
        this.notifications.update((items) =>
          items.map((notification) => ({
            ...notification,
            read: true,
            readAt: notification.readAt ?? readAt,
          })),
        );
        this.isMarkingAllRead.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.errorText(error));
        this.isMarkingAllRead.set(false);
      },
    });
  }

  openNotification(notification: Notification): void {
    if (this.markingReadIds()[notification.id]) {
      return;
    }

    if (notification.read) {
      this.navigateToNotificationTarget(notification);
      return;
    }

    this.markingReadIds.update((ids) => ({ ...ids, [notification.id]: true }));
    this.errorMessage.set(null);

    this.notificationService.markAsRead(notification.id).subscribe({
      next: (updatedNotification) => {
        this.applyReadNotification(updatedNotification);
        this.clearMarkingRead(notification.id);
        this.navigateToNotificationTarget(updatedNotification);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.errorText(error));
        this.clearMarkingRead(notification.id);
      },
    });
  }

  notificationActionLabel(notification: Notification): string {
    switch (notification.targetType) {
      case 'MESSAGE':
        return 'Open messages';
      case 'POST':
        return 'Open post in feed';
      case 'USER':
        return 'Open profile';
      case 'REPORT':
        return 'Open reports';
      default:
        return 'Open notification';
    }
  }

  trackNotification(_: number, notification: Notification): UUID {
    return notification.id;
  }

  private navigateToNotificationTarget(notification: Notification): void {
    if (!notification.targetId) {
      return;
    }

    switch (notification.targetType) {
      case 'MESSAGE':
        void this.router.navigate(['/messages'], {
          queryParams: { messageId: notification.targetId },
        });
        return;
      case 'POST':
        void this.router.navigate(['/feed'], {
          queryParams: { postId: notification.targetId },
        });
        return;
      case 'USER':
        void this.router.navigate(['/profile'], {
          queryParams: { userId: notification.targetId },
        });
        return;
      case 'REPORT':
        void this.router.navigate(['/admin/reports'], {
          queryParams: { reportId: notification.targetId },
        });
        return;
      default:
        return;
    }
  }

  private clearMarkingRead(notificationId: UUID): void {
    this.markingReadIds.update((ids) => {
      const { [notificationId]: _, ...rest } = ids;
      return rest;
    });
  }

  private applyReadNotification(updatedNotification: Notification): void {
    this.notifications.update((items) => {
      const updatedItems = items.map((item) => (item.id === updatedNotification.id ? updatedNotification : item));
      return this.unreadOnly() ? updatedItems.filter((item) => !item.read) : updatedItems;
    });
  }

  private errorText(error: unknown): string {
    if (this.hasErrorMessage(error)) {
      return error.error.message;
    }

    return 'Unable to load notifications. Try again.';
  }

  private hasErrorMessage(error: unknown): error is { error: { message: string } } {
    return (
      typeof error === 'object' &&
      error !== null &&
      'error' in error &&
      typeof (error as { error?: unknown }).error === 'object' &&
      (error as { error?: unknown }).error !== null &&
      'message' in ((error as { error?: unknown }).error as Record<string, unknown>) &&
      typeof ((error as { error: { message?: unknown } }).error.message) === 'string'
    );
  }
}
