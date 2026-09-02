import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { NotificationService } from '../../core/services';
import { Notification } from '../../models';
import { NotificationsPage } from './notifications-page';

describe('NotificationsPage', () => {
  let fixture: ComponentFixture<NotificationsPage>;
  let notificationService: {
    loadUnreadCount: ReturnType<typeof vi.fn>;
    listNotifications: ReturnType<typeof vi.fn>;
    markAsRead: ReturnType<typeof vi.fn>;
    markAllAsRead: ReturnType<typeof vi.fn>;
  };

  const messageNotification = createNotification('notification-1', 'MESSAGE', 'message-1', false);
  const postNotification = createNotification('notification-2', 'POST', 'post-1', true);

  beforeEach(async () => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();

    notificationService = {
      loadUnreadCount: vi.fn(() => of({ unreadCount: 1 })),
      listNotifications: vi.fn(() => of({ items: [messageNotification, postNotification], nextCursor: 'cursor-2' })),
      markAsRead: vi.fn((notificationId: string) =>
        of({
          ...messageNotification,
          id: notificationId,
          read: true,
          readAt: '2026-07-25T10:05:00Z',
        }),
      ),
      markAllAsRead: vi.fn(() => of({ unreadCount: 0 })),
    };

    await TestBed.configureTestingModule({
      imports: [NotificationsPage],
      providers: [provideRouter([]), { provide: NotificationService, useValue: notificationService }],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationsPage);
    fixture.detectChanges();
  });

  it('loads unread count and notifications on init', () => {
    expect(notificationService.loadUnreadCount).toHaveBeenCalledOnce();
    expect(notificationService.listNotifications).toHaveBeenCalledWith({ unreadOnly: false, limit: 20 });
    expect(fixture.componentInstance.notifications()).toEqual([messageNotification, postNotification]);
    expect(fixture.componentInstance.nextCursor()).toBe('cursor-2');
    expect(fixture.nativeElement.textContent).toContain(messageNotification.title);
  });

  it('loads more notifications when next cursor exists', () => {
    const extraNotification = createNotification('notification-3', 'USER', 'user-2', false);
    notificationService.listNotifications.mockReturnValueOnce(of({ items: [extraNotification], nextCursor: null }));

    fixture.componentInstance.loadMore();

    expect(notificationService.listNotifications).toHaveBeenLastCalledWith({
      unreadOnly: false,
      cursor: 'cursor-2',
      limit: 20,
    });
    expect(fixture.componentInstance.notifications()).toEqual([
      messageNotification,
      postNotification,
      extraNotification,
    ]);
    expect(fixture.componentInstance.nextCursor()).toBeNull();
  });

  it('reloads notifications when unread-only filter changes', () => {
    fixture.componentInstance.toggleUnreadOnly({ target: { checked: true } } as unknown as Event);

    expect(fixture.componentInstance.unreadOnly()).toBe(true);
    expect(notificationService.listNotifications).toHaveBeenLastCalledWith({ unreadOnly: true, limit: 20 });
  });

  it('marks one notification as read', () => {
    fixture.componentInstance.markAsRead(messageNotification);

    expect(notificationService.markAsRead).toHaveBeenCalledWith(messageNotification.id);
    expect(fixture.componentInstance.notifications()[0].read).toBe(true);
    expect(fixture.componentInstance.markingReadIds()[messageNotification.id]).toBeUndefined();
  });

  it('removes a notification from the unread-only list after marking it read', () => {
    fixture.componentInstance.unreadOnly.set(true);
    fixture.componentInstance.notifications.set([messageNotification]);

    fixture.componentInstance.markAsRead(messageNotification);

    expect(fixture.componentInstance.notifications()).toEqual([]);
  });

  it('does not mark an already-read notification again', () => {
    fixture.componentInstance.markAsRead(postNotification);

    expect(notificationService.markAsRead).not.toHaveBeenCalled();
  });

  it('marks all notifications as read', () => {
    fixture.componentInstance.markAllAsRead();

    expect(notificationService.markAllAsRead).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.notifications().every((notification) => notification.read)).toBe(true);
    expect(fixture.componentInstance.isMarkingAllRead()).toBe(false);
  });

  it('shows load errors', () => {
    notificationService.listNotifications.mockReturnValueOnce(
      throwError(() => ({ error: { message: 'Notifications unavailable.' } })),
    );

    fixture.componentInstance.loadNotifications();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Notifications unavailable.');
    expect(fixture.componentInstance.isLoading()).toBe(false);
  });

  it('marks unread notification as read before opening its target', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.openNotification(messageNotification);

    expect(notificationService.markAsRead).toHaveBeenCalledWith(messageNotification.id);
    expect(navigate).toHaveBeenCalledWith(['/messages'], {
      queryParams: { messageId: messageNotification.targetId },
    });
  });

  it('opens already-read post notifications directly', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.componentInstance.openNotification(postNotification);

    expect(notificationService.markAsRead).not.toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/feed'], {
      queryParams: { postId: postNotification.targetId },
    });
  });

  it('returns action labels by target type', () => {
    expect(fixture.componentInstance.notificationActionLabel(createNotification('n1', 'MESSAGE', 'm1', false))).toBe(
      'Open messages',
    );
    expect(fixture.componentInstance.notificationActionLabel(createNotification('n2', 'POST', 'p1', false))).toBe(
      'Open post in feed',
    );
    expect(fixture.componentInstance.notificationActionLabel(createNotification('n3', 'USER', 'u1', false))).toBe(
      'Open profile',
    );
    expect(fixture.componentInstance.notificationActionLabel(createNotification('n4', 'REPORT', 'r1', false))).toBe(
      'Open reports',
    );
    expect(fixture.componentInstance.notificationActionLabel(createNotification('n5', null, null, false))).toBe(
      'Open notification',
    );
  });

  it('handles unread count, load more, mark read, and mark all errors', async () => {
    notificationService.loadUnreadCount.mockReturnValueOnce(throwError(() => new Error('ignored')));
    notificationService.listNotifications
      .mockReturnValueOnce(of({ items: [messageNotification], nextCursor: null }))
      .mockReturnValueOnce(throwError(() => ({ error: { message: 'Load more failed.' } })));

    fixture = TestBed.createComponent(NotificationsPage);
    fixture.detectChanges();

    fixture.componentInstance.loadMore();
    expect(notificationService.listNotifications).toHaveBeenCalledTimes(2);

    fixture.componentInstance.nextCursor.set('cursor-3');
    fixture.componentInstance.isLoadingMore.set(true);
    fixture.componentInstance.loadMore();
    expect(notificationService.listNotifications).toHaveBeenCalledTimes(2);

    fixture.componentInstance.isLoadingMore.set(false);
    fixture.componentInstance.loadMore();
    expect(fixture.componentInstance.errorMessage()).toBe('Load more failed.');
    expect(fixture.componentInstance.isLoadingMore()).toBe(false);

    notificationService.markAsRead.mockReturnValueOnce(throwError(() => ({ error: { message: 'Mark failed.' } })));
    fixture.componentInstance.markAsRead(messageNotification);
    expect(fixture.componentInstance.errorMessage()).toBe('Mark failed.');
    expect(fixture.componentInstance.markingReadIds()[messageNotification.id]).toBeUndefined();

    notificationService.markAllAsRead.mockReturnValueOnce(throwError(() => ({})));
    fixture.componentInstance.markAllAsRead();
    expect(fixture.componentInstance.errorMessage()).toBe('Unable to load notifications. Try again.');
    expect(fixture.componentInstance.isMarkingAllRead()).toBe(false);
  });

  it('uses mark all and open guards', () => {
    fixture.componentInstance.notifications.set([{ ...postNotification, read: true }]);
    fixture.componentInstance.markAllAsRead();
    expect(notificationService.markAllAsRead).not.toHaveBeenCalled();

    fixture.componentInstance.isMarkingAllRead.set(true);
    fixture.componentInstance.notifications.set([messageNotification]);
    fixture.componentInstance.markAllAsRead();
    expect(notificationService.markAllAsRead).not.toHaveBeenCalled();

    fixture.componentInstance.markingReadIds.set({ [messageNotification.id]: true });
    fixture.componentInstance.openNotification(messageNotification);
    expect(notificationService.markAsRead).not.toHaveBeenCalled();
  });

  it('opens user and report notifications and ignores missing targets', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const userNotification = createNotification('notification-3', 'USER', 'user-2', true);
    const reportNotification = createNotification('notification-4', 'REPORT', 'report-1', true);
    const noTargetNotification = createNotification('notification-5', 'POST', null, true);

    fixture.componentInstance.openNotification(userNotification);
    fixture.componentInstance.openNotification(reportNotification);
    fixture.componentInstance.openNotification(noTargetNotification);

    expect(navigate).toHaveBeenCalledWith(['/profile'], {
      queryParams: { userId: userNotification.targetId },
    });
    expect(navigate).toHaveBeenCalledWith(['/admin/reports'], {
      queryParams: { reportId: reportNotification.targetId },
    });
    expect(navigate).toHaveBeenCalledTimes(2);
  });

  it('shows loading and empty states', () => {
    fixture.componentInstance.isLoading.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading notifications...');

    fixture.componentInstance.isLoading.set(false);
    fixture.componentInstance.notifications.set([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No notifications found.');
  });
});

function createNotification(
  id: string,
  targetType: string | null,
  targetId: string | null,
  read: boolean,
): Notification {
  return {
    id,
    recipientId: 'user-1',
    actor: {
      id: 'user-2',
      username: 'mira',
      displayName: 'Mira Alem',
      avatarUrl: null,
    },
    notificationType: targetType === 'MESSAGE' ? 'MESSAGE' : 'COMMENT',
    title: `${targetType ?? 'System'} notification`,
    body: 'Notification body',
    targetType,
    targetId,
    read,
    createdAt: '2026-07-25T10:00:00Z',
    readAt: read ? '2026-07-25T10:01:00Z' : null,
  };
}
