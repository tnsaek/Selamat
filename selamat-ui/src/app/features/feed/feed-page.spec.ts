import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import {
  AuthService,
  CommentService,
  FeedService,
  MediaService,
  PostService,
  ReactionService,
  ReportService,
} from '../../core/services';
import { Comment, PageResponse, Post, User } from '../../models';
import { FeedPage } from './feed-page';

describe('FeedPage', () => {
  const currentUser = user();
  const currentUserSignal = signal<User | null>(currentUser);
  const authService = {
    currentUser: currentUserSignal.asReadonly(),
  };
  const feedService = {
    getFeed: vi.fn(),
  };
  const commentService = {
    listPostComments: vi.fn(),
    createComment: vi.fn(),
  };
  const postService = {
    createPost: vi.fn(),
    getPostById: vi.fn(),
    deletePost: vi.fn(),
  };
  const mediaService = {
    uploadMedia: vi.fn(),
  };
  const reactionService = {
    reactToPost: vi.fn(),
    removePostReaction: vi.fn(),
  };
  const reportService = {
    createReport: vi.fn(),
  };
  let queryParamMap: BehaviorSubject<ParamMap>;

  beforeEach(async () => {
    vi.clearAllMocks();
    currentUserSignal.set(currentUser);
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    feedService.getFeed.mockReturnValue(of(page<Post>([])));
    commentService.listPostComments.mockReturnValue(of(page<Comment>([])));
    commentService.createComment.mockReturnValue(of(commentResponse('comment-created', 'post-1', null)));
    postService.createPost.mockReturnValue(of(postResponse('created-post', currentUser.id)));
    postService.getPostById.mockReturnValue(of(postResponse('target-post', 'other-user')));
    postService.deletePost.mockReturnValue(of(undefined));
    mediaService.uploadMedia.mockReturnValue(of(mediaResponse('media-1', 'IMAGE')));
    reactionService.reactToPost.mockReturnValue(of(undefined));
    reactionService.removePostReaction.mockReturnValue(of(undefined));
    reportService.createReport.mockReturnValue(of({ id: 'report-1' }));
    await TestBed.configureTestingModule({
      imports: [FeedPage],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: FeedService, useValue: feedService },
        { provide: CommentService, useValue: commentService },
        { provide: PostService, useValue: postService },
        { provide: MediaService, useValue: mediaService },
        { provide: ReactionService, useValue: reactionService },
        { provide: ReportService, useValue: reportService },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap,
          },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('deletes an owned post after confirmation', () => {
    const post = postResponse('post-1', currentUser.id);
    feedService.getFeed.mockReturnValue(of(page([post])));
    postService.deletePost.mockReturnValue(of(undefined));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.commentsByPostId.set({
      [post.id]: [commentResponse('comment-1', post.id, null)],
    });
    component.deletePost(post);

    expect(postService.deletePost).toHaveBeenCalledWith(post.id);
    expect(component.posts()).toEqual([]);
    expect(component.commentsByPostId()[post.id]).toBeUndefined();
  });

  it('does not delete a post when confirmation is cancelled', () => {
    const post = postResponse('post-1', currentUser.id);
    feedService.getFeed.mockReturnValue(of(page([post])));
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.deletePost(post);

    expect(postService.deletePost).not.toHaveBeenCalled();
    expect(component.posts()).toEqual([post]);
  });

  it('does not delete another user post', () => {
    const post = postResponse('post-1', 'other-user');
    feedService.getFeed.mockReturnValue(of(page([post])));
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.deletePost(post);

    expect(postService.deletePost).not.toHaveBeenCalled();
    expect(component.posts()).toEqual([post]);
  });

  it('creates a reply with parentCommentId and nests it under the parent', () => {
    const post = postResponse('post-1', currentUser.id);
    const parent = commentResponse('comment-1', post.id, null);
    const reply = commentResponse('reply-1', post.id, parent.id);
    feedService.getFeed.mockReturnValue(of(page([post])));
    commentService.createComment.mockReturnValue(of(reply));
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.commentsByPostId.set({
      [post.id]: [parent],
    });
    component.replyInputByCommentId.set({
      [parent.id]: 'Thanks',
    });
    component.replyFormOpenByCommentId.set({
      [parent.id]: true,
    });
    component.createReply(post.id, parent);

    expect(commentService.createComment).toHaveBeenCalledWith(post.id, {
      content: 'Thanks',
      parentCommentId: parent.id,
    });
    expect(component.repliesForComment(post.id, parent.id).map((item) => item.id)).toEqual([reply.id]);
    expect(component.replyFormOpenByCommentId()[parent.id]).toBe(false);
  });

  it('restores reply text and shows an inline error when reply creation fails', () => {
    const post = postResponse('post-1', currentUser.id);
    const parent = commentResponse('comment-1', post.id, null);
    feedService.getFeed.mockReturnValue(of(page([post])));
    commentService.createComment.mockReturnValue(
      throwError(() => ({
        error: {
          message: 'Post not found.',
        },
      })),
    );
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.commentsByPostId.set({
      [post.id]: [parent],
    });
    component.replyInputByCommentId.set({
      [parent.id]: 'Thanks',
    });
    component.createReply(post.id, parent);

    expect(component.replyInputByCommentId()[parent.id]).toBe('Thanks');
    expect(component.replyErrorByCommentId()[parent.id]).toBe('Post not found.');
    expect(component.repliesForComment(post.id, parent.id)).toEqual([]);
  });

  it('rejects oversized media before creating a post', () => {
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;
    const oversizedFile = new File(['x'.repeat(10 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' });

    fixture.detectChanges();
    component.selectFiles(fileEvent([oversizedFile]));

    expect(component.selectedFiles()).toEqual([]);
    expect(component.composerError()).toBe('Each media file must not exceed 10 MB.');
    expect(mediaService.uploadMedia).not.toHaveBeenCalled();
  });

  it('loads the feed, renders posts with media, and appends more results', () => {
    const firstPost = {
      ...postResponse('post-1', 'other-user'),
      author: {
        id: 'other-user',
        username: 'mira',
        displayName: 'Mira Alem',
        avatarUrl: 'https://example.com/avatar.png',
      },
      media: [mediaResponse('image-1', 'IMAGE'), mediaResponse('doc-1', 'DOCUMENT')],
      viewerReaction: 'LIKE' as const,
      reactionCount: 2,
      commentCount: 1,
    };
    const nextPost = postResponse('post-2', currentUser.id);
    feedService.getFeed.mockReturnValueOnce(of({ items: [firstPost], nextCursor: 'cursor-1' })).mockReturnValueOnce(
      of({
        items: [nextPost],
        nextCursor: null,
      }),
    );
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.loadMore();
    fixture.detectChanges();

    expect(feedService.getFeed).toHaveBeenNthCalledWith(1, { limit: 10 });
    expect(feedService.getFeed).toHaveBeenNthCalledWith(2, { cursor: 'cursor-1', limit: 10 });
    expect(component.posts().map((post) => post.id)).toEqual(['post-1', 'post-2']);
    expect(fixture.nativeElement.textContent).toContain('Mira Alem');
    expect(fixture.nativeElement.textContent).toContain('Document attachment');
    expect(fixture.nativeElement.textContent).toContain('You reacted: LIKE');
  });

  it('shows feed and pagination errors', () => {
    feedService.getFeed.mockReturnValueOnce(
      throwError(() => ({
        status: 0,
      })),
    );
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Cannot reach the backend. Check that the API is running.');

    component.nextCursor.set('cursor-1');
    feedService.getFeed.mockReturnValueOnce(
      throwError(() => ({
        status: 500,
      })),
    );
    component.loadMore();

    expect(component.errorMessage()).toBe('Feed request failed with status 500.');
  });

  it('loads, refreshes, and paginates comments', () => {
    const post = postResponse('post-1', 'other-user');
    const first = commentResponse('comment-1', post.id, null);
    const duplicate = { ...first, content: 'Updated comment' };
    const second = commentResponse('comment-2', post.id, null);
    feedService.getFeed.mockReturnValue(of(page([post])));
    commentService.listPostComments
      .mockReturnValueOnce(of({ items: [first], nextCursor: 'comment-cursor' }))
      .mockReturnValueOnce(of({ items: [duplicate], nextCursor: 'comment-cursor' }))
      .mockReturnValueOnce(of({ items: [second], nextCursor: null }));
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.loadComments(post.id);
    component.loadComments(post.id);
    component.loadMoreComments(post.id);

    expect(commentService.listPostComments).toHaveBeenNthCalledWith(1, post.id, { limit: 5 });
    expect(commentService.listPostComments).toHaveBeenNthCalledWith(3, post.id, { cursor: 'comment-cursor', limit: 5 });
    expect(component.commentsByPostId()[post.id].map((comment) => comment.id)).toEqual(['comment-1', 'comment-2']);
    expect(component.commentsByPostId()[post.id].find((comment) => comment.id === first.id)?.content).toBe(
      'Updated comment',
    );
  });

  it('creates comments and restores input when creation fails', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-30T10:00:00Z'));
    const post = postResponse('post-1', 'other-user');
    const created = commentResponse('comment-1', post.id, null);
    feedService.getFeed.mockReturnValue(of(page([post])));
    commentService.createComment.mockReturnValueOnce(of(created)).mockReturnValueOnce(
      throwError(() => ({
        error: {
          message: 'Comment rejected.',
        },
      })),
    );
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.commentInputByPostId.set({ [post.id]: ' First comment ' });
    component.createComment(post.id);

    expect(commentService.createComment).toHaveBeenCalledWith(post.id, {
      content: 'First comment',
      parentCommentId: null,
    });
    expect(component.posts()[0].commentCount).toBe(1);

    component.commentInputByPostId.set({ [post.id]: ' Second comment ' });
    component.createComment(post.id);

    expect(component.commentInputByPostId()[post.id]).toBe('Second comment');
    expect(component.commentErrorByPostId()[post.id]).toBe('Comment rejected.');
  });

  it('does not create comments or replies without content or a current user', () => {
    const post = postResponse('post-1', 'other-user');
    const parent = commentResponse('comment-1', post.id, null);
    feedService.getFeed.mockReturnValue(of(page([post])));
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.createComment(post.id);
    component.createReply(post.id, parent);
    currentUserSignal.set(null);
    component.commentInputByPostId.set({ [post.id]: 'Comment' });
    component.replyInputByCommentId.set({ [parent.id]: 'Reply' });
    component.createComment(post.id);
    component.createReply(post.id, parent);

    expect(commentService.createComment).not.toHaveBeenCalled();
    expect(component.commentErrorByPostId()[post.id]).toBe('You must be logged in to comment.');
    expect(component.replyErrorByCommentId()[parent.id]).toBe('You must be logged in to comment.');
  });

  it('toggles reactions and reports reaction errors', () => {
    const post = postResponse('post-1', 'other-user');
    const likedPost = { ...post, viewerReaction: 'LIKE' as const, reactionCount: 1 };
    feedService.getFeed.mockReturnValue(of(page([post])));
    reactionService.removePostReaction.mockReturnValueOnce(of(undefined)).mockReturnValueOnce(
      throwError(() => ({
        error: {
          message: 'Reaction failed.',
        },
      })),
    );
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.toggleReaction(post);
    component.toggleReaction(likedPost);
    component.toggleReaction(likedPost);

    expect(reactionService.reactToPost).toHaveBeenCalledWith(post.id, { reactionType: 'LIKE' });
    expect(reactionService.removePostReaction).toHaveBeenCalledWith(likedPost.id);
    expect(component.errorMessage()).toBe('Reaction failed.');
  });

  it('creates posts with uploaded media and handles empty or failed submissions', () => {
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;
    const imageFile = new File(['image'], 'image.png', { type: 'image/png' });
    const documentFile = new File(['pdf'], 'file.pdf', { type: 'application/pdf' });
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:preview');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    mediaService.uploadMedia
      .mockReturnValueOnce(of(mediaResponse('media-1', 'IMAGE')))
      .mockReturnValueOnce(of(mediaResponse('media-2', 'DOCUMENT')));
    postService.createPost.mockReturnValueOnce(of(postResponse('created-post', currentUser.id))).mockReturnValueOnce(
      throwError(() => ({
        status: 400,
      })),
    );

    fixture.detectChanges();
    component.createPost();
    expect(component.composerError()).toBe('Add text or media before posting.');

    component.selectFiles(fileEvent([imageFile, documentFile]));
    component.postForm.setValue({ content: '  Media post  ', visibility: 'FOLLOWERS_ONLY' });
    component.createPost();

    expect(mediaService.uploadMedia).toHaveBeenCalledTimes(2);
    expect(postService.createPost).toHaveBeenCalledWith({
      content: 'Media post',
      visibility: 'FOLLOWERS_ONLY',
      mediaIds: ['media-1', 'media-2'],
    });
    expect(component.selectedFiles()).toEqual([]);

    component.postForm.setValue({ content: 'Broken post', visibility: 'PUBLIC' });
    component.createPost();

    expect(component.composerError()).toBe('Feed request failed with status 400.');
  });

  it('removes selected files and revokes image previews', () => {
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;
    const revokeObjectURL = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const file = new File(['image'], 'image.png', { type: 'image/png' });

    fixture.detectChanges();
    component.selectedFiles.set([{ file, previewUrl: 'blob:preview' }]);
    component.removeSelectedFile(0);

    expect(revokeObjectURL).toHaveBeenCalledWith('blob:preview');
    expect(component.selectedFiles()).toEqual([]);
  });

  it('submits post and comment reports and closes their forms', () => {
    const post = postResponse('post-1', 'other-user');
    const comment = commentResponse('comment-1', post.id, null, 'other-user');
    feedService.getFeed.mockReturnValue(of(page([post])));
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.toggleReportForm(post);
    component.updateReportReason(post.id, selectEvent('Spam'));
    component.updateReportDetails(post.id, textEvent('Details'));
    component.submitReport(post);

    expect(reportService.createReport).toHaveBeenCalledWith({
      targetType: 'POST',
      targetId: post.id,
      reason: 'Spam',
      details: 'Details',
    });
    expect(component.reportedPostIds()[post.id]).toBe(true);
    expect(component.reportFormOpenByPostId()[post.id]).toBe(false);

    component.toggleCommentReportForm(comment);
    component.updateCommentReportReason(comment.id, selectEvent('Harassment'));
    component.updateCommentReportDetails(comment.id, textEvent(''));
    component.submitCommentReport(comment);

    expect(reportService.createReport).toHaveBeenCalledWith({
      targetType: 'COMMENT',
      targetId: comment.id,
      reason: 'Harassment',
      details: null,
    });
    expect(component.reportedCommentIds()[comment.id]).toBe(true);
    expect(component.commentReportFormOpenByCommentId()[comment.id]).toBe(false);
  });

  it('ignores report guards and surfaces report errors', () => {
    const ownPost = postResponse('post-1', currentUser.id);
    const otherPost = postResponse('post-2', 'other-user');
    const ownComment = commentResponse('comment-1', otherPost.id, null);
    const otherComment = commentResponse('comment-2', otherPost.id, null, 'other-user');
    feedService.getFeed.mockReturnValue(of(page([ownPost, otherPost])));
    reportService.createReport.mockReturnValue(
      throwError(() => ({
        error: {
          message: 'Report failed.',
        },
      })),
    );
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.toggleReportForm(ownPost);
    component.toggleCommentReportForm(ownComment);
    component.submitReport(ownPost);
    component.submitCommentReport(ownComment);

    expect(component.reportFormOpenByPostId()[ownPost.id]).toBeUndefined();
    expect(component.commentReportFormOpenByCommentId()[ownComment.id]).toBeUndefined();
    expect(reportService.createReport).not.toHaveBeenCalled();

    component.toggleReportForm(otherPost);
    component.reportedPostIds.set({ [otherPost.id]: true });
    component.toggleReportForm(otherPost);
    expect(component.reportFormOpenByPostId()[otherPost.id]).toBe(true);

    component.reportingPostIds.set({ [otherPost.id]: false });
    component.reportedPostIds.set({});
    component.updateReportReason(otherPost.id, selectEvent('Spam'));
    component.submitReport(otherPost);
    expect(component.errorMessage()).toBe('Report failed.');

    component.toggleCommentReportForm(otherComment);
    component.updateCommentReportReason(otherComment.id, selectEvent('Spam'));
    component.submitCommentReport(otherComment);
    expect(component.errorMessage()).toBe('Report failed.');
  });

  it('loads a target post from query params and scrolls when present', () => {
    vi.useFakeTimers();
    const targetPost = postResponse('target-post', 'other-user');
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;
    feedService.getFeed.mockReturnValue(of(page([])));
    postService.getPostById.mockReturnValue(of(targetPost));
    queryParamMap.next(convertToParamMap({ postId: targetPost.id }));
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;
    const postElement = document.createElement('article');
    postElement.id = `post-${targetPost.id}`;
    document.body.appendChild(postElement);

    fixture.detectChanges();
    vi.advanceTimersByTime(51);

    expect(postService.getPostById).toHaveBeenCalledWith(targetPost.id);
    expect(component.posts()[0].id).toBe(targetPost.id);
    expect(scrollIntoView).toHaveBeenCalled();

    queryParamMap.next(convertToParamMap({ postId: targetPost.id }));
    vi.advanceTimersByTime(51);
    expect(scrollIntoView).toHaveBeenCalledTimes(2);

    postElement.remove();
  });

  it('renders the expanded comment, reply, report, selected media, and loading branches', () => {
    const post = postResponse('post-1', 'other-user');
    const parent = commentResponse('comment-1', post.id, null, 'other-user');
    const reply = commentResponse('reply-1', post.id, parent.id, 'other-user');
    const looseComment = {
      ...commentResponse('comment-2', post.id, 'missing-parent', 'other-user'),
      content: 'Loose comment',
    };
    const imageFile = new File(['image'], 'image.png', { type: 'image/png' });
    const documentFile = new File(['pdf'], 'file.pdf', { type: 'application/pdf' });
    feedService.getFeed.mockReturnValue(of({ items: [post], nextCursor: 'next-feed' }));
    const fixture = TestBed.createComponent(FeedPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    component.selectedFiles.set([
      { file: imageFile, previewUrl: 'blob:image-preview' },
      { file: documentFile, previewUrl: null },
    ]);
    component.commentsByPostId.set({ [post.id]: [parent, reply] });
    component.commentCursorByPostId.set({ [post.id]: 'next-comments' });
    component.commentInputByPostId.set({ [post.id]: 'Comment text' });
    component.commentErrorByPostId.set({ [post.id]: 'Comment failed.' });
    component.loadingCommentsByPostId.set({ [post.id]: true });
    component.submittingCommentByPostId.set({ [post.id]: true });
    component.replyFormOpenByCommentId.set({ [parent.id]: true });
    component.replyInputByCommentId.set({ [parent.id]: 'Reply text' });
    component.replyErrorByCommentId.set({ [parent.id]: 'Reply failed.' });
    component.submittingReplyByCommentId.set({ [parent.id]: true });
    component.reportFormOpenByPostId.set({ [post.id]: true });
    component.reportDraftByPostId.set({ [post.id]: { reason: 'Spam', details: 'Post details' } });
    component.reportingPostIds.set({ [post.id]: true });
    component.commentReportFormOpenByCommentId.set({
      [parent.id]: true,
      [reply.id]: true,
    });
    component.commentReportDraftByCommentId.set({
      [parent.id]: { reason: 'Harassment', details: 'Comment details' },
      [reply.id]: { reason: 'Other', details: 'Reply details' },
    });
    component.reportingCommentIds.set({ [parent.id]: true, [reply.id]: true });
    fixture.detectChanges();

    let text = fixture.nativeElement.textContent;
    expect(text).toContain('Remove');
    expect(text).toContain('file.pdf');
    expect(text).toContain('Submitting...');
    expect(text).toContain('Reply (1)');
    expect(text).toContain('Reply failed.');
    expect(text).toContain('Loading comments...');
    expect(text).toContain('Load more comments');
    expect(text).toContain('Load more');

    component.commentsByPostId.set({ [post.id]: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No comments yet.');

    component.commentsByPostId.set({ [post.id]: [looseComment] });
    fixture.detectChanges();
    text = fixture.nativeElement.textContent;
    expect(text).toContain('Loose comment');
    expect(text).toContain('Report');
  });

  function page<T>(items: T[]): PageResponse<T> {
    return {
      items,
      nextCursor: null,
    };
  }

  function user(): User {
    return {
      id: 'user-1',
      username: 'tinsae',
      email: 'tinsae@example.com',
      status: 'ACTIVE',
      roles: ['USER'],
      createdAt: '2026-07-24T10:00:00Z',
    };
  }

  function postResponse(id: string, authorId: string): Post {
    return {
      id,
      author: {
        id: authorId,
        username: authorId === currentUser.id ? currentUser.username : 'other',
        displayName: null,
        avatarUrl: null,
      },
      content: 'Post content',
      visibility: 'PUBLIC',
      status: 'PUBLISHED',
      media: [],
      commentCount: 0,
      reactionCount: 0,
      viewerReaction: null,
      createdAt: '2026-07-24T10:00:00Z',
      updatedAt: '2026-07-24T10:00:00Z',
    };
  }

  function commentResponse(
    id: string,
    postId: string,
    parentCommentId: string | null,
    authorId = currentUser.id,
  ): Comment {
    return {
      id,
      postId,
      parentCommentId,
      author: {
        id: authorId,
        username: authorId === currentUser.id ? currentUser.username : 'other',
        displayName: authorId === currentUser.id ? currentUser.username : 'Other User',
        avatarUrl: null,
      },
      content: parentCommentId ? 'Reply content' : 'Comment content',
      status: 'VISIBLE',
      reactionCount: 0,
      createdAt: parentCommentId ? '2026-07-24T10:01:00Z' : '2026-07-24T10:00:00Z',
    };
  }

  function fileEvent(files: File[]): Event {
    return {
      target: {
        files,
        value: 'selected-files',
      },
    } as unknown as Event;
  }

  function mediaResponse(id: string, mediaType: 'IMAGE' | 'DOCUMENT') {
    return {
      id,
      url: `https://example.com/${id}`,
      mediaType,
      mimeType: mediaType === 'IMAGE' ? 'image/png' : 'application/pdf',
      sizeBytes: 100,
      altText: mediaType === 'IMAGE' ? 'Image alt' : null,
      createdAt: '2026-07-24T10:00:00Z',
    };
  }

  function selectEvent(value: string): Event {
    return {
      target: {
        value,
      },
    } as unknown as Event;
  }

  function textEvent(value: string): Event {
    return {
      target: {
        value,
      },
    } as unknown as Event;
  }
});
