import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../services';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  const authService = {
    accessToken: vi.fn(),
    clearSession: vi.fn(),
  };
  const router = {
    navigate: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('adds bearer token to protected requests', async () => {
    authService.accessToken.mockReturnValue('access-token');
    const request = new HttpRequest('GET', '/api/feed');
    const next = createNext();

    await TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(request, next)));

    expect(next).toHaveBeenCalledOnce();
    expect(next.mock.calls[0][0].headers.get('Authorization')).toBe('Bearer access-token');
  });

  it('does not add bearer token to public auth requests', async () => {
    authService.accessToken.mockReturnValue('access-token');
    const request = new HttpRequest('POST', '/api/auth/login', {});
    const next = createNext();

    await TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(request, next)));

    expect(next.mock.calls[0][0].headers.has('Authorization')).toBe(false);
  });

  it('does not add bearer token to password reset requests', async () => {
    authService.accessToken.mockReturnValue('access-token');
    const request = new HttpRequest('POST', '/api/auth/reset-password', {});
    const next = createNext();

    await TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(request, next)));

    expect(next.mock.calls[0][0].headers.has('Authorization')).toBe(false);
  });

  it('does not add bearer token when no token exists', async () => {
    authService.accessToken.mockReturnValue(null);
    const request = new HttpRequest('GET', '/api/feed');
    const next = createNext();

    await TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(request, next)));

    expect(next.mock.calls[0][0].headers.has('Authorization')).toBe(false);
  });

  it('clears session and redirects to login on 401', async () => {
    authService.accessToken.mockReturnValue('access-token');
    const request = new HttpRequest('GET', '/api/feed');
    const error = new HttpErrorResponse({ status: 401 });
    const next = vi.fn(() => throwError(() => error));

    await expect(TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(request, next)))).rejects.toBe(error);

    expect(authService.clearSession).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: {
        reason: 'expired',
      },
    });
  });
});

function createNext(): ReturnType<typeof vi.fn<HttpHandlerFn>> {
  return vi.fn((handledRequest: HttpRequest<unknown>): Observable<HttpEvent<unknown>> => {
    return of(new HttpResponse({ status: 200, body: handledRequest }));
  });
}
