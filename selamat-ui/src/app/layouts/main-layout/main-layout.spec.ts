import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService, NotificationService } from '../../core/services';
import { User } from '../../models';
import { MainLayout } from './main-layout';

describe('MainLayout', () => {
  let fixture: ComponentFixture<MainLayout>;
  let authService: {
    currentUser: ReturnType<typeof signal<User | null>>;
    logout: ReturnType<typeof vi.fn>;
    clearSession: ReturnType<typeof vi.fn>;
  };
  let notificationService: {
    unreadCount: ReturnType<typeof signal<number>>;
    loadUnreadCount: ReturnType<typeof vi.fn>;
  };

  const createUser = (roles: string[] = ['USER']): User => ({
    id: 'user-1',
    username: 'selam',
    email: 'selam@example.com',
    status: 'ACTIVE',
    roles,
    createdAt: '2026-07-24T12:00:00Z',
  });

  async function configureLayout(user: User | null = createUser(), unreadCount = 0): Promise<void> {
    authService = {
      currentUser: signal(user),
      logout: vi.fn(() => of(undefined)),
      clearSession: vi.fn(),
    };
    notificationService = {
      unreadCount: signal(unreadCount),
      loadUnreadCount: vi.fn(() => of({ unreadCount })),
    };

    await TestBed.configureTestingModule({
      imports: [MainLayout],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: NotificationService, useValue: notificationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MainLayout);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('loads unread notification count when created', async () => {
    await configureLayout();

    expect(notificationService.loadUnreadCount).toHaveBeenCalledOnce();
  });

  it('hides admin navigation for normal users', async () => {
    await configureLayout(createUser(['USER']));

    expect(fixture.nativeElement.textContent).not.toContain('Admin');
  });

  it('shows admin navigation for admins', async () => {
    await configureLayout(createUser(['ADMIN']));

    expect(fixture.nativeElement.textContent).toContain('Admin');
  });

  it('links admin navigation to the admin dashboard', async () => {
    await configureLayout(createUser(['ADMIN']));

    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    const adminLink = links.find((link) => link.textContent?.trim() === 'Admin');

    expect(adminLink?.getAttribute('href')).toBe('/admin');
  });

  it('shows admin navigation for moderators', async () => {
    await configureLayout(createUser(['MODERATOR']));

    expect(fixture.nativeElement.textContent).toContain('Admin');
  });

  it('shows capped unread notification count', async () => {
    await configureLayout(createUser(), 120);

    const badge = fixture.nativeElement.querySelector('.badge') as HTMLElement | null;

    expect(badge?.textContent?.trim()).toBe('99+');
  });

  it('logs out, clears session, and redirects to login', async () => {
    await configureLayout();
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.logout();

    expect(authService.logout).toHaveBeenCalledOnce();
    expect(authService.clearSession).toHaveBeenCalledOnce();
    expect(navigateByUrl).toHaveBeenCalledWith('/login');
    expect(fixture.componentInstance.isLoggingOut()).toBe(false);
  });

  it('still clears session and redirects when logout request fails', async () => {
    await configureLayout();
    authService.logout.mockReturnValue(throwError(() => new Error('logout failed')));
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.logout();

    expect(authService.clearSession).toHaveBeenCalledOnce();
    expect(navigateByUrl).toHaveBeenCalledWith('/login');
    expect(fixture.componentInstance.isLoggingOut()).toBe(false);
  });
});
