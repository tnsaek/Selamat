import { Injectable, effect, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

const INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000;

@Injectable({
  providedIn: 'root',
})
export class SessionTimeoutService {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly activityEvents = ['click', 'keydown', 'mousemove', 'mousedown', 'scroll', 'touchstart'] as const;
  private timeoutId: number | null = null;
  private started = false;

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        this.resetTimer();
      } else {
        this.clearTimer();
      }
    });
  }

  start(): void {
    if (this.started) {
      return;
    }

    this.started = true;
    this.activityEvents.forEach((eventName) =>
      window.addEventListener(eventName, this.handleActivity, { passive: true }),
    );

    if (this.authService.isAuthenticated()) {
      this.resetTimer();
    }
  }

  private readonly handleActivity = (): void => {
    if (this.authService.isAuthenticated()) {
      this.resetTimer();
    }
  };

  private resetTimer(): void {
    this.clearTimer();
    this.timeoutId = window.setTimeout(() => this.logoutForInactivity(), INACTIVITY_TIMEOUT_MS);
  }

  private clearTimer(): void {
    if (this.timeoutId === null) {
      return;
    }

    window.clearTimeout(this.timeoutId);
    this.timeoutId = null;
  }

  private logoutForInactivity(): void {
    this.authService.clearSession();
    void this.router.navigate(['/login'], {
      queryParams: {
        reason: 'inactive',
      },
    });
  }
}
