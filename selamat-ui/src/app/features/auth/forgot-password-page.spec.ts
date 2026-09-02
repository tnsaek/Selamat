import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../core/services';
import { ForgotPasswordPage } from './forgot-password-page';

describe('ForgotPasswordPage', () => {
  let fixture: ComponentFixture<ForgotPasswordPage>;
  let authService: {
    forgotPassword: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();

    authService = {
      forgotPassword: vi.fn(() =>
        of({ message: 'If an account exists for this email, a reset link has been sent.' }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordPage);
    fixture.detectChanges();
  });

  it('shows validation and does not submit invalid email', () => {
    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.forgotPassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Enter a valid email address.');
  });

  it('submits email and shows generic success message', () => {
    fixture.componentInstance.forgotPasswordForm.setValue({ email: 'tinsae@example.com' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(authService.forgotPassword).toHaveBeenCalledWith({ email: 'tinsae@example.com' });
    expect(fixture.nativeElement.textContent).toContain(
      'If an account exists for this email, a reset link has been sent.',
    );
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });

  it('shows backend error message when request fails', () => {
    authService.forgotPassword.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { message: 'Email service unavailable.' } })),
    );
    fixture.componentInstance.forgotPasswordForm.setValue({ email: 'tinsae@example.com' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Email service unavailable.');
    expect(fixture.componentInstance.isSubmitting()).toBe(false);
  });
});
