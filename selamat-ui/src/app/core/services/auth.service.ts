import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  PasswordResetMessageResponse,
  RefreshTokenRequest,
  ResetPasswordRequest,
  SignUpRequest,
  User,
} from '../../models';
import { ApiService } from './api.service';

const ACCESS_TOKEN_KEY = 'selamat.accessToken';
const REFRESH_TOKEN_KEY = 'selamat.refreshToken';
const CURRENT_USER_KEY = 'selamat.currentUser';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly currentUserSignal = signal<User | null>(this.loadCurrentUser());
  private readonly accessTokenSignal = signal<string | null>(this.readStorage(ACCESS_TOKEN_KEY));
  private readonly refreshTokenSignal = signal<string | null>(this.readStorage(REFRESH_TOKEN_KEY));

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly accessToken = this.accessTokenSignal.asReadonly();
  readonly refreshToken = this.refreshTokenSignal.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.accessTokenSignal() && this.currentUserSignal()));

  signup(request: SignUpRequest): Observable<AuthResponse> {
    return this.api
      .post<AuthResponse, SignUpRequest>('/api/auth/signup', request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api
      .post<AuthResponse, LoginRequest>('/api/auth/login', request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  refreshSession(): Observable<AuthResponse> {
    const refreshToken = this.refreshTokenSignal();
    const request: RefreshTokenRequest = {
      refreshToken: refreshToken ?? '',
    };

    return this.api
      .post<AuthResponse, RefreshTokenRequest>('/api/auth/refresh', request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  logout(): Observable<void> {
    return this.api.post<void, Record<string, never>>('/api/auth/logout', {}).pipe(tap(() => this.clearSession()));
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<PasswordResetMessageResponse> {
    return this.api.post<PasswordResetMessageResponse, ForgotPasswordRequest>('/api/auth/forgot-password', request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<PasswordResetMessageResponse> {
    return this.api.post<PasswordResetMessageResponse, ResetPasswordRequest>('/api/auth/reset-password', request);
  }

  clearSession(): void {
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.currentUserSignal.set(null);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(CURRENT_USER_KEY);
  }

  private saveSession(response: AuthResponse): void {
    this.accessTokenSignal.set(response.accessToken);
    this.refreshTokenSignal.set(response.refreshToken);
    this.currentUserSignal.set(response.user);

    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(response.user));
  }

  private loadCurrentUser(): User | null {
    const rawUser = this.readStorage(CURRENT_USER_KEY);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as User;
    } catch {
      localStorage.removeItem(CURRENT_USER_KEY);
      return null;
    }
  }

  private readStorage(key: string): string | null {
    return localStorage.getItem(key);
  }
}
