import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, finalize, of, switchMap, tap } from 'rxjs';

import { AuthService, MessageService, ReportService, UserService } from '../../core/services';
import { ErrorResponse, Message, UserSummary, UUID } from '../../models';

interface ReportDraft {
  reason: string;
  details: string;
}

interface Conversation {
  participant: UserSummary;
  messages: Message[];
  latestMessage: Message;
  unreadCount: number;
}

@Component({
  selector: 'app-messages-page',
  imports: [ReactiveFormsModule],
  templateUrl: './messages-page.html',
  styleUrl: './messages-page.scss',
})
export class MessagesPage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly reportService = inject(ReportService);
  private readonly userService = inject(UserService);
  private readonly pageSize = 20;

  readonly currentUser = this.authService.currentUser;
  readonly messages = signal<Message[]>([]);
  readonly userResults = signal<UserSummary[]>([]);
  readonly selectedRecipient = signal<UserSummary | null>(null);
  readonly selectedConversationId = signal<UUID | null>(null);
  readonly targetMessageId = signal<UUID | null>(null);
  readonly nextCursor = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isLoadingMore = signal(false);
  readonly isLoadingTargetMessage = signal(false);
  readonly isSending = signal(false);
  readonly isSearchingUsers = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly sendError = signal<string | null>(null);
  readonly reportFormOpenByMessageId = signal<Record<string, boolean>>({});
  readonly reportDraftByMessageId = signal<Record<string, ReportDraft>>({});
  readonly reportingMessageIds = signal<Record<string, boolean>>({});
  readonly reportedMessageIds = signal<Record<string, boolean>>({});
  readonly markingConversationReadIds = signal<Record<string, boolean>>({});

  readonly messageForm = this.formBuilder.nonNullable.group({
    recipientQuery: ['', [Validators.required]],
    recipientId: ['', [Validators.required]],
    content: ['', [Validators.required, Validators.maxLength(5000)]],
  });

  readonly conversations = computed(() => {
    const currentUserId = this.currentUser()?.id;
    if (!currentUserId) {
      return [];
    }

    const conversationsByParticipant = new Map<UUID, Conversation>();
    for (const message of this.messages()) {
      const participant = message.sender.id === currentUserId ? message.recipient : message.sender;
      const existing = conversationsByParticipant.get(participant.id);
      const messages = existing ? [...existing.messages, message] : [message];
      const latestMessage = messages.reduce((latest, item) =>
        new Date(item.sentAt).getTime() > new Date(latest.sentAt).getTime() ? item : latest,
      );
      const unreadCount = messages.filter((item) => item.recipient.id === currentUserId && item.status !== 'READ').length;

      conversationsByParticipant.set(participant.id, {
        participant,
        messages,
        latestMessage,
        unreadCount,
      });
    }

    return Array.from(conversationsByParticipant.values()).sort(
      (first, second) => new Date(second.latestMessage.sentAt).getTime() - new Date(first.latestMessage.sentAt).getTime(),
    );
  });

  readonly selectedConversation = computed(() => {
    const selectedConversationId = this.selectedConversationId();
    return this.conversations().find((conversation) => conversation.participant.id === selectedConversationId) ?? null;
  });

  readonly selectedConversationMessages = computed(() => {
    const conversation = this.selectedConversation();
    if (!conversation) {
      return [];
    }

    return [...conversation.messages].sort(
      (first, second) => new Date(first.sentAt).getTime() - new Date(second.sentAt).getTime(),
    );
  });

  ngOnInit(): void {
    this.watchRecipientSearch();
    this.watchTargetMessage();
    this.loadMessages();
  }

  loadMessages(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.messageService
      .listMessages({ limit: this.pageSize })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.set(response.items);
          this.nextCursor.set(response.nextCursor);
          this.selectInitialConversation();
          this.ensureTargetMessageVisible();
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

    this.messageService
      .listMessages({ cursor, limit: this.pageSize })
      .pipe(finalize(() => this.isLoadingMore.set(false)))
      .subscribe({
        next: (response) => {
          this.messages.update((messages) => [...messages, ...response.items]);
          this.nextCursor.set(response.nextCursor);
          this.selectInitialConversation();
          this.ensureTargetMessageVisible();
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  sendMessage(): void {
    if (this.messageForm.invalid) {
      this.messageForm.markAllAsTouched();
      return;
    }

    const rawValue = this.messageForm.getRawValue();
    this.isSending.set(true);
    this.sendError.set(null);

    this.messageService
      .sendMessage({
        recipientId: rawValue.recipientId.trim(),
        content: rawValue.content.trim(),
      })
      .pipe(finalize(() => this.isSending.set(false)))
      .subscribe({
        next: (message) => {
          this.messages.update((messages) => [message, ...messages]);
          this.selectedConversationId.set(message.recipient.id);
          this.selectedRecipient.set(message.recipient);
          this.messageForm.controls.content.reset('');
          this.scrollToLatestMessage();
        },
        error: (error: HttpErrorResponse) => this.sendError.set(this.errorText(error)),
      });
  }

  isInvalid(controlName: keyof typeof this.messageForm.controls): boolean {
    const control = this.messageForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  selectRecipient(user: UserSummary): void {
    this.selectedRecipient.set(user);
    this.selectedConversationId.set(user.id);
    this.userResults.set([]);
    this.messageForm.patchValue({
      recipientQuery: this.userLabel(user),
      recipientId: user.id,
    });
    this.markConversationMessagesRead(user.id);
  }

  clearRecipient(): void {
    this.selectedRecipient.set(null);
    this.selectedConversationId.set(null);
    this.userResults.set([]);
    this.messageForm.patchValue({
      recipientQuery: '',
      recipientId: '',
    });
  }

  selectConversation(conversation: Conversation): void {
    this.selectedConversationId.set(conversation.participant.id);
    this.selectedRecipient.set(conversation.participant);
    this.userResults.set([]);
    this.messageForm.patchValue({
      recipientQuery: this.userLabel(conversation.participant),
      recipientId: conversation.participant.id,
    });
    this.markConversationMessagesRead(conversation.participant.id);
    this.scrollToLatestMessage();
  }

  toggleReportForm(message: Message): void {
    if (this.isOwnMessage(message) || this.reportedMessageIds()[message.id]) {
      return;
    }

    this.reportFormOpenByMessageId.update((forms) => ({
      ...forms,
      [message.id]: !forms[message.id],
    }));
    this.reportDraftByMessageId.update((drafts) => ({
      ...drafts,
      [message.id]: drafts[message.id] ?? {
        reason: '',
        details: '',
      },
    }));
  }

  updateReportReason(messageId: UUID, event: Event): void {
    const input = event.target as HTMLSelectElement;
    this.updateReportDraft(messageId, {
      reason: input.value,
    });
  }

  updateReportDetails(messageId: UUID, event: Event): void {
    const input = event.target as HTMLTextAreaElement;
    this.updateReportDraft(messageId, {
      details: input.value,
    });
  }

  submitReport(message: Message): void {
    const draft = this.reportDraftByMessageId()[message.id];
    const reason = draft?.reason.trim();

    if (!reason || this.reportingMessageIds()[message.id] || this.isOwnMessage(message)) {
      return;
    }

    this.setMessageFlag(this.reportingMessageIds, message.id, true);
    this.errorMessage.set(null);

    this.reportService
      .createReport({
        targetType: 'MESSAGE',
        targetId: message.id,
        reason,
        details: draft.details.trim() || null,
      })
      .pipe(finalize(() => this.setMessageFlag(this.reportingMessageIds, message.id, false)))
      .subscribe({
        next: () => {
          this.setMessageFlag(this.reportedMessageIds, message.id, true);
          this.reportFormOpenByMessageId.update((forms) => ({
            ...forms,
            [message.id]: false,
          }));
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  userLabel(user: UserSummary): string {
    return user.displayName ? `${user.displayName} (@${user.username})` : `@${user.username}`;
  }

  trackMessage(_: number, message: Message): string {
    return message.id;
  }

  trackUser(_: number, user: UserSummary): string {
    return user.id;
  }

  trackConversation(_: number, conversation: Conversation): string {
    return conversation.participant.id;
  }

  isTargetMessage(message: Message): boolean {
    return this.targetMessageId() === message.id;
  }

  isOwnMessage(message: Message): boolean {
    return this.currentUser()?.id === message.sender.id;
  }

  messagePreview(message: Message): string {
    return message.content.length > 72 ? `${message.content.slice(0, 72)}...` : message.content;
  }

  sentDate(message: Message): string {
    return new Intl.DateTimeFormat('en', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(message.sentAt));
  }

  private errorText(error: HttpErrorResponse): string {
    const response = error.error as Partial<ErrorResponse> | undefined;
    if (response?.message) {
      return response.message;
    }

    if (error.status === 0) {
      return 'Cannot reach the backend. Check that the API is running.';
    }

    return `Message request failed with status ${error.status}.`;
  }

  private setMessageFlag(
    signalRef: { update: (updater: (value: Record<string, boolean>) => Record<string, boolean>) => void },
    messageId: UUID,
    value: boolean,
  ): void {
    signalRef.update((flags) => ({
      ...flags,
      [messageId]: value,
    }));
  }

  private updateReportDraft(messageId: UUID, patch: Partial<ReportDraft>): void {
    this.reportDraftByMessageId.update((drafts) => ({
      ...drafts,
      [messageId]: {
        reason: drafts[messageId]?.reason ?? '',
        details: drafts[messageId]?.details ?? '',
        ...patch,
      },
    }));
  }

  private watchRecipientSearch(): void {
    this.messageForm.controls.recipientQuery.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap((query) => {
          const selectedRecipient = this.selectedRecipient();
          if (selectedRecipient && query !== this.userLabel(selectedRecipient)) {
            this.selectedRecipient.set(null);
            this.messageForm.controls.recipientId.setValue('');
          }
        }),
        switchMap((query) => {
          const normalizedQuery = query.trim();
          if (this.selectedRecipient() || normalizedQuery.length < 2) {
            this.isSearchingUsers.set(false);
            this.userResults.set([]);
            return of([]);
          }

          this.isSearchingUsers.set(true);
          return this.userService.searchUsers(normalizedQuery).pipe(
            catchError(() => of([])),
            finalize(() => this.isSearchingUsers.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((users) => this.userResults.set(users));
  }

  private watchTargetMessage(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.targetMessageId.set(params.get('messageId'));

      if (!this.isLoading()) {
        this.ensureTargetMessageVisible();
      }
    });
  }

  private ensureTargetMessageVisible(): void {
    const messageId = this.targetMessageId();
    if (!messageId) {
      return;
    }

    if (this.messages().some((message) => message.id === messageId)) {
      this.scrollToTargetMessage(messageId);
      return;
    }

    if (this.isLoadingTargetMessage()) {
      return;
    }

    this.isLoadingTargetMessage.set(true);
    this.messageService
      .getMessageById(messageId)
      .pipe(finalize(() => this.isLoadingTargetMessage.set(false)))
      .subscribe({
        next: (message) => {
          this.messages.update((messages) => [message, ...messages.filter((item) => item.id !== message.id)]);
          const currentUserId = this.currentUser()?.id;
          if (currentUserId) {
            this.selectedConversationId.set(message.sender.id === currentUserId ? message.recipient.id : message.sender.id);
          }
          this.scrollToTargetMessage(message.id);
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  private scrollToTargetMessage(messageId: UUID): void {
    window.setTimeout(() => {
      document.getElementById(`message-${messageId}`)?.scrollIntoView?.({
        behavior: 'smooth',
        block: 'center',
      });
    });
  }

  private markConversationMessagesRead(participantId: UUID): void {
    const currentUserId = this.currentUser()?.id;
    if (!currentUserId || this.markingConversationReadIds()[participantId]) {
      return;
    }

    const hasUnreadMessages = this.messages().some(
      (message) =>
        message.sender.id === participantId &&
        message.recipient.id === currentUserId &&
        message.status !== 'READ' &&
        message.status !== 'DELETED',
    );
    if (!hasUnreadMessages) {
      return;
    }

    this.setMessageFlag(this.markingConversationReadIds, participantId, true);
    this.messageService
      .markConversationAsRead(participantId)
      .pipe(finalize(() => this.setMessageFlag(this.markingConversationReadIds, participantId, false)))
      .subscribe({
        next: () => {
          const readAt = new Date().toISOString();
          this.messages.update((messages) =>
            messages.map((message) =>
              message.sender.id === participantId &&
              message.recipient.id === currentUserId &&
              message.status !== 'READ' &&
              message.status !== 'DELETED'
                ? {
                    ...message,
                    status: 'READ',
                    readAt: message.readAt ?? readAt,
                  }
                : message,
            ),
          );
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  private selectInitialConversation(): void {
    if (this.selectedConversationId() || this.conversations().length === 0) {
      return;
    }

    this.selectConversation(this.conversations()[0]);
  }

  private scrollToLatestMessage(): void {
    window.setTimeout(() => {
      const messages = this.selectedConversationMessages();
      const latestMessage = messages[messages.length - 1];
      if (latestMessage) {
        document.getElementById(`message-${latestMessage.id}`)?.scrollIntoView?.({
          behavior: 'smooth',
          block: 'end',
        });
      }
    });
  }
}
