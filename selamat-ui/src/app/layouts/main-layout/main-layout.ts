import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService, NotificationService } from '../../core/services';

@Component({
  selector: 'app-main-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;
  readonly unreadNotificationCount = this.notificationService.unreadCount;
  readonly isLoggingOut = signal(false);

  constructor() {
    this.notificationService.loadUnreadCount().subscribe({
      error: () => undefined,
    });
  }

  canAccessAdmin(): boolean {
    return this.currentUser()?.roles.some((role) => role === 'ADMIN' || role === 'MODERATOR') ?? false;
  }

  logout(): void {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);

    this.authService
      .logout()
      .pipe(
        finalize(() => {
          this.authService.clearSession();
          this.isLoggingOut.set(false);
          void this.router.navigateByUrl('/login');
        }),
      )
      .subscribe({
        error: () => undefined,
      });
  }
}
