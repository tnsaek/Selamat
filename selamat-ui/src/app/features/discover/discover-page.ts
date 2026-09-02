import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, debounceTime, distinctUntilChanged, finalize, forkJoin, map, of, switchMap, tap } from 'rxjs';

import { FollowService, UserService } from '../../core/services';
import { ErrorResponse, UserSummary, UUID } from '../../models';

@Component({
  selector: 'app-discover-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './discover-page.html',
  styleUrl: './discover-page.scss',
})
export class DiscoverPage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly followService = inject(FollowService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly userService = inject(UserService);

  readonly users = signal<UserSummary[]>([]);
  readonly followingByUserId = signal<Record<UUID, boolean>>({});
  readonly changingFollowByUserId = signal<Record<UUID, boolean>>({});
  readonly isSearching = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly searchForm = this.formBuilder.nonNullable.group({
    query: [''],
  });

  ngOnInit(): void {
    this.watchSearch();
  }

  followUser(user: UserSummary): void {
    if (this.changingFollowByUserId()[user.id]) {
      return;
    }

    this.setChangingFollow(user.id, true);
    this.errorMessage.set(null);

    this.followService
      .followUser(user.id)
      .pipe(finalize(() => this.setChangingFollow(user.id, false)))
      .subscribe({
        next: () => this.setFollowing(user.id, true),
        error: (error) => {
          const message = this.errorText(error);
          if (message.toLowerCase().includes('already following')) {
            this.setFollowing(user.id, true);
            return;
          }
          this.errorMessage.set(message);
        },
      });
  }

  unfollowUser(user: UserSummary): void {
    if (this.changingFollowByUserId()[user.id]) {
      return;
    }

    this.setChangingFollow(user.id, true);
    this.errorMessage.set(null);

    this.followService
      .unfollowUser(user.id)
      .pipe(finalize(() => this.setChangingFollow(user.id, false)))
      .subscribe({
        next: () => this.setFollowing(user.id, false),
        error: (error) => this.errorMessage.set(this.errorText(error)),
      });
  }

  trackUser(_: number, user: UserSummary): UUID {
    return user.id;
  }

  private watchSearch(): void {
    this.searchForm.controls.query.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap(() => this.errorMessage.set(null)),
        switchMap((query) => {
          const normalizedQuery = query.trim();
          if (normalizedQuery.length < 2) {
            this.users.set([]);
            this.followingByUserId.set({});
            this.isSearching.set(false);
            return of([]);
          }

          this.isSearching.set(true);
          return this.userService.searchUsers(normalizedQuery, 20).pipe(
            switchMap((users) => this.withFollowStatuses(users)),
            catchError((error) => {
              this.errorMessage.set(this.errorText(error));
              return of([]);
            }),
            finalize(() => this.isSearching.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((users) => this.users.set(users));
  }

  private withFollowStatuses(users: UserSummary[]) {
    if (users.length === 0) {
      this.followingByUserId.set({});
      return of(users);
    }

    const statusRequests = users.map((user) =>
      this.followService.getFollowStatus(user.id).pipe(
        map((status) => [user.id, status.following] as const),
        catchError(() => of([user.id, false] as const)),
      ),
    );

    return forkJoin(statusRequests).pipe(
      tap((statuses) => {
        this.followingByUserId.set(Object.fromEntries(statuses));
      }),
      map(() => users),
    );
  }

  private setFollowing(userId: UUID, following: boolean): void {
    this.followingByUserId.update((statuses) => ({
      ...statuses,
      [userId]: following,
    }));
  }

  private setChangingFollow(userId: UUID, changing: boolean): void {
    this.changingFollowByUserId.update((statuses) => ({
      ...statuses,
      [userId]: changing,
    }));
  }

  private errorText(error: unknown): string {
    if (this.hasErrorMessage(error)) {
      return error.error.message;
    }

    return 'Unable to search users. Try again.';
  }

  private hasErrorMessage(error: unknown): error is { error: ErrorResponse } {
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
