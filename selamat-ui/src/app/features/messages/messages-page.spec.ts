import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService, MessageService, ReportService, UserService } from '../../core/services';
import { Message, User, UserSummary } from '../../models';
import { MessagesPage } from './messages-page';

describe('MessagesPage', () => {
  let fixture: ComponentFixture<MessagesPage>;
  let queryParamMap: BehaviorSubject<ParamMap>;
  let authService: {
    currentUser: () => User | null;
  };
  let messageService: {
    listMessages: ReturnType<typeof vi.fn>;
    getMessageById: ReturnType<typeof vi.fn>;
    sendMessage: ReturnType<typeof vi.fn>;
    markConversationAsRead: ReturnType<typeof vi.fn>;
  };
  let reportService: {
    createReport: ReturnType<typeof vi.fn>;
  };
  let userService: {
    searchUsers: ReturnType<typeof vi.fn>;
  };

  const currentUser = createUser('user-1', 'selam');
  const recipient = createUserSummary('user-2', 'mira', 'Mira Alem');
  const incomingMessage = createMessage('message-1', recipient, userSummary(currentUser), 'Hello Selam');
  const outgoingMessage = createMessage('message-2', userSummary(currentUser), recipient, 'Hello Mira');

  async function configureMessagesPage(messageId: string | null = null): Promise<void> {
    queryParamMap = new BehaviorSubject(convertToParamMap(messageId ? { messageId } : {}));
    authService = {
      currentUser: () => currentUser,
    };
    messageService = {
      listMessages: vi.fn(() => of({ items: [incomingMessage], nextCursor: 'cursor-2' })),
      getMessageById: vi.fn(() => of(outgoingMessage)),
      sendMessage: vi.fn(() => of(outgoingMessage)),
      markConversationAsRead: vi.fn(() => of(undefined)),
    };
    reportService = {
      createReport: vi.fn(() => of({ id: 'report-1' })),
    };
    userService = {
      searchUsers: vi.fn(() => of([recipient])),
    };

    await TestBed.configureTestingModule({
      imports: [MessagesPage],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap } },
        { provide: AuthService, useValue: authService },
        { provide: MessageService, useValue: messageService },
        { provide: ReportService, useValue: reportService },
        { provide: UserService, useValue: userService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MessagesPage);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads messages on init', async () => {
    await configureMessagesPage();

    expect(messageService.listMessages).toHaveBeenCalledWith({ limit: 20 });
    expect(messageService.markConversationAsRead).toHaveBeenCalledWith(recipient.id);
    expect(fixture.componentInstance.messages()[0]).toMatchObject({
      id: incomingMessage.id,
      status: 'READ',
    });
    expect(fixture.componentInstance.nextCursor()).toBe('cursor-2');
    expect(fixture.nativeElement.textContent).toContain('Hello Selam');
  });

  it('loads more messages when next cursor exists', async () => {
    await configureMessagesPage();
    messageService.listMessages.mockReturnValueOnce(of({ items: [outgoingMessage], nextCursor: null }));

    fixture.componentInstance.loadMore();

    expect(messageService.listMessages).toHaveBeenLastCalledWith({ cursor: 'cursor-2', limit: 20 });
    expect(fixture.componentInstance.messages()).toEqual([
      expect.objectContaining({ id: incomingMessage.id, status: 'READ' }),
      outgoingMessage,
    ]);
    expect(fixture.componentInstance.nextCursor()).toBeNull();
  });

  it('clears unread count when selecting a conversation', async () => {
    await configureMessagesPage();

    expect(fixture.componentInstance.conversations()[0].unreadCount).toBe(0);
    expect(messageService.markConversationAsRead).toHaveBeenCalledWith(recipient.id);
  });

  it('shows load errors', async () => {
    await configureMessagesPage();
    messageService.listMessages.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Messages unavailable.' } })),
    );

    fixture.componentInstance.loadMessages();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Messages unavailable.');
    expect(fixture.componentInstance.isLoading()).toBe(false);
  });

  it('does not send invalid messages', async () => {
    await configureMessagesPage();

    fixture.componentInstance.sendMessage();
    fixture.detectChanges();

    expect(messageService.sendMessage).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Message content is required and must not exceed 5000 characters.');
  });

  it('selects a recipient and sends a trimmed message', async () => {
    await configureMessagesPage();

    fixture.componentInstance.selectRecipient(recipient);
    fixture.componentInstance.messageForm.controls.content.setValue('  Hello there  ');
    fixture.componentInstance.sendMessage();

    expect(messageService.sendMessage).toHaveBeenCalledWith({
      recipientId: recipient.id,
      content: 'Hello there',
    });
    expect(fixture.componentInstance.messages()[0]).toEqual(outgoingMessage);
    expect(fixture.componentInstance.messageForm.controls.content.value).toBe('');
  });

  it('searches recipients after debounce', async () => {
    vi.useFakeTimers();
    await configureMessagesPage();

    fixture.componentInstance.clearRecipient();
    fixture.componentInstance.messageForm.controls.recipientQuery.setValue('mi');
    await vi.advanceTimersByTimeAsync(250);

    expect(userService.searchUsers).toHaveBeenCalledWith('mi');
    expect(fixture.componentInstance.userResults()).toEqual([recipient]);
  });

  it('loads target message when it is not in the first page', async () => {
    await configureMessagesPage('message-2');

    expect(messageService.getMessageById).toHaveBeenCalledWith('message-2');
    expect(fixture.componentInstance.messages()[0]).toEqual(outgoingMessage);
    expect(fixture.componentInstance.isTargetMessage(outgoingMessage)).toBe(true);
  });

  it('reports a received message', async () => {
    await configureMessagesPage();

    fixture.componentInstance.toggleReportForm(incomingMessage);
    fixture.componentInstance.updateReportReason(incomingMessage.id, { target: { value: 'Spam' } } as unknown as Event);
    fixture.componentInstance.updateReportDetails(incomingMessage.id, {
      target: { value: 'Suspicious message' },
    } as unknown as Event);
    fixture.componentInstance.submitReport(incomingMessage);

    expect(reportService.createReport).toHaveBeenCalledWith({
      targetType: 'MESSAGE',
      targetId: incomingMessage.id,
      reason: 'Spam',
      details: 'Suspicious message',
    });
    expect(fixture.componentInstance.reportedMessageIds()[incomingMessage.id]).toBe(true);
    expect(fixture.componentInstance.reportFormOpenByMessageId()[incomingMessage.id]).toBe(false);
  });

  it('does not report own messages', async () => {
    await configureMessagesPage();

    fixture.componentInstance.toggleReportForm(outgoingMessage);
    fixture.componentInstance.submitReport(outgoingMessage);

    expect(reportService.createReport).not.toHaveBeenCalled();
    expect(fixture.componentInstance.reportFormOpenByMessageId()[outgoingMessage.id]).toBeUndefined();
  });
});

function createUser(id: string, username: string): User {
  return {
    id,
    username,
    email: `${username}@example.com`,
    status: 'ACTIVE',
    roles: ['USER'],
    createdAt: '2026-07-25T10:00:00Z',
  };
}

function createUserSummary(id: string, username: string, displayName: string | null): UserSummary {
  return {
    id,
    username,
    displayName,
    avatarUrl: null,
  };
}

function userSummary(user: User): UserSummary {
  return createUserSummary(user.id, user.username, user.username);
}

function createMessage(id: string, sender: UserSummary, recipient: UserSummary, content: string): Message {
  return {
    id,
    sender,
    recipient,
    content,
    status: 'SENT',
    sentAt: '2026-07-25T10:00:00Z',
    deliveredAt: null,
    readAt: null,
  };
}
