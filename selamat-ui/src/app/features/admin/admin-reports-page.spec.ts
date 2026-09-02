import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ModerationService, ReportService } from '../../core/services';
import { Report, ReportTarget, UserSummary } from '../../models';
import { AdminReportsPage } from './admin-reports-page';

describe('AdminReportsPage', () => {
  let fixture: ComponentFixture<AdminReportsPage>;
  let queryParamMap: BehaviorSubject<ParamMap>;
  let reportService: {
    listReports: ReturnType<typeof vi.fn>;
    getReportById: ReturnType<typeof vi.fn>;
    getReportTarget: ReturnType<typeof vi.fn>;
    resolveReport: ReturnType<typeof vi.fn>;
  };
  let moderationService: {
    hidePost: ReturnType<typeof vi.fn>;
    hideComment: ReturnType<typeof vi.fn>;
    deleteMessage: ReturnType<typeof vi.fn>;
    hideMedia: ReturnType<typeof vi.fn>;
    suspendUser: ReturnType<typeof vi.fn>;
  };

  const reporter = user('user-1', 'reporter');
  const author = user('user-2', 'author');
  const openReport = report('report-1', 'POST', 'post-1', 'OPEN');
  const rejectedReport = report('report-2', 'MESSAGE', 'message-1', 'REJECTED');
  const postTarget = target('report-1', 'POST', 'post-1', {
    author,
    status: 'PUBLISHED',
    visibility: 'PUBLIC',
    content: 'Reported post content',
    commentCount: 2,
    reactionCount: 3,
    media: [
      {
        id: 'media-1',
        url: 'https://example.com/post.png',
        mediaType: 'IMAGE',
        mimeType: 'image/png',
        sizeBytes: 2048,
        altText: 'Post image',
      },
    ],
  });

  async function configureReportsPage(reportId: string | null = null): Promise<void> {
    queryParamMap = new BehaviorSubject(convertToParamMap(reportId ? { reportId } : {}));
    reportService = {
      listReports: vi.fn(() => of({ items: [openReport], nextCursor: 'cursor-2' })),
      getReportById: vi.fn(() => of(rejectedReport)),
      getReportTarget: vi.fn(() => of(postTarget)),
      resolveReport: vi.fn(() => of({ ...openReport, status: 'RESOLVED', resolutionNote: 'Handled' })),
    };
    moderationService = {
      hidePost: vi.fn(() => of(undefined)),
      hideComment: vi.fn(() => of(undefined)),
      deleteMessage: vi.fn(() => of(undefined)),
      hideMedia: vi.fn(() => of(undefined)),
      suspendUser: vi.fn(() => of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [AdminReportsPage],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap } },
        { provide: ReportService, useValue: reportService },
        { provide: ModerationService, useValue: moderationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReportsPage);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    TestBed.resetTestingModule();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('loads reports on init', async () => {
    await configureReportsPage();

    expect(reportService.listReports).toHaveBeenCalledWith({ status: null, limit: 20 });
    expect(fixture.componentInstance.reports()).toEqual([openReport]);
    expect(fixture.componentInstance.nextCursor()).toBe('cursor-2');
    expect(fixture.nativeElement.textContent).toContain('Reports');
  });

  it('loads more reports when a cursor exists', async () => {
    await configureReportsPage();
    reportService.listReports.mockReturnValueOnce(of({ items: [rejectedReport], nextCursor: null }));

    fixture.componentInstance.loadMore();

    expect(reportService.listReports).toHaveBeenLastCalledWith({ status: null, cursor: 'cursor-2', limit: 20 });
    expect(fixture.componentInstance.reports()).toEqual([openReport, rejectedReport]);
    expect(fixture.componentInstance.nextCursor()).toBeNull();
  });

  it('reloads reports when status changes', async () => {
    await configureReportsPage();

    fixture.componentInstance.changeStatus({ target: { value: 'OPEN' } } as unknown as Event);

    expect(fixture.componentInstance.selectedStatus()).toBe('OPEN');
    expect(reportService.listReports).toHaveBeenLastCalledWith({ status: 'OPEN', limit: 20 });
  });

  it('loads a target report from the query string when missing from the first page', async () => {
    await configureReportsPage('report-2');

    expect(reportService.getReportById).toHaveBeenCalledWith('report-2');
    expect(fixture.componentInstance.reports()[0]).toEqual(rejectedReport);
    expect(fixture.componentInstance.isTargetReport(rejectedReport)).toBe(true);
  });

  it('loads a reported target preview', async () => {
    await configureReportsPage();

    fixture.componentInstance.loadReportTarget(openReport);

    expect(reportService.getReportTarget).toHaveBeenCalledWith(openReport.id);
    expect(fixture.componentInstance.targetByReportId()[openReport.id]).toEqual(postTarget);
  });

  it('resolves reports with a trimmed note', async () => {
    await configureReportsPage();

    fixture.componentInstance.updateResolutionNote(openReport.id, { target: { value: '  Handled  ' } } as unknown as Event);
    fixture.componentInstance.resolveReport(openReport, 'RESOLVED');

    expect(reportService.resolveReport).toHaveBeenCalledWith(openReport.id, {
      status: 'RESOLVED',
      resolutionNote: 'Handled',
    });
    expect(fixture.componentInstance.reports()[0].status).toBe('RESOLVED');
    expect(fixture.componentInstance.resolutionNoteByReportId()[openReport.id]).toBe('');
  });

  it('does not resolve final reports', async () => {
    await configureReportsPage();

    fixture.componentInstance.resolveReport(rejectedReport, 'RESOLVED');

    expect(reportService.resolveReport).not.toHaveBeenCalled();
  });

  it('applies moderation actions for supported targets', async () => {
    await configureReportsPage();

    fixture.componentInstance.moderateTarget(openReport, postTarget);

    expect(window.confirm).toHaveBeenCalledWith('Hide post?');
    expect(moderationService.hidePost).toHaveBeenCalledWith('post-1');
    expect(reportService.getReportTarget).toHaveBeenCalledWith(openReport.id);
    expect(fixture.componentInstance.successMessage()).toBe('Post hidden.');
  });

  it('does not moderate when confirmation is cancelled', async () => {
    await configureReportsPage();
    vi.mocked(window.confirm).mockReturnValue(false);

    fixture.componentInstance.moderateTarget(openReport, postTarget);

    expect(moderationService.hidePost).not.toHaveBeenCalled();
  });

  it('does not moderate unavailable targets', async () => {
    await configureReportsPage();
    const hiddenPost = target('report-1', 'POST', 'post-1', { status: 'HIDDEN' });
    const mediaTarget = target('report-1', 'MEDIA', 'media-1', {
      status: 'HIDDEN',
      mediaType: 'IMAGE',
      mimeType: 'image/png',
      sizeBytes: null,
    });

    fixture.componentInstance.moderateTarget(openReport, hiddenPost);
    fixture.componentInstance.moderateTarget(openReport, mediaTarget);

    expect(moderationService.hidePost).not.toHaveBeenCalled();
    expect(fixture.componentInstance.canModerateTarget(mediaTarget)).toBe(false);
    expect(fixture.componentInstance.moderationActionLabel(mediaTarget)).toBe('Hide media');
  });

  it('maps moderation labels, eligibility, and success messages by target type', async () => {
    await configureReportsPage();
    const commentTarget = target('report-1', 'COMMENT', 'comment-1', { status: 'VISIBLE' });
    const messageTarget = target('report-1', 'MESSAGE', 'message-1', { status: 'SENT' });
    const userTarget = target('report-1', 'USER', 'user-2', { user: author, userStatus: 'ACTIVE' });
    const mediaTarget = target('report-1', 'MEDIA', 'media-1', { status: 'VISIBLE' });

    expect(fixture.componentInstance.moderationActionLabel(commentTarget)).toBe('Hide comment');
    expect(fixture.componentInstance.moderationActionLabel(messageTarget)).toBe('Delete message');
    expect(fixture.componentInstance.moderationActionLabel(userTarget)).toBe('Suspend user');
    expect(fixture.componentInstance.moderationActionLabel(mediaTarget)).toBe('Hide media');
    expect(fixture.componentInstance.canModerateTarget(target('report-1', 'COMMENT', 'comment-1', { status: 'DELETED' }))).toBe(false);
    expect(fixture.componentInstance.canModerateTarget(target('report-1', 'MESSAGE', 'message-1', { status: 'DELETED' }))).toBe(false);
    expect(fixture.componentInstance.canModerateTarget(target('report-1', 'USER', 'user-2', { userStatus: 'SUSPENDED' }))).toBe(false);
    expect(fixture.componentInstance.canModerateTarget(target('report-1', 'MEDIA', 'media-1', { status: 'DELETED' }))).toBe(false);

    fixture.componentInstance.moderateTarget(openReport, commentTarget);
    fixture.componentInstance.moderateTarget(openReport, messageTarget);
    fixture.componentInstance.moderateTarget(openReport, userTarget);
    fixture.componentInstance.moderateTarget(openReport, mediaTarget);

    expect(moderationService.hideComment).toHaveBeenCalledWith('comment-1');
    expect(moderationService.deleteMessage).toHaveBeenCalledWith('message-1');
    expect(moderationService.suspendUser).toHaveBeenCalledWith('user-2');
    expect(moderationService.hideMedia).toHaveBeenCalledWith('media-1');
  });

  it('formats display helpers', async () => {
    await configureReportsPage();

    expect(fixture.componentInstance.userLabel({ ...author, displayName: 'Author Name' })).toBe('Author Name (@author)');
    expect(fixture.componentInstance.userLabel({ ...author, displayName: null })).toBe('@author');
    expect(fixture.componentInstance.formatBytes(null)).toBe('Unknown size');
    expect(fixture.componentInstance.formatBytes(512)).toBe('512 B');
    expect(fixture.componentInstance.formatBytes(2048)).toBe('2.0 KB');
    expect(fixture.componentInstance.formatBytes(2 * 1024 * 1024)).toBe('2.0 MB');
    expect(fixture.componentInstance.isFinalStatus('RESOLVED')).toBe(true);
    expect(fixture.componentInstance.isFinalStatus('OPEN')).toBe(false);
  });

  it('shows backend error messages', async () => {
    await configureReportsPage();
    reportService.listReports.mockReturnValueOnce(
      throwError(() => ({ error: { message: 'Reports unavailable.' } })),
    );

    fixture.componentInstance.loadReports();

    expect(fixture.componentInstance.errorMessage()).toBe('Reports unavailable.');
  });

  it('renders empty and selected-report loading states', async () => {
    reportService = {
      listReports: vi.fn(() => of({ items: [], nextCursor: null })),
      getReportById: vi.fn(() => of(rejectedReport)),
      getReportTarget: vi.fn(() => of(postTarget)),
      resolveReport: vi.fn(() => of({ ...openReport, status: 'RESOLVED', resolutionNote: 'Handled' })),
    };
    moderationService = {
      hidePost: vi.fn(() => of(undefined)),
      hideComment: vi.fn(() => of(undefined)),
      deleteMessage: vi.fn(() => of(undefined)),
      hideMedia: vi.fn(() => of(undefined)),
      suspendUser: vi.fn(() => of(undefined)),
    };
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    await TestBed.configureTestingModule({
      imports: [AdminReportsPage],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap } },
        { provide: ReportService, useValue: reportService },
        { provide: ModerationService, useValue: moderationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReportsPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No reports found');

    fixture.componentInstance.isLoadingTargetReport.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Loading selected report...');
  });

  it('renders resolved reports and all target preview variants', async () => {
    const resolvedReport = {
      ...openReport,
      status: 'RESOLVED' as const,
      resolver: author,
      resolvedAt: '2026-07-25T11:00:00Z',
      resolutionNote: 'Handled by moderation.',
    };
    await configureReportsPage();
    fixture.componentInstance.reports.set([resolvedReport]);

    const targets = [
      postTarget,
      target('report-1', 'COMMENT', 'comment-1', {
        author,
        postId: 'post-1',
        status: 'VISIBLE',
        content: 'Reported comment content',
      }),
      target('report-1', 'MESSAGE', 'message-1', {
        sender: author,
        recipient: reporter,
        status: 'SENT',
        content: 'Reported message content',
      }),
      target('report-1', 'USER', 'user-2', {
        user: author,
        email: 'author@example.com',
        userStatus: 'ACTIVE',
        bio: 'Reported profile bio',
      }),
      target('report-1', 'MEDIA', 'media-1', {
        uploader: author,
        postId: 'post-1',
        mediaType: 'IMAGE',
        mimeType: 'image/png',
        sizeBytes: 1024,
        url: 'https://example.com/media.png',
        altText: 'Reported media',
      }),
    ];

    for (const preview of targets) {
      fixture.componentInstance.targetByReportId.set({ [resolvedReport.id]: preview });
      fixture.detectChanges();
    }

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Resolver');
    expect(text).toContain('Resolution note');
    expect(text).toContain('Handled by moderation.');
    expect(text).toContain('Reported media');
  });

  it('handles loading, resolving, target, moderation, and target-report errors', async () => {
    await configureReportsPage();

    fixture.componentInstance.nextCursor.set(null);
    fixture.componentInstance.loadMore();
    fixture.componentInstance.nextCursor.set('cursor-2');
    fixture.componentInstance.isLoadingMore.set(true);
    fixture.componentInstance.loadMore();
    expect(reportService.listReports).toHaveBeenCalledTimes(1);

    fixture.componentInstance.isLoadingMore.set(false);
    reportService.listReports.mockReturnValueOnce(throwError(() => ({})));
    fixture.componentInstance.loadMore();
    expect(fixture.componentInstance.errorMessage()).toBe('Unable to load reports. Try again.');

    fixture.componentInstance.loadingTargetIds.set({ [openReport.id]: true });
    fixture.componentInstance.loadReportTarget(openReport);
    expect(reportService.getReportTarget).not.toHaveBeenCalledWith(openReport.id);

    fixture.componentInstance.loadingTargetIds.set({});
    reportService.getReportTarget.mockReturnValueOnce(throwError(() => ({ error: { message: 'Target unavailable.' } })));
    fixture.componentInstance.loadReportTarget(openReport);
    expect(fixture.componentInstance.errorMessage()).toBe('Target unavailable.');

    reportService.resolveReport.mockReturnValueOnce(throwError(() => ({ error: { message: 'Resolve failed.' } })));
    fixture.componentInstance.resolveReport(openReport, 'REJECTED');
    expect(fixture.componentInstance.errorMessage()).toBe('Resolve failed.');

    moderationService.hidePost.mockReturnValueOnce(throwError(() => ({ error: { message: 'Moderation failed.' } })));
    fixture.componentInstance.moderateTarget(openReport, postTarget);
    expect(fixture.componentInstance.errorMessage()).toBe('Moderation failed.');

    reportService.getReportById.mockReturnValueOnce(throwError(() => ({ error: { message: 'Report missing.' } })));
    queryParamMap.next(convertToParamMap({ reportId: 'missing-report' }));
    expect(fixture.componentInstance.errorMessage()).toBe('Report missing.');
  });
});

function user(id: string, username: string): UserSummary {
  return {
    id,
    username,
    displayName: username,
    avatarUrl: null,
  };
}

function report(id: string, targetType: Report['targetType'], targetId: string, status: Report['status']): Report {
  return {
    id,
    reporter: user('user-1', 'reporter'),
    resolver: null,
    targetType,
    targetId,
    reason: 'Spam',
    details: 'Reported details',
    resolutionNote: null,
    status,
    createdAt: '2026-07-25T10:00:00Z',
    resolvedAt: null,
  };
}

function target(
  reportId: string,
  targetType: ReportTarget['targetType'],
  targetId: string,
  patch: Partial<ReportTarget> = {},
): ReportTarget {
  return {
    reportId,
    targetType,
    targetId,
    user: null,
    email: null,
    userStatus: null,
    bio: null,
    author: null,
    sender: null,
    recipient: null,
    uploader: null,
    postId: null,
    content: null,
    status: null,
    visibility: null,
    commentCount: null,
    reactionCount: null,
    url: null,
    mediaType: null,
    mimeType: null,
    sizeBytes: null,
    altText: null,
    createdAt: '2026-07-25T10:00:00Z',
    media: [],
    ...patch,
  };
}
