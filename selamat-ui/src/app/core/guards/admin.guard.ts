import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.currentUser();

  return user?.roles.some((role) => role === 'ADMIN' || role === 'MODERATOR') ? true : router.createUrlTree(['/feed']);
};
