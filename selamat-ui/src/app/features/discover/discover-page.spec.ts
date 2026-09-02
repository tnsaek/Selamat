import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { FollowService, UserService } from '../../core/services';
import { UserSummary } from '../../models';
import { DiscoverPage } from './discover-page';

describe('DiscoverPage', () => {
  let fixture: ComponentFixture<DiscoverPage>;
  let followService: {
    followUser: ReturnType<typeof vi.fn>;
    getFollowStatus: ReturnType<typeof vi.fn>;
    unfollowUser: ReturnType<typeof vi.fn>;
  };
  let userService: {
    searchUsers: ReturnType<typeof vi.fn>;
  };

  const users: UserSummary[] = [
    {
      id: 'user-1',
      username: 'selam',
      displayName: 'Selam Tesfaye',
      avatarUrl: null,
    },
    {
      id: 'user-2',
      username: 'mira',
      displayName: 'Mira Alem',
      avatarUrl: 'https://example.com/mira.png',
    },
  ];

  beforeEach(async () => {
    vi.clearAllMocks();
    vi.useRealTimers();
    TestBed.resetTestingModule();

    followService = {
      followUser: vi.fn(() => of({ id: 'follow-1' })),
      getFollowStatus: vi.fn((userId: string) => of({ following: userId === 'user-2' })),
      unfollowUser: vi.fn(() => of(undefined)),
    };
    userService = {
      searchUsers: vi.fn(() => of(users)),
    };

    await TestBed.configureTestingModule({
      imports: [DiscoverPage],
      providers: [
        provideRouter([]),
        { provide: FollowService, useValue: followService },
        { provide: UserService, useValue: userService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DiscoverPage);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows the initial empty search state', () => {
    expect(fixture.nativeElement.textContent).toContain('Type at least two characters to search for users.');
    expect(userService.searchUsers).not.toHaveBeenCalled();
  });

  it('searches users and loads follow statuses', async () => {
    vi.useFakeTimers();

    fixture.componentInstance.searchForm.controls.query.setValue(' se ');

    await vi.advanceTimersByTimeAsync(250);
    fixture.detectChanges();

    expect(userService.searchUsers).toHaveBeenCalledWith('se', 20);
    expect(followService.getFollowStatus).toHaveBeenCalledWith('user-1');
    expect(followService.getFollowStatus).toHaveBeenCalledWith('user-2');
    expect(fixture.componentInstance.users()).toEqual(users);
    expect(fixture.componentInstance.followingByUserId()).toEqual({
      'user-1': false,
      'user-2': true,
    });
    expect(fixture.nativeElement.textContent).toContain('Selam Tesfaye');
    expect(fixture.nativeElement.textContent).toContain('Mira Alem');
  });

  it('clears users when search query is too short', async () => {
    vi.useFakeTimers();
    fixture.componentInstance.users.set(users);
    fixture.componentInstance.followingByUserId.set({ 'user-1': true });

    fixture.componentInstance.searchForm.controls.query.setValue('s');

    await vi.advanceTimersByTimeAsync(250);
    fixture.detectChanges();

    expect(fixture.componentInstance.users()).toEqual([]);
    expect(fixture.componentInstance.followingByUserId()).toEqual({});
    expect(userService.searchUsers).not.toHaveBeenCalled();
  });

  it('shows empty results message when search returns no users', async () => {
    vi.useFakeTimers();
    userService.searchUsers.mockReturnValue(of([]));

    fixture.componentInstance.searchForm.controls.query.setValue('unknown');

    await vi.advanceTimersByTimeAsync(250);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No users found');
  });

  it('shows search errors', async () => {
    vi.useFakeTimers();
    userService.searchUsers.mockReturnValue(
      throwError(() => ({ error: { message: 'Search service unavailable.' } })),
    );

    fixture.componentInstance.searchForm.controls.query.setValue('selam');

    await vi.advanceTimersByTimeAsync(250);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Search service unavailable.');
    expect(fixture.componentInstance.users()).toEqual([]);
  });

  it('follows and unfollows a user', () => {
    const user = users[0];

    fixture.componentInstance.followUser(user);
    expect(followService.followUser).toHaveBeenCalledWith(user.id);
    expect(fixture.componentInstance.followingByUserId()[user.id]).toBe(true);

    fixture.componentInstance.unfollowUser(user);
    expect(followService.unfollowUser).toHaveBeenCalledWith(user.id);
    expect(fixture.componentInstance.followingByUserId()[user.id]).toBe(false);
  });

  it('marks user as followed when backend says already following', () => {
    const user = users[0];
    followService.followUser.mockReturnValue(
      throwError(() => ({ error: { message: 'Already following this user.' } })),
    );

    fixture.componentInstance.followUser(user);

    expect(fixture.componentInstance.followingByUserId()[user.id]).toBe(true);
    expect(fixture.componentInstance.errorMessage()).toBeNull();
  });

  it('shows follow errors that are not already-following conflicts', () => {
    const user = users[0];
    followService.followUser.mockReturnValue(throwError(() => ({ error: { message: 'Cannot follow user.' } })));

    fixture.componentInstance.followUser(user);

    expect(fixture.nativeElement.textContent).not.toContain('Cannot follow user.');
    expect(fixture.componentInstance.errorMessage()).toBe('Cannot follow user.');
  });
});
