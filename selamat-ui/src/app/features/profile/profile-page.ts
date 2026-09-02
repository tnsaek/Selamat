import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, WritableSignal, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, forkJoin, map, of, switchMap } from 'rxjs';

import { FollowService, MediaService, ProfileService, UserService } from '../../core/services';
import { ErrorResponse, Profile, UpdateProfileRequest, User, UUID } from '../../models';

const MAX_PROFILE_IMAGE_BYTES = 10 * 1024 * 1024;

@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly followService = inject(FollowService);
  private readonly mediaService = inject(MediaService);
  private readonly profileService = inject(ProfileService);
  private readonly userService = inject(UserService);

  readonly currentUser = signal<User | null>(null);
  readonly viewedUser = signal<User | null>(null);
  readonly profile = signal<Profile | null>(null);
  readonly isOwnProfile = computed(() => this.currentUser()?.id === this.viewedUser()?.id);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isProfileImageUploading = signal(false);
  readonly isCoverImageUploading = signal(false);
  readonly isFollowChanging = signal(false);
  readonly isFollowingViewedUser = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly profileForm = this.formBuilder.nonNullable.group({
    displayName: ['', [Validators.maxLength(100)]],
    bio: ['', [Validators.maxLength(500)]],
    avatarUrl: ['', [Validators.maxLength(500)]],
    coverImageUrl: ['', [Validators.maxLength(500)]],
    street: ['', [Validators.maxLength(255)]],
    city: ['', [Validators.maxLength(120)]],
    state: ['', [Validators.maxLength(120)]],
    country: ['', [Validators.maxLength(120)]],
    websiteUrl: ['', [Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.loadProfile(params.get('userId'));
    });
  }

  saveProfile(): void {
    const user = this.viewedUser();

    if (!user || !this.isOwnProfile()) {
      return;
    }

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.profileService
      .updateProfile(user.id, this.profileRequest())
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.patchForm(profile);
          this.successMessage.set('Profile updated successfully.');
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  isInvalid(controlName: keyof typeof this.profileForm.controls): boolean {
    const control = this.profileForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  profileAddress(profile: Profile | null): string | null {
    if (!profile) {
      return null;
    }

    const address = [profile.street, profile.city, profile.state, profile.country]
      .map((part) => part?.trim())
      .filter((part): part is string => !!part)
      .join(', ');

    return address || null;
  }

  followViewedUser(): void {
    const user = this.viewedUser();
    if (!user || this.isOwnProfile() || this.isFollowChanging()) {
      return;
    }

    this.isFollowChanging.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.followService
      .followUser(user.id)
      .pipe(finalize(() => this.isFollowChanging.set(false)))
      .subscribe({
        next: () => {
          this.isFollowingViewedUser.set(true);
          this.successMessage.set(`You are now following @${user.username}.`);
        },
        error: (error: HttpErrorResponse) => {
          const message = this.errorText(error);
          if (message.toLowerCase().includes('already following')) {
            this.isFollowingViewedUser.set(true);
          }
          this.errorMessage.set(message);
        },
      });
  }

  unfollowViewedUser(): void {
    const user = this.viewedUser();
    if (!user || this.isOwnProfile() || this.isFollowChanging()) {
      return;
    }

    this.isFollowChanging.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.followService
      .unfollowUser(user.id)
      .pipe(finalize(() => this.isFollowChanging.set(false)))
      .subscribe({
        next: () => {
          this.isFollowingViewedUser.set(false);
          this.successMessage.set(`You unfollowed @${user.username}.`);
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  onProfileImageSelected(event: Event): void {
    this.uploadSelectedImage(event, 'avatarUrl', this.isProfileImageUploading, 'Profile image uploaded.');
  }

  onCoverImageSelected(event: Event): void {
    this.uploadSelectedImage(event, 'coverImageUrl', this.isCoverImageUploading, 'Cover image uploaded.');
  }

  private loadProfile(targetUserId: UUID | null): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.userService
      .getCurrentUser()
      .pipe(
        switchMap((currentUser) => {
          const viewedUserId = targetUserId ?? currentUser.id;
          const viewedUser$ =
            viewedUserId === currentUser.id ? of(currentUser) : this.userService.getUserById(viewedUserId);

          return forkJoin({
            currentUser: of(currentUser),
            viewedUser: viewedUser$,
            profile: this.profileService.getProfile(viewedUserId),
            followStatus:
              viewedUserId === currentUser.id
                ? of({ following: false })
                : this.followService.getFollowStatus(viewedUserId).pipe(catchError(() => of({ following: false }))),
          });
        }),
        map(({ currentUser, viewedUser, profile, followStatus }) => ({
          currentUser,
          viewedUser,
          profile,
          followStatus,
        })),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: ({ currentUser, viewedUser, profile, followStatus }) => {
          this.currentUser.set(currentUser);
          this.viewedUser.set(viewedUser);
          this.profile.set(profile);
          this.isFollowingViewedUser.set(followStatus.following);
          this.patchForm(profile);

          if (currentUser.id === viewedUser.id) {
            this.profileForm.enable();
          } else {
            this.profileForm.disable();
          }
        },
        error: (error: HttpErrorResponse) => this.errorMessage.set(this.errorText(error)),
      });
  }

  private patchForm(profile: Profile): void {
    this.profileForm.reset({
      displayName: profile.displayName ?? '',
      bio: profile.bio ?? '',
      avatarUrl: profile.avatarUrl ?? '',
      coverImageUrl: profile.coverImageUrl ?? '',
      street: profile.street ?? '',
      city: profile.city ?? '',
      state: profile.state ?? '',
      country: profile.country ?? '',
      websiteUrl: profile.websiteUrl ?? '',
    });
  }

  private profileRequest(): UpdateProfileRequest {
    const rawValue = this.profileForm.getRawValue();
    return {
      displayName: this.nullIfBlank(rawValue.displayName),
      bio: this.nullIfBlank(rawValue.bio),
      avatarUrl: this.nullIfBlank(rawValue.avatarUrl),
      coverImageUrl: this.nullIfBlank(rawValue.coverImageUrl),
      street: this.nullIfBlank(rawValue.street),
      city: this.nullIfBlank(rawValue.city),
      state: this.nullIfBlank(rawValue.state),
      country: this.nullIfBlank(rawValue.country),
      websiteUrl: this.nullIfBlank(rawValue.websiteUrl),
    };
  }

  private nullIfBlank(value: string): string | null {
    const trimmed = value.trim().replace(/^["']|["']$/g, '');
    return trimmed.length > 0 ? trimmed : null;
  }

  private uploadSelectedImage(
    event: Event,
    controlName: 'avatarUrl' | 'coverImageUrl',
    uploading: WritableSignal<boolean>,
    successMessage: string,
  ): void {
    if (!this.isOwnProfile()) {
      return;
    }

    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.errorMessage.set('Select an image file.');
      input.value = '';
      return;
    }

    if (file.size > MAX_PROFILE_IMAGE_BYTES) {
      this.errorMessage.set('Image must not exceed 10 MB.');
      input.value = '';
      return;
    }

    uploading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.mediaService
      .uploadMedia({ file, altText: this.viewedUser()?.username ?? null })
      .pipe(finalize(() => uploading.set(false)))
      .subscribe({
        next: (media) => {
          this.profileForm.controls[controlName].setValue(media.url);
          this.updateProfilePreview(controlName, media.url);
          this.successMessage.set(`${successMessage} Save profile to keep this change.`);
          input.value = '';
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(this.errorText(error));
          input.value = '';
        },
      });
  }

  private updateProfilePreview(controlName: 'avatarUrl' | 'coverImageUrl', url: string): void {
    const profile = this.profile();
    if (!profile) {
      return;
    }

    this.profile.set({
      ...profile,
      [controlName]: url,
    });
  }

  private errorText(error: HttpErrorResponse): string {
    const response = error.error as Partial<ErrorResponse> | undefined;
    if (response?.message) {
      return response.message;
    }

    if (error.status === 0) {
      return 'Cannot reach the backend. Check that the API is running.';
    }

    return `Profile request failed with status ${error.status}.`;
  }
}
