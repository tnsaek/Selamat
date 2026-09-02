import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AuthResponse, LoginRequest, User } from '../../models';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  const apiService = {
    post: vi.fn(),
  };

  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: ApiService, useValue: apiService }],
    });
  });

  it('saves session data after login', () => {
    const request: LoginRequest = {
      identifier: 'tinsae',
      password: 'Password123!',
    };
    const response = authResponse();
    apiService.post.mockReturnValue(of(response));
    const authService = TestBed.inject(AuthService);

    authService.login(request).subscribe();

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/login', request);
    expect(authService.accessToken()).toBe(response.accessToken);
    expect(authService.refreshToken()).toBe(response.refreshToken);
    expect(authService.currentUser()?.id).toBe(response.user.id);
    expect(authService.isAuthenticated()).toBe(true);
    expect(localStorage.getItem('selamat.accessToken')).toBe(response.accessToken);
  });

  it('saves session data after signup', () => {
    const request = {
      username: 'selam',
      email: 'selam@example.com',
      password: 'Password123!',
    };
    const response = authResponse();
    apiService.post.mockReturnValue(of(response));
    const authService = TestBed.inject(AuthService);

    authService.signup(request).subscribe();

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/signup', request);
    expect(authService.currentUser()).toEqual(response.user);
    expect(authService.isAuthenticated()).toBe(true);
  });

  it('refreshes the current session using the stored refresh token', () => {
    localStorage.setItem('selamat.refreshToken', 'stored-refresh');
    const response = authResponse();
    apiService.post.mockReturnValue(of(response));
    const authService = TestBed.inject(AuthService);

    authService.refreshSession().subscribe();

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/refresh', {
      refreshToken: 'stored-refresh',
    });
    expect(authService.accessToken()).toBe(response.accessToken);
  });

  it('sends an empty refresh token when no refresh token is stored', () => {
    const response = authResponse();
    apiService.post.mockReturnValue(of(response));
    const authService = TestBed.inject(AuthService);

    authService.refreshSession().subscribe();

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/refresh', {
      refreshToken: '',
    });
  });

  it('clears the session after logout succeeds', () => {
    localStorage.setItem('selamat.accessToken', 'access');
    localStorage.setItem('selamat.refreshToken', 'refresh');
    localStorage.setItem('selamat.currentUser', JSON.stringify(userResponse()));
    apiService.post.mockReturnValue(of(undefined));
    const authService = TestBed.inject(AuthService);

    authService.logout().subscribe();

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/logout', {});
    expect(authService.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('selamat.accessToken')).toBeNull();
  });

  it('loads existing session from localStorage', () => {
    const user = userResponse();
    localStorage.setItem('selamat.accessToken', 'existing-access');
    localStorage.setItem('selamat.refreshToken', 'existing-refresh');
    localStorage.setItem('selamat.currentUser', JSON.stringify(user));

    const authService = TestBed.inject(AuthService);

    expect(authService.accessToken()).toBe('existing-access');
    expect(authService.refreshToken()).toBe('existing-refresh');
    expect(authService.currentUser()?.username).toBe(user.username);
    expect(authService.isAuthenticated()).toBe(true);
  });

  it('clears invalid stored user JSON', () => {
    localStorage.setItem('selamat.currentUser', '{bad-json');

    const authService = TestBed.inject(AuthService);

    expect(authService.currentUser()).toBeNull();
    expect(localStorage.getItem('selamat.currentUser')).toBeNull();
  });

  it('clears session data', () => {
    localStorage.setItem('selamat.accessToken', 'access');
    localStorage.setItem('selamat.refreshToken', 'refresh');
    localStorage.setItem('selamat.currentUser', JSON.stringify(userResponse()));
    const authService = TestBed.inject(AuthService);

    authService.clearSession();

    expect(authService.accessToken()).toBeNull();
    expect(authService.refreshToken()).toBeNull();
    expect(authService.currentUser()).toBeNull();
    expect(authService.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('selamat.accessToken')).toBeNull();
  });

  it('requests a password reset link', () => {
    const request = { email: 'tinsae@example.com' };
    const response = { message: 'If an account exists for this email, a reset link has been sent.' };
    apiService.post.mockReturnValue(of(response));
    const authService = TestBed.inject(AuthService);

    authService.forgotPassword(request).subscribe((result) => {
      expect(result).toBe(response);
    });

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/forgot-password', request);
  });

  it('resets password with token and new password', () => {
    const request = { token: 'reset-token', newPassword: 'NewPassword123' };
    const response = { message: 'Password has been reset successfully. Log in with your new password.' };
    apiService.post.mockReturnValue(of(response));
    const authService = TestBed.inject(AuthService);

    authService.resetPassword(request).subscribe((result) => {
      expect(result).toBe(response);
    });

    expect(apiService.post).toHaveBeenCalledWith('/api/auth/reset-password', request);
  });

  function authResponse(): AuthResponse {
    return {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      user: userResponse(),
    };
  }

  function userResponse(): User {
    return {
      id: 'user-1',
      username: 'tinsae',
      email: 'tinsae@example.com',
      status: 'ACTIVE',
      roles: ['USER'],
      createdAt: '2026-07-24T10:00:00Z',
    };
  }
});
