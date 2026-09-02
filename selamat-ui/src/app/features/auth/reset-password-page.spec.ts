import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../core/services';
import { ResetPasswordPage } from './reset-password-page';

describe('ResetPasswordPage', () => {
  let fixture: ComponentFixture<ResetPasswordPage>;
  let authService: {
    resetPassword: ReturnType<typeof vi.fn>;
  };

  async function configureResetPasswordPage(token: string | null = 'reset-token'): Promise<void> {
    authService = {
      resetPassword: vi.fn(() =>
        of({ message: 'Password has been reset successfully. Log in with your new password.' }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordPage],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(token ? { token } : {}),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordPage);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('shows validation and does not submit invalid password', async () => {
    await configureResetPasswordPage();

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.resetPassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Password must be between 8 and 100 characters.');
  });

  it('shows missing token message', async () => {
    await configureResetPasswordPage(null);
    fixture.componentInstance.resetPasswordForm.setValue({ newPassword: 'NewPassword123' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.resetPassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Password reset token is missing.');
  });

  it('submits token and new password and shows success message', async () => {
    await configureResetPasswordPage('reset-token');
    fixture.componentInstance.resetPasswordForm.setValue({ newPassword: 'NewPassword123' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.resetPassword).toHaveBeenCalledWith({
      token: 'reset-token',
      newPassword: 'NewPassword123',
    });
    expect(fixture.nativeElement.textContent).toContain(
      'Password has been reset successfully. Log in with your new password.',
    );
    expect(fixture.componentInstance.resetPasswordForm.controls.newPassword.value).toBe('');
  });

  it('shows backend error message when reset fails', async () => {
    await configureResetPasswordPage();
    authService.resetPassword.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401, error: { message: 'Invalid or expired token.' } })),
    );
    fixture.componentInstance.resetPasswordForm.setValue({ newPassword: 'NewPassword123' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Invalid or expired token.');
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });
});
