import { HttpErrorResponse } from '@angular/common/http';
import { TitleCasePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, Observable, of, switchMap } from 'rxjs';

import {
  AuthService,
  CommentService,
  FeedService,
  MediaService,
  PostService,
  ReactionService,
  ReportService,
} from '../../core/services';
import { Comment, ErrorResponse, Media, Post, PostVisibility, ReactionType, UUID } from '../../models';

const MAX_MEDIA_FILE_BYTES = 10 * 1024 * 1024;

interface SelectedMediaFile {
  file: File;
  previewUrl: string | null;
}

interface ReportDraft {
  reason: string;
  details: string;
}

@Component({
  selector: 'app-feed-page',
  imports: [ReactiveFormsModule, TitleCasePipe],
  templateUrl: './feed-page.html',
  styleUrl: './feed-page.scss',
})
export class FeedPage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly commentService = inject(CommentService);
  private readonly feedService = inject(FeedService);
  private readonly mediaService = inject(MediaService);
  private readonly postService = inject(PostService);
  private readonly reactionService = inject(ReactionService);
  private readonly reportService = inject(ReportService);
  private readonly pageSize = 10;

  readonly currentUser = this.authService.currentUser;
  readonly posts = signal<Post[]>([]);
  readonly nextCursor = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isLoadingMore = signal(false);
  readonly isLoadingTargetPost = signal(false);
  readonly isCreatingPost = signal(false);
  readonly targetPostId = signal<UUID | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly composerError = signal<string | null>(null);
  readonly selectedFiles = signal<SelectedMediaFile[]>([]);
  readonly commentsByPostId = signal<Record<string, Comment[]>>({});
  readonly commentCursorByPostId = signal<Record<string, string | null>>({});
  readonly commentInputByPostId = signal<Record<string, string>>({});
  readonly commentErrorByPostId = signal<Record<string, string | null>>({});
  readonly replyInputByCommentId = signal<Record<string, string>>({});
  readonly replyErrorByCommentId = signal<Record<string, string | null>>({});
  readonly replyFormOpenByCommentId = signal<Record<string, boolean>>({});
  readonly submittingReplyByCommentId = signal<Record<string, boolean>>({});
  readonly loadingCommentsByPostId = signal<Record<string, boolean>>({});
  readonly submittingCommentByPostId = signal<Record<string, boolean>>({});
  readonly reactingPostIds = signal<Record<string, boolean>>({});
  readonly deletingPostIds = signal<Record<string, boolean>>({});
  readonly reportFormOpenByPostId = signal<Record<string, boolean>>({});
  readonly reportDraftByPostId = signal<Record<string, ReportDraft>>({});
  readonly reportingPostIds = signal<Record<string, boolean>>({});
  readonly reportedPostIds = signal<Record<string, boolean>>({});
  readonly commentReportFormOpenByCommentId = signal<Record<string, boolean>>({});
  readonly commentReportDraftByCommentId = signal<Record<string, ReportDraft>>({});
  readonly reportingCommentIds = signal<Record<string, boolean>>({});
  readonly reportedCommentIds = signal<Record<string, boolean>>({});

  readonly postForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.maxLength(5000)]],
    visibility: ['PUBLIC' as PostVisibility, [Validators.required]],
  });

  ngOnInit(): void {
    this.watchTargetPost();
    this.loadFeed();
  }

  loadFeed(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.feedService
      .getFeed({ limit: this.pageSize })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.posts.set(response.items);
          this.nextCursor.set(response.nextCursor);
          this.ensureTargetPostVisible();
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  loadMore(): void {
    const cursor = this.nextCursor();
    if (!cursor || this.isLoadingMore()) {
      return;
    }

    this.isLoadingMore.set(true);
    this.errorMessage.set(null);

    this.feedService
      .getFeed({ cursor, limit: this.pageSize })
      .pipe(finalize(() => this.isLoadingMore.set(false)))
      .subscribe({
        next: (response) => {
          this.posts.update((posts) => [...posts, ...response.items]);
          this.nextCursor.set(response.nextCursor);
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  loadComments(postId: UUID): void {
    if (this.loadingCommentsByPostId()[postId]) {
      return;
    }

    this.setPostFlag(this.loadingCommentsByPostId, postId, true);
    this.errorMessage.set(null);

    this.commentService
      .listPostComments(postId, { limit: 5 })
      .pipe(finalize(() => this.setPostFlag(this.loadingCommentsByPostId, postId, false)))
      .subscribe({
        next: (response) => {
          this.commentsByPostId.update((comments) => ({
            ...comments,
            [postId]: this.mergeComments(comments[postId] ?? [], response.items),
          }));
          this.commentCursorByPostId.update((cursors) => ({
            ...cursors,
            [postId]: response.nextCursor,
          }));
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  loadMoreComments(postId: UUID): void {
    const cursor = this.commentCursorByPostId()[postId];
    if (!cursor || this.loadingCommentsByPostId()[postId]) {
      return;
    }

    this.setPostFlag(this.loadingCommentsByPostId, postId, true);
    this.errorMessage.set(null);

    this.commentService
      .listPostComments(postId, { cursor, limit: 5 })
      .pipe(finalize(() => this.setPostFlag(this.loadingCommentsByPostId, postId, false)))
      .subscribe({
        next: (response) => {
          this.commentsByPostId.update((comments) => ({
            ...comments,
            [postId]: [...(comments[postId] ?? []), ...response.items],
          }));
          this.commentCursorByPostId.update((cursors) => ({
            ...cursors,
            [postId]: response.nextCursor,
          }));
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  createComment(postId: UUID): void {
    const content = this.commentInputByPostId()[postId]?.trim();
    if (!content || this.submittingCommentByPostId()[postId]) {
      return;
    }

    this.createCommentOrReply({
      postId,
      content,
      parentCommentId: null,
      pendingId: `pending-${postId}-${Date.now()}`,
      onStart: () => {
        this.commentInputByPostId.update((inputs) => ({
          ...inputs,
          [postId]: '',
        }));
        this.setPostFlag(this.submittingCommentByPostId, postId, true);
        this.setCommentError(postId, null);
      },
      onFinish: () => this.setPostFlag(this.submittingCommentByPostId, postId, false),
      onError: (message) => {
        this.commentInputByPostId.update((inputs) => ({
          ...inputs,
          [postId]: content,
        }));
        this.setCommentError(postId, message);
      },
    });
  }

  createReply(postId: UUID, parentComment: Comment): void {
    const content = this.replyInputByCommentId()[parentComment.id]?.trim();
    if (!content || this.submittingReplyByCommentId()[parentComment.id]) {
      return;
    }

    this.createCommentOrReply({
      postId,
      content,
      parentCommentId: parentComment.id,
      pendingId: `pending-reply-${parentComment.id}-${Date.now()}`,
      onStart: () => {
        this.replyInputByCommentId.update((inputs) => ({
          ...inputs,
          [parentComment.id]: '',
        }));
        this.setPostFlag(this.submittingReplyByCommentId, parentComment.id, true);
        this.setReplyError(parentComment.id, null);
      },
      onFinish: () => this.setPostFlag(this.submittingReplyByCommentId, parentComment.id, false),
      onError: (message) => {
        this.replyInputByCommentId.update((inputs) => ({
          ...inputs,
          [parentComment.id]: content,
        }));
        this.setReplyError(parentComment.id, message);
      },
      onSuccess: () => {
        this.replyFormOpenByCommentId.update((forms) => ({
          ...forms,
          [parentComment.id]: false,
        }));
      },
    });
  }

  toggleReplyForm(comment: Comment): void {
    this.replyFormOpenByCommentId.update((forms) => ({
      ...forms,
      [comment.id]: !forms[comment.id],
    }));
    this.setReplyError(comment.id, null);
  }

  updateReplyInput(commentId: UUID, event: Event): void {
    const input = event.target as HTMLInputElement | HTMLTextAreaElement;
    this.replyInputByCommentId.update((inputs) => ({
      ...inputs,
      [commentId]: input.value,
    }));
  }

  parentComments(postId: UUID): Comment[] {
    return (this.commentsByPostId()[postId] ?? []).filter((comment) => !comment.parentCommentId);
  }

  repliesForComment(postId: UUID, commentId: UUID): Comment[] {
    return (this.commentsByPostId()[postId] ?? []).filter((comment) => comment.parentCommentId === commentId);
  }

  commentReplyCount(postId: UUID, commentId: UUID): number {
    return this.repliesForComment(postId, commentId).length;
  }

  private createCommentOrReply(options: {
    postId: UUID;
    content: string;
    parentCommentId: UUID | null;
    pendingId: UUID;
    onStart: () => void;
    onFinish: () => void;
    onError: (message: string) => void;
    onSuccess?: () => void;
  }): void {
    const currentUser = this.currentUser();
    if (!currentUser) {
      options.onError('You must be logged in to comment.');
      return;
    }

    const pendingComment: Comment = {
      id: options.pendingId,
      postId: options.postId,
      parentCommentId: options.parentCommentId,
      author: {
        id: currentUser.id,
        username: currentUser.username,
        displayName: currentUser.username,
        avatarUrl: null,
      },
      content: options.content,
      status: 'VISIBLE',
      reactionCount: 0,
      createdAt: new Date().toISOString(),
    };

    this.commentsByPostId.update((comments) => ({
      ...comments,
      [options.postId]: this.mergeComments([pendingComment], comments[options.postId] ?? []),
    }));
    options.onStart();
    this.errorMessage.set(null);

    this.commentService
      .createComment(options.postId, {
        content: options.content,
        parentCommentId: options.parentCommentId,
      })
      .pipe(finalize(options.onFinish))
      .subscribe({
        next: (comment) => {
          this.commentsByPostId.update((comments) => ({
            ...comments,
            [options.postId]: this.mergeComments(
              [comment],
              (comments[options.postId] ?? []).filter((item) => item.id !== options.pendingId),
            ),
          }));
          this.updatePost(options.postId, (post) => ({
            ...post,
            commentCount: post.commentCount + 1,
          }));
          options.onSuccess?.();
          this.scrollToComment(comment.id);
        },
        error: (error: HttpErrorResponse) => {
          this.commentsByPostId.update((comments) => ({
            ...comments,
            [options.postId]: (comments[options.postId] ?? []).filter((comment) => comment.id !== options.pendingId),
          }));
          options.onError(this.errorText(error));
        },
      });
  }

  toggleReaction(post: Post): void {
    if (this.reactingPostIds()[post.id]) {
      return;
    }

    this.setPostFlag(this.reactingPostIds, post.id, true);
    this.errorMessage.set(null);

    const request$: Observable<unknown> = post.viewerReaction
      ? this.reactionService.removePostReaction(post.id)
      : this.reactionService.reactToPost(post.id, { reactionType: 'LIKE' });

    request$.pipe(finalize(() => this.setPostFlag(this.reactingPostIds, post.id, false))).subscribe({
      next: () => {
        this.updatePost(post.id, (currentPost) => {
          const nextReaction: ReactionType | null = currentPost.viewerReaction ? null : 'LIKE';
          const reactionDelta = currentPost.viewerReaction ? -1 : 1;
          return {
            ...currentPost,
            viewerReaction: nextReaction,
            reactionCount: Math.max(0, currentPost.reactionCount + reactionDelta),
          };
        });
      },
      error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
    });
  }

  deletePost(post: Post): void {
    if (!this.isOwnPost(post) || this.deletingPostIds()[post.id]) {
      return;
    }

    const confirmed = window.confirm('Delete this post? This action cannot be undone.');
    if (!confirmed) {
      return;
    }

    this.setPostFlag(this.deletingPostIds, post.id, true);
    this.errorMessage.set(null);

    this.postService
      .deletePost(post.id)
      .pipe(finalize(() => this.setPostFlag(this.deletingPostIds, post.id, false)))
      .subscribe({
        next: () => {
          this.posts.update((posts) => posts.filter((item) => item.id !== post.id));
          this.commentsByPostId.update((comments) => {
            const nextComments = { ...comments };
            delete nextComments[post.id];
            return nextComments;
          });
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  createPost(): void {
    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }

    const content = this.postForm.controls.content.value.trim();
    const files = this.selectedFiles();

    if (!content && files.length === 0) {
      this.composerError.set('Add text or media before posting.');
      return;
    }

    this.isCreatingPost.set(true);
    this.composerError.set(null);

    const uploads = files.length
      ? forkJoin(files.map((selectedFile) => this.mediaService.uploadMedia({ file: selectedFile.file })))
      : of([] as Media[]);

    uploads
      .pipe(
        switchMap((media) =>
          this.postService.createPost({
            content: content || null,
            visibility: this.postForm.controls.visibility.value,
            mediaIds: media.map((item) => item.id),
          }),
        ),
        finalize(() => this.isCreatingPost.set(false)),
      )
      .subscribe({
        next: (post) => {
          this.posts.update((posts) => [post, ...posts]);
          this.resetComposer();
        },
        error: (error: HttpErrorResponse) => this.composerError.set(this.errorText(error)),
      });
  }

  selectFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    const oversizedFiles = files.filter((file) => file.size > MAX_MEDIA_FILE_BYTES);

    if (oversizedFiles.length > 0) {
      this.clearSelectedFilePreviews();
      this.selectedFiles.set([]);
      this.composerError.set('Each media file must not exceed 10 MB.');
      input.value = '';
      return;
    }

    this.composerError.set(null);
    this.clearSelectedFilePreviews();
    this.selectedFiles.set(
      files.map((file) => ({
        file,
        previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
      })),
    );
    input.value = '';
  }

  removeSelectedFile(index: number): void {
    const files = this.selectedFiles();
    const removed = files[index];
    if (removed?.previewUrl) {
      URL.revokeObjectURL(removed.previewUrl);
    }
    this.selectedFiles.set(files.filter((_, fileIndex) => fileIndex !== index));
  }

  updateCommentInput(postId: UUID, event: Event): void {
    const input = event.target as HTMLInputElement | HTMLTextAreaElement;
    this.commentInputByPostId.update((inputs) => ({
      ...inputs,
      [postId]: input.value,
    }));
  }

  toggleReportForm(post: Post): void {
    if (this.isOwnPost(post) || this.reportedPostIds()[post.id]) {
      return;
    }

    this.reportFormOpenByPostId.update((forms) => ({
      ...forms,
      [post.id]: !forms[post.id],
    }));
    this.reportDraftByPostId.update((drafts) => ({
      ...drafts,
      [post.id]: drafts[post.id] ?? {
        reason: '',
        details: '',
      },
    }));
  }

  updateReportReason(postId: UUID, event: Event): void {
    const input = event.target as HTMLSelectElement;
    this.updateReportDraft(postId, {
      reason: input.value,
    });
  }

  updateReportDetails(postId: UUID, event: Event): void {
    const input = event.target as HTMLTextAreaElement;
    this.updateReportDraft(postId, {
      details: input.value,
    });
  }

  submitReport(post: Post): void {
    const draft = this.reportDraftByPostId()[post.id];
    const reason = draft?.reason.trim();

    if (!reason || this.reportingPostIds()[post.id] || this.isOwnPost(post)) {
      return;
    }

    this.setPostFlag(this.reportingPostIds, post.id, true);
    this.errorMessage.set(null);

    this.reportService
      .createReport({
        targetType: 'POST',
        targetId: post.id,
        reason,
        details: draft.details.trim() || null,
      })
      .pipe(finalize(() => this.setPostFlag(this.reportingPostIds, post.id, false)))
      .subscribe({
        next: () => {
          this.setPostFlag(this.reportedPostIds, post.id, true);
          this.reportFormOpenByPostId.update((forms) => ({
            ...forms,
            [post.id]: false,
          }));
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  toggleCommentReportForm(comment: Comment): void {
    if (this.isOwnComment(comment) || this.reportedCommentIds()[comment.id]) {
      return;
    }

    this.commentReportFormOpenByCommentId.update((forms) => ({
      ...forms,
      [comment.id]: !forms[comment.id],
    }));
    this.commentReportDraftByCommentId.update((drafts) => ({
      ...drafts,
      [comment.id]: drafts[comment.id] ?? {
        reason: '',
        details: '',
      },
    }));
  }

  updateCommentReportReason(commentId: UUID, event: Event): void {
    const input = event.target as HTMLSelectElement;
    this.updateCommentReportDraft(commentId, {
      reason: input.value,
    });
  }

  updateCommentReportDetails(commentId: UUID, event: Event): void {
    const input = event.target as HTMLTextAreaElement;
    this.updateCommentReportDraft(commentId, {
      details: input.value,
    });
  }

  submitCommentReport(comment: Comment): void {
    const draft = this.commentReportDraftByCommentId()[comment.id];
    const reason = draft?.reason.trim();

    if (!reason || this.reportingCommentIds()[comment.id] || this.isOwnComment(comment)) {
      return;
    }

    this.setPostFlag(this.reportingCommentIds, comment.id, true);
    this.errorMessage.set(null);

    this.reportService
      .createReport({
        targetType: 'COMMENT',
        targetId: comment.id,
        reason,
        details: draft.details.trim() || null,
      })
      .pipe(finalize(() => this.setPostFlag(this.reportingCommentIds, comment.id, false)))
      .subscribe({
        next: () => {
          this.setPostFlag(this.reportedCommentIds, comment.id, true);
          this.commentReportFormOpenByCommentId.update((forms) => ({
            ...forms,
            [comment.id]: false,
          }));
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  trackPost(_: number, post: Post): string {
    return post.id;
  }

  trackSelectedFile(index: number, selectedFile: SelectedMediaFile): string {
    return `${selectedFile.file.name}-${selectedFile.file.size}-${index}`;
  }

  trackComment(_: number, comment: Comment): string {
    return comment.id;
  }

  isTargetPost(post: Post): boolean {
    return this.targetPostId() === post.id;
  }

  isOwnPost(post: Post): boolean {
    return this.currentUser()?.id === post.author.id;
  }

  isOwnComment(comment: Comment): boolean {
    return this.currentUser()?.id === comment.author.id;
  }

  createdDate(post: Post): string {
    return new Intl.DateTimeFormat('en', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(post.createdAt));
  }

  private errorText(error: HttpErrorResponse): string {
    const response = error.error as Partial<ErrorResponse> | undefined;
    if (response?.message) {
      return response.message;
    }

    if (error.status === 0) {
      return 'Cannot reach the backend. Check that the API is running.';
    }

    return `Feed request failed with status ${error.status}.`;
  }

  private resetComposer(): void {
    this.postForm.reset({
      content: '',
      visibility: 'PUBLIC',
    });
    this.clearSelectedFilePreviews();
    this.selectedFiles.set([]);
  }

  private clearSelectedFilePreviews(): void {
    for (const selectedFile of this.selectedFiles()) {
      if (selectedFile.previewUrl) {
        URL.revokeObjectURL(selectedFile.previewUrl);
      }
    }
  }

  private setPostFlag(
    signalRef: { update: (updater: (value: Record<string, boolean>) => Record<string, boolean>) => void },
    postId: UUID,
    value: boolean,
  ): void {
    signalRef.update((flags) => ({
      ...flags,
      [postId]: value,
    }));
  }

  private setCommentError(postId: UUID, message: string | null): void {
    this.commentErrorByPostId.update((errors) => ({
      ...errors,
      [postId]: message,
    }));
  }

  private setReplyError(commentId: UUID, message: string | null): void {
    this.replyErrorByCommentId.update((errors) => ({
      ...errors,
      [commentId]: message,
    }));
  }

  private updatePost(postId: UUID, updater: (post: Post) => Post): void {
    this.posts.update((posts) => posts.map((post) => (post.id === postId ? updater(post) : post)));
  }

  private mergeComments(existingComments: Comment[], loadedComments: Comment[]): Comment[] {
    const commentsById = new Map<string, Comment>();

    for (const comment of existingComments) {
      commentsById.set(comment.id, comment);
    }

    for (const comment of loadedComments) {
      commentsById.set(comment.id, comment);
    }

    return Array.from(commentsById.values()).sort(
      (first, second) => new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime(),
    );
  }

  private updateReportDraft(postId: UUID, patch: Partial<ReportDraft>): void {
    this.reportDraftByPostId.update((drafts) => ({
      ...drafts,
      [postId]: {
        reason: drafts[postId]?.reason ?? '',
        details: drafts[postId]?.details ?? '',
        ...patch,
      },
    }));
  }

  private updateCommentReportDraft(commentId: UUID, patch: Partial<ReportDraft>): void {
    this.commentReportDraftByCommentId.update((drafts) => ({
      ...drafts,
      [commentId]: {
        reason: drafts[commentId]?.reason ?? '',
        details: drafts[commentId]?.details ?? '',
        ...patch,
      },
    }));
  }

  private watchTargetPost(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.targetPostId.set(params.get('postId'));

      if (!this.isLoading()) {
        this.ensureTargetPostVisible();
      }
    });
  }

  private ensureTargetPostVisible(): void {
    const postId = this.targetPostId();
    if (!postId) {
      return;
    }

    if (this.posts().some((post) => post.id === postId)) {
      this.scrollToTargetPost(postId);
      return;
    }

    if (this.isLoadingTargetPost()) {
      return;
    }

    this.isLoadingTargetPost.set(true);
    this.postService
      .getPostById(postId)
      .pipe(finalize(() => this.isLoadingTargetPost.set(false)))
      .subscribe({
        next: (post) => {
          this.posts.update((posts) => [post, ...posts.filter((item) => item.id !== post.id)]);
          this.scrollToTargetPost(post.id);
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  private scrollToTargetPost(postId: UUID): void {
    this.scrollToPostWhenReady(postId, 0);
  }

  private scrollToPostWhenReady(postId: UUID, attempt: number): void {
    window.setTimeout(() => {
      const postElement = document.getElementById(`post-${postId}`);
      if (!postElement && attempt < 10) {
        this.scrollToPostWhenReady(postId, attempt + 1);
        return;
      }

      postElement?.scrollIntoView?.({
        behavior: 'smooth',
        block: 'center',
      });
    }, 50);
  }

  private scrollToComment(commentId: UUID): void {
    window.setTimeout(() => {
      document.getElementById(`comment-${commentId}`)?.scrollIntoView?.({
        behavior: 'smooth',
        block: 'center',
      });
    });
  }
}
