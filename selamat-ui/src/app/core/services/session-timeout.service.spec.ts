import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from './auth.service';
import { SessionTimeoutService } from './session-timeout.service';

describe('SessionTimeoutService', () => {
  const authenticated = signal(false);
  const authService = {
    isAuthenticated: authenticated.asReadonly(),
    clearSession: vi.fn(),
  };
  const router = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    authenticated.set(false);
    TestBed.configureTestingModule({
      providers: [
        SessionTimeoutService,
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('does not attach duplicate activity listeners when started twice', () => {
    const addEventListener = vi.spyOn(window, 'addEventListener');
    const service = TestBed.inject(SessionTimeoutService);

    service.start();
    service.start();

    expect(addEventListener).toHaveBeenCalledTimes(6);
  });

  it('logs out and routes to login after the inactivity timeout', () => {
    authenticated.set(true);
    const service = TestBed.inject(SessionTimeoutService);

    service.start();
    vi.advanceTimersByTime(15 * 60 * 1000);

    expect(authService.clearSession).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: {
        reason: 'inactive',
      },
    });
  });

  it('resets the inactivity timer when authenticated activity occurs', () => {
    authenticated.set(true);
    const service = TestBed.inject(SessionTimeoutService);

    service.start();
    vi.advanceTimersByTime(10 * 60 * 1000);
    (service as unknown as { handleActivity: () => void }).handleActivity();
    vi.advanceTimersByTime(10 * 60 * 1000);

    expect(authService.clearSession).not.toHaveBeenCalled();

    vi.advanceTimersByTime(5 * 60 * 1000);

    expect(authService.clearSession).toHaveBeenCalledOnce();
  });

  it('ignores activity when the user is not authenticated', () => {
    const service = TestBed.inject(SessionTimeoutService);

    service.start();
    (service as unknown as { handleActivity: () => void }).handleActivity();
    vi.advanceTimersByTime(15 * 60 * 1000);

    expect(authService.clearSession).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
