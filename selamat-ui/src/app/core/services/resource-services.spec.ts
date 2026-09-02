import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { vi } from 'vitest';

import { API_BASE_URL } from '../config';
import { ApiService } from './api.service';
import { CommentService } from './comment.service';
import { FeedService } from './feed.service';
import { FollowService } from './follow.service';
import { MediaService } from './media.service';
import { MessageService } from './message.service';
import { ModerationService } from './moderation.service';
import { NotificationService } from './notification.service';
import { PostService } from './post.service';
import { ProfileService } from './profile.service';
import { ReactionService } from './reaction.service';
import { ReportService } from './report.service';
import { UserService } from './user.service';

describe('resource services', () => {
  const response = { id: 'response-id' };
  let api: {
    get: ReturnType<typeof vi.fn>;
    post: ReturnType<typeof vi.fn>;
    put: ReturnType<typeof vi.fn>;
    patch: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };

  function configureApiService(): void {
    api = {
      get: vi.fn(() => of(response)),
      post: vi.fn(() => of(response)),
      put: vi.fn(() => of(response)),
      patch: vi.fn(() => of(response)),
      delete: vi.fn(() => of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: ApiService, useValue: api }],
    });
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
    configureApiService();
  });

  it('maps UserService methods to user endpoints', async () => {
    const service = TestBed.inject(UserService);

    await firstValueFrom(service.getCurrentUser());
    await firstValueFrom(service.getUserById('user-1'));
    await firstValueFrom(service.searchUsers('sel', 5));

    expect(api.get).toHaveBeenNthCalledWith(1, '/api/users/me');
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/users/user-1');
    expect(api.get).toHaveBeenNthCalledWith(3, '/api/users/search', { query: 'sel', limit: 5 });
  });

  it('maps ProfileService methods to profile endpoints', async () => {
    const service = TestBed.inject(ProfileService);
    const request = { displayName: 'Selam' };

    await firstValueFrom(service.getProfile('user-1'));
    await firstValueFrom(service.updateProfile('user-1', request));

    expect(api.get).toHaveBeenCalledWith('/api/users/user-1/profile');
    expect(api.patch).toHaveBeenCalledWith('/api/users/user-1/profile', request);
  });

  it('maps PostService methods to post endpoints', async () => {
    const service = TestBed.inject(PostService);
    const createRequest = { content: 'Hello', visibility: 'PUBLIC' as const, mediaIds: ['media-1'] };
    const updateRequest = { content: 'Updated' };

    await firstValueFrom(service.createPost(createRequest));
    await firstValueFrom(service.getPostById('post-1'));
    await firstValueFrom(service.updatePost('post-1', updateRequest));
    await firstValueFrom(service.deletePost('post-1'));

    expect(api.post).toHaveBeenCalledWith('/api/posts', createRequest);
    expect(api.get).toHaveBeenCalledWith('/api/posts/post-1');
    expect(api.patch).toHaveBeenCalledWith('/api/posts/post-1', updateRequest);
    expect(api.delete).toHaveBeenCalledWith('/api/posts/post-1');
  });

  it('maps FeedService method to feed endpoint with cursor pagination', async () => {
    const service = TestBed.inject(FeedService);

    await firstValueFrom(service.getFeed({ cursor: 'cursor-1', limit: 20 }));

    expect(api.get).toHaveBeenCalledWith('/api/feed', { cursor: 'cursor-1', limit: 20 });
  });

  it('maps CommentService methods to comment endpoints', async () => {
    const service = TestBed.inject(CommentService);
    const request = { content: 'Nice post', parentCommentId: 'comment-1' };

    await firstValueFrom(service.listPostComments('post-1', { cursor: 'cursor-1', limit: 10 }));
    await firstValueFrom(service.createComment('post-1', request));

    expect(api.get).toHaveBeenCalledWith('/api/posts/post-1/comments', { cursor: 'cursor-1', limit: 10 });
    expect(api.post).toHaveBeenCalledWith('/api/posts/post-1/comments', request);
  });

  it('maps ReactionService methods to reaction endpoints', async () => {
    const service = TestBed.inject(ReactionService);
    const request = { reactionType: 'LIKE' as const };

    await firstValueFrom(service.reactToPost('post-1', request));
    await firstValueFrom(service.removePostReaction('post-1'));

    expect(api.put).toHaveBeenCalledWith('/api/posts/post-1/reactions', request);
    expect(api.delete).toHaveBeenCalledWith('/api/posts/post-1/reactions');
  });

  it('maps FollowService methods to follow endpoints', async () => {
    const service = TestBed.inject(FollowService);

    await firstValueFrom(service.followUser('user-1'));
    await firstValueFrom(service.getFollowStatus('user-1'));
    await firstValueFrom(service.unfollowUser('user-1'));

    expect(api.post).toHaveBeenCalledWith('/api/users/user-1/follow', {});
    expect(api.get).toHaveBeenCalledWith('/api/users/user-1/follow');
    expect(api.delete).toHaveBeenCalledWith('/api/users/user-1/follow');
  });

  it('maps MessageService methods to message endpoints', async () => {
    const service = TestBed.inject(MessageService);
    const request = { recipientId: 'user-2', content: 'Hello' };

    await firstValueFrom(service.listMessages({ cursor: 'cursor-1', limit: 25 }));
    await firstValueFrom(service.getMessageById('message-1'));
    await firstValueFrom(service.sendMessage(request));
    await firstValueFrom(service.markConversationAsRead('user-2'));

    expect(api.get).toHaveBeenNthCalledWith(1, '/api/messages', { cursor: 'cursor-1', limit: 25 });
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/messages/message-1');
    expect(api.post).toHaveBeenCalledWith('/api/messages', request);
    expect(api.patch).toHaveBeenCalledWith('/api/messages/conversations/user-2/read', {});
  });

  it('maps NotificationService methods and updates unread count', async () => {
    const service = TestBed.inject(NotificationService);
    api.patch.mockReturnValueOnce(of({ read: true })).mockReturnValueOnce(of({ unreadCount: 3 }));
    api.get.mockReturnValueOnce(of(response)).mockReturnValueOnce(of({ unreadCount: 4 }));

    await firstValueFrom(service.listNotifications({ unreadOnly: true, cursor: 'cursor-1', limit: 15 }));
    await firstValueFrom(service.loadUnreadCount());
    await firstValueFrom(service.markAsRead('notification-1'));
    await firstValueFrom(service.markAllAsRead());

    expect(api.get).toHaveBeenNthCalledWith(1, '/api/notifications', {
      unreadOnly: true,
      cursor: 'cursor-1',
      limit: 15,
    });
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/notifications/unread-count');
    expect(api.patch).toHaveBeenNthCalledWith(1, '/api/notifications/notification-1/read', {});
    expect(api.patch).toHaveBeenNthCalledWith(2, '/api/notifications/read-all', {});
    expect(service.unreadCount()).toBe(3);
  });

  it('maps ReportService methods to report endpoints', async () => {
    const service = TestBed.inject(ReportService);
    const createRequest = { targetType: 'POST' as const, targetId: 'post-1', reason: 'Spam' };
    const resolveRequest = { status: 'RESOLVED' as const, resolutionNote: 'Handled' };

    await firstValueFrom(service.createReport(createRequest));
    await firstValueFrom(service.getAdminSummary());
    await firstValueFrom(service.listReports({ status: 'OPEN', cursor: 'cursor-1', limit: 20 }));
    await firstValueFrom(service.getReportById('report-1'));
    await firstValueFrom(service.getReportTarget('report-1'));
    await firstValueFrom(service.resolveReport('report-1', resolveRequest));

    expect(api.post).toHaveBeenCalledWith('/api/reports', createRequest);
    expect(api.get).toHaveBeenNthCalledWith(1, '/api/admin/reports/summary');
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/admin/reports', {
      status: 'OPEN',
      cursor: 'cursor-1',
      limit: 20,
    });
    expect(api.get).toHaveBeenNthCalledWith(3, '/api/admin/reports/report-1');
    expect(api.get).toHaveBeenNthCalledWith(4, '/api/admin/reports/report-1/target');
    expect(api.patch).toHaveBeenCalledWith('/api/admin/reports/report-1/resolve', resolveRequest);
  });

  it('maps ModerationService methods to moderation endpoints', async () => {
    const service = TestBed.inject(ModerationService);

    await firstValueFrom(service.hidePost('post-1'));
    await firstValueFrom(service.hideComment('comment-1'));
    await firstValueFrom(service.deleteMessage('message-1'));
    await firstValueFrom(service.hideMedia('media-1'));
    await firstValueFrom(service.suspendUser('user-1'));

    expect(api.patch).toHaveBeenNthCalledWith(1, '/api/admin/moderation/posts/post-1/hide', {});
    expect(api.patch).toHaveBeenNthCalledWith(2, '/api/admin/moderation/comments/comment-1/hide', {});
    expect(api.patch).toHaveBeenNthCalledWith(3, '/api/admin/moderation/messages/message-1/delete', {});
    expect(api.patch).toHaveBeenNthCalledWith(4, '/api/admin/moderation/media/media-1/hide', {});
    expect(api.patch).toHaveBeenNthCalledWith(5, '/api/admin/moderation/users/user-1/suspend', {});
  });
});

describe('MediaService', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://localhost:8080' },
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('uploads media as multipart form data with trimmed alt text', async () => {
    const service = TestBed.inject(MediaService);
    const file = new File(['image'], 'post.png', { type: 'image/png' });
    const upload = firstValueFrom(service.uploadMedia({ file, altText: '  Post image  ' }));

    const request = httpTesting.expectOne('http://localhost:8080/api/media');
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBe(true);
    expect(request.request.body.get('file')).toBe(file);
    expect(request.request.body.get('altText')).toBe('Post image');

    request.flush({ id: 'media-1' });
    await upload;
  });

  it('does not send blank alt text', async () => {
    const service = TestBed.inject(MediaService);
    const file = new File(['image'], 'post.png', { type: 'image/png' });
    const upload = firstValueFrom(service.uploadMedia({ file, altText: '   ' }));

    const request = httpTesting.expectOne('http://localhost:8080/api/media');
    expect(request.request.body.has('altText')).toBe(false);

    request.flush({ id: 'media-1' });
    await upload;
  });
});
