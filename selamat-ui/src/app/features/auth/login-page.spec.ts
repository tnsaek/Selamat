import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../core/services';
import { LoginPage } from './login-page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let authService: {
    login: ReturnType<typeof vi.fn>;
  };

  async function configureLoginPage(reason: string | null = null): Promise<void> {
    authService = {
      login: vi.fn(() => of({ accessToken: 'access-token' })),
    };

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(reason ? { reason } : {}),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('shows validation messages and does not submit invalid form', async () => {
    await configureLoginPage();

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.login).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Username or email is required.');
    expect(fixture.nativeElement.textContent).toContain('Password is required.');
  });

  it('submits credentials and redirects to feed', async () => {
    await configureLoginPage();
    const router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.loginForm.setValue({
      identifier: 'selam@example.com',
      password: 'password123',
    });
    fixture.componentInstance.submit();

    expect(authService.login).toHaveBeenCalledWith({
      identifier: 'selam@example.com',
      password: 'password123',
    });
    expect(navigateByUrl).toHaveBeenCalledWith('/feed');
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });

  it('shows backend error message when login fails', async () => {
    await configureLoginPage();
    authService.login.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { message: 'Invalid request body.' } })),
    );

    fixture.componentInstance.loginForm.setValue({
      identifier: 'selam@example.com',
      password: 'password123',
    });
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Invalid request body.');
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });

  it('shows invalid credentials message for 401 without backend message', async () => {
    await configureLoginPage();
    authService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));

    fixture.componentInstance.loginForm.setValue({
      identifier: 'selam@example.com',
      password: 'wrong-password',
    });
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Invalid username, email, or password.');
  });

  it('shows inactive session message from route query parameter', async () => {
    await configureLoginPage('inactive');

    expect(fixture.nativeElement.textContent).toContain('You were logged out because your session was inactive.');
  });

  it('shows expired session message from route query parameter', async () => {
    await configureLoginPage('expired');

    expect(fixture.nativeElement.textContent).toContain('Your session expired. Log in again to continue.');
  });
});
