import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard } from './core/guards';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { MainLayout } from './layouts/main-layout/main-layout';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'feed',
  },
  {
    path: '',
    component: AuthLayout,
    canActivateChild: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
      },
      {
        path: 'signup',
        loadComponent: () => import('./features/auth/signup-page').then((m) => m.SignupPage),
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password-page').then((m) => m.ForgotPasswordPage),
      },
      {
        path: 'reset-password',
        loadComponent: () => import('./features/auth/reset-password-page').then((m) => m.ResetPasswordPage),
      },
    ],
  },
  {
    path: '',
    component: MainLayout,
    canActivateChild: [authGuard],
    children: [
      {
        path: 'feed',
        loadComponent: () => import('./features/feed/feed-page').then((m) => m.FeedPage),
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile-page').then((m) => m.ProfilePage),
      },
      {
        path: 'discover',
        loadComponent: () => import('./features/discover/discover-page').then((m) => m.DiscoverPage),
      },
      {
        path: 'messages',
        loadComponent: () => import('./features/messages/messages-page').then((m) => m.MessagesPage),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications-page').then((m) => m.NotificationsPage),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/admin-dashboard-page').then((m) => m.AdminDashboardPage),
      },
      {
        path: 'admin/reports',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/admin-reports-page').then((m) => m.AdminReportsPage),
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'feed',
  },
];
