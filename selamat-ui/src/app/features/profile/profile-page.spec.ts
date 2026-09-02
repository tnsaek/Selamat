import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { FollowService, MediaService, ProfileService, UserService } from '../../core/services';
import { Profile, User } from '../../models';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  let fixture: ComponentFixture<ProfilePage>;
  let queryParamMap: BehaviorSubject<ParamMap>;
  let followService: {
    followUser: ReturnType<typeof vi.fn>;
    getFollowStatus: ReturnType<typeof vi.fn>;
    unfollowUser: ReturnType<typeof vi.fn>;
  };
  let profileService: {
    getProfile: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
  };
  let mediaService: {
    uploadMedia: ReturnType<typeof vi.fn>;
  };
  let userService: {
    getCurrentUser: ReturnType<typeof vi.fn>;
    getUserById: ReturnType<typeof vi.fn>;
  };

  const currentUser = createUser('user-1', 'selam', ['USER']);
  const viewedUser = createUser('user-2', 'mira', ['USER']);
  const currentProfile = createProfile('profile-1', currentUser.id, 'Selam Tesfaye');
  const viewedProfile = createProfile('profile-2', viewedUser.id, 'Mira Alem');

  async function configureProfilePage(targetUserId: string | null = null): Promise<void> {
    queryParamMap = new BehaviorSubject(convertToParamMap(targetUserId ? { userId: targetUserId } : {}));
    followService = {
      followUser: vi.fn(() => of({ id: 'follow-1' })),
      getFollowStatus: vi.fn(() => of({ following: false })),
      unfollowUser: vi.fn(() => of(undefined)),
    };
    profileService = {
      getProfile: vi.fn((userId: string) => of(userId === currentUser.id ? currentProfile : viewedProfile)),
      updateProfile: vi.fn(() => of({ ...currentProfile, displayName: 'Updated Name' })),
    };
    mediaService = {
      uploadMedia: vi.fn(() =>
        of({
          id: 'media-1',
          url: 'https://example.com/uploaded.png',
          mediaType: 'IMAGE',
          mimeType: 'image/png',
          sizeBytes: 1200,
          altText: null,
          createdAt: '2026-07-29T10:00:00Z',
        }),
      ),
    };
    userService = {
      getCurrentUser: vi.fn(() => of(currentUser)),
      getUserById: vi.fn(() => of(viewedUser)),
    };

    await TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap } },
        { provide: FollowService, useValue: followService },
        { provide: MediaService, useValue: mediaService },
        { provide: ProfileService, useValue: profileService },
        { provide: UserService, useValue: userService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfilePage);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('loads and enables the current user profile form', async () => {
    await configureProfilePage();

    expect(userService.getCurrentUser).toHaveBeenCalledOnce();
    expect(profileService.getProfile).toHaveBeenCalledWith(currentUser.id);
    expect(followService.getFollowStatus).not.toHaveBeenCalled();
    expect(fixture.componentInstance.isOwnProfile()).toBe(true);
    expect(fixture.componentInstance.profileForm.enabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Profile');
    expect(fixture.nativeElement.textContent).toContain('selam@example.com');
  });

  it('renders avatar and cover images when profile URLs are present', async () => {
    await configureProfilePage();
    const profileWithImages = {
      ...currentProfile,
      avatarUrl: 'https://example.com/avatar.png',
      coverImageUrl: 'https://example.com/cover.png',
    };
    profileService.getProfile.mockReturnValueOnce(of(profileWithImages));

    queryParamMap.next(convertToParamMap({}));
    fixture.detectChanges();

    const images = Array.from(fixture.nativeElement.querySelectorAll('img')) as HTMLImageElement[];
    expect(images.map((image) => image.src)).toContain('https://example.com/cover.png');
    expect(images.map((image) => image.src)).toContain('https://example.com/avatar.png');
  });

  it('loads another user profile and disables editing', async () => {
    await configureProfilePage(viewedUser.id);

    expect(userService.getUserById).toHaveBeenCalledWith(viewedUser.id);
    expect(profileService.getProfile).toHaveBeenCalledWith(viewedUser.id);
    expect(followService.getFollowStatus).toHaveBeenCalledWith(viewedUser.id);
    expect(fixture.componentInstance.isOwnProfile()).toBe(false);
    expect(fixture.componentInstance.profileForm.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('@mira');
    expect(fixture.nativeElement.textContent).toContain('Bole Road, Addis Ababa, Addis Ababa, Ethiopia');
  });

  it('saves own profile with trimmed values and nulls blank fields', async () => {
    await configureProfilePage();

    fixture.componentInstance.profileForm.setValue({
      displayName: '  Updated Name  ',
      bio: '  New bio  ',
      avatarUrl: '  ',
      coverImageUrl: '',
      street: ' Bole Road ',
      city: ' Addis Ababa ',
      state: ' Addis Ababa ',
      country: ' Ethiopia ',
      websiteUrl: ' https://example.com ',
    });
    fixture.componentInstance.saveProfile();
    fixture.detectChanges();

    expect(profileService.updateProfile).toHaveBeenCalledWith(currentUser.id, {
      displayName: 'Updated Name',
      bio: 'New bio',
      avatarUrl: null,
      coverImageUrl: null,
      street: 'Bole Road',
      city: 'Addis Ababa',
      state: 'Addis Ababa',
      country: 'Ethiopia',
      websiteUrl: 'https://example.com',
    });
    expect(fixture.nativeElement.textContent).toContain('Profile updated successfully.');
  });

  it('uploads profile and cover images into the form and preview', async () => {
    await configureProfilePage();
    const profileFile = new File(['profile'], 'profile.png', { type: 'image/png' });
    const coverFile = new File(['cover'], 'cover.png', { type: 'image/png' });

    fixture.componentInstance.onProfileImageSelected(fileEvent(profileFile));
    fixture.componentInstance.onCoverImageSelected(fileEvent(coverFile));
    fixture.detectChanges();

    expect(mediaService.uploadMedia).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.profileForm.controls.avatarUrl.value).toBe('https://example.com/uploaded.png');
    expect(fixture.componentInstance.profileForm.controls.coverImageUrl.value).toBe('https://example.com/uploaded.png');
    expect(fixture.componentInstance.profile()?.avatarUrl).toBe('https://example.com/uploaded.png');
    expect(fixture.componentInstance.profile()?.coverImageUrl).toBe('https://example.com/uploaded.png');
  });

  it('rejects oversized profile images before upload', async () => {
    await configureProfilePage();
    const oversizedFile = new File(['x'.repeat(10 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' });

    fixture.componentInstance.onProfileImageSelected(fileEvent(oversizedFile));
    fixture.detectChanges();

    expect(mediaService.uploadMedia).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Image must not exceed 10 MB.');
  });

  it('does not save another user profile', async () => {
    await configureProfilePage(viewedUser.id);

    fixture.componentInstance.saveProfile();

    expect(profileService.updateProfile).not.toHaveBeenCalled();
  });

  it('shows profile load errors', async () => {
    await configureProfilePage();
    profileService.getProfile.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Profile unavailable.' } })),
    );

    queryParamMap.next(convertToParamMap({}));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Profile unavailable.');
    expect(fixture.componentInstance.isLoading()).toBe(false);
  });

  it('follows and unfollows a viewed user', async () => {
    await configureProfilePage(viewedUser.id);

    fixture.componentInstance.followViewedUser();
    expect(followService.followUser).toHaveBeenCalledWith(viewedUser.id);
    expect(fixture.componentInstance.isFollowingViewedUser()).toBe(true);

    fixture.componentInstance.unfollowViewedUser();
    expect(followService.unfollowUser).toHaveBeenCalledWith(viewedUser.id);
    expect(fixture.componentInstance.isFollowingViewedUser()).toBe(false);
  });

  it('marks invalid own profile form and does not save', async () => {
    await configureProfilePage();

    fixture.componentInstance.profileForm.controls.displayName.setValue('x'.repeat(101));
    fixture.componentInstance.saveProfile();
    fixture.detectChanges();

    expect(profileService.updateProfile).not.toHaveBeenCalled();
    expect(fixture.componentInstance.profileForm.controls.displayName.touched).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Display name must not exceed 100 characters.');
  });

  it('handles profile save errors and fallback error messages', async () => {
    await configureProfilePage();
    profileService.updateProfile.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    ).mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 400 })),
    );

    fixture.componentInstance.saveProfile();
    expect(fixture.componentInstance.errorMessage()).toBe('Cannot reach the backend. Check that the API is running.');

    fixture.componentInstance.saveProfile();
    expect(fixture.componentInstance.errorMessage()).toBe('Profile request failed with status 400.');
  });

  it('handles follow status and follow action errors', async () => {
    await configureProfilePage(viewedUser.id);
    followService.getFollowStatus.mockReturnValueOnce(throwError(() => new Error('ignored')));

    queryParamMap.next(convertToParamMap({ userId: viewedUser.id }));
    fixture.detectChanges();

    expect(fixture.componentInstance.isFollowingViewedUser()).toBe(false);

    followService.followUser.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'You are already following this user.' } })),
    );
    fixture.componentInstance.followViewedUser();

    expect(fixture.componentInstance.isFollowingViewedUser()).toBe(true);
    expect(fixture.componentInstance.errorMessage()).toBe('You are already following this user.');

    followService.unfollowUser.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Unable to unfollow.' } })),
    );
    fixture.componentInstance.unfollowViewedUser();

    expect(fixture.componentInstance.errorMessage()).toBe('Unable to unfollow.');
  });

  it('rejects non-image uploads, empty file events, upload failures, and missing preview profiles', async () => {
    await configureProfilePage();
    const textFile = new File(['text'], 'note.txt', { type: 'text/plain' });

    fixture.componentInstance.onProfileImageSelected(fileEvent(null));
    fixture.componentInstance.onProfileImageSelected(fileEvent(textFile));
    fixture.detectChanges();

    expect(mediaService.uploadMedia).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Select an image file.');

    mediaService.uploadMedia.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Upload failed.' } })),
    );
    fixture.componentInstance.onCoverImageSelected(fileEvent(new File(['cover'], 'cover.png', { type: 'image/png' })));
    expect(fixture.componentInstance.errorMessage()).toBe('Upload failed.');

    fixture.componentInstance.profile.set(null);
    mediaService.uploadMedia.mockReturnValueOnce(
      of({
        id: 'media-2',
        url: 'https://example.com/late.png',
        mediaType: 'IMAGE',
        mimeType: 'image/png',
        sizeBytes: 1200,
        altText: null,
        createdAt: '2026-07-29T10:00:00Z',
      }),
    );
    fixture.componentInstance.onProfileImageSelected(fileEvent(new File(['profile'], 'profile.png', { type: 'image/png' })));

    expect(fixture.componentInstance.profile()).toBeNull();
    expect(fixture.componentInstance.profileAddress(null)).toBeNull();
    expect(fixture.componentInstance.profileAddress({ ...currentProfile, street: ' ', city: null, state: '', country: null })).toBeNull();
  });
});

function fileEvent(file: File | null): Event {
  return {
    target: {
      files: file ? [file] : [],
      value: 'selected-file',
    },
  } as unknown as Event;
}

function createUser(id: string, username: string, roles: string[]): User {
  return {
    id,
    username,
    email: `${username}@example.com`,
    status: 'ACTIVE',
    roles,
    createdAt: '2026-07-25T10:00:00Z',
  };
}

function createProfile(id: string, userId: string, displayName: string): Profile {
  return {
    id,
    userId,
    displayName,
    bio: 'Profile bio',
    avatarUrl: null,
    coverImageUrl: null,
    street: 'Bole Road',
    city: 'Addis Ababa',
    state: 'Addis Ababa',
    country: 'Ethiopia',
    websiteUrl: 'https://example.com',
  };
}
